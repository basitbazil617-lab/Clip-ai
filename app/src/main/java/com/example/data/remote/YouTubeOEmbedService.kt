package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class YouTubeOEmbedResponse(
    @Json(name = "title") val title: String = "",
    @Json(name = "author_name") val authorName: String = "",
    @Json(name = "author_url") val authorUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "provider_name") val providerName: String = "YouTube"
)

interface YouTubeOEmbedApi {
    @GET("oembed")
    suspend fun getVideoInfo(
        @Query("url") videoUrl: String,
        @Query("format") format: String = "json"
    ): YouTubeOEmbedResponse
}

data class YouTubeValidationResult(
    val isValid: Boolean,
    val videoId: String?,
    val canonicalUrl: String?,
    val errorMessage: String?
)

data class AuthorizedStreamResult(
    val localCachedFile: File?,
    val errorMessage: String?
)

object YouTubeUrlValidator {
    private val youtubeRegex = Regex(
        "^(https?://)?(www\\.|m\\.)?(youtube\\.com/(watch\\?.*v=|shorts/|live/|embed/)|youtu\\.be/)([a-zA-Z0-9_-]{11}).*$"
    )

    fun validate(rawUrl: String): YouTubeValidationResult {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return YouTubeValidationResult(false, null, null, "Paste a YouTube video URL to continue.")
        }
        val match = youtubeRegex.find(trimmed)
            ?: return YouTubeValidationResult(
                isValid = false,
                videoId = null,
                canonicalUrl = null,
                errorMessage = "Invalid YouTube URL. Supported formats: youtube.com/watch?v=..., youtu.be/..., youtube.com/shorts/..."
            )
        val videoId = match.groupValues[5]
        return YouTubeValidationResult(
            isValid = true,
            videoId = videoId,
            canonicalUrl = "https://www.youtube.com/watch?v=$videoId",
            errorMessage = null
        )
    }
}

class YouTubeMetadataRepository {
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val oEmbedApi: YouTubeOEmbedApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.youtube.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(YouTubeOEmbedApi::class.java)
    }

    suspend fun fetchPublicMetadata(canonicalUrl: String): Result<YouTubeOEmbedResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(oEmbedApi.getVideoInfo(canonicalUrl))
            } catch (e: Exception) {
                Result.failure(
                    IOException(
                        e.localizedMessage ?: "Could not reach YouTube oEmbed metadata endpoint.",
                        e
                    )
                )
            }
        }

    /**
     * Connects to a user-provided authorized processing backend endpoint (e.g., creator's own
     * signed studio server) to fetch an owned MP4 file without bypassing YouTube security.
     * Expected endpoint contract:
     *   POST <backendUrl> with JSON {"youtubeUrl": "...", "videoId": "...", "permissionAttested": true}
     *   Returns either direct video/mp4 bytes OR JSON {"mp4Url": "https://..."}.
     */
    suspend fun fetchFromAuthorizedBackend(
        backendEndpointUrl: String,
        canonicalYoutubeUrl: String,
        videoId: String,
        destinationFile: File,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = backendEndpointUrl.trim()
            if (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Authorized backend URL must start with https:// or http://")
                )
            }
            val payload = JSONObject().apply {
                put("youtubeUrl", canonicalYoutubeUrl)
                put("videoId", videoId)
                put("permissionAttested", true)
            }.toString()

            val request = Request.Builder()
                .url(cleanUrl)
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("Authorized backend returned HTTP ${response.code}: ${response.message}")
                    )
                }
                val contentType = response.header("Content-Type").orEmpty()
                val body = response.body ?: return@withContext Result.failure(
                    IOException("Authorized backend returned empty response body.")
                )
                if (contentType.contains("application/json")) {
                    val json = JSONObject(body.string())
                    val mp4Url = json.optString("mp4Url", "").ifBlank {
                        json.optString("downloadUrl", "")
                    }
                    if (mp4Url.isBlank()) {
                        val err = json.optString("error", "Backend JSON did not include mp4Url.")
                        return@withContext Result.failure(IOException(err))
                    }
                    downloadDirectMp4(mp4Url, destinationFile, onProgress)
                } else {
                    saveResponseBodyToFile(body, destinationFile, onProgress)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun downloadDirectMp4(
        mp4Url: String,
        destinationFile: File,
        onProgress: (Int) -> Unit
    ): Result<File> {
        val request = Request.Builder().url(mp4Url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return Result.failure(
                    IOException("Media stream returned HTTP ${response.code}: ${response.message}")
                )
            }
            val body = response.body ?: return Result.failure(IOException("Empty media stream."))
            return saveResponseBodyToFile(body, destinationFile, onProgress)
        }
    }

    private fun saveResponseBodyToFile(
        body: okhttp3.ResponseBody,
        destinationFile: File,
        onProgress: (Int) -> Unit
    ): Result<File> {
        val total = body.contentLength()
        destinationFile.parentFile?.mkdirs()
        body.byteStream().use { input ->
            destinationFile.outputStream().use { output ->
                val buffer = ByteArray(8192)
                var downloaded = 0L
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                    downloaded += read
                    if (total > 0L) {
                        val pct = ((downloaded * 100L) / total).toInt().coerceIn(0, 99)
                        onProgress(pct)
                    }
                }
                output.flush()
            }
        }
        if (destinationFile.length() == 0L) {
            return Result.failure(IOException("Downloaded file is empty (0 bytes)."))
        }
        onProgress(100)
        return Result.success(destinationFile)
    }
}
