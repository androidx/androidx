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

package androidx.car.widget;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.car.R;

/** A custom view to provide list scroll behaviour -- up/down buttons and scroll indicator. */
public class PagedScrollBarView extends FrameLayout
        implements View.OnClickListener, View.OnLongClickListener {
    private static final float BUTTON_DISABLED_ALPHA = 0.2f;

    @DayNightStyle private int mDayNightStyle;

    /** Listener for when the list should paginate. */
    public interface PaginationListener {
        int PAGE_UP = 0;
        int PAGE_DOWN = 1;

        /** Called when the linked view should be paged in the given direction */
        void onPaginate(int direction);
    }

    private final ImageView mUpButton;
    private final ImageView mDownButton;
    private final ImageView mScrollThumb;
    /** The "filler" view between the up and down buttons */
    private final View mFiller;

    private final Interpolator mPaginationInterpolator = new AccelerateDecelerateInterpolator();
    private final int mMinThumbLength;
    private final int mMaxThumbLength;
    private PaginationListener mPaginationListener;

    public PagedScrollBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0 /*defStyleAttrs*/, 0 /*defStyleRes*/);
    }

    public PagedScrollBarView(Context context, AttributeSet attrs, int defStyleAttrs) {
        this(context, attrs, defStyleAttrs, 0 /*defStyleRes*/);
    }

    public PagedScrollBarView(
            Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.car_paged_scrollbar_buttons, this /* root */,
                true /* attachToRoot */);

        mUpButton = (ImageView) findViewById(R.id.page_up);
        mUpButton.setOnClickListener(this);
        mUpButton.setOnLongClickListener(this);
        mDownButton = (ImageView) findViewById(R.id.page_down);
        mDownButton.setOnClickListener(this);
        mDownButton.setOnLongClickListener(this);

        mScrollThumb = (ImageView) findViewById(R.id.scrollbar_thumb);
        mFiller = findViewById(R.id.filler);

        mMinThumbLength = getResources().getDimensionPixelSize(R.dimen.min_thumb_height);
        mMaxThumbLength = getResources().getDimensionPixelSize(R.dimen.max_thumb_height);
    }

    @Override
    public void onClick(View v) {
        dispatchPageClick(v);
    }

    @Override
    public boolean onLongClick(View v) {
        dispatchPageClick(v);
        return true;
    }

    /** Sets the icon to be used for the up button. */
    public void setUpButtonIcon(Drawable icon) {
        mUpButton.setImageDrawable(icon);
    }

    /** Sets the icon to be used for the down button. */
    public void setDownButtonIcon(Drawable icon) {
        mDownButton.setImageDrawable(icon);
    }

    /**
     * Sets the listener that will be notified when the up and down buttons have been pressed.
     *
     * @param listener The listener to set.
     */
    public void setPaginationListener(PaginationListener listener) {
        mPaginationListener = listener;
    }

    /** Returns {@code true} if the "up" button is pressed */
    public boolean isUpPressed() {
        return mUpButton.isPressed();
    }

    /** Returns {@code true} if the "down" button is pressed */
    public boolean isDownPressed() {
        return mDownButton.isPressed();
    }

    /** Sets the range, offset and extent of the scroll bar. See {@link View}. */
    public void setParameters(int range, int offset, int extent, boolean animate) {
        // This method is where we take the computed parameters from the PagedLayoutManager and
        // render it within the specified constraints ({@link #mMaxThumbLength} and
        // {@link #mMinThumbLength}).
        final int size = mFiller.getHeight() - mFiller.getPaddingTop() - mFiller.getPaddingBottom();

        int thumbLength = extent * size / range;
        thumbLength = Math.max(Math.min(thumbLength, mMaxThumbLength), mMinThumbLength);

        int thumbOffset = size - thumbLength;
        if (isDownEnabled()) {
            // We need to adjust the offset so that it fits into the possible space inside the
            // filler with regarding to the constraints set by mMaxThumbLength and mMinThumbLength.
            thumbOffset = (size - thumbLength) * offset / range;
        }

        // Sets the size of the thumb and request a redraw if needed.
        final ViewGroup.LayoutParams lp = mScrollThumb.getLayoutParams();
        if (lp.height != thumbLength) {
            lp.height = thumbLength;
            mScrollThumb.requestLayout();
        }

        moveY(mScrollThumb, thumbOffset, animate);
    }

    /**
     * Sets how this {@link PagedScrollBarView} responds to day/night configuration changes. By
     * default, the PagedScrollBarView is darker in the day and lighter at night.
     *
     * @param dayNightStyle A value from {@link DayNightStyle}.
     * @see DayNightStyle
     */
    public void setDayNightStyle(@DayNightStyle int dayNightStyle) {
        mDayNightStyle = dayNightStyle;
        reloadColors();
    }

    /**
     * Sets whether or not the up button on the scroll bar is clickable.
     *
     * @param enabled {@code true} if the up button is enabled.
     */
    public void setUpEnabled(boolean enabled) {
        mUpButton.setEnabled(enabled);
        mUpButton.setAlpha(enabled ? 1f : BUTTON_DISABLED_ALPHA);
    }

    /**
     * Sets whether or not the down button on the scroll bar is clickable.
     *
     * @param enabled {@code true} if the down button is enabled.
     */
    public void setDownEnabled(boolean enabled) {
        mDownButton.setEnabled(enabled);
        mDownButton.setAlpha(enabled ? 1f : BUTTON_DISABLED_ALPHA);
    }

    /**
     * Returns whether or not the down button on the scroll bar is clickable.
     *
     * @return {@code true} if the down button is enabled. {@code false} otherwise.
     */
    public boolean isDownEnabled() {
        return mDownButton.isEnabled();
    }

    /** Reload the colors for the current {@link DayNightStyle}. */
    private void reloadColors() {
        int tint;
        int thumbBackground;
        int upDownBackgroundResId;

        switch (mDayNightStyle) {
            case DayNightStyle.AUTO:
                tint = ContextCompat.getColor(getContext(), R.color.car_tint);
                thumbBackground = ContextCompat.getColor(getContext(),
                        R.color.car_scrollbar_thumb);
                upDownBackgroundResId = R.drawable.car_pagination_background;
                break;
            case DayNightStyle.AUTO_INVERSE:
                tint = ContextCompat.getColor(getContext(), R.color.car_tint_inverse);
                thumbBackground = ContextCompat.getColor(getContext(),
                        R.color.car_scrollbar_thumb_inverse);
                upDownBackgroundResId = R.drawable.car_pagination_background_inverse;
                break;
            case DayNightStyle.FORCE_NIGHT:
                tint = ContextCompat.getColor(getContext(), R.color.car_tint_light);
                thumbBackground = ContextCompat.getColor(getContext(),
                        R.color.car_scrollbar_thumb_light);
                upDownBackgroundResId = R.drawable.car_pagination_background_night;
                break;
            case DayNightStyle.FORCE_DAY:
                tint = ContextCompat.getColor(getContext(), R.color.car_tint_dark);
                thumbBackground = ContextCompat.getColor(getContext(),
                        R.color.car_scrollbar_thumb_dark);
                upDownBackgroundResId = R.drawable.car_pagination_background_day;
                break;
            default:
                throw new IllegalArgumentException("Unknown DayNightStyle: " + mDayNightStyle);
        }

        mScrollThumb.setBackgroundColor(thumbBackground);

        mUpButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        mUpButton.setBackgroundResource(upDownBackgroundResId);

        mDownButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        mDownButton.setBackgroundResource(upDownBackgroundResId);
    }

    private void dispatchPageClick(View v) {
        final PaginationListener listener = mPaginationListener;
        if (listener == null) {
            return;
        }

        int direction = v.getId() == R.id.page_up
                ? PaginationListener.PAGE_UP
                : PaginationListener.PAGE_DOWN;
        listener.onPaginate(direction);
    }

    /** Moves the given view to the specified 'y' position. */
    private void moveY(final View view, float newPosition, boolean animate) {
        final int duration = animate ? 200 : 0;
        view.animate()
                .y(newPosition)
                .setDuration(duration)
                .setInterpolator(mPaginationInterpolator)
                .start();
    }
}
