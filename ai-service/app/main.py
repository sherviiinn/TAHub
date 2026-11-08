"""Entry point of the AI service. Run with:  uvicorn app.main:app --reload --port 8000"""
from fastapi import Depends, FastAPI, HTTPException
from pydantic import BaseModel, Field

from app import llm
from app.config import settings
from app.security import require_internal_key

app = FastAPI(title="TA Hub AI Service")


@app.get("/health")
def health():
    """No key needed. Used to check that the service is up."""
    return {"status": "ok", "model": settings.gemini_model}


class ChatIn(BaseModel):
    message: str = Field(min_length=1, max_length=2000)


@app.post("/debug/chat", dependencies=[Depends(require_internal_key)])
def debug_chat(body: ChatIn):
    """Temporary endpoint to test the whole chain (key check + Gemini). Removed before the demo."""
    try:
        answer = llm.generate_text(
            body.message,
            system="You are a helpful assistant for university students. Answer in Persian.",
        )
    except llm.LlmError as e:
        status = 429 if e.code == "rate_limited" else 502
        raise HTTPException(status_code=status, detail=f"{e.code}: {e}")
    return {"answer": answer}
