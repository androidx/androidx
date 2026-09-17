/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.testing.impl.fakes

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import androidx.camera.video.internal.encoder.EncodeException
import androidx.camera.video.internal.encoder.EncodedData
import androidx.camera.video.internal.encoder.Encoder
import androidx.camera.video.internal.encoder.EncoderCallback
import androidx.camera.video.internal.encoder.EncoderConfig
import androidx.camera.video.internal.encoder.EncoderInfo
import androidx.camera.video.internal.encoder.VideoEncoderConfig
import androidx.camera.video.internal.encoder.VideoEncoderInfo
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import java.nio.ByteBuffer
import java.util.concurrent.Executor

public class FakeEncoder(
    encoderInput: Encoder.EncoderInput? = null,
    private val encoderInfo: EncoderInfo = FakeVideoEncoderInfo(),
    encoderConfig: EncoderConfig? = null,
    private val configuredBitrate: Int =
        when (encoderConfig) {
            is VideoEncoderConfig ->
                (encoderInfo as? VideoEncoderInfo)
                    ?.supportedBitrateRange
                    ?.clamp(encoderConfig.bitrate) ?: encoderConfig.bitrate
            else -> 0
        },
    public val releasedFuture: ResolvableFuture<Void?> = ResolvableFuture.create(),
    private val onStateChanged: ((Boolean) -> Unit)? = null,
) : Encoder {

    private var internalSurfaceTexture: SurfaceTexture? = null
    private var internalSurface: Surface? = null
    private val encoderInput: Encoder.EncoderInput =
        encoderInput
            ?: run {
                val surfaceTexture = SurfaceTexture(0).also { internalSurfaceTexture = it }
                val surface = Surface(surfaceTexture).also { internalSurface = it }
                FakeEncoderSurfaceInput(surface)
            }

    public var isReleaseCalled: Boolean = false
    public var isStarted: Boolean = false
        private set

    public var isPaused: Boolean = false
        private set

    private var encoderCallback: EncoderCallback? = null
    private var callbackExecutor: Executor? = null

    override fun getInput(): Encoder.EncoderInput = encoderInput

    override fun getEncoderInfo(): EncoderInfo = encoderInfo

    override fun getConfiguredBitrate(): Int = configuredBitrate

    override fun start(expectedStartTimeUs: Long) {
        isStarted = true
        isPaused = false
        onStateChanged?.invoke(true)
        val callback = encoderCallback ?: return
        val executor = callbackExecutor ?: return
        executor.execute {
            callback.onEncodeStart()
            callback.onOutputConfigUpdate { MediaFormat() }
        }
    }

    override fun stop(expectedStopTimeUs: Long) {
        isStarted = false
        isPaused = false
        onStateChanged?.invoke(false)
        val callback = encoderCallback ?: return
        val executor = callbackExecutor ?: return
        executor.execute { callback.onEncodeStop() }
    }

    override fun pause(expectedPauseTimeUs: Long) {
        isPaused = true
        onStateChanged?.invoke(false)
        val callback = encoderCallback ?: return
        val executor = callbackExecutor ?: return
        executor.execute { callback.onEncodePaused() }
    }

    override fun release() {
        isStarted = false
        isPaused = false
        isReleaseCalled = true
        onStateChanged?.invoke(false)
        internalSurface?.release()
        internalSurfaceTexture?.release()
        releasedFuture.set(null)
    }

    override fun getReleasedFuture(): ListenableFuture<Void?> = releasedFuture

    override fun setEncoderCallback(encoderCallback: EncoderCallback, executor: Executor) {
        this.encoderCallback = encoderCallback
        this.callbackExecutor = executor
    }

    override fun requestKeyFrame() {}

    public fun triggerEncodeError(errorType: Int, message: String, cause: Throwable? = null) {
        val callback = encoderCallback ?: return
        val executor = callbackExecutor ?: return
        executor.execute { callback.onEncodeError(EncodeException(errorType, message, cause)) }
    }

    public fun sendEncodedData(
        size: Int = 1024,
        presentationTimeUs: Long = 0L,
        isKeyFrame: Boolean = true,
    ) {
        if (!isStarted || isPaused) return
        val callback = encoderCallback ?: return
        val executor = callbackExecutor ?: return
        val bufferInfo =
            MediaCodec.BufferInfo().apply {
                set(
                    0,
                    size,
                    presentationTimeUs,
                    if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0,
                )
            }
        val byteBuffer = ByteBuffer.allocateDirect(size)
        val encodedData =
            object : EncodedData {
                private val closedFuture = ResolvableFuture.create<Void?>()

                override fun getByteBuffer(): ByteBuffer = byteBuffer

                override fun getBufferInfo(): MediaCodec.BufferInfo = bufferInfo

                override fun getPresentationTimeUs(): Long = bufferInfo.presentationTimeUs

                override fun size(): Long = bufferInfo.size.toLong()

                override fun isKeyFrame(): Boolean =
                    (bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0

                override fun close() {
                    closedFuture.set(null)
                }

                override fun getClosedFuture(): ListenableFuture<Void?> = closedFuture
            }
        executor.execute {
            if (isStarted && !isPaused) {
                callback.onEncodedData(encodedData)
            }
        }
    }
}
