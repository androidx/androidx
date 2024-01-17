/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.compose

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.constraintlayout.core.motion.Motion
import androidx.constraintlayout.core.motion.MotionPaths

internal class MotionRenderDebug(textSize: Float) {
    var mPoints: FloatArray? = null
    var mPathMode: IntArray
    var mKeyFramePoints: FloatArray
    var mPath: Path? = null
    var mPaint: Paint
    var mPaintKeyframes: Paint
    var mPaintGraph: Paint
    var mTextPaint: Paint
    var mFillPaint: Paint
    private val mRectangle: FloatArray
    val mRedColor = -0x55cd
    val mKeyframeColor = -0x1f8a66
    val mGraphColor = -0xcc5600
    val mShadowColor = 0x77000000
    val mDiamondSize = 10
    var mDashPathEffect: DashPathEffect
    var mKeyFrameCount = 0
    var mBounds = Rect()
    var mPresentationMode = false
    var mShadowTranslate = 1

    init {
        mPaint = Paint()
        mPaint.isAntiAlias = true
        mPaint.color = mRedColor
        mPaint.strokeWidth = 2f
        mPaint.style = Paint.Style.STROKE
        mPaintKeyframes = Paint()
        mPaintKeyframes.isAntiAlias = true
        mPaintKeyframes.color = mKeyframeColor
        mPaintKeyframes.strokeWidth = 2f
        mPaintKeyframes.style = Paint.Style.STROKE
        mPaintGraph = Paint()
        mPaintGraph.isAntiAlias = true
        mPaintGraph.color = mGraphColor
        mPaintGraph.strokeWidth = 2f
        mPaintGraph.style = Paint.Style.STROKE
        mTextPaint = Paint()
        mTextPaint.isAntiAlias = true
        mTextPaint.color = mGraphColor
        mTextPaint.textSize = textSize
        mRectangle = FloatArray(8)
        mFillPaint = Paint()
        mFillPaint.isAntiAlias = true
        mDashPathEffect = DashPathEffect(floatArrayOf(4f, 8f), 0f)
        mPaintGraph.pathEffect = mDashPathEffect
        mKeyFramePoints = FloatArray(MAX_KEY_FRAMES * 2)
        mPathMode = IntArray(MAX_KEY_FRAMES)
        if (mPresentationMode) {
            mPaint.strokeWidth = 8f
            mFillPaint.strokeWidth = 8f
            mPaintKeyframes.strokeWidth = 8f
            mShadowTranslate = 4
        }
    }

    fun draw(
        canvas: Canvas,
        frameArrayList: HashMap<String?, Motion>?,
        duration: Int,
        debugPath: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        if (frameArrayList == null || frameArrayList.size == 0) {
            return
        }
        canvas.save()
        for (motionController in frameArrayList.values) {
            draw(
                canvas, motionController, duration, debugPath,
                layoutWidth, layoutHeight
            )
        }
        canvas.restore()
    }

    fun draw(
        canvas: Canvas,
        motionController: Motion,
        duration: Int,
        debugPath: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        var mode = motionController.drawPath
        if (debugPath > 0 && mode == Motion.DRAW_PATH_NONE) {
            mode = Motion.DRAW_PATH_BASIC
        }
        if (mode == Motion.DRAW_PATH_NONE) { // do not draw path
            return
        }
        mKeyFrameCount = motionController.buildKeyFrames(mKeyFramePoints, mPathMode, null)
        if (mode >= Motion.DRAW_PATH_BASIC) {
            val frames = duration / DEBUG_PATH_TICKS_PER_MS
            if (mPoints == null || mPoints!!.size != frames * 2) {
                mPoints = FloatArray(frames * 2)
                mPath = Path()
            }
            canvas.translate(mShadowTranslate.toFloat(), mShadowTranslate.toFloat())
            mPaint.color = mShadowColor
            mFillPaint.color = mShadowColor
            mPaintKeyframes.color = mShadowColor
            mPaintGraph.color = mShadowColor
            motionController.buildPath(mPoints, frames)
            drawAll(canvas, mode, mKeyFrameCount, motionController, layoutWidth, layoutHeight)
            mPaint.color = mRedColor
            mPaintKeyframes.color = mKeyframeColor
            mFillPaint.color = mKeyframeColor
            mPaintGraph.color = mGraphColor
            canvas.translate(-mShadowTranslate.toFloat(), -mShadowTranslate.toFloat())
            drawAll(canvas, mode, mKeyFrameCount, motionController, layoutWidth, layoutHeight)
            if (mode == Motion.DRAW_PATH_RECTANGLE) {
                drawRectangle(canvas, motionController)
            }
        }
    }

