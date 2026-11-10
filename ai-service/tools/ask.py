r"""Asks a question about the indexed notes (for testing, before Spring is connected).

Usage:   python tools\ask.py courseId professorId "question"
Example: python tools\ask.py c1 p1 "شرط پایه در تابع بازگشتی چیست؟"
The answer is saved to "answer.txt" (UTF-8) - open it with Notepad.
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.rag.qa import ask  # noqa: E402

if len(sys.argv) != 4:
    sys.exit(__doc__)

result = ask(*sys.argv[1:])
Path("answer.txt").write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
print("status:", result["status"], "| scores:", result["hits"])
print("Saved to answer.txt -- open it with Notepad.")
