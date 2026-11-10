"""Quiz questions, written problems and flashcards made from the notes of one course + professor.

kinds: quiz (4 options), problems (written problems such as code or time complexity, with a
step-by-step solution in the notes' own method), flashcards (front / back).

The result has a `status` like the answers (see qa.py): ready, off_topic (topic not in the notes), no_materials,
busy or not_configured. Correct answers come together with the questions, so showing
an answer later needs no new AI call.
"""
from app import llm
from app.rag import store
from app.rag.qa import MIN_SCORE, _source, _status_for

MAX_ITEMS = 10
MAX_PROBLEMS = 5  # written problems have long solutions
EXCERPTS = 8

_SYSTEM = """You write study material for university students, in Persian.
Use ONLY the numbered excerpts from the course notes. Never invent facts that are not in them.
Every item must say which excerpt it comes from."""

_QUIZ_PROMPT = """Write {count} multiple-choice questions.
Each question has exactly 4 options and exactly one correct option. Wrong options must be believable.
Answer with ONLY this JSON:
{{"items": [{{"question": "...", "options": ["...", "...", "...", "..."], "correctIndex": 0,
  "explanation": "why the correct option is right", "source": excerpt number}}]}}"""

_PROBLEM_PROMPT = """Write {count} written (descriptive) problems like a university exam: for example writing or
tracing code, or finding the time complexity / recurrence of an algorithm. Base them on the examples and
methods in the excerpts, but do NOT copy an example from the notes as it is: change the numbers,
values or small details so the student has to solve a new problem with the same method.
For each problem write a step-by-step solution that uses EXACTLY the method the notes use (for example,
if the notes solve a recurrence by repeated substitution, solve it the same way). Put code inside
``` blocks, and put every short code expression inside the text between single backticks
(for example `*q = 10`), so it stays readable inside Persian text. Also give the short final answer.
Answer with ONLY this JSON:
{{"items": [{{"question": "...", "solution": "step-by-step solution", "finalAnswer": "...", "source": excerpt number}}]}}"""

_CARD_PROMPT = """Write {count} flashcards. The front is a short question or term, the back is a short, clear answer.
Answer with ONLY this JSON:
{{"items": [{{"front": "...", "back": "...", "source": excerpt number}}]}}"""


def _result(status: str, kind: str, items=None) -> dict:
    return {"status": status, "kind": kind, "items": items or []}


def make(course_id: str, professor_id: str, kind: str = "quiz", count: int = 5,
         topic: str | None = None) -> dict:
    count = max(1, min(int(count), MAX_PROBLEMS if kind == "problems" else MAX_ITEMS))
    if not store.has_materials(course_id, professor_id):
        return _result("no_materials", kind)

    try:
        if topic and topic.strip():
            vector = llm.embed([topic.strip()[:500]], for_query=True)[0]
            chunks = [c for c in store.search(course_id, professor_id, vector, EXCERPTS)
                      if c["score"] >= MIN_SCORE]
            if not chunks:
                return _result("off_topic", kind)
        else:
            chunks = store.random_chunks(course_id, professor_id, EXCERPTS)

        excerpts = "\n\n".join(
            f"[{n}] ({c['title']}, page {c['page']})\n{c['text']}" for n, c in enumerate(chunks, start=1)
        )
        prompt = {"quiz": _QUIZ_PROMPT, "problems": _PROBLEM_PROMPT}.get(kind, _CARD_PROMPT)
        task = prompt.format(count=count)
        result = llm.generate_json(f"Excerpts:\n{excerpts}\n\n{task}", system=_SYSTEM)
    except llm.LlmError as e:
        return _result(_status_for(e), kind)

    raw = result.get("items", []) if isinstance(result, dict) else result if isinstance(result, list) else []
    items = []
    for item in raw:
        if not isinstance(item, dict):
            continue
        n = item.get("source")
        source = _source(chunks[n - 1]) if isinstance(n, int) and 1 <= n <= len(chunks) else None
        if kind == "quiz":
            options = item.get("options")
            correct = item.get("correctIndex")
            if (isinstance(options, list) and len(options) == 4 and all(isinstance(o, str) and o.strip() for o in options)
                    and isinstance(correct, int) and 0 <= correct < 4 and str(item.get("question", "")).strip()):
                items.append({"question": str(item["question"]).strip(), "options": [o.strip() for o in options],
                              "correctIndex": correct, "explanation": str(item.get("explanation", "")).strip(),
                              "source": source})
        elif kind == "problems":
            if str(item.get("question", "")).strip() and str(item.get("solution", "")).strip():
                items.append({"question": str(item["question"]).strip(), "solution": str(item["solution"]).strip(),
                              "finalAnswer": str(item.get("finalAnswer", "")).strip(), "source": source})
        elif str(item.get("front", "")).strip() and str(item.get("back", "")).strip():
            items.append({"front": str(item["front"]).strip(), "back": str(item["back"]).strip(),
                          "source": source})
    if not items:
        return _result("busy", kind)  # the model returned nothing usable; trying again usually works
    return _result("ready", kind, items[:count])
