package com.example.careermatchai.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore by preferencesDataStore("app_prefs")

class AppPrefs(private val context: Context) {

    // Existing keys
    private object Keys {
        val IS_DARK   = booleanPreferencesKey("is_dark")
        val IS_DEMO   = booleanPreferencesKey("is_demo")
        val DEMO_RES  = stringPreferencesKey("demo_resume")
        val DEMO_JOB  = stringPreferencesKey("demo_job")
    }

    // New persisted sort + page size
    private object PrefKeys {
        val LAST_SORT = stringPreferencesKey("last_sort")
        val LAST_PAGE_SIZE = intPreferencesKey("last_page_size")
    }

    // New flows (note: use context.dataStore)
    val lastSort: Flow<String> = context.dataStore.data
        .map { it[PrefKeys.LAST_SORT] ?: "DATE_DESC" }   // default newest first

    val lastPageSize: Flow<Int> = context.dataStore.data
        .map { it[PrefKeys.LAST_PAGE_SIZE] ?: 20 }

    suspend fun setLastSort(value: String) {
        context.dataStore.edit { it[PrefKeys.LAST_SORT] = value }
    }

    suspend fun setLastPageSize(value: Int) {
        context.dataStore.edit { it[PrefKeys.LAST_PAGE_SIZE] = value }
    }

    // Existing prefs
    val isDarkFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK] ?: false }
    val isDemoFlow: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DEMO] ?: false }
    val demoResumeFlow: Flow<String> = context.dataStore.data.map { it[Keys.DEMO_RES] ?: "" }
    val demoJobFlow: Flow<String> = context.dataStore.data.map { it[Keys.DEMO_JOB] ?: "" }

    suspend fun setDark(v: Boolean) = context.dataStore.edit { it[Keys.IS_DARK] = v }
    suspend fun setDemo(v: Boolean) = context.dataStore.edit { it[Keys.IS_DEMO] = v }
    suspend fun setDemoResume(v: String) = context.dataStore.edit { it[Keys.DEMO_RES] = v }
    suspend fun setDemoJob(v: String) = context.dataStore.edit { it[Keys.DEMO_JOB] = v }
}
