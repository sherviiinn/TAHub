"""Where the pieces (chunks) of the notes and their vectors are kept: one SQLite file.

Search is done with numpy: load the vectors of one course+professor and compare them
with the question's vector. This is fast enough for thousands of chunks.
"""
import sqlite3
from contextlib import closing

import numpy as np

from app.config import settings

_SCHEMA = """
CREATE TABLE IF NOT EXISTS materials (
    material_id  TEXT PRIMARY KEY,
    course_id    TEXT NOT NULL,
    professor_id TEXT NOT NULL,
    title        TEXT NOT NULL,
    chapter      TEXT,
    file_hash    TEXT NOT NULL,
    indexed_at   TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS chunks (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    material_id  TEXT NOT NULL REFERENCES materials(material_id) ON DELETE CASCADE,
    page         INTEGER NOT NULL,
    chunk_index  INTEGER NOT NULL,
    text         TEXT NOT NULL,
    embedding    BLOB NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_materials_owner ON materials(course_id, professor_id);
CREATE INDEX IF NOT EXISTS idx_chunks_material ON chunks(material_id);
"""


def _connect() -> sqlite3.Connection:
    settings.data_dir.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(settings.data_dir / "rag.db")
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA foreign_keys = ON")
    conn.executescript(_SCHEMA)
    return conn


def _normalize(vectors) -> np.ndarray:
    """Makes every vector length 1, so a dot product is the cosine similarity."""
    arr = np.asarray(vectors, dtype=np.float32)
    norms = np.linalg.norm(arr, axis=-1, keepdims=True)
    return arr / np.where(norms == 0, 1, norms)


def get_hash(material_id: str) -> str | None:
    with closing(_connect()) as conn:
        row = conn.execute("SELECT file_hash FROM materials WHERE material_id = ?",
                           (material_id,)).fetchone()
        return row["file_hash"] if row else None


def save_material(material_id: str, course_id: str, professor_id: str, title: str,
                  chapter: str | None, file_hash: str, chunks: list[dict], vectors) -> None:
    """Replaces everything stored for this material. chunks: [{"page", "chunk_index", "text"}]."""
    vecs = _normalize(vectors)
    with closing(_connect()) as conn, conn:  # "with conn" = one transaction
        conn.execute("DELETE FROM materials WHERE material_id = ?", (material_id,))
        conn.execute(
            "INSERT INTO materials (material_id, course_id, professor_id, title, chapter, file_hash)"
            " VALUES (?, ?, ?, ?, ?, ?)",
            (material_id, course_id, professor_id, title, chapter, file_hash),
        )
        conn.executemany(
            "INSERT INTO chunks (material_id, page, chunk_index, text, embedding) VALUES (?, ?, ?, ?, ?)",
            [(material_id, c["page"], c["chunk_index"], c["text"], v.tobytes())
             for c, v in zip(chunks, vecs)],
        )


def delete_material(material_id: str) -> int:
    """Removes a material and its chunks. Returns how many chunks were removed."""
    with closing(_connect()) as conn, conn:
        count = conn.execute("SELECT COUNT(*) FROM chunks WHERE material_id = ?",
                             (material_id,)).fetchone()[0]
        conn.execute("DELETE FROM materials WHERE material_id = ?", (material_id,))
        return count


def search(course_id: str, professor_id: str, query_vector, top_k: int = 5) -> list[dict]:
    """The top_k chunks of this course+professor most similar to the query, best first."""
    with closing(_connect()) as conn:
        rows = conn.execute(
            "SELECT c.text, c.page, c.embedding, m.title, m.chapter, m.material_id"
            " FROM chunks c JOIN materials m ON m.material_id = c.material_id"
            " WHERE m.course_id = ? AND m.professor_id = ?",
            (course_id, professor_id),
        ).fetchall()
    if not rows:
        return []
    matrix = np.stack([np.frombuffer(r["embedding"], dtype=np.float32) for r in rows])
    scores = matrix @ _normalize(query_vector)
    best = np.argsort(-scores)[:top_k]
    return [
        {"text": rows[i]["text"], "page": rows[i]["page"], "title": rows[i]["title"],
         "chapter": rows[i]["chapter"], "materialId": rows[i]["material_id"],
         "score": float(scores[i])}
        for i in best
    ]
