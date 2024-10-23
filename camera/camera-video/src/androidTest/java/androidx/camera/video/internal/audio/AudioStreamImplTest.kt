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

import android.Manifest
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.Logger
import androidx.camera.core.impl.utils.executor.CameraXExecutors.ioExecutor
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.testing.impl.AudioUtil
import androidx.camera.testing.impl.RequiresDevice
import androidx.camera.testing.impl.mocks.MockConsumer
import androidx.camera.testing.impl.mocks.helpers.ArgumentCaptor
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class AudioStreamImplTest {

    companion object {
        private const val TAG = "AudioStreamImplTest"
        private const val DEFAULT_READ_TIMES = 100
        private val DIFF_LIMIT_FROM_SYSTEM_TIME_NS = TimeUnit.MILLISECONDS.toNanos(500)
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val COMMON_TIMEOUT_MS = 3000L
    }

    @get:Rule
    var mAudioPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    private val byteBuffer = ByteBuffer.allocateDirect(1024)
    private lateinit var audioStream: AudioStreamImpl
    private lateinit var audioStreamCallback: AudioStreamCallback

    @Before
    fun setUp() {
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator()
        )

        assumeTrue(AudioStreamImpl.isSettingsSupported(SAMPLE_RATE, CHANNEL_COUNT, AUDIO_FORMAT))
        assumeTrue(AudioUtil.canStartAudioRecord(AUDIO_SOURCE))

        audioStream =
            AudioStreamImpl(
                AudioSettings.builder()
                    .setAudioSource(AUDIO_SOURCE)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelCount(CHANNEL_COUNT)
                    .setAudioFormat(AUDIO_FORMAT)
                    .build(),
                /*attributionContext=*/ null
            )
        audioStreamCallback = AudioStreamCallback()
        audioStream.setCallback(audioStreamCallback, ioExecutor())
    }

    @After
    fun tearDown() {
        if (this::audioStream.isInitialized) {
            audioStream.release()
        }
    }

    @RequiresDevice // b/264902324
    @Test
    fun readBeforeStart_throwException() {
        assertThrows(IllegalStateException::class.java) { audioStream.read(byteBuffer) }
    }

    @RequiresDevice // b/264902324
    @Test
    fun readAfterStop_throwException() {
        audioStream.start()
        audioStream.stop()
        assertThrows(IllegalStateException::class.java) { audioStream.read(byteBuffer) }
    }

    @RequiresDevice // b/264902324
    @Test
    fun startAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) { audioStream.start() }
    }

    @RequiresDevice // b/264902324
    @Test
    fun setCallbackAfterStarted_throwException() {
        audioStream.start()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, ioExecutor())
        }
    }

    @RequiresDevice // b/264902324
    @Test
    fun setCallbackAfterReleased_throwException() {
        audioStream.release()
        assertThrows(IllegalStateException::class.java) {
            audioStream.setCallback(audioStreamCallback, ioExecutor())
        }
    }

    @RequiresDevice // b/264902324
    @Test
    fun canRead() {
        // Act.
        audioStream.start()

        repeat(3) {
            byteBuffer.clear()
            val packetInfo = audioStream.read(byteBuffer)

            // Assert.
            assertThat(packetInfo.sizeInBytes).isGreaterThan(0)
            assertThat(packetInfo.timestampNs).isGreaterThan(0)
        }
    }

    // See b/301067226 for more information.
    @RequiresDevice
    @Test
    fun canRead_withTimestampDiffToSystemInLimit_whenAudioStreamStartMultipleTimes() {
        repeat(5) {
            Logger.i(TAG, "Starting audio recording, round: $it")

            // Act.
            audioStream.start()

            // Assert.
            readAndVerifyTimestampDiffToSystemMultipleTimes()

            // Act.
            audioStream.stop()
        }
    }

    private fun readAndVerifyTimestampDiffToSystemMultipleTimes(times: Int = DEFAULT_READ_TIMES) {
        repeat(times) {
            byteBuffer.clear()
            val packetInfo = audioStream.read(byteBuffer)

            // Assert.
            val timestampDiff = abs(packetInfo.timestampNs - System.nanoTime())
            assertThat(timestampDiff).isLessThan(DIFF_LIMIT_FROM_SYSTEM_TIME_NS)
        }
    }

    @RequiresDevice // b/264902324
    @Test
    fun canRestartAudioStream() {
        repeat(2) {
            // Act.
            audioStream.start()

            // Act: read packet.
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

    @RequiresDevice // b/264902324
    @SdkSuppress(minSdkVersion = 21, maxSdkVersion = 28)
    @Test
    fun canReceiveOnSilenceStateChangedAfterStarted_belowApi29() {
        // Act.
        audioStream.start()

        // Assert: Initial isSilenced is always false.
        audioStreamCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.first()).isFalse()
        }
    }

    @RequiresDevice // b/264902324
    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun canReceiveOnSilenceStateChangedAfterStarted() {
        // Act.
        audioStream.start()

        // Assert: Do not check isSilenced value since it depends on real status.
        audioStreamCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS)
    }

    private class AudioStreamCallback : AudioStream.AudioStreamCallback {
        private val onSilencedCallback = MockConsumer<Boolean>()

        override fun onSilenceStateChanged(isSilenced: Boolean) {
            onSilencedCallback.accept(isSilenced)
        }

        fun verifyOnSilenceStateChanged(
            callTimes: CallTimes,
            timeoutMs: Long = MockConsumer.NO_TIMEOUT,
            inOder: Boolean = false,
            onSilenceStateChanged: ((List<Boolean>) -> Unit)? = null,
        ) {
            val captor = onSilenceStateChanged?.let { ArgumentCaptor<Boolean>() }
            onSilencedCallback.verifyAcceptCall(
                java.lang.Boolean::class.java,
                inOder,
                timeoutMs,
                callTimes,
                captor
            )
            onSilenceStateChanged?.invoke(captor!!.allValues)
        }
    }
}
