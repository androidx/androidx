/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.renderer.internal;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.tiles.renderer.R;

/**
 * A line, drawn inside an arc.
 *
 * <p>This widget takes three parameters, the thickness of the line to draw, its sweep angle, and
 * the color to draw with. This widget will then draw an arc, with the specified thickness, around
 * its parent arc. The sweep angle is specified in degrees, clockwise.
 */
public class WearCurvedLineView extends View implements WearArcLayout.ArcLayoutWidget {
    private static final int DEFAULT_THICKNESS_PX = 0;
    private static final float DEFAULT_SWEEP_ANGLE_DEGREES = 0;
    @ColorInt private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private int mThicknessPx;
    private float mSweepAngleDegrees;
    @ColorInt private int mColor;

    private Paint mPaint;
    private Path mPath;

    public WearCurvedLineView(@NonNull Context context) {
        this(context, null);
    }

    public WearCurvedLineView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearCurvedLineView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WearCurvedLineView(
            @NonNull Context context,
            @Nullable AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a =
                context.obtainStyledAttributes(
                        attrs, R.styleable.WearCurvedLineView, defStyleAttr, defStyleRes);

        mThicknessPx =
                (int)
                        a.getDimension(
                                R.styleable.WearCurvedLineView_thickness, DEFAULT_THICKNESS_PX);
        mColor = a.getColor(R.styleable.WearCurvedLineView_color, DEFAULT_COLOR);
        mSweepAngleDegrees =
                a.getFloat(
                        R.styleable.WearCurvedLineView_sweepAngleDegrees,
                        DEFAULT_SWEEP_ANGLE_DEGREES);

        a.recycle();
    }

    private void updatePathAndPaint() {
        mPath = new Path();
        float insetPx = mThicknessPx / 2f;

        if (mSweepAngleDegrees >= 360f) {
            // Android internally will take the modulus of the angle with 360, so drawing a full
            // ring can't be done using path.arcTo. In that case, just draw a circle.
            mPath.addOval(
                    insetPx,
                    insetPx,
                    this.getMeasuredWidth() - insetPx,
                    this.getMeasuredHeight() - insetPx,
                    Direction.CW);
        } else if (mSweepAngleDegrees != 0) {
            // The arc needs to be offset by -90 degrees. The ArcContainer will rotate this widget
            // such that the "12 o clock" position on the canvas is aligned to the center of our
            // requested angle, but 0 degrees in Android corresponds to the "3 o clock" position.
            mPath.moveTo(0, 0); // Work-around for b/177676885
            mPath.arcTo(
                    insetPx,
                    insetPx,
                    this.getMeasuredWidth() - insetPx,
                    this.getMeasuredHeight() - insetPx,
                    -90 - (mSweepAngleDegrees / 2f),
                    mSweepAngleDegrees,
                    true);
        }

        mPaint = new Paint();
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeCap(Cap.ROUND);
        mPaint.setColor(mColor);
        mPaint.setStrokeWidth(mThicknessPx);
        mPaint.setAntiAlias(true);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updatePathAndPaint();
    }

    @Override
    public int getThicknessPx() {
        return mThicknessPx;
    }

    /** Sets the thickness of this arc in pixels. */
    public void setThicknessPx(int thicknessPx) {
        if (thicknessPx < 0) {
            thicknessPx = 0;
        }

        this.mThicknessPx = thicknessPx;
        updatePathAndPaint();
        invalidate();
    }

    @Override
    public float getSweepAngleDegrees() {
        return mSweepAngleDegrees;
    }

    /** Sets the sweep angle of this arc in degrees. */
    public void setSweepAngleDegrees(float sweepAngleDegrees) {
        this.mSweepAngleDegrees = sweepAngleDegrees;
        updatePathAndPaint();
        invalidate();
    }

    /** Returns the color of this arc, in ARGB format. */
    @ColorInt
    public int getColor() {
        return mColor;
    }

    /** Sets the color of this arc, in ARGB format. */
    public void setColor(@ColorInt int color) {
        this.mColor = color;
        updatePathAndPaint();
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    @Override
    public void checkInvalidAttributeAsChild() {
        // Nothing required...
    }

    @Override
    public boolean insideClickArea(float x, float y) {
        // Stolen from WearCurvedTextView...
        float radius2 = min(getWidth(), getHeight()) / 2f - getPaddingTop();
        float radius1 = radius2 - mThicknessPx;

        float dx = x - getWidth() / 2;
        float dy = y - getHeight() / 2;

        float r2 = dx * dx + dy * dy;
        if (r2 < radius1 * radius1 || r2 > radius2 * radius2) {
            return false;
        }

        // Since we are symmetrical on the Y-axis, we can constrain the angle to the x>=0 quadrants.
        float angle = (float) Math.toDegrees(Math.atan2(Math.abs(dx), -dy));
        return angle < mSweepAngleDegrees / 2;
    }
}
