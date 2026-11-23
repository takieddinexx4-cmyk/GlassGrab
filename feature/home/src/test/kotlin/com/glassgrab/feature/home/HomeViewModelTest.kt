package com.glassgrab.feature.home

import app.cash.turbine.test
import com.glassgrab.core.common.GlassError
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.Platform
import com.glassgrab.core.domain.usecase.AnalyzeUrlUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val analyzeUseCase = mockk<AnalyzeUrlUseCase>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits OpenFormatPicker effect when analysis succeeds`() = runTest {
        val sample = MediaItemInfo(
            id = "42",
            url = "https://www.tiktok.com/@a/video/42",
            title = "Clip",
            author = "Creator",
            thumbnailUrl = null,
            durationSeconds = 12,
            platform = Platform.TIKTOK,
            formats = emptyList(),
        )
        coEvery { analyzeUseCase(any()) } returns GlassResult.Success(sample)
        val vm = HomeViewModel(analyzeUseCase)

        vm.effects.test {
            vm.onIntent(HomeIntent.AnalyzeSharedUrl(sample.url))
            testDispatcher.scheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue(effect is HomeEffect.OpenFormatPicker)
            assertEquals(sample, (effect as HomeEffect.OpenFormatPicker).media)
        }
    }

    @Test
    fun `updates error state when analysis fails`() = runTest {
        coEvery { analyzeUseCase(any()) } returns GlassResult.Failure(GlassError.Offline())
        val vm = HomeViewModel(analyzeUseCase)

        vm.onIntent(HomeIntent.UrlChanged("https://youtu.be/test"))
        vm.onIntent(HomeIntent.AnalyzeClicked)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value.error is GlassError.Offline)
    }
}
