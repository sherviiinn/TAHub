"""Entry point of the AI service. Run with:  uvicorn app.main:app --reload --port 8000

Every endpoint except /health needs the X-Internal-Key header: only Spring calls this service.
Field names are camelCase, the same as in the Java code.
"""
import logging
from typing import Literal

from fastapi import Depends, FastAPI, HTTPException, Query, Request
from pydantic import BaseModel, Field

from app import llm, materials
from app.config import settings
from app.rag import qa, quiz, store
from app.security import require_internal_key

logging.basicConfig(level=logging.INFO, format="%(levelname)s %(name)s: %(message)s")

app = FastAPI(title="TA Hub AI Service")
store.fail_unfinished_jobs()  # jobs cut off by a restart are marked failed, so Spring can retry them

_key = [Depends(require_internal_key)]


@app.get("/health")
def health():
    """No key needed. Used to check that the service is up."""
    return {"status": "ok", "model": settings.gemini_model, "configured": llm.is_configured()}


# ---------- Notes (materials) ----------

class MaterialIn(BaseModel):
    materialId: str = Field(min_length=1, max_length=64)
    courseId: str = Field(min_length=1, max_length=64)
    professorId: str = Field(min_length=1, max_length=64)
    title: str = Field(min_length=1, max_length=200)
    chapter: str | None = Field(default=None, max_length=100)
    url: str = Field(min_length=1, max_length=500)


def _meta(material_id, course_id, professor_id, title, chapter) -> dict:
    return {"material_id": material_id, "course_id": course_id, "professor_id": professor_id,
            "title": title, "chapter": chapter}


@app.post("/materials/ingest", dependencies=_key, status_code=202)
def ingest_link(body: MaterialIn):
    """Download the PDF from the link and index it in the background. Check progress with GET."""
    try:
        materials.submit_link(_meta(body.materialId, body.courseId, body.professorId,
                                    body.title, body.chapter), body.url)
    except materials.MaterialError as e:
        raise HTTPException(status_code=400, detail=e.code)
    return {"materialId": body.materialId, "status": "queued"}


@app.post("/materials/upload", dependencies=_key, status_code=202)
async def ingest_upload(
    request: Request,
    materialId: str = Query(min_length=1, max_length=64),
    courseId: str = Query(min_length=1, max_length=64),
    professorId: str = Query(min_length=1, max_length=64),
    title: str = Query(min_length=1, max_length=200),
    chapter: str | None = Query(default=None, max_length=100),
):
    """Fallback when a link cannot be downloaded: the body is the PDF itself (Content-Type: application/pdf)."""
    if int(request.headers.get("content-length") or 0) > materials.MAX_BYTES:
        raise HTTPException(status_code=400, detail="too_large")
    data = await request.body()
    try:
        materials.submit_bytes(_meta(materialId, courseId, professorId, title, chapter), data)
    except materials.MaterialError as e:
        raise HTTPException(status_code=400, detail=e.code)
    return {"materialId": materialId, "status": "queued"}


@app.get("/materials/{material_id}", dependencies=_key)
def material_status(material_id: str):
    """status: queued, downloading, reading, ready or failed (then `error` says why)."""
    result = materials.status(material_id)
    if result is None:
        raise HTTPException(status_code=404, detail="unknown_material")
    return result


@app.delete("/materials/{material_id}", dependencies=_key)
def material_delete(material_id: str):
    return {"materialId": material_id, "status": "deleted", "chunks": materials.delete(material_id)}


# ---------- Course assistant ----------

class HistoryItem(BaseModel):
    role: Literal["user", "assistant"]
    text: str = Field(max_length=4000)


class AskIn(BaseModel):
    courseId: str = Field(min_length=1, max_length=64)
    professorId: str = Field(min_length=1, max_length=64)
    question: str = Field(min_length=1, max_length=1000)
    history: list[HistoryItem] = Field(default_factory=list, max_length=20)


@app.post("/ask", dependencies=_key)
def ask(body: AskIn):
    """Always answers with 200 and a `status`: answered, study_plan, greeting, thanks, bye, off_topic,
    not_found, no_materials, busy or not_configured (meanings in app/rag/qa.py)."""
    return qa.ask(body.courseId, body.professorId, body.question,
                  [h.model_dump() for h in body.history])


class QuizIn(BaseModel):
    courseId: str = Field(min_length=1, max_length=64)
    professorId: str = Field(min_length=1, max_length=64)
    kind: Literal["quiz", "problems", "flashcards"] = "quiz"
    count: int = Field(default=5, ge=1, le=quiz.MAX_ITEMS)
    topic: str | None = Field(default=None, max_length=500)


@app.post("/quiz", dependencies=_key)
def make_quiz(body: QuizIn):
    """Always answers with 200 and a `status` (ready, off_topic, no_materials, busy, not_configured)."""
    return quiz.make(body.courseId, body.professorId, body.kind, body.count, body.topic)


# ---------- Temporary ----------

class ChatIn(BaseModel):
    message: str = Field(min_length=1, max_length=2000)


@app.post("/debug/chat", dependencies=_key)
def debug_chat(body: ChatIn):
    """Temporary endpoint to test the whole chain (key check + Gemini). Removed before the demo."""
    try:
        answer = llm.generate_text(
            body.message,
            system="You are a helpful assistant for university students. Answer in Persian.",
        )
    except llm.LlmError as e:
        status = {"rate_limited": 429, "not_configured": 503}.get(e.code, 502)
        raise HTTPException(status_code=status, detail=f"{e.code}: {e}")
    return {"answer": answer}