    fun drawAll(
        canvas: Canvas,
        mode: Int,
        keyFrames: Int,
        motionController: Motion,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        if (mode == Motion.DRAW_PATH_AS_CONFIGURED) {
            drawPathAsConfigured(canvas)
        }
        if (mode == Motion.DRAW_PATH_RELATIVE) {
            drawPathRelative(canvas)
        }
        if (mode == Motion.DRAW_PATH_CARTESIAN) {
            drawPathCartesian(canvas)
        }
        drawBasicPath(canvas)
        drawTicks(canvas, mode, keyFrames, motionController, layoutWidth, layoutHeight)
    }

    /**
     * Draws the paths of the given [motionController][Motion], forcing the drawing mode
     * [Motion.DRAW_PATH_BASIC].
     *
     * @param canvas Canvas instance used to draw on
     * @param motionController Controller containing path information
     * @param duration Defined in milliseconds, sets the amount of ticks used to draw the path
     * based on [.DEBUG_PATH_TICKS_PER_MS]
     * @param layoutWidth Width of the containing MotionLayout
     * @param layoutHeight Height of the containing MotionLayout
     * @param drawPath Whether to draw the path, paths are drawn using dashed lines
     * @param drawTicks Whether to draw diamond shaped ticks that indicate KeyPositions along a path
     */
    fun basicDraw(
        canvas: Canvas,
        motionController: Motion,
        duration: Int,
        layoutWidth: Int,
        layoutHeight: Int,
        drawPath: Boolean,
        drawTicks: Boolean
    ) {
        val mode = Motion.DRAW_PATH_BASIC
        mKeyFrameCount = motionController.buildKeyFrames(mKeyFramePoints, mPathMode, null)
        val frames = duration / DEBUG_PATH_TICKS_PER_MS
        if (mPoints == null || mPoints!!.size != frames * 2) {
            mPoints = FloatArray(frames * 2)
            mPath = Path()
        }
        canvas.translate(mShadowTranslate.toFloat(), mShadowTranslate.toFloat())
        mPaint.color = mShadowColor
        mFillPaint.color = mShadowColor
        mPaintKeyframes.color = mShadowColor
        mPaintGraph.color = mShadowColor
        motionController.buildPath(mPoints, frames)
        if (drawPath) {
            drawBasicPath(canvas)
        }
        if (drawTicks) {
            drawTicks(canvas, mode, mKeyFrameCount, motionController, layoutWidth, layoutHeight)
        }
        mPaint.color = mRedColor
        mPaintKeyframes.color = mKeyframeColor
        mFillPaint.color = mKeyframeColor
        mPaintGraph.color = mGraphColor
        canvas.translate(-mShadowTranslate.toFloat(), -mShadowTranslate.toFloat())
        if (drawPath) {
            drawBasicPath(canvas)
        }
        if (drawTicks) {
            drawTicks(canvas, mode, mKeyFrameCount, motionController, layoutWidth, layoutHeight)
        }
    }

    private fun drawBasicPath(canvas: Canvas) {
        canvas.drawLines(mPoints!!, mPaint)
    }

    private fun drawTicks(
        canvas: Canvas,
        mode: Int,
        keyFrames: Int,
        motionController: Motion,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        var viewWidth = 0
        var viewHeight = 0
        if (motionController.view != null) {
            viewWidth = motionController.view.width
            viewHeight = motionController.view.height
        }
        for (i in 1 until keyFrames - 1) {
            if (
                mode == Motion.DRAW_PATH_AS_CONFIGURED && mPathMode[i - 1] == Motion.DRAW_PATH_NONE
            ) {
                continue
            }
            val x = mKeyFramePoints[i * 2]
            val y = mKeyFramePoints[i * 2 + 1]
            mPath!!.reset()
            mPath!!.moveTo(x, y + mDiamondSize)
            mPath!!.lineTo(x + mDiamondSize, y)
            mPath!!.lineTo(x, y - mDiamondSize)
            mPath!!.lineTo(x - mDiamondSize, y)
            mPath!!.close()
            val dx = 0f // framePoint.translationX
            val dy = 0f // framePoint.translationY
            if (mode == Motion.DRAW_PATH_AS_CONFIGURED) {
                if (mPathMode[i - 1] == MotionPaths.PERPENDICULAR) {
                    drawPathRelativeTicks(canvas, x - dx, y - dy)
                } else if (mPathMode[i - 1] == MotionPaths.CARTESIAN) {
                    drawPathCartesianTicks(canvas, x - dx, y - dy)
                } else if (mPathMode[i - 1] == MotionPaths.SCREEN) {
                    drawPathScreenTicks(
                        canvas, x - dx, y - dy,
                        viewWidth, viewHeight, layoutWidth, layoutHeight
                    )
                }
                canvas.drawPath(mPath!!, mFillPaint)
            }
            if (mode == Motion.DRAW_PATH_RELATIVE) {
                drawPathRelativeTicks(canvas, x - dx, y - dy)
            }
            if (mode == Motion.DRAW_PATH_CARTESIAN) {
                drawPathCartesianTicks(canvas, x - dx, y - dy)
            }
            if (mode == Motion.DRAW_PATH_SCREEN) {
                drawPathScreenTicks(
                    canvas, x - dx, y - dy,
                    viewWidth, viewHeight, layoutWidth, layoutHeight
                )
            }
            if (dx != 0f || dy != 0f) {
                drawTranslation(canvas, x - dx, y - dy, x, y)
            } else {
                canvas.drawPath(mPath!!, mFillPaint)
            }
        }
        if (mPoints!!.size > 1) {
            // Draw the starting and ending circle
            canvas.drawCircle(mPoints!![0], mPoints!![1], 8f, mPaintKeyframes)
            canvas.drawCircle(
                mPoints!![mPoints!!.size - 2],
                mPoints!![mPoints!!.size - 1], 8f, mPaintKeyframes
            )
        }
    }

    private fun drawTranslation(canvas: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) {
        canvas.drawRect(x1, y1, x2, y2, mPaintGraph)
        canvas.drawLine(x1, y1, x2, y2, mPaintGraph)
    }

    private fun drawPathRelative(canvas: Canvas) {
        canvas.drawLine(
            mPoints!![0], mPoints!![1],
            mPoints!![mPoints!!.size - 2], mPoints!![mPoints!!.size - 1], mPaintGraph
        )
    }

    private fun drawPathAsConfigured(canvas: Canvas) {
        var path = false
        var cart = false
        for (i in 0 until mKeyFrameCount) {
            if (mPathMode[i] == MotionPaths.PERPENDICULAR) {
                path = true
            }
            if (mPathMode[i] == MotionPaths.CARTESIAN) {
                cart = true
            }
        }
        if (path) {
            drawPathRelative(canvas)
        }
        if (cart) {
            drawPathCartesian(canvas)
        }
    }

    private fun drawPathRelativeTicks(canvas: Canvas, x: Float, y: Float) {
        val x1 = mPoints!![0]
        val y1 = mPoints!![1]
        val x2 = mPoints!![mPoints!!.size - 2]
        val y2 = mPoints!![mPoints!!.size - 1]
        val dist = Math.hypot((x1 - x2).toDouble(), (y1 - y2).toDouble()).toFloat()
        val t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (dist * dist)
        val xp = x1 + t * (x2 - x1)
        val yp = y1 + t * (y2 - y1)
        val path = Path()
        path.moveTo(x, y)
        path.lineTo(xp, yp)
        val len = Math.hypot((xp - x).toDouble(), (yp - y).toDouble()).toFloat()
        val text = "" + (100 * len / dist).toInt() / 100.0f
        getTextBounds(text, mTextPaint)
        val off = len / 2 - mBounds.width() / 2
        canvas.drawTextOnPath(text, path, off, -20f, mTextPaint)
        canvas.drawLine(x, y, xp, yp, mPaintGraph)
    }

    fun getTextBounds(text: String, paint: Paint) {
        paint.getTextBounds(text, 0, text.length, mBounds)
    }

