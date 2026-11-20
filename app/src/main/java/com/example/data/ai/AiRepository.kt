package com.example.data.ai

import kotlinx.coroutines.delay

data class DetectedSceneClip(
    val title: String,
    val category: String, // Funny Moment, Reaction, Highlight, Action Scene
    val startMs: Long,
    val endMs: Long,
    val subtitleText: String,
    val headerOverlayText: String,
    val recommendedEffect: String
)

data class AiSceneAnalysisResult(
    val clips: List<DetectedSceneClip>,
    val isSamplePlaceholder: Boolean,
    val noticeMessage: String
)

data class AiSeoResult(
    val titles: List<String>,
    val description: String,
    val hashtags: List<String>,
    val searchTags: List<String>,
    val pinnedComment: String,
    val isSamplePlaceholder: Boolean,
    val noticeMessage: String
)

interface AiRepository {
    suspend fun analyzeScenesForClips(
        videoTitle: String,
        durationMs: Long,
        contextNotes: String
    ): AiSceneAnalysisResult

    suspend fun generateYouTubeSeo(
        clipTopic: String,
        vibe: String,
        durationSec: Int
    ): AiSeoResult
}

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * Always surfaces a clear notice and clearly labeled sample outputs instead of
 * bypassing Firebase or embedding direct API keys.
 */
class PlaceholderAiRepository : AiRepository {

    companion object {
        const val FIREBASE_REQUIRED_NOTICE =
            "Sample AI output — Live Gemini scene detection & SEO require enabling Firebase AI Logic."
    }

    override suspend fun analyzeScenesForClips(
        videoTitle: String,
        durationMs: Long,
        contextNotes: String
    ): AiSceneAnalysisResult {
        delay(450L)
        val safeDuration = durationMs.coerceAtLeast(16_000L)
        val cleanTitle = videoTitle.ifBlank { "Creator Gameplay Highlight" }.take(38)

        val candidates = listOf(
            Triple("Funny Moment", "WAIT FOR THE ENDING!", "NEON_VICE"),
            Triple("Reaction", "NO WAY THAT JUST HAPPENED", "HIGH_CONTRAST"),
            Triple("Highlight", "CLEANEST CLUTCH IN VICE CITY", "CINEMATIC_ZOOM"),
            Triple("Action Scene", "FULL SPEED CHASE MODE", "FADE_INTRO")
        )

        val generated = mutableListOf<DetectedSceneClip>()
        val targetClipCount = when {
            safeDuration < 35_000L -> 1
            safeDuration < 75_000L -> 2
            safeDuration < 120_000L -> 3
            else -> 4
        }

        val segmentSpan = safeDuration / targetClipCount
        for (i in 0 until targetClipCount) {
            val (category, caption, effect) = candidates[i % candidates.size]
            val start = (i * segmentSpan).coerceAtMost((safeDuration - 15_000L).coerceAtLeast(0L))
            // Enforce 15-60s clip range whenever source duration allows
            val rawLength = (segmentSpan - 2_000L).coerceIn(15_000L, 58_000L)
            val end = (start + rawLength).coerceAtMost(safeDuration)
            val finalStart = if (end - start < 15_000L && safeDuration >= 15_000L) {
                (end - 15_000L).coerceAtLeast(0L)
            } else {
                start
            }
            generated += DetectedSceneClip(
                title = "[Sample] $category #${i + 1}: $cleanTitle",
                category = category,
                startMs = finalStart,
                endMs = end,
                subtitleText = "[Sample Caption] $caption",
                headerOverlayText = cleanTitle.uppercase().take(24),
                recommendedEffect = effect
            )
        }

        return AiSceneAnalysisResult(
            clips = generated,
            isSamplePlaceholder = true,
            noticeMessage = FIREBASE_REQUIRED_NOTICE
        )
    }

    override suspend fun generateYouTubeSeo(
        clipTopic: String,
        vibe: String,
        durationSec: Int
    ): AiSeoResult {
        delay(450L)
        val topic = clipTopic.trim().ifBlank { "Midnight Neon Chase & Reaction" }
        val shortTopic = topic.take(42)

        val titles = when (vibe) {
            "Funny Fail" -> listOf(
                "[Sample] Wait Until 0:${durationSec.coerceIn(15, 59)}... $shortTopic 😂",
                "[Sample] Nobody Expected This $shortTopic Fail 💀",
                "[Sample] Instant Karma in $shortTopic #Shorts"
            )
            "Pro Tips" -> listOf(
                "[Sample] Master $shortTopic in ${durationSec}s ⚡",
                "[Sample] The Secret Trick for $shortTopic 🎯",
                "[Sample] Stop Doing This Wrong in $shortTopic"
            )
            "Hype Trailer" -> listOf(
                "[Sample] Vice Nights: $shortTopic 🔥",
                "[Sample] Unreal Moment — $shortTopic (9:16)",
                "[Sample] Pure Cinema: $shortTopic ✨"
            )
            else -> listOf(
                "[Sample] Insane $shortTopic Reaction 🤯",
                "[Sample] Top Highlight: $shortTopic 🔥",
                "[Sample] How Did That Happen?! $shortTopic"
            )
        }

        val description = """
            [Sample SEO Description — Connect Firebase AI Logic for Live Gemini Generation]
            
            Watch this $durationSec-second vertical highlight featuring $topic ($vibe edition)!
            Edited in 9:16 Shorts format with custom neon grading, tight pacing, and synced captions.
            
            Chapters:
            0:00 - Hook & Setup
            0:05 - Peak Reaction Moment
            0:${(durationSec - 4).coerceAtLeast(10)} - Final Payoff
            
            Drop a like if you want Part 2 and tell us your favorite moment below!
        """.trimIndent()

        val hashtags = listOf(
            "#Shorts",
            "#GamingClips",
            "#ViceVibes",
            "#ViralShorts",
            "#ClipAI",
            "#GamingHighlights",
            "#${vibe.replace(" ", "")}"
        )

        val searchTags = listOf(
            topic.lowercase(),
            "$vibe shorts",
            "vertical gaming clips",
            "youtube shorts highlights",
            "funny gaming reactions",
            "9:16 gaming edit",
            "neon city gaming clip",
            "viral shorts moments"
        )

        val pinnedComment =
            "[Sample Pinned Comment] Rate this $vibe clip from 1 to 10! What moment caught you off guard? 👇🔥"

        return AiSeoResult(
            titles = titles,
            description = description,
            hashtags = hashtags,
            searchTags = searchTags,
            pinnedComment = pinnedComment,
            isSamplePlaceholder = true,
            noticeMessage = FIREBASE_REQUIRED_NOTICE
        )
    }
}
