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
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.VisibleForTesting;
import androidx.car.R;
import androidx.core.content.ContextCompat;

/** A custom view to provide list scroll behaviour -- up/down buttons and scroll indicator. */
public class PagedScrollBarView extends ViewGroup {
    private static final float BUTTON_DISABLED_ALPHA = 0.2f;
    private static final int SCROLL_TRANSLATION_ANIM_DURATION_MS = 200;

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

    private final int mSeparatingMargin;
    private final int mScrollBarThumbWidth;

    private boolean mShowScrollBarThumb;
    private boolean mAlphaJumpButtonEnabled;

    /** The amount of space that the scroll thumb is allowed to roam over. */
    private int mScrollThumbTrackHeight;

    private final Interpolator mPaginationInterpolator = new AccelerateDecelerateInterpolator();

    public PagedScrollBarView(Context context) {
        this(context, /* attrs= */ null, R.attr.pagedScrollBarViewStyle,
                R.style.Widget_Car_Scrollbar_Light);
    }

    public PagedScrollBarView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.pagedScrollBarViewStyle, R.style.Widget_Car_Scrollbar_Light);
    }

    public PagedScrollBarView(Context context, AttributeSet attrs, int defStyleAttrs) {
        this(context, attrs, defStyleAttrs, R.style.Widget_Car_Scrollbar_Light);
    }

    public PagedScrollBarView(
            Context context, AttributeSet attrs, int defStyleAttrs, int defStyleRes) {
        super(context, attrs, defStyleAttrs, defStyleRes);

        Resources res = context.getResources();
        mSeparatingMargin = res.getDimensionPixelSize(R.dimen.car_padding_4);
        mScrollBarThumbWidth = res.getDimensionPixelSize(R.dimen.car_scroll_bar_thumb_width);

        LayoutInflater.from(context).inflate(R.layout.car_paged_scrollbar_buttons,
                /* root= */ this, /* attachToRoot= */ true);

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

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PagedScrollBarView,
                defStyleAttrs, defStyleRes);

        mShowScrollBarThumb = a.getBoolean(R.styleable.PagedScrollBarView_showScrollBarThumb, true);
        setScrollbarThumbEnabled(mShowScrollBarThumb);
        Drawable upButtonIcon = a.getDrawable(R.styleable.PagedScrollBarView_upButtonIcon);
        if (upButtonIcon != null) {
            setUpButtonIcon(upButtonIcon);
        }

        Drawable downButtonIcon = a.getDrawable(R.styleable.PagedScrollBarView_downButtonIcon);
        if (downButtonIcon != null) {
            setDownButtonIcon(downButtonIcon);
        }

        int scrollBarColor = a.getResourceId(R.styleable.PagedScrollBarView_scrollBarColor, -1);
        if (scrollBarColor != -1) {
            setScrollbarThumbColor(scrollBarColor);
        }

        int buttonTintColor = a.getResourceId(R.styleable.PagedScrollBarView_buttonTintColor, -1);
        if (buttonTintColor != -1) {
            setButtonTintColor(buttonTintColor);
        }

        int buttonRippleBackground =
                a.getResourceId(R.styleable.PagedScrollBarView_buttonRippleBackground, -1);
        if (buttonRippleBackground != -1) {
            setButtonRippleBackground(buttonRippleBackground);
        }

        a.recycle();
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
        mAlphaJumpButtonEnabled = show;
        mAlphaJumpButton.setVisibility(show ? VISIBLE : GONE);
    }

    /** Returns {@code true} if the scroll bar thumb is visible */
    public boolean isScrollbarThumbEnabled() {
        return mShowScrollBarThumb;
    }
    /**
     * Sets whether or not the scroll bar thumb is visible, the default value is true.
     *
     * @param show {@code true} if the scroll bar thumb is visible.
     */
    public void setScrollbarThumbEnabled(boolean show) {
        mShowScrollBarThumb = show;
        mScrollThumb.setVisibility(mShowScrollBarThumb ? VISIBLE : GONE);
    }

    /**
     * Sets the range, offset and extent of the scroll bar. The range represents the size of a
     * container for the scrollbar thumb; offset is the distance from the start of the container
     * to where the thumb should be; and finally, extent is the size of the thumb.
     *
     * <p>These values can be expressed in arbitrary units, so long as they share the same units.
     * The values should also be positive.
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
    public void setParameters(
            @IntRange(from = 0) int range,
            @IntRange(from = 0) int offset,
            @IntRange(from = 0) int extent, boolean animate) {
        // Not laid out yet or not during layout process, so values cannot be calculated.
        if (!isLaidOut() || isInLayout()) {
            return;
        }
        // If the scroll bars aren't visible, then no need to update.
        if (getVisibility() == GONE || range == 0) {
            return;
        }

        int thumbLength = calculateScrollThumbLength(range, extent);

        // Sets the size of the thumb and request a redraw if needed.
        ViewGroup.LayoutParams lp = mScrollThumb.getLayoutParams();

        if (lp.height != thumbLength) {
            lp.height = thumbLength;
            mScrollThumb.requestLayout();
        }
        int thumbOffset = calculateScrollThumbOffset(range, offset, thumbLength);
        moveTranslationY(mScrollThumb, thumbOffset, animate);
    }

    /**
     * An optimized version of {@link #setParameters(int, int, int, boolean)} that is meant to be
     * called if a view is laying itself out. This method will avoid a complete remeasure of
     * the views in the {@code PagedScrollBarView} if the scroll thumb's height needs to be changed.
     * Instead, only the thumb itself will be remeasured and laid out.
     *
     * <p>These values can be expressed in arbitrary units, so long as they share the same units.
     *
     * @param range The range of the scrollbar's thumb
     * @param offset The offset of the scrollbar's thumb
     * @param extent The extent of the scrollbar's thumb
     *
     * @see #setParameters(int, int, int, boolean)
     */
    void setParametersInLayout(int range, int offset, int extent) {
        // If the scroll bars aren't visible, then no need to update.
        if (getVisibility() == GONE || range == 0) {
            return;
        }

        int thumbLength = calculateScrollThumbLength(range, extent);

        // Sets the size of the thumb and request a redraw if needed.
        ViewGroup.LayoutParams lp = mScrollThumb.getLayoutParams();

        if (lp.height != thumbLength) {
            lp.height = thumbLength;
            measureAndLayoutScrollThumb();
        }

        int thumbOffset = calculateScrollThumbOffset(range, offset, thumbLength);
        moveTranslationY(mScrollThumb, thumbOffset, /* animate= */ false);
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
     * @param color Resource identifier of the color.
     */
    public void setScrollbarThumbColor(@ColorRes int color) {
        GradientDrawable background = (GradientDrawable) mScrollThumb.getBackground();
        background.setColor(getContext().getColor(color));
    }

    /**
     * Sets the tint color for the up and down buttons of this view.
     *
     * @param tintResId Resource identifier of the tint color.
     */
    public void setButtonTintColor(@ColorRes int tintResId) {
        int tint = ContextCompat.getColor(getContext(), tintResId);
        mUpButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
        mDownButton.setColorFilter(tint, PorterDuff.Mode.SRC_IN);
    }

    /**
     * Sets the drawable that will function as the background for the buttons in this view. This
     * background should provide the ripple.
     *
     * @param backgroundResId The drawable resource identifier for the ripple background.
     */
    public void setButtonRippleBackground(@DrawableRes int backgroundResId) {
        mUpButton.setBackgroundResource(backgroundResId);
        mDownButton.setBackgroundResource(backgroundResId);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int requestedWidth = MeasureSpec.getSize(widthMeasureSpec);
        int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);

        int wrapMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        mUpButton.measure(wrapMeasureSpec, wrapMeasureSpec);
        mDownButton.measure(wrapMeasureSpec, wrapMeasureSpec);

        // A seperating margin is needed between up and down button.
        int currentHeight = getPaddingTop() + mUpButton.getMeasuredHeight() + mSeparatingMargin
                + mDownButton.getMeasuredHeight() +  getPaddingBottom();

        // Check if there is enough room to draw buttons
        if (currentHeight > requestedHeight) {
            mUpButton.setVisibility(GONE);
            mDownButton.setVisibility(GONE);
        } else {
            mUpButton.setVisibility(VISIBLE);
            mDownButton.setVisibility(VISIBLE);
        }

        if (mShowScrollBarThumb) {
            measureScrollThumb();
            currentHeight += mScrollThumb.getMeasuredHeight() + mSeparatingMargin;
            // Check if there is enough room to draw thumb
            mScrollThumb.setVisibility(currentHeight > requestedHeight ? GONE : VISIBLE);
        }

        if (mAlphaJumpButtonEnabled) {
            mAlphaJumpButton.measure(wrapMeasureSpec, wrapMeasureSpec);
            currentHeight += mAlphaJumpButton.getMeasuredHeight() + mSeparatingMargin;
            // Check if there is enough room to draw alpha jump button
            mAlphaJumpButton.setVisibility(currentHeight > requestedHeight ? GONE : VISIBLE);
        }

        setMeasuredDimension(requestedWidth, requestedHeight);
    }

    @Override
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        // When up and down button are GONE, this implies there is not enough room to layout the
        // rest of the views.
        if (mUpButton.getVisibility() == GONE) {
            return;
        }
        int width = right - left;
        int height = bottom - top;

        // This value will keep track of the top of the current view being laid out.
        int layoutTop = getPaddingTop();

        // Lay out the up button at the top of the view.
        layoutViewCenteredFromTop(mUpButton, layoutTop, width);
        layoutTop = mUpButton.getBottom();

        if (mAlphaJumpButton.getVisibility() != GONE) {
            // Lay out the alpha jump button if it exists. This button goes below the up button.
            layoutTop += mSeparatingMargin;

            layoutViewCenteredFromTop(mAlphaJumpButton, layoutTop, width);

            layoutTop = mAlphaJumpButton.getBottom();
        }

        if (mScrollThumb.getVisibility() != GONE) {
            // Lay out the scroll thumb
            layoutTop += mSeparatingMargin;
            layoutViewCenteredFromTop(mScrollThumb, layoutTop, width);
        }

        // Lay out the bottom button at the bottom of the view.
        int downBottom = height - getPaddingBottom();
        layoutViewCenteredFromBottom(mDownButton, downBottom, width);

        calculateScrollThumbTrackHeight();
    }

    /**
     * Calculate the amount of space that the scroll bar thumb is allowed to roam. The thumb
     * is allowed to take up the space between the down bottom and the up or alpha jump
     * button, depending on if the latter is visible.
     */
    private void calculateScrollThumbTrackHeight() {
        // Subtracting (2 * mSeparatingMargin) for the top/bottom margin above and below the
        // scroll bar thumb.
        mScrollThumbTrackHeight = mDownButton.getTop() - (2 * mSeparatingMargin);

        // If there's an alpha jump button, then the thumb is laid out starting from below that.
        if (mAlphaJumpButton.getVisibility() != GONE) {
            mScrollThumbTrackHeight -= mAlphaJumpButton.getBottom();
        } else {
            mScrollThumbTrackHeight -= mUpButton.getBottom();
        }
        mScrollThumbTrackHeight = Math.max(0, mScrollThumbTrackHeight);
    }

    private void measureScrollThumb() {
        int scrollWidth = MeasureSpec.makeMeasureSpec(mScrollBarThumbWidth, MeasureSpec.EXACTLY);
        int scrollHeight = MeasureSpec.makeMeasureSpec(
                mScrollThumb.getLayoutParams().height,
                MeasureSpec.EXACTLY);
        mScrollThumb.measure(scrollWidth, scrollHeight);
    }

    /**
     * An optimization method to only remeasure and lay out the scroll thumb. This method should be
     * used when the height of the thumb has changed, but no other views need to be remeasured.
     */
    private void measureAndLayoutScrollThumb() {
        measureScrollThumb();

        // The top value should not change from what it was before; only the height is assumed to
        // be changing.
        int layoutTop = mScrollThumb.getTop();
        layoutViewCenteredFromTop(mScrollThumb, layoutTop, getMeasuredWidth());
    }

    /**
     * Lays out the given View starting from the given {@code top} value downwards and centered
     * within the given {@code availableWidth}.
     *
     * @param  view The view to lay out.
     * @param  top The top value to start laying out from. This value will be the resulting top
     *             value of the view.
     * @param  availableWidth The width in which to center the given view.
     */
    private void layoutViewCenteredFromTop(View view, int top, int availableWidth) {
        int viewWidth = view.getMeasuredWidth();
        int viewLeft = (availableWidth - viewWidth) / 2;
        view.layout(viewLeft, top, viewLeft + viewWidth,
                top + view.getMeasuredHeight());
    }

    /**
     * Lays out the given View starting from the given {@code bottom} value upwards and centered
     * within the given {@code availableSpace}.
     *
     * @param  view The view to lay out.
     * @param  bottom The bottom value to start laying out from. This value will be the resulting
     *                bottom value of the view.
     * @param  availableWidth The width in which to center the given view.
     */
    private void layoutViewCenteredFromBottom(View view, int bottom, int availableWidth) {
        int viewWidth = view.getMeasuredWidth();
        int viewLeft = (availableWidth - viewWidth) / 2;
        view.layout(viewLeft, bottom - view.getMeasuredHeight(),
                viewLeft + viewWidth, bottom);
    }

    @VisibleForTesting
    int getScrollbarThumbColor() {
        return ((GradientDrawable) mScrollThumb.getBackground()).getColor().getDefaultColor();
    }

    /**
     * Calculates and returns how big the scroll bar thumb should be based on the given range and
     * extent.
     *
     * @param range The total amount of space the scroll bar is allowed to roam over.
     * @param extent The amount of space that the scroll bar takes up relative to the range.
     * @return The height of the scroll bar thumb in pixels.
     */
    private int calculateScrollThumbLength(int range, int extent) {
        // Scale the length by the available space that the thumb can fill.
        return Math.round(((float) extent / range) * mScrollThumbTrackHeight);
    }

    /**
     * Calculates and returns how much the scroll thumb should be offset from the top of where it
     * has been laid out.
     *
     * @param  range The total amount of space the scroll bar is allowed to roam over.
     * @param  offset The amount the scroll bar should be offset, expressed in the same units as
     *                the given range.
     * @param  thumbLength The current length of the thumb in pixels.
     * @return The amount the thumb should be offset from its current to position in pixels.
     */
    private int calculateScrollThumbOffset(int range, int offset, int thumbLength) {
        // Ensure that if the user has reached the bottom of the list, then the scroll bar is
        // aligned to the bottom as well. Otherwise, scale the offset appropriately.
        return isDownEnabled()
                ? Math.round(((float) offset / range) * mScrollThumbTrackHeight)
                : mScrollThumbTrackHeight - thumbLength;
    }

    /** Moves the given view's translationY to the specified position. */
    private void moveTranslationY(View view, float translationY, boolean animate) {
        int duration = animate ? SCROLL_TRANSLATION_ANIM_DURATION_MS : 0;
        view.animate()
                .translationY(translationY)
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

        AlphaJumpButtonClickListener() {
        }

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
