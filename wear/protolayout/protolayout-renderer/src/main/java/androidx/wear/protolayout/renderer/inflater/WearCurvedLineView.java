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

package androidx.wear.protolayout.renderer.inflater;

import static java.lang.Math.min;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.wear.protolayout.renderer.R;
import androidx.wear.widget.ArcLayout;

/**
 * A line, drawn inside an arc.
 *
 * <p>This widget takes four parameters, the thickness of the line to draw, optionally the sweep
 * angle of the "container", the sweep angle of the line, and the color to draw with. This widget
 * will then draw an arc, with the specified thickness, around its parent arc. All sweep angles are
 * specified in degrees, clockwise.
 *
 * <p>The "container" length is used when calculating how much of the parent arc to occupy, such
 * that the line length can grow/shrink within that container length without affecting the elements
 * around it. If the line length is greater than the container length, then the line will be
 * truncated to fit inside the container.
 */
public class WearCurvedLineView extends View implements ArcLayout.Widget {
    public static final float SWEEP_ANGLE_WRAP_LENGTH = -1;

    private static final int DEFAULT_THICKNESS_PX = 0;
    private static final float DEFAULT_MAX_SWEEP_ANGLE_DEGREES = SWEEP_ANGLE_WRAP_LENGTH;
    private static final float DEFAULT_LINE_SWEEP_ANGLE_DEGREES = 0;
    private static final int DEFAULT_LINE_STROKE_CAP = Cap.ROUND.ordinal();
    @ColorInt private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    /**
     * The base angle for drawings. The zero angle in Android corresponds to the "3 o clock"
     * position, while ProtoLayout and ArcLayout use the "12 o clock" position as zero.
     */
    private static final float BASE_DRAW_ANGLE_SHIFT = -90f;

    private int mThicknessPx;

    private float mMaxSweepAngleDegrees;
    private float mLineSweepAngleDegrees;

    @ColorInt private int mColor;

