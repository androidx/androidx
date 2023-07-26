/*
 * Copyright 2020 The Android Open Source Project
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

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import androidx.camera.core.impl.utils.executor.CameraXExecutors.ioExecutor
import androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture
import androidx.camera.core.impl.utils.futures.Futures.immediateFuture
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast
import androidx.camera.video.internal.BufferProvider
import androidx.camera.video.internal.FakeBufferProvider
import androidx.camera.video.internal.encoder.FakeInputBuffer
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit.NANOSECONDS
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class AudioSourceTest {

    companion object {
        private const val COMMON_TIMEOUT_MS = 1000L
        private const val SAMPLE_RATE = 44100
        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.CAMCORDER
        private const val CHANNEL_COUNT = 1
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTE_BUFFER_CAPACITY = 16
    }

    private val audioSourcesToRelease = mutableListOf<AudioSource>()

    @After
    fun tearDown() {
        for (audioSource in audioSourcesToRelease) {
            audioSource.release()
        }
    }

    @Test
    fun canStartAndStopAudioSource() {
        // Arrange.
        val audioDataProvider = createAudioDataProvider(audioRecordingDelayMillis = 1)
        val audioStream = createAudioStream(audioDataProvider = audioDataProvider)
        val bufferProvider = createBufferProvider()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            bufferProvider = bufferProvider
        )

        // Act.
        audioSource.start()

        // Assert: Audio stream is started.
        audioStream.verifyStartCall(CallTimes(1), COMMON_TIMEOUT_MS)

        // Since the AudioSource's read call might not be synchronized to the AudioStream
        // immediately, waiting for the AudioStream to produce more data than required to ensure
        // that the AudioSource has data to be read.
        val verifyCount = 3
        audioStream.verifyReadCall(CallTimesAtLeast(verifyCount + 1), COMMON_TIMEOUT_MS)

        // Assert: Buffers are continuously written.
        bufferProvider.verifySubmittedBufferCall(
            CallTimesAtLeast(verifyCount),
            COMMON_TIMEOUT_MS
        ) { submittedBuffers ->
            // Assert: Ensure buffers are written correctly.
            for (i in 0 until verifyCount) {
                verifyBufferContentEquals(submittedBuffers[i], audioStream.getAudioDataList()[i])
            }
        }

        // Act.
        audioSource.stop()

        // Assert.
        audioStream.verifyStopCall(CallTimes(1), COMMON_TIMEOUT_MS)
    }

    @Test
    fun release_AudioStreamIsReleased() {
        // Arrange
        val audioStream = createAudioStream()
        val audioSource = createAudioSource(audioStreamFactory = { _, _ -> audioStream })

        // Act.
        audioSource.release()

        // Assert.
        audioStream.verifyReleaseCall(CallTimes(1), COMMON_TIMEOUT_MS)
    }

    @Test
    fun canResetBufferProvider() {
        // Arrange.
        val audioStream = createAudioStream()
        val bufferProvider1 = createBufferProvider()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            bufferProvider = bufferProvider1,
        )
        audioSource.start()
        bufferProvider1.verifySubmittedBufferCall(CallTimesAtLeast(3), COMMON_TIMEOUT_MS)

        // Act.
        val bufferProvider2 = createBufferProvider()
        audioSource.setBufferProvider(bufferProvider2)

        // Assert.
        bufferProvider2.verifySubmittedBufferCall(CallTimesAtLeast(3), COMMON_TIMEOUT_MS)
    }

    @Test
    fun startWhenBufferProviderIsInactive_willNotSendAudio() {
        // Arrange.
        val audioStream = createAudioStream()
        val bufferProvider = createBufferProvider(initState = BufferProvider.State.INACTIVE)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioSource.start()

        // Assert.
        audioSourceCallback.verifyOnSuspendStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
    }

    @Test
    fun bufferProviderBecomeActive_startSendingAudio() {
        // Arrange.
        val audioStream = createAudioStream()
        val bufferProvider = createBufferProvider(initState = BufferProvider.State.INACTIVE)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioSource.start()
        audioSourceCallback.verifyOnSuspendStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
        bufferProvider.setState(BufferProvider.State.ACTIVE)

        // Assert.
        audioSourceCallback.verifyOnSuspendStateChanged(CallTimes(2), COMMON_TIMEOUT_MS) {
            assertThat(it.last()).isFalse()
        }
        audioStream.verifyStartCall(CallTimes(1), COMMON_TIMEOUT_MS)
    }

    @Test
    fun bufferProviderBecomeInactive_stopSendingAudio() {
        // Arrange.
        val audioStream = createAudioStream()
        val bufferProvider = createBufferProvider(initState = BufferProvider.State.ACTIVE)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioSource.start()
        audioStream.verifyStartCall(CallTimes(1), COMMON_TIMEOUT_MS)
        bufferProvider.setState(BufferProvider.State.INACTIVE)

        // Assert.
        audioSourceCallback.verifyOnSuspendStateChanged(CallTimes(1), COMMON_TIMEOUT_MS)
        audioStream.verifyStopCall(CallTimes(1), COMMON_TIMEOUT_MS)
    }

    @Test
    fun canReceiveSilence() {
        // Arrange.
        val audioStream = createAudioStream()
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioStream.isSilenced = true
        audioSource.start()

        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
    }

    @Test
    fun bufferProviderReturnFailedInputBuffer_receiveError() {
        // Arrange.
        val audioStream = createAudioStream()
        val error = RuntimeException()
        val bufferProvider = createBufferProvider(bufferFactory = {
            immediateFailedFuture(error)
        })
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioSource.start()

        // Assert.
        audioSourceCallback.verifyOnError(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isEqualTo(error)
        }
    }

    @Test
    fun failedToStartAudioStream_retryStart() {
        // Arrange.
        val error = AudioStream.AudioStreamException()
        val audioStream = createAudioStream(
            exceptionOnStart = error,
            exceptionOnStartMaxTimes = 1,
        )
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            audioStreamFactory = { _, _ -> audioStream },
            audioSourceCallback = audioSourceCallback,
            retryStartIntervalMs = 200L
        )

        // Act.
        audioSource.start()

        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(2), COMMON_TIMEOUT_MS) {
            assertThat(it[0]).isTrue()
            assertThat(it[1]).isFalse()
        }
    }

    @Test
    fun canMuteAudioSource_beforeStart() {
        // Arrange.
        val bufferProvider = createBufferProvider()
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioSource.mute(true)
        audioSource.start()

        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
        // Assert: Ensure the content is silence.
        val verifyCount = 3
        bufferProvider.verifySubmittedBufferCall(
            CallTimesAtLeast(verifyCount),
            COMMON_TIMEOUT_MS
        ) { submittedBuffers ->
            // Assert: Ensure buffers are written correctly.
            for (i in 0 until verifyCount) {
                verifyBufferIsSilence(submittedBuffers[i].byteBuffer)
            }
        }
    }

    @Test
    fun start_canStartMuted() {
        // Arrange.
        val bufferProvider = createBufferProvider()
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act.
        audioSource.start(true)

        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
        // Assert: Ensure the content is silence.
        val verifyCount = 3
        bufferProvider.verifySubmittedBufferCall(
            CallTimesAtLeast(verifyCount),
            COMMON_TIMEOUT_MS
        ) { submittedBuffers ->
            // Assert: Ensure buffers are written correctly.
            for (i in 0 until verifyCount) {
                verifyBufferIsSilence(submittedBuffers[i].byteBuffer)
            }
        }
    }

    @Test
    fun canSwitchBetweenMuteAndUnMute() {
        // Arrange.
        val bufferProvider = createBufferProvider()
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource = createAudioSource(
            bufferProvider = bufferProvider,
            audioSourceCallback = audioSourceCallback,
        )

        // Act: Default un-mute.
        audioSource.start()
        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isFalse()
        }
        // Act: Mute.
        audioSource.mute(true)
        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(2), COMMON_TIMEOUT_MS) {
            assertThat(it.last()).isTrue()
        }
        // Act: Un-mute.
        audioSource.mute(false)
        // Assert.
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(3), COMMON_TIMEOUT_MS) {
            assertThat(it.last()).isFalse()
        }
    }

    private fun createAudioStream(
        audioDataProvider: (Int) -> FakeAudioStream.AudioData = createAudioDataProvider(),
        exceptionOnStart: AudioStream.AudioStreamException? = null,
        exceptionOnStartMaxTimes: Int = Int.MAX_VALUE,
    ) = FakeAudioStream(
        audioDataProvider,
        exceptionOnStart = exceptionOnStart,
        exceptionOnStartMaxTimes = exceptionOnStartMaxTimes
    )

    @SuppressLint("BanThreadSleep") // Needed to simulate the audio recording delays.
    private fun createAudioDataProvider(
        audioRecordingDelayMillis: Long = 0
    ): (Int) -> FakeAudioStream.AudioData = { index ->
        val byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY).put(0, index.toByte())
        val timestampNs = index.toLong()

        // Simulate the audio recording delays.
        if (audioRecordingDelayMillis > 0) {
            Thread.sleep(audioRecordingDelayMillis)
        }

        FakeAudioStream.AudioData(byteBuffer, timestampNs)
    }

    private fun createBufferProvider(
        initState: BufferProvider.State = BufferProvider.State.ACTIVE,
        bufferFactory: (Int) -> ListenableFuture<FakeInputBuffer> = { _ ->
            val inputBuffer = FakeInputBuffer(BYTE_BUFFER_CAPACITY)
            immediateFuture(inputBuffer)
        }
    ): FakeBufferProvider = FakeBufferProvider(
        state = initState,
        bufferFactory = bufferFactory,
    )

    private fun createAudioSource(
        audioSettings: AudioSettings = createAudioSettings(),
        executor: Executor = ioExecutor(),
        audioStreamFactory: AudioStreamFactory = AudioStreamFactory { _, _ -> createAudioStream() },
        bufferProvider: FakeBufferProvider = createBufferProvider(),
        audioSourceCallback: FakeAudioSourceCallback = createAudioSourceCallback(),
        callbackExecutor: Executor = ioExecutor(),
        retryStartIntervalMs: Long = AudioSource.DEFAULT_START_RETRY_INTERVAL_MS,
    ): AudioSource = AudioSource(
        audioSettings,
        executor,
        /*attributionContext=*/null,
        audioStreamFactory,
        retryStartIntervalMs,
    ).apply {
        setAudioSourceCallback(callbackExecutor, audioSourceCallback)
        setBufferProvider(bufferProvider)
        audioSourcesToRelease.add(this)
    }

    private fun createAudioSettings() = AudioSettings.builder()
        .setAudioSource(AUDIO_SOURCE)
        .setSampleRate(SAMPLE_RATE)
        .setChannelCount(CHANNEL_COUNT)
        .setAudioFormat(AUDIO_FORMAT)
        .build()

    private fun createAudioSourceCallback() = FakeAudioSourceCallback()

    private fun verifyBufferContentEquals(
        inputBuffer: FakeInputBuffer,
        audioData: FakeAudioStream.AudioData
    ) {
        assertThat(inputBuffer.isSubmitted).isTrue()
        assertThat(inputBuffer.byteBuffer).isEqualTo(audioData.byteBuffer.rewind())
        assertThat(inputBuffer.getPresentationTimeUs())
            .isEqualTo(NANOSECONDS.toMicros(audioData.timestampNs))
        assertThat(inputBuffer.isEndOfStream()).isFalse()
    }

    private fun verifyBufferIsSilence(byteBuffer: ByteBuffer) {
        val size = byteBuffer.remaining()
        assertThat(size).isGreaterThan(0)
        val bytes = ByteArray(size)
        val zeroBytes = ByteArray(size)
        byteBuffer.get(bytes)
        assertThat(bytes).isEqualTo(zeroBytes)
    }
}
