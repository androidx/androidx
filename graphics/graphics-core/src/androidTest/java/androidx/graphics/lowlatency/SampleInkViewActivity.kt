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

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import java.util.Collections

@RequiresApi(Build.VERSION_CODES.S)
class SampleInkViewActivity : Activity() {

    private var inkView: View? = null
    private var toggle: Button? = null
    private var container: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addInkViews()
    }

    private fun addInkViews() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toggle =
                Button(this).apply {
                    layoutParams =
                        FrameLayout.LayoutParams(300, WRAP_CONTENT).apply {
                            gravity = Gravity.BOTTOM or Gravity.RIGHT
                        }
                    setOnClickListener { toggleLowLatencyView() }
                }
            container =
                FrameLayout(this).apply {
                    addView(toggle)
                    setBackgroundColor(Color.BLACK)
                }
            toggleLowLatencyView()
            setContentView(container)
        }
    }

    private fun toggleLowLatencyView() {
        inkView?.let { view -> container?.removeView(view) }
        if (inkView == null || inkView is LowLatencyCanvasView) {
            inkView = InkCanvasView(this)
            toggle?.text = "CanvasFrontBufferedRenderer"
        } else if (inkView is InkCanvasView) {
            inkView = InkSurfaceView(this)
            toggle?.text = "OpenGL"
        } else if (inkView is InkSurfaceView) {
            inkView = createLowLatencyCanvasView(this)
            toggle?.text = "LowLatencyCanvasView"
        }
        container?.addView(inkView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        container?.bringChildToFront(toggle)
    }

    private fun createLowLatencyCanvasView(context: Context): LowLatencyCanvasView =
        LowLatencyCanvasView(context).apply {
            setBackgroundColor(Color.WHITE)

            data class Line(
                val x1: Float,
                val y1: Float,
                val x2: Float,
                val y2: Float,
            )
            // Thread safe collection to support creation of new lines from the UI thread as well as
            // consumption of lines from the background drawing thread
            val lines = Collections.synchronizedList(ArrayList<Line>())
            setRenderCallback(
                object : LowLatencyCanvasView.Callback {

                    val mAllLines = ArrayList<Line>()

                    private val mPaint =
                        Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            strokeWidth = 15f
                            color = Color.CYAN
                            alpha = 128
                        }

                    @WorkerThread
                    override fun onRedrawRequested(canvas: Canvas, width: Int, height: Int) {
                        for (line in mAllLines) {
                            canvas.drawLine(line.x1, line.y1, line.x2, line.y2, mPaint)
                        }
                    }

                    @WorkerThread
                    override fun onDrawFrontBufferedLayer(canvas: Canvas, width: Int, height: Int) {
                        lines.removeFirstOrNull()?.let { line ->
                            mAllLines.add(line)
                            canvas.drawLine(line.x1, line.y1, line.x2, line.y2, mPaint)
                        }
                    }
                }
            )
            setOnTouchListener(
                object : View.OnTouchListener {

                    var mCurrentX = -1f
                    var mCurrentY = -1f
                    var mPreviousX = -1f
                    var mPreviousY = -1f

                    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                        if (event == null) return false
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

                                val line = Line(mPreviousX, mPreviousY, mCurrentX, mCurrentY)
                                lines.add(line)
                                renderFrontBufferedLayer()
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                cancel()
                            }
                            MotionEvent.ACTION_UP -> {
                                commit()
                            }
                        }
                        return true
                    }
                }
            )
        }
}
