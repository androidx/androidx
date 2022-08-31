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

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.opengl.egl.EGLManager
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(Build.VERSION_CODES.Q)
class InkSurfaceView(context: Context) : SurfaceView(context) {

    private var mFrontBufferRenderer: GLFrontBufferedRenderer<FloatArray>? = null

    /**
     * LineRenderer to be created on the GL Thread
     */
    private var mLineRenderer: LineRenderer? = null

    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var mPreviousX: Float = 0f
    private var mPreviousY: Float = 0f
    private var mCurrentX: Float = 0f
    private var mCurrentY: Float = 0f

    @WorkerThread // GLThread
    private fun obtainRenderer(): LineRenderer =
        mLineRenderer ?: (LineRenderer()
            .apply {
                initialize()
                mLineRenderer = this
            })

    private val mCallbacks = object : GLFrontBufferedRenderer.Callback<FloatArray> {

        private val mMVPMatrix = FloatArray(16)

        override fun onDrawFrontBufferedLayer(eglManager: EGLManager, param: FloatArray) {
            GLES20.glViewport(0, 0, mWidth, mHeight)
            // Map Android coordinates to GL coordinates
            Matrix.orthoM(
                mMVPMatrix,
                0,
                0f,
                mWidth.toFloat(),
                0f,
                mHeight.toFloat(),
                -1f,
                1f
            )
            obtainRenderer().drawLines(mMVPMatrix, param, Color.RED)
        }

        override fun onDrawDoubleBufferedLayer(
            eglManager: EGLManager,
            params: Collection<FloatArray>
        ) {
            GLES20.glViewport(0, 0, mWidth, mHeight)
            Matrix.orthoM(mMVPMatrix, 0, 0f, mWidth.toFloat(), 0f, mHeight.toFloat(), -1f, 1f)
            for (line in params) {
                obtainRenderer().drawLines(mMVPMatrix, line, Color.BLUE)
            }
        }
    }

    val renderCount = AtomicInteger(0)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestUnbufferedDispatch(InputDevice.SOURCE_CLASS_POINTER)
        }
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    mCurrentX = event.x
                    mCurrentY = event.y
                    renderCount.set(0)
                }
                MotionEvent.ACTION_MOVE -> {
                    mPreviousX = mCurrentX
                    mPreviousY = mCurrentY
                    mCurrentX = event.x
                    mCurrentY = event.y

                    val line = FloatArray(4).apply {
                        this[0] = mPreviousX
                        this[1] = mPreviousY
                        this[2] = mCurrentX
                        this[3] = mCurrentY
                    }
                    renderCount.incrementAndGet()
                    mFrontBufferRenderer?.renderFrontBufferedLayer(line)
                }
                MotionEvent.ACTION_CANCEL -> {
                    mFrontBufferRenderer?.commit()
                }
                MotionEvent.ACTION_UP -> {
                    mFrontBufferRenderer?.commit()
                }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mFrontBufferRenderer = GLFrontBufferedRenderer(this, mCallbacks)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mWidth = right - left
        mHeight = bottom - top
    }

    override fun onDetachedFromWindow() {
        mFrontBufferRenderer?.release(true) {
            obtainRenderer().release()
        }
        super.onDetachedFromWindow()
    }
}