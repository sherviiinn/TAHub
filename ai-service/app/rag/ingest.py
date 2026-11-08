"""PDF -> page texts -> chunks -> vectors -> database."""
import logging

from app import llm
from app.rag import reader, store

log = logging.getLogger("ingest")

MAX_CHUNK = 1200   # a page longer than this is split
CHUNK_SIZE = 1000
OVERLAP = 150


def make_chunks(pages: list[dict]) -> list[dict]:
    """One chunk per page (our notes are slides). Long pages are cut with some overlap."""
    chunks = []
    for p in pages:
        text = " ".join(p["text"].split())  # one line, single spaces
        if not text:
            continue
        if len(text) <= MAX_CHUNK:
            parts = [text]
        else:
            parts = [text[i:i + CHUNK_SIZE] for i in range(0, len(text), CHUNK_SIZE - OVERLAP)]
        for n, part in enumerate(parts):
            chunks.append({"page": p["page"], "chunk_index": n, "text": part})
    return chunks


def ingest_pdf(pdf_bytes: bytes, material_id: str, course_id: str, professor_id: str,
               title: str, chapter: str | None = None) -> dict:
    """Indexes one PDF. If the same file was already indexed, nothing is done."""
    file_hash = reader.file_hash(pdf_bytes)
    if store.get_hash(material_id) == file_hash:
        return {"status": "unchanged"}

    pages = reader.read_pages(pdf_bytes)
    chunks = make_chunks(pages)
    # The title goes in front of each chunk, so the search also knows the topic.
    vectors = llm.embed([f"{title}\n{c['text']}" for c in chunks])
    store.save_material(material_id, course_id, professor_id, title, chapter,
                        file_hash, chunks, vectors)
    log.info("Indexed %s: %s pages, %s chunks", material_id, len(pages), len(chunks))
    return {"status": "indexed", "pages": len(pages), "chunks": len(chunks)}
