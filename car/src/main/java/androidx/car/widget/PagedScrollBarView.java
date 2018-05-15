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
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.VisibleForTesting;
import androidx.car.R;
import androidx.core.content.ContextCompat;

/** A custom view to provide list scroll behaviour -- up/down buttons and scroll indicator. */
public class PagedScrollBarView extends FrameLayout {
    private static final float BUTTON_DISABLED_ALPHA = 0.2f;

    @DayNightStyle private int mDayNightStyle;

    /** Listener for when the list should paginate. */
    public interface PaginationListener {
        int PAGE_UP = 0;
        int PAGE_DOWN = 1;

        /** Called when the linked view should be paged in the given direction */
        void onPaginate(int direction);

        /**
         * Called when the 'alpha jump' button is clicked and the linked view should switch into
         * alpha jump mode, where we display a list of buttons to allow the user to quickly scroll
         * to a certain point in the list, bypassing a lot of manual scrolling.
         */
        void onAlphaJump();
    }

    private final ImageView mUpButton;
    private final PaginateButtonClickListener mUpButtonClickListener;
    private final ImageView mDownButton;
    private final PaginateButtonClickListener mDownButtonClickListener;
    private final TextView mAlphaJumpButton;
    private final AlphaJumpButtonClickListener mAlphaJumpButtonClickListener;
    private final View mScrollThumb;
    /** The "filler" view between the up and down buttons */
    private final View mFiller;

    private final Interpolator mPaginationInterpolator = new AccelerateDecelerateInterpolator();
    private boolean mUseCustomThumbBackground;
    @ColorRes private int mCustomThumbBackgroundResId;
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

        mUpButtonClickListener = new PaginateButtonClickListener(PaginationListener.PAGE_UP);
        mDownButtonClickListener = new PaginateButtonClickListener(PaginationListener.PAGE_DOWN);
        mAlphaJumpButtonClickListener = new AlphaJumpButtonClickListener();

        mUpButton = findViewById(R.id.page_up);
        mUpButton.setOnClickListener(mUpButtonClickListener);
        mDownButton = findViewById(R.id.page_down);
        mDownButton.setOnClickListener(mDownButtonClickListener);
        mAlphaJumpButton = findViewById(R.id.alpha_jump);
        mAlphaJumpButton.setOnClickListener(mAlphaJumpButtonClickListener);

        mScrollThumb = findViewById(R.id.scrollbar_thumb);
        mFiller = findViewById(R.id.filler);
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
        mUpButtonClickListener.setPaginationListener(listener);
        mDownButtonClickListener.setPaginationListener(listener);
        mAlphaJumpButtonClickListener.setPaginationListener(listener);
    }

    /** Returns {@code true} if the "up" button is pressed */
    public boolean isUpPressed() {
        return mUpButton.isPressed();
    }

    /** Returns {@code true} if the "down" button is pressed */
    public boolean isDownPressed() {
        return mDownButton.isPressed();
    }

    void setShowAlphaJump(boolean show) {
        mAlphaJumpButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * Sets the range, offset and extent of the scroll bar. The range represents the size of a
     * container for the scrollbar thumb; offset is the distance from the start of the container
     * to where the thumb should be; and finally, extent is the size of the thumb.
     *
     * <p>These values can be expressed in arbitrary units, so long as they share the same units.
     *
     * @param range The range of the scrollbar's thumb
     * @param offset The offset of the scrollbar's thumb
     * @param extent The extent of the scrollbar's thumb
     * @param animate Whether or not the thumb should animate from its current position to the
     *                position specified by the given range, offset and extent.
     *
     * @see View#computeVerticalScrollRange()
     * @see View#computeVerticalScrollOffset()
     * @see View#computeVerticalScrollExtent()
     */
    public void setParameters(int range, int offset, int extent, boolean animate) {
        // If the scroll bars aren't visible, then no need to update.
        if (getVisibility() == View.GONE || range == 0) {
            return;
        }

        int availableSpace = mFiller.getHeight() - mFiller.getPaddingTop()
                - mFiller.getPaddingBottom();

        // Scale the length by the available space that the thumb can fill.
        int thumbLength = Math.round(((float) extent / range) * availableSpace);

        // Ensure that if the user has reached the bottom of the list, then the scroll bar is
        // aligned to the bottom as well. Otherwise, scale the offset appropriately.
        int thumbOffset = isDownEnabled()
                ? Math.round(((float) offset / range) * availableSpace)
                : availableSpace - thumbLength;

        // Sets the size of the thumb and request a redraw if needed.
        ViewGroup.LayoutParams lp = mScrollThumb.getLayoutParams();

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

    /**
     * Sets the color of thumb.
     *
     * <p>Custom thumb color ignores {@link DayNightStyle}. Calling {@link #resetThumbColor} resets
     * to default color.
     *
     * @param color Resource identifier of the color.
     */
    public void setThumbColor(@ColorRes int color) {
        mUseCustomThumbBackground = true;
        mCustomThumbBackgroundResId = color;
        reloadColors();
    }

    /**
     * Resets the color of thumb to default.
     */
    public void resetThumbColor() {
        mUseCustomThumbBackground = false;
        reloadColors();
    }

    /** Reload the colors for the current {@link DayNightStyle}. */
    @SuppressWarnings("deprecation")
    private void reloadColors() {
        int tintResId;
        int thumbColorResId;
        int upDownBackgroundResId;

        switch (mDayNightStyle) {
            case DayNightStyle.AUTO:
                tintResId = R.color.car_tint;
                thumbColorResId = R.color.car_scrollbar_thumb;
                upDownBackgroundResId = R.drawable.car_button_ripple_background;
                break;
            case DayNightStyle.AUTO_INVERSE:
                tintResId = R.color.car_tint_inverse;
                thumbColorResId = R.color.car_scrollbar_thumb_inverse;
                upDownBackgroundResId = R.drawable.car_button_ripple_background_inverse;
                break;
            case DayNightStyle.FORCE_NIGHT:
            case DayNightStyle.ALWAYS_LIGHT:
                tintResId = R.color.car_tint_light;
                thumbColorResId = R.color.car_scrollbar_thumb_light;
                upDownBackgroundResId = R.drawable.car_button_ripple_background_night;
                break;
            case DayNightStyle.FORCE_DAY:
            case DayNightStyle.ALWAYS_DARK:
                tintResId = R.color.car_tint_dark;
                thumbColorResId = R.color.car_scrollbar_thumb_dark;
                upDownBackgroundResId = R.drawable.car_button_ripple_background_day;
                break;
            default:
                throw new IllegalArgumentException("Unknown DayNightStyle: " + mDayNightStyle);
        }

        if (mUseCustomThumbBackground) {
            thumbColorResId = mCustomThumbBackgroundResId;
        }

        setScrollbarThumbColor(thumbColorResId);

        int tint = ContextCompat.getColor(getContext(), tintResId);
        mUpButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        mUpButton.setBackgroundResource(upDownBackgroundResId);

        mDownButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        mDownButton.setBackgroundResource(upDownBackgroundResId);

        mAlphaJumpButton.setBackgroundResource(upDownBackgroundResId);
    }

    private void setScrollbarThumbColor(@ColorRes int color) {
        GradientDrawable background = (GradientDrawable) mScrollThumb.getBackground();
        background.setColor(getContext().getColor(color));
    }

    @VisibleForTesting
    int getScrollbarThumbColor() {
        return ((GradientDrawable) mScrollThumb.getBackground()).getColor().getDefaultColor();
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

    private static class PaginateButtonClickListener implements View.OnClickListener {
        private final int mPaginateDirection;
        private PaginationListener mPaginationListener;

        PaginateButtonClickListener(int paginateDirection) {
            mPaginateDirection = paginateDirection;
        }

        public void setPaginationListener(PaginationListener listener) {
            mPaginationListener = listener;
        }

        @Override
        public void onClick(View v) {
            if (mPaginationListener != null) {
                mPaginationListener.onPaginate(mPaginateDirection);
            }
        }
    }

    private static class AlphaJumpButtonClickListener implements View.OnClickListener {
        private PaginationListener mPaginationListener;

        public void setPaginationListener(PaginationListener listener) {
            mPaginationListener = listener;
        }

        @Override
        public void onClick(View v) {
            if (mPaginationListener != null) {
                mPaginationListener.onAlphaJump();
            }
        }

    }
}
