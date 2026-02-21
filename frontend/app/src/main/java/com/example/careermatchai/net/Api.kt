package com.example.careermatchai.net

import retrofit2.http.Body
import retrofit2.http.POST

interface Api {
    @POST("analyze")
    suspend fun analyze(@Body body: AnalyzeRequest): AnalysisOut
}
