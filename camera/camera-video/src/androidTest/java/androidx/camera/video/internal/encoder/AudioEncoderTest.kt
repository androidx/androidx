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
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.testing.impl.AndroidUtil.isEmulator
import androidx.camera.video.internal.BufferProvider
import androidx.concurrent.futures.await
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock

@LargeTest
@RunWith(AndroidJUnit4::class)
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
        // Skip for b/264902324
        assumeFalse(
            "Emulator API 30 crashes running this test.",
            Build.VERSION.SDK_INT == 30 && isEmulator(),
        )

        encoderCallback = Mockito.mock(EncoderCallback::class.java)
        Mockito.doAnswer { args: InvocationOnMock ->
                val encodedData: EncodedData = args.getArgument(0)
                encodedData.close()
                null
            }
            .`when`(encoderCallback)
            .onEncodedData(any())

        encoder =
            EncoderImpl(
                CameraXExecutors.ioExecutor(),
                AudioEncoderConfig.builder()
                    .setMimeType(MIME_TYPE)
                    .setProfile(ENCODER_PROFILE)
                    .setInputTimebase(INPUT_TIMEBASE)
                    .setBitrate(BIT_RATE)
                    .setCaptureSampleRate(SAMPLE_RATE)
                    .setEncodeSampleRate(SAMPLE_RATE)
                    .setChannelCount(CHANNEL_COUNT)
                    .build(),
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
    fun canRestartEncoder() {
        // Skip for b/269129619
        assumeFalse(
            "Skip test for Cuttlefish API 30 flaky native crash",
            Build.MODEL.contains("Cuttlefish") && Build.VERSION.SDK_INT == 30,
        )

        // Arrange.
        fakeAudioLoop.start()

        repeat(3) {
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

    private class FakeAudioLoop(private val bufferProvider: BufferProvider<InputBuffer>) {
        private val inputByteBuffer = ByteBuffer.allocateDirect(1024)
        private val started = AtomicBoolean(false)
        private var job: Job? = null

        @OptIn(DelicateCoroutinesApi::class)
        fun start() {
            if (started.getAndSet(true)) {
                return
            }
            job =
                GlobalScope.launch(CameraXExecutors.ioExecutor().asCoroutineDispatcher()) {
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
                                setPresentationTimeUs(
                                    TimeUnit.NANOSECONDS.toMicros(System.nanoTime())
                                )
                                submit()
                            }
                        } catch (e: IllegalStateException) {
                            if (e is CancellationException) {
                                // When the fake loop is stopped, cancel acquired InputBuffer if
                                // any.
                                if (!acquireFuture.cancel(true)) {
                                    try {
                                        acquireFuture.await().cancel()
                                    } catch (_: Exception) {}
                                }
                            }
                            // For simplicity, AudioLoop doesn't monitor the encoder's state.
                            // When an IllegalStateException is thrown by encoder which is not
                            // started,
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
