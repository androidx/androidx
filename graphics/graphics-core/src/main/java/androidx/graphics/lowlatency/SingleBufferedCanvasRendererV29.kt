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

import android.graphics.RenderNode
import android.graphics.SurfaceTexture
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.SurfaceTextureRenderer
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
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(Build.VERSION_CODES.Q)
internal class SingleBufferedCanvasRendererV29<T>(
    private val width: Int,
    private val height: Int,
    private val bufferTransformer: BufferTransformer,
    private val executor: Executor,
    private val callbacks: SingleBufferedCanvasRenderer.RenderCallbacks<T>,
) : SingleBufferedCanvasRenderer<T> {

    private val mMainHandler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    private val mRenderNode = RenderNode("renderNode").apply {
        setPosition(
            0,
            0,
            this@SingleBufferedCanvasRendererV29.width,
            this@SingleBufferedCanvasRendererV29.height
        )
    }

    /**
     * Runnable used to execute the request to render batched parameters
     */
    private val mRenderPendingRunnable = Runnable { renderPendingParameters() }

    /**
     * Runnable used to execute the request to clear buffer content on screen
     */
    private val mClearContentsRunnable = Runnable {
        mFrameBufferRenderer.clear()
        obtainFrameBufferTarget().requestRender()
    }

    /**
     * SurfaceTextureRenderer used to render contents of a RenderNode into a SurfaceTexture
     * that is then rendered into a HardwareBuffer for consumption
     */
    private val mSurfaceTextureRenderer = SurfaceTextureRenderer(
            mRenderNode,
            width,
            height,
            mMainHandler
        ) { texture ->
            mSurfaceTexture = texture
            obtainFrameBufferTarget().requestRender()
        }

    /**
     * Helper method to request the provided RenderNode content to be drawn on the texture
     * rendering thread
     */
    internal fun dispatchRenderTextureRequest() {
        executor.execute(mRenderPendingRunnable)
    }

    /**
     * Helper method to request clearing the contents of the destination HardwareBuffer
     */
    private fun dispatchClearRequest() {
        executor.execute(mClearContentsRunnable)
    }

    /**
     * Maximum number of pending renders to the SurfaceTexture before we queue up parameters
     * and wait for the consumer to catch up. Some devices have very fast input sampling rates
     * which make the producing side much faster than the consuming side. We batch the pending
     * parameters and when the consuming side catches up, we batch and render all the pending
     * parameters into the SurfaceTexture that then gets drawn into the destination HardwareBuffer.
     * This ensures we don't drop any attempts to render.
     */
    private val mMaxPendingBuffers = 2

    /**
     * Keep track of the number of pending renders of the source SurfaceTexture to the destination
     * HardwareBuffer
     */
    private val mPendingBuffers = AtomicInteger(0)

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

                val state = mState.get()
                if (state != RELEASED) {
                    executor.execute {
                        callbacks.onBufferReady(frameBuffer.hardwareBuffer, syncFenceCompat)
                    }
                }
                val pending = mPendingBuffers.decrementAndGet()
                if (state == PENDING_RELEASE && pending <= 0) {
                    mMainHandler.post(::tearDown)
                } else {
                    // After rendering see if there are additional queued content to render
                    // If all images within the SurfaceTexture are pending being drawn into the
                    // destination HardwareBuffer, they are queued up to be batch rendered after
                    // texture image has been released
                    dispatchRenderTextureRequest()
                }
            }
        },
        mFrontBufferSyncStrategy
    )

    /**
     * [GLRenderer] used to render contents of the SurfaceTexture into a HardwareBuffer
     */
    private val mGLRenderer = GLRenderer().apply { start() }

    /**
     * Thread safe queue of parameters to be consumed in on the texture render thread that are
     * provided in [SingleBufferedCanvasRenderer.render]
     */
    private val mParams = ParamQueue<T>()

    /**
     * State to determine if [release] has been called on this [SingleBufferedCanvasRendererV29]
     * instance. If true, all subsequent operations are a no-op
     */
    private val mState = AtomicInteger(ACTIVE)

    /**
     * Pending release callback to be invoked when the renderer is torn down
     */
    private var mReleaseComplete: (() -> Unit)? = null

    /**
     * Flag to maintain visibility state on the main thread
     */
    private var mIsVisible = false

    private fun isReleased(): Boolean {
        val state = mState.get()
        return state == RELEASED || state == PENDING_RELEASE
    }

    @WorkerThread
    internal fun renderPendingParameters() {
        val pending = mPendingBuffers.get()
        // If there are pending requests to draw and we are not waiting on the consuming side
        // to catch up, then render content in the RenderNode and issue a request to draw into
        // the SurfaceTexture
        if (pending < mMaxPendingBuffers) {
            val params = mParams.release()
            if (params.isNotEmpty()) {
                val canvas = mRenderNode.beginRecording()
                for (p in params) {
                    callbacks.render(canvas, width, height, p)
                }
                mRenderNode.endRecording()
                mPendingBuffers.incrementAndGet()
                mSurfaceTextureRenderer.renderFrame()
            }
        }
    }

    override var isVisible: Boolean
        set(value) {
            mGLRenderer.execute {
                mFrontBufferSyncStrategy.isVisible = value
            }
            mIsVisible = value
        }
        get() = mFrontBufferSyncStrategy.isVisible

    private var mFrontBufferLayer: FrameBuffer? = null

    private fun obtainFrameBufferTarget(): GLRenderer.RenderTarget =
        mFrameBufferTarget ?: mGLRenderer.createRenderTarget(width, height, mFrameBufferRenderer)
            .also { mFrameBufferTarget = it }

    private var mFrameBufferTarget: GLRenderer.RenderTarget? = null

    override fun render(param: T) {
        ifNotReleased {
            mParams.add(param)
            if (mPendingBuffers.get() < mMaxPendingBuffers) {
                dispatchRenderTextureRequest()
            }
        }
    }

    override fun release(cancelPending: Boolean, onReleaseComplete: (() -> Unit)?) {
        ifNotReleased {
            mReleaseComplete = onReleaseComplete
            if (cancelPending || !isPendingRendering()) {
                mState.set(RELEASED)
                cancelPending()
                tearDown()
            } else {
                mState.set(PENDING_RELEASE)
            }
        }
    }

    private fun isPendingRendering() = mParams.isEmpty() || mPendingBuffers.get() > 0

    internal fun tearDown() {
        mFrameBufferTarget?.detach(true) {
            // GL Thread
            mQuadRenderer?.release()
            if (mTextureId != -1) {
                buffer[0] = mTextureId
                GLES20.glDeleteTextures(1, buffer, 0)
                mTextureId = -1
            }
        }
        mGLRenderer.stop(false)
        mSurfaceTexture?.let { texture ->
            if (!texture.isReleased) {
                texture.release()
            }
        }
        mRenderNode.discardDisplayList()
        mSurfaceTextureRenderer.release()

        mReleaseComplete?.let { callback ->
            mMainHandler.post(callback)
        }
    }

    override fun cancelPending() {
        ifNotReleased {
            mParams.clear()
        }
    }

    override fun clear() {
        ifNotReleased {
            dispatchClearRequest()
        }
    }

    private inline fun ifNotReleased(block: () -> Unit) {
        if (!isReleased()) {
            block()
        } else {
            Log.w(TAG, "Attempt to use already released renderer")
        }
    }

    // See: b/236394768. Some test emulator instances have not picked up the fix so
    // apply a workaround here for testing purposes
    internal val forceFlush = AtomicBoolean(false)

    private companion object {

        /**
         * Indicates the renderer is an in active state and can render content
         */
        const val ACTIVE = 0

        /**
         * Indicates the renderer is released and is no longer in a valid state to render content
         */
        const val RELEASED = 1

        /**
         * Indicates the renderer is completing rendering of current pending frames but not accepting
         * new requests to render
         */
        const val PENDING_RELEASE = 2

        const val TAG = "SingleBufferedCanvasV29"
    }
}