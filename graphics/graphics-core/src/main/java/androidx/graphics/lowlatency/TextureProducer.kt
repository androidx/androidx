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

import android.graphics.Canvas
import android.graphics.RenderNode
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import androidx.annotation.AnyThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.SurfaceTextureRenderer
import androidx.graphics.utils.post

/**
 * Class responsible for the producing side of SurfaceTextures that are rendered with content
 * provided from a canvas. This class handles proxying all requests to an internal thread
 * as well as throttles production of frames based on consumption rate.
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class TextureProducer<T>(
    val width: Int,
    val height: Int,
    val callbacks: Callbacks<T>
) {

    interface Callbacks<T> {
        fun onTextureAvailable(texture: SurfaceTexture)

        fun render(canvas: Canvas, width: Int, height: Int, param: T)
    }

    private var mIsReleasing = false
    private val mParams = ArrayList<T>()
    private var mPendingRenders = 0
    private val mProducerThread = HandlerThread("producerThread").apply { start() }
    private val mProducerHandler = Handler(mProducerThread.looper)

    private val mCancelPendingRunnable = Runnable {
        mParams.clear()
    }

    @WorkerThread // ProducerThread
    private fun teardown(releaseCallback: (() -> Unit)? = null) {
        releaseCallback?.invoke()
        mSurfaceTextureRenderer.release()
        mProducerThread.quit()
    }

    @WorkerThread // ProducerThread
    private fun isPendingRendering() = mParams.isNotEmpty() || mPendingRenders > 0

    private val mRenderNode = RenderNode("node").apply {
        setPosition(
            0,
            0,
            this@TextureProducer.width,
            this@TextureProducer.height
        )
    }

    private inline fun RenderNode.record(block: (Canvas) -> Unit) {
        val canvas = beginRecording()
        block(canvas)
        endRecording()
    }

    private val mSurfaceTextureRenderer = SurfaceTextureRenderer(
        mRenderNode,
        width,
        height,
        mProducerHandler
    ) { texture ->
        callbacks.onTextureAvailable(texture)
    }

    @WorkerThread // ProducerThread
    private fun doRender() {
        if (mPendingRenders < MAX_PENDING_RENDERS) {
            if (mParams.isNotEmpty()) {
                mRenderNode.record { canvas ->
                    for (p in mParams) {
                        callbacks.render(canvas, width, height, p)
                    }
                }
                mParams.clear()
                mPendingRenders++
                mSurfaceTextureRenderer.renderFrame()
            }
        }
    }

    @AnyThread
    fun requestRender(param: T) {
        mProducerHandler.post(RENDER) {
            if (!mIsReleasing) {
                mParams.add(param)
                doRender()
            }
        }
    }

    @AnyThread
    fun cancelPending() {
        mProducerHandler.removeCallbacksAndMessages(RENDER)
        mProducerHandler.post(CANCEL_PENDING, mCancelPendingRunnable)
    }

    @AnyThread
    fun markTextureConsumed() {
        mProducerHandler.post(TEXTURE_CONSUMED) {
            mPendingRenders--
            if (mIsReleasing && !isPendingRendering()) {
                teardown()
            } else {
                doRender()
            }
        }
    }

    @AnyThread
    fun execute(runnable: Runnable) {
        mProducerHandler.post(runnable)
    }

    @AnyThread
    fun remove(runnable: Runnable) {
        mProducerHandler.removeCallbacks(runnable)
    }

    @AnyThread
    fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)? = null) {
        if (cancelPending) {
            cancelPending()
        }
        mProducerHandler.post(RELEASE) {
            mIsReleasing = true
            if (!isPendingRendering()) {
                teardown(onReleaseComplete)
            }
        }
    }

    private companion object {
        /**
         * Constant to indicate a request to render new content into a SurfaceTexture
         * for consumption.
         */
        const val RENDER = 0

        /**
         * Constant to indicate that a previously produced frame has been consumed.
         */
        const val TEXTURE_CONSUMED = 1

        /**
         * Cancel all pending requests to render and clear all parameters that are to be consumed
         * for an upcoming frame
         */
        const val CANCEL_PENDING = 2

        /**
         * Release the resources associated with this [TextureProducer] instance
         */
        const val RELEASE = 3

        /**
         * Maximum number of frames to produce before the producer pauses. Subsequent attempts
         * to render will batch parameters and continue to produce frames when the consumer
         * signals that the corresponding textures have been consumed.
         */
        const val MAX_PENDING_RENDERS = 2
    }
}
