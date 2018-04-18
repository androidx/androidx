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

package androidx.wear.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;
import androidx.wear.R;

/**
 * {@link CircularProgressLayout} adds a circular countdown timer behind the view it contains,
 * typically used to automatically confirm an operation after a short delay has elapsed.
 *
 * <p>The developer can specify a countdown interval via {@link #setTotalTime(long)} and a listener
 * via {@link #setOnTimerFinishedListener(OnTimerFinishedListener)} to be called when the time has
 * elapsed after {@link #startTimer()} has been called. Tap action can be received via {@link
 * #setOnClickListener(OnClickListener)} and can be used to cancel the timer via {@link
 * #stopTimer()} method.
 *
 * <p>Alternatively, this layout can be used to show indeterminate progress by calling {@link
 * #setIndeterminate(boolean)} method.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
public class CircularProgressLayout extends FrameLayout {

    /**
     * Update interval for 60 fps.
     */
    private static final long DEFAULT_UPDATE_INTERVAL = 1000 / 60;

    /**
     * Starting rotation for the progress indicator. Geometric clockwise [0..360] degree range
     * correspond to [0..1] range. 0.75 corresponds to 12 o'clock direction on a watch.
     */
    private static final float DEFAULT_ROTATION = 0.75f;

    /**
     * Used as background of this layout.
     */
    private CircularProgressDrawable mProgressDrawable;

    /**
     * Used to control this layout.
     */
    private CircularProgressLayoutController mController;

    /**
     * Angle for the progress to start from.
     */
    private float mStartingRotation = DEFAULT_ROTATION;

    /**
     * Duration of the timer in milliseconds.
     */
    private long mTotalTime;


    /**
     * Interface to implement for listening to {@link
     * OnTimerFinishedListener#onTimerFinished(CircularProgressLayout)} event.
     */
    public interface OnTimerFinishedListener {

        /**
         * Called when the timer started by {@link #startTimer()} method finishes.
         *
         * @param layout {@link CircularProgressLayout} that calls this method.
         */
        void onTimerFinished(CircularProgressLayout layout);
    }

    public CircularProgressLayout(Context context) {
        this(context, null);
    }

    public CircularProgressLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularProgressLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CircularProgressLayout(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mProgressDrawable = new CircularProgressDrawable(context);
        mProgressDrawable.setProgressRotation(DEFAULT_ROTATION);
        mProgressDrawable.setStrokeCap(Paint.Cap.BUTT);
        setBackground(mProgressDrawable);

        // If a child view is added, make it center aligned so it fits in the progress drawable.
        setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {
                // Ensure that child view is aligned in center
                LayoutParams params = (LayoutParams) child.getLayoutParams();
                params.gravity = Gravity.CENTER;
                child.setLayoutParams(params);
            }

            @Override
            public void onChildViewRemoved(View parent, View child) {

            }
        });

        mController = new CircularProgressLayoutController(this);

        Resources r = context.getResources();
        TypedArray a = r.obtainAttributes(attrs, R.styleable.CircularProgressLayout);

        if (a.getType(R.styleable.CircularProgressLayout_colorSchemeColors) == TypedValue
                .TYPE_REFERENCE || !a.hasValue(
                R.styleable.CircularProgressLayout_colorSchemeColors)) {
            int arrayResId = a.getResourceId(R.styleable.CircularProgressLayout_colorSchemeColors,
                    R.array.circular_progress_layout_color_scheme_colors);
            setColorSchemeColors(getColorListFromResources(r, arrayResId));
        } else {
            setColorSchemeColors(a.getColor(R.styleable.CircularProgressLayout_colorSchemeColors,
                    Color.BLACK));
        }

        setStrokeWidth(a.getDimensionPixelSize(R.styleable.CircularProgressLayout_strokeWidth,
                r.getDimensionPixelSize(
                        R.dimen.circular_progress_layout_stroke_width)));

        setBackgroundColor(a.getColor(R.styleable.CircularProgressLayout_backgroundColor,
                ContextCompat.getColor(context,
                        R.color.circular_progress_layout_background_color)));

        setIndeterminate(a.getBoolean(R.styleable.CircularProgressLayout_indeterminate, false));

        a.recycle();
    }

    private int[] getColorListFromResources(Resources resources, int arrayResId) {
        TypedArray colorArray = resources.obtainTypedArray(arrayResId);
        int[] colors = new int[colorArray.length()];
        for (int i = 0; i < colorArray.length(); i++) {
            colors[i] = colorArray.getColor(i, 0);
        }
        colorArray.recycle();
        return colors;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getChildCount() != 0) {
            View childView = getChildAt(0);
            // Wrap the drawable around the child view
            mProgressDrawable.setCenterRadius(
                    Math.min(childView.getWidth(), childView.getHeight()) / 2f);
        } else {
            // Fill the bounds if no child view is present
            mProgressDrawable.setCenterRadius(0f);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mController.reset();
    }

    /**
     * Sets the background color of the {@link CircularProgressDrawable}, which is drawn as a circle
     * inside the progress drawable. Colors are in ARGB format defined in {@link Color}.
     *
     * @param color an ARGB color
     */
    @Override
    public void setBackgroundColor(@ColorInt int color) {
        mProgressDrawable.setBackgroundColor(color);
    }

    /**
     * Returns the background color of the {@link CircularProgressDrawable}.
     *
     * @return an ARGB color
     */
    @ColorInt
    public int getBackgroundColor() {
        return mProgressDrawable.getBackgroundColor();
    }

    /**
     * Returns the {@link CircularProgressDrawable} used as background of this layout.
     *
     * @return {@link CircularProgressDrawable}
     */
    @NonNull
    public CircularProgressDrawable getProgressDrawable() {
        return mProgressDrawable;
    }

    /**
     * Sets if progress should be shown as an indeterminate spinner.
     *
     * @param indeterminate {@code true} if indeterminate spinner should be shown, {@code false}
     *                      otherwise.
     */
    public void setIndeterminate(boolean indeterminate) {
        mController.setIndeterminate(indeterminate);
    }

    /**
     * Returns if progress is showing as an indeterminate spinner.
     *
     * @return {@code true} if indeterminate spinner is shown, {@code false} otherwise.
     */
    public boolean isIndeterminate() {
        return mController.isIndeterminate();
    }

    /**
     * Sets the total time in milliseconds for the timer to countdown to. Calling this method while
     * the timer is already running will not change the duration of the current timer.
     *
     * @param totalTime total time in milliseconds
     */
    public void setTotalTime(long totalTime) {
        if (totalTime <= 0) {
            throw new IllegalArgumentException("Total time should be greater than zero.");
        }
        mTotalTime = totalTime;
    }

    /**
     * Returns the total time in milliseconds for the timer to countdown to.
     *
     * @return total time in milliseconds
     */
    public long getTotalTime() {
        return mTotalTime;
    }

    /**
     * Starts the timer countdown. Once the countdown is finished, if there is an {@link
     * OnTimerFinishedListener} registered by {@link
     * #setOnTimerFinishedListener(OnTimerFinishedListener)} method, its
     * {@link OnTimerFinishedListener#onTimerFinished(CircularProgressLayout)} method is called. If
     * this method is called while there is already a running timer, it will restart the timer.
     */
    public void startTimer() {
        mController.startTimer(mTotalTime, DEFAULT_UPDATE_INTERVAL);
        mProgressDrawable.setProgressRotation(mStartingRotation);
    }

    /**
     * Stops the timer countdown. If there is no timer running, calling this method will not do
     * anything.
     */
    public void stopTimer() {
        mController.stopTimer();
    }

    /**
     * Returns if the timer is running.
     *
     * @return {@code true} if the timer is running, {@code false} otherwise
     */
    public boolean isTimerRunning() {
        return mController.isTimerRunning();
    }

    /**
     * Sets the starting rotation for the progress drawable to start from. Default starting rotation
     * is {@code 0.75} and it corresponds clockwise geometric 270 degrees (12 o'clock on a watch)
     *
     * @param rotation starting rotation from [0..1]
     */
    public void setStartingRotation(float rotation) {
        mStartingRotation = rotation;
    }

    /**
     * Returns the starting rotation of the progress drawable.
     *
     * @return starting rotation from [0..1]
     */
    public float getStartingRotation() {
        return mStartingRotation;
    }

    /**
     * Sets the stroke width of the progress drawable in pixels.
     *
     * @param strokeWidth stroke width in pixels
     */
    public void setStrokeWidth(float strokeWidth) {
        mProgressDrawable.setStrokeWidth(strokeWidth);
    }

    /**
     * Returns the stroke width of the progress drawable in pixels.
     *
     * @return stroke width in pixels
     */
    public float getStrokeWidth() {
        return mProgressDrawable.getStrokeWidth();
    }

    /**
     * Sets the color scheme colors of the progress drawable, which is equivalent to calling {@link
     * CircularProgressDrawable#setColorSchemeColors(int...)} method on background drawable of this
     * layout.
     *
     * @param colors list of ARGB colors
     */
    public void setColorSchemeColors(int... colors) {
        mProgressDrawable.setColorSchemeColors(colors);
    }

    /**
     * Returns the color scheme colors of the progress drawable
     *
     * @return list of ARGB colors
     */
    public int[] getColorSchemeColors() {
        return mProgressDrawable.getColorSchemeColors();
    }

    /**
     * Returns the {@link OnTimerFinishedListener} that is registered to this layout.
     *
     * @return registered {@link OnTimerFinishedListener}
     */
    @Nullable
    public OnTimerFinishedListener getOnTimerFinishedListener() {
        return mController.getOnTimerFinishedListener();
    }

    /**
     * Sets the {@link OnTimerFinishedListener} to be notified when timer countdown is finished.
     *
     * @param listener {@link OnTimerFinishedListener} to be notified, or {@code null} to clear
     */
    public void setOnTimerFinishedListener(@Nullable OnTimerFinishedListener listener) {
        mController.setOnTimerFinishedListener(listener);
    }
}
