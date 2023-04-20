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
package androidx.camera.video.internal.encoder

import android.media.MediaCodecInfo
import android.os.Build
import androidx.camera.core.impl.Observable.Observer
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.video.internal.BufferProvider
import androidx.camera.video.internal.BufferProvider.State
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class AudioEncoderTest {

    companion object {
        private const val MIME_TYPE = "audio/mp4a-latm"
        private const val ENCODER_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC
        private val INPUT_TIMEBASE = Timebase.UPTIME
        private const val BIT_RATE = 64000
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 1
    }

    private lateinit var encoder: EncoderImpl
    private lateinit var encoderCallback: EncoderCallback
    private lateinit var fakeAudioLoop: FakeAudioLoop

    @Before
    fun setup() {
        encoderCallback = Mockito.mock(EncoderCallback::class.java)
        Mockito.doAnswer { args: InvocationOnMock ->
            val encodedData: EncodedData = args.getArgument(0)
            encodedData.close()
            null
        }.`when`(encoderCallback).onEncodedData(any())

        encoder = EncoderImpl(
            CameraXExecutors.ioExecutor(),
            AudioEncoderConfig.builder()
                .setMimeType(MIME_TYPE)
                .setProfile(ENCODER_PROFILE)
                .setInputTimebase(INPUT_TIMEBASE)
                .setBitrate(BIT_RATE)
                .setSampleRate(SAMPLE_RATE)
                .setChannelCount(CHANNEL_COUNT)
                .build()
        )
        encoder.setEncoderCallback(encoderCallback, CameraXExecutors.directExecutor())

        @Suppress("UNCHECKED_CAST")
        fakeAudioLoop = FakeAudioLoop(encoder.input as BufferProvider<InputBuffer>)
    }

    @After
    fun tearDown() {
        if (this::encoder.isInitialized) {
            encoder.release()
            encoder.releasedFuture[10, TimeUnit.SECONDS]
        }
        if (this::fakeAudioLoop.isInitialized) {
            fakeAudioLoop.stop()
        }
    }

    @Test
    fun canGetEncoderInfo() {
        assertThat(encoder.encoderInfo).isNotNull()
    }

    @Test
    fun discardInputBufferBeforeStart() {
        // Arrange.
        fakeAudioLoop.start()

        // Act.
        // Wait a second to receive data
        Thread.sleep(3000L)

        // Assert.
        verify(encoderCallback, never()).onEncodedData(any())
    }

    @Test
    fun canRestartEncoder() {
        // Skip for b/269129619
        assumeFalse(
            "Skip test for Cuttlefish API 30 flaky native crash",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 30
        )

        // Arrange.
        fakeAudioLoop.start()

        for (i in 0..3) {
            // Arrange.
            clearInvocations(encoderCallback)

            // Act.
            encoder.start()

            // Assert.
            val inOrder = inOrder(encoderCallback)
            inOrder.verify(encoderCallback, timeout(5000L)).onEncodeStart()
            inOrder.verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

            // Act.
            encoder.stop()

            // Assert.
            inOrder.verify(encoderCallback, timeout(5000L)).onEncodeStop()
        }
    }

    @Test
    fun canRestartEncoderImmediately() {
        // Arrange.
        fakeAudioLoop.start()

        // Act.
        encoder.start()
        encoder.stop()
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canPauseResumeEncoder() {
        // Arrange.
        fakeAudioLoop.start()

        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Act.
        encoder.pause()

        // Assert.
        verify(encoderCallback, timeout(5000L)).onEncodePaused()

        // Arrange.
        clearInvocations(encoderCallback)

        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canPauseStopStartEncoder() {
        // Arrange.
        fakeAudioLoop.start()

        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Act.
        encoder.pause()

        // Assert.
        verify(encoderCallback, timeout(5000L)).onEncodePaused()

        // Act.
        encoder.stop()

        // Assert.
        verify(encoderCallback, timeout(5000L)).onEncodeStop()

        // Arrange.
        clearInvocations(encoderCallback)

        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canRestartPauseEncoder() {
        // Arrange.
        fakeAudioLoop.start()

        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Act.
        encoder.stop()
        encoder.start()
        encoder.pause()

        // Assert.
        verify(encoderCallback, timeout(5000L)).onEncodePaused()
    }

    @Test
    fun pauseEncoder_presentationTimeShouldExcludePausedDuration() {
        // Arrange.
        // The test step is "start and wait data" -> "pause for a while" -> "resume and wait
        // data", then make sure the timestamp of resume data doesn't include the pause duration.
        // Make the pause duration = wait data timeout, then even the worst case, the difference
        // between 2 data should be always smaller than pause duration if the pause duration is
        // not included.
        val timeoutWaitDataMs = 1500L
        val pauseDurationMs = timeoutWaitDataMs

        val presentationTimeUs = AtomicLong()
        val encoderCallback = Mockito.mock(EncoderCallback::class.java)
        Mockito.doAnswer { args: InvocationOnMock ->
            val encodedData: EncodedData = args.getArgument(0)
            presentationTimeUs.set(encodedData.presentationTimeUs)
            encodedData.close()
            null
        }.`when`(encoderCallback).onEncodedData(any())
        encoder.setEncoderCallback(encoderCallback, CameraXExecutors.directExecutor())

        // Act.
        fakeAudioLoop.start()
        encoder.start()

        // Get presentation time of encoded data before pause.
        verify(encoderCallback, timeout(timeoutWaitDataMs).atLeastOnce()).onEncodedData(any())
        val presentationTimeBeforePause = presentationTimeUs.get()

        encoder.pause()
        Thread.sleep(pauseDurationMs)
        encoder.start()

        // Get presentation time of encoded data after resume.
        verify(encoderCallback, timeout(timeoutWaitDataMs).atLeastOnce()).onEncodedData(any())
        val presentationTimeAfterResume = presentationTimeUs.get()

        // Assert.
        assertThat(presentationTimeAfterResume > presentationTimeBeforePause)
        val timeDiffMs = TimeUnit.MICROSECONDS.toMillis(
            presentationTimeAfterResume - presentationTimeBeforePause
        )
        assertThat(timeDiffMs < pauseDurationMs)
    }

    @Test
    fun pauseResumeEncoder_getChronologicalData() {
        // Arrange.
        fakeAudioLoop.start()
        val inOrder = inOrder(encoderCallback)

        // Act.
        encoder.start()
        inOrder.verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        encoder.pause()
        inOrder.verify(encoderCallback, timeout(5000L)).onEncodePaused()

        encoder.start()
        inOrder.verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Assert.
        val captor = ArgumentCaptor.forClass(EncodedData::class.java)
        verify(
            encoderCallback,
            Mockito.atLeast(/*start*/5 + /*resume*/5)
        ).onEncodedData(captor.capture())
        verifyDataInChronologicalOrder(captor.allValues)

        // Cleanup.
        encoder.stop()
    }

    @Test
    fun stopEncoder_reachStopTime() {
        // Arrange.
        fakeAudioLoop.start()

        // Act.
        encoder.start()
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        val stopTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime())

        encoder.stop()
        verify(encoderCallback, timeout(5000L)).onEncodeStop()

        // Assert.
        // If the last data timestamp is null, it means the encoding is probably stopped because of timeout.
        assertThat(encoder.mLastDataStopTimestamp).isNotNull()
        assertThat(encoder.mLastDataStopTimestamp).isAtLeast(stopTimeUs)
    }

    @Test
    fun bufferProvider_canAcquireBuffer() {
        // Arrange.
        encoder.start()

        for (i in 0..8) {
            // Act.
            val inputBuffer = (encoder.input as Encoder.ByteBufferInput)
                .acquireBuffer()
                .get(3, TimeUnit.SECONDS)

            // Assert.
            assertThat(inputBuffer).isNotNull()
            inputBuffer.cancel()
        }
    }

    @Test
    fun bufferProvider_canReceiveBufferProviderStateChange() {
        // Arrange.
        fakeAudioLoop.start()
        val stateRef = AtomicReference<State>()
        val lock = Semaphore(0)
        (encoder.input as Encoder.ByteBufferInput).addObserver(
            CameraXExecutors.directExecutor(),
            object : Observer<State> {
                override fun onNewData(state: State?) {
                    stateRef.set(state)
                    lock.release()
                }

                override fun onError(t: Throwable) {
                    stateRef.set(null)
                    lock.release()
                }
            }
        )

        // Assert.
        assertThat(lock.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        assertThat(stateRef.get()).isEqualTo(State.INACTIVE)

        // Act.
        encoder.start()

        // Assert.
        assertThat(lock.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        assertThat(stateRef.get()).isEqualTo(State.ACTIVE)

        // Act.
        encoder.pause()

        // Assert
        assertThat(lock.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        assertThat(stateRef.get()).isEqualTo(State.INACTIVE)

        // Act.
        encoder.start()

        // Assert.
        assertThat(lock.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        assertThat(stateRef.get()).isEqualTo(State.ACTIVE)

        // Act.
        encoder.stop()

        // Assert.
        assertThat(lock.tryAcquire(3, TimeUnit.SECONDS)).isTrue()
        assertThat(stateRef.get()).isEqualTo(State.INACTIVE)
    }

    private fun verifyDataInChronologicalOrder(encodedDataList: List<EncodedData>) {
        // For each item indexed by n and n+1, verify that the timestamp of n is less than n+1.
        encodedDataList.take(encodedDataList.size - 1).forEachIndexed { index, _ ->
            assertThat(encodedDataList[index].presentationTimeUs)
                .isLessThan(encodedDataList[index + 1].presentationTimeUs)
        }
    }

    private class FakeAudioLoop(private val bufferProvider: BufferProvider<InputBuffer>) {
        private val inputByteBuffer = ByteBuffer.allocateDirect(1024)
        private val started = AtomicBoolean(false)
        private var job: Job? = null

        @OptIn(DelicateCoroutinesApi::class)
        fun start() {
            if (started.getAndSet(true)) {
                return
            }
            job = GlobalScope.launch(
                CameraXExecutors.ioExecutor().asCoroutineDispatcher()
            ) {
                while (true) {
                    val acquireFuture = bufferProvider.acquireBuffer()
                    try {
                        val inputBuffer = acquireFuture.await()
                        inputBuffer.apply {
                            byteBuffer.apply {
                                put(
                                    inputByteBuffer.apply {
                                        clear()
                                        limit(limit().coerceAtMost(byteBuffer.capacity()))
                                    }
                                )
                                flip()
                            }
                            setPresentationTimeUs(TimeUnit.NANOSECONDS.toMicros(System.nanoTime()))
                            submit()
                        }
                    } catch (e: IllegalStateException) {
                        if (e is CancellationException) {
                            // When the fake loop is stopped, cancel acquired InputBuffer if any.
                            if (!acquireFuture.cancel(true)) {
                                try {
                                    acquireFuture.await().cancel()
                                } catch (ignored: Exception) {
                                }
                            }
                        }
                        // For simplicity, AudioLoop doesn't monitor the encoder's state.
                        // When an IllegalStateException is thrown by encoder which is not started,
                        // AudioLoop should retry with a delay to avoid busy loop.
                        // CancellationException is a subclass of IllegalStateException and is
                        // ambiguous since the cancellation could be caused by ListenableFuture
                        // was cancelled or coroutine Job was cancelled. For the
                        // ListenableFuture case, AudioLoop will need to retry with a delay as
                        // IllegalStateException. For the coroutine Job case, the loop should
                        // be stopped. The goal can be simply achieved by calling delay() method
                        // because the method will also get CancellationException if it is
                        // coroutine Job cancellation, and eventually leave the audio loop.
                        delay(300L)
                    }
                }
            }
        }

        fun stop() {
            if (!started.getAndSet(false)) {
                return
            }
            job!!.cancel()
        }
    }
}
