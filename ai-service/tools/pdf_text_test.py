r"""Checks how well text can be extracted from a PDF (especially Persian ones).

Usage:   python tools\pdf_text_test.py path\to\file.pdf

It prints the page count and how many characters each of the first pages has.
It also saves the text of the first 3 pages that contain text into
"extracted_sample.txt" (UTF-8). Open that file with Notepad to check that the
Persian text is readable and in the right order (the Windows terminal itself
cannot display Persian correctly).
"""
import sys

import fitz  # PyMuPDF

if len(sys.argv) != 2:
    sys.exit("Usage: python tools\\pdf_text_test.py path\\to\\file.pdf")

doc = fitz.open(sys.argv[1])
print(f"Pages: {len(doc)}")

empty = 0
saved = []
for number, page in enumerate(doc, start=1):
    text = page.get_text().strip()
    if not text:
        empty += 1
    elif len(saved) < 3:
        saved.append((number, text))
    if number <= 5:
        print(f"  page {number}: {len(text)} characters")

print(f"Pages with NO text: {empty} of {len(doc)}  (a high number means a scanned PDF)")

with open("extracted_sample.txt", "w", encoding="utf-8") as f:
    for number, text in saved:
        f.write(f"===== page {number} =====\n{text}\n\n")

if saved:
    print("Saved the text of", len(saved), "page(s) to extracted_sample.txt -- open it with Notepad.")
else:
    print("No page had any text, so nothing was saved (the PDF is probably scanned images).")
