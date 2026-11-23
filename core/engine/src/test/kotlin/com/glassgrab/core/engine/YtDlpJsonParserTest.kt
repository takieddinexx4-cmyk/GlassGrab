package com.glassgrab.core.engine

import com.glassgrab.core.domain.model.Platform
import com.glassgrab.core.domain.model.VideoCodec
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class YtDlpJsonParserTest {

    private val parser = YtDlpJsonParser()

    @Test
    fun `parses TikTok JSON preferring no-watermark streams and skipping watermarked ones`() {
        val json = """
            {
              "id": "7382910293",
              "title": "Sunset in Sahara",
              "uploader": "@biskra_creator",
              "duration": 24.0,
              "thumbnail": "https://example.com/thumb.jpg",
              "formats": [
                {
                  "format_id": "download_addr-0",
                  "ext": "mp4",
                  "height": 2160,
                  "fps": 60,
                  "vcodec": "hvc1",
                  "acodec": "mp4a.40.2",
                  "filesize": 42000000,
                  "format_note": "Direct video"
                },
                {
                  "format_id": "play_watermark-1",
                  "ext": "mp4",
                  "height": 1080,
                  "fps": 30,
                  "vcodec": "avc1",
                  "acodec": "mp4a.40.2",
                  "filesize": 12000000,
                  "format_note": "watermarked"
                }
              ]
            }
        """.trimIndent()

        val info = parser.parseDumpJson(json, "https://www.tiktok.com/@biskra_creator/video/7382910293")

        assertEquals(Platform.TIKTOK, info.platform)
        assertEquals("Sunset in Sahara", info.title)
        assertTrue(info.formats.first().isBestOriginal)
        assertTrue(info.formats.first().isNoWatermark)
        assertFalse(info.formats.any { it.formatId == "play_watermark-1" })
        val uhd = info.formats.first { it.formatId == "download_addr-0" }
        assertEquals(2160, uhd.resolutionHeight)
        assertEquals(VideoCodec.HEVC, uhd.videoCodec)
    }

    @Test
    fun `parses YouTube separate video and audio streams with merge flag`() {
        val json = """
            {
              "id": "yt123",
              "title": "8K Nature",
              "channel": "DemoChannel",
              "duration": 180,
              "subtitles": { "en": [], "ar": [] },
              "formats": [
                {
                  "format_id": "571",
                  "ext": "mp4",
                  "height": 4320,
                  "fps": 60,
                  "vcodec": "av01.0.16M.08",
                  "acodec": "none",
                  "filesize": 500000000
                },
                {
                  "format_id": "251",
                  "ext": "webm",
                  "vcodec": "none",
                  "acodec": "opus",
                  "abr": 160,
                  "filesize": 3500000
                }
              ]
            }
        """.trimIndent()

        val info = parser.parseDumpJson(json, "https://www.youtube.com/watch?v=yt123")

        assertEquals(Platform.YOUTUBE, info.platform)
        assertTrue(info.hasSubtitles)
        assertEquals(listOf("en", "ar"), info.subtitleLanguages)
        val eightK = info.formats.first { it.formatId == "571" }
        assertTrue(eightK.requiresMerge)
        assertEquals("571+bestaudio/best", eightK.ytDlpSelector)
        assertEquals(VideoCodec.AV1, eightK.videoCodec)
    }

    @Test
    fun `parses playlist JSON into batch entries`() {
        val json = """
            {
              "_type": "playlist",
              "id": "PL123",
              "title": "Design Talks",
              "uploader": "GlassChannel",
              "entries": [
                { "id": "v1", "url": "https://youtu.be/v1", "title": "Part 1", "duration": 60 },
                { "id": "v2", "url": "https://youtu.be/v2", "title": "Part 2", "duration": 90 }
              ]
            }
        """.trimIndent()

        val info = parser.parseDumpJson(json, "https://www.youtube.com/playlist?list=PL123")

        assertTrue(info.isPlaylist)
        assertEquals(2, info.playlistEntries.size)
        assertEquals(150L, info.durationSeconds)
    }
}
