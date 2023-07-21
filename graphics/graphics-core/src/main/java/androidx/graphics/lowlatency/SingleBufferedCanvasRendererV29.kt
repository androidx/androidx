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

import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.graphics.lowlatency.FrontBufferUtils.Companion.obtainHardwareBufferUsageFlags
import androidx.graphics.opengl.FrameBuffer
import androidx.graphics.opengl.FrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.QuadTextureRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.hardware.SyncFenceCompat
import java.nio.IntBuffer
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

    private val mProducer = TextureProducer<T>(
        width,
        height,
        object : TextureProducer.Callbacks<T> {
            override fun onTextureAvailable(texture: SurfaceTexture) {
                mSurfaceTexture = texture
                mFrameBufferTarget.requestRender()
            }

            override fun render(canvas: Canvas, width: Int, height: Int, param: T) {
                callbacks.render(canvas, width, height, param)
            }
        })

    /**
     * Source SurfaceTexture that the destination of content to be rendered from the provided
     * RenderNode
     */
    private var mSurfaceTexture: SurfaceTexture? = null

    /**
     * HardwareBuffer flags for front buffered rendering
     */
    private val mHardwareBufferUsageFlags = obtainHardwareBufferUsageFlags()

    // ---------- GLThread ------

    /**
     * [FrontBufferSyncStrategy] used for [FrameBufferRenderer] to conditionally decide
     * when to create a [SyncFenceCompat] for transaction calls.
     */
    private val mFrontBufferSyncStrategy = FrontBufferSyncStrategy(mHardwareBufferUsageFlags)

    /**
     * Shader that handles rendering a texture as a quad into the destination
     */
    private var mQuadRenderer: QuadTextureRenderer? = null

    /**
     * Texture id of the SurfaceTexture that is to be rendered
     */
    private var mTextureId: Int = -1

    /**
     * Scratch buffer used for gen/delete texture operations
     */
    private val buffer = IntArray(1)

    // ---------- GLThread ------

    private val mFrameBufferRenderer = FrameBufferRenderer(
        object : FrameBufferRenderer.RenderCallback {

            private val mMVPMatrix = FloatArray(16)
            private val mProjection = FloatArray(16)

            private fun obtainQuadRenderer(): QuadTextureRenderer =
                mQuadRenderer ?: QuadTextureRenderer().apply {
                    GLES20.glGenTextures(1, buffer, 0)
                    mTextureId = buffer[0]
                    mSurfaceTexture?.let { texture ->
                        texture.attachToGLContext(mTextureId)
                        setSurfaceTexture(texture)
                    }
                    mQuadRenderer = this
                }

            override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer {
                return mFrontBufferLayer ?: FrameBuffer(
                    egl,
                    HardwareBuffer.create(
                        bufferTransformer.glWidth,
                        bufferTransformer.glHeight,
                        HardwareBuffer.RGBA_8888,
                        1,
                        mHardwareBufferUsageFlags
                    )
                ).also { mFrontBufferLayer = it }
            }

            @RequiresApi(Build.VERSION_CODES.S)
            override fun onDraw(eglManager: EGLManager) {
                mSurfaceTexture?.let { texture ->
                    val bufferWidth = bufferTransformer.glWidth
                    val bufferHeight = bufferTransformer.glHeight
                    GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                    Matrix.orthoM(
                        mMVPMatrix,
                        0,
                        0f,
                        bufferWidth.toFloat(),
                        0f,
                        bufferHeight.toFloat(),
                        -1f,
                        1f
                    )

                    Matrix.multiplyMM(mProjection, 0, mMVPMatrix, 0, bufferTransformer.transform, 0)
                    // texture.updateTexImage is called within QuadTextureRenderer#draw
                    obtainQuadRenderer().draw(mProjection, width.toFloat(), height.toFloat())
                    texture.releaseTexImage()
                    mProducer.markTextureConsumed()
                }
            }

            override fun onDrawComplete(
                frameBuffer: FrameBuffer,
                syncFenceCompat: SyncFenceCompat?
            ) {
                if (forceFlush.get()) {
                    // See b/236394768. On some ANGLE versions, attempting to do a glClear + flush
                    // does not actually flush pixels to FBOs with HardwareBuffer attachments
                    // For testing purposes when verifying clear use cases, do a GPU readback
                    // to actually executed any pending clear operations to verify output
                    GLES20.glReadPixels(0, 0, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                        IntBuffer.wrap(IntArray(1)))
                }

                mProducer.execute {
                    callbacks.onBufferReady(frameBuffer.hardwareBuffer, syncFenceCompat)
                }
            }
        },
        mFrontBufferSyncStrategy
    )

    /**
     * [GLRenderer] used to render contents of the SurfaceTexture into a HardwareBuffer
     */
    private val mGLRenderer = GLRenderer().apply { start() }

    private var mFrameBufferTarget: GLRenderer.RenderTarget =
        mGLRenderer.createRenderTarget(width, height, mFrameBufferRenderer)

    /**
     * Flag to maintain visibility state on the main thread
     */
    private var mIsVisible = false

    /**
     * Flag to determine if the SingleBufferedCanvasRenderer instance has been released
     */
    private var mIsReleased = false

    override var isVisible: Boolean
        set(value) {
            mGLRenderer.execute {
                mFrontBufferSyncStrategy.isVisible = value
            }
            mIsVisible = value
        }
        get() = mFrontBufferSyncStrategy.isVisible

    private var mFrontBufferLayer: FrameBuffer? = null

    override fun render(param: T) {
        if (!mIsReleased) {
            mProducer.requestRender(param)
        } else {
            Log.w(TAG, "Attempt to render with CanvasRenderer that has already been released")
        }
    }

    override fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)?) {
        if (!mIsReleased) {
            if (cancelPending) {
                mProducer.cancelPending()
            }
            mProducer.release(cancelPending) {
                // If the producer is torn down after all pending requests are completed
                // then there is nothing left for the render target to consume so
                // detach immediately
                mFrameBufferTarget.detach(true) {
                    // GL Thread
                    mQuadRenderer?.release()
                    if (mTextureId != -1) {
                        buffer[0] = mTextureId
                        GLES20.glDeleteTextures(1, buffer, 0)
                        mTextureId = -1
                    }
                }
                mGLRenderer.stop(true)
                onReleaseComplete?.invoke()
           }

            mIsReleased = true
        } else {
            Log.w(TAG, "Attempt to release CanvasRenderer that has already been released")
        }
    }

    private val mClearRunnable = Runnable {
        mFrameBufferRenderer.clear()
        mFrameBufferTarget.requestRender()
    }

    override fun cancelPending() {
        if (!mIsReleased) {
            mProducer.cancelPending()
        } else {
            Log.w(TAG, "Attempt to cancel pending requests when the CanvasRender has " +
                "already been released")
        }
    }

    override fun clear() {
        if (!mIsReleased) {
            mProducer.execute(mClearRunnable)
        } else {
            Log.w(TAG, "Attempt to clear contents when the CanvasRenderer has already " +
                "been released")
        }
    }

    // See: b/236394768. Some test emulator instances have not picked up the fix so
    // apply a workaround here for testing purposes
    internal val forceFlush = AtomicBoolean(false)

    private companion object {

        const val TAG = "SingleBufferedCanvasV29"
    }
}