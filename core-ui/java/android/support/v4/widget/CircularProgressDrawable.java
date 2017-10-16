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

package android.support.v4.widget;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v4.util.Preconditions;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.DisplayMetrics;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Drawable that renders the animated indeterminate progress indicator in the Material design style
 * without depending on API level 11.
 *
 * <p>While this may be used to draw an indeterminate spinner using {@link #start()} and {@link
 * #stop()} methods, this may also be used to draw a progress arc using {@link
 * #setStartEndTrim(float, float)} method. CircularProgressDrawable also supports adding an arrow
 * at the end of the arc by {@link #setArrowEnabled(boolean)} and {@link #setArrowDimensions(float,
 * float)} methods.
 *
 * <p>To use one of the pre-defined sizes instead of using your own, {@link #setStyle(int)} should
 * be called with one of the {@link #DEFAULT} or {@link #LARGE} styles as its parameter. Doing it
 * so will update the arrow dimensions, ring size and stroke width to fit the one specified.
 *
 * <p>If no center radius is set via {@link #setCenterRadius(float)} or {@link #setStyle(int)}
 * methods, CircularProgressDrawable will fill the bounds set via {@link #setBounds(Rect)}.
 */
public class CircularProgressDrawable extends Drawable implements Animatable {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator MATERIAL_INTERPOLATOR = new FastOutSlowInInterpolator();

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LARGE, DEFAULT})
    public @interface ProgressDrawableSize {
    }

    /** Maps to ProgressBar.Large style. */
    public static final int LARGE = 0;

    private static final float CENTER_RADIUS_LARGE = 11f;
    private static final float STROKE_WIDTH_LARGE = 3f;
    private static final int ARROW_WIDTH_LARGE = 12;
    private static final int ARROW_HEIGHT_LARGE = 6;

    /** Maps to ProgressBar default style. */
    public static final int DEFAULT = 1;

    private static final float CENTER_RADIUS = 7.5f;
    private static final float STROKE_WIDTH = 2.5f;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 5;

    /**
     * This is the default set of colors that's used in spinner. {@link
     * #setColorSchemeColors(int...)} allows modifying colors.
     */
    private static final int[] COLORS = new int[]{
            Color.BLACK
    };

    /**
     * The value in the linear interpolator for animating the drawable at which
     * the color transition should start
     */
    private static final float COLOR_CHANGE_OFFSET = 0.75f;
    private static final float SHRINK_OFFSET = 0.5f;

    /** The duration of a single progress spin in milliseconds. */
    private static final int ANIMATION_DURATION = 1332;

    /** Full rotation that's done for the animation duration in degrees. */
    private static final float GROUP_FULL_ROTATION = 1080f / 5f;

    /** The indicator ring, used to manage animation state. */
    private final Ring mRing;

    /** Canvas rotation in degrees. */
    private float mRotation;

    /** Maximum length of the progress arc during the animation. */
    private static final float MAX_PROGRESS_ARC = .8f;
    /** Minimum length of the progress arc during the animation. */
    private static final float MIN_PROGRESS_ARC = .01f;

    /** Rotation applied to ring during the animation, to complete it to a full circle. */
    private static final float RING_ROTATION = 1f - (MAX_PROGRESS_ARC - MIN_PROGRESS_ARC);

    private Resources mResources;
    private Animator mAnimator;
    private float mRotationCount;
    private boolean mFinishing;

    /**
     * @param context application context
     */
    public CircularProgressDrawable(Context context) {
        mResources = Preconditions.checkNotNull(context).getResources();

        mRing = new Ring();
        mRing.setColors(COLORS);

        setStrokeWidth(STROKE_WIDTH);
        setupAnimators();
    }

    /** Sets all parameters at once in dp. */
    private void setSizeParameters(float centerRadius, float strokeWidth, float arrowWidth,
            float arrowHeight) {
        final Ring ring = mRing;
        final DisplayMetrics metrics = mResources.getDisplayMetrics();
        final float screenDensity = metrics.density;

        ring.setStrokeWidth(strokeWidth * screenDensity);
        ring.setCenterRadius(centerRadius * screenDensity);
        ring.setColorIndex(0);
        ring.setArrowDimensions(arrowWidth * screenDensity, arrowHeight * screenDensity);
    }

    /**
     * Sets the overall size for the progress spinner. This updates the radius
     * and stroke width of the ring, and arrow dimensions.
     *
     * @param size one of {@link #LARGE} or {@link #DEFAULT}
     */
    public void setStyle(@ProgressDrawableSize int size) {
        if (size == LARGE) {
            setSizeParameters(CENTER_RADIUS_LARGE, STROKE_WIDTH_LARGE, ARROW_WIDTH_LARGE,
                    ARROW_HEIGHT_LARGE);
        } else {
            setSizeParameters(CENTER_RADIUS, STROKE_WIDTH, ARROW_WIDTH, ARROW_HEIGHT);
        }
        invalidateSelf();
    }

    /**
     * Returns the stroke width for the progress spinner in pixels.
     *
     * @return stroke width in pixels
     */
    public float getStrokeWidth() {
        return mRing.getStrokeWidth();
    }

    /**
     * Sets the stroke width for the progress spinner in pixels.
     *
     * @param strokeWidth stroke width in pixels
     */
    public void setStrokeWidth(float strokeWidth) {
        mRing.setStrokeWidth(strokeWidth);
        invalidateSelf();
    }

    /**
     * Returns the center radius for the progress spinner in pixels.
     *
     * @return center radius in pixels
     */
    public float getCenterRadius() {
        return mRing.getCenterRadius();
    }

    /**
     * Sets the center radius for the progress spinner in pixels. If set to 0, this drawable will
     * fill the bounds when drawn.
     *
     * @param centerRadius center radius in pixels
     */
    public void setCenterRadius(float centerRadius) {
        mRing.setCenterRadius(centerRadius);
        invalidateSelf();
    }

    /**
     * Sets the stroke cap of the progress spinner. Default stroke cap is {@link Paint.Cap#SQUARE}.
     *
     * @param strokeCap stroke cap
     */
    public void setStrokeCap(Paint.Cap strokeCap) {
        mRing.setStrokeCap(strokeCap);
        invalidateSelf();
    }

    /**
     * Returns the stroke cap of the progress spinner.
     *
     * @return stroke cap
     */
    public Paint.Cap getStrokeCap() {
        return mRing.getStrokeCap();
    }

    /**
     * Returns the arrow width in pixels.
     *
     * @return arrow width in pixels
     */
    public float getArrowWidth() {
        return mRing.getArrowWidth();
    }

    /**
     * Returns the arrow height in pixels.
     *
     * @return arrow height in pixels
     */
    public float getArrowHeight() {
        return mRing.getArrowHeight();
    }

    /**
     * Sets the dimensions of the arrow at the end of the spinner in pixels.
     *
     * @param width width of the baseline of the arrow in pixels
     * @param height distance from tip of the arrow to its baseline in pixels
     */
    public void setArrowDimensions(float width, float height) {
        mRing.setArrowDimensions(width, height);
        invalidateSelf();
    }

    /**
     * Returns {@code true} if the arrow at the end of the spinner is shown.
     *
     * @return {@code true} if the arrow is shown, {@code false} otherwise.
     */
    public boolean getArrowEnabled() {
        return mRing.getShowArrow();
    }

    /**
     * Sets if the arrow at the end of the spinner should be shown.
     *
     * @param show {@code true} if the arrow should be drawn, {@code false} otherwise
     */
    public void setArrowEnabled(boolean show) {
        mRing.setShowArrow(show);
        invalidateSelf();
    }

    /**
     * Returns the scale of the arrow at the end of the spinner.
     *
     * @return scale of the arrow
     */
    public float getArrowScale() {
        return mRing.getArrowScale();
    }

    /**
     * Sets the scale of the arrow at the end of the spinner.
     *
     * @param scale scaling that will be applied to the arrow's both width and height when drawing.
     */
    public void setArrowScale(float scale) {
        mRing.setArrowScale(scale);
        invalidateSelf();
    }

    /**
     * Returns the start trim for the progress spinner arc
     *
     * @return start trim from [0..1]
     */
    public float getStartTrim() {
        return mRing.getStartTrim();
    }

    /**
     * Returns the end trim for the progress spinner arc
     *
     * @return end trim from [0..1]
     */
    public float getEndTrim() {
        return mRing.getEndTrim();
    }

    /**
     * Sets the start and end trim for the progress spinner arc. 0 corresponds to the geometric
     * angle of 0 degrees (3 o'clock on a watch) and it increases clockwise, coming to a full circle
     * at 1.
     *
     * @param start starting position of the arc from [0..1]
     * @param end ending position of the arc from [0..1]
     */
    public void setStartEndTrim(float start, float end) {
        mRing.setStartTrim(start);
        mRing.setEndTrim(end);
        invalidateSelf();
    }

    /**
     * Returns the amount of rotation applied to the progress spinner.
     *
     * @return amount of rotation from [0..1]
     */
    public float getProgressRotation() {
        return mRing.getRotation();
    }

    /**
     * Sets the amount of rotation to apply to the progress spinner.
     *
     * @param rotation rotation from [0..1]
     */
    public void setProgressRotation(float rotation) {
        mRing.setRotation(rotation);
        invalidateSelf();
    }

    /**
     * Returns the background color of the circle drawn inside the drawable.
     *
     * @return an ARGB color
     */
    public int getBackgroundColor() {
        return mRing.getBackgroundColor();
    }

    /**
     * Sets the background color of the circle inside the drawable. Calling {@link
     * #setAlpha(int)} does not affect the visibility background color, so it should be set
     * separately if it needs to be hidden or visible.
     *
     * @param color an ARGB color
     */
    public void setBackgroundColor(int color) {
        mRing.setBackgroundColor(color);
        invalidateSelf();
    }

    /**
     * Returns the colors used in the progress animation
     *
     * @return list of ARGB colors
     */
    public int[] getColorSchemeColors() {
        return mRing.getColors();
    }

    /**
     * Sets the colors used in the progress animation from a color list. The first color will also
     * be the color to be used if animation is not started yet.
     *
     * @param colors list of ARGB colors to be used in the spinner
     */
    public void setColorSchemeColors(int... colors) {
        mRing.setColors(colors);
        mRing.setColorIndex(0);
        invalidateSelf();
    }

    @Override
    public void draw(Canvas canvas) {
        final Rect bounds = getBounds();
        canvas.save();
        canvas.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY());
        mRing.draw(canvas, bounds);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        mRing.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        return mRing.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mRing.setColorFilter(colorFilter);
        invalidateSelf();
    }

    private void setRotation(float rotation) {
        mRotation = rotation;
    }

    private float getRotation() {
        return mRotation;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        return mAnimator.isRunning();
    }

    /**
     * Starts the animation for the spinner.
     */
    @Override
    public void start() {
        mAnimator.cancel();
        mRing.storeOriginals();
        // Already showing some part of the ring
        if (mRing.getEndTrim() != mRing.getStartTrim()) {
            mFinishing = true;
            mAnimator.setDuration(ANIMATION_DURATION / 2);
            mAnimator.start();
        } else {
            mRing.setColorIndex(0);
            mRing.resetOriginals();
            mAnimator.setDuration(ANIMATION_DURATION);
            mAnimator.start();
        }
    }

    /**
     * Stops the animation for the spinner.
     */
    @Override
    public void stop() {
        mAnimator.cancel();
        setRotation(0);
        mRing.setShowArrow(false);
        mRing.setColorIndex(0);
        mRing.resetOriginals();
        invalidateSelf();
    }

    // Adapted from ArgbEvaluator.java
    private int evaluateColorChange(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 0xff;
        int startR = (startValue >> 16) & 0xff;
        int startG = (startValue >> 8) & 0xff;
        int startB = startValue & 0xff;

        int endA = (endValue >> 24) & 0xff;
        int endR = (endValue >> 16) & 0xff;
        int endG = (endValue >> 8) & 0xff;
        int endB = endValue & 0xff;

        return (startA + (int) (fraction * (endA - startA))) << 24
                | (startR + (int) (fraction * (endR - startR))) << 16
                | (startG + (int) (fraction * (endG - startG))) << 8
                | (startB + (int) (fraction * (endB - startB)));
    }

    /**
     * Update the ring color if this is within the last 25% of the animation.
     * The new ring color will be a translation from the starting ring color to
     * the next color.
     */
    private void updateRingColor(float interpolatedTime, Ring ring) {
        if (interpolatedTime > COLOR_CHANGE_OFFSET) {
            ring.setColor(evaluateColorChange((interpolatedTime - COLOR_CHANGE_OFFSET)
                            / (1f - COLOR_CHANGE_OFFSET), ring.getStartingColor(),
                    ring.getNextColor()));
        } else {
            ring.setColor(ring.getStartingColor());
        }
    }

    /**
     * Update the ring start and end trim if the animation is finishing (i.e. it started with
     * already visible progress, so needs to shrink back down before starting the spinner).
     */
    private void applyFinishTranslation(float interpolatedTime, Ring ring) {
        // shrink back down and complete a full rotation before
        // starting other circles
        // Rotation goes between [0..1].
        updateRingColor(interpolatedTime, ring);
        float targetRotation = (float) (Math.floor(ring.getStartingRotation() / MAX_PROGRESS_ARC)
                + 1f);
        final float startTrim = ring.getStartingStartTrim()
                + (ring.getStartingEndTrim() - MIN_PROGRESS_ARC - ring.getStartingStartTrim())
                * interpolatedTime;
        ring.setStartTrim(startTrim);
        ring.setEndTrim(ring.getStartingEndTrim());
        final float rotation = ring.getStartingRotation()
                + ((targetRotation - ring.getStartingRotation()) * interpolatedTime);
        ring.setRotation(rotation);
    }

    /**
     * Update the ring start and end trim according to current time of the animation.
     */
    private void applyTransformation(float interpolatedTime, Ring ring, boolean lastFrame) {
        if (mFinishing) {
            applyFinishTranslation(interpolatedTime, ring);
            // Below condition is to work around a ValueAnimator issue where onAnimationRepeat is
            // called before last frame (1f).
        } else if (interpolatedTime != 1f || lastFrame) {
            final float startingRotation = ring.getStartingRotation();
            float startTrim, endTrim;

            if (interpolatedTime < SHRINK_OFFSET) { // Expansion occurs on first half of animation
                final float scaledTime = interpolatedTime / SHRINK_OFFSET;
                startTrim = ring.getStartingStartTrim();
                endTrim = startTrim + ((MAX_PROGRESS_ARC - MIN_PROGRESS_ARC)
                        * MATERIAL_INTERPOLATOR.getInterpolation(scaledTime) + MIN_PROGRESS_ARC);
            } else { // Shrinking occurs on second half of animation
                float scaledTime = (interpolatedTime - SHRINK_OFFSET) / (1f - SHRINK_OFFSET);
                endTrim = ring.getStartingStartTrim() + (MAX_PROGRESS_ARC - MIN_PROGRESS_ARC);
                startTrim = endTrim - ((MAX_PROGRESS_ARC - MIN_PROGRESS_ARC)
                        * (1f - MATERIAL_INTERPOLATOR.getInterpolation(scaledTime))
                        + MIN_PROGRESS_ARC);
            }

            final float rotation = startingRotation + (RING_ROTATION * interpolatedTime);
            float groupRotation = GROUP_FULL_ROTATION * (interpolatedTime + mRotationCount);

            ring.setStartTrim(startTrim);
            ring.setEndTrim(endTrim);
            ring.setRotation(rotation);
            setRotation(groupRotation);
        }
    }

    private void setupAnimators() {
        final Ring ring = mRing;
        final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float interpolatedTime = (float) animation.getAnimatedValue();
                updateRingColor(interpolatedTime, ring);
                applyTransformation(interpolatedTime, ring, false);
                invalidateSelf();
            }
        });
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(LINEAR_INTERPOLATOR);
        animator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {
                mRotationCount = 0;
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                // do nothing
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // do nothing
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
                applyTransformation(1f, ring, true);
                ring.storeOriginals();
                ring.goToNextColor();
                if (mFinishing) {
                    // finished closing the last ring from the swipe gesture; go
                    // into progress mode
                    mFinishing = false;
                    animator.cancel();
                    animator.setDuration(ANIMATION_DURATION);
                    animator.start();
                    ring.setShowArrow(false);
                } else {
                    mRotationCount = mRotationCount + 1;
                }
            }
        });
        mAnimator = animator;
    }

    /**
     * A private class to do all the drawing of CircularProgressDrawable, which includes background,
     * progress spinner and the arrow. This class is to separate drawing from animation.
     */
    private static class Ring {
        final RectF mTempBounds = new RectF();
        final Paint mPaint = new Paint();
        final Paint mArrowPaint = new Paint();
        final Paint mCirclePaint = new Paint();

        float mStartTrim = 0f;
        float mEndTrim = 0f;
        float mRotation = 0f;
        float mStrokeWidth = 5f;

        int[] mColors;
        // mColorIndex represents the offset into the available mColors that the
        // progress circle should currently display. As the progress circle is
        // animating, the mColorIndex moves by one to the next available color.
        int mColorIndex;
        float mStartingStartTrim;
        float mStartingEndTrim;
        float mStartingRotation;
        boolean mShowArrow;
        Path mArrow;
        float mArrowScale = 1;
        float mRingCenterRadius;
        int mArrowWidth;
        int mArrowHeight;
        int mAlpha = 255;
        int mCurrentColor;

        Ring() {
            mPaint.setStrokeCap(Paint.Cap.SQUARE);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Style.STROKE);

            mArrowPaint.setStyle(Paint.Style.FILL);
            mArrowPaint.setAntiAlias(true);

            mCirclePaint.setColor(Color.TRANSPARENT);
        }

        /**
         * Sets the dimensions of the arrowhead.
         *
         * @param width width of the hypotenuse of the arrow head
         * @param height height of the arrow point
         */
        void setArrowDimensions(float width, float height) {
            mArrowWidth = (int) width;
            mArrowHeight = (int) height;
        }

        void setStrokeCap(Paint.Cap strokeCap) {
            mPaint.setStrokeCap(strokeCap);
        }

        Paint.Cap getStrokeCap() {
            return mPaint.getStrokeCap();
        }

        float getArrowWidth() {
            return mArrowWidth;
        }

        float getArrowHeight() {
            return mArrowHeight;
        }

        /**
         * Draw the progress spinner
         */
        void draw(Canvas c, Rect bounds) {
            final RectF arcBounds = mTempBounds;
            float arcRadius = mRingCenterRadius + mStrokeWidth / 2f;
            if (mRingCenterRadius <= 0) {
                // If center radius is not set, fill the bounds
                arcRadius = Math.min(bounds.width(), bounds.height()) / 2f - Math.max(
                        (mArrowWidth * mArrowScale) / 2f, mStrokeWidth / 2f);
            }
            arcBounds.set(bounds.centerX() - arcRadius,
                    bounds.centerY() - arcRadius,
                    bounds.centerX() + arcRadius,
                    bounds.centerY() + arcRadius);

            final float startAngle = (mStartTrim + mRotation) * 360;
            final float endAngle = (mEndTrim + mRotation) * 360;
            float sweepAngle = endAngle - startAngle;

            mPaint.setColor(mCurrentColor);
            mPaint.setAlpha(mAlpha);

            // Draw the background first
            float inset = mStrokeWidth / 2f; // Calculate inset to draw inside the arc
            arcBounds.inset(inset, inset); // Apply inset
            c.drawCircle(arcBounds.centerX(), arcBounds.centerY(), arcBounds.width() / 2f,
                    mCirclePaint);
            arcBounds.inset(-inset, -inset); // Revert the inset

            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint);

            drawTriangle(c, startAngle, sweepAngle, arcBounds);
        }

        void drawTriangle(Canvas c, float startAngle, float sweepAngle, RectF bounds) {
            if (mShowArrow) {
                if (mArrow == null) {
                    mArrow = new android.graphics.Path();
                    mArrow.setFillType(android.graphics.Path.FillType.EVEN_ODD);
                } else {
                    mArrow.reset();
                }
                float centerRadius = Math.min(bounds.width(), bounds.height()) / 2f;
                float inset = mArrowWidth * mArrowScale / 2f;
                // Update the path each time. This works around an issue in SKIA
                // where concatenating a rotation matrix to a scale matrix
                // ignored a starting negative rotation. This appears to have
                // been fixed as of API 21.
                mArrow.moveTo(0, 0);
                mArrow.lineTo(mArrowWidth * mArrowScale, 0);
                mArrow.lineTo((mArrowWidth * mArrowScale / 2), (mArrowHeight
                        * mArrowScale));
                mArrow.offset(centerRadius + bounds.centerX() - inset,
                        bounds.centerY() + mStrokeWidth / 2f);
                mArrow.close();
                // draw a triangle
                mArrowPaint.setColor(mCurrentColor);
                mArrowPaint.setAlpha(mAlpha);
                c.save();
                c.rotate(startAngle + sweepAngle, bounds.centerX(),
                        bounds.centerY());
                c.drawPath(mArrow, mArrowPaint);
                c.restore();
            }
        }

        /**
         * Sets the colors the progress spinner alternates between.
         *
         * @param colors array of ARGB colors. Must be non-{@code null}.
         */
        void setColors(@NonNull int[] colors) {
            mColors = colors;
            // if colors are reset, make sure to reset the color index as well
            setColorIndex(0);
        }

        int[] getColors() {
            return mColors;
        }

        /**
         * Sets the absolute color of the progress spinner. This is should only
         * be used when animating between current and next color when the
         * spinner is rotating.
         *
         * @param color an ARGB color
         */
        void setColor(int color) {
            mCurrentColor = color;
        }

        /**
         * Sets the background color of the circle inside the spinner.
         */
        void setBackgroundColor(int color) {
            mCirclePaint.setColor(color);
        }

        int getBackgroundColor() {
            return mCirclePaint.getColor();
        }

        /**
         * @param index index into the color array of the color to display in
         *              the progress spinner.
         */
        void setColorIndex(int index) {
            mColorIndex = index;
            mCurrentColor = mColors[mColorIndex];
        }

        /**
         * @return int describing the next color the progress spinner should use when drawing.
         */
        int getNextColor() {
            return mColors[getNextColorIndex()];
        }

        int getNextColorIndex() {
            return (mColorIndex + 1) % (mColors.length);
        }

        /**
         * Proceed to the next available ring color. This will automatically
         * wrap back to the beginning of colors.
         */
        void goToNextColor() {
            setColorIndex(getNextColorIndex());
        }

        void setColorFilter(ColorFilter filter) {
            mPaint.setColorFilter(filter);
        }

        /**
         * @param alpha alpha of the progress spinner and associated arrowhead.
         */
        void setAlpha(int alpha) {
            mAlpha = alpha;
        }

        /**
         * @return current alpha of the progress spinner and arrowhead
         */
        int getAlpha() {
            return mAlpha;
        }

        /**
         * @param strokeWidth set the stroke width of the progress spinner in pixels.
         */
        void setStrokeWidth(float strokeWidth) {
            mStrokeWidth = strokeWidth;
            mPaint.setStrokeWidth(strokeWidth);
        }

        float getStrokeWidth() {
            return mStrokeWidth;
        }

        void setStartTrim(float startTrim) {
            mStartTrim = startTrim;
        }

        float getStartTrim() {
            return mStartTrim;
        }

        float getStartingStartTrim() {
            return mStartingStartTrim;
        }

        float getStartingEndTrim() {
            return mStartingEndTrim;
        }

        int getStartingColor() {
            return mColors[mColorIndex];
        }

        void setEndTrim(float endTrim) {
            mEndTrim = endTrim;
        }

        float getEndTrim() {
            return mEndTrim;
        }

        void setRotation(float rotation) {
            mRotation = rotation;
        }

        float getRotation() {
            return mRotation;
        }

        /**
         * @param centerRadius inner radius in px of the circle the progress spinner arc traces
         */
        void setCenterRadius(float centerRadius) {
            mRingCenterRadius = centerRadius;
        }

        float getCenterRadius() {
            return mRingCenterRadius;
        }

        /**
         * @param show {@code true} if should show the arrow head on the progress spinner
         */
        void setShowArrow(boolean show) {
            if (mShowArrow != show) {
                mShowArrow = show;
            }
        }

        boolean getShowArrow() {
            return mShowArrow;
        }

        /**
         * @param scale scale of the arrowhead for the spinner
         */
        void setArrowScale(float scale) {
            if (scale != mArrowScale) {
                mArrowScale = scale;
            }
        }

        float getArrowScale() {
            return mArrowScale;
        }

        /**
         * @return The amount the progress spinner is currently rotated, between [0..1].
         */
        float getStartingRotation() {
            return mStartingRotation;
        }

        /**
         * If the start / end trim are offset to begin with, store them so that animation starts
         * from that offset.
         */
        void storeOriginals() {
            mStartingStartTrim = mStartTrim;
            mStartingEndTrim = mEndTrim;
            mStartingRotation = mRotation;
        }

        /**
         * Reset the progress spinner to default rotation, start and end angles.
         */
        void resetOriginals() {
            mStartingStartTrim = 0;
            mStartingEndTrim = 0;
            mStartingRotation = 0;
            setStartTrim(0);
            setEndTrim(0);
            setRotation(0);
        }
    }
}
