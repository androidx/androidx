/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v7.graphics.drawable;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.FloatRange;
import android.support.annotation.IntDef;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A drawable that can draw a "Drawer hamburger" menu or an arrow and animate between them.
 * <p>
 * The progress between the two states is controlled via {@link #setProgress(float)}.
 * </p>
 */
public class DrawerArrowDrawable extends Drawable {

    /**
     * Direction to make the arrow point towards the left.
     *
     * @see #setDirection(int)
     * @see #getDirection()
     */
    public static final int ARROW_DIRECTION_LEFT = 0;

    /**
     * Direction to make the arrow point towards the right.
     *
     * @see #setDirection(int)
     * @see #getDirection()
     */
    public static final int ARROW_DIRECTION_RIGHT = 1;

    /**
     * Direction to make the arrow point towards the start.
     *
     * <p>When used in a view with a {@link ViewCompat#LAYOUT_DIRECTION_RTL RTL} layout direction,
     * this is the same as {@link #ARROW_DIRECTION_RIGHT}, otherwise it is the same as
     * {@link #ARROW_DIRECTION_LEFT}.</p>
     *
     * @see #setDirection(int)
     * @see #getDirection()
     */
    public static final int ARROW_DIRECTION_START = 2;

    /**
     * Direction to make the arrow point to the end.
     *
     * <p>When used in a view with a {@link ViewCompat#LAYOUT_DIRECTION_RTL RTL} layout direction,
     * this is the same as {@link #ARROW_DIRECTION_LEFT}, otherwise it is the same as
     * {@link #ARROW_DIRECTION_RIGHT}.</p>
     *
     * @see #setDirection(int)
     * @see #getDirection()
     */
    public static final int ARROW_DIRECTION_END = 3;

    /** @hide */
    @IntDef({ARROW_DIRECTION_LEFT, ARROW_DIRECTION_RIGHT,
            ARROW_DIRECTION_START, ARROW_DIRECTION_END})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ArrowDirection {}

    private final Paint mPaint = new Paint();

    // The angle in degress that the arrow head is inclined at.
    private static final float ARROW_HEAD_ANGLE = (float) Math.toRadians(45);
    private final float mBarThickness;
    // The length of top and bottom bars when they merge into an arrow
    private final float mTopBottomArrowSize;
    // The length of middle bar
    private final float mBarSize;
    // The length of the middle bar when arrow is shaped
    private final float mMiddleArrowSize;
    // The space between bars when they are parallel
    private final float mBarGap;
    // Whether bars should spin or not during progress
    private final boolean mSpin;
    // Use Path instead of canvas operations so that if color has transparency, overlapping sections
    // wont look different
    private final Path mPath = new Path();
    // The reported intrinsic size of the drawable.
    private final int mSize;
    // Whether we should mirror animation when animation is reversed.
    private boolean mVerticalMirror = false;
    // The interpolated version of the original progress
    private float mProgress;
    // the amount that overlaps w/ bar size when rotation is max
    private float mMaxCutForBarSize;
    // The distance of arrow's center from top when horizontal
    private float mCenterOffset;
    // The arrow direction
    private int mDirection = ARROW_DIRECTION_START;

    /**
     * @param context used to get the configuration for the drawable from
     */
    public DrawerArrowDrawable(Context context) {
        final TypedArray a = context.getTheme()
                .obtainStyledAttributes(null, R.styleable.DrawerArrowToggle,
                        R.attr.drawerArrowStyle,
                        R.style.Base_Widget_AppCompat_DrawerArrowToggle);
        mPaint.setAntiAlias(true);
        mPaint.setColor(a.getColor(R.styleable.DrawerArrowToggle_color, 0));
        mSize = a.getDimensionPixelSize(R.styleable.DrawerArrowToggle_drawableSize, 0);
        // round this because having this floating may cause bad measurements
        mBarSize = Math.round(a.getDimension(R.styleable.DrawerArrowToggle_barSize, 0));
        // round this because having this floating may cause bad measurements
        mTopBottomArrowSize = Math.round(a.getDimension(
                R.styleable.DrawerArrowToggle_topBottomBarArrowSize, 0));
        mBarThickness = a.getDimension(R.styleable.DrawerArrowToggle_thickness, 0);
        // round this because having this floating may cause bad measurements
        mBarGap = Math.round(a.getDimension(R.styleable.DrawerArrowToggle_gapBetweenBars, 0));
        mSpin = a.getBoolean(R.styleable.DrawerArrowToggle_spinBars, true);
        mMiddleArrowSize = a.getDimension(R.styleable.DrawerArrowToggle_middleBarArrowSize, 0);
        final int remainingSpace = (int) (mSize - mBarThickness * 3 - mBarGap * 2);
        mCenterOffset = (remainingSpace / 4) * 2; //making sure it is a multiple of 2.
        mCenterOffset += mBarThickness * 1.5 + mBarGap;
        a.recycle();

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.MITER);
        mPaint.setStrokeCap(Paint.Cap.BUTT);
        mPaint.setStrokeWidth(mBarThickness);

        mMaxCutForBarSize = (float) (mBarThickness / 2 * Math.cos(ARROW_HEAD_ANGLE));
    }

    /**
     * Set the arrow direction.
     */
    public void setDirection(@ArrowDirection int direction) {
        if (direction != mDirection) {
            mDirection = direction;
            invalidateSelf();
        }
    }

    /**
     * Returns the arrow direction.
     */
    @ArrowDirection
    public int getDirection() {
        return mDirection;
    }

    /**
     * If set, canvas is flipped when progress reached to end and going back to start.
     */
    public void setVerticalMirror(boolean verticalMirror) {
        if (mVerticalMirror != verticalMirror) {
            mVerticalMirror = verticalMirror;
            invalidateSelf();
        }
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();

        final boolean flipToPointRight;
        switch (mDirection) {
            case ARROW_DIRECTION_LEFT:
                flipToPointRight = false;
                break;
            case ARROW_DIRECTION_RIGHT:
                flipToPointRight = true;
                break;
            case ARROW_DIRECTION_END:
                flipToPointRight = DrawableCompat.getLayoutDirection(this)
                        == ViewCompat.LAYOUT_DIRECTION_LTR;
                break;
            case ARROW_DIRECTION_START:
            default:
                flipToPointRight = DrawableCompat.getLayoutDirection(this)
                        == ViewCompat.LAYOUT_DIRECTION_RTL;
                break;
        }

        // Interpolated widths of arrow bars
        final float arrowSize = lerp(mBarSize, mTopBottomArrowSize, mProgress);
        final float middleBarSize = lerp(mBarSize, mMiddleArrowSize, mProgress);
        // Interpolated size of middle bar
        final float middleBarCut = Math.round(lerp(0, mMaxCutForBarSize, mProgress));
        // The rotation of the top and bottom bars (that make the arrow head)
        final float rotation = lerp(0, ARROW_HEAD_ANGLE, mProgress);

        // The whole canvas rotates as the transition happens
        final float canvasRotate = lerp(flipToPointRight ? 0 : -180,
                flipToPointRight ? 180 : 0, mProgress);

        final float arrowWidth = Math.round(arrowSize * Math.cos(rotation));
        final float arrowHeight = Math.round(arrowSize * Math.sin(rotation));

        mPath.rewind();
        final float topBottomBarOffset = lerp(mBarGap + mBarThickness, -mMaxCutForBarSize,
                mProgress);

        final float arrowEdge = -middleBarSize / 2;
        // draw middle bar
        mPath.moveTo(arrowEdge + middleBarCut, 0);
        mPath.rLineTo(middleBarSize - middleBarCut * 2, 0);

        // bottom bar
        mPath.moveTo(arrowEdge, topBottomBarOffset);
        mPath.rLineTo(arrowWidth, arrowHeight);

        // top bar
        mPath.moveTo(arrowEdge, -topBottomBarOffset);
        mPath.rLineTo(arrowWidth, -arrowHeight);

        mPath.close();

        canvas.save();
        // Rotate the whole canvas if spinning, if not, rotate it 180 to get
        // the arrow pointing the other way for RTL.
        canvas.translate(bounds.centerX(), mCenterOffset);
        if (mSpin) {
            canvas.rotate(canvasRotate * ((mVerticalMirror ^ flipToPointRight) ? -1 : 1));
        } else if (flipToPointRight) {
            canvas.rotate(180);
        }
        canvas.drawPath(mPath, mPaint);

        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        if (alpha != mPaint.getAlpha()) {
            mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    /**
     * Returns the current progress of the arrow.
     */
    @FloatRange(from = 0.0, to = 1.0)
    public float getProgress() {
        return mProgress;
    }

    /**
     * Set the progress of the arrow.
     *
     * <p>A value of {@code 0.0} indicates that the arrow should be drawn in it's starting
     * position. A value of {@code 1.0} indicates that the arrow should be drawn in it's ending
     * position.</p>
     */
    public void setProgress(@FloatRange(from = 0.0, to = 1.0) float progress) {
        if (mProgress != progress) {
            mProgress = progress;
            invalidateSelf();
        }
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}