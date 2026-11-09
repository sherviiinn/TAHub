"""All settings of the AI service, read once from the .env file / environment."""
import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

load_dotenv()

# Folder of the project (the one that contains "app" and ".env").
BASE_DIR = Path(__file__).resolve().parent.parent


@dataclass(frozen=True)
class Settings:
    gemini_api_key: str
    gemini_model: str
    gemini_fallback_model: str | None
    embedding_model: str
    internal_api_key: str
    data_dir: Path


def _load() -> Settings:
    # The service also starts without a key (for teammates who do not have one);
    # every AI request then fails with the "not_configured" error code.
    return Settings(
        gemini_api_key=os.getenv("GEMINI_API_KEY", "").strip(),
        # Model names live here, so changing them never needs a code change.
        gemini_model=os.getenv("GEMINI_MODEL", "gemini-3.8-flash").strip(),
        # Used when the main model is overloaded (503) or out of quota (429). Optional.
        gemini_fallback_model=os.getenv("GEMINI_FALLBACK_MODEL", "").strip() or None,
        embedding_model=os.getenv("EMBEDDING_MODEL", "gemini-embedding-001").strip(),
        internal_api_key=os.getenv("INTERNAL_API_KEY", "change-me-dev-key").strip(),
        # The database of chunks and the cache of read pages are kept here.
        data_dir=Path(os.getenv("DATA_DIR", str(BASE_DIR / "data"))),
    )


settings = _load()
