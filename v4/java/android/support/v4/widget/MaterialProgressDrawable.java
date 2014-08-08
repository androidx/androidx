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

import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Animatable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Fancy progress indicator for Material theme.
 */
class MaterialProgressDrawable extends Drawable implements Animatable {
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator END_CURVE_INTERPOLATOR = new EndCurveInterpolator();
    private static final Interpolator START_CURVE_INTERPOLATOR = new StartCurveInterpolator();

    @Retention(RetentionPolicy.CLASS)
    @IntDef({LARGE, DEFAULT, SMALL })
    public @interface ProgressDrawableSize {}
    // Maps to ProgressBar.Large style
    static final int LARGE = 0;
    // Maps to ProgressBar default style
    static final int DEFAULT = 1;
    // Maps to ProgressBar.Small style
    static final int SMALL = 2;

    // Maps to ProgressBar default style
    private static final int CIRCLE_DIAMETER = 40;
    private static final int INNER_RADIUS = 10;
    private static final int STROKE_WIDTH = 4;

    // Maps to ProgressBar.Large style
    private static final int CIRCLE_DIAMETER_LARGE = 76;
    private static final float INNER_RADIUS_LARGE = 15.5f;
    private static final float STROKE_WIDTH_LARGE = 6.3f;

    // Maps to ProgressBar.Small style
    private static final int CIRCLE_DIAMETER_SMALL = 16;
    private static final float INNER_RADIUS_SMALL = 3.15f;
    private static final float STROKE_WIDTH_SMALL = 1.3f;

    private final int[] COLORS = new int[] {
        Color.BLACK
    };

    /** The duration of a single progress spin in milliseconds. */
    private static final int ANIMATION_DURATION = 1000 * 80 / 60;

    /** The number of points in the progress "star". */
    private static final float NUM_POINTS = 5f;
    /** The list of animators operating on this drawable. */
    private final ArrayList<Animation> mAnimators = new ArrayList<Animation>();

    /** The indicator ring, used to manage animation state. */
    private final Ring mRing;

    /** Canvas rotation in degrees. */
    private float mRotation;

    /** Layout info for the arrowhead in dp */
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_HEIGHT = 5;
    private static final float ARROW_OFFSET_ANGLE = 5;

    private Resources mResources;
    private int mColorIndex;
    private View mParent;
    private Animation mAnimation;
    private float mRotationCount;
    private int[] mColors;
    private double mWidth;
    private double mHeight;
    private double mInnerRadius;
    private double mStrokeWidth;
    private Animation mFinishAnimation;

    public MaterialProgressDrawable(Context context, View parent) {
        mParent = parent;
        mResources = context.getResources();

        mRing = new Ring(mCallback);
        mColors = COLORS;
        mRing.setColors(mColors);

        initialize(CIRCLE_DIAMETER, CIRCLE_DIAMETER, INNER_RADIUS, STROKE_WIDTH);
        setupAnimators();
    }

    private void initialize(double progressCircleWidth, double progressCircleHeight,
            double innerRadius, double strokeWidth) {
        final Ring ring = mRing;
        final DisplayMetrics metrics = mResources.getDisplayMetrics();
        final float screenDensity = metrics.density;

        mWidth = progressCircleWidth * screenDensity;
        mHeight = progressCircleHeight * screenDensity;
        mInnerRadius = innerRadius * screenDensity;
        mStrokeWidth = strokeWidth * screenDensity;
        ring.setStrokeWidth((float) mStrokeWidth);
        ring.setInnerRadius(mInnerRadius);
        ring.setDensity(screenDensity);
        ring.setColorIndex(0);

        final float minEdge = (float) Math.min(mWidth, mHeight);
        if (mInnerRadius <= 0 || minEdge < 0) {
            ring.setInsets((int) Math.ceil(mStrokeWidth / 2.0f));
        } else {
            float insets = (float) (minEdge / 2.0f - mInnerRadius);
            ring.setInsets(insets);
        }
    }

    /**
     * Set the overall size for the progress spinner. This updates the radius
     * and stroke width of the ring.
     *
     * @param size One of {@link MaterialProgressDrawable.LARGE},
     *            {@link MaterialProgressDrawable.DEFAULT}, or
     *            {@link MaterialProgressDrawable.SMALL}.
     */
    public void updateSizes(@ProgressDrawableSize int size) {
        final DisplayMetrics metrics = mResources.getDisplayMetrics();
        final float screenDensity = metrics.density;
        int progressCircleWidth;
        int progressCircleHeight;
        float innerRadius;
        float strokeWidth;

        if (size == LARGE) {
            progressCircleWidth = progressCircleHeight = CIRCLE_DIAMETER_LARGE;
            innerRadius = INNER_RADIUS_LARGE;
            strokeWidth = STROKE_WIDTH_LARGE;
        } else if (size == SMALL) {
            progressCircleWidth = progressCircleHeight = CIRCLE_DIAMETER_SMALL;
            innerRadius = INNER_RADIUS_SMALL;
            strokeWidth = STROKE_WIDTH_SMALL;
        } else {
            progressCircleWidth = progressCircleHeight = CIRCLE_DIAMETER;
            innerRadius = INNER_RADIUS;
            strokeWidth = STROKE_WIDTH;
        }
        mWidth = progressCircleWidth * screenDensity;
        mHeight = progressCircleHeight * screenDensity;
        mInnerRadius = innerRadius * screenDensity;
        mStrokeWidth = strokeWidth * screenDensity;
    }

    /**
     * @param show Set to true to display the arrowhead on the progress spinner.
     */
    public void showArrow(boolean show) {
        mRing.setShowArrow(show);
    }

    /**
     * @param scale Set the scale of the arrowhead for the spinner.
     */
    public void setArrowScale(float scale) {
        mRing.setArrowScale(scale);
    }

    /**
     * Set the start and end trim for the progress spinner arc.
     *
     * @param startAngle start angle
     * @param endAngle end angle
     */
    public void setStartEndTrim(float startAngle, float endAngle) {
        mRing.setStartTrim(startAngle);
        mRing.setEndTrim(endAngle);
    }

    /**
     * Set the amount of rotation to apply to the progress spinner.
     *
     * @param rotation Rotation in degrees
     */
    public void setProgressRotation(float rotation) {
        mRing.setRotation(rotation);
    }

    /**
     * Set the colors used in the progress animation from color resources.
     * The first color will also be the color of the bar that grows in response
     * to a user swipe gesture.
     *
     * @param colors
     */
    public void setColorSchemeColors(int... colors) {
        mColors = colors;
        mRing.setColors(mColors);
    }

    @Override
    public int getIntrinsicHeight() {
        return (int) mHeight;
    }

    @Override
    public int getIntrinsicWidth() {
        return (int) mWidth;
    }

    @Override
    public void draw(Canvas c) {
        final Rect bounds = getBounds();
        final int saveCount = c.save();
        c.rotate(mRotation, bounds.exactCenterX(), bounds.exactCenterY());
        mRing.draw(c, bounds);
        c.restoreToCount(saveCount);
    }

    @Override
    public void setAlpha(int alpha) {
        mRing.setAlpha(alpha);
    }

    public int getAlpha() {
        return mRing.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mRing.setColorFilter(colorFilter);
    }

    @SuppressWarnings("unused")
    private void setRotation(float rotation) {
        mRotation = rotation;
        invalidateSelf();
    }

