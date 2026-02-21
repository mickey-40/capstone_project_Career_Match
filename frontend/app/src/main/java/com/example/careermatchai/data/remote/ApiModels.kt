package com.example.careermatchai.data.remote

data class HealthOut(val status: String)
data class HealthResponse(val status: String)

data class LoginRequest(val email: String, val password: String)
data class Token(val access_token: String, val token_type: String = "bearer")
data class AnalyzeRequest(val resumeText: String, val jobText: String, val strategy: String = "keyword")
data class SkillItem(val name: String, val category: String?, val matchType: String, val source: String, val weight: Double)
data class Suggestion(val type: String, val message: String, val priority: Int)
data class AnalysisOut(
    val id: String,
    val readinessScore: Double,
    val createdAt: String,
    val skills: List<SkillItem>,
    val suggestions: List<Suggestion>
)
data class AnalysisRow(val id: String, val readinessScore: Double, val createdAt: String)
data class PaginatedAnalyses(val items: List<AnalysisRow>, val total: Int)

