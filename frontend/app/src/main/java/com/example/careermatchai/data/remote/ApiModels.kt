package com.example.careermatchai.data.remote

data class HealthOut(val status: String)
data class HealthResponse(val status: String)

data class LoginRequest(val email: String, val password: String)
data class Token(val access_token: String, val token_type: String = "bearer")
data class AnalyzeRequest(val resumeText: String, val jobText: String, val strategy: String = "keyword")
data class SkillItem(val name: String, val category: String?, val matchType: String, val source: String, val weight: Double)
data class Suggestion(val type: String, val message: String, val priority: Int)
data class KeywordOut(val score: Double, val skills: List<SkillItem>, val suggestions: List<Suggestion>)
data class SemanticMatch(val resumeChunk: String, val jobChunk: String, val similarity: Double)
data class SemanticMissing(val concept: String, val evidence: String, val confidence: Double)
data class SemanticOut(val score: Double, val topMatches: List<SemanticMatch>, val missingConcepts: List<SemanticMissing>, val error: String? = null)
data class SummaryOut(val overallScore: Double, val improvementNote: String? = null, val topGaps: List<String>)
data class AnalysisOut(
    val id: String,
    val readinessScore: Double,
    val createdAt: String,
    val skills: List<SkillItem>,
    val suggestions: List<Suggestion>,
    val keyword: KeywordOut? = null,
    val semantic: SemanticOut? = null,
    val summary: SummaryOut? = null
)
data class AnalysisRow(
    val id: String,
    val readinessScore: Double,
    val createdAt: String,
    val keywordScore: Double? = null,
    val semanticScore: Double? = null,
    val overallScore: Double? = null
)
data class PaginatedAnalyses(val items: List<AnalysisRow>, val total: Int)
