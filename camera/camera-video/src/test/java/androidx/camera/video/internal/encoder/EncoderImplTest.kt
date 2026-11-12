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
import org.robolectric.shadows.ShadowLog
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
            )
        encoder.setEncoderCallback(encoderCallback, CameraXExecutors.directExecutor())
    }

    @After
    fun tearDown() {
        if (::encoder.isInitialized) {
            encoderCallback.heldData.forEach { it.close() }
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

    @Test
    fun encodeErrorWhileStopping_notifiesErrorOnlyAndCanRestart() {
        // Arrange: stop the encoder. It keeps stopping until the data reaches the stop time.
        encoder.start()
        feedInput(5)
        encoder.stop()

        // Act.
        encoder.handleEncodeError(EncodeException.ERROR_UNKNOWN, "Fake error", null)
        idle()

        // Assert: the error is notified instead of the stop.
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.stopCount).isEqualTo(0)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

        // Act: restart the encoder.
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)

        // Assert.
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    fun stopAfterPendingStart_staysStoppingUntilStopIsDone() {
        // Arrange: stop the encoder, then start it while it is still stopping.
        encoder.start()
        feedInput(5)
        encoder.stop()
        encoder.start()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.PENDING_START)

        // Act: stop the pending start.
        encoder.stop()

        // Assert: the encoder is still stopping rather than configured.
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.STOPPING)

        // Act: let the data reach the stop time.
        feedInputUntil { encoderCallback.stopCount > 0 }

        // Assert: the stop is done once.
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
    }

    @Test
    fun stopMediaCodecWhileStopping_handlesStoppedCodecOnce() {
        // Arrange: an error occurs while the encoded data is held, so the codec is being stopped.
        val heldData = startAndHoldEncodedData()
        encoder.handleEncodeError(EncodeException.ERROR_UNKNOWN, "Fake error", null)

        // Act: stop the codec again while it is being stopped, e.g. the end of stream is reached,
        // then return the data.
        encoder.stopMediaCodec()
        heldData.close()
        idle()

        // Assert: the stopped codec is handled once. Handling it again would act on a codec that
        // has already been reset, which throws on the encoder executor.
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.stopCount).isEqualTo(0)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
        assertNoExceptionOnEncoderExecutor()
    }

    @Test
    fun endOfStreamWhileEncoding_notifiesStopAndCanRestart() {
        // Arrange.
        encoder.start()
        feedInput(5)

        // Act: the codec emits an end-of-stream buffer without being stopped.
        feedInput(1, endOfStream = true)

        // Assert: the codec is stopped as if the encoder were stopped.
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
        assertNoExceptionOnEncoderExecutor()

        // Act: restart the encoder.
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)

        // Assert.
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    fun releaseStartedEncoder_waitsForOutstandingEncodedData() {
        // Arrange.
        val heldData = startAndHoldEncodedData()

        // Act.
        encoder.release()
        idle()

        // Assert: the codec is not released while the encoded data is held.
        assertThat(encoder.releasedFuture.isDone).isFalse()

        // Act.
        heldData.close()
        idle()

        // Assert: the codec is released without notifying the stop or an error.
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(encoderCallback.stopCount).isEqualTo(0)
        assertThat(encoderCallback.errors).isEmpty()
    }

    @Test
    fun releaseInErrorState_waitsForOutstandingEncodedDataWithoutNotifyingError() {
        // Arrange: an error occurs while encoding and the encoded data is held.
        val heldData = startAndHoldEncodedData()
        encoder.handleEncodeError(EncodeException.ERROR_UNKNOWN, "Fake error", null)

        // Act.
        encoder.release()
        idle()

        // Assert: the codec is not released while the encoded data is held.
        assertThat(encoder.releasedFuture.isDone).isFalse()

        // Act.
        heldData.close()
        idle()

        // Assert: the codec is released. The encoder is released without a requested stop, so
        // the error isn't notified. See Encoder#release().
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(encoderCallback.errors).isEmpty()
    }

    @Test
    fun releaseInErrorStateAfterStop_notifiesError() {
        // Arrange: an error occurs while stopping and the encoded data is held.
        val heldData = startAndHoldEncodedData()
        encoder.stop()
        encoder.handleEncodeError(EncodeException.ERROR_UNKNOWN, "Fake error", null)

        // Act.
        encoder.release()
        heldData.close()
        idle()

        // Assert: the codec is released, and the error is notified as the result of the stop.
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(encoderCallback.errors).hasSize(1)
    }

    @Test
    fun releaseInErrorStateWithoutStoppingCodec_releasesEncoderImmediately() {
        // Arrange: simulate an encoder left in the ERROR state without a stop in progress, e.g.
        // resetting the codec failed after the error.
        encoder.mState = EncoderImpl.InternalState.ERROR

        // Act.
        encoder.release()
        idle()

        // Assert.
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.RELEASED)
    }

    @Test
    fun encodeErrorWhileReleasingStartedEncoder_releasesEncoderWithoutNotifyingError() {
        // Arrange: release the started encoder while the encoded data is held.
        val heldData = startAndHoldEncodedData()
        encoder.release()
        idle()

        // Act: an error occurs while the release is pending, then the data is returned.
        encoder.handleEncodeError(EncodeException.ERROR_UNKNOWN, "Fake error", null)
        heldData.close()
        idle()

        // Assert: the codec is released rather than reset. An encoder released while encoding
        // doesn't report the error, e.g. the encoder of a persistent recording.
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.RELEASED)
        assertThat(encoderCallback.errors).isEmpty()
        assertThat(encoderCallback.stopCount).isEqualTo(0)
        assertNoExceptionOnEncoderExecutor()
    }

    @Test
    fun encodeErrorWhileReleasingStoppingEncoder_releasesEncoderAndNotifiesError() {
        // Arrange: stop the encoder while the encoded data is held, then release it before the
        // stop is done.
        val heldData = startAndHoldEncodedData()
        encoder.stop()
        encoder.release()
        idle()

        // Act: an error occurs while the release is pending.
        encoder.handleEncodeError(EncodeException.ERROR_UNKNOWN, "Fake error", null)

        // Assert: the release doesn't wait for the end of stream anymore, so the pending stop is
        // cancelled and its timeout can't act on the codec.
        assertThat(encoder.mPendingCodecStop).isFalse()
        assertThat(encoder.mStopTimeoutFuture).isNull()

        // Act: the data is returned.
        heldData.close()
        idle()

        // Assert: the codec is released rather than reset, and the error is notified once as the
        // result of the stop.
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.RELEASED)
        assertThat(encoderCallback.errors).hasSize(1)
        assertNoExceptionOnEncoderExecutor()
    }

    @Test
    fun releaseWhileStopping_notifiesStopAndReleasesEncoder() {
        // Arrange: stop the encoder. It keeps stopping until the data reaches the stop time.
        encoder.start()
        feedInput(5)
        encoder.stop()

        // Act: release the encoder, then let the data reach the stop time.
        encoder.release()
        feedInputUntil { encoder.releasedFuture.isDone }

        // Assert: the stop is notified before the codec is released.
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).isEmpty()
    }

    /**
     * Starts the encoder and holds the first encoded data without closing it.
     *
     * Note that the fake codec only keeps one input buffer in rotation: an input buffer becomes
     * available again only when its output buffer is released. So no more data is encoded while the
     * data is held.
     */
    private fun startAndHoldEncodedData(): EncodedData {
        encoderCallback.holdFirstData = true
        encoder.start()
        feedInputUntil { encoderCallback.heldData.isNotEmpty() }
        return encoderCallback.heldData.single()
    }

    /**
     * Asserts that no task threw on the encoder executor. The sequential executor of the encoder
     * catches and logs the exceptions thrown by its tasks.
     */
    private fun assertNoExceptionOnEncoderExecutor() {
        assertThat(ShadowLog.getLogsForTag("SequentialExecutor")).isEmpty()
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
     * Submits [count] input buffers, 10ms apart, and lets the codec process them. The buffers are
     * marked as the end of stream if [endOfStream] is true. Stops early once the encoder doesn't
     * provide input buffers anymore, e.g. before it is started or after the codec is stopping.
     */
    private fun feedInput(count: Int, endOfStream: Boolean = false) {
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
            inputBuffer.setEndOfStream(endOfStream)
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
        /** Whether to hold the first encoded data instead of closing it. */
        var holdFirstData = false
        val heldData = mutableListOf<EncodedData>()

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
            if (holdFirstData && heldData.isEmpty()) {
                heldData.add(encodedData)
            } else {
                encodedData.close()
            }
        }

        override fun onOutputConfigUpdate(outputConfig: OutputConfig) {}
    }
}
