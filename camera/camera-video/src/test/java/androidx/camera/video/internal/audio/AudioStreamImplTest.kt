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
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor
import androidx.camera.video.internal.compat.quirk.AudioTimestampFramePositionIncorrectQuirk
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.util.concurrent.Executor
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
import org.robolectric.shadow.api.Shadow
import org.robolectric.util.ReflectionHelpers

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
        audioStream = createAudioStream(Quirks(emptyList()))
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

    @Test
    fun stopWithAudioTimestampQuirk_recreatesAudioRecord() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        val oldAudioRecord = getCurrentAudioRecord()

        audioStream.stop()

        val newAudioRecord = getCurrentAudioRecord()
        assertThat(newAudioRecord).isNotSameInstanceAs(oldAudioRecord)
        assertThat(shadowAudioRecord(oldAudioRecord).isReleased).isTrue()
        assertThat(shadowAudioRecord(newAudioRecord).isReleased).isFalse()
    }

    @Config(minSdk = 29)
    @Test
    fun stopWithAudioTimestampQuirk_movesRecordingCallbackToRecreatedAudioRecord() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        val oldAudioRecord = getCurrentAudioRecord()
        assertThat(shadowAudioRecord(oldAudioRecord).registeredCallbacks).hasSize(1)

        audioStream.stop()

        val newAudioRecord = getCurrentAudioRecord()
        assertThat(shadowAudioRecord(oldAudioRecord).registeredCallbacks).isEmpty()
        assertThat(shadowAudioRecord(newAudioRecord).registeredCallbacks).hasSize(1)
    }

    @Config(minSdk = 29)
    @Test
    fun stopWithAudioTimestampQuirk_noCallback_doesNotRegisterRecordingCallback() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.start()

        audioStream.stop()

        assertThat(shadowAudioRecord(getCurrentAudioRecord()).registeredCallbacks).isEmpty()
    }

    @Test
    fun releaseAfterStopWithAudioTimestampQuirk_releasesAudioRecord() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        audioStream.stop()
        val audioRecord = getCurrentAudioRecord()

        audioStream.release()

        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks).isEmpty()
    }

    @Test
    fun startStopAgainWithAudioTimestampQuirk_keepsRecordingCallbackRegistered() {
        useAudioStreamWithAudioTimestampQuirk()
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        audioStream.stop()
        audioStream.start()
        audioStream.stop()

        val audioRecord = getCurrentAudioRecord()
        val expectedCallbackCount = if (Build.VERSION.SDK_INT >= 29) 1 else 0
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks)
            .hasSize(expectedCallbackCount)
        audioStream.release()
        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
    }

    @Test
    fun stopWithoutAudioTimestampQuirk_keepsAudioRecordAndRecordingCallback() {
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.start()
        val audioRecord = getCurrentAudioRecord()

        audioStream.stop()

        assertThat(getCurrentAudioRecord()).isSameInstanceAs(audioRecord)
        assertThat(shadowAudioRecord(audioRecord).isReleased).isFalse()
        val expectedCallbackCount = if (Build.VERSION.SDK_INT >= 29) 1 else 0
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks)
            .hasSize(expectedCallbackCount)

        audioStream.release()

        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
        assertThat(shadowAudioRecord(audioRecord).registeredCallbacks).isEmpty()
    }

    @Config(minSdk = 29)
    @Test
    fun releaseAfterCallbackCleared_releasesAudioRecord() {
        audioStream.setCallback(audioStreamCallback, directExecutor())
        audioStream.setCallback(null, null)
        val audioRecord = getCurrentAudioRecord()

        audioStream.release()

        assertThat(shadowAudioRecord(audioRecord).isReleased).isTrue()
    }

    private fun createAudioStream(quirks: Quirks): AudioStreamImpl =
        AudioStreamImpl(
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(SAMPLE_RATE)
                .setEncodeSampleRate(SAMPLE_RATE)
                .setChannelCount(CHANNEL_COUNT)
                .setAudioFormat(AUDIO_FORMAT)
                .build(),
            /*attributionContext=*/ null,
            quirks,
        )

    /** Replaces [audioStream] with one created with [AudioTimestampFramePositionIncorrectQuirk]. */
    private fun useAudioStreamWithAudioTimestampQuirk() {
        audioStream.release()
        audioStream = createAudioStream(Quirks(listOf(AudioTimestampFramePositionIncorrectQuirk())))
    }

    private fun getCurrentAudioRecord(): AudioRecord =
        ReflectionHelpers.getField(audioStream, "mAudioRecord")

    private fun shadowAudioRecord(audioRecord: AudioRecord): ShadowAudioRecord =
        Shadow.extract(audioRecord)

    @Implements(AudioRecord::class)
    class ShadowAudioRecord {
        companion object {
            @Implementation
            @JvmStatic
            fun getMinBufferSize(sampleRateInHz: Int, channelConfig: Int, audioFormat: Int): Int {
                return 1024
            }
        }

        /** The registered recording callbacks. */
        val registeredCallbacks = mutableSetOf<AudioManager.AudioRecordingCallback>()

        /** Whether [AudioRecord.release] has been called. */
        var isReleased = false
            private set

        @Implementation(minSdk = 29)
        fun getActiveRecordingConfiguration(): AudioRecordingConfiguration? {
            return null
        }

        @Implementation(minSdk = 29)
        fun registerAudioRecordingCallback(
            executor: Executor,
            callback: AudioManager.AudioRecordingCallback,
        ) {
            require(registeredCallbacks.add(callback)) {
                "AudioRecordingCallback already registered"
            }
        }

        /** Throws [IllegalArgumentException] as the platform does for unknown callbacks. */
        @Implementation(minSdk = 29)
        fun unregisterAudioRecordingCallback(callback: AudioManager.AudioRecordingCallback) {
            require(registeredCallbacks.remove(callback)) {
                "AudioRecordingCallback was not registered"
            }
        }

        /** Only records the call. Unlike the platform, it doesn't change [AudioRecord.getState]. */
        @Implementation
        fun release() {
            isReleased = true
        }
    }
}
