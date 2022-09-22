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
package androidx.constraintlayout.compose;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import androidx.constraintlayout.core.motion.Motion;
import androidx.constraintlayout.core.motion.MotionPaths;

import java.util.HashMap;

class MotionRenderDebug {
    public static final int DEBUG_SHOW_NONE = 0;
    public static final int DEBUG_SHOW_PROGRESS = 1;
    public static final int DEBUG_SHOW_PATH = 2;

    static final int MAX_KEY_FRAMES = 50;
    private static final int DEBUG_PATH_TICKS_PER_MS = 16;
    float[] mPoints;
    int[] mPathMode;
    float[] mKeyFramePoints;
    Path mPath;
    Paint mPaint;
    Paint mPaintKeyframes;
    Paint mPaintGraph;
    Paint mTextPaint;
    Paint mFillPaint;
    private float[] mRectangle;
    final int mRedColor = 0xFFFFAA33;
    final int mKeyframeColor = 0xffe0759a;
    final int mGraphColor = 0xFF33AA00;
    final int mShadowColor = 0x77000000;
    final int mDiamondSize = 10;
    DashPathEffect mDashPathEffect;
    int mKeyFrameCount;
    Rect mBounds = new Rect();
    boolean mPresentationMode = false;
    int mShadowTranslate = 1;

    MotionRenderDebug(float textSize) {

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(mRedColor);
        mPaint.setStrokeWidth(2);
        mPaint.setStyle(Paint.Style.STROKE);

        mPaintKeyframes = new Paint();
        mPaintKeyframes.setAntiAlias(true);
        mPaintKeyframes.setColor(mKeyframeColor);
        mPaintKeyframes.setStrokeWidth(2);
        mPaintKeyframes.setStyle(Paint.Style.STROKE);

        mPaintGraph = new Paint();
        mPaintGraph.setAntiAlias(true);
        mPaintGraph.setColor(mGraphColor);
        mPaintGraph.setStrokeWidth(2);
        mPaintGraph.setStyle(Paint.Style.STROKE);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(mGraphColor);
        mTextPaint.setTextSize(textSize);
        mRectangle = new float[8];
        mFillPaint = new Paint();
        mFillPaint.setAntiAlias(true);
        mDashPathEffect = new DashPathEffect(new float[]{4, 8}, 0);
        mPaintGraph.setPathEffect(mDashPathEffect);
        mKeyFramePoints = new float[MAX_KEY_FRAMES * 2];
        mPathMode = new int[MAX_KEY_FRAMES];

        if (mPresentationMode) {
            mPaint.setStrokeWidth(8);
            mFillPaint.setStrokeWidth(8);
            mPaintKeyframes.setStrokeWidth(8);
            mShadowTranslate = 4;
        }
    }

    public void draw(Canvas canvas,
            HashMap<String, Motion> frameArrayList,
            int duration, int debugPath,
            int layoutWidth, int layoutHeight) {
        if (frameArrayList == null || frameArrayList.size() == 0) {
            return;
        }
        canvas.save();

        for (Motion motionController : frameArrayList.values()) {
            draw(canvas, motionController, duration, debugPath,
                    layoutWidth, layoutHeight);
        }
        canvas.restore();
    }

    public void draw(Canvas canvas,
            Motion motionController,
            int duration, int debugPath,
            int layoutWidth, int layoutHeight) {
        int mode = motionController.getDrawPath();
        if (debugPath > 0 && mode == Motion.DRAW_PATH_NONE) {
            mode = Motion.DRAW_PATH_BASIC;
        }
        if (mode == Motion.DRAW_PATH_NONE) { // do not draw path
            return;
        }

        mKeyFrameCount = motionController.buildKeyFrames(mKeyFramePoints, mPathMode, null);

        if (mode >= Motion.DRAW_PATH_BASIC) {

            int frames = duration / DEBUG_PATH_TICKS_PER_MS;
            if (mPoints == null || mPoints.length != frames * 2) {
                mPoints = new float[frames * 2];
                mPath = new Path();
            }

            canvas.translate(mShadowTranslate, mShadowTranslate);

            mPaint.setColor(mShadowColor);
            mFillPaint.setColor(mShadowColor);
            mPaintKeyframes.setColor(mShadowColor);
            mPaintGraph.setColor(mShadowColor);
            motionController.buildPath(mPoints, frames);
            drawAll(canvas, mode, mKeyFrameCount, motionController, layoutWidth, layoutHeight);
            mPaint.setColor(mRedColor);
            mPaintKeyframes.setColor(mKeyframeColor);
            mFillPaint.setColor(mKeyframeColor);
            mPaintGraph.setColor(mGraphColor);

            canvas.translate(-mShadowTranslate, -mShadowTranslate);
            drawAll(canvas, mode, mKeyFrameCount, motionController, layoutWidth, layoutHeight);
            if (mode == Motion.DRAW_PATH_RECTANGLE) {
                drawRectangle(canvas, motionController);
            }
        }

    }


    public void drawAll(Canvas canvas, int mode, int keyFrames, Motion motionController,
            int layoutWidth, int layoutHeight) {
        if (mode == Motion.DRAW_PATH_AS_CONFIGURED) {
            drawPathAsConfigured(canvas);
        }
        if (mode == Motion.DRAW_PATH_RELATIVE) {
            drawPathRelative(canvas);
        }
        if (mode == Motion.DRAW_PATH_CARTESIAN) {
            drawPathCartesian(canvas);
        }
        drawBasicPath(canvas);
        drawTicks(canvas, mode, keyFrames, motionController, layoutWidth, layoutHeight);
    }

    private void drawBasicPath(Canvas canvas) {
        canvas.drawLines(mPoints, mPaint);
    }

    private void drawTicks(Canvas canvas, int mode, int keyFrames, Motion motionController,
            int layoutWidth, int layoutHeight) {
        int viewWidth = 0;
        int viewHeight = 0;
        if (motionController.getView() != null) {
            viewWidth = motionController.getView().getWidth();
            viewHeight = motionController.getView().getHeight();
        }
        for (int i = 1; i < keyFrames - 1; i++) {
            if (mode == Motion.DRAW_PATH_AS_CONFIGURED
                    && mPathMode[i - 1] == Motion.DRAW_PATH_NONE) {
                continue;

            }
            float x = mKeyFramePoints[i * 2];
            float y = mKeyFramePoints[i * 2 + 1];
            mPath.reset();
            mPath.moveTo(x, y + mDiamondSize);
            mPath.lineTo(x + mDiamondSize, y);
            mPath.lineTo(x, y - mDiamondSize);
            mPath.lineTo(x - mDiamondSize, y);
            mPath.close();
            float dx = 0; //framePoint.translationX
            float dy = 0; //framePoint.translationY
            if (mode == Motion.DRAW_PATH_AS_CONFIGURED) {

                if (mPathMode[i - 1] == MotionPaths.PERPENDICULAR) {
                    drawPathRelativeTicks(canvas, x - dx, y - dy);
                } else if (mPathMode[i - 1] == MotionPaths.CARTESIAN) {
                    drawPathCartesianTicks(canvas, x - dx, y - dy);
                } else if (mPathMode[i - 1] == MotionPaths.SCREEN) {
                    drawPathScreenTicks(canvas, x - dx, y - dy,
                            viewWidth, viewHeight, layoutWidth, layoutHeight);
                }

                canvas.drawPath(mPath, mFillPaint);
            }
            if (mode == Motion.DRAW_PATH_RELATIVE) {
                drawPathRelativeTicks(canvas, x - dx, y - dy);
            }
            if (mode == Motion.DRAW_PATH_CARTESIAN) {
                drawPathCartesianTicks(canvas, x - dx, y - dy);
            }
            if (mode == Motion.DRAW_PATH_SCREEN) {
                drawPathScreenTicks(canvas, x - dx, y - dy,
                        viewWidth, viewHeight, layoutWidth, layoutHeight);
            }
            if (dx != 0 || dy != 0) {
                drawTranslation(canvas, x - dx, y - dy, x, y);
            } else {
                canvas.drawPath(mPath, mFillPaint);
            }
        }
        if (mPoints.length > 1) {
            // Draw the starting and ending circle
            canvas.drawCircle(mPoints[0], mPoints[1], 8, mPaintKeyframes);
            canvas.drawCircle(mPoints[mPoints.length - 2],
                    mPoints[mPoints.length - 1], 8, mPaintKeyframes);
        }
    }

    private void drawTranslation(Canvas canvas, float x1, float y1, float x2, float y2) {
        canvas.drawRect(x1, y1, x2, y2, mPaintGraph);
        canvas.drawLine(x1, y1, x2, y2, mPaintGraph);
    }

    private void drawPathRelative(Canvas canvas) {
        canvas.drawLine(mPoints[0], mPoints[1],
                mPoints[mPoints.length - 2], mPoints[mPoints.length - 1], mPaintGraph);
    }

    private void drawPathAsConfigured(Canvas canvas) {
        boolean path = false;
        boolean cart = false;
        for (int i = 0; i < mKeyFrameCount; i++) {
            if (mPathMode[i] == MotionPaths.PERPENDICULAR) {
                path = true;
            }
            if (mPathMode[i] == MotionPaths.CARTESIAN) {
                cart = true;
            }
        }
        if (path) {
            drawPathRelative(canvas);
        }
        if (cart) {
            drawPathCartesian(canvas);
        }
    }

    private void drawPathRelativeTicks(Canvas canvas, float x, float y) {
        float x1 = mPoints[0];
        float y1 = mPoints[1];
        float x2 = mPoints[mPoints.length - 2];
        float y2 = mPoints[mPoints.length - 1];
        float dist = (float) Math.hypot(x1 - x2, y1 - y2);
        float t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / (dist * dist);
        float xp = x1 + t * (x2 - x1);
        float yp = y1 + t * (y2 - y1);

        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(xp, yp);
        float len = (float) Math.hypot(xp - x, yp - y);
        String text = "" + ((int) (100 * len / dist)) / 100.0f;
        getTextBounds(text, mTextPaint);
        float off = len / 2 - mBounds.width() / 2;
        canvas.drawTextOnPath(text, path, off, -20, mTextPaint);
        canvas.drawLine(x, y, xp, yp, mPaintGraph);
    }

    void getTextBounds(String text, Paint paint) {
        paint.getTextBounds(text, 0, text.length(), mBounds);
    }

    private void drawPathCartesian(Canvas canvas) {
        float x1 = mPoints[0];
        float y1 = mPoints[1];
        float x2 = mPoints[mPoints.length - 2];
        float y2 = mPoints[mPoints.length - 1];

        canvas.drawLine(Math.min(x1, x2), Math.max(y1, y2),
                Math.max(x1, x2), Math.max(y1, y2), mPaintGraph);
        canvas.drawLine(Math.min(x1, x2), Math.min(y1, y2),
                Math.min(x1, x2), Math.max(y1, y2), mPaintGraph);
    }

    private void drawPathCartesianTicks(Canvas canvas, float x, float y) {
        float x1 = mPoints[0];
        float y1 = mPoints[1];
        float x2 = mPoints[mPoints.length - 2];
        float y2 = mPoints[mPoints.length - 1];
        float minx = Math.min(x1, x2);
        float maxy = Math.max(y1, y2);
        float xgap = x - Math.min(x1, x2);
        float ygap = Math.max(y1, y2) - y;
        // Horizontal line
        String text = "" + ((int) (0.5 + 100 * xgap / Math.abs(x2 - x1))) / 100.0f;
        getTextBounds(text, mTextPaint);
        float off = xgap / 2 - mBounds.width() / 2;
        canvas.drawText(text, off + minx, y - 20, mTextPaint);
        canvas.drawLine(x, y,
                Math.min(x1, x2), y, mPaintGraph);

        // Vertical line
        text = "" + ((int) (0.5 + 100 * ygap / Math.abs(y2 - y1))) / 100.0f;
        getTextBounds(text, mTextPaint);
        off = ygap / 2 - mBounds.height() / 2;
        canvas.drawText(text, x + 5, maxy - off, mTextPaint);
        canvas.drawLine(x, y,
                x, Math.max(y1, y2), mPaintGraph);
    }

    private void drawPathScreenTicks(Canvas canvas, float x, float y, int viewWidth, int viewHeight,
            int layoutWidth, int layoutHeight) {
        float x1 = 0;
        float y1 = 0;
        float x2 = 1;
        float y2 = 1;
        float minx = 0;
        float maxy = 0;
        float xgap = x;
        float ygap = y;
        // Horizontal line
        String text = "" + ((int) (0.5 + 100 * (xgap - viewWidth / 2)
                / (layoutWidth - viewWidth))) / 100.0f;
        getTextBounds(text, mTextPaint);
        float off = xgap / 2 - mBounds.width() / 2;
        canvas.drawText(text, off + minx, y - 20, mTextPaint);
        canvas.drawLine(x, y,
                Math.min(x1, x2), y, mPaintGraph);

        // Vertical line
        text = "" + ((int) (0.5 + 100 * (ygap - viewHeight / 2)
                / (layoutHeight - viewHeight))) / 100.0f;
        getTextBounds(text, mTextPaint);
        off = ygap / 2 - mBounds.height() / 2;
        canvas.drawText(text, x + 5, maxy - off, mTextPaint);
        canvas.drawLine(x, y,
                x, Math.max(y1, y2), mPaintGraph);
    }

    private void drawRectangle(Canvas canvas, Motion motionController) {
        mPath.reset();
        int rectFrames = 50;
        for (int i = 0; i <= rectFrames; i++) {
            float p = i / (float) rectFrames;
            motionController.buildRect(p, mRectangle, 0);
            mPath.moveTo(mRectangle[0], mRectangle[1]);
            mPath.lineTo(mRectangle[2], mRectangle[3]);
            mPath.lineTo(mRectangle[4], mRectangle[5]);
            mPath.lineTo(mRectangle[6], mRectangle[7]);
            mPath.close();
        }
        mPaint.setColor(0x44000000);
        canvas.translate(2, 2);
        canvas.drawPath(mPath, mPaint);

        canvas.translate(-2, -2);
        mPaint.setColor(0xFFFF0000);
        canvas.drawPath(mPath, mPaint);
    }

}
