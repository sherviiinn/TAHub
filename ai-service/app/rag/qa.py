"""Answers a student's question using only the notes of one course + professor.

The answer always has a `status`, so Spring can always show the student something.
Spring shows its own fixed text (messages.properties) for every status except answered/study_plan:
  answered       - answer from the notes, with sources
  study_plan     - "how should I study this course?": a short plan built from the topics of the notes
  greeting / thanks / bye - small talk, answered with a short fixed text (no AI call)
  off_topic      - nothing in the notes is close to the message (unrelated or personal); no AI call
  not_found      - the notes are about this topic but do not contain the answer
  no_materials   - this course + professor has no ready notes yet
  busy           - the AI server is busy or unreachable; try again in a moment
                   (relatedPages may still list the pages that match the question)
  not_configured - no Gemini key on this machine (only on developer laptops)
"""
from app import llm
from app.rag import intent, store

TOP_K = 5
# Chunks less similar than this are ignored (tested: related 0.69-0.74, other topic 0.62, unrelated 0.49).
MIN_SCORE = 0.55
HISTORY_MESSAGES = 6  # how many earlier messages of the conversation are sent along

_SYSTEM = """You are a teaching assistant for a university course. You answer in Persian.
Use ONLY the numbered excerpts from the course notes that you are given.
The earlier conversation is only there to understand follow-up questions such as "another example".
If the excerpts do not contain the answer, do not answer from your own knowledge: set found to false.
Answer with ONLY this JSON:
{"found": true or false, "answer": "...", "sources": [numbers of the excerpts you used]}
"""


def _status_for(error: llm.LlmError) -> str:
    return "not_configured" if error.code == "not_configured" else "busy"


def _source(hit: dict) -> dict:
    return {"materialId": hit["materialId"], "title": hit["title"],
            "chapter": hit["chapter"], "page": hit["page"]}


def _unique_sources(hits: list[dict]) -> list[dict]:
    sources, seen = [], set()
    for h in hits:
        key = (h["materialId"], h["page"])
        if key not in seen:
            seen.add(key)
            sources.append(_source(h))
    return sources


def _result(status: str, answer: str = "", sources=None, related=None, hits=None) -> dict:
    return {"status": status, "answer": answer, "sources": sources or [],
            "relatedPages": related or [],
            "hits": [{"page": h["page"], "score": round(h["score"], 3)} for h in hits or []]}


def ask(course_id: str, professor_id: str, question: str, history: list[dict] | None = None) -> dict:
    """history: earlier messages of this conversation, oldest first: [{"role": "user"|"assistant", "text": "..."}]"""
    question = question.strip()[:1000]
    history = (history or [])[-HISTORY_MESSAGES:]
    kind = intent.small_talk(question)
    if kind:
        return _result(kind)
    if not store.has_materials(course_id, professor_id):
        return _result("no_materials")
    if intent.wants_study_help(question):
        return study_plan(course_id, professor_id, question)

    # For a follow-up like "another example", search with the previous question too.
    last_question = next((h["text"] for h in reversed(history) if h.get("role") == "user"), "")
    search_text = f"{last_question[:500]}\n{question}" if last_question else question
    try:
        query_vector = llm.embed([search_text], for_query=True)[0]
    except llm.LlmError as e:
        return _result(_status_for(e))

    hits = [h for h in store.search(course_id, professor_id, query_vector, TOP_K)
            if h["score"] >= MIN_SCORE]
    if not hits:
        return _result("off_topic")

    excerpts = "\n\n".join(
        f"[{n}] ({h['title']}, page {h['page']})\n{h['text']}" for n, h in enumerate(hits, start=1)
    )
    conversation = "\n".join(
        f"{'Student' if h.get('role') == 'user' else 'Assistant'}: {str(h.get('text', ''))[:1000]}"
        for h in history
    )
    prompt = (f"Excerpts:\n{excerpts}\n\n"
              + (f"Earlier conversation:\n{conversation}\n\n" if conversation else "")
              + f"Question: {question}")
    try:
        result = llm.generate_json(prompt, system=_SYSTEM)
    except llm.LlmError as e:
        return _result(_status_for(e), related=_unique_sources(hits)[:3], hits=hits)

    if not isinstance(result, dict) or not result.get("found") or not str(result.get("answer", "")).strip():
        return _result("not_found", hits=hits)
    used = [n for n in result.get("sources", []) if isinstance(n, int) and 1 <= n <= len(hits)]
    return _result("answered", answer=str(result["answer"]).strip(),
                   sources=_unique_sources([hits[n - 1] for n in used] or hits[:1]), hits=hits)


_PLAN_SYSTEM = """You help a university student plan how to study a course, in Persian.
You get the list of pages of the course notes (title, page number and the start of each page).
Use ONLY the topics in this list. Write a short plan: the order to study the topics, with page
ranges, plus 3 or 4 short practical tips (for example: solve the exercises in the notes, review the
code examples). Use plain text with short lines; no tables. Do not invent topics."""


def study_plan(course_id: str, professor_id: str, question: str) -> dict:
    pages = store.outline(course_id, professor_id)
    listing = "\n".join(
        f"- {p['title']}{' / ' + p['chapter'] if p['chapter'] else ''}, page {p['page']}: {p['head']}"
        for p in pages
    )
    try:
        answer = llm.generate_text(f"Pages of the notes:\n{listing}\n\nStudent: {question}",
                                   system=_PLAN_SYSTEM)
    except llm.LlmError as e:
        return _result(_status_for(e))
    if not answer:
        return _result("busy")
    sources, seen = [], set()
    for p in pages:  # one source per material: its first page
        if p["materialId"] not in seen:
            seen.add(p["materialId"])
            sources.append({"materialId": p["materialId"], "title": p["title"],
                            "chapter": p["chapter"], "page": p["page"]})
    return _result("study_plan", answer=answer, sources=sources)
