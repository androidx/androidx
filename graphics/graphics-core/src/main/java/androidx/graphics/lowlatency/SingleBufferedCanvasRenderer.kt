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

package androidx.graphics.lowlatency

import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.CanvasBufferedRenderer
import androidx.graphics.RenderQueue
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.HardwareBufferFormat
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Class to provide an abstraction around implementations for a low latency hardware
 * accelerated [Canvas] that provides a [HardwareBuffer] with the [Canvas] rendered scene
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class SingleBufferedCanvasRenderer<T>(
    private val width: Int,
    private val height: Int,
    bufferWidth: Int,
    bufferHeight: Int,
    @HardwareBufferFormat bufferFormat: Int,
    private val transformHint: Int,
    handlerThread: HandlerThreadExecutor,
    private val callbacks: RenderCallbacks<T>
) {

    interface RenderCallbacks<T> {
        @WorkerThread
        fun render(canvas: Canvas, width: Int, height: Int, param: T)

        @WorkerThread
        fun onBufferReady(hardwareBuffer: HardwareBuffer, syncFenceCompat: SyncFenceCompat?)

        @WorkerThread
        fun onBufferCancelled(
            hardwareBuffer: HardwareBuffer,
            syncFenceCompat: SyncFenceCompat?
        ) {
            // NO-OP
        }
    }

    constructor(
        width: Int,
        height: Int,
        bufferTransformer: BufferTransformer,
        handlerThread: HandlerThreadExecutor,
        callbacks: RenderCallbacks<T>
    ) : this(
        width,
        height,
        bufferTransformer.bufferWidth,
        bufferTransformer.bufferHeight,
        HardwareBuffer.RGBA_8888,
        bufferTransformer.computedTransform,
        handlerThread,
        callbacks
    )

    private val mRenderNode = RenderNode("node").apply {
        setPosition(
            0,
            0,
            this@SingleBufferedCanvasRenderer.width,
            this@SingleBufferedCanvasRenderer.height
        )
    }

    private val mRenderQueue = RenderQueue(
        handlerThread,
        object : RenderQueue.FrameProducer {
            override fun renderFrame(
                executor: Executor,
                requestComplete: (HardwareBuffer, SyncFenceCompat?) -> Unit
            ) {
                mHardwareBufferRenderer.obtainRenderRequest().apply {
                    if (transformHint != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                        setBufferTransform(transformHint)
                    }
                    preserveContents(true)
                    setColorSpace(this@SingleBufferedCanvasRenderer.colorSpace)
                    drawAsync(executor) { result ->
                        requestComplete.invoke(result.hardwareBuffer, result.fence)
                    }
                }
            }
        },
        object : RenderQueue.FrameCallback {
            override fun onFrameComplete(
                hardwareBuffer: HardwareBuffer,
                fence: SyncFenceCompat?
            ) {
                callbacks.onBufferReady(hardwareBuffer, fence)
            }

            override fun onFrameCancelled(
                hardwareBuffer: HardwareBuffer,
                fence: SyncFenceCompat?
            ) {
                callbacks.onBufferCancelled(hardwareBuffer, fence)
            }
        }
    )

    private val mVisibleFlag = AtomicBoolean(false)

    private fun tearDown() {
        mHardwareBufferRenderer.close()
    }

    private val mHardwareBufferRenderer = CanvasBufferedRenderer.Builder(bufferWidth, bufferHeight)
        .setUsageFlags(FrontBufferUtils.obtainHardwareBufferUsageFlags())
        .setMaxBuffers(1)
        .setBufferFormat(bufferFormat)
        .build()
        .apply { setContentRoot(mRenderNode) }

    private val mPendingParams = ArrayList<T>()

    private inner class DrawParamRequest(val param: T) : RenderQueue.Request {

        override fun onEnqueued() {
            mPendingParams.add(param)
        }

        override fun execute() {
            val canvas = mRenderNode.beginRecording()
            for (pendingParam in mPendingParams) {
                callbacks.render(canvas, width, height, pendingParam)
            }
            mPendingParams.clear()
            mRenderNode.endRecording()
        }

        override fun onComplete() {
            // NO-OP
        }

        override val id: Int = RENDER
    }

    private inner class ClearRequest(val clearRequest: (() -> Unit)?) : RenderQueue.Request {
        override fun execute() {
            val canvas = mRenderNode.beginRecording()
            canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
            mRenderNode.endRecording()
        }

        override fun onComplete() {
            clearRequest?.invoke()
        }

        override fun isMergeable(): Boolean = clearRequest == null

        override val id: Int = CLEAR
    }

    private val defaultClearRequest = ClearRequest(null)

    /**
     * Render into the [HardwareBuffer] with the given parameter and bounds
     */
    fun render(param: T) {
        mRenderQueue.enqueue(DrawParamRequest(param))
    }

    /**
     * Flag to indicate whether or not the contents of the [SingleBufferedCanvasRenderer] are visible.
     * This is used to help internal state to determine appropriate synchronization
     */
    var isVisible: Boolean
        get() = mVisibleFlag.get()
        set(value) {
            mVisibleFlag.set(value)
        }

    /**
     * Configure the color space that the content is rendered with
     */
    var colorSpace: ColorSpace = CanvasBufferedRenderer.DefaultColorSpace

    /**
     * Releases resources associated with [SingleBufferedCanvasRenderer] instance. Attempts to
     * use this object after it is closed will be ignored
     */
    fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        mRenderQueue.release(cancelPending) {
            onReleaseComplete?.invoke()
            tearDown()
        }
    }

    /**
     * Clear the contents of the [HardwareBuffer]
     */
    fun clear(clearComplete: (() -> Unit)? = null) {
        val clearRequest = if (clearComplete == null) {
            defaultClearRequest
        } else {
            ClearRequest(clearComplete)
        }
        mRenderQueue.enqueue(clearRequest)
    }

    /**
     * Cancel all pending render requests
     */
    fun cancelPending() {
        mRenderQueue.cancelPending()
    }

    private companion object {
        const val RENDER = 0
        const val CLEAR = 1
    }
}
