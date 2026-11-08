"""Answers a student's question using only the notes of one course + professor."""
from app import llm
from app.rag import store

TOP_K = 5
# Chunks less similar than this are ignored. Tune it with tools/ask.py (it prints the scores).
MIN_SCORE = 0.55

_SYSTEM = """You are a teaching assistant for a university course. You answer in Persian.
Use ONLY the numbered excerpts from the course notes that you are given.
If the excerpts do not contain the answer, do not answer from your own knowledge: set found to false.
Answer with ONLY this JSON:
{"found": true or false, "answer": "...", "sources": [numbers of the excerpts you used]}
"""


def ask(course_id: str, professor_id: str, question: str) -> dict:
    question = question.strip()[:1000]
    query_vector = llm.embed([question], for_query=True)[0]
    hits = [h for h in store.search(course_id, professor_id, query_vector, TOP_K)
            if h["score"] >= MIN_SCORE]
    if not hits:
        return {"found": False, "answer": "", "sources": [], "hits": []}

    excerpts = "\n\n".join(
        f"[{n}] ({h['title']}, page {h['page']})\n{h['text']}" for n, h in enumerate(hits, start=1)
    )
    result = llm.generate_json(f"Excerpts:\n{excerpts}\n\nQuestion: {question}", system=_SYSTEM)

    found = bool(result.get("found")) if isinstance(result, dict) else False
    used = [n for n in result.get("sources", []) if isinstance(n, int) and 1 <= n <= len(hits)] \
        if found else []
    sources, seen = [], set()
    for n in used:
        h = hits[n - 1]
        key = (h["materialId"], h["page"])
        if key not in seen:
            seen.add(key)
            sources.append({"title": h["title"], "chapter": h["chapter"], "page": h["page"]})
    return {
        "found": found,
        "answer": str(result.get("answer", "")).strip() if found else "",
        "sources": sources,
        "hits": [{"page": h["page"], "score": round(h["score"], 3)} for h in hits],  # for testing
    }
