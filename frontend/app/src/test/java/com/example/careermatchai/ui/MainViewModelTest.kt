package com.example.careermatchai.ui

import com.example.careermatchai.data.Repository
import com.example.careermatchai.data.local.AppPrefs
import com.example.careermatchai.data.remote.AnalysisOut
import com.example.careermatchai.data.remote.AnalysisRow
import com.example.careermatchai.data.remote.PaginatedAnalyses
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun fakePage(page: Int, size: Int) = PaginatedAnalyses(
        total = 100,
        items = (1..size).map {
            AnalysisRow(
                id = "id-$page-$it",
                readinessScore = 0.5,
                createdAt = "2024-01-01T12:00:00Z"
            )
        }
    )

    private fun repoStub(): Repository {
        val repo = mockk<Repository>(relaxed = true, relaxUnitFun = true)

        // Generic safe defaults
        coEvery { repo.ping() } returns "OK"
        coEvery { repo.login(any(), any()) } returns "token-123"
        coEvery { repo.logout() } returns Unit

        coEvery { repo.analyze(any(), any()) } returns AnalysisOut(
            id = "x", readinessScore = 0.85, suggestions = emptyList(),
            createdAt = "2024-01-01T12:00:00Z", skills = emptyList()
        )
        coEvery { repo.getAnalysis(any()) } returns AnalysisOut(
            id = "y", readinessScore = 0.90, suggestions = emptyList(),
            createdAt = "2024-01-02T12:00:00Z", skills = emptyList()
        )

        // 🔐 Important: stub BOTH q=null and q=any() variants so MockK always finds an answer.
        coEvery { repo.listAnalyses(page = any(), size = any(), q = null) } answers {
            val page = firstArg<Int>()
            val size = secondArg<Int>()
            fakePage(page, size)
        }
        coEvery { repo.listAnalyses(page = any(), size = any(), q = any()) } answers {
            val page = firstArg<Int>()
            val size = secondArg<Int>()
            fakePage(page, size)
        }

        return repo
    }

    private fun prefsStub(): AppPrefs {
        val prefs = mockk<AppPrefs>(relaxed = true, relaxUnitFun = true)
        every { prefs.lastSort } returns flowOf("DATE_DESC")
        every { prefs.lastPageSize } returns flowOf(10)
        return prefs
    }

    @Test
    fun setSort_does_not_throw() = runTest {
        val vm = MainViewModel(repoStub(), prefsStub())
        advanceUntilIdle()
        vm.setSort(Sort.SCORE_DESC)
        advanceUntilIdle()
        // no assertion needed; lack of exception = pass
    }

    @Test
    fun setPageSize_does_not_throw() = runTest {
        val vm = MainViewModel(repoStub(), prefsStub())
        advanceUntilIdle()
        vm.setPageSize(20)
        advanceUntilIdle()
    }

    @Test
    fun reloadFirstPage_does_not_throw() = runTest {
        val vm = MainViewModel(repoStub(), prefsStub())
        advanceUntilIdle()
        vm.reloadFirstPage()
        advanceUntilIdle()
    }
}
