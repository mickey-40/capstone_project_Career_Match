package com.example.careermatchai.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.careermatchai.data.Repository
import com.example.careermatchai.data.local.AppPrefs
import com.example.careermatchai.data.remote.AnalysisOut
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class Sort { DATE_DESC, SCORE_DESC }

data class UiState(
    val pingStatus: String? = null,
    val loginStatus: String? = null,
    val token: String? = null,
    val analysis: AnalysisOut? = null,
    val loading: Boolean = false,
    val error: String? = null,

    // list/paging
    val analyses: List<com.example.careermatchai.data.remote.AnalysisRow> = emptyList(),
    val listLoading: Boolean = false,
    val listError: String? = null,
    val page: Int = 1,
    val pageSize: Int = 10,
    val total: Int = 0,
    val sort: Sort = Sort.DATE_DESC,

    // prefs surfaced to UI
    val isDark: Boolean = false,
    val isDemo: Boolean = false,
    val demoResume: String = "",
    val demoJob: String = ""
)

class MainViewModel(
    private val repo: Repository,
    private val prefs: AppPrefs
) : ViewModel() {

    var state by mutableStateOf(UiState())
        private set

    private val handler = CoroutineExceptionHandler { _, e ->
        state = state.copy(listLoading = false, listError = e.message)
    }

    private fun update(block: (UiState) -> UiState) { state = block(state) }
    private fun launchSafe(block: suspend () -> Unit) { viewModelScope.launch(handler) { block() } }

    // ---------- Backend calls ----------
    fun ping() = viewModelScope.launch {
        update { it.copy(loading = true, error = null) }
        runCatching { repo.ping() }
            .onSuccess { s -> update { it.copy(loading = false, pingStatus = s) } }
            .onFailure { e -> update { it.copy(loading = false, error = e.message ?: "Ping failed") } }
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        update { it.copy(loading = true, error = null) }
        runCatching { repo.login(email, password) }
            .onSuccess { t ->
                update { it.copy(loading = false, loginStatus = "OK", token = t) }
                loadAnalyses(page = 1)
            }
            .onFailure { e ->
                update { it.copy(loading = false, error = e.message ?: "Login failed") } }
    }

    fun logout() = viewModelScope.launch {
        runCatching { repo.logout() }
        update {
            it.copy(
                loginStatus = null, token = null, analysis = null,
                analyses = emptyList(), total = 0, page = 1
            )
        }
    }

    fun analyze(resume: String, job: String, strategy: String = "keyword") = viewModelScope.launch {
        update { it.copy(loading = true, error = null) }
        runCatching { repo.analyze(resume, job, strategy) }
            .onSuccess { out -> update { it.copy(loading = false, analysis = out) } }
            .onFailure { e -> update { it.copy(loading = false, error = e.message ?: "Analyze failed") } }
    }

    fun loadAnalyses(page: Int = state.page, q: String? = null) = launchSafe {
        update { it.copy(listLoading = true, listError = null) }
        val result = repo.listAnalyses(page = page, size = state.pageSize, q = q)

        // client-side sort
        val sorted = when (state.sort) {
            Sort.DATE_DESC  -> result.items.sortedByDescending { it.createdAt }
            Sort.SCORE_DESC -> result.items.sortedByDescending { it.readinessScore }
        }

        update {
            it.copy(
                listLoading = false,
                analyses = sorted,
                total = result.total,
                page = page
            )
        }
    }

    fun nextPage() {
        val maxPage = ((state.total + state.pageSize - 1) / state.pageSize).coerceAtLeast(1)
        if (state.page < maxPage) loadAnalyses(state.page + 1)
    }
    fun prevPage() { if (state.page > 1) loadAnalyses(state.page - 1) }

    fun openAnalysis(id: String) = viewModelScope.launch {
        update { it.copy(loading = true, error = null) }
        runCatching { repo.getAnalysis(id) }
            .onSuccess { out -> update { it.copy(loading = false, analysis = out) } }
            .onFailure { e -> update { it.copy(loading = false, error = e.message ?: "Load failed") } }
    }

    fun deleteAnalysis(id: String) = viewModelScope.launch {
        update { it.copy(listLoading = true, listError = null) }
        runCatching { repo.deleteAnalysis(id) }
            .onSuccess { loadAnalyses(state.page) }
            .onFailure { e -> update { it.copy(error = e.message ?: "Delete failed") } }
    }

    fun showUiError(message: String) { update { it.copy(error = message) } }

    // ---------- Preferences actions (persist + reload page 1) ----------
    fun setSort(sort: Sort) = viewModelScope.launch {
        if (state.sort == sort) return@launch
        prefs.setLastSort(sort.name)
        update { it.copy(sort = sort, page = 1) }
        loadAnalyses(page = 1)
    }

    fun setPageSize(size: Int) = viewModelScope.launch {
        if (state.pageSize == size) return@launch
        prefs.setLastPageSize(size)
        update { it.copy(pageSize = size, page = 1) }
        loadAnalyses(page = 1)
    }

    fun reloadFirstPage() = viewModelScope.launch {
        update { it.copy(page = 1) }
        loadAnalyses(page = 1)
    }

    // ---------- Init: apply saved prefs then load ----------
    init {
        viewModelScope.launch {
            val (savedSort, savedSize) = combine(prefs.lastSort, prefs.lastPageSize) { s, sz -> s to sz }.first()
            val enumSort = runCatching { Sort.valueOf(savedSort) }.getOrElse { Sort.DATE_DESC }
            update { it.copy(sort = enumSort, pageSize = savedSize, page = 1) }
            loadAnalyses(page = 1)
        }

        viewModelScope.launch { prefs.isDarkFlow.collectLatest { v -> update { it.copy(isDark = v) } } }
        viewModelScope.launch { prefs.isDemoFlow.collectLatest { v -> update { it.copy(isDemo = v) } } }
        viewModelScope.launch { prefs.demoResumeFlow.collectLatest { v -> update { it.copy(demoResume = v) } } }
        viewModelScope.launch { prefs.demoJobFlow.collectLatest { v -> update { it.copy(demoJob = v) } } }
    }
}
