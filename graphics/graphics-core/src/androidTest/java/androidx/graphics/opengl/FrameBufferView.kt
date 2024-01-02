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

package androidx.graphics.opengl

import android.content.Context
import android.graphics.Color
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.graphics.lowlatency.BufferInfo
import androidx.graphics.lowlatency.LineRenderer
import androidx.graphics.opengl.egl.EGLManager
import java.util.concurrent.ConcurrentLinkedQueue

@RequiresApi(Build.VERSION_CODES.Q)
class FrameBufferView(context: Context) : SurfaceView(context) {

    private var mFrameBufferRenderer: GLFrameBufferRenderer? = null

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

    private val mSceneParams = ConcurrentLinkedQueue<FloatArray>()

    private val mCallbacks = object : GLFrameBufferRenderer.Callback {

        private val mMVPMatrix = FloatArray(16)
        private val mProjection = FloatArray(16)

        override fun onDrawFrame(
            eglManager: EGLManager,
            width: Int,
            height: Int,
            bufferInfo: BufferInfo,
            transform: FloatArray
        ) {
            GLES20.glViewport(0, 0, bufferInfo.width, bufferInfo.height)
            GLES20.glClearColor(0f, 0f, 0f, 0f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glFlush()
            Matrix.orthoM(
                mMVPMatrix,
                0,
                0f,
                bufferInfo.width.toFloat(),
                0f,
                bufferInfo.height.toFloat(),
                -1f,
                1f
            )
            Matrix.multiplyMM(mProjection, 0, mMVPMatrix, 0, transform, 0)
            for (line in mSceneParams) {
                obtainRenderer().drawLines(mProjection, line, Color.BLUE, LINE_WIDTH)
            }
        }
    }

    init {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    requestUnbufferedDispatch(event)
                    mCurrentX = event.x
                    mCurrentY = event.y
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
                    mSceneParams.add(line)
                    mFrameBufferRenderer?.render()
                }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mFrameBufferRenderer = GLFrameBufferRenderer.Builder(this, mCallbacks)
            .setMaxBuffers(1)
            .build()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mWidth = right - left
        mHeight = bottom - top
    }

    override fun onDetachedFromWindow() {
        mFrameBufferRenderer?.release(true) {
            obtainRenderer().release()
        }
        super.onDetachedFromWindow()
    }

    private companion object {
        private const val LINE_WIDTH = 5f
    }
}
