"""The ONLY file that talks to Gemini. Everything else calls the functions here.

If we ever change the provider (or the model), this is the one file to edit.
"""
import json
import logging
import time

import httpx
from google import genai
from google.genai import errors, types

from app.config import settings

log = logging.getLogger("llm")

# A request that takes longer than this is cancelled and tried again (instead of hanging forever).
_TIMEOUT_SECONDS = 120
_client = genai.Client(
    api_key=settings.gemini_api_key,
    http_options=types.HttpOptions(timeout=_TIMEOUT_SECONDS * 1000),  # milliseconds
)

# The free tier is rate limited and busy models answer 503, so we wait and try again.
_RETRY_CODES = {429, 500, 503}
_WAITS = [5, 15]  # seconds to wait before the 2nd and 3rd try
# When the main model is still busy (503) or out of quota (429), the fallback model is tried.
_SWITCH_CODES = {429, 503}
# A model that was busy is skipped for this many seconds (we go straight to the other one).
_BUSY_SECONDS = 600
_busy_until: dict[str, float] = {}

# Size of one embedding vector. 768 is plenty for our notes and keeps the database small.
EMBEDDING_SIZE = 768
_EMBED_BATCH = 50  # texts sent in one embedding request


class LlmError(Exception):
    """Raised when Gemini fails. `code` tells the caller what kind of failure it was."""

    def __init__(self, code: str, message: str = "", status: int | None = None):
        super().__init__(message or code)
        self.code = code  # "rate_limited", "network_error" or "llm_failed"
        self.status = status  # HTTP status from Gemini, if there was one


def _with_retry(fn):
    """Runs fn() and tries again on rate limits, busy servers and broken connections."""
    tries = len(_WAITS) + 1
    for attempt in range(1, tries + 1):
        try:
            return fn()
        except errors.APIError as e:
            log.warning("Gemini error: %s (try %s/%s)", e, attempt, tries)
            if e.code in _RETRY_CODES and attempt < tries:
                time.sleep(_WAITS[attempt - 1])
                continue
            code = "rate_limited" if e.code == 429 else "llm_failed"
            raise LlmError(code, str(e), status=e.code) from e
        except (httpx.HTTPError, OSError) as e:
            # The connection itself broke (VPN dropped, firewall, no internet...).
            log.warning("Network error: %s (try %s/%s)", e, attempt, tries)
            if attempt < tries:
                time.sleep(_WAITS[attempt - 1])
                continue
            raise LlmError("network_error", str(e)) from e


def _call(**kwargs):
    models = [settings.gemini_model]
    if settings.gemini_fallback_model:
        models.append(settings.gemini_fallback_model)
    # Models that were busy a moment ago go to the end of the list.
    now = time.time()
    models.sort(key=lambda m: _busy_until.get(m, 0) > now)
    for i, model in enumerate(models):
        try:
            return _with_retry(lambda: _client.models.generate_content(model=model, **kwargs))
        except LlmError as e:
            if e.status in _SWITCH_CODES:
                _busy_until[model] = time.time() + _BUSY_SECONDS
                if i + 1 < len(models):
                    log.warning("Model %s is busy, switching to %s", model, models[i + 1])
                    continue
            raise


def _parse_json(response):
    text = (response.text or "").strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError as e:
        raise LlmError("llm_failed", "Model did not return valid JSON") from e


# Note: temperature is left unset on purpose (Gemini's default). Newer Gemini models
# work best with their default; pass a value only if you have a specific reason.


def generate_text(prompt: str, system: str | None = None, temperature: float | None = None) -> str:
    """Plain text answer."""
    response = _call(
        contents=prompt,
        config=types.GenerateContentConfig(system_instruction=system, temperature=temperature),
    )
    return (response.text or "").strip()


def generate_json(prompt: str, system: str | None = None, temperature: float | None = None):
    """Answer that must be valid JSON. Returns the parsed Python object (dict or list)."""
    response = _call(
        contents=prompt,
        config=types.GenerateContentConfig(
            system_instruction=system,
            temperature=temperature,
            response_mime_type="application/json",
        ),
    )
    return _parse_json(response)


def generate_json_from_pdf(pdf_bytes: bytes, prompt: str, system: str | None = None,
                           temperature: float | None = None):
    """Gemini reads the PDF pages directly (text AND images, like a human) and answers in JSON."""
    response = _call(
        contents=[types.Part.from_bytes(data=pdf_bytes, mime_type="application/pdf"), prompt],
        config=types.GenerateContentConfig(
            system_instruction=system,
            temperature=temperature,
            response_mime_type="application/json",
        ),
    )
    return _parse_json(response)


def embed(texts: list[str], for_query: bool = False) -> list[list[float]]:
    """Turns each text into a vector of numbers. Similar meaning -> similar vectors.

    for_query=True for the student's question, False for the pieces of the notes.
    """
    task = "RETRIEVAL_QUERY" if for_query else "RETRIEVAL_DOCUMENT"
    vectors: list[list[float]] = []
    for start in range(0, len(texts), _EMBED_BATCH):
        batch = texts[start:start + _EMBED_BATCH]
        result = _with_retry(lambda: _client.models.embed_content(
            model=settings.embedding_model,
            contents=batch,
            config=types.EmbedContentConfig(task_type=task, output_dimensionality=EMBEDDING_SIZE),
        ))
        vectors.extend(e.values for e in result.embeddings)
    return vectors