    @Nullable private ArcDrawable mArcDrawable;
    @NonNull private Cap mCap;

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
        mMaxSweepAngleDegrees =
                a.getFloat(
                        R.styleable.WearCurvedLineView_maxSweepAngleDegrees,
                        DEFAULT_MAX_SWEEP_ANGLE_DEGREES);
        mLineSweepAngleDegrees =
                a.getFloat(
                        R.styleable.WearCurvedLineView_sweepAngleDegrees,
                        DEFAULT_LINE_SWEEP_ANGLE_DEGREES);
        mCap =
                Cap.values()[
                        a.getInt(
                                R.styleable.WearCurvedLineView_strokeCap, DEFAULT_LINE_STROKE_CAP)];
        a.recycle();
    }

    private Paint makePaint() {
        Paint paint = new Paint();
        paint.setStyle(Style.STROKE);
        paint.setStrokeCap(mCap);
        paint.setColor(mColor);
        paint.setStrokeWidth(mThicknessPx);
        paint.setAntiAlias(true);
        return paint;
    }

    private void updateArcDrawable() {
        float insetPx = mThicknessPx / 2f;
        RectF bounds =
                new RectF(
                        insetPx,
                        insetPx,
                        getMeasuredWidth() - insetPx,
                        getMeasuredHeight() - insetPx);
        float clampedLineLength = resolveSweepAngleDegrees();

        Paint paint = makePaint();
        mArcDrawable = new ArcDrawableLegacy(bounds, clampedLineLength, paint);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        updateArcDrawable();
    }

    /** Sets the thickness of this arc in pixels. */
    public void setThickness(int thickness) {
        if (thickness < 0) {
            thickness = 0;
        }

        this.mThicknessPx = thickness;
        updateArcDrawable();
        requestLayout();
        postInvalidate();
    }

    private float resolveSweepAngleDegrees() {
        return mMaxSweepAngleDegrees == SWEEP_ANGLE_WRAP_LENGTH
                ? mLineSweepAngleDegrees
                : min(mLineSweepAngleDegrees, mMaxSweepAngleDegrees);
    }

    @Override
    public float getSweepAngleDegrees() {
        return resolveSweepAngleDegrees();
    }

    @Override
    public void setSweepAngleDegrees(float sweepAngleDegrees) {
        this.mLineSweepAngleDegrees = sweepAngleDegrees;
    }

    /** Gets the sweep angle of the actual line contained within this CurvedLineView. */
    public float getLineSweepAngleDegrees() {
        return mLineSweepAngleDegrees;
    }

    @Override
    public int getThickness() {
        return mThicknessPx;
    }

    /**
     * Sets the maximum sweep angle of the line, in degrees. If a max size is not required, pass
     * {@link WearCurvedLineView#SWEEP_ANGLE_WRAP_LENGTH} instead.
     */
    public void setMaxSweepAngleDegrees(float maxSweepAngleDegrees) {
        this.mMaxSweepAngleDegrees = maxSweepAngleDegrees;
        updateArcDrawable();
        requestLayout();
        postInvalidate();
    }

    /**
     * Gets the maximum sweep angle of the line, in degrees. If a max size is not set, this will
     * return {@link WearCurvedLineView#SWEEP_ANGLE_WRAP_LENGTH}.
     */
    public float getMaxSweepAngleDegrees() {
        return mMaxSweepAngleDegrees;
    }

    /**
     * Sets the length of the line contained within this CurvedLineView. If this is greater than the
     * max sweep angle set using {@link WearCurvedLineView#setMaxSweepAngleDegrees(float)}, then the
     * sweep angle will be clamped to that value.
     */
    public void setLineSweepAngleDegrees(float lineLengthDegrees) {
        this.mLineSweepAngleDegrees = lineLengthDegrees;

        updateArcDrawable();
        requestLayout();
        postInvalidate();
    }

    /** Returns the color of this arc, in ARGB format. */
    @ColorInt
    public int getColor() {
        return mColor;
    }

    /** Sets the color of this arc, in ARGB format. */
    public void setColor(@ColorInt int color) {
        this.mColor = color;
        updateArcDrawable();
        invalidate();
    }

    /** Returns the stockCap of this arc. */
    @NonNull
    public Cap getStrokeCap() {
        return mCap;
    }

    /** Sets the strokeCap of this arc. */
    public void setStrokeCap(@NonNull Cap cap) {
        mCap = cap;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (mArcDrawable == null) {
            return;
        }
        mArcDrawable.onDraw(canvas);
    }

    @Override
    public void checkInvalidAttributeAsChild() {
        // Nothing required...
    }

    @Override
    public boolean isPointInsideClickArea(float x, float y) {
        // Stolen from WearCurvedTextView...
        float radius2 = min(getWidth(), getHeight()) / 2f - getPaddingTop();
        float radius1 = radius2 - mThicknessPx;

        float dx = x - getWidth() / 2f;
        float dy = y - getHeight() / 2f;

        float r2 = dx * dx + dy * dy;
        if (r2 < radius1 * radius1 || r2 > radius2 * radius2) {
            return false;
        }

        // Since we are symmetrical on the Y-axis, we can constrain the angle to the x>=0 quadrants.
        float angle = (float) Math.toDegrees(Math.atan2(Math.abs(dx), -dy));
        return angle < resolveSweepAngleDegrees() / 2;
    }

    /** Legacy LinePath, which supports drawing the line as a single Path. */
    private static class ArcDrawableLegacy implements ArcDrawable {

        @NonNull private final Paint mPaint;
        @NonNull private final Path mPath = new Path();

        ArcDrawableLegacy(@NonNull RectF bounds, float clampedLineLength, @NonNull Paint paint) {
            this.mPaint = paint;

            if (clampedLineLength >= 360f) {
                // Android internally will take the modulus of the angle with 360, so drawing a full
                // ring can't be done using path.arcTo. In that case, just draw a circle.
                mPath.addOval(bounds, Direction.CW);
            } else if (clampedLineLength != 0) {
                mPath.moveTo(0, 0); // Work-around for b/177676885
                mPath.arcTo(
                        bounds,
                        BASE_DRAW_ANGLE_SHIFT - (clampedLineLength / 2f),
                        clampedLineLength,
                        /* forceMoveTo= */ true);
            }
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            canvas.drawPath(mPath, mPaint);
        }
    }

    /** Definition of an arc that can be drawn on the canvas. */
    private interface ArcDrawable {
        /** Called when the arc should be drawn on the canvas. */
        void onDraw(@NonNull Canvas canvas);
    }
}