    private fun drawPathCartesian(canvas: Canvas) {
        val x1 = mPoints!![0]
        val y1 = mPoints!![1]
        val x2 = mPoints!![mPoints!!.size - 2]
        val y2 = mPoints!![mPoints!!.size - 1]
        canvas.drawLine(
            Math.min(x1, x2), Math.max(y1, y2),
            Math.max(x1, x2), Math.max(y1, y2), mPaintGraph
        )
        canvas.drawLine(
            Math.min(x1, x2), Math.min(y1, y2),
            Math.min(x1, x2), Math.max(y1, y2), mPaintGraph
        )
    }

    private fun drawPathCartesianTicks(canvas: Canvas, x: Float, y: Float) {
        val x1 = mPoints!![0]
        val y1 = mPoints!![1]
        val x2 = mPoints!![mPoints!!.size - 2]
        val y2 = mPoints!![mPoints!!.size - 1]
        val minx = Math.min(x1, x2)
        val maxy = Math.max(y1, y2)
        val xgap = x - Math.min(x1, x2)
        val ygap = Math.max(y1, y2) - y
        // Horizontal line
        var text = "" + (0.5 + 100 * xgap / Math.abs(x2 - x1)).toInt() / 100.0f
        getTextBounds(text, mTextPaint)
        var off = xgap / 2 - mBounds.width() / 2
        canvas.drawText(text, off + minx, y - 20, mTextPaint)
        canvas.drawLine(
            x, y,
            Math.min(x1, x2), y, mPaintGraph
        )

        // Vertical line
        text = "" + (0.5 + 100 * ygap / Math.abs(y2 - y1)).toInt() / 100.0f
        getTextBounds(text, mTextPaint)
        off = ygap / 2 - mBounds.height() / 2
        canvas.drawText(text, x + 5, maxy - off, mTextPaint)
        canvas.drawLine(
            x, y,
            x, Math.max(y1, y2), mPaintGraph
        )
    }

    private fun drawPathScreenTicks(
        canvas: Canvas,
        x: Float,
        y: Float,
        viewWidth: Int,
        viewHeight: Int,
        layoutWidth: Int,
        layoutHeight: Int
    ) {
        val x1 = 0f
        val y1 = 0f
        val x2 = 1f
        val y2 = 1f
        val minx = 0f
        val maxy = 0f
        // Horizontal line
        var text = "" + (0.5 + 100 * (x - viewWidth / 2) /
            (layoutWidth - viewWidth)).toInt() / 100.0f
        getTextBounds(text, mTextPaint)
        var off = x / 2 - mBounds.width() / 2
        canvas.drawText(text, off + minx, y - 20, mTextPaint)
        canvas.drawLine(
            x, y,
            Math.min(x1, x2), y, mPaintGraph
        )

        // Vertical line
        text = "" + (0.5 + 100 * (y - viewHeight / 2) /
            (layoutHeight - viewHeight)).toInt() / 100.0f
        getTextBounds(text, mTextPaint)
        off = y / 2 - mBounds.height() / 2
        canvas.drawText(text, x + 5, maxy - off, mTextPaint)
        canvas.drawLine(
            x, y,
            x, Math.max(y1, y2), mPaintGraph
        )
    }

    private fun drawRectangle(canvas: Canvas, motionController: Motion) {
        mPath!!.reset()
        val rectFrames = 50
        for (i in 0..rectFrames) {
            val p = i / rectFrames.toFloat()
            motionController.buildRect(p, mRectangle, 0)
            mPath!!.moveTo(mRectangle[0], mRectangle[1])
            mPath!!.lineTo(mRectangle[2], mRectangle[3])
            mPath!!.lineTo(mRectangle[4], mRectangle[5])
            mPath!!.lineTo(mRectangle[6], mRectangle[7])
            mPath!!.close()
        }
        mPaint.color = 0x44000000
        canvas.translate(2f, 2f)
        canvas.drawPath(mPath!!, mPaint)
        canvas.translate(-2f, -2f)
        mPaint.color = -0x10000
        canvas.drawPath(mPath!!, mPaint)
    }

    companion object {
        const val DEBUG_SHOW_NONE = 0
        const val DEBUG_SHOW_PROGRESS = 1
        const val DEBUG_SHOW_PATH = 2
        const val MAX_KEY_FRAMES = 50
        private const val DEBUG_PATH_TICKS_PER_MS = 16
    }
}
