from .keyword import KeywordAnalyzer

__all__ = ["KeywordAnalyzer", "SemanticAnalyzer"]


def __getattr__(name: str):
    if name == "SemanticAnalyzer":
        from .semantic import SemanticAnalyzer
        return SemanticAnalyzer
    raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
