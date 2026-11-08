r"""Tests reading PDF pages with Gemini (instead of plain text extraction).

Usage:   python tools\gemini_pdf_test.py path\to\file.pdf [pages]
Example: python tools\gemini_pdf_test.py "C:\Users\me\Desktop\part 8.pdf" 3,10,24

If you do not give the pages, pages 3, 10 and 24 are used.
The result is saved to "gemini_extracted_sample.txt" (UTF-8) - open it with Notepad.
"""
import sys
import time
from pathlib import Path

import pymupdf

# Make "import app" work when this file is run from the tools folder.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app import llm  # noqa: E402

PROMPT = """You are reading pages of a university lecture slide deck. The text is Persian,
mixed with English words and C++ code.

For EACH page of this PDF, transcribe everything that is on the page, faithfully:
- headings and body text, in Persian, exactly as written (do not summarize, do not add anything)
- code: copy it exactly
- formulas: write them in plain text, for example T(n) = 2T(n-1) + 1
- diagrams and pictures: describe briefly in Persian what they show, including every number,
  label and value that is visible
- if something is unreadable, write [unreadable]

Answer with ONLY a JSON list. One object per page, in the same order as the pages of this PDF:
[{"page": 1, "text": "..."}, {"page": 2, "text": "..."}]
where "page" is the position of the page inside this PDF (1 for the first page, 2 for the second...).
"""

if len(sys.argv) < 2:
    sys.exit("Usage: python tools\\gemini_pdf_test.py path\\to\\file.pdf [pages, e.g. 3,10,24]")

pdf_path = sys.argv[1]
pages = [int(p) for p in (sys.argv[2] if len(sys.argv) > 2 else "3,10,24").split(",")]

source = pymupdf.open(pdf_path)
for p in pages:
    if not 1 <= p <= len(source):
        sys.exit(f"Page {p} does not exist (the PDF has {len(source)} pages).")

# Build a small PDF that contains only the chosen pages.
small = pymupdf.open()
for p in pages:
    small.insert_pdf(source, from_page=p - 1, to_page=p - 1)
data = small.tobytes()
print(f"Sending {len(pages)} page(s) {pages} ({len(data) // 1024} KB) to Gemini... please wait")

started = time.time()
try:
    result = llm.generate_json_from_pdf(data, PROMPT)
except llm.LlmError as e:
    sys.exit(f"Gemini failed: {e.code}: {e}")
print(f"Done in {time.time() - started:.0f} seconds.")

if isinstance(result, dict):  # sometimes the list is wrapped in an object
    result = next((v for v in result.values() if isinstance(v, list)), [])

with open("gemini_extracted_sample.txt", "w", encoding="utf-8") as f:
    for i, item in enumerate(result):
        original = pages[i] if i < len(pages) else "?"
        f.write(f"===== original page {original} =====\n{item.get('text', '')}\n\n")

print(f"Pages returned: {len(result)} (expected {len(pages)}).")
print("Saved to gemini_extracted_sample.txt -- open it with Notepad.")
