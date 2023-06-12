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
import android.graphics.HardwareBufferRenderer
import android.graphics.RenderNode
import android.hardware.HardwareBuffer
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.annotation.RequiresApi
import androidx.hardware.SyncFenceCompat
import java.util.concurrent.Executor

@RequiresApi(34)
internal class SingleBufferedCanvasRendererV34<T>(
    private val width: Int,
    private val height: Int,
    private val bufferTransformer: BufferTransformer,
    private val executor: Executor,
    private val callbacks: SingleBufferedCanvasRenderer.RenderCallbacks<T>
) : SingleBufferedCanvasRenderer<T> {

    private val mRenderNode = RenderNode("node").apply {
        setPosition(
            0,
            0,
            width,
            height
        )
        clipToBounds = false
    }

    private val mInverseTransform =
        bufferTransformer.invertBufferTransform(bufferTransformer.computedTransform)
    private val mHandlerThread = HandlerThread("renderRequestThread").apply { start() }
    private val mHandler = Handler(mHandlerThread.looper)

    private inline fun dispatchOnExecutor(crossinline block: () -> Unit) {
        executor.execute {
            block()
        }
    }

    private inline fun doRender(block: (Canvas) -> Unit) {
        val canvas = mRenderNode.beginRecording()
        block(canvas)
        mRenderNode.endRecording()

        mHardwareBufferRenderer.obtainRenderRequest().apply {
            if (mInverseTransform != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                setBufferTransform(mInverseTransform)
            }
            draw(executor) { result ->
                callbacks.onBufferReady(mHardwareBuffer, SyncFenceCompat(result.fence))
            }
        }
    }

    private fun tearDown() {
        mHardwareBufferRenderer.close()
        mHandlerThread.quit()
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

    private var mIsReleasing = false

    override fun render(param: T) {
        if (!mIsReleasing) {
            mHandler.post(RENDER) {
                dispatchOnExecutor {
                    doRender { canvas ->
                        callbacks.render(canvas, width, height, param)
                    }
                }
            }
        }
    }

    override var isVisible: Boolean = false

    override fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)?) {
        if (!mIsReleasing) {
            if (cancelPending) {
                cancelPending()
            }
            mHandler.post(RELEASE) {
                tearDown()
                if (onReleaseComplete != null) {
                    dispatchOnExecutor {
                        onReleaseComplete.invoke()
                    }
                }
            }
            mIsReleasing = true
        }
    }

    override fun clear() {
        if (!mIsReleasing) {
            mHandler.post(CLEAR) {
                dispatchOnExecutor {
                    doRender { canvas ->
                        canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
                    }
                }
            }
        }
    }

    override fun cancelPending() {
        if (!mIsReleasing) {
            mHandler.removeCallbacksAndMessages(CLEAR)
            mHandler.removeCallbacksAndMessages(RENDER)
        }
    }

    private companion object {
        const val RENDER = 0
        const val CLEAR = 1
        const val RELEASE = 2
    }

    /**
     * Handler does not expose a post method that takes a token and a runnable.
     * We need the token to be able to cancel pending requests so just call
     * postAtTime with the default of SystemClock.uptimeMillis
     */
    private fun Handler.post(token: Any?, runnable: Runnable) {
        postAtTime(runnable, token, SystemClock.uptimeMillis())
    }
}