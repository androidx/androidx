/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.integration.macrobenchmark.target

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.M)
class AudioActivity() : AppCompatActivity() {
    private lateinit var thread: Thread

    @Synchronized get
    @Synchronized set
    private var finished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio)

        findViewById<TextView>(R.id.audioTextNotice).setText(R.string.audio_notice)

        val sampleRateHz = 22050
        val bufferDurationMs = 250
        val buffer = generateBuffer(bufferDurationMs, 500, sampleRateHz)

        // plays beeps continuously until activity is destroyed
        thread = thread {
            var format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRateHz)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            var attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(buffer.size * 2)
                .build()

            track.play()

            while (!finished) {
                var currentTime = System.currentTimeMillis()
                track.write(buffer, 0, buffer.size)

                // sleep twice as buffer duration to generate pauses
                val targetTime = currentTime + bufferDurationMs * 2
                Thread.sleep(targetTime - System.currentTimeMillis())
            }

            track.stop()
        }
    }

    override fun onDestroy() {
        finished = true
        thread.join()

        super.onDestroy()
    }

    private fun generateBuffer(durationMs: Int, frequency: Int, sampleRateHz: Int): ShortArray {
        val numSamples = durationMs * sampleRateHz / 1000

        val buffer = ShortArray(numSamples)
        for (i in 0 until numSamples) {
            val sample = sin(2 * Math.PI * i / (sampleRateHz / frequency)) * 0.1
            buffer[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }

        return buffer
    }
}