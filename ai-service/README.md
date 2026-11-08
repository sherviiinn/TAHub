# TA Hub AI Service

Python service used by the Spring backend (RAG Q&A, quiz generator, review summarizer).
It is called only by Spring, never by the browser.

## Run (Windows)
    python -m venv venv
    venv\Scripts\activate
    pip install -r requirements.txt
    copy .env.example .env      (then edit .env and put the real Gemini key)
    uvicorn app.main:app --reload --port 8000

Open http://127.0.0.1:8000/docs to try the endpoints.