    @SuppressWarnings("unused")
    private float getRotation() {
        return mRotation;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public boolean isRunning() {
        final ArrayList<Animation> animators = mAnimators;
        final int N = animators.size();
        for (int i = 0; i < N; i++) {
            final Animation animator = animators.get(i);
            if (animator.hasStarted() && !animator.hasEnded()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        mAnimation.reset();
        mRing.storeOriginals();
        if (mRing.getStartingStartTrim() != 0) {
            mParent.startAnimation(mFinishAnimation);
        } else {
            mColorIndex = 0;
            mRing.setColorIndex(mColorIndex);
            mRing.resetOriginals();
            mParent.startAnimation(mAnimation);
        }
    }

    @Override
    public void stop() {
        mParent.clearAnimation();
        setRotation(0);
        mColorIndex = 0;
        mRing.setColorIndex(mColorIndex);
        mRing.resetOriginals();
    }

    private void setupAnimators() {
        final Ring ring = mRing;
        final Animation finishRingAnimation = new Animation() {
            public void applyTransformation(float interpolatedTime, Transformation t) {
                // shrink back down and complete a full roation before starting other circles
                float targetRotation = (float) (Math.floor(ring.getStartingRotation() / .75f) + 1f);
                final float startTrim = ring.getStartingEndTrim()
                        + (ring.getStartingStartTrim() - ring.getStartingEndTrim())
                        * interpolatedTime;
                ring.setEndTrim(startTrim);
                final float rotation = ring.getStartingRotation()
                        + ((targetRotation - ring.getStartingRotation()) * interpolatedTime);
                ring.setRotation(rotation);
            }
        };
        finishRingAnimation.setInterpolator(LINEAR_INTERPOLATOR);
        finishRingAnimation.setDuration(ANIMATION_DURATION / 2);
        finishRingAnimation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mColorIndex = (mColorIndex + 1) % (mColors.length);
                ring.setColorIndex(mColorIndex);
                ring.resetOriginals();
                mParent.startAnimation(mAnimation);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        final Animation animation = new Animation() {
            @Override
            public void applyTransformation(float interpolatedTime, Transformation t) {
                final float endTrim =
                        0.75f * START_CURVE_INTERPOLATOR
                                .getInterpolation(interpolatedTime);
                ring.setEndTrim(endTrim);
                final float startTrim = 0.75f * END_CURVE_INTERPOLATOR
                                .getInterpolation(interpolatedTime);
                ring.setStartTrim(startTrim);
                final float rotation = 0.25f * interpolatedTime;
                ring.setRotation(rotation);
                float groupRotation = ((720.0f / NUM_POINTS) * interpolatedTime)
                        + (720.0f * (mRotationCount / NUM_POINTS));
                setRotation(groupRotation);
            }
        };
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(LINEAR_INTERPOLATOR);
        animation.setDuration(ANIMATION_DURATION);
        animation.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                mRotationCount = 0;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                mColorIndex = (mColorIndex + 1) % (mColors.length);
                ring.setColorIndex(mColorIndex);
                ring.resetOriginals();
                mRotationCount = (mRotationCount + 1) % (NUM_POINTS);
            }
        });
        mFinishAnimation = finishRingAnimation;
        mAnimation = animation;
    }

