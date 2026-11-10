"""Adding notes (materials) to the assistant: from a link or from the PDF bytes.

Indexing a PDF takes minutes, so it runs in the background. The caller gets an answer
at once and asks for the status later (GET /materials/{id}).
"""
import ipaddress
import logging
import re
import socket
from concurrent.futures import ThreadPoolExecutor
from urllib.parse import urlparse

import httpx

from app import llm
from app.rag import store
from app.rag.ingest import ingest_pdf

log = logging.getLogger("materials")

MAX_BYTES = 20 * 1024 * 1024  # 20 MB
# One file at a time, so the free Gemini quota is not used up by many files at once.
_executor = ThreadPoolExecutor(max_workers=1)


class MaterialError(Exception):
    """A problem with the file or link. `code` is short and stable, for Spring to show a message."""

    def __init__(self, code: str):
        super().__init__(code)
        self.code = code  # bad_link, unsupported_link, too_large, not_pdf, download_failed


def normalize_url(url: str) -> str:
    """Checks the link and turns a Google Drive page link into a direct download link."""
    url = url.strip()
    parsed = urlparse(url)
    if parsed.scheme != "https" or not parsed.hostname:
        raise MaterialError("bad_link")
    host = parsed.hostname.lower()
    if host in ("t.me", "telegram.me") or host.endswith(".t.me"):
        raise MaterialError("unsupported_link")  # Telegram files need a login
    if host == "drive.google.com":
        match = re.search(r"/file/d/([\w-]+)", parsed.path) or re.search(r"[?&]id=([\w-]+)", url)
        if not match:
            raise MaterialError("bad_link")
        return f"https://drive.usercontent.google.com/download?id={match.group(1)}&export=download&confirm=t"
    return url


def _check_host(request: httpx.Request) -> None:
    """Never download from addresses inside our own network (also after a redirect)."""
    host = request.url.host
    try:
        infos = socket.getaddrinfo(host, None)
    except OSError:
        return  # cannot resolve here (for example behind a proxy); the proxy resolves it
    for info in infos:
        ip = ipaddress.ip_address(info[4][0])
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved:
            raise MaterialError("bad_link")


def check_pdf(data: bytes) -> None:
    if len(data) > MAX_BYTES:
        raise MaterialError("too_large")
    if not data.startswith(b"%PDF-"):
        raise MaterialError("not_pdf")


def download(url: str) -> bytes:
    try:
        with httpx.Client(follow_redirects=True, timeout=60,
                          event_hooks={"request": [_check_host]}) as client:
            with client.stream("GET", url) as response:
                response.raise_for_status()
                data = bytearray()
                for part in response.iter_bytes():
                    data.extend(part)
                    if len(data) > MAX_BYTES:
                        raise MaterialError("too_large")
    except httpx.HTTPError as e:
        log.warning("Download failed for %s: %s", url, e)
        raise MaterialError("download_failed") from e
    check_pdf(bytes(data))
    return bytes(data)


def _run(meta: dict, url: str | None = None, data: bytes | None = None) -> None:
    material_id = meta["material_id"]
    try:
        if url:
            store.set_job_status(material_id, "downloading")
            data = download(url)
        store.set_job_status(material_id, "reading")
        ingest_pdf(data, **meta)
        store.set_job_status(material_id, "ready")
    except MaterialError as e:
        store.set_job_status(material_id, "failed", e.code)
    except llm.LlmError as e:
        store.set_job_status(material_id, "failed", e.code)
    except Exception:  # noqa: BLE001 - the job must always end in a final status
        log.exception("Indexing %s failed", material_id)
        store.set_job_status(material_id, "failed", "internal_error")


def submit_link(meta: dict, url: str) -> None:
    url = normalize_url(url)  # bad links are refused at once, not in the background
    store.save_job(**meta, source_url=url)
    _executor.submit(_run, meta, url=url)


def submit_bytes(meta: dict, data: bytes) -> None:
    check_pdf(data)
    store.save_job(**meta, source_url=None)
    _executor.submit(_run, meta, data=data)


def status(material_id: str) -> dict | None:
    job = store.get_job(material_id)
    if not job:
        return None
    return {
        "materialId": material_id,
        "status": job["status"],
        "error": job["error"],
        "chunks": store.count_chunks(material_id) if job["status"] == "ready" else 0,
        "updatedAt": job["updated_at"],
    }


def delete(material_id: str) -> int:
    store.delete_job(material_id)
    return store.delete_material(material_id)
