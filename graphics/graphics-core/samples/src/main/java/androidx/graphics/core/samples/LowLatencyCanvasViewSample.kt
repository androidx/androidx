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

package androidx.graphics.core.samples

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.annotation.Sampled
import androidx.annotation.WorkerThread
import androidx.graphics.lowlatency.LowLatencyCanvasView
import java.util.Collections

@Sampled
fun lowLatencyCanvasViewSample(context: Context) {
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
        setRenderCallback(object : LowLatencyCanvasView.Callback {

            val mAllLines = ArrayList<Line>()

            private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 15f
                color = Color.CYAN
                alpha = 128
            }

            @WorkerThread
            override fun onRedrawRequested(
                canvas: Canvas,
                width: Int,
                height: Int
            ) {
                for (line in mAllLines) {
                    canvas.drawLine(line.x1, line.y1, line.x2, line.y2, mPaint)
                }
            }

            @WorkerThread
            override fun onDrawFrontBufferedLayer(
                canvas: Canvas,
                width: Int,
                height: Int
            ) {
                lines.removeFirstOrNull()?.let { line ->
                    mAllLines.add(line)
                    canvas.drawLine(line.x1, line.y1, line.x2, line.y2, mPaint)
                }
            }
        })
        setOnTouchListener(object : View.OnTouchListener {

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
        })
    }
}
