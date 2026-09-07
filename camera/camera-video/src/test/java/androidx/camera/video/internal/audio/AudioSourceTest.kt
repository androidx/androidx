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

import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.camera.core.impl.utils.executor.CameraXExecutors.ioExecutor
import androidx.camera.core.impl.utils.futures.Futures.immediateFailedFuture
import androidx.camera.core.impl.utils.futures.Futures.immediateFuture
import androidx.camera.testing.impl.mocks.helpers.CallTimes
import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast
import androidx.camera.video.AudioProcessor
import androidx.camera.video.PassthroughAudioProcessor
import androidx.camera.video.internal.BufferProvider
import androidx.camera.video.internal.FakeBufferProvider
import androidx.camera.video.internal.encoder.FakeInputBuffer
import com.google.common.truth.Truth.assertThat
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.NANOSECONDS
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.ALL_SDKS])
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
        val audioStream = createAudioStream()
        val bufferProvider = createBufferProvider()
        val audioSource =
            createAudioSource(
                audioStreamFactory = { _, _ -> audioStream },
                bufferProvider = bufferProvider,
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
            COMMON_TIMEOUT_MS,
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
        val audioSource =
            createAudioSource(
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
        val audioSource =
            createAudioSource(
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
        val audioSource =
            createAudioSource(
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
        val audioSource =
            createAudioSource(
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
        val audioSource =
            createAudioSource(
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
        val bufferProvider = createBufferProvider(bufferFactory = { immediateFailedFuture(error) })
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
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
        val audioStream = createAudioStream(exceptionOnStart = error, exceptionOnStartMaxTimes = 1)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
                audioStreamFactory = { _, _ -> audioStream },
                audioSourceCallback = audioSourceCallback,
                retryStartIntervalMs = 200L,
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
    fun failedToStartAudioStream_stopWillStopSilentAudioStream() {
        // Arrange.
        val error = AudioStream.AudioStreamException()
        val audioStream = createAudioStream(exceptionOnStart = error)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
                audioStreamFactory = { _, _ -> audioStream },
                audioSourceCallback = audioSourceCallback,
            )

        // Act: Start AudioSource (which fails to start mAudioStream and falls back to
        // mSilentAudioStream).
        audioSource.start()

        // Assert: Silence state is triggered (indicating SilentAudioStream was started).
        audioSourceCallback.verifyOnSilenceStateChanged(CallTimes(1), COMMON_TIMEOUT_MS) {
            assertThat(it.single()).isTrue()
        }
        assertThat(audioSource.mSilentAudioStream.isStarted).isTrue()

        // Act: Stop AudioSource.
        audioSource.stop()

        // Wait for stop() to complete on AudioSource's sequential executor.
        val latch = CountDownLatch(1)
        audioSource.mExecutor.execute { latch.countDown() }
        assertThat(latch.await(COMMON_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()

        // Assert: SilentAudioStream is stopped.
        assertThat(audioSource.mSilentAudioStream.isStarted).isFalse()
    }

    @Test
    fun canMuteAudioSource_beforeStart() {
        // Arrange.
        val bufferProvider = createBufferProvider()
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
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
            COMMON_TIMEOUT_MS,
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
        val audioSource =
            createAudioSource(
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
            COMMON_TIMEOUT_MS,
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
        val audioSource =
            createAudioSource(
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

    @Test
    fun audioSource_amplitudeCallback_throttledBy200Milliseconds() {
        val totalValidPackets = 9
        val audioDataProvider: (Int) -> FakeAudioStream.AudioData = { index ->
            val buf = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY).order(ByteOrder.nativeOrder())
            // 50ms interval between packets up to index 8 (400ms).
            // Cap timestamp at 400ms for subsequent packets so exactly 2 callbacks fire.
            val effectiveIndex = minOf(index, totalValidPackets - 1)
            val timestampNs = TimeUnit.MILLISECONDS.toNanos(50L * effectiveIndex)
            FakeAudioStream.AudioData(buf, timestampNs)
        }

        val audioStream = createAudioStream(audioDataProvider = audioDataProvider)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
                audioStreamFactory = { _, _ -> audioStream },
                audioSourceCallback = audioSourceCallback,
            )

        audioSource.start()

        // Wait until all valid packets have been read
        audioStream.verifyReadCall(CallTimesAtLeast(totalValidPackets), COMMON_TIMEOUT_MS)

        // Only 2 callbacks should be triggered (at 200ms and 400ms) due to 200ms throttling
        audioSourceCallback.verifyOnAmplitudeValue(CallTimes(2), COMMON_TIMEOUT_MS)

        audioSource.stop()
    }

    @Test
    fun audioSource_amplitudeCallback_calculatesCorrectMaxAmplitude() {
        val testAmplitudeValue = (Short.MAX_VALUE / 2).toShort()
        val audioDataProvider: (Int) -> FakeAudioStream.AudioData = { index ->
            val buf = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY).order(ByteOrder.nativeOrder())
            val shortBuf = buf.asShortBuffer()
            while (shortBuf.hasRemaining()) {
                shortBuf.put(testAmplitudeValue)
            }
            // timestamp >= 200ms to trigger amplitude callback on first packet
            FakeAudioStream.AudioData(buf, TimeUnit.MILLISECONDS.toNanos(200L * (index + 1)))
        }

        val audioStream = createAudioStream(audioDataProvider = audioDataProvider)
        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
                audioStreamFactory = { _, _ -> audioStream },
                audioSourceCallback = audioSourceCallback,
            )

        audioSource.start()

        audioStream.verifyReadCall(CallTimesAtLeast(1), COMMON_TIMEOUT_MS)
        audioSourceCallback.verifyOnAmplitudeValue(CallTimesAtLeast(1), COMMON_TIMEOUT_MS) {
            amplitudes ->
            assertThat(amplitudes.first()).isWithin(0.01).of(0.5)
        }

        audioSource.stop()
    }

    @Test
    fun audioProcessor_withAudioProcessors_modifiesAudioData() {
        val gainProcessor = createAudioProcessor { inputBuffer ->
            val remaining = inputBuffer.remaining()
            val outputBuffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
            val shortIn = inputBuffer.asShortBuffer()
            val shortOut = outputBuffer.asShortBuffer()
            while (shortIn.hasRemaining()) {
                shortOut.put((shortIn.get() * 2).toShort())
            }
            outputBuffer.position(0)
            outputBuffer.limit(remaining)
            outputBuffer
        }

        val audioDataProvider: (Int) -> FakeAudioStream.AudioData = { index ->
            val buf = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY).order(ByteOrder.nativeOrder())
            val shortBuf = buf.asShortBuffer()
            repeat(BYTE_BUFFER_CAPACITY / 2) {
                shortBuf.put((index + 1).toShort())
            }
            FakeAudioStream.AudioData(buf, index.toLong())
        }

        val audioStream = createAudioStream(audioDataProvider = audioDataProvider)
        val bufferProvider = createBufferProvider()
        val audioSource =
            createAudioSource(
                audioProcessors = listOf(gainProcessor),
                audioStreamFactory = { _, _ -> audioStream },
                bufferProvider = bufferProvider,
            )

        audioSource.start()

        val verifyCount = 3
        audioStream.verifyReadCall(CallTimesAtLeast(verifyCount + 1), COMMON_TIMEOUT_MS)

        bufferProvider.verifySubmittedBufferCall(
            CallTimesAtLeast(verifyCount),
            COMMON_TIMEOUT_MS,
        ) { submittedBuffers ->
            for (i in 0 until verifyCount) {
                val inputBuf = submittedBuffers[i]
                assertThat(inputBuf.isSubmitted).isTrue()
                val shortOut = inputBuf.byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer()
                val expectedValue = ((i + 1) * 2).toShort()
                while (shortOut.hasRemaining()) {
                    assertThat(shortOut.get()).isEqualTo(expectedValue)
                }
            }
        }
    }

    @Test
    fun audioProcessor_withPassthroughAudioProcessor_observesAudioData() {
        val observedBuffers = mutableListOf<ByteArray>()
        val passthroughProcessor =
            object : PassthroughAudioProcessor() {
                override fun onAudioBuffer(audioBuffer: ByteBuffer) {
                    val bytes = ByteArray(audioBuffer.remaining())
                    audioBuffer.get(bytes)
                    observedBuffers.add(bytes)
                }
            }

        val audioDataProvider = createAudioDataProvider()
        val audioStream = createAudioStream(audioDataProvider = audioDataProvider)
        val bufferProvider = createBufferProvider()
        val audioSource =
            createAudioSource(
                audioProcessors = listOf(passthroughProcessor),
                audioStreamFactory = { _, _ -> audioStream },
                bufferProvider = bufferProvider,
            )

        audioSource.start()

        val verifyCount = 3
        audioStream.verifyReadCall(CallTimesAtLeast(verifyCount + 1), COMMON_TIMEOUT_MS)

        bufferProvider.verifySubmittedBufferCall(
            CallTimesAtLeast(verifyCount),
            COMMON_TIMEOUT_MS,
        ) { submittedBuffers ->
            assertThat(observedBuffers.size).isAtLeast(verifyCount)
            for (i in 0 until verifyCount) {
                val inputBuf = submittedBuffers[i]
                assertThat(inputBuf.isSubmitted).isTrue()
                assertThat(inputBuf.byteBuffer.get(0)).isEqualTo(observedBuffers[i][0])
                assertThat(observedBuffers[i].size).isEqualTo(BYTE_BUFFER_CAPACITY)
            }
        }
    }

    @Test
    fun audioProcessor_unsupportedAudioFormatException_throwsAudioSourceAccessException() {
        val failingProcessor =
            createAudioProcessor(
                onConfigure = { audioFormat ->
                    throw AudioProcessor.UnsupportedAudioFormatException(
                        "Unsupported configuration",
                        audioFormat,
                    )
                }
            )

        org.junit.Assert.assertThrows(AudioSourceAccessException::class.java) {
            AudioSource(
                createAudioSettings(),
                ioExecutor(),
                /*attributionContext=*/ null,
                listOf(failingProcessor),
                { _, _ -> createAudioStream() },
                AudioSource.DEFAULT_START_RETRY_INTERVAL_MS,
            )
        }
    }

    @Test
    fun audioProcessor_terminalProcessorNon16BitPcm_throwsAudioSourceAccessException() {
        val floatProcessor =
            createAudioProcessor(
                onConfigure = { inputFormat ->
                    AudioProcessor.AudioFormat(
                        inputFormat.sampleRate,
                        inputFormat.channelCount,
                        AudioFormat.ENCODING_PCM_FLOAT,
                    )
                }
            )

        val exception =
            org.junit.Assert.assertThrows(AudioSourceAccessException::class.java) {
                AudioSource(
                    createAudioSettings(),
                    ioExecutor(),
                    /*attributionContext=*/ null,
                    listOf(floatProcessor),
                    { _, _ -> createAudioStream() },
                    AudioSource.DEFAULT_START_RETRY_INTERVAL_MS,
                )
            }
        assertThat(exception.message).contains("ENCODING_PCM_16BIT")
    }

    @Test
    fun audioProcessor_processorModifiesSampleRateAndChannelCount_updatesOutputAudioSettings() {
        val resampleProcessor =
            createAudioProcessor(
                onConfigure = {
                    AudioProcessor.AudioFormat(
                        sampleRate = 44100,
                        channelCount = 1,
                        encoding = AudioFormat.ENCODING_PCM_16BIT,
                    )
                }
            )

        val audioSource =
            createAudioSource(
                audioSettings = createAudioSettings(), // 48000 Hz, 2 channels
                audioProcessors = listOf(resampleProcessor),
            )
        assertThat(audioSource.outputAudioSettings.captureSampleRate).isEqualTo(44100)
        assertThat(audioSource.outputAudioSettings.encodeSampleRate).isEqualTo(44100)
        assertThat(audioSource.outputAudioSettings.channelCount).isEqualTo(1)
        assertThat(audioSource.outputAudioSettings.audioFormat)
            .isEqualTo(AudioFormat.ENCODING_PCM_16BIT)
    }

    @Test
    fun audioProcessor_slowMotion_processorModifiesSampleRate_scalesEncodeSampleRate() {
        val resampleProcessor =
            createAudioProcessor(
                onConfigure = {
                    AudioProcessor.AudioFormat(
                        sampleRate = 48000,
                        channelCount = 1,
                        encoding = AudioFormat.ENCODING_PCM_16BIT,
                    )
                }
            )

        // Slow-motion 2x: capture 96000, encode 48000 (ratio 0.5)
        val slowMotionSettings =
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(96000)
                .setEncodeSampleRate(48000)
                .setChannelCount(2)
                .setAudioFormat(AUDIO_FORMAT)
                .build()

        val audioSource =
            createAudioSource(
                audioSettings = slowMotionSettings,
                audioProcessors = listOf(resampleProcessor),
            )
        // With new capture 48000, encode should be scaled to 24000
        assertThat(audioSource.outputAudioSettings.captureSampleRate).isEqualTo(48000)
        assertThat(audioSource.outputAudioSettings.encodeSampleRate).isEqualTo(24000)
    }

    @Test
    fun audioProcessor_stopAndRelease_flushesAndResetsAudioProcessors() {
        val flushLatch = CountDownLatch(1)
        val resetLatch = CountDownLatch(1)

        val testProcessor =
            createAudioProcessor(
                onFlush = { flushLatch.countDown() },
                onReset = { resetLatch.countDown() },
            )

        val audioSource = createAudioSource(audioProcessors = listOf(testProcessor))

        audioSource.start()
        audioSource.stop()
        assertThat(flushLatch.await(COMMON_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()

        audioSource.release().get(COMMON_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        assertThat(resetLatch.await(COMMON_TIMEOUT_MS, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun audioProcessor_outputsUnalignedBuffer_dispatchesFrameAlignedBuffers() {
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(SAMPLE_RATE)
                .setEncodeSampleRate(SAMPLE_RATE)
                .setChannelCount(2)
                .setAudioFormat(AUDIO_FORMAT)
                .build()
        val bytesPerFrame = audioSettings.bytesPerFrame
        assertThat(bytesPerFrame).isEqualTo(4)

        // Custom processor that produces an unaligned buffer (15 bytes = 3 frames (12 bytes) + 3
        // unaligned bytes)
        val unalignedProcessor = createAudioProcessor { inputBuffer ->
            val size = 15
            val outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            for (i in 0 until size) {
                outputBuffer.put((i % 128).toByte())
            }
            outputBuffer.flip()
            inputBuffer.position(inputBuffer.limit())
            outputBuffer
        }

        val bufferProvider = createBufferProvider()
        val audioSource =
            createAudioSource(
                audioSettings = audioSettings,
                audioProcessors = listOf(unalignedProcessor),
                bufferProvider = bufferProvider,
            )

        audioSource.start()

        bufferProvider.verifySubmittedBufferCall(CallTimesAtLeast(1), COMMON_TIMEOUT_MS) {
            submittedBuffers ->
            val firstBuffer = submittedBuffers[0]
            assertThat(firstBuffer.isSubmitted).isTrue()
            val submittedBytes = firstBuffer.byteBuffer.remaining()
            assertThat(submittedBytes % bytesPerFrame).isEqualTo(0)
            assertThat(submittedBytes).isEqualTo(12)
        }
    }

    @Test
    fun audioProcessor_outputsUnalignedBuffer_doesNotLivelock() {
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(SAMPLE_RATE)
                .setEncodeSampleRate(SAMPLE_RATE)
                .setChannelCount(2)
                .setAudioFormat(AUDIO_FORMAT)
                .build()
        val bytesPerFrame = audioSettings.bytesPerFrame
        assertThat(bytesPerFrame).isEqualTo(4)

        // Custom processor that produces an unaligned remainder (< bytesPerFrame)
        // on the first packet, then aligned buffers on subsequent packets.
        var callCount = 0
        val unalignedProcessor = createAudioProcessor { inputBuffer ->
            callCount++
            val size = if (callCount == 1) 2 else 16
            val outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            outputBuffer.limit(size)
            inputBuffer.position(inputBuffer.limit())
            outputBuffer
        }

        val bufferProvider = createBufferProvider()
        val audioSource =
            createAudioSource(
                audioSettings = audioSettings,
                audioProcessors = listOf(unalignedProcessor),
                bufferProvider = bufferProvider,
            )

        audioSource.start()

        // Verify pipeline does not freeze/livelock and successfully submits subsequent buffers
        bufferProvider.verifySubmittedBufferCall(CallTimesAtLeast(1), COMMON_TIMEOUT_MS) {
            submittedBuffers ->
            val submittedBuffer = submittedBuffers[0]
            assertThat(submittedBuffer.isSubmitted).isTrue()
            assertThat(submittedBuffer.byteBuffer.remaining()).isEqualTo(16)
        }
    }

    @Test
    fun audioProcessor_throwsRuntimeException_notifiesError() {
        val testException = RuntimeException("Custom DSP failure")
        val throwingProcessor = createAudioProcessor { throw testException }

        val audioSourceCallback = createAudioSourceCallback()
        val audioSource =
            createAudioSource(
                audioProcessors = listOf(throwingProcessor),
                audioSourceCallback = audioSourceCallback,
            )

        audioSource.start()

        audioSourceCallback.verifyOnError(CallTimesAtLeast(1), COMMON_TIMEOUT_MS) { errors ->
            assertThat(errors[0]).isInstanceOf(AudioSourceAccessException::class.java)
            assertThat(errors[0].cause).isEqualTo(testException)
        }
    }

    @Test
    fun audioProcessor_slowMotion_computesDurationUsingCaptureSampleRate() {
        // Slow-motion 4x: captureSampleRate = 48000, encodeSampleRate = 12000
        val audioSettings =
            AudioSettings.builder()
                .setAudioSource(AUDIO_SOURCE)
                .setCaptureSampleRate(48000)
                .setEncodeSampleRate(12000)
                .setChannelCount(2)
                .setAudioFormat(AUDIO_FORMAT)
                .build()

        // Processor outputs 24 bytes (6 frames at 4 bytes/frame).
        // FakeBufferProvider capacity is 16 bytes (4 frames).
        // Buffer 1 takes 16 bytes (4 frames).
        // Buffer 2 takes remaining 8 bytes (2 frames).
        val multiBufferProcessor = createAudioProcessor { inputBuffer ->
            val size = 24
            val outputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
            for (i in 0 until size) {
                outputBuffer.put(i.toByte())
            }
            outputBuffer.flip()
            inputBuffer.position(inputBuffer.limit())
            outputBuffer
        }

        val bufferProvider = createBufferProvider()
        val audioSource =
            createAudioSource(
                audioSettings = audioSettings,
                audioProcessors = listOf(multiBufferProcessor),
                bufferProvider = bufferProvider,
            )

        audioSource.start()

        bufferProvider.verifySubmittedBufferCall(CallTimesAtLeast(2), COMMON_TIMEOUT_MS) {
            submittedBuffers ->
            val buf1 = submittedBuffers[0]
            val buf2 = submittedBuffers[1]
            // Expected duration offset for 4 frames at 48000 Hz capture rate = 4 * 1_000_000 /
            // 48000 = 83 us
            // (If encodeSampleRate 12000 Hz were used, it would incorrectly be 333 us)
            val expectedOffsetUs = 4 * 1_000_000L / 48000L
            val actualOffsetUs = buf2.getPresentationTimeUs() - buf1.getPresentationTimeUs()
            assertThat(actualOffsetUs).isEqualTo(expectedOffsetUs)
        }
    }

    @Test
    fun audioProcessor_readBufferSizeIsMultipleOfBytesPerFrame() {
        val convertingProcessor =
            createAudioProcessor(
                onConfigure = { inputFormat ->
                    AudioProcessor.AudioFormat(
                        inputFormat.sampleRate,
                        inputFormat.channelCount,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                }
            )

        val testConfigs =
            listOf(
                // 16-bit PCM Mono: bytesPerFrame = 2
                Pair(AudioFormat.ENCODING_PCM_16BIT, 1),
                // 16-bit PCM Stereo: bytesPerFrame = 4
                Pair(AudioFormat.ENCODING_PCM_16BIT, 2),
                // 16-bit PCM Tri-mic (3 channels): bytesPerFrame = 6
                Pair(AudioFormat.ENCODING_PCM_16BIT, 3),
                // 24-bit PCM Mono: bytesPerFrame = 3 (4096 / 3 = 1365 * 3 = 4095)
                Pair(AudioFormat.ENCODING_PCM_24BIT_PACKED, 1),
                // 24-bit PCM Stereo: bytesPerFrame = 6 (1024 * 6 = 6144)
                Pair(AudioFormat.ENCODING_PCM_24BIT_PACKED, 2),
                // 32-bit Float Stereo: bytesPerFrame = 8 (1024 * 8 = 8192)
                Pair(AudioFormat.ENCODING_PCM_FLOAT, 2),
            )

        for ((encoding, channelCount) in testConfigs) {
            val audioSettings =
                AudioSettings.builder()
                    .setAudioSource(AUDIO_SOURCE)
                    .setCaptureSampleRate(SAMPLE_RATE)
                    .setEncodeSampleRate(SAMPLE_RATE)
                    .setChannelCount(channelCount)
                    .setAudioFormat(encoding)
                    .build()
            val bytesPerFrame = audioSettings.bytesPerFrame

            val audioSource =
                createAudioSource(
                    audioSettings = audioSettings,
                    audioProcessors =
                        if (encoding != AudioFormat.ENCODING_PCM_16BIT) {
                            listOf(convertingProcessor)
                        } else {
                            emptyList()
                        },
                )

            assertThat(audioSource.mReadBufferSize % bytesPerFrame).isEqualTo(0)
            assertThat(audioSource.mReadBufferSize).isAtLeast(1024 * bytesPerFrame)
        }
    }

    private fun createAudioStream(
        audioDataProvider: (Int) -> FakeAudioStream.AudioData = createAudioDataProvider(),
        exceptionOnStart: AudioStream.AudioStreamException? = null,
        exceptionOnStartMaxTimes: Int = Int.MAX_VALUE,
        readDelayMs: Long = 1,
    ) =
        FakeAudioStream(
            audioDataProvider,
            exceptionOnStart = exceptionOnStart,
            exceptionOnStartMaxTimes = exceptionOnStartMaxTimes,
            readDelayMs = readDelayMs,
        )

    private fun createAudioDataProvider(): (Int) -> FakeAudioStream.AudioData = { index ->
        val byteBuffer = ByteBuffer.allocate(BYTE_BUFFER_CAPACITY).put(0, index.toByte())
        val timestampNs = index.toLong()
        FakeAudioStream.AudioData(byteBuffer, timestampNs)
    }

    private fun createBufferProvider(
        initState: BufferProvider.State = BufferProvider.State.ACTIVE,
        bufferFactory: (Int) -> ListenableFuture<FakeInputBuffer> = { _ ->
            val inputBuffer = FakeInputBuffer(BYTE_BUFFER_CAPACITY)
            inputBuffer.byteBuffer.order(ByteOrder.nativeOrder())
            immediateFuture(inputBuffer)
        },
    ): FakeBufferProvider = FakeBufferProvider(state = initState, bufferFactory = bufferFactory)

    private fun createAudioSource(
        audioSettings: AudioSettings = createAudioSettings(),
        executor: Executor = ioExecutor(),
        audioProcessors: List<AudioProcessor> = emptyList(),
        audioStreamFactory: AudioStreamFactory = AudioStreamFactory { _, _ -> createAudioStream() },
        bufferProvider: FakeBufferProvider = createBufferProvider(),
        audioSourceCallback: FakeAudioSourceCallback = createAudioSourceCallback(),
        callbackExecutor: Executor = ioExecutor(),
        retryStartIntervalMs: Long = AudioSource.DEFAULT_START_RETRY_INTERVAL_MS,
    ): AudioSource =
        AudioSource(
                audioSettings,
                executor,
                /*attributionContext=*/ null,
                audioProcessors,
                audioStreamFactory,
                retryStartIntervalMs,
            )
            .apply {
                setAudioSourceCallback(callbackExecutor, audioSourceCallback)
                setBufferProvider(bufferProvider)
                audioSourcesToRelease.add(this)
            }

    private fun createAudioProcessor(
        onConfigure: (AudioProcessor.AudioFormat) -> AudioProcessor.AudioFormat = { it },
        onFlush: () -> Unit = {},
        onReset: () -> Unit = {},
        processBlock: (inputBuffer: ByteBuffer) -> ByteBuffer = { AudioProcessor.EMPTY_BUFFER },
    ): AudioProcessor =
        object : AudioProcessor {
            private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER

            override fun configure(
                inputAudioFormat: AudioProcessor.AudioFormat
            ): AudioProcessor.AudioFormat = onConfigure(inputAudioFormat)

            override fun queueInput(inputBuffer: ByteBuffer) {
                outputBuffer = processBlock(inputBuffer)
            }

            override fun getOutput(): ByteBuffer {
                val out = outputBuffer
                outputBuffer = AudioProcessor.EMPTY_BUFFER
                return out
            }

            override fun flush() {
                outputBuffer = AudioProcessor.EMPTY_BUFFER
                onFlush()
            }

            override fun reset() {
                outputBuffer = AudioProcessor.EMPTY_BUFFER
                onReset()
            }
        }

    private fun createAudioSettings() =
        AudioSettings.builder()
            .setAudioSource(AUDIO_SOURCE)
            .setCaptureSampleRate(SAMPLE_RATE)
            .setEncodeSampleRate(SAMPLE_RATE)
            .setChannelCount(CHANNEL_COUNT)
            .setAudioFormat(AUDIO_FORMAT)
            .build()

    private fun createAudioSourceCallback() = FakeAudioSourceCallback()

    private fun verifyBufferContentEquals(
        inputBuffer: FakeInputBuffer,
        audioData: FakeAudioStream.AudioData,
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
