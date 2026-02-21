package com.example.careermatchai.data.remote

import retrofit2.http.*

interface ApiService {
    @GET("health")
    suspend fun health(): HealthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): Token

    @POST("analyze")
    suspend fun analyze(
        @Header("Authorization") bearer: String,
        @Body body: AnalyzeRequest
    ): AnalysisOut

    @GET("analyses")
    suspend fun listAnalyses(
        @Header("Authorization") bearer: String,
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 10,
        @Query("q") q: String? = null
    ): PaginatedAnalyses

    @GET("analyses/{id}")
    suspend fun getAnalysis(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): AnalysisOut

    @DELETE("analyses/{id}")
    suspend fun deleteAnalysis(
        @Header("Authorization") bearer: String,
        @Path("id") id: String
    ): retrofit2.Response<Unit>


}