    private final Callback mCallback = new Callback() {
        @Override
        public void invalidateDrawable(Drawable d) {
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(Drawable d, Runnable what, long when) {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(Drawable d, Runnable what) {
            unscheduleSelf(what);
        }
    };

    private static class Ring {
        private final RectF mTempBounds = new RectF();
        private final Paint mPaint = new Paint();
        private final Paint mArrowPaint = new Paint();

        private final Callback mCallback;

        private float mStartTrim = 0.0f;
        private float mEndTrim = 0.0f;
        private float mRotation = 0.0f;
        private float mStrokeWidth = 5.0f;
        private float mStrokeInset = 2.5f;

        private int[] mColors;
        private int mColorIndex;
        private float mStartingStartTrim;
        private float mStartingEndTrim;
        private float mStartingRotation;
        private boolean mShowArrow;
        private Path mArrow;
        private float mArrowScale;
        private double mRingInnerRadius;
        private Path mArrowCopy;
        private int mArrowWidth;
        private int mArrowHeight;
        private float mDensity;
        private Matrix mArrowScaleMatrix;

        public Ring(Callback callback) {
            mCallback = callback;

            mPaint.setStrokeCap(Cap.ROUND);
            mPaint.setAntiAlias(true);
            mPaint.setStyle(Style.STROKE);

            mArrowPaint.setStrokeWidth(4);
            mArrowPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mArrowPaint.setAntiAlias(true);
        }

        /**
         * Draw the progress spinner
         */
        public void draw(Canvas c, Rect bounds) {
            final RectF arcBounds = mTempBounds;
            arcBounds.set(bounds);
            arcBounds.inset(mStrokeInset, mStrokeInset);

            final float startAngle = (mStartTrim + mRotation) * 360;
            final float endAngle = (mEndTrim + mRotation) * 360;
            float sweepAngle = endAngle - startAngle;

            // Ensure the sweep angle isn't too small to draw.
            final float diameter = Math.min(arcBounds.width(), arcBounds.height());
            final float minAngle = (float) (360.0 / (diameter * Math.PI));
            if (sweepAngle < minAngle && sweepAngle > -minAngle) {
                sweepAngle = Math.signum(sweepAngle) * minAngle;
            }
            mPaint.setColor(mColors[mColorIndex]);
            c.drawArc(arcBounds, startAngle, sweepAngle, false, mPaint);

            if (mArrow == null) {
                mArrowWidth = (int) (ARROW_WIDTH * mDensity);
                mArrowHeight = (int) (ARROW_HEIGHT * mDensity);

                // Adjust the position of the triangle so that it is inset as much as the arc, but
                // also centered on the arc.
                int inset = (int) (mStrokeInset/2 + (mArrowWidth / mStrokeWidth));
                double rad = Math.toRadians(startAngle + sweepAngle);
                float x = (float) (mRingInnerRadius * Math.cos(rad) + bounds.exactCenterX());
                float y = (float) (mRingInnerRadius * Math.sin(rad) + bounds.exactCenterY());
                Point a = new Point((int) (x - inset), (int) (y));
                Point b = new Point((int) (x - inset) + mArrowWidth, (int) (y));
                Point cPoint = new Point((int) (x - inset) + (mArrowWidth / 2),
                        (int) (y) + mArrowHeight);

                mArrow = new android.graphics.Path();
                mArrow.setFillType(android.graphics.Path.FillType.EVEN_ODD);
                mArrow.moveTo(a.x, a.y);
                mArrow.lineTo(b.x, b.y);
                mArrow.lineTo(cPoint.x, cPoint.y);
                mArrow.lineTo(a.x, a.y);
                mArrow.close();
                mArrowCopy = new Path();
            }

            if (mShowArrow) {
                // draw a triangle
                if (mArrowScaleMatrix == null) {
                    mArrowScaleMatrix = new Matrix();
                }
                Matrix scaleMatrix = mArrowScaleMatrix;
                RectF arrowRect = mTempBounds;
                mArrow.computeBounds(arrowRect, true);
                scaleMatrix.setScale(mArrowScale, mArrowScale, arrowRect.centerX(),
                        arrowRect.centerY());
                mArrow.transform(scaleMatrix, mArrowCopy);
                mArrowPaint.setColor(mColors[mColorIndex]);
                // Offset the arrow slightly so that it aligns with the cap on the arc
                c.rotate(startAngle + sweepAngle - ARROW_OFFSET_ANGLE, bounds.exactCenterX(),
                        bounds.exactCenterY());
                c.drawPath(mArrowCopy, mArrowPaint);
            }
        }

        /**
         * Set the colors the progress spinner alternates between.
         *
         * @param colors Array of integers describing the colors. Must be non-<code>null</code>.
         */
        public void setColors(@NonNull int[] colors) {
            mColors = colors;
        }

        /**
         * @param index Index into the color array of the color to display in
         *            the progress spinner.
         */
        public void setColorIndex(int index) {
            mColorIndex = index;
        }

        public void setColorFilter(ColorFilter filter) {
            mPaint.setColorFilter(filter);
            invalidateSelf();
        }

        /**
         * @param alpha Set the alpha of the progress spinner and associated arrowhead.
         */
        public void setAlpha(int alpha) {
            final int oldAlpha = mPaint.getAlpha();
            if (alpha != oldAlpha) {
                mPaint.setAlpha(alpha);
                invalidateSelf();
            }
        }

        /**
         * @return Current alpha of the progress spinner and arrowhead.
         */
        public int getAlpha() {
            return mPaint.getAlpha();
        }

        /**
         * @param strokeWidth Set the stroke width of the progress spinner in pixels.
         */
        public void setStrokeWidth(float strokeWidth) {
            mStrokeWidth = strokeWidth;
            mPaint.setStrokeWidth(strokeWidth);
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getStrokeWidth() {
            return mStrokeWidth;
        }

        @SuppressWarnings("unused")
        public void setStartTrim(float startTrim) {
            mStartTrim = startTrim;
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getStartTrim() {
            return mStartTrim;
        }

        public float getStartingStartTrim() {
            return mStartingStartTrim;
        }

        public float getStartingEndTrim() {
            return mStartingEndTrim;
        }

        @SuppressWarnings("unused")
        public void setEndTrim(float endTrim) {
            mEndTrim = endTrim;
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getEndTrim() {
            return mEndTrim;
        }

        @SuppressWarnings("unused")
        public void setRotation(float rotation) {
            mRotation = rotation;
            invalidateSelf();
        }

        @SuppressWarnings("unused")
        public float getRotation() {
            return mRotation;
        }

        public void setInsets(float insets) {
            mStrokeInset = insets;
        }

        @SuppressWarnings("unused")
        public float getInsets() {
            return mStrokeInset;
        }


        /**
         * @param screenDensity Logical screen density on which the progress
         *            spinner is drawn.
         */
        public void setDensity(float screenDensity) {
            mDensity = screenDensity;
        }

        /**
         * @param innerRadius Inner radius in px of the circle the progress
         *            spinner arc traces.
         */
        public void setInnerRadius(double innerRadius) {
            mRingInnerRadius = innerRadius;
        }

        /**
         * @param show Set to true to show the arrow head on the progress spinner.
         */
        public void setShowArrow(boolean show) {
            if (mShowArrow != show) {
                mShowArrow = show;
                invalidateSelf();
            }
        }

        /**
         * @param scale Set the scale of the arrowhead for the spinner.
         */
        public void setArrowScale(float scale) {
            if (scale != mArrowScale) {
                mArrowScale = scale;
                invalidateSelf();
            }
        }

        /**
         * @return The amount in degrees the progress spinner is currently rotated.
         */
        public float getStartingRotation() {
            return mStartingRotation;
        }

        /**
         * If the start / end trim are offset to begin with, store them so that
         * animation starts from that offset.
         */
        public void storeOriginals() {
            mStartingStartTrim = mStartTrim;
            mStartingEndTrim = mEndTrim;
            mStartingRotation = mRotation;
        }

        /**
         * Reset the progress spinner to default rotation, start and end angles.
         */
        public void resetOriginals() {
            mStartingStartTrim = 0;
            mStartingEndTrim = 0;
            mStartingRotation = 0;
            setStartTrim(0);
            setEndTrim(0);
            setRotation(0);
        }

        private void invalidateSelf() {
            mCallback.invalidateDrawable(null);
        }
    }

    /**
     * Squishes the interpolation curve into the second half of the animation.
     */
    private static class EndCurveInterpolator extends AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.max(0, (input - 0.5f) * 2.0f));
        }
    }

    /**
     * Squishes the interpolation curve into the first half of the animation.
     */
    private static class StartCurveInterpolator extends AccelerateDecelerateInterpolator {
        @Override
        public float getInterpolation(float input) {
            return super.getInterpolation(Math.min(1, input * 2.0f));
        }
    }
}
