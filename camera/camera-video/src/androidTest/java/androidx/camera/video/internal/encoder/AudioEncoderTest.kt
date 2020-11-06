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

import android.media.AudioFormat
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.timeout
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import java.nio.ByteBuffer

@LargeTest
@RunWith(AndroidJUnit4::class)
class AudioEncoderTest {

    companion object {
        private const val MIME_TYPE = "audio/mp4a-latm"
        private const val BIT_RATE = 64000
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_COUNT = 1
    }

    private lateinit var encoder: Encoder
    private lateinit var encoderCallback: EncoderCallback
    private lateinit var byteBufferProviderJob: Job

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
                .setBitrate(BIT_RATE)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setChannelCount(CHANNEL_COUNT)
                .build()
        )
        encoder.setEncoderCallback(encoderCallback, CameraXExecutors.directExecutor())

        // Prepare a fake audio source
        val byteBuffer = ByteBuffer.allocateDirect(1024)
        byteBufferProviderJob = GlobalScope.launch(Dispatchers.Default) {
            while (true) {
                byteBuffer.rewind()
                (encoder.input as Encoder.ByteBufferInput).putByteBuffer(byteBuffer)
                delay(200)
            }
        }
    }

    @After
    fun tearDown() {
        encoder.release()
        byteBufferProviderJob.cancel(null)
    }

    @Test
    fun discardInputBufferBeforeStart() {
        // Act.
        // Wait a second to receive data
        Thread.sleep(3000L)

        // Assert.
        verify(encoderCallback, never()).onEncodedData(any())
    }

    @Test
    fun canRestartEncoder() {
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
        // Act.
        encoder.start()
        encoder.stop()
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canPauseResumeEncoder() {
        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Act.
        encoder.pause()

        // Assert.
        // Since there is no exact event to know the encoder is paused, wait for a while until no
        // callback.
        verify(encoderCallback, noInvocation(3000L, 10000L)).onEncodedData(any())

        // Arrange.
        clearInvocations(encoderCallback)

        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())
    }

    @Test
    fun canPauseStopStartEncoder() {
        // Act.
        encoder.start()

        // Assert.
        verify(encoderCallback, timeout(15000L).atLeast(5)).onEncodedData(any())

        // Act.
        encoder.pause()

        // Assert.
        // Since there is no exact event to know the encoder is paused, wait for a while until no
        // callback.
        verify(encoderCallback, noInvocation(3000L, 10000L)).onEncodedData(any())

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
}
