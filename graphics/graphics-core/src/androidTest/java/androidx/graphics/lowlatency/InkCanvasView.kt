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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class InkCanvasView(context: Context) : SurfaceView(context) {

    private var mCanvasFrontBufferedRenderer: CanvasFrontBufferedRenderer<FloatArray>? = null
    private val mLinesDrawable = LinesDrawable().apply {
        strokeWidth = 15f
    }
    private val mSceneParams = ArrayList<FloatArray>()
    private val mCallbacks = object : CanvasFrontBufferedRenderer.Callback<FloatArray> {

        override fun onDrawFrontBufferedLayer(
            canvas: Canvas,
            bufferWidth: Int,
            bufferHeight: Int,
            param: FloatArray
        ) {
            with(mLinesDrawable) {
                setBounds(0, 0, bufferWidth, bufferHeight)
                setLines(param)
                setColor(Color.MAGENTA)
                alpha = 128
                draw(canvas)
            }
        }

        override fun onDrawMultiBufferedLayer(
            canvas: Canvas,
            bufferWidth: Int,
            bufferHeight: Int,
            params: Collection<FloatArray>
        ) {
            mSceneParams.addAll(params)
            with(mLinesDrawable) {
                setBounds(0, 0, bufferWidth, bufferHeight)
                setColor(Color.CYAN)
                alpha = 128
                for (param in mSceneParams) {
                    setLines(param)
                    draw(canvas)
                }
            }
        }
    }

    private var mPreviousX: Float = 0f
    private var mPreviousY: Float = 0f
    private var mCurrentX: Float = 0f
    private var mCurrentY: Float = 0f

    init {
        setZOrderOnTop(true)
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
                    mCanvasFrontBufferedRenderer?.renderFrontBufferedLayer(line)
                }
                MotionEvent.ACTION_CANCEL -> {
                    mCanvasFrontBufferedRenderer?.cancel()
                }
                MotionEvent.ACTION_UP -> {
                    mCanvasFrontBufferedRenderer?.commit()
                }
            }
            true
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mCanvasFrontBufferedRenderer = CanvasFrontBufferedRenderer(this, mCallbacks)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mCanvasFrontBufferedRenderer?.release(true)
        mCanvasFrontBufferedRenderer = null
    }
}
