/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.wear.widget;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Px;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.wear.R;
import android.util.AttributeSet;
import android.view.View;

import java.util.Objects;

/**
 * An image view surrounded by a circle.
 *
 * @hide
 */
@TargetApi(Build.VERSION_CODES.M)
@RestrictTo(Scope.LIBRARY_GROUP)
public class CircledImageView extends View {

    private static final ArgbEvaluator ARGB_EVALUATOR = new ArgbEvaluator();

    private static final int SQUARE_DIMEN_NONE = 0;
    private static final int SQUARE_DIMEN_HEIGHT = 1;
    private static final int SQUARE_DIMEN_WIDTH = 2;

    private final RectF mOval;
    private final Paint mPaint;
    private final OvalShadowPainter mShadowPainter;
    private final float mInitialCircleRadius;
    private final ProgressDrawable mIndeterminateDrawable;
    private final Rect mIndeterminateBounds = new Rect();
    private final Drawable.Callback mDrawableCallback =
            new Drawable.Callback() {
                @Override
                public void invalidateDrawable(Drawable drawable) {
                    invalidate();
                }

                @Override
                public void scheduleDrawable(Drawable drawable, Runnable runnable, long l) {
                    // Not needed.
                }

                @Override
                public void unscheduleDrawable(Drawable drawable, Runnable runnable) {
                    // Not needed.
                }
            };
    private ColorStateList mCircleColor;
    private Drawable mDrawable;
    private float mCircleRadius;
    private float mCircleRadiusPercent;
    private float mCircleRadiusPressed;
    private float mCircleRadiusPressedPercent;
    private float mRadiusInset;
    private int mCircleBorderColor;
    private Paint.Cap mCircleBorderCap;
    private float mCircleBorderWidth;
    private boolean mCircleHidden = false;
    private float mProgress = 1f;
    private boolean mPressed = false;
    private boolean mProgressIndeterminate;
    private boolean mVisible;
    private boolean mWindowVisible;
    private long mColorChangeAnimationDurationMs = 0;
    private float mImageCirclePercentage = 1f;
    private float mImageHorizontalOffcenterPercentage = 0f;
    private Integer mImageTint;
    private Integer mSquareDimen;
    private int mCurrentColor;

