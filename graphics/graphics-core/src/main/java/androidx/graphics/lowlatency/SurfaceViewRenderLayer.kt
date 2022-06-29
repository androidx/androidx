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

import android.opengl.GLES20
import android.os.Build
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EglManager
import androidx.graphics.opengl.egl.EglSpec
import androidx.graphics.surface.SurfaceControlCompat
import java.util.Collections

/**
 * [ParentRenderLayer] instance that leverages a [SurfaceView]'s [SurfaceControlCompat] as the
 * parent [SurfaceControlCompat] for the wet and dry layers
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class SurfaceViewRenderLayer(private val surfaceView: SurfaceView) : ParentRenderLayer {

    private var mLayerCallback: ParentRenderLayer.Callback? = null
    private var mHardwareBufferRenderer: HardwareBufferRenderer? = null
    private var mRenderTarget: GLRenderer.RenderTarget? = null
    private var mParentSurfaceControl: SurfaceControlCompat? = null

    init {
        surfaceView.hostToken
    }

    override fun buildReparentTransaction(
        child: SurfaceControlCompat,
        transaction: SurfaceControlCompat.Transaction
    ) {
        transaction.reparent(child, surfaceView)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun createRenderTarget(
        renderer: GLRenderer,
        renderLayerCallback: GLWetDryRenderer.Callback
    ): GLRenderer.RenderTarget {
        val hardwareBufferRenderer = HardwareBufferRenderer(
            object : HardwareBufferRenderer.RenderCallbacks {

                override fun obtainRenderBuffer(egl: EglSpec): RenderBuffer =
                    mLayerCallback?.getRenderBufferPool()?.obtain(egl)
                        ?: throw IllegalArgumentException("No RenderBufferPool available")

                override fun onDraw(eglManager: EglManager) {
                    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
                    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
                    val params = mLayerCallback?.obtainDryLayerParams() ?: Collections.EMPTY_LIST
                    renderLayerCallback.onDrawDryLayer(eglManager, params)
                    params?.clear()
                }

                @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                override fun onDrawComplete(renderBuffer: RenderBuffer) {
                    val wetLayerSurfaceControl = mLayerCallback
                            ?.getWetLayerSurfaceControl()
                    val sc = mParentSurfaceControl
                        // At this point the parentSurfaceControl should already be created
                        // in the surfaceChanged callback, however, if for whatever reason this
                        // was not the case, create the dry SurfaceControl now and cache it
                        ?: createDrySurfaceControl().also {
                            mParentSurfaceControl = it
                        }
                    if (wetLayerSurfaceControl != null) {
                        val transaction = SurfaceControlCompat.Transaction()
                            .setVisibility(wetLayerSurfaceControl, false)
                            .setVisibility(sc, true)
                            .setBuffer(sc, renderBuffer.hardwareBuffer) {
                                mLayerCallback?.getRenderBufferPool()?.release(renderBuffer)
                            }

                        renderLayerCallback.onDryLayerRenderComplete(
                            wetLayerSurfaceControl,
                            transaction
                        )
                        transaction.commit()
                    } else {
                        Log.e(TAG, "Error, no wet SurfaceControl available to synchronize " +
                            "transaction with")
                    }
                }
        })
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder) {
                // NO-OP wait on surfaceChanged callback
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                mParentSurfaceControl?.release()
                mLayerCallback?.onSizeChanged(width, height)
                mParentSurfaceControl = createDrySurfaceControl()
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                mLayerCallback?.onLayerDestroyed()
            }
        })
        val renderTarget = renderer.attach(surfaceView, hardwareBufferRenderer)
        mRenderTarget = renderTarget
        mHardwareBufferRenderer = hardwareBufferRenderer
        return renderTarget
    }

    internal fun createDrySurfaceControl(): SurfaceControlCompat =
        SurfaceControlCompat.Builder()
            .setParent(surfaceView)
            .setName("DryLayer")
            .build()

    override fun setParentLayerCallbacks(callback: ParentRenderLayer.Callback?) {
        mLayerCallback = callback
    }

    override fun clear() {
        mHardwareBufferRenderer?.clear()
        mRenderTarget?.requestRender()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun release(transaction: SurfaceControlCompat.Transaction) {
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