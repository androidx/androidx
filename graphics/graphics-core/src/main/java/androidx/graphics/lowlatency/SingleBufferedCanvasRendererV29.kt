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
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RenderNode
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.MultiBufferedCanvasRenderer
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.utils.post
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

@RequiresApi(Build.VERSION_CODES.Q)
internal class SingleBufferedCanvasRendererV29<T>(
    private val width: Int,
    private val height: Int,
    private val bufferTransformer: BufferTransformer,
    private val executor: Executor,
    private val callbacks: SingleBufferedCanvasRenderer.RenderCallbacks<T>,
) : SingleBufferedCanvasRenderer<T> {

    private val mRenderNode = RenderNode("renderNode").apply {
        setPosition(
            0,
            0,
            bufferTransformer.glWidth,
            bufferTransformer.glHeight)
    }
    private val mHandlerThread = HandlerThread("renderRequestThread").apply { start() }
    private val mHandler = Handler(mHandlerThread.looper)
    private var mIsReleasing = AtomicBoolean(false)

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
        mRenderNode,
        bufferTransformer.glWidth,
        bufferTransformer.glHeight,
        maxImages = 1
    )

    private inline fun dispatchOnExecutor(crossinline block: () -> Unit) {
        executor.execute {
            block()
        }
    }

    // Executor thread
    private var mPendingDraw = false
    private val mPendingParams = ArrayList<T>()
    private var mReleaseCallback: (() -> Unit)? = null

    @WorkerThread // Executor thread
    private inline fun draw(
        canvasOperations: (Canvas) -> Unit,
        noinline onDrawComplete: (() -> Unit) = {}
    ) {
        if (!mPendingDraw) {
            val canvas = mRenderNode.beginRecording()
            canvasOperations(canvas)
            mRenderNode.endRecording()
            mPendingDraw = true
            mBufferedRenderer.renderFrame(executor) { hardwareBuffer ->
                callbacks.onBufferReady(hardwareBuffer, null)
                mPendingDraw = false
                onDrawComplete.invoke()
            }
        }
    }

    @WorkerThread // Executor thread
    private fun doRender() {
        if (mPendingParams.isNotEmpty()) {
            draw(
                canvasOperations = { canvas ->
                    canvas.save()
                    canvas.setMatrix(mTransform)
                    for (pendingParam in mPendingParams) {
                        callbacks.render(canvas, width, height, pendingParam)
                    }
                    canvas.restore()
                    mPendingParams.clear()
                },
                onDrawComplete = {
                    // Render and teardown both early-return when `isPendingDraw == true`, so they
                    // need to be run again after draw completion if needed.
                    if (mPendingParams.isNotEmpty()) {
                        doRender()
                    } else if (mIsReleasing.get()) {
                        tearDown()
                    }
                }
            )
        }
    }

    private fun isPendingDraw() = mPendingDraw || mPendingParams.isNotEmpty()

    override var isVisible: Boolean = false

    @WorkerThread // Executor thread
    private fun tearDown() {
        mReleaseCallback?.invoke()
        mBufferedRenderer.release()
        mHandlerThread.quit()
    }

    override fun render(param: T) {
        if (!mIsReleasing.get()) {
            mHandler.post(RENDER) {
                dispatchOnExecutor {
                    mPendingParams.add(param)
                    doRender()
                }
            }
        }
    }

    override fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)?) {
        if (!mIsReleasing.get()) {
            if (cancelPending) {
                cancelPending()
            }
            mHandler.post(RELEASE) {
                dispatchOnExecutor {
                    mReleaseCallback = onReleaseComplete
                    if (cancelPending || !isPendingDraw()) {
                        tearDown()
                    }
                }
            }
            mIsReleasing.set(true)
        }
    }

    override fun clear() {
        if (!mIsReleasing.get()) {
            mHandler.post(CLEAR) {
                dispatchOnExecutor {
                    draw({ canvas ->
                        canvas.drawColor(Color.BLACK, BlendMode.CLEAR)
                    })
                }
            }
        }
    }

    override fun cancelPending() {
        if (!mIsReleasing.get()) {
            mHandler.removeCallbacksAndMessages(CLEAR)
            mHandler.removeCallbacksAndMessages(RENDER)
            dispatchOnExecutor { mPendingParams.clear() }
        }
    }

    private companion object {
        const val RENDER = 0
        const val CLEAR = 1
        const val RELEASE = 2
    }
}