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
import android.graphics.Color
import android.graphics.HardwareBufferRenderer
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import androidx.annotation.RequiresApi
import androidx.graphics.RenderQueue
import androidx.graphics.utils.HandlerThreadExecutor
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor

@RequiresApi(34)
internal class SingleBufferedCanvasRendererV34<T>(
    private val width: Int,
    private val height: Int,
    bufferTransformer: BufferTransformer,
    handlerThread: HandlerThreadExecutor,
    private val callbacks: SingleBufferedCanvasRenderer.RenderCallbacks<T>
) : SingleBufferedCanvasRenderer<T> {

    private val mRenderNode = RenderNode("node").apply {
        setPosition(
            0,
            0,
            this@SingleBufferedCanvasRendererV34.width,
            this@SingleBufferedCanvasRendererV34.height
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
                    if (mInverseTransform != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                        setBufferTransform(mInverseTransform)
                    }
                    draw(executor) { result ->
                        requestComplete.invoke(mHardwareBuffer, SyncFenceCompat(result.fence))
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

    private val mInverseTransform =
        bufferTransformer.invertBufferTransform(bufferTransformer.computedTransform)

    private fun tearDown() {
        mHardwareBufferRenderer.close()
    }

    private val mHardwareBuffer = HardwareBuffer.create(
        bufferTransformer.glWidth,
        bufferTransformer.glHeight,
        HardwareBuffer.RGBA_8888,
        1,
        FrontBufferUtils.obtainHardwareBufferUsageFlags()
    )

    private val mHardwareBufferRenderer = HardwareBufferRenderer(mHardwareBuffer).apply {
        setContentRoot(mRenderNode)
    }

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

        override val id: Int = RENDER
    }

    private val clearRequest = object : RenderQueue.Request {
        override fun execute() {
            val canvas = mRenderNode.beginRecording()
            canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
            mRenderNode.endRecording()
        }

        override val id: Int = CLEAR
    }

    override fun render(param: T) {
        mRenderQueue.enqueue(DrawParamRequest(param))
    }

    override var isVisible: Boolean = false

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
