from abc import ABC, abstractmethod
from typing import Dict, List


class Analyzer(ABC):
  @abstractmethod
  def run(self, resume_text: str, job_text: str) -> Dict:
    """Return dict with readinessScore, skills[], suggestions[]"""
    raise NotImplementedError