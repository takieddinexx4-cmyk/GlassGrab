package com.glassgrab.core.domain

import com.glassgrab.core.common.DeviceCapabilities
import com.glassgrab.core.common.GlassError
import com.glassgrab.core.common.GlassResult
import com.glassgrab.core.domain.model.DownloadRequest
import com.glassgrab.core.domain.model.MediaFormat
import com.glassgrab.core.domain.model.MediaItemInfo
import com.glassgrab.core.domain.model.Platform
import com.glassgrab.core.domain.model.VideoCodec
import com.glassgrab.core.domain.repository.DownloadRepository
import com.glassgrab.core.domain.repository.MediaExtractorRepository
import com.glassgrab.core.domain.usecase.AnalyzeUrlUseCase
import com.glassgrab.core.domain.usecase.StartDownloadUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UseCasesTest {

    private val extractor = mockk<MediaExtractorRepository>()
    private val downloads = mockk<DownloadRepository>()
    private val capabilities = mockk<DeviceCapabilities>()

    @Test
    fun `analyze rejects DRM platforms before calling engine`() = runTest {
        every { capabilities.isOnline() } returns true
        val useCase = AnalyzeUrlUseCase(extractor, capabilities)

        val result = useCase("https://www.netflix.com/watch/80100172")

        assertTrue(result is GlassResult.Failure)
        val error = (result as GlassResult.Failure).error
        assertTrue(error is GlassError.DrmNotSupported)
        assertEquals("Netflix", (error as GlassError.DrmNotSupported).platformName)
    }

    @Test
    fun `analyze returns Offline error when device has no network`() = runTest {
        every { capabilities.isOnline() } returns false
        val useCase = AnalyzeUrlUseCase(extractor, capabilities)

        val result = useCase("https://www.tiktok.com/@creator/video/123456")

        assertTrue(result is GlassResult.Failure)
        assertTrue((result as GlassResult.Failure).error is GlassError.Offline)
    }

    @Test
    fun `analyze delegates valid URL to extractor`() = runTest {
        every { capabilities.isOnline() } returns true
        val expected = MediaItemInfo(
            id = "123",
            url = "https://www.tiktok.com/@creator/video/123",
            title = "Glass Demo",
            author = "creator",
            thumbnailUrl = null,
            durationSeconds = 15,
            platform = Platform.TIKTOK,
            formats = emptyList(),
        )
        coEvery { extractor.analyzeUrl(any()) } returns GlassResult.Success(expected)
        val useCase = AnalyzeUrlUseCase(extractor, capabilities)

        val result = useCase("https://www.tiktok.com/@creator/video/123")

        assertEquals(GlassResult.Success(expected), result)
    }

    @Test
    fun `startDownload fails with LowStorage when free space is insufficient`() = runTest {
        every { capabilities.isOnline() } returns true
        every { capabilities.availableStorageBytes() } returns 10L * 1024 * 1024
        val useCase = StartDownloadUseCase(downloads, capabilities)

        val request = DownloadRequest(
            url = "https://youtu.be/abc",
            title = "4K Clip",
            author = "Tester",
            thumbnailUrl = null,
            platform = Platform.YOUTUBE,
            format = MediaFormat(
                formatId = "313+251",
                label = "2160p 60fps",
                resolutionHeight = 2160,
                fps = 60,
                videoCodec = VideoCodec.VP9,
                audioCodec = null,
                container = "mkv",
                fileSizeBytes = 250L * 1024 * 1024,
                isAudioOnly = false,
                ytDlpSelector = "313+251",
            ),
        )

        val result = useCase(request)
        assertTrue(result is GlassResult.Failure)
        assertTrue((result as GlassResult.Failure).error is GlassError.LowStorage)
    }
}