    private final AnimatorUpdateListener mAnimationListener =
            new AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (int) animation.getAnimatedValue();
                    if (color != CircledImageView.this.mCurrentColor) {
                        CircledImageView.this.mCurrentColor = color;
                        CircledImageView.this.invalidate();
                    }
                }
            };

    private ValueAnimator mColorAnimator;

    public CircledImageView(Context context) {
        this(context, null);
    }

    public CircledImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircledImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircledImageView);
        mDrawable = a.getDrawable(R.styleable.CircledImageView_android_src);
        if (mDrawable != null && mDrawable.getConstantState() != null) {
            // The provided Drawable may be used elsewhere, so make a mutable clone before setTint()
            // or setAlpha() is called on it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDrawable =
                        mDrawable.getConstantState()
                                .newDrawable(context.getResources(), context.getTheme());
            } else {
                mDrawable = mDrawable.getConstantState().newDrawable(context.getResources());
            }
            mDrawable = mDrawable.mutate();
        }

        mCircleColor = a.getColorStateList(R.styleable.CircledImageView_background_color);
        if (mCircleColor == null) {
            mCircleColor = ColorStateList.valueOf(context.getColor(android.R.color.darker_gray));
        }

        mCircleRadius = a.getDimension(R.styleable.CircledImageView_background_radius, 0);
        mInitialCircleRadius = mCircleRadius;
        mCircleRadiusPressed = a.getDimension(
                R.styleable.CircledImageView_background_radius_pressed, mCircleRadius);
        mCircleBorderColor = a
                .getColor(R.styleable.CircledImageView_background_border_color, Color.BLACK);
        mCircleBorderCap =
                Paint.Cap.values()[a.getInt(R.styleable.CircledImageView_background_border_cap, 0)];
        mCircleBorderWidth = a.getDimension(
                R.styleable.CircledImageView_background_border_width, 0);

        if (mCircleBorderWidth > 0) {
            // The border arc is drawn from the middle of the arc - take that into account.
            mRadiusInset += mCircleBorderWidth / 2;
        }

        float circlePadding = a.getDimension(R.styleable.CircledImageView_img_padding, 0);
        if (circlePadding > 0) {
            mRadiusInset += circlePadding;
        }

        mImageCirclePercentage = a
                .getFloat(R.styleable.CircledImageView_img_circle_percentage, 0f);

        mImageHorizontalOffcenterPercentage =
                a.getFloat(R.styleable.CircledImageView_img_horizontal_offset_percentage, 0f);

        if (a.hasValue(R.styleable.CircledImageView_img_tint)) {
            mImageTint = a.getColor(R.styleable.CircledImageView_img_tint, 0);
        }

        if (a.hasValue(R.styleable.CircledImageView_clip_dimen)) {
            mSquareDimen = a.getInt(R.styleable.CircledImageView_clip_dimen, SQUARE_DIMEN_NONE);
        }

        mCircleRadiusPercent =
                a.getFraction(R.styleable.CircledImageView_background_radius_percent, 1, 1, 0f);

        mCircleRadiusPressedPercent =
                a.getFraction(
                        R.styleable.CircledImageView_background_radius_pressed_percent, 1, 1,
                        mCircleRadiusPercent);

        float shadowWidth = a.getDimension(R.styleable.CircledImageView_background_shadow_width, 0);

        a.recycle();

        mOval = new RectF();
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mShadowPainter = new OvalShadowPainter(shadowWidth, 0, getCircleRadius(),
                mCircleBorderWidth);

        mIndeterminateDrawable = new ProgressDrawable();
        // {@link #mDrawableCallback} must be retained as a member, as Drawable callback
        // is held by weak reference, we must retain it for it to continue to be called.
        mIndeterminateDrawable.setCallback(mDrawableCallback);

        setWillNotDraw(false);

        setColorForCurrentState();
    }

    /** Sets the circle to be hidden. */
    public void setCircleHidden(boolean circleHidden) {
        if (circleHidden != mCircleHidden) {
            mCircleHidden = circleHidden;
            invalidate();
        }
    }

    @Override
    protected boolean onSetAlpha(int alpha) {
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();

        float circleRadius = mPressed ? getCircleRadiusPressed() : getCircleRadius();

        // Maybe draw the shadow
        mShadowPainter.draw(canvas, getAlpha());
        if (mCircleBorderWidth > 0) {
            // First let's find the center of the view.
            mOval.set(
                    paddingLeft,
                    paddingTop,
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom());
            // Having the center, lets make the border meet the circle.
            mOval.set(
                    mOval.centerX() - circleRadius,
                    mOval.centerY() - circleRadius,
                    mOval.centerX() + circleRadius,
                    mOval.centerY() + circleRadius);
            mPaint.setColor(mCircleBorderColor);
            // {@link #Paint.setAlpha} is a helper method that just sets the alpha portion of the
            // color. {@link #Paint.setPaint} will clear any previously set alpha value.
            mPaint.setAlpha(Math.round(mPaint.getAlpha() * getAlpha()));
            mPaint.setStyle(Style.STROKE);
            mPaint.setStrokeWidth(mCircleBorderWidth);
            mPaint.setStrokeCap(mCircleBorderCap);

            if (mProgressIndeterminate) {
                mOval.roundOut(mIndeterminateBounds);
                mIndeterminateDrawable.setBounds(mIndeterminateBounds);
                mIndeterminateDrawable.setRingColor(mCircleBorderColor);
                mIndeterminateDrawable.setRingWidth(mCircleBorderWidth);
                mIndeterminateDrawable.draw(canvas);
            } else {
                canvas.drawArc(mOval, -90, 360 * mProgress, false, mPaint);
            }
        }
        if (!mCircleHidden) {
            mOval.set(
                    paddingLeft,
                    paddingTop,
                    getWidth() - getPaddingRight(),
                    getHeight() - getPaddingBottom());
            // {@link #Paint.setAlpha} is a helper method that just sets the alpha portion of the
            // color. {@link #Paint.setPaint} will clear any previously set alpha value.
            mPaint.setColor(mCurrentColor);
            mPaint.setAlpha(Math.round(mPaint.getAlpha() * getAlpha()));

            mPaint.setStyle(Style.FILL);
            float centerX = mOval.centerX();
            float centerY = mOval.centerY();

            canvas.drawCircle(centerX, centerY, circleRadius, mPaint);
        }

        if (mDrawable != null) {
            mDrawable.setAlpha(Math.round(getAlpha() * 255));

            if (mImageTint != null) {
                mDrawable.setTint(mImageTint);
            }
            mDrawable.draw(canvas);
        }

        super.onDraw(canvas);
    }

    private void setColorForCurrentState() {
        int newColor =
                mCircleColor.getColorForState(getDrawableState(), mCircleColor.getDefaultColor());
        if (mColorChangeAnimationDurationMs > 0) {
            if (mColorAnimator != null) {
                mColorAnimator.cancel();
            } else {
                mColorAnimator = new ValueAnimator();
            }
            mColorAnimator.setIntValues(new int[]{mCurrentColor, newColor});
            mColorAnimator.setEvaluator(ARGB_EVALUATOR);
            mColorAnimator.setDuration(mColorChangeAnimationDurationMs);
            mColorAnimator.addUpdateListener(this.mAnimationListener);
            mColorAnimator.start();
        } else {
            if (newColor != mCurrentColor) {
                mCurrentColor = newColor;
                invalidate();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        final float radius =
                getCircleRadius()
                        + mCircleBorderWidth
                        + mShadowPainter.mShadowWidth * mShadowPainter.mShadowVisibility;
        float desiredWidth = radius * 2;
        float desiredHeight = radius * 2;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = (int) Math.min(desiredWidth, widthSize);
        } else {
            width = (int) desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = (int) Math.min(desiredHeight, heightSize);
        } else {
            height = (int) desiredHeight;
        }

        if (mSquareDimen != null) {
            switch (mSquareDimen) {
                case SQUARE_DIMEN_HEIGHT:
                    width = height;
                    break;
                case SQUARE_DIMEN_WIDTH:
                    height = width;
                    break;
            }
        }

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mDrawable != null) {
            // Retrieve the sizes of the drawable and the view.
            final int nativeDrawableWidth = mDrawable.getIntrinsicWidth();
            final int nativeDrawableHeight = mDrawable.getIntrinsicHeight();
            final int viewWidth = getMeasuredWidth();
            final int viewHeight = getMeasuredHeight();
            final float imageCirclePercentage =
                    mImageCirclePercentage > 0 ? mImageCirclePercentage : 1;

            final float scaleFactor =
                    Math.min(
                            1f,
                            Math.min(
                                    (float) nativeDrawableWidth != 0
                                            ? imageCirclePercentage * viewWidth
                                            / nativeDrawableWidth
                                            : 1,
                                    (float) nativeDrawableHeight != 0
                                            ? imageCirclePercentage * viewHeight
                                            / nativeDrawableHeight
                                            : 1));

            // Scale the drawable down to fit the view, if needed.
            final int drawableWidth = Math.round(scaleFactor * nativeDrawableWidth);
            final int drawableHeight = Math.round(scaleFactor * nativeDrawableHeight);

            // Center the drawable within the view.
            final int drawableLeft =
                    (viewWidth - drawableWidth) / 2
                            + Math.round(mImageHorizontalOffcenterPercentage * drawableWidth);
            final int drawableTop = (viewHeight - drawableHeight) / 2;

            mDrawable.setBounds(
                    drawableLeft, drawableTop, drawableLeft + drawableWidth,
                    drawableTop + drawableHeight);
        }

        super.onLayout(changed, left, top, right, bottom);
    }

    /** Sets the image given a resource. */
    public void setImageResource(int resId) {
        setImageDrawable(resId == 0 ? null : getContext().getDrawable(resId));
    }

    /** Sets the size of the image based on a percentage in [0, 1]. */
    public void setImageCirclePercentage(float percentage) {
        float clamped = Math.max(0, Math.min(1, percentage));
        if (clamped != mImageCirclePercentage) {
            mImageCirclePercentage = clamped;
            invalidate();
        }
    }

    /** Sets the horizontal offset given a percentage in [0, 1]. */
    public void setImageHorizontalOffcenterPercentage(float percentage) {
        if (percentage != mImageHorizontalOffcenterPercentage) {
            mImageHorizontalOffcenterPercentage = percentage;
            invalidate();
        }
    }

    /** Sets the tint. */
    public void setImageTint(int tint) {
        if (mImageTint == null || tint != mImageTint) {
            mImageTint = tint;
            invalidate();
        }
    }

    /** Returns the circle radius. */
    public float getCircleRadius() {
        float radius = mCircleRadius;
        if (mCircleRadius <= 0 && mCircleRadiusPercent > 0) {
            radius = Math.max(getMeasuredHeight(), getMeasuredWidth()) * mCircleRadiusPercent;
        }

        return radius - mRadiusInset;
    }

    /** Sets the circle radius. */
    public void setCircleRadius(float circleRadius) {
        if (circleRadius != mCircleRadius) {
            mCircleRadius = circleRadius;
            mShadowPainter
                    .setInnerCircleRadius(mPressed ? getCircleRadiusPressed() : getCircleRadius());
            invalidate();
        }
    }

    /** Gets the circle radius percent. */
    public float getCircleRadiusPercent() {
        return mCircleRadiusPercent;
    }

    /**
     * Sets the radius of the circle to be a percentage of the largest dimension of the view.
     *
     * @param circleRadiusPercent A {@code float} from 0 to 1 representing the radius percentage.
     */
    public void setCircleRadiusPercent(float circleRadiusPercent) {
        if (circleRadiusPercent != mCircleRadiusPercent) {
            mCircleRadiusPercent = circleRadiusPercent;
            mShadowPainter
                    .setInnerCircleRadius(mPressed ? getCircleRadiusPressed() : getCircleRadius());
            invalidate();
        }
    }

    /** Gets the circle radius when pressed. */
    public float getCircleRadiusPressed() {
        float radius = mCircleRadiusPressed;

        if (mCircleRadiusPressed <= 0 && mCircleRadiusPressedPercent > 0) {
            radius =
                    Math.max(getMeasuredHeight(), getMeasuredWidth()) * mCircleRadiusPressedPercent;
        }

        return radius - mRadiusInset;
    }

    /** Sets the circle radius when pressed. */
    public void setCircleRadiusPressed(float circleRadiusPressed) {
        if (circleRadiusPressed != mCircleRadiusPressed) {
            mCircleRadiusPressed = circleRadiusPressed;
            invalidate();
        }
    }

    /** Gets the circle radius when pressed as a percent. */
    public float getCircleRadiusPressedPercent() {
        return mCircleRadiusPressedPercent;
    }

    /**
     * Sets the radius of the circle to be a percentage of the largest dimension of the view when
     * pressed.
     *
     * @param circleRadiusPressedPercent A {@code float} from 0 to 1 representing the radius
     * percentage.
     */
    public void setCircleRadiusPressedPercent(float circleRadiusPressedPercent) {
        if (circleRadiusPressedPercent != mCircleRadiusPressedPercent) {
            mCircleRadiusPressedPercent = circleRadiusPressedPercent;
            mShadowPainter
                    .setInnerCircleRadius(mPressed ? getCircleRadiusPressed() : getCircleRadius());
            invalidate();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        setColorForCurrentState();
    }

    /** Sets the circle color. */
    public void setCircleColor(int circleColor) {
        setCircleColorStateList(ColorStateList.valueOf(circleColor));
    }

    /** Gets the circle color. */
    public ColorStateList getCircleColorStateList() {
        return mCircleColor;
    }

    /** Sets the circle color. */
    public void setCircleColorStateList(ColorStateList circleColor) {
        if (!Objects.equals(circleColor, mCircleColor)) {
            mCircleColor = circleColor;
            setColorForCurrentState();
            invalidate();
        }
    }

    /** Gets the default circle color. */
    public int getDefaultCircleColor() {
        return mCircleColor.getDefaultColor();
    }

    /**
     * Show the circle border as an indeterminate progress spinner. The views circle border width
     * and color must be set for this to have an effect.
     *
     * @param show true if the progress spinner is shown, false to hide it.
     */
    public void showIndeterminateProgress(boolean show) {
        mProgressIndeterminate = show;
        if (mIndeterminateDrawable != null) {
            if (show && mVisible && mWindowVisible) {
                mIndeterminateDrawable.startAnimation();
            } else {
                mIndeterminateDrawable.stopAnimation();
            }
        }
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        mVisible = (visibility == View.VISIBLE);
        showIndeterminateProgress(mProgressIndeterminate);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        mWindowVisible = (visibility == View.VISIBLE);
        showIndeterminateProgress(mProgressIndeterminate);
    }

    /** Sets the progress. */
    public void setProgress(float progress) {
        if (progress != mProgress) {
            mProgress = progress;
            invalidate();
        }
    }

    /**
     * Set how much of the shadow should be shown.
     *
     * @param shadowVisibility Value between 0 and 1.
     */
    public void setShadowVisibility(float shadowVisibility) {
        if (shadowVisibility != mShadowPainter.mShadowVisibility) {
            mShadowPainter.setShadowVisibility(shadowVisibility);
            invalidate();
        }
    }

    public float getInitialCircleRadius() {
        return mInitialCircleRadius;
    }

    public void setCircleBorderColor(int circleBorderColor) {
        mCircleBorderColor = circleBorderColor;
    }

    /**
     * Set the border around the circle.
     *
     * @param circleBorderWidth Width of the border around the circle.
     */
    public void setCircleBorderWidth(float circleBorderWidth) {
        if (circleBorderWidth != mCircleBorderWidth) {
            mCircleBorderWidth = circleBorderWidth;
            mShadowPainter.setInnerCircleBorderWidth(circleBorderWidth);
            invalidate();
        }
    }

    /**
     * Set the stroke cap for the border around the circle.
     *
     * @param circleBorderCap Stroke cap for the border around the circle.
     */
    public void setCircleBorderCap(Paint.Cap circleBorderCap) {
        if (circleBorderCap != mCircleBorderCap) {
            mCircleBorderCap = circleBorderCap;
            invalidate();
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (pressed != mPressed) {
            mPressed = pressed;
            mShadowPainter
                    .setInnerCircleRadius(mPressed ? getCircleRadiusPressed() : getCircleRadius());
            invalidate();
        }
    }

    @Override
    public void setPadding(@Px int left, @Px int top, @Px int right, @Px int bottom) {
        if (left != getPaddingLeft()
                || top != getPaddingTop()
                || right != getPaddingRight()
                || bottom != getPaddingBottom()) {
            mShadowPainter.setBounds(left, top, getWidth() - right, getHeight() - bottom);
        }
        super.setPadding(left, top, right, bottom);
    }

    @Override
    public void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        if (newWidth != oldWidth || newHeight != oldHeight) {
            mShadowPainter.setBounds(
                    getPaddingLeft(),
                    getPaddingTop(),
                    newWidth - getPaddingRight(),
                    newHeight - getPaddingBottom());
        }
    }

    public Drawable getImageDrawable() {
        return mDrawable;
    }

    /** Sets the image drawable. */
    public void setImageDrawable(Drawable drawable) {
        if (drawable != mDrawable) {
            final Drawable existingDrawable = mDrawable;
            mDrawable = drawable;
            if (mDrawable != null && mDrawable.getConstantState() != null) {
                // The provided Drawable may be used elsewhere, so make a mutable clone before
                // setTint() or setAlpha() is called on it.
                mDrawable =
                        mDrawable
                                .getConstantState()
                                .newDrawable(getResources(), getContext().getTheme())
                                .mutate();
            }

            final boolean skipLayout =
                    drawable != null
                            && existingDrawable != null
                            && existingDrawable.getIntrinsicHeight() == drawable
                            .getIntrinsicHeight()
                            && existingDrawable.getIntrinsicWidth() == drawable.getIntrinsicWidth();

            if (skipLayout) {
                mDrawable.setBounds(existingDrawable.getBounds());
            } else {
                requestLayout();
            }

            invalidate();
        }
    }

    /**
     * @return the milliseconds duration of the transition animation when the color changes.
     */
    public long getColorChangeAnimationDuration() {
        return mColorChangeAnimationDurationMs;
    }

    /**
     * @param mColorChangeAnimationDurationMs the milliseconds duration of the color change
     * animation. The color change animation will run if the color changes with {@link
     * #setCircleColor} or as a result of the active state changing.
     */
    public void setColorChangeAnimationDuration(long mColorChangeAnimationDurationMs) {
        this.mColorChangeAnimationDurationMs = mColorChangeAnimationDurationMs;
    }

    /**
     * Helper class taking care of painting a shadow behind the displayed image. TODO(amad): Replace
     * this with elevation, when moving to support/wearable?
     */
    private static class OvalShadowPainter {

        private final int[] mShaderColors = new int[]{Color.BLACK, Color.TRANSPARENT};
        private final float[] mShaderStops = new float[]{0.6f, 1f};
        private final RectF mBounds = new RectF();
        private final float mShadowWidth;
        private final Paint mShadowPaint = new Paint();

        private float mShadowRadius;
        private float mShadowVisibility;
        private float mInnerCircleRadius;
        private float mInnerCircleBorderWidth;

        OvalShadowPainter(
                float shadowWidth,
                float shadowVisibility,
                float innerCircleRadius,
                float innerCircleBorderWidth) {
            mShadowWidth = shadowWidth;
            mShadowVisibility = shadowVisibility;
            mInnerCircleRadius = innerCircleRadius;
            mInnerCircleBorderWidth = innerCircleBorderWidth;
            mShadowRadius =
                    mInnerCircleRadius + mInnerCircleBorderWidth + mShadowWidth * mShadowVisibility;
            mShadowPaint.setColor(Color.BLACK);
            mShadowPaint.setStyle(Style.FILL);
            mShadowPaint.setAntiAlias(true);
            updateRadialGradient();
        }

        void draw(Canvas canvas, float alpha) {
            if (mShadowWidth > 0 && mShadowVisibility > 0) {
                mShadowPaint.setAlpha(Math.round(mShadowPaint.getAlpha() * alpha));
                canvas.drawCircle(mBounds.centerX(), mBounds.centerY(), mShadowRadius,
                        mShadowPaint);
            }
        }

        void setBounds(@Px int left, @Px int top, @Px int right, @Px int bottom) {
            mBounds.set(left, top, right, bottom);
            updateRadialGradient();
        }

        void setInnerCircleRadius(float newInnerCircleRadius) {
            mInnerCircleRadius = newInnerCircleRadius;
            updateRadialGradient();
        }

        void setInnerCircleBorderWidth(float newInnerCircleBorderWidth) {
            mInnerCircleBorderWidth = newInnerCircleBorderWidth;
            updateRadialGradient();
        }

        void setShadowVisibility(float newShadowVisibility) {
            mShadowVisibility = newShadowVisibility;
            updateRadialGradient();
        }

        private void updateRadialGradient() {
            // Make the shadow start beyond the circled and possibly the border.
            mShadowRadius =
                    mInnerCircleRadius + mInnerCircleBorderWidth + mShadowWidth * mShadowVisibility;
            // This may happen if the innerCircleRadius has not been correctly computed yet while
            // the view has already been inflated, but not yet measured. In this case, if the view
            // specifies the radius as a percentage of the screen width, then that evaluates to 0
            // and will be corrected after measuring, through onSizeChanged().
            if (mShadowRadius > 0) {
                mShadowPaint.setShader(
                        new RadialGradient(
                                mBounds.centerX(),
                                mBounds.centerY(),
                                mShadowRadius,
                                mShaderColors,
                                mShaderStops,
                                Shader.TileMode.MIRROR));
            }
        }
    }
}
