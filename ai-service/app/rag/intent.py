"""Recognizes messages that are not course questions, without calling the AI.

- small talk: greetings, thanks, goodbyes ("سلام"، "سلام خوبی؟"، "مرسی"، "خداحافظ"، "hi", ...)
  Only when the WHOLE message is small talk; "سلام، شرط پایه چیه؟" is still a question.
- study help: "این درس رو چطور بخونم؟"، "از کجا شروع کنم؟"، "چه مباحثی داره؟"
"""
import re

_GREETING = ["سلام", "سلام علیکم", "درود", "وقت بخیر", "صبح بخیر", "ظهر بخیر", "عصر بخیر", "شب بخیر",
             "خوبی", "خوبین", "خوبید", "چطوری", "چطورین", "حالت چطوره", "حالتون چطوره", "خسته نباشی",
             "خسته نباشید", "hi", "hello", "hey", "salam", "good morning", "good evening", "how are you"]
_THANKS = ["مرسی", "ممنون", "ممنونم", "متشکرم", "تشکر", "سپاس", "سپاسگزارم", "دمت گرم", "دستت درد نکنه",
           "عالی بود", "سلامت باشی", "قربونت", "قربانت", "لطف کردی", "thanks", "thank you", "thx", "merci"]
_BYE = ["خداحافظ", "خدانگهدار", "بای", "فعلا", "فعلا خداحافظ", "شب خوش", "bye", "goodbye", "see you"]
# Words that may sit next to small talk without making it a question.
_FILLER = {"و", "هم", "جان", "عزیز", "خیلی", "زیاد", "تو", "شما", "دوست", "من", "از", "بابت", "همه", "چیز",
           "ممنون", "واقعا", "خوبم", "منم", "ok", "اوکی", "باشه", "خب", "bot", "دستیار"}

_STUDY = re.compile(
    r"(چطور|چطوری|چجوری|چگونه|چه\s*جوری|چه\s*طور).{0,25}(بخونم|بخوانم|مطالعه|درس\s*بخونم|یاد\s*بگیرم|آماده\s*بشم|پاس\s*کنم)"
    r"|از\s*کجا\s*شروع"
    r"|برنامه\s*(ی\s*)?(مطالعه|درسی|خوندن)"
    r"|(چه|چی|کدوم)\s*(مباحث|مبحث|فصل|موضوع)"
    r"|سرفصل|مباحث\s*(این\s*)?(درس|جزوه)"
    r"|how\s+(should|do|can)\s+i\s+(study|learn|prepare)|study\s+plan"
)


def _normalize(text: str) -> str:
    text = text.replace("ي", "ی").replace("ك", "ک").replace("‌", " ").lower()
    text = re.sub(r"[^\w\s]", " ", text)  # punctuation and emoji out
    return " ".join(text.split())


def _strip_phrases(text: str, phrases: list[str]) -> tuple[str, bool]:
    found = False
    for phrase in sorted(phrases, key=len, reverse=True):
        pattern = rf"(^|\s){re.escape(phrase)}(\s|$)"
        if re.search(pattern, text):
            found = True
            text = re.sub(pattern, " ", text)
    return " ".join(text.split()), found


def small_talk(message: str) -> str | None:
    """'greeting', 'thanks' or 'bye' when the whole message is small talk, otherwise None."""
    text = _normalize(message)
    if not text or len(text) > 60:
        return None
    kinds = []
    for kind, phrases in (("thanks", _THANKS), ("bye", _BYE), ("greeting", _GREETING)):
        text, found = _strip_phrases(text, phrases)
        if found:
            kinds.append(kind)
    leftover = [w for w in text.split() if w not in _FILLER]
    if not kinds or leftover:
        return None
    return kinds[0]  # thanks/bye win over greeting ("مرسی سلامت باشی" is thanks)


def wants_study_help(message: str) -> bool:
    return bool(_STUDY.search(_normalize(message)))
