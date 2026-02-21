package com.example.careermatchai.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.careermatchai.data.Repository
import com.example.careermatchai.data.local.AppPrefs
import com.example.careermatchai.data.local.TokenStore
import com.example.careermatchai.data.remote.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val BASE_URL = "https://d424-software-engineering-capstone-tdvq.onrender.com"

private fun provideApiService(baseUrl: String = BASE_URL): ApiService {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    val client = OkHttpClient.Builder().addInterceptor(logging).build()
    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    return retrofit.create(ApiService::class.java)
}

object Di {
    fun provideRepository(context: Context): Repository {
        val api = provideApiService()
        val store = TokenStore(context)
        return Repository(api, store)
    }
    fun provideViewModel(context: Context): MainViewModel {
        val repo = provideRepository(context)
        val prefs = AppPrefs(context)
        return MainViewModel(repo, prefs)
    }
}

class VmFactory(
    private val context: Context,
    private val baseUrl: String = BASE_URL
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val api = provideApiService(baseUrl)
        val store = TokenStore(context)
        val repo = Repository(api, store)
        val prefs = AppPrefs(context)
        return MainViewModel(repo, prefs) as T
    }
}
