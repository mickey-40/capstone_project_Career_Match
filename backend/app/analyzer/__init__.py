from .keyword import KeywordAnalyzer

try:
    from .semantic import SemanticAnalyzer
except Exception:  # Optional dependency may be unavailable in lightweight environments.
    SemanticAnalyzer = None  # type: ignore[assignment]

__all__ = ["KeywordAnalyzer", "SemanticAnalyzer"]
