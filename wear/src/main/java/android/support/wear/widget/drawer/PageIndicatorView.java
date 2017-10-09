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

package android.support.wear.widget.drawer;

import android.animation.Animator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.wear.R;
import android.support.wear.widget.SimpleAnimatorListener;
import android.util.AttributeSet;
import android.view.View;

import java.util.concurrent.TimeUnit;

/**
 * A page indicator for {@link ViewPager} based on {@link
 * android.support.wear.view.DotsPageIndicator} which identifies the current page in relation to
 * all available pages. Pages are represented as dots. The current page can be highlighted with a
 * different color or size dot.
 *
 * <p>The default behavior is to fade out the dots when the pager is idle (not settling or being
 * dragged). This can be changed with {@link #setDotFadeWhenIdle(boolean)}.
 *
 * <p>Use {@link #setPager(ViewPager)} to connect this view to a pager instance.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.M)
@RestrictTo(Scope.LIBRARY_GROUP)
public class PageIndicatorView extends View implements OnPageChangeListener {

    private static final String TAG = "Dots";
    private final Paint mDotPaint;
    private final Paint mDotPaintShadow;
    private final Paint mDotPaintSelected;
    private final Paint mDotPaintShadowSelected;
    private int mDotSpacing;
    private float mDotRadius;
    private float mDotRadiusSelected;
    private int mDotColor;
    private int mDotColorSelected;
    private boolean mDotFadeWhenIdle;
    private int mDotFadeOutDelay;
    private int mDotFadeOutDuration;
    private int mDotFadeInDuration;
    private float mDotShadowDx;
    private float mDotShadowDy;
    private float mDotShadowRadius;
    private int mDotShadowColor;
    private PagerAdapter mAdapter;
    private int mNumberOfPositions;
    private int mSelectedPosition;
    private int mCurrentViewPagerState;
    private boolean mVisible;

    public PageIndicatorView(Context context) {
        this(context, null);
    }

    public PageIndicatorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PageIndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final TypedArray a =
                getContext()
                        .obtainStyledAttributes(
                                attrs, R.styleable.PageIndicatorView, defStyleAttr,
                                R.style.WsPageIndicatorViewStyle);

        mDotSpacing = a.getDimensionPixelOffset(
                R.styleable.PageIndicatorView_wsPageIndicatorDotSpacing, 0);
        mDotRadius = a.getDimension(R.styleable.PageIndicatorView_wsPageIndicatorDotRadius, 0);
        mDotRadiusSelected =
                a.getDimension(R.styleable.PageIndicatorView_wsPageIndicatorDotRadiusSelected, 0);
        mDotColor = a.getColor(R.styleable.PageIndicatorView_wsPageIndicatorDotColor, 0);
        mDotColorSelected = a
                .getColor(R.styleable.PageIndicatorView_wsPageIndicatorDotColorSelected, 0);
        mDotFadeOutDelay =
                a.getInt(R.styleable.PageIndicatorView_wsPageIndicatorDotFadeOutDelay, 0);
        mDotFadeOutDuration =
                a.getInt(R.styleable.PageIndicatorView_wsPageIndicatorDotFadeOutDuration, 0);
        mDotFadeInDuration =
                a.getInt(R.styleable.PageIndicatorView_wsPageIndicatorDotFadeInDuration, 0);
        mDotFadeWhenIdle =
                a.getBoolean(R.styleable.PageIndicatorView_wsPageIndicatorDotFadeWhenIdle, false);
        mDotShadowDx = a.getDimension(R.styleable.PageIndicatorView_wsPageIndicatorDotShadowDx, 0);
        mDotShadowDy = a.getDimension(R.styleable.PageIndicatorView_wsPageIndicatorDotShadowDy, 0);
        mDotShadowRadius =
                a.getDimension(R.styleable.PageIndicatorView_wsPageIndicatorDotShadowRadius, 0);
        mDotShadowColor =
                a.getColor(R.styleable.PageIndicatorView_wsPageIndicatorDotShadowColor, 0);
        a.recycle();

        mDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaint.setColor(mDotColor);
        mDotPaint.setStyle(Style.FILL);

        mDotPaintSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaintSelected.setColor(mDotColorSelected);
        mDotPaintSelected.setStyle(Style.FILL);
        mDotPaintShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDotPaintShadowSelected = new Paint(Paint.ANTI_ALIAS_FLAG);

        mCurrentViewPagerState = ViewPager.SCROLL_STATE_IDLE;
        if (isInEditMode()) {
            // When displayed in layout preview:
            // Simulate 5 positions, currently on the 3rd position.
            mNumberOfPositions = 5;
            mSelectedPosition = 2;
            mDotFadeWhenIdle = false;
        }

        if (mDotFadeWhenIdle) {
            mVisible = false;
            animate().alpha(0f).setStartDelay(2000).setDuration(mDotFadeOutDuration).start();
        } else {
            animate().cancel();
            setAlpha(1.0f);
        }
        updateShadows();
    }

    private void updateShadows() {
        updateDotPaint(
                mDotPaint, mDotPaintShadow, mDotRadius, mDotShadowRadius, mDotColor,
                mDotShadowColor);
        updateDotPaint(
                mDotPaintSelected,
                mDotPaintShadowSelected,
                mDotRadiusSelected,
                mDotShadowRadius,
                mDotColorSelected,
                mDotShadowColor);
    }

    private void updateDotPaint(
            Paint dotPaint,
            Paint shadowPaint,
            float baseRadius,
            float shadowRadius,
            int color,
            int shadowColor) {
        float radius = baseRadius + shadowRadius;
        float shadowStart = baseRadius / radius;
        Shader gradient =
                new RadialGradient(
                        0,
                        0,
                        radius,
                        new int[]{shadowColor, shadowColor, Color.TRANSPARENT},
                        new float[]{0f, shadowStart, 1f},
                        TileMode.CLAMP);

        shadowPaint.setShader(gradient);
        dotPaint.setColor(color);
        dotPaint.setStyle(Style.FILL);
    }

    /**
     * Supplies the ViewPager instance, and attaches this views {@link OnPageChangeListener} to the
     * pager.
     *
     * @param pager the pager for the page indicator
     */
    public void setPager(ViewPager pager) {
        pager.addOnPageChangeListener(this);
        setPagerAdapter(pager.getAdapter());
        mAdapter = pager.getAdapter();
        if (mAdapter != null && mAdapter.getCount() > 0) {
            positionChanged(0);
        }
    }

    /**
     * Gets the center-to-center distance between page dots.
     *
     * @return the distance between page dots
     */
    public float getDotSpacing() {
        return mDotSpacing;
    }

    /**
     * Sets the center-to-center distance between page dots.
     *
     * @param spacing the distance between page dots
     */
    public void setDotSpacing(int spacing) {
        if (mDotSpacing != spacing) {
            mDotSpacing = spacing;
            requestLayout();
        }
    }

    /**
     * Gets the radius of the page dots.
     *
     * @return the radius of the page dots
     */
    public float getDotRadius() {
        return mDotRadius;
    }

    /**
     * Sets the radius of the page dots.
     *
     * @param radius the radius of the page dots
     */
    public void setDotRadius(int radius) {
        if (mDotRadius != radius) {
            mDotRadius = radius;
            updateShadows();
            invalidate();
        }
    }

    /**
     * Gets the radius of the page dot for the selected page.
     *
     * @return the radius of the selected page dot
     */
    public float getDotRadiusSelected() {
        return mDotRadiusSelected;
    }

    /**
     * Sets the radius of the page dot for the selected page.
     *
     * @param radius the radius of the selected page dot
     */
    public void setDotRadiusSelected(int radius) {
        if (mDotRadiusSelected != radius) {
            mDotRadiusSelected = radius;
            updateShadows();
            invalidate();
        }
    }

    /**
     * Returns the color used for dots other than the selected page.
     *
     * @return color the color used for dots other than the selected page
     */
    public int getDotColor() {
        return mDotColor;
    }

    /**
     * Sets the color used for dots other than the selected page.
     *
     * @param color the color used for dots other than the selected page
     */
    public void setDotColor(int color) {
        if (mDotColor != color) {
            mDotColor = color;
            invalidate();
        }
    }

    /**
     * Returns the color of the dot for the selected page.
     *
     * @return the color used for the selected page dot
     */
    public int getDotColorSelected() {
        return mDotColorSelected;
    }

    /**
     * Sets the color of the dot for the selected page.
     *
     * @param color the color of the dot for the selected page
     */
    public void setDotColorSelected(int color) {
        if (mDotColorSelected != color) {
            mDotColorSelected = color;
            invalidate();
        }
    }

    /**
     * Indicates if the dots fade out when the pager is idle.
     *
     * @return whether the dots fade out when idle
     */
    public boolean getDotFadeWhenIdle() {
        return mDotFadeWhenIdle;
    }

    /**
     * Sets whether the dots fade out when the pager is idle.
     *
     * @param fade whether the dots fade out when idle
     */
    public void setDotFadeWhenIdle(boolean fade) {
        mDotFadeWhenIdle = fade;
        if (!fade) {
            fadeIn();
        }
    }

    /**
     * Returns the duration of fade out animation, in milliseconds.
     *
     * @return the duration of the fade out animation, in milliseconds
     */
    public int getDotFadeOutDuration() {
        return mDotFadeOutDuration;
    }

    /**
     * Sets the duration of the fade out animation.
     *
     * @param duration the duration of the fade out animation
     */
    public void setDotFadeOutDuration(int duration, TimeUnit unit) {
        mDotFadeOutDuration = (int) TimeUnit.MILLISECONDS.convert(duration, unit);
    }

    /**
     * Returns the duration of the fade in duration, in milliseconds.
     *
     * @return the duration of the fade in duration, in milliseconds
     */
    public int getDotFadeInDuration() {
        return mDotFadeInDuration;
    }

    /**
     * Sets the duration of the fade in animation.
     *
     * @param duration the duration of the fade in animation
     */
    public void setDotFadeInDuration(int duration, TimeUnit unit) {
        mDotFadeInDuration = (int) TimeUnit.MILLISECONDS.convert(duration, unit);
    }

    /**
     * Sets the delay between the pager arriving at an idle state, and the fade out animation
     * beginning, in milliseconds.
     *
     * @return the delay before the fade out animation begins, in milliseconds
     */
    public int getDotFadeOutDelay() {
        return mDotFadeOutDelay;
    }

    /**
     * Sets the delay between the pager arriving at an idle state, and the fade out animation
     * beginning, in milliseconds.
     *
     * @param delay the delay before the fade out animation begins, in milliseconds
     */
    public void setDotFadeOutDelay(int delay) {
        mDotFadeOutDelay = delay;
    }

    /**
     * Sets the pixel radius of shadows drawn beneath the dots.
     *
     * @return the pixel radius of shadows rendered beneath the dots
     */
    public float getDotShadowRadius() {
        return mDotShadowRadius;
    }

    /**
     * Sets the pixel radius of shadows drawn beneath the dots.
     *
     * @param radius the pixel radius of shadows rendered beneath the dots
     */
    public void setDotShadowRadius(float radius) {
        if (mDotShadowRadius != radius) {
            mDotShadowRadius = radius;
            updateShadows();
            invalidate();
        }
    }

    /**
     * Returns the horizontal offset of shadows drawn beneath the dots.
     *
     * @return the horizontal offset of shadows drawn beneath the dots
     */
    public float getDotShadowDx() {
        return mDotShadowDx;
    }

    /**
     * Sets the horizontal offset of shadows drawn beneath the dots.
     *
     * @param dx the horizontal offset of shadows drawn beneath the dots
     */
    public void setDotShadowDx(float dx) {
        mDotShadowDx = dx;
        invalidate();
    }

    /**
     * Returns the vertical offset of shadows drawn beneath the dots.
     *
     * @return the vertical offset of shadows drawn beneath the dots
     */
    public float getDotShadowDy() {
        return mDotShadowDy;
    }

    /**
     * Sets the vertical offset of shadows drawn beneath the dots.
     *
     * @param dy the vertical offset of shadows drawn beneath the dots
     */
    public void setDotShadowDy(float dy) {
        mDotShadowDy = dy;
        invalidate();
    }

    /**
     * Returns the color of the shadows drawn beneath the dots.
     *
     * @return the color of the shadows drawn beneath the dots
     */
    public int getDotShadowColor() {
        return mDotShadowColor;
    }

    /**
     * Sets the color of the shadows drawn beneath the dots.
     *
     * @param color the color of the shadows drawn beneath the dots
     */
    public void setDotShadowColor(int color) {
        mDotShadowColor = color;
        updateShadows();
        invalidate();
    }

    private void positionChanged(int position) {
        mSelectedPosition = position;
        invalidate();
    }

    private void updateNumberOfPositions() {
        int count = mAdapter.getCount();
        if (count != mNumberOfPositions) {
            mNumberOfPositions = count;
            requestLayout();
        }
    }

    private void fadeIn() {
        mVisible = true;
        animate().cancel();
        animate().alpha(1f).setStartDelay(0).setDuration(mDotFadeInDuration).start();
    }

    private void fadeOut(long delayMillis) {
        mVisible = false;
        animate().cancel();
        animate().alpha(0f).setStartDelay(delayMillis).setDuration(mDotFadeOutDuration).start();
    }

    private void fadeInOut() {
        mVisible = true;
        animate().cancel();
        animate()
                .alpha(1f)
                .setStartDelay(0)
                .setDuration(mDotFadeInDuration)
                .setListener(
                        new SimpleAnimatorListener() {
                            @Override
                            public void onAnimationComplete(Animator animator) {
                                mVisible = false;
                                animate()
                                        .alpha(0f)
                                        .setListener(null)
                                        .setStartDelay(mDotFadeOutDelay)
                                        .setDuration(mDotFadeOutDuration)
                                        .start();
                            }
                        })
                .start();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mDotFadeWhenIdle) {
            if (mCurrentViewPagerState == ViewPager.SCROLL_STATE_DRAGGING) {
                if (positionOffset != 0) {
                    if (!mVisible) {
                        fadeIn();
                    }
                } else {
                    if (mVisible) {
                        fadeOut(0);
                    }
                }
            }
        }
    }

    @Override
    public void onPageSelected(int position) {
        if (position != mSelectedPosition) {
            positionChanged(position);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        if (mCurrentViewPagerState != state) {
            mCurrentViewPagerState = state;
            if (mDotFadeWhenIdle) {
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    if (mVisible) {
                        fadeOut(mDotFadeOutDelay);
                    } else {
                        fadeInOut();
                    }
                }
            }
        }
    }

    /**
     * Sets the {@link PagerAdapter}.
     */
    public void setPagerAdapter(PagerAdapter adapter) {
        mAdapter = adapter;
        if (mAdapter != null) {
            updateNumberOfPositions();
            if (mDotFadeWhenIdle) {
                fadeInOut();
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int totalWidth;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            totalWidth = MeasureSpec.getSize(widthMeasureSpec);
        } else {
            int contentWidth = mNumberOfPositions * mDotSpacing;
            totalWidth = contentWidth + getPaddingLeft() + getPaddingRight();
        }
        int totalHeight;
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            totalHeight = MeasureSpec.getSize(heightMeasureSpec);
        } else {
            float maxRadius =
                    Math.max(mDotRadius + mDotShadowRadius, mDotRadiusSelected + mDotShadowRadius);
            int contentHeight = (int) Math.ceil(maxRadius * 2);
            contentHeight = (int) (contentHeight + mDotShadowDy);
            totalHeight = contentHeight + getPaddingTop() + getPaddingBottom();
        }
        setMeasuredDimension(
                resolveSizeAndState(totalWidth, widthMeasureSpec, 0),
                resolveSizeAndState(totalHeight, heightMeasureSpec, 0));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mNumberOfPositions > 1) {
            float dotCenterLeft = getPaddingLeft() + (mDotSpacing / 2f);
            float dotCenterTop = getHeight() / 2f;
            canvas.save();
            canvas.translate(dotCenterLeft, dotCenterTop);
            for (int i = 0; i < mNumberOfPositions; i++) {
                if (i == mSelectedPosition) {
                    float radius = mDotRadiusSelected + mDotShadowRadius;
                    canvas.drawCircle(mDotShadowDx, mDotShadowDy, radius, mDotPaintShadowSelected);
                    canvas.drawCircle(0, 0, mDotRadiusSelected, mDotPaintSelected);
                } else {
                    float radius = mDotRadius + mDotShadowRadius;
                    canvas.drawCircle(mDotShadowDx, mDotShadowDy, radius, mDotPaintShadow);
                    canvas.drawCircle(0, 0, mDotRadius, mDotPaint);
                }
                canvas.translate(mDotSpacing, 0);
            }
            canvas.restore();
        }
    }

    /**
     * Notifies the view that the data set has changed.
     */
    public void notifyDataSetChanged() {
        if (mAdapter != null && mAdapter.getCount() > 0) {
            updateNumberOfPositions();
        }
    }
}
