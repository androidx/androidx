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

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaFormat.MIMETYPE_AUDIO_AAC
import android.media.MediaFormat.MIMETYPE_VIDEO_AVC
import android.os.Bundle
import android.os.Looper.getMainLooper
import android.util.Size
import android.view.Surface
import androidx.camera.core.impl.Observable
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.executor.CameraXExecutors
import androidx.camera.video.internal.BufferProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.MediaCodecInfoBuilder
import org.robolectric.shadows.ShadowLog
import org.robolectric.shadows.ShadowMediaCodec
import org.robolectric.shadows.ShadowMediaCodecList
import org.robolectric.util.ReflectionHelpers.ClassParameter

private const val SAMPLE_RATE = 44100
private const val CHANNEL_COUNT = 1
private const val BUFFER_SIZE = 1024

/**
 * Tests the state machine of [EncoderImpl] with a fake codec.
 *
 * Most tests use an audio encoder, while surface-specific paths use
 * [FaultInjectingShadowMediaCodec] to stub persistent input surface APIs on the JVM.
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
        FaultInjectingShadowMediaCodec.resetFlags()
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

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun outputBufferIllegalStateException_notifiesEncodeErrorAndCanRestart() {
        encoder.start()
        FaultInjectingShadowMediaCodec.throwOnGetOutputBuffer = true

        feedInput(1)

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(encoderCallback.errors.single().cause)
            .isInstanceOf(IllegalStateException::class.java)

        FaultInjectingShadowMediaCodec.throwOnGetOutputBuffer = false
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).hasSize(5)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun droppedOutputBufferIllegalStateException_notifiesEncodeError() {
        // Start in the future so output frames are dropped via releaseOutputBuffer(index, false).
        encoder.start(Long.MAX_VALUE / 2)
        FaultInjectingShadowMediaCodec.throwOnReleaseOutputBuffer = true

        feedInput(1)

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun encodedDataCloseIllegalStateException_notifiesEncodeError() {
        val heldData = startAndHoldEncodedData()
        FaultInjectingShadowMediaCodec.throwOnReleaseOutputBuffer = true

        heldData.close()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun inputBufferIllegalStateException_completesAcquisitionAndNotifiesSingleError() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val (heldInputs, pendingAcquire) = drainAvailableInputBuffers(input)
        FaultInjectingShadowMediaCodec.throwOnGetInputBuffer = true

        submitAndCancelRemaining(heldInputs)

        assertNoExceptionOnEncoderExecutor()
        assertThat(pendingAcquire.isDone).isTrue()
        val thrown = assertThrows(ExecutionException::class.java) { pendingAcquire.get() }
        assertThat(thrown.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(thrown.cause).isNotInstanceOf(CancellationException::class.java)
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun endOfStreamAcquisitionIllegalStateException_notifiesSingleError() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val (heldInputs, pendingAcquire) = drainAvailableInputBuffers(input)
        pendingAcquire.cancel(true)
        encoder.stop()
        idle(1_000L)
        FaultInjectingShadowMediaCodec.throwOnGetInputBuffer = true

        submitAndCancelRemaining(heldInputs)

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun syncEndOfStreamAcquisitionIllegalStateException_notifiesSingleError() {
        encoder.start()
        idle()
        encoder.stop()
        FaultInjectingShadowMediaCodec.throwOnGetInputBuffer = true

        // Stop timeout invokes signalCodecStop() -> signalEndOfInputStream() while a free input
        // buffer index is already queued and no EncodedData is outstanding.
        idle(1_000L)

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun queueInputBufferAndStartIllegalStateException_notifyEncodeErrorAndDeactivateInput() {
        encoder.start()
        val input = encoder.input as Encoder.ByteBufferInput
        val inputBuffer = input.acquireBuffer().apply { idle() }.get(0, TimeUnit.MILLISECONDS)
        FaultInjectingShadowMediaCodec.throwOnQueueInputBuffer = true

        inputBuffer.byteBuffer.put(ByteArray(BUFFER_SIZE / 2)).flip()
        inputBuffer.submit()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(input.fetchData().get(0, TimeUnit.MILLISECONDS))
            .isEqualTo(BufferProvider.State.INACTIVE)

        FaultInjectingShadowMediaCodec.throwOnQueueInputBuffer = false
        FaultInjectingShadowMediaCodec.throwOnStart = true
        encoder.start()
        idle()

        assertThat(encoderCallback.errors).hasSize(2)
        assertThat(encoderCallback.errors.last().errorType).isEqualTo(EncodeException.ERROR_CODEC)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun resumeAndRequestKeyFrameSetParametersIllegalStateException_notifyEncodeError() {
        encoder.start()
        feedInput(5)
        encoder.pause()
        feedInputUntil { encoderCallback.pauseCount > 0 }
        FaultInjectingShadowMediaCodec.throwOnSetParameters = true

        encoder.start()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(
                (encoder.input as Encoder.ByteBufferInput).fetchData().get(0, TimeUnit.MILLISECONDS)
            )
            .isEqualTo(BufferProvider.State.INACTIVE)

        FaultInjectingShadowMediaCodec.throwOnSetParameters = false
        encoder.start()
        FaultInjectingShadowMediaCodec.throwOnSetParameters = true
        encoder.requestKeyFrame()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(2)
        assertThat(encoderCallback.errors.last().errorType).isEqualTo(EncodeException.ERROR_CODEC)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun pauseSetParametersIllegalStateExceptionInCheckBufferInfo_skipsReleaseOutputBuffer() {
        encoder.start()
        feedInput(1)
        encoder.pause()
        FaultInjectingShadowMediaCodec.throwOnSetParameters = true
        FaultInjectingShadowMediaCodec.throwOnReleaseOutputBuffer = true

        feedInput(1)

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(
                ShadowLog.getLogsForTag("AudioEncoder").none {
                    it.msg.contains("Get more than one error")
                }
            )
            .isTrue()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun stopMediaCodecIllegalStateExceptionInHandleStopped_notifiesStopAndResets() {
        encoder.start()
        feedInput(5)
        encoder.stop()
        FaultInjectingShadowMediaCodec.throwOnStop = true

        feedInputUntil { encoderCallback.stopCount > 0 }
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).isEmpty()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

        FaultInjectingShadowMediaCodec.throwOnStop = false
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun flushedVideoEncoderStopAndFlushIllegalStateException_recoverAndCompleteRelease() =
        withVideoEncoder { videoEncoder, videoCallback ->
            // Flush failure during handleStopped() still notifies stop and resets to CONFIGURED.
            videoEncoder.start()
            FaultInjectingShadowMediaCodec.throwOnFlush = true
            videoEncoder.stop()
            idle(1_000L)

            assertNoExceptionOnEncoderExecutor()
            assertThat(videoCallback.stopCount).isEqualTo(1)
            assertThat(videoCallback.errors).isEmpty()
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

            // Successful stop leaves SurfaceInput codec flushed; stop() ISE on
            // signalSourceStopped() still runs reset(), and stop() ISE on release() still
            // completes releasedFuture.
            FaultInjectingShadowMediaCodec.throwOnFlush = false
            videoEncoder.start()
            videoEncoder.stop()
            idle(1_000L)
            assertThat(videoCallback.stopCount).isEqualTo(2)

            FaultInjectingShadowMediaCodec.throwOnStop = true
            videoEncoder.signalSourceStopped()
            idle()
            assertNoExceptionOnEncoderExecutor()

            FaultInjectingShadowMediaCodec.throwOnStop = false
            videoEncoder.start()
            videoEncoder.stop()
            idle(1_000L)
            assertThat(videoCallback.stopCount).isEqualTo(3)

            FaultInjectingShadowMediaCodec.throwOnStop = true
            videoEncoder.release()
            idle()

            assertNoExceptionOnEncoderExecutor()
            assertThat(videoEncoder.releasedFuture.isDone).isTrue()
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.RELEASED)
        }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun videoNonKeyFrameRequestKeyFrameIllegalStateException_skipsReleaseOutputBuffer() =
        withVideoEncoder { videoEncoder, videoCallback ->
            videoEncoder.start()

            FaultInjectingShadowMediaCodec.throwOnSetParameters = true
            FaultInjectingShadowMediaCodec.throwOnReleaseOutputBuffer = true
            val shadowCodec =
                Shadow.extract<FaultInjectingShadowMediaCodec>(videoEncoder.mMediaCodec)
            shadowCodec.emitOutputBuffer(
                index = 1,
                size = 16,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 1_000L,
                flags = 0,
            )
            idle()

            assertNoExceptionOnEncoderExecutor()
            assertThat(videoCallback.errors).hasSize(1)
            assertThat(videoCallback.errors.single().errorType)
                .isEqualTo(EncodeException.ERROR_CODEC)
            assertThat(
                    ShadowLog.getLogsForTag("VideoEncoder").none {
                        it.msg.contains("Get more than one error")
                    }
                )
                .isTrue()
        }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun videoStartResumeInputSurfaceSetParametersIllegalStateException_notifiesErrorAndResets() =
        withVideoEncoder { videoEncoder, videoCallback ->
            FaultInjectingShadowMediaCodec.throwOnSetParameters = true

            videoEncoder.start()
            idle()

            assertNoExceptionOnEncoderExecutor()
            assertThat(videoCallback.errors).hasSize(1)
            assertThat(videoCallback.errors.single().errorType)
                .isEqualTo(EncodeException.ERROR_CODEC)
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

            FaultInjectingShadowMediaCodec.throwOnSetParameters = false
            videoEncoder.start()
            idle()
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.STARTED)
        }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun constructorResetIllegalStateException_throwsInvalidConfigExceptionAndReleasesResources() {
        // 1. Fail in reset() before Surface allocation -> MediaCodec is still released.
        FaultInjectingShadowMediaCodec.throwOnReset = true
        assertThrows(InvalidConfigException::class.java) { createVideoEncoder() }
        assertThat(FaultInjectingShadowMediaCodec.releaseCount).isEqualTo(1)

        // 2. Fail in setInputSurface() after persistent Surface allocation -> both released, and
        // any async onError arriving on the registered callback does not throw on null mState.
        FaultInjectingShadowMediaCodec.resetFlags()
        FaultInjectingShadowMediaCodec.throwOnSetInputSurface = true
        assertThrows(InvalidConfigException::class.java) { createVideoEncoder() }
        assertThat(FaultInjectingShadowMediaCodec.releaseCount).isEqualTo(1)
        assertThat(FaultInjectingShadowMediaCodec.lastCreatedPersistentSurface?.isValid).isFalse()
        val failedShadow = FaultInjectingShadowMediaCodec.lastCreatedShadow
        assertThat(failedShadow).isNotNull()
        failedShadow!!.emitErrorTo(failedShadow.capturedCallback)
        idle()
        assertNoExceptionOnEncoderExecutor()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun startAndResetIllegalStateException_staysConfiguredAndRecoversOnRetry() {
        FaultInjectingShadowMediaCodec.throwOnStart = true
        FaultInjectingShadowMediaCodec.throwOnReset = true
        encoder.start()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

        // Next start() while still broken reports a fresh ERROR_CODEC (never hangs).
        encoder.start()
        idle()
        assertThat(encoderCallback.errors).hasSize(2)

        // Once transient fault clears, start() re-runs reset() and encodes normally.
        FaultInjectingShadowMediaCodec.resetFlags()
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun errorAndResetIllegalStateException_skipsPreReleaseStartAndCompletesRelease() =
        withVideoEncoder { videoEncoder, videoCallback ->
            videoEncoder.start()
            FaultInjectingShadowMediaCodec.throwOnSetParameters = true
            FaultInjectingShadowMediaCodec.throwOnReset = true

            videoEncoder.requestKeyFrame()
            idle()

            assertNoExceptionOnEncoderExecutor()
            assertThat(videoCallback.errors).hasSize(1)
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

            videoEncoder.start()
            idle()
            assertThat(videoCallback.errors).hasSize(2)

            val startCountBeforeRelease = FaultInjectingShadowMediaCodec.startCount
            videoEncoder.release()
            idle()
            assertThat(videoEncoder.releasedFuture.isDone).isTrue()
            assertThat(FaultInjectingShadowMediaCodec.startCount).isEqualTo(startCountBeforeRelease)
        }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun resetIllegalStateExceptionAfterStop_recoversOnRestartAndDeactivatesInputOnRelease() {
        encoder.start()
        feedInput(5)
        FaultInjectingShadowMediaCodec.throwOnReset = true
        encoder.stop()
        feedInputUntil { encoderCallback.stopCount == 1 }
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
        assertThat(
                (encoder.input as Encoder.ByteBufferInput).fetchData().get(0, TimeUnit.MILLISECONDS)
            )
            .isEqualTo(BufferProvider.State.INACTIVE)

        FaultInjectingShadowMediaCodec.throwOnReset = false
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()

        // STOPPING -> release() (PENDING_RELEASE) -> spontaneous EOS -> ByteBufferInput becomes
        // INACTIVE on RELEASED.
        encoder.stop()
        encoder.release()
        val shadowCodec = Shadow.extract<FaultInjectingShadowMediaCodec>(encoder.mMediaCodec)
        shadowCodec.emitOutputBuffer(
            index = 1,
            size = 0,
            presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
            flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
        )
        idle()
        assertThat(encoder.releasedFuture.isDone).isTrue()
        assertThat(
                (encoder.input as Encoder.ByteBufferInput).fetchData().get(0, TimeUnit.MILLISECONDS)
            )
            .isEqualTo(BufferProvider.State.INACTIVE)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun pendingStartResetIllegalStateException_notifiesErrorAndRecoversOnRetry() {
        encoder.start()
        feedInput(5)
        encoder.stop()
        encoder.start() // Transitions STOPPING -> PENDING_START
        FaultInjectingShadowMediaCodec.throwOnReset = true
        feedInputUntil { encoderCallback.errors.isNotEmpty() }
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

        FaultInjectingShadowMediaCodec.throwOnReset = false
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun pendingStartStopIllegalStateException_notifiesStopAndStartsNextSession() {
        encoder.start()
        feedInput(5)
        encoder.stop()
        encoder.start() // Transitions STOPPING -> PENDING_START
        FaultInjectingShadowMediaCodec.throwOnStop = true
        feedInputUntil { encoderCallback.stopCount == 1 }
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).isEmpty()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.STARTED)

        FaultInjectingShadowMediaCodec.throwOnStop = false
        encoderCallback.encodedData.clear()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun unexpectedEndOfStream_stopsDispatchingBuffersImmediatelyAndWaitsForPreEosBuffer() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val preEosInputBuffer = input.acquireBuffer().apply { idle() }.get(0, TimeUnit.MILLISECONDS)
        val shadowCodec = Shadow.extract<FaultInjectingShadowMediaCodec>(encoder.mMediaCodec)

        // Spontaneous EOS triggers reachEndData() -> stopMediaCodec(), which must immediately
        // stop dispatching new buffers while still waiting on preEosInputBuffer.
        shadowCodec.emitOutputBuffer(
            index = 1,
            size = 0,
            presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
            flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
        )
        idle()

        assertThat(input.fetchData().get(0, TimeUnit.MILLISECONDS))
            .isEqualTo(BufferProvider.State.INACTIVE)
        val postEosAcquire = input.acquireBuffer().apply { idle() }
        val thrown = assertThrows(ExecutionException::class.java) { postEosAcquire.get() }
        assertThat(thrown.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(encoderCallback.stopCount).isEqualTo(0)

        FaultInjectingShadowMediaCodec.throwOnQueueInputBuffer = true
        preEosInputBuffer.cancel()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(0)
        assertThat(encoderCallback.errors).hasSize(1)
        assertThat(encoderCallback.errors.single().errorType).isEqualTo(EncodeException.ERROR_CODEC)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

        FaultInjectingShadowMediaCodec.throwOnQueueInputBuffer = false
        encoderCallback.encodedData.clear()
        encoder.start()
        feedInput(5)
        assertThat(encoderCallback.encodedData).isNotEmpty()
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun unexpectedEndOfStreamDuringVideoStop_cancelsStopTimeoutAndIgnoresLateOnError() =
        withVideoEncoder { videoEncoder, videoCallback ->
            videoEncoder.start()
            videoEncoder.stop()
            val shadowCodec =
                Shadow.extract<FaultInjectingShadowMediaCodec>(videoEncoder.mMediaCodec)
            val oldCallback = shadowCodec.capturedCallback

            shadowCodec.emitOutputBuffer(
                index = 1,
                size = 0,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
            idle()

            assertThat(videoCallback.stopCount).isEqualTo(1)
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

            // Advancing past STOP_TIMEOUT_MS must not fire signalCodecStop() on the flushed codec.
            idle(1_000L)
            assertThat(videoCallback.errors).isEmpty()

            // A late onError from the previous MediaCodecCallback must be ignored after restart.
            videoEncoder.start()
            idle()
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.STARTED)
            shadowCodec.emitErrorTo(oldCallback)
            idle()
            assertThat(videoCallback.errors).isEmpty()
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.STARTED)
        }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun resumeWhileStoppingAfterUnexpectedEos_stopsWithoutRestarting() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val held = input.acquireBuffer().apply { idle() }.get(0, TimeUnit.MILLISECONDS)
        encoder.pause()
        idle()

        Shadow.extract<FaultInjectingShadowMediaCodec>(encoder.mMediaCodec)
            .emitOutputBuffer(
                index = 1,
                size = 0,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        idle()

        // Resuming during an unexpected-EOS stop must not restart on the old callback.
        encoder.start()
        idle()
        held.cancel()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).isEmpty()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun stopWhileStoppingAfterUnexpectedEos_cancelsTimeoutAndStopsOnce() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val held = input.acquireBuffer().apply { idle() }.get(0, TimeUnit.MILLISECONDS)

        Shadow.extract<FaultInjectingShadowMediaCodec>(encoder.mMediaCodec)
            .emitOutputBuffer(
                index = 1,
                size = 0,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        idle()

        // Calling stop() during an unexpected-EOS stop must not arm a new stop timeout.
        encoder.stop()
        idle(1_000L)
        held.cancel()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).isEmpty()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun pauseWhileStoppingAfterUnexpectedEos_doesNotLeaveStalePauseRange() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val held = input.acquireBuffer().apply { idle() }.get(0, TimeUnit.MILLISECONDS)

        Shadow.extract<FaultInjectingShadowMediaCodec>(encoder.mMediaCodec)
            .emitOutputBuffer(
                index = 1,
                size = 0,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        idle()

        // Pausing during an unexpected-EOS stop must be a no-op rather than adding a pause range.
        encoder.pause()
        idle()
        assertThat(encoder.mActivePauseResumeTimeRanges).isEmpty()

        held.cancel()
        idle()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun requestKeyFrameWhileStoppingAfterUnexpectedEos_isIgnoredAndStopsOnce() {
        encoder.start()
        idle()
        val input = encoder.input as Encoder.ByteBufferInput
        val held = input.acquireBuffer().apply { idle() }.get(0, TimeUnit.MILLISECONDS)

        Shadow.extract<FaultInjectingShadowMediaCodec>(encoder.mMediaCodec)
            .emitOutputBuffer(
                index = 1,
                size = 0,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()) + 10_000L,
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        idle()

        // Requesting a key frame during an unexpected-EOS stop must be a no-op rather than
        // calling setParameters() on a stopping codec and turning stop into an error.
        FaultInjectingShadowMediaCodec.throwOnSetParameters = true
        encoder.requestKeyFrame()
        idle()
        held.cancel()
        idle()

        assertNoExceptionOnEncoderExecutor()
        assertThat(encoderCallback.stopCount).isEqualTo(1)
        assertThat(encoderCallback.errors).isEmpty()
        assertThat(encoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
    }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun videoSignalEndOfInputStreamAndReleaseExceptions_notifyErrorAndCompleteRelease() =
        withVideoEncoder { videoEncoder, videoCallback ->
            videoEncoder.start()
            videoEncoder.stop()
            FaultInjectingShadowMediaCodec.throwOnSignalEndOfInputStream = true
            idle(1_000L)

            assertNoExceptionOnEncoderExecutor()
            assertThat(videoCallback.errors).hasSize(1)
            assertThat(videoCallback.errors.single().errorType)
                .isEqualTo(EncodeException.ERROR_CODEC)
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)

            FaultInjectingShadowMediaCodec.throwOnSignalEndOfInputStream = false
            FaultInjectingShadowMediaCodec.throwOnRelease = true
            videoEncoder.release()
            idle()

            assertNoExceptionOnEncoderExecutor()
            assertThat(FaultInjectingShadowMediaCodec.lastCreatedPersistentSurface?.isValid)
                .isFalse()
            assertThat(videoEncoder.mState).isEqualTo(EncoderImpl.InternalState.RELEASED)
            assertThat(videoEncoder.releasedFuture.isDone).isTrue()
        }

    @Test
    @Config(sdk = [Config.TARGET_SDK], shadows = [FaultInjectingShadowMediaCodec::class])
    fun cancelledAcquisitionCancelIllegalStateException_notifiesEncodeErrorDuringStopping() {
        val tasks = ArrayDeque<Runnable>()
        val queuedExecutor = Executor { tasks.addLast(it) }
        fun drainTasks() {
            while (tasks.isNotEmpty()) {
                tasks.removeFirst().run()
            }
        }
        val queuedCallback = FakeEncoderCallback()
        val queuedEncoder =
            EncoderImpl(
                queuedExecutor,
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
        try {
            queuedEncoder.setEncoderCallback(queuedCallback, CameraXExecutors.directExecutor())
            queuedEncoder.start()
            drainTasks()
            idle()
            drainTasks()

            val input = queuedEncoder.input as Encoder.ByteBufferInput
            val heldInputs = mutableListOf<InputBuffer>()
            lateinit var pendingAcquire: ListenableFuture<InputBuffer>
            while (true) {
                val future = input.acquireBuffer()
                drainTasks()
                idle()
                drainTasks()
                if (future.isDone) {
                    heldInputs.add(future.get(0, TimeUnit.MILLISECONDS))
                } else {
                    pendingAcquire = future
                    break
                }
            }

            queuedEncoder.stop()
            drainTasks()

            // Submitting one buffer schedules onOutputBufferAvailable; running that task releases
            // the output buffer so the next idle() queues onInputBufferAvailable before
            // pendingAcquire.cancel(true) queues mAcquisitionQueue.remove(completer).
            heldInputs.first().apply {
                byteBuffer.put(ByteArray(BUFFER_SIZE / 2)).flip()
                setPresentationTimeUs(TimeUnit.NANOSECONDS.toMicros(System.nanoTime()))
                submit()
            }
            idle()
            tasks.removeFirst().run()
            idle()
            pendingAcquire.cancel(true)
            FaultInjectingShadowMediaCodec.throwOnQueueInputBuffer = true
            drainTasks()
            heldInputs.drop(1).forEach { it.cancel() }
            drainTasks()

            assertNoExceptionOnEncoderExecutor()
            assertThat(queuedCallback.errors).hasSize(1)
            assertThat(queuedCallback.errors.single().errorType)
                .isEqualTo(EncodeException.ERROR_CODEC)
            assertThat(queuedEncoder.mState).isEqualTo(EncoderImpl.InternalState.CONFIGURED)
        } finally {
            FaultInjectingShadowMediaCodec.resetFlags()
            queuedEncoder.release()
            drainTasks()
        }
    }

    private fun drainAvailableInputBuffers(
        input: Encoder.ByteBufferInput
    ): Pair<List<InputBuffer>, ListenableFuture<InputBuffer>> {
        val heldInputs = mutableListOf<InputBuffer>()
        while (true) {
            val future = input.acquireBuffer()
            idle()
            if (future.isDone) {
                heldInputs.add(future.get(0, TimeUnit.MILLISECONDS))
            } else {
                return heldInputs to future
            }
        }
    }

    private fun submitAndCancelRemaining(heldInputs: List<InputBuffer>) {
        heldInputs.first().apply {
            byteBuffer.put(ByteArray(BUFFER_SIZE / 2)).flip()
            setPresentationTimeUs(TimeUnit.NANOSECONDS.toMicros(System.nanoTime()))
            submit()
        }
        heldInputs.drop(1).forEach { it.cancel() }
        idle()
    }

    private fun withVideoEncoder(block: (EncoderImpl, FakeEncoderCallback) -> Unit) {
        val videoEncoder = createVideoEncoder()
        try {
            val videoCallback = FakeEncoderCallback()
            videoEncoder.setEncoderCallback(videoCallback, CameraXExecutors.directExecutor())
            block(videoEncoder, videoCallback)
        } finally {
            FaultInjectingShadowMediaCodec.resetFlags()
            videoEncoder.release()
            idle()
        }
    }

    private fun createVideoEncoder(): EncoderImpl {
        ShadowMediaCodecList.reset()
        ShadowMediaCodecList.addCodec(
            MediaCodecInfoBuilder.newBuilder()
                .setName("fake.avc.encoder")
                .setIsEncoder(true)
                .setCapabilities(
                    MediaCodecInfoBuilder.CodecCapabilitiesBuilder.newBuilder()
                        .setMediaFormat(MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, 640, 480))
                        .setIsEncoder(true)
                        .setColorFormats(
                            intArrayOf(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                        )
                        .build()
                )
                .build()
        )
        return EncoderImpl(
            CameraXExecutors.directExecutor(),
            VideoEncoderConfig.builder()
                .setMimeType(MIMETYPE_VIDEO_AVC)
                .setInputTimebase(Timebase.UPTIME)
                .setResolution(Size(640, 480))
                .setColorFormat(MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                .setDataSpace(VideoEncoderDataSpace.ENCODER_DATA_SPACE_UNSPECIFIED)
                .setCaptureFrameRate(30)
                .setEncodeFrameRate(30)
                .setIFrameInterval(1)
                .setBitrate(1_000_000)
                .build(),
        )
    }

    @Implements(MediaCodec::class)
    class FaultInjectingShadowMediaCodec : ShadowMediaCodec() {
        @RealObject private lateinit var realMediaCodec: MediaCodec
        var capturedCallback: MediaCodec.Callback? = null

        companion object {
            var throwOnGetInputBuffer = false
            var throwOnGetOutputBuffer = false
            var throwOnReleaseOutputBuffer = false
            var throwOnQueueInputBuffer = false
            var throwOnStart = false
            var throwOnStop = false
            var throwOnFlush = false
            var throwOnSetParameters = false
            var throwOnReset = false
            var throwOnSetInputSurface = false
            var throwOnSignalEndOfInputStream = false
            var throwOnRelease = false
            var startCount = 0
            var releaseCount = 0
            var lastCreatedShadow: FaultInjectingShadowMediaCodec? = null
            var lastCreatedPersistentSurface: Surface? = null
            private var lastCreatedSurfaceTexture: SurfaceTexture? = null

            fun resetFlags() {
                throwOnGetInputBuffer = false
                throwOnGetOutputBuffer = false
                throwOnReleaseOutputBuffer = false
                throwOnQueueInputBuffer = false
                throwOnStart = false
                throwOnStop = false
                throwOnFlush = false
                throwOnSetParameters = false
                throwOnReset = false
                throwOnSetInputSurface = false
                throwOnSignalEndOfInputStream = false
                throwOnRelease = false
                startCount = 0
                releaseCount = 0
                lastCreatedShadow = null
                lastCreatedPersistentSurface = null
                lastCreatedSurfaceTexture?.release()
                lastCreatedSurfaceTexture = null
            }

            @Implementation
            @JvmStatic
            fun createPersistentInputSurface(): Surface =
                Surface(SurfaceTexture(0).also { lastCreatedSurfaceTexture = it }).also {
                    lastCreatedPersistentSurface = it
                }
        }

        @Implementation
        public override fun native_setCallback(callback: MediaCodec.Callback?) {
            this.capturedCallback = callback
            lastCreatedShadow = this
            super.native_setCallback(callback)
        }

        @Implementation
        public fun setInputSurface(surface: Surface) {
            if (throwOnSetInputSurface) {
                throw IllegalStateException("Simulated setInputSurface IllegalStateException")
            }
        }

        @Implementation
        public fun release() {
            releaseCount++
            if (throwOnRelease) {
                throw IllegalStateException("Simulated release IllegalStateException")
            }
            Shadow.directlyOn<Any?, MediaCodec>(realMediaCodec, MediaCodec::class.java, "release")
        }

        @Implementation
        public fun signalEndOfInputStream() {
            if (throwOnSignalEndOfInputStream) {
                throw IllegalStateException(
                    "Simulated signalEndOfInputStream IllegalStateException"
                )
            }
            emitOutputBuffer(
                // Buffer index 0 is already queued by ShadowMediaCodec.native_start(), so use 1
                // to avoid ShadowMediaCodec.releaseOutputBuffer throwing on double-queue.
                index = 1,
                size = 0,
                presentationTimeUs = TimeUnit.NANOSECONDS.toMicros(System.nanoTime()),
                flags = MediaCodec.BUFFER_FLAG_END_OF_STREAM,
            )
        }

        fun emitOutputBuffer(index: Int, size: Int, presentationTimeUs: Long, flags: Int) {
            capturedCallback?.onOutputBufferAvailable(
                realMediaCodec,
                index,
                MediaCodec.BufferInfo().apply { set(0, size, presentationTimeUs, flags) },
            )
        }

        fun emitErrorTo(targetCallback: MediaCodec.Callback?) {
            targetCallback?.onError(
                realMediaCodec,
                Shadow.newInstanceOf(MediaCodec.CodecException::class.java),
            )
        }

        @Implementation
        public override fun getBuffer(input: Boolean, index: Int): ByteBuffer? {
            if ((input && throwOnGetInputBuffer) || (!input && throwOnGetOutputBuffer)) {
                throw IllegalStateException("Simulated MediaCodec.getBuffer IllegalStateException")
            }
            return super.getBuffer(input, index)
        }

        @Implementation
        public override fun releaseOutputBuffer(index: Int, render: Boolean) {
            if (throwOnReleaseOutputBuffer) {
                throw IllegalStateException("Simulated releaseOutputBuffer IllegalStateException")
            }
            super.releaseOutputBuffer(index, render)
        }

        @Implementation
        public override fun native_queueInputBuffer(
            index: Int,
            offset: Int,
            size: Int,
            presentationTimeUs: Long,
            flags: Int,
        ) {
            if (throwOnQueueInputBuffer) {
                throw IllegalStateException("Simulated queueInputBuffer IllegalStateException")
            }
            super.native_queueInputBuffer(index, offset, size, presentationTimeUs, flags)
        }

        @Implementation
        public override fun native_start() {
            startCount++
            if (throwOnStart) {
                throw IllegalStateException("Simulated start IllegalStateException")
            }
            super.native_start()
        }

        @Implementation
        public fun stop() {
            if (throwOnStop) {
                throw IllegalStateException("Simulated stop IllegalStateException")
            }
            Shadow.directlyOn<Any?, MediaCodec>(realMediaCodec, MediaCodec::class.java, "stop")
        }

        @Implementation
        public override fun native_flush() {
            if (throwOnFlush) {
                throw IllegalStateException("Simulated flush IllegalStateException")
            }
            super.native_flush()
        }

        @Implementation
        public fun reset() {
            if (throwOnReset) {
                throw IllegalStateException("Simulated reset IllegalStateException")
            }
            Shadow.directlyOn<Any?, MediaCodec>(realMediaCodec, MediaCodec::class.java, "reset")
        }

        @Implementation
        public fun setParameters(params: Bundle?) {
            if (throwOnSetParameters) {
                throw IllegalStateException("Simulated setParameters IllegalStateException")
            }
            Shadow.directlyOn<Any?, MediaCodec>(
                realMediaCodec,
                MediaCodec::class.java,
                "setParameters",
                ClassParameter.from(Bundle::class.java, params),
            )
        }
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
