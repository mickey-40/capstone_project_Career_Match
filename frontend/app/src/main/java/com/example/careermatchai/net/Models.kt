package com.example.careermatchai.net

data class AnalyzeRequest(
    val resume_text: String,
    val job_description: String
)

data class AnalysisOut(
    val id: Long,
    val score: Double,
    val matched_skills: List<String>,
    val missing_skills: List<String>,
    val suggestions: String,
    val created_at: String
)
