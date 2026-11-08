r"""Indexes one PDF from your disk (for testing, before Spring is connected).

Usage:   python tools\ingest_file.py "path\to\file.pdf" materialId courseId professorId "Title"
Example: python tools\ingest_file.py "C:\Users\salam\Desktop\part 8.pdf" m1 c1 p1 "Recursion, structs, pointers"
"""
import logging
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app import llm  # noqa: E402
from app.rag.ingest import ingest_pdf  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(message)s")

if len(sys.argv) != 6:
    sys.exit(__doc__)

path, material_id, course_id, professor_id, title = sys.argv[1:]
started = time.time()
try:
    result = ingest_pdf(Path(path).read_bytes(), material_id, course_id, professor_id, title)
except llm.LlmError as e:
    sys.exit(f"Gemini failed: {e.code}: {e}")
print(f"{result}  ({time.time() - started:.0f} seconds)")
