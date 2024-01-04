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

import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.MultiBufferedCanvasRenderer
import androidx.graphics.RenderQueue
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor

@RequiresApi(Build.VERSION_CODES.Q)
internal class SingleBufferedCanvasRendererV29<T>(
    private val width: Int,
    private val height: Int,
    private val bufferTransformer: BufferTransformer,
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

    private val mTransform = android.graphics.Matrix().apply {
        when (bufferTransformer.computedTransform) {
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_90 -> {
                setRotate(270f)
                postTranslate(0f, width.toFloat())
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_180 -> {
                setRotate(180f)
                postTranslate(width.toFloat(), height.toFloat())
            }
            SurfaceControlCompat.BUFFER_TRANSFORM_ROTATE_270 -> {
                setRotate(90f)
                postTranslate(height.toFloat(), 0f)
            }
            else -> {
                reset()
            }
        }
    }

    private val mBufferedRenderer = MultiBufferedCanvasRenderer(
        RenderNode("renderNode").apply {
            setPosition(
                0,
                0,
                bufferTransformer.glWidth,
                bufferTransformer.glHeight)
        },
        bufferTransformer.glWidth,
        bufferTransformer.glHeight,
        usage = FrontBufferUtils.obtainHardwareBufferUsageFlags(),
        maxImages = 1
    )

    private val mPendingParams = ArrayList<T>()

    private inner class DrawParamRequest(val param: T) : RenderQueue.Request {

        override fun onEnqueued() {
            mPendingParams.add(param)
        }

        override fun execute() {
            mBufferedRenderer.record { canvas ->
                canvas.save()
                canvas.setMatrix(mTransform)
                for (pendingParam in mPendingParams) {
                    callbacks.render(canvas, width, height, pendingParam)
                }
                canvas.restore()
                mPendingParams.clear()
            }
        }

        override val id: Int = RENDER
    }

    private val clearRequest = object : RenderQueue.Request {
        override fun execute() {
            mBufferedRenderer.record { canvas -> canvas.drawColor(Color.BLACK, BlendMode.CLEAR) }
        }

        override val id: Int = CLEAR
    }

    override var isVisible: Boolean = false
        set(value) {
            mBufferedRenderer.preserveContents = isVisible
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

    override fun clear() {
        mRenderQueue.enqueue(clearRequest)
    }

    override fun cancelPending() {
        mRenderQueue.cancelPending()
    }

    private companion object {
        const val RENDER = 0
        const val CLEAR = 1
    }
}
