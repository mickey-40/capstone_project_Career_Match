from abc import ABC, abstractmethod
from typing import Dict, List, Tuple
import re

from sentence_transformers import SentenceTransformer, util
import os


class BaseSemanticAnalyzer(ABC):
    @abstractmethod
    def run(self, resume_text: str, job_text: str) -> Dict: ...


class SemanticAnalyzer(BaseSemanticAnalyzer):
    """
    Skeleton semantic analyzer.
    Replace stub logic with embedding-based similarity and missing concept detection.
    """
    _model: SentenceTransformer | None = None

    def __init__(self, model_name: str | None = None, top_k: int = 8, missing_threshold: float = 0.35):
        # Default to a smaller model for production
        self.model_name = model_name or os.getenv("SEMANTIC_MODEL", "all-MiniLM-L3-v2")
        self.top_k = top_k
        self.missing_threshold = missing_threshold

    def _get_model(self) -> SentenceTransformer:
        if SemanticAnalyzer._model is None:
            SemanticAnalyzer._model = SentenceTransformer(self.model_name)
        return SemanticAnalyzer._model

    def _chunk_text(self, text: str) -> List[str]:
        # Prefer paragraph splits; fall back to sentence-ish splits
        full = text.strip()
        parts = [p.strip() for p in text.split("\n\n") if p.strip()]
        if len(parts) >= 2:
            if full and full not in parts:
                return [full] + parts
            return parts
        # Simple sentence splitting (keep minimal to avoid heavy deps)
        sentences = re.split(r"(?<=[.!?])\s+", text.strip())
        sentences = [s.strip() for s in sentences if s.strip()]
        if sentences:
            if full and full not in sentences:
                return [full] + sentences
            return sentences
        return [full] if full else [text.strip()]

    def _summarize_concept(self, text: str, max_words: int = 8) -> str:
        words = re.findall(r"[A-Za-z0-9+#\.]+", text)
        return " ".join(words[:max_words])

    def run(self, resume_text: str, job_text: str) -> Dict:
        model = self._get_model()
        resume_chunks = self._chunk_text(resume_text)
        job_chunks = self._chunk_text(job_text)

        resume_emb = model.encode(resume_chunks, normalize_embeddings=True)
        job_emb = model.encode(job_chunks, normalize_embeddings=True)

        # Similarity matrix: job x resume
        sim = util.cos_sim(job_emb, resume_emb)

        # Top matches: take top_k highest pairs
        flat_scores = []
        for j_idx in range(len(job_chunks)):
            for r_idx in range(len(resume_chunks)):
                flat_scores.append((float(sim[j_idx][r_idx]), j_idx, r_idx))
        flat_scores.sort(reverse=True, key=lambda x: x[0])

        top_matches = []
        for score, j_idx, r_idx in flat_scores[: self.top_k]:
            top_matches.append({
                "resumeChunk": resume_chunks[r_idx],
                "jobChunk": job_chunks[j_idx],
                "similarity": round(score, 3),
            })

        # Missing concepts: job chunks with low best match
        missing = []
        for j_idx in range(len(job_chunks)):
            best = float(sim[j_idx].max())
            if best < self.missing_threshold:
                missing.append({
                    "concept": self._summarize_concept(job_chunks[j_idx]),
                    "evidence": "Low semantic match to resume content",
                    "confidence": round(1.0 - best, 3),
                })

        # Score: mean of top-N best similarities to avoid overly harsh averages
        best_scores = [float(sim[j_idx].max()) for j_idx in range(len(job_chunks))] if job_chunks else [0.0]
        best_scores.sort(reverse=True)
        top_n = min(max(self.top_k, 1), len(best_scores))
        score = sum(best_scores[:top_n]) / max(top_n, 1)

        return {
            "score": round(score, 3),
            "topMatches": top_matches,
            "missingConcepts": missing,
        }
