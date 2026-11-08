"""Reads the text of every page of a PDF.

Gemini reads the pages (so pictures, diagrams and the broken Persian text work too).
Each file is read only once: the result is cached on disk by the file's hash, so
indexing the same file again does not use the free Gemini quota.
"""
import hashlib
import json
import logging

import pymupdf

from app import llm
from app.config import settings

log = logging.getLogger("reader")

PAGES_PER_REQUEST = 5

_PROMPT = """You are reading pages of a university lecture slide deck. The text is mostly Persian,
mixed with English words, code and formulas.

For EACH page of this PDF, transcribe everything that is on the page, faithfully:
- headings and body text exactly as written (do not summarize, do not add anything)
- code: copy it exactly
- formulas: write them in plain text, for example T(n) = 2T(n-1) + 1
- diagrams and pictures: describe briefly in Persian what they show, including every number,
  label and value that is visible
- if something is unreadable, write [unreadable]

Answer with ONLY a JSON list. One object per page, in the same order as the pages of this PDF:
[{"page": 1, "text": "..."}, {"page": 2, "text": "..."}]
"""


def file_hash(pdf_bytes: bytes) -> str:
    return hashlib.sha256(pdf_bytes).hexdigest()


def _ask_gemini(source: pymupdf.Document, pages: list[int]) -> list[str]:
    """Sends the given pages (0-based) as a small PDF and returns one text per page."""
    small = pymupdf.open()
    for p in pages:
        small.insert_pdf(source, from_page=p, to_page=p)
    result = llm.generate_json_from_pdf(small.tobytes(), _PROMPT)
    if isinstance(result, dict):  # sometimes the list is wrapped in an object
        result = next((v for v in result.values() if isinstance(v, list)), [])
    return [str(item.get("text", "")).strip() if isinstance(item, dict) else "" for item in result]


def read_pages(pdf_bytes: bytes) -> list[dict]:
    """Returns [{"page": 1, "text": "..."}, ...] for the whole PDF (page numbers start at 1).

    Progress is saved after every batch, so if a run stops in the middle (error, Ctrl+C),
    the next run continues from where it stopped instead of reading everything again.
    """
    cache_dir = settings.data_dir / "cache"
    cache_dir.mkdir(parents=True, exist_ok=True)
    name = file_hash(pdf_bytes)
    done_file = cache_dir / f"{name}.json"
    partial_file = cache_dir / f"{name}.partial.json"
    if done_file.exists():
        return json.loads(done_file.read_text(encoding="utf-8"))

    texts: list[str] = []
    if partial_file.exists():
        texts = json.loads(partial_file.read_text(encoding="utf-8"))
        log.info("Continuing: %s pages were already read", len(texts))

    source = pymupdf.open(stream=pdf_bytes, filetype="pdf")
    for start in range(len(texts), len(source), PAGES_PER_REQUEST):
        batch = list(range(start, min(start + PAGES_PER_REQUEST, len(source))))
        log.info("Reading pages %s-%s of %s", batch[0] + 1, batch[-1] + 1, len(source))
        got = _ask_gemini(source, batch)
        if len(got) != len(batch):
            # Gemini merged or skipped pages: read this batch again one page at a time.
            log.warning("Expected %s pages, got %s. Reading one by one.", len(batch), len(got))
            got = [(_ask_gemini(source, [p]) or [""])[0] for p in batch]
        texts.extend(got)
        partial_file.write_text(json.dumps(texts, ensure_ascii=False), encoding="utf-8")

    pages = [{"page": i + 1, "text": t} for i, t in enumerate(texts)]
    done_file.write_text(json.dumps(pages, ensure_ascii=False, indent=1), encoding="utf-8")
    partial_file.unlink(missing_ok=True)
    return pages
