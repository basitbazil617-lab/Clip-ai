package com.example.media

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Real-time 16-bit PCM audio gain processor used by both ExoPlayer preview and Media3 Transformer export.
 */
@UnstableApi
class VolumeAudioProcessor(
    @Volatile var volume: Float = 1.0f
) : BaseAudioProcessor() {

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val gain = volume.coerceIn(0f, 2.0f)
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        if (gain == 1.0f) {
            outputBuffer.put(inputBuffer)
        } else if (gain == 0.0f) {
            inputBuffer.position(inputBuffer.limit())
            for (i in 0 until remaining) {
                outputBuffer.put(0)
            }
        } else {
            while (inputBuffer.remaining() >= 2) {
                val sample = inputBuffer.short.toInt()
                val scaled = (sample * gain).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                outputBuffer.putShort(scaled.toShort())
            }
            if (inputBuffer.hasRemaining()) {
                outputBuffer.put(inputBuffer.get())
            }
        }
        outputBuffer.flip()
    }
}
