package com.example.careermatchai.api

import retrofit2.http.GET
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

interface HealthService {
    @GET("/health")
    suspend fun health(): Map<String, Any>
}

object Api {
    // Emulator -> host loopback
    private const val BASE_URL = "https://d424-software-engineering-capstone-tdvq.onrender.com"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val health: HealthService = retrofit.create(HealthService::class.java)
}
