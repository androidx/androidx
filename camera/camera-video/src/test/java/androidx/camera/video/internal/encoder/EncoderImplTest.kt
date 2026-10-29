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

package androidx.camera.video.internal.encoder

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.os.Looper.getMainLooper
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.SessionConfig.SESSION_TYPE_REGULAR
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.video.internal.BufferProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.MediaCodecInfoBuilder
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaCodecList

private const val SAMPLE_RATE = 44100
private const val CHANNEL_COUNT = 1
private const val BUFFER_SIZE = 1024

/**
 * Tests the state machine of [EncoderImpl] with a fake codec.
 *
 * The tests use an audio encoder, since a video encoder requires a persistent input surface that is
 * not available on the JVM. The state machine is shared by both.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.ALL_SDKS])
class EncoderImplTest {

    private val encoderCallback = FakeEncoderCallback()
    private lateinit var encoder: EncoderImpl

    @Before
    fun setUp() {
        ShadowMediaCodecList.addCodec(
            MediaCodecInfoBuilder.newBuilder()
                .setName("fake.aac.encoder")
                .setIsEncoder(true)
                .setCapabilities(
                    MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                        .setMediaFormat(
                            MediaFormat.createAudioFormat(
                                MIMETYPE_AUDIO_AAC,
                                SAMPLE_RATE,
                                CHANNEL_COUNT,
                            )
                        )
                        .setIsEncoder(true)
                        .setProfileLevels(
                            arrayOf(
                                MediaCodecInfo.CodecProfileLevel().apply {
                                    profile = MediaCodecInfo.CodecProfileLevel.AACObjectLC
                                }
                            )
                        )
                        .build()
                )
                .build()
        )
        // The fake codec copies each input buffer to an output buffer.
        ShadowMediaCodec.addEncoder(
            MIMETYPE_AUDIO_AAC,
            ShadowMediaCodec.CodecConfig(BUFFER_SIZE, BUFFER_SIZE) { input, output ->
                output.put(input)
            },
        )
        encoder =
            EncoderImpl(
                CameraXExecutors.directExecutor(),
                AudioEncoderConfig.builder()
                    .setMimeType(MIMETYPE_AUDIO_AAC)
                    .setProfile(MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                    .setInputTimebase(Timebase.UPTIME)
                    .setBitrate(64000)
                    .setCaptureSampleRate(SAMPLE_RATE)
                    .setEncodeSampleRate(SAMPLE_RATE)
                    .setChannelCount(CHANNEL_COUNT)
                    .build(),
                SESSION_TYPE_REGULAR,
            )
        encoder.setEncoderCallback(encoderCallback, CameraXExecutors.directExecutor())
    }

    @After
    fun tearDown() {
        if (::encoder.isInitialized) {
            encoder.release()
            idle()
        }
        ShadowMediaCodec.clearCodecs()
        ShadowMediaCodecList.reset()
    }

    @Test
    fun canGetEncoderInfo() {
        assertThat(encoder.encoderInfo).isInstanceOf(AudioEncoderInfo::class.java)
    }

    @Test
    fun discardInputBufferBeforeStart() {
        // Act.
        feedInput(5)

        // Assert.
        assertThat(encoderCallback.encodedData).isEmpty()
    }

    @Test
    fun canRestartEncoderImmediately() {
        // Act.
        encoder.start()
        encoder.stop()
        encoder.start()
        feedInput(5)

        // Assert.
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    fun canPauseStopStartEncoder() {
        // Arrange.
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()

        // Act.
        encoder.pause()
        feedInputUntil { encoderCallback.pauseCount > 0 }
        encoder.stop()
        feedInputUntil { encoderCallback.stopCount > 0 }

        // Assert.
        assertThat(encoderCallback.pauseCount).isEqualTo(1)
        assertThat(encoderCallback.stopCount).isEqualTo(1)

        // Act.
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)

        // Assert.
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    fun canRestartPauseEncoder() {
        // Arrange.
        encoder.start()
        feedInput(5)

        // Act.
        encoder.stop()
        encoder.start()
        encoder.pause()
        feedInputUntil { encoderCallback.pauseCount > 0 }

        // Assert.
        assertThat(encoderCallback.pauseCount).isEqualTo(1)
    }

    @Test
    fun pauseResumeEncoder_getChronologicalData() {
        // Act.
        encoder.start()
        feedInput(5)
        encoder.pause()
        feedInputUntil { encoderCallback.pauseCount > 0 }
        encoder.start()
        feedInput(5)

        // Assert.
        val timestamps = encoderCallback.encodedDataTimestampsUs
        assertThat(timestamps.size).isAtLeast(5 + 5)
        assertThat(timestamps).isInStrictOrder()
    }

    @Test
    fun stopEncoder_reachStopTime() {
        // Arrange.
        encoder.start()
        feedInput(5)
        val stopTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime())

        // Act.
        encoder.stop()
        feedInputUntil { encoderCallback.stopCount > 0 }

        // Assert: the encoding is stopped by data instead of the timeout.
        assertThat(encoder.mLastDataStopTimestamp).isNotNull()
        assertThat(encoder.mLastDataStopTimestamp).isAtLeast(stopTimeUs)
    }

    @Test
    fun bufferProvider_canAcquireBuffer() {
        // Arrange.
        encoder.start()
        val input = encoder.input as Encoder.ByteBufferInput

        repeat(8) {
            // Act.
            val future = input.acquireBuffer()
            idle()

            // Assert.
            assertThat(future.isDone).isTrue()
            val inputBuffer = future.get()
            assertThat(inputBuffer).isNotNull()
            inputBuffer.cancel()
            idle()
        }
    }

    @Test
    fun bufferProvider_canReceiveBufferProviderStateChange() {
        // Arrange.
        val states = mutableListOf<BufferProvider.State?>()
        (encoder.input as Encoder.ByteBufferInput).addObserver(
            CameraXExecutors.directExecutor(),
            object : Observable.Observer<BufferProvider.State> {
                override fun onNewData(value: BufferProvider.State?) {
                    states.add(value)
                }

                override fun onError(t: Throwable) {
                    states.add(null)
                }
            },
        )

        // Assert.
        assertThat(states).containsExactly(BufferProvider.State.INACTIVE)

        // Act: start, then pause once the data reaches the pause time.
        encoder.start()
        feedInput(5)
        encoder.pause()
        feedInputUntil { states.size >= 3 }

        // Act: resume, then stop once the data reaches the stop time.
        encoder.start()
        feedInput(5)
        encoder.stop()
        feedInputUntil { states.size >= 5 }

        // Assert.
        assertThat(states)
            .containsExactly(
                BufferProvider.State.INACTIVE,
                BufferProvider.State.ACTIVE,
                BufferProvider.State.INACTIVE,
                BufferProvider.State.ACTIVE,
                BufferProvider.State.INACTIVE,
            )
            .inOrder()
    }

    private fun idle(millis: Long = 0) {
        shadowOf(getMainLooper()).idleFor(millis, TimeUnit.MILLISECONDS)
    }

    /** Feeds input buffers until [condition] is met. */
    private fun feedInputUntil(condition: () -> Boolean) {
        repeat(200) {
            if (condition()) {
                return
            }
            feedInput(1)
        }
        assertWithMessage("Condition is not met after feeding input").that(condition()).isTrue()
    }

    /**
     * Submits [count] input buffers, 10ms apart, and lets the codec process them. Stops early once
     * the encoder doesn't provide input buffers anymore, e.g. before it is started or after the
     * codec is stopping.
     */
    private fun feedInput(count: Int) {
        val input = encoder.input as Encoder.ByteBufferInput
        repeat(count) {
            idle(10)
            val future = input.acquireBuffer()
            // Don't cancel a pending acquisition: an InputBuffer acquired after the cancellation
            // would never be returned, which blocks the stop.
            repeat(100) {
                if (!future.isDone) {
                    idle(10)
                }
            }
            val inputBuffer =
                try {
                    future.get(0, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    return
                }
            inputBuffer.byteBuffer.put(ByteArray(BUFFER_SIZE / 2)).flip()
            inputBuffer.setPresentationTimeUs(TimeUnit.NANOSECONDS.toMicros(System.nanoTime()))
            inputBuffer.submit()
            idle()
        }
    }

    private class FakeEncoderCallback : EncoderCallback {
        val encodedData = mutableListOf<EncodedData>()
        val encodedDataTimestampsUs = mutableListOf<Long>()
        val errors = mutableListOf<EncodeException>()
        var stopCount = 0
        var pauseCount = 0

        override fun onEncodeStart() {}

        override fun onEncodeStop() {
            stopCount++
        }

        override fun onEncodePaused() {
            pauseCount++
        }

        override fun onEncodeError(e: EncodeException) {
            errors.add(e)
        }

        override fun onEncodedData(encodedData: EncodedData) {
            this.encodedData.add(encodedData)
            encodedDataTimestampsUs.add(encodedData.presentationTimeUs)
            encodedData.close()
        }

        override fun onOutputConfigUpdate(outputConfig: OutputConfig) {}
    }
}
