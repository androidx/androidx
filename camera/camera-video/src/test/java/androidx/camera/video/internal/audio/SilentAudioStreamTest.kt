/*
 * Copyright 2023 The Android Open Source Project
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
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.impl.utils.executor.CameraXExecutors.ioExecutor
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SilentAudioStreamTest {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val COMMON_TIMEOUT_MS = 1000L
    }

    private lateinit var byteBuffer: ByteBuffer
    private lateinit var audioStream: SilentAudioStream
    private lateinit var audioStreamCallback: FakeAudioStreamCallback

    @Before
    fun setUp() {
        val audioSettings = AudioSettings.builder()
            .setAudioSource(AUDIO_SOURCE)
            .setSampleRate(SAMPLE_RATE)
            .setChannelCount(CHANNEL_COUNT)
            .setAudioFormat(AUDIO_FORMAT)
            .build()
        audioStream = SilentAudioStream(audioSettings)
        audioStreamCallback = FakeAudioStreamCallback()
        audioStream.setCallback(audioStreamCallback, ioExecutor())
        byteBuffer = ByteBuffer.allocate(audioSettings.bytesPerFrame * 5)
    }

    @After
    fun tearDown() {
        if (this::audioStream.isInitialized) {
            audioStream.release()
        }
    }

    @Test
    fun readBeforeStart_throwException() {
        assertThrows(IllegalStateException::class.java) {
            audioStream.read(byteBuffer)
        }
    }

    @Test
    fun readAfterStop_throwException() {
        audioStream.start()
        audioStream.stop()
        assertThrows(IllegalStateException::class.java) {
            audioStream.read(byteBuffer)
        }
    }

    @Test
    fun startAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) {
            audioStream.start()
        }
    }

    @Test
    fun setCallbackAfterStarted_throwException() {
        audioStream.start()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, ioExecutor())
        }
    }

    @Test
    fun setCallbackAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, ioExecutor())
        }
    }

    @Test
    fun canReadSilence() {
        // Act.
        audioStream.start()

        repeat(3) {
            byteBuffer.clear()
            val packetInfo = audioStream.read(byteBuffer)

            // Assert.
            assertThat(packetInfo.sizeInBytes).isGreaterThan(0)
            assertThat(packetInfo.timestampNs).isGreaterThan(0)
            verifySilenceBuffer(byteBuffer)
        }
    }

    @Test
    fun canRestartAudioStream() {
        repeat(2) {
            // Act.
            audioStream.start()

            // Act: read packet info.
            byteBuffer.clear()
            val packetInfo = audioStream.read(byteBuffer)

            // Assert.
            assertThat(packetInfo.sizeInBytes).isGreaterThan(0)
            assertThat(packetInfo.timestampNs).isGreaterThan(0)

            // Act.
            audioStream.stop()

            assertThrows(IllegalStateException::class.java) {
                byteBuffer.clear()
                audioStream.read(byteBuffer)
            }
        }
    }

    @Test
    fun canReceiveOnSilenceStateChanged() {
        // Act.
        audioStream.start()

        // Assert.
        audioStreamCallback.verifyOnSilenceStateChangedCall(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
    }

    private fun verifySilenceBuffer(byteBuffer: ByteBuffer) {
        for (i in 0 until byteBuffer.remaining()) {
            assertThat(byteBuffer.get(byteBuffer.position() + i)).isEqualTo(0)
        }
    }
}
