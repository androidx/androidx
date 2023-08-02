/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.integration.avsync.model

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.annotation.VisibleForTesting
import androidx.camera.core.Logger
import androidx.core.util.Preconditions.checkArgument
import androidx.core.util.Preconditions.checkArgumentNonnegative
import kotlin.math.pow
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AudioGenerator"
private const val DEFAULT_SAMPLE_RATE: Int = 44100
private const val SAMPLE_WIDTH: Int = 2
private const val MAGNITUDE = 0.5
private const val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
private const val CHANNEL = AudioFormat.CHANNEL_OUT_MONO

class AudioGenerator(private val isEnabled: Boolean = true) {

    @VisibleForTesting
    var audioTrack: AudioTrack? = null

    fun start() {
        if (isEnabled) {
            startInternal()
        } else {
            Logger.i(TAG, "Will not start audio generation, since AudioGenerator is disabled.")
        }
    }

    private fun startInternal() {
        Logger.i(TAG, "start audio generation")
        audioTrack!!.play()
    }

    fun stop() {
        if (isEnabled) {
            stopInternal()
        } else {
            Logger.i(TAG, "Will not stop audio generation, since AudioGenerator is disabled.")
        }
    }

    private fun stopInternal() {
        Logger.i(TAG, "stop audio generation")
        Logger.i(TAG, "playState before stopped: ${audioTrack!!.playState}")
        Logger.i(TAG, "playbackHeadPosition before stopped: ${audioTrack!!.playbackHeadPosition}")
        audioTrack!!.stop()
        audioTrack!!.reloadStaticData()
    }

    suspend fun initial(
        context: Context,
        frequency: Int,
        beepLengthInSec: Double,
    ): Boolean {
        checkArgumentNonnegative(frequency, "The input frequency should not be negative.")
        checkArgument(beepLengthInSec >= 0, "The beep length should not be negative.")

        return if (isEnabled) {
            initAudioTrackInternal(context, frequency, beepLengthInSec)
        } else {
            Logger.i(TAG, "Will not initial audio track, since AudioGenerator is disabled.")
            true
        }
    }

    private suspend fun initAudioTrackInternal(
        context: Context,
        frequency: Int,
        beepLengthInSec: Double,
    ): Boolean {
        val sampleRate = getOutputSampleRate(context)
        val samples = generateSineSamples(frequency, beepLengthInSec, SAMPLE_WIDTH, sampleRate)
        val bufferSize = samples.size

        Logger.i(TAG, "initAudioTrack with sample rate: $sampleRate")
        Logger.i(TAG, "initAudioTrack with beep frequency: $frequency")
        Logger.i(TAG, "initAudioTrack with buffer size: $bufferSize")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(ENCODING)
            .setChannelMask(CHANNEL)
            .build()

        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            bufferSize,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack!!.write(samples, 0, samples.size)

        return true
    }

    private fun getOutputSampleRate(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRate: String? = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)

        return sampleRate?.toInt() ?: DEFAULT_SAMPLE_RATE
    }

    @VisibleForTesting
    suspend fun generateSineSamples(
        frequency: Int,
        beepLengthInSec: Double,
        sampleWidth: Int,
        sampleRate: Int
    ): ByteArray {
        val waveData = generateSineData(frequency, beepLengthInSec, sampleRate)
        val samples = toSamples(waveData, sampleWidth)

        return samples.toByteArray()
    }

    /**
     * magnitude is expected to be from 0 to 1
     */
    @VisibleForTesting
    suspend fun generateSineData(
        frequency: Int,
        lengthInSec: Double,
        sampleRate: Int,
        magnitude: Double = MAGNITUDE
    ): List<Double> = withContext(Dispatchers.Default) {
        val n = (lengthInSec * sampleRate).toInt()
        val angularFrequency = 2.0 * Math.PI * frequency

        val res = mutableListOf<Double>()
        for (i in 0 until n) {
            val x = i * lengthInSec / n
            val y = magnitude * sin(angularFrequency * x)
            res.add(y)
        }

        res
    }

    @VisibleForTesting
    suspend fun toSamples(
        data: List<Double>,
        sampleWidth: Int
    ): List<Byte> = withContext(Dispatchers.Default) {
        val scaleFactor = 2.toDouble().pow(8 * sampleWidth - 1) - 1

        data.flatMap {
            (it * scaleFactor).toInt().toBytes(sampleWidth)
        }
    }

    @VisibleForTesting
    fun Int.toBytes(sampleWidth: Int): List<Byte> {
        val res = mutableListOf<Byte>()
        for (i in 0 until sampleWidth) {
            val byteValue = (this shr (8 * i)).toByte()
            res.add(byteValue)
        }

        return res
    }
}
