/*
 * Copyright 2022 The Android Open Source Project
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

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.hardware.HardwareBuffer
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.BufferedRendererImpl
import androidx.graphics.MultiBufferedCanvasRenderer
import androidx.graphics.RenderQueue
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.Q)
internal class SingleBufferedCanvasRendererV29<T>(
    private val width: Int,
    private val height: Int,
    bufferTransformer: BufferTransformer,
    handlerThread: HandlerThreadExecutor,
    private val callbacks: SingleBufferedCanvasRenderer.RenderCallbacks<T>,
) : SingleBufferedCanvasRenderer<T> {

    private val mRenderQueue = RenderQueue(
            handlerThread,
            object : RenderQueue.FrameProducer {
                override fun renderFrame(
                    executor: Executor,
                    requestComplete: (HardwareBuffer, SyncFenceCompat?) -> Unit
                ) {
                    mBufferedRenderer.renderFrame(executor, requestComplete)
                }
            },
            object : RenderQueue.FrameCallback {
                override fun onFrameComplete(
                    hardwareBuffer: HardwareBuffer,
                    fence: SyncFenceCompat?
                ) {
                    mBufferedRenderer.releaseBuffer(hardwareBuffer, fence)
                    mPreservedRenderStrategy.onRenderComplete(hardwareBuffer, fence, colorSpace)
                    callbacks.onBufferReady(hardwareBuffer, fence)
                }

                override fun onFrameCancelled(
                    hardwareBuffer: HardwareBuffer,
                    fence: SyncFenceCompat?
                ) {
                    mBufferedRenderer.releaseBuffer(hardwareBuffer, fence)
                    mPreservedRenderStrategy.onRenderComplete(hardwareBuffer, fence, colorSpace)
                    callbacks.onBufferCancelled(hardwareBuffer, fence)
                }
            }
        )

    private val mPreservedRenderStrategy = createPreservationStrategy(bufferTransformer)

    private val mBufferedRenderer = MultiBufferedCanvasRenderer(
        width,
        height,
        bufferTransformer,
        usage = FrontBufferUtils.obtainHardwareBufferUsageFlags(),
        maxImages = mPreservedRenderStrategy.maxImages
    )

    private val mPendingParams = ArrayList<T>()

    private inner class DrawParamRequest(val param: T) : RenderQueue.Request {

        override fun onEnqueued() {
            mPendingParams.add(param)
        }

        override fun execute() {
            mBufferedRenderer.record { canvas ->
                mPreservedRenderStrategy.restoreContents(canvas, width, height)
                for (pendingParam in mPendingParams) {
                    callbacks.render(canvas, width, height, pendingParam)
                }
                mPendingParams.clear()
            }
        }

        override fun onComplete() {
            // NO-OP
        }

        override val id: Int = RENDER
    }

    private inner class ClearRequest(val clearComplete: (() -> Unit)?) : RenderQueue.Request {
        override fun execute() {
            mBufferedRenderer.record { canvas -> canvas.drawColor(Color.BLACK, BlendMode.CLEAR) }
        }

        override fun onComplete() {
            clearComplete?.invoke()
        }

        override fun isMergeable(): Boolean = clearComplete == null

        override val id: Int = CLEAR
    }

    private val defaultClearRequest = ClearRequest(null)

    override var isVisible: Boolean = false
        set(value) {
            mBufferedRenderer.preserveContents = value
            field = value
        }

    @WorkerThread // Executor thread
    private fun tearDown() {
        mBufferedRenderer.release()
    }

    override fun render(param: T) {
        mRenderQueue.enqueue(DrawParamRequest(param))
    }

    override fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)?) {
        mRenderQueue.release(cancelPending) {
            onReleaseComplete?.invoke()
            tearDown()
        }
    }

    override fun clear(clearComplete: (() -> Unit)?) {
        val clearRequest = if (clearComplete == null) {
            defaultClearRequest
        } else {
            ClearRequest(clearComplete)
        }
        mRenderQueue.enqueue(clearRequest)
    }

    override fun cancelPending() {
        mRenderQueue.cancelPending()
    }

    override var colorSpace: ColorSpace
        get() = mBufferedRenderer.colorSpace
        set(value) { mBufferedRenderer.colorSpace = value }

    private companion object {

        val TAG = "PersistedCanvas"

        const val RENDER = 0
        const val CLEAR = 1

        fun createPreservationStrategy(
            bufferTransformer: BufferTransformer
        ): PreservedRenderStrategy {
            val verifier = PreservedBufferContentsVerifier()
            val supportsContentPreservation = verifier.supportsPreservedRenderedContent()
            verifier.release()
            return if (supportsContentPreservation) {
                Log.v(TAG, "Device supports persisted canvas optimizations")
                SingleBufferedStrategy()
            } else {
                Log.w(TAG,
                    "Warning, device DOES NOT support persisted canvas optimizations.")
                RedrawBufferStrategy(bufferTransformer)
            }
        }
    }

    internal interface PreservedRenderStrategy {
        val maxImages: Int

        fun restoreContents(canvas: Canvas, width: Int, height: Int)

        fun onRenderComplete(
            hardwareBuffer: HardwareBuffer,
            fence: SyncFenceCompat?,
            colorSpace: ColorSpace
        )
    }

    internal class SingleBufferedStrategy : PreservedRenderStrategy {
        override val maxImages = 1

        override fun restoreContents(canvas: Canvas, width: Int, height: Int) {
            // NO-OP HWUI preserves contents
        }

        override fun onRenderComplete(
            hardwareBuffer: HardwareBuffer,
            fence: SyncFenceCompat?,
            colorSpace: ColorSpace
        ) {
            // NO-OP
        }
    }

    internal class RedrawBufferStrategy(
        bufferTransformer: BufferTransformer
    ) : PreservedRenderStrategy {

        private val inverseTransform = android.graphics.Matrix().apply {
            bufferTransformer.configureMatrix(this)
            invert(this)
        }

        override val maxImages: Int = 2

        private var mHardwareBuffer: HardwareBuffer? = null
        private var mFence: SyncFenceCompat? = null
        private var mColorSpace: ColorSpace = BufferedRendererImpl.DefaultColorSpace

        override fun restoreContents(canvas: Canvas, width: Int, height: Int) {
            mHardwareBuffer?.let { buffer ->
                mFence?.awaitForever()
                val bitmap = Bitmap.wrapHardwareBuffer(buffer, mColorSpace)
                if (bitmap != null) {
                    canvas.save()
                    canvas.concat(inverseTransform)
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    canvas.restore()
                }
            }
        }

        override fun onRenderComplete(
            hardwareBuffer: HardwareBuffer,
            fence: SyncFenceCompat?,
            colorSpace: ColorSpace
        ) {
            mHardwareBuffer = hardwareBuffer
            mFence = fence
            mColorSpace = colorSpace
        }
    }
}
