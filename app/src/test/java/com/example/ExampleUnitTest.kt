package com.example

import com.example.data.ai.PlaceholderAiRepository
import com.example.data.remote.YouTubeUrlValidator
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun youtubeUrlValidator_acceptsStandardShortsAndLiveUrls() {
        val watch = YouTubeUrlValidator.validate("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertTrue(watch.isValid)
        assertEquals("dQw4w9WgXcQ", watch.videoId)

        val shortUrl = YouTubeUrlValidator.validate("https://youtu.be/abc123DEF_9")
        assertTrue(shortUrl.isValid)
        assertEquals("abc123DEF_9", shortUrl.videoId)

        val shorts = YouTubeUrlValidator.validate("https://youtube.com/shorts/xyz98765432")
        assertTrue(shorts.isValid)
        assertEquals("xyz98765432", shorts.videoId)
    }

    @Test
    fun youtubeUrlValidator_rejectsInvalidUrls() {
        val bad = YouTubeUrlValidator.validate("https://vimeo.com/123456")
        assertFalse(bad.isValid)

        val blank = YouTubeUrlValidator.validate("   ")
        assertFalse(blank.isValid)
    }

    @Test
    fun placeholderAi_generatesLabeledSampleClipsInRange() = runTest {
        val repo = PlaceholderAiRepository()
        val analysis = repo.analyzeScenesForClips(
            videoTitle = "Midnight Neon Stunt",
            durationMs = 120_000L,
            contextNotes = ""
        )
        assertTrue(analysis.isSamplePlaceholder)
        assertTrue(analysis.clips.isNotEmpty())
        analysis.clips.forEach { clip ->
            val len = clip.endMs - clip.startMs
            assertTrue("Clip duration should be 15-60s", len in 15_000L..60_000L)
            assertTrue(clip.title.contains("Sample"))
        }
    }
}
