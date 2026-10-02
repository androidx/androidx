/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.video.internal.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import java.nio.ByteBuffer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS], shadows = [AudioStreamImplTest.ShadowAudioRecord::class])
class AudioStreamImplTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private val byteBuffer = ByteBuffer.allocateDirect(1024)
    private lateinit var audioStream: AudioStreamImpl
    private val audioStreamCallback =
        object : AudioStream.AudioStreamCallback {
            override fun onSilenceStateChanged(isSilenced: Boolean) {}
        }

    @Before
    fun setUp() {
        audioStream =
            AudioStreamImpl(
                AudioSettings.builder()
                    .setAudioSource(AUDIO_SOURCE)
                    .setCaptureSampleRate(SAMPLE_RATE)
                    .setEncodeSampleRate(SAMPLE_RATE)
                    .setChannelCount(CHANNEL_COUNT)
                    .setAudioFormat(AUDIO_FORMAT)
                    .build(),
                /*attributionContext=*/ null,
            )
    }

    @After
    fun tearDown() {
        if (this::audioStream.isInitialized) {
            audioStream.release()
        }
    }

    @Test
    fun readBeforeStart_throwException() {
        assertThrows(IllegalStateException::class.java) { audioStream.read(byteBuffer) }
    }

    @Test
    fun readAfterStop_throwException() {
        audioStream.start()
        audioStream.stop()
        assertThrows(IllegalStateException::class.java) { audioStream.read(byteBuffer) }
    }

    @Test
    fun startAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) { audioStream.start() }
    }

    @Test
    fun setCallbackAfterStarted_throwException() {
        audioStream.start()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, directExecutor())
        }
    }

    @Test
    fun setCallbackAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, directExecutor())
        }
    }

    @Implements(AudioRecord::class)
    class ShadowAudioRecord {
        companion object {
            @Implementation
            @JvmStatic
            fun getMinBufferSize(sampleRateInHz: Int, channelConfig: Int, audioFormat: Int): Int {
                return 1024
            }
        }

        @Implementation(minSdk = 29)
        fun getActiveRecordingConfiguration(): AudioRecordingConfiguration? {
            return null
        }
    }
}
