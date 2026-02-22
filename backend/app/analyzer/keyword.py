# app/analyzer/keyword.py
from abc import ABC, abstractmethod
import re
from string import punctuation
from typing import Dict, List, Set, Tuple

# General stopwords + domain stopwords common in job posts
STOPWORDS = {
    # general
    "the","a","an","and","or","of","to","in","for","with","on","at","by","from",
    "as","is","are","be","been","being","this","that","these","those","it","its",
    "your","our","their","i","we","you","they","he","she","him","her","his","hers",
    "them","my","me","us","than","then","but","so","if","else","do","does","did",
    "done","have","has","had","having","into","over","under","about","across","per",
    "via","etc","e.g","eg","ie","i.e","an","am","s","t",
    # job-domain filler
    "seeking","experience","experienced","opportunity","role","position","years",
    "strong","solid","requirements","preferred","plus","looking","candidate",
    "team","teams","environment","work","responsibilities","skills","ability",
    # job-post prose that tends to inflate denominator
    "build","maintain","maintaining","develop","developing","design","implement",
    "platform","service","services","scalable","reliable","reliability",
    "improve","improving","collaborate","collaboration","partner","cross",
    "functional","stakeholders","deliver","delivery","ensure","support",
    "understanding","knowledge","familiarity","proficiency","required",
    "qualification","qualifications","nice","must","minimum","preferred",
    "bonus","plus","including","include","across","using","used","use","based",
    "systems","software","engineer","engineering","backend","frontend","product",
    "organization","company","customer","business","impact","high","quality",
    "production","applications","application"
}

# Heuristic list to boost technical terms
TECH_HINTS = {
    "python","fastapi","flask","django",
    "java","kotlin","android","compose",
    "postgres","postgresql","mysql","sqlite","sql","nosql",
    "docker","kubernetes","k8s","aws","gcp","azure",
    "redis","rabbitmq","kafka",
    "nlp","ml","machine","learning","pytorch","tensorflow",
    "git","ci","cd","ci_cd","rest","api","graphql","oauth","jwt",
    "microservices","testing","pytest","junit","agile","scrum"
}

ALIASES = {
    "postgres": "postgresql",
    "k8s": "kubernetes",
    "apis": "api",
    "microservice": "microservices",
}

PHRASE_PATTERNS: List[Tuple[re.Pattern[str], str]] = [
    (re.compile(r"\bci\s*/\s*cd\b", re.IGNORECASE), "ci_cd"),
    (re.compile(r"\bcontinuous integration\b", re.IGNORECASE), "ci_cd"),
    (re.compile(r"\bcontinuous delivery\b", re.IGNORECASE), "ci_cd"),
    (re.compile(r"\brest(?:ful)?\s+api(?:s)?\b", re.IGNORECASE), "rest"),
    (re.compile(r"\bamazon web services\b", re.IGNORECASE), "aws"),
    (re.compile(r"\bgoogle cloud platform\b", re.IGNORECASE), "gcp"),
    (re.compile(r"\bmachine learning\b", re.IGNORECASE), "ml"),
]

TOKEN_RE = re.compile(r"[A-Za-z0-9+#\.]+")

def _normalize_token(tok: str) -> str:
    t = tok.lower().strip()
    while t and t[-1] in punctuation:
        t = t[:-1]
    return ALIASES.get(t, t)

def _is_noise(t: str) -> bool:
    # drop 1-char, all-digits, or stopwords
    return len(t) < 2 or t.isdigit() or t in STOPWORDS

def _extract_phrases(text: str) -> Set[str]:
    found: Set[str] = set()
    for pattern, canonical in PHRASE_PATTERNS:
        if pattern.search(text):
            found.add(canonical)
    return found

def extract_keywords(text: str) -> Set[str]:
    raw = TOKEN_RE.findall(text)
    normed = (_normalize_token(t) for t in raw)
    tokens = {t for t in normed if t and not _is_noise(t)}
    tokens |= _extract_phrases(text)
    return tokens

def _split_required_and_nice(job_text: str) -> Tuple[str, str]:
    """
    Split common job description sections into required and nice-to-have text.
    If no markers are found, treat entire text as required.
    """
    lower = job_text.lower()
    markers = ["nice to have", "preferred", "bonus", "plus:"]
    marker_positions = [lower.find(m) for m in markers if lower.find(m) != -1]
    if not marker_positions:
        return job_text, ""
    idx = min(marker_positions)
    return job_text[:idx], job_text[idx:]

class BaseAnalyzer(ABC):
    @abstractmethod
    def run(self, resume_text: str, job_text: str) -> Dict: ...

class KeywordAnalyzer(BaseAnalyzer):
    """Keyword matcher with normalization, phrase matching, and weighted scoring."""
    def run(self, resume_text: str, job_text: str) -> Dict:
        resume_set = extract_keywords(resume_text)
        required_text, nice_text = _split_required_and_nice(job_text)
        required_set = extract_keywords(required_text)
        nice_set = extract_keywords(nice_text)
        job_set = required_set | nice_set

        # Assign weights (tech terms get a boost)
        def weight(token: str) -> float:
            base = 1.5 if token in TECH_HINTS else 1.0
            if token in required_set:
                return base * 1.2
            if token in nice_set:
                return base * 0.8
            return base

        matched = sorted(job_set & resume_set)
        missing = sorted(job_set - resume_set)
        extra   = sorted(resume_set - job_set)

        total_weight = sum(weight(t) for t in job_set) or 1.0
        matched_weight = sum(weight(t) for t in matched)
        score = round(matched_weight / total_weight, 3)

        skills: List[Dict] = []
        for s in matched:
            skills.append({"name": s, "category": None, "matchType": "MATCHED", "source": "BOTH", "weight": weight(s)})
        for s in missing:
            skills.append({"name": s, "category": None, "matchType": "MISSING", "source": "JOB", "weight": weight(s)})
        for s in extra:
            skills.append({"name": s, "category": None, "matchType": "EXTRA", "source": "RESUME", "weight": 0.5})

        suggestions: List[Dict] = []
        if missing:
            suggestions.append({
                "type": "ADD",
                "message": f"Consider adding or demonstrating: {', '.join(missing[:10])}.",
                "priority": 1
            })
        if extra:
            suggestions.append({
                "type": "REPHRASE",
                "message": "Trim or reframe unrelated items to better match the job description.",
                "priority": 2
            })

        return {
            "readinessScore": score,
            "skills": skills,
            "suggestions": suggestions,
        }
