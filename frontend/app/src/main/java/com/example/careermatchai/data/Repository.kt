package com.example.careermatchai.data

import com.example.careermatchai.data.local.TokenStore
import com.example.careermatchai.data.remote.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow

class Repository(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    // Expose the token so VM can observe it
    val tokenFlow: Flow<String?> get() = tokenStore.tokenFlow

    suspend fun ping(): String = api.health().status

    suspend fun login(email: String, password: String): String {
        val token = api.login(LoginRequest(email, password))
        tokenStore.save(token.access_token)
        return token.access_token
    }

    suspend fun logout() {
        tokenStore.clear()
    }

    suspend fun analyze(resume: String, job: String, strategy: String = "keyword"): AnalysisOut {
        val token = tokenStore.tokenFlow.firstOrNull() ?: error("Not authenticated")
        return api.analyze("Bearer $token", AnalyzeRequest(resume, job, strategy))
    }

    suspend fun listAnalyses(page: Int, size: Int, q: String? = null): PaginatedAnalyses {
        val token = tokenStore.tokenFlow.firstOrNull() ?: error("Not authenticated")
        return api.listAnalyses("Bearer $token", page, size, q)
    }

    suspend fun getAnalysis(id: String): AnalysisOut {
        val token = tokenStore.tokenFlow.firstOrNull() ?: error("Not authenticated")
        return api.getAnalysis("Bearer $token", id)
    }

    suspend fun deleteAnalysis(id: String) {
        val token = tokenStore.tokenFlow.firstOrNull() ?: error("Not authenticated")
        val resp = api.deleteAnalysis("Bearer $token", id)
        if (!resp.isSuccessful) error("Delete failed (${resp.code()})")
    }


}
