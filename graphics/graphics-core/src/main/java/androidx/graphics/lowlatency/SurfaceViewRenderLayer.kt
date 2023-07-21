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

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.FrameBuffer
import androidx.graphics.opengl.FrameBufferRenderer
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.surface.SurfaceControlCompat
import androidx.hardware.SyncFenceCompat
import java.util.Collections

/**
 * [ParentRenderLayer] instance that leverages a [SurfaceView]'s [SurfaceControlCompat] as the
 * parent [SurfaceControlCompat] for the front and double buffered layers
 */
@RequiresApi(Build.VERSION_CODES.Q)
internal class SurfaceViewRenderLayer<T>(
    private val surfaceView: SurfaceView
) : ParentRenderLayer<T> {

    private var mLayerCallback: ParentRenderLayer.Callback<T>? = null
    private var mFrameBufferRenderer: FrameBufferRenderer? = null
    private var mRenderTarget: GLRenderer.RenderTarget? = null
    private var mParentSurfaceControl: SurfaceControlCompat? = null
    private val mBufferTransform = BufferTransformer()

    private val mTransformResolver = BufferTransformHintResolver()

    private var mTransformHint = BufferTransformHintResolver.UNKNOWN_TRANSFORM
    private var mInverse = BufferTransformHintResolver.UNKNOWN_TRANSFORM
    private val mHolderCallback = object : SurfaceHolder.Callback {

        override fun surfaceCreated(holder: SurfaceHolder) {
            // NO-OP wait on surfaceChanged callback
        }

        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            mTransformHint = mTransformResolver.getBufferTransformHint(surfaceView)
            mInverse = mBufferTransform.invertBufferTransform(mTransformHint)
            mBufferTransform.computeTransform(width, height, mInverse)
            mParentSurfaceControl?.release()
            mParentSurfaceControl = createDoubleBufferedSurfaceControl()
            mLayerCallback?.onSizeChanged(width, height)
        }

        override fun surfaceDestroyed(p0: SurfaceHolder) {
            mLayerCallback?.onLayerDestroyed()
        }
    }

    init {
        surfaceView.holder.addCallback(mHolderCallback)
    }

    override fun getInverseBufferTransform(): Int = mInverse

    override fun getBufferWidth(): Int = mBufferTransform.glWidth

    override fun getBufferHeight(): Int = mBufferTransform.glHeight

    override fun getTransform(): FloatArray = mBufferTransform.transform

    override fun buildReparentTransaction(
        child: SurfaceControlCompat,
        transaction: SurfaceControlCompat.Transaction
    ) {
        transaction.reparent(child, mParentSurfaceControl)
    }

    override fun setParent(builder: SurfaceControlCompat.Builder) {
        mParentSurfaceControl?.let { parentSurfaceControl ->
            builder.setParent(parentSurfaceControl)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun createRenderTarget(
        renderer: GLRenderer,
        renderLayerCallback: GLFrontBufferedRenderer.Callback<T>
    ): GLRenderer.RenderTarget {
        var params: Collection<T>? = null
        val bufferInfo = BufferInfo()
        val frameBufferRenderer = FrameBufferRenderer(
            object : FrameBufferRenderer.RenderCallback {

                override fun obtainFrameBuffer(egl: EGLSpec): FrameBuffer {
                    val frameBuffer = mLayerCallback?.getFrameBufferPool()?.obtain(egl)
                        ?: throw IllegalArgumentException("No FrameBufferPool available")
                    bufferInfo.frameBufferId = frameBuffer.frameBuffer
                    return frameBuffer
                }

                override fun onDraw(eglManager: EGLManager) {
                    bufferInfo.apply {
                        this.width = mBufferTransform.glWidth
                        this.height = mBufferTransform.glHeight
                    }
                    renderLayerCallback.onDrawMultiBufferedLayer(
                        eglManager,
                        bufferInfo,
                        mBufferTransform.transform,
                        params ?: Collections.emptyList()
                    )
                }

                @SuppressLint("WrongConstant")
                @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                override fun onDrawComplete(
                    frameBuffer: FrameBuffer,
                    syncFenceCompat: SyncFenceCompat?
                ) {
                    val frontBufferedLayerSurfaceControl = mLayerCallback
                        ?.getFrontBufferedLayerSurfaceControl()
                    val sc = mParentSurfaceControl
                    // At this point the parentSurfaceControl should already be created
                    // in the surfaceChanged callback, however, if for whatever reason this
                    // was not the case, create the double buffered SurfaceControl now and cache
                    // it
                        ?: createDoubleBufferedSurfaceControl().also {
                            mParentSurfaceControl = it
                        }
                    if (frontBufferedLayerSurfaceControl != null) {
                        val transaction = SurfaceControlCompat.Transaction()
                            .setVisibility(frontBufferedLayerSurfaceControl, false)
                            .setVisibility(sc, true)
                            .setBuffer(sc, frameBuffer.hardwareBuffer, syncFenceCompat) {
                                mLayerCallback?.getFrameBufferPool()?.release(frameBuffer)
                            }
                        if (mTransformHint != BufferTransformHintResolver.UNKNOWN_TRANSFORM) {
                            transaction.setBufferTransform(sc, mInverse)
                        }

                        renderLayerCallback.onMultiBufferedLayerRenderComplete(
                            frontBufferedLayerSurfaceControl,
                            transaction
                        )
                        transaction.commit()
                    } else {
                        Log.e(
                            TAG, "Error, no front buffered SurfaceControl available to " +
                                "synchronize transaction with"
                        )
                    }
                }
            })
        val parentFrameBufferRenderer = WrapperFrameBufferRenderer<T>(frameBufferRenderer) {
            params = mLayerCallback?.obtainMultiBufferedLayerParams()
            params != null
        }
        val renderTarget = renderer.attach(surfaceView, parentFrameBufferRenderer)
        mRenderTarget = renderTarget
        mFrameBufferRenderer = frameBufferRenderer
        return renderTarget
    }

    internal fun createDoubleBufferedSurfaceControl(): SurfaceControlCompat {
        val surfaceControl = SurfaceControlCompat.Builder()
            .setParent(surfaceView)
            .setName("DoubleBufferedLayer")
            .build()
        // SurfaceControl is not visible by default so make it visible right after creation
        SurfaceControlCompat.Transaction().setVisibility(surfaceControl, true).commit()
        return surfaceControl
    }

    override fun setParentLayerCallbacks(callback: ParentRenderLayer.Callback<T>?) {
        mLayerCallback = callback
    }

    override fun clear() {
        mFrameBufferRenderer?.clear()
        mRenderTarget?.requestRender()
    }

    override fun release(transaction: SurfaceControlCompat.Transaction) {
        surfaceView.holder.removeCallback(mHolderCallback)
        mParentSurfaceControl?.let {
            transaction.reparent(it, null)
            it.release()
        }
        mParentSurfaceControl = null
    }

    internal companion object {
        internal const val TAG = "SurfaceViewRenderLayer"
    }
}