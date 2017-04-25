/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.wear.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

/**
 * Wearable specific implementation of the {@link RecyclerView} enabling {@link
 * #setCircularScrollingGestureEnabled(boolean)} circular scrolling} and semi-circular layouts.
 *
 * @see #setCircularScrollingGestureEnabled(boolean)
 */
@TargetApi(Build.VERSION_CODES.M)
public class WearableRecyclerView extends RecyclerView {
    private static final String TAG = "WearableRecyclerView";

    private static final int NO_VALUE = Integer.MIN_VALUE;

    private final ScrollManager mScrollManager = new ScrollManager();
    private boolean mCircularScrollingEnabled;
    private boolean mEdgeItemsCenteringEnabled;
    private boolean mCenterEdgeItemsWhenThereAreChildren;

    private int mOriginalPaddingTop = NO_VALUE;
    private int mOriginalPaddingBottom = NO_VALUE;

    /** Pre-draw listener which is used to adjust the padding on this view before its first draw. */
    private final ViewTreeObserver.OnPreDrawListener mPaddingPreDrawListener =
            new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (mCenterEdgeItemsWhenThereAreChildren && getChildCount() > 0) {
                        setupCenteredPadding();
                        mCenterEdgeItemsWhenThereAreChildren = false;
                    }
                    return true;
                }
            };

    public WearableRecyclerView(Context context) {
        this(context, null);
    }

    public WearableRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WearableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        this(context, attrs, defStyle, 0);
    }

    public WearableRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle,
            int defStyleRes) {
        super(context, attrs, defStyle);

        setHasFixedSize(true);
        // Padding is used to center the top and bottom items in the list, don't clip to padding to
        // allows the items to draw in that space.
        setClipToPadding(false);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.WearableRecyclerView,
                    defStyle, defStyleRes);

            setCircularScrollingGestureEnabled(
                    a.getBoolean(
                            R.styleable.WearableRecyclerView_circularScrollingGestureEnabled,
                            mCircularScrollingEnabled));
            setBezelFraction(
                    a.getFloat(R.styleable.WearableRecyclerView_bezelWidth,
                            mScrollManager.getBezelWidth()));
            setScrollDegreesPerScreen(
                    a.getFloat(
                            R.styleable.WearableRecyclerView_scrollDegreesPerScreen,
                            mScrollManager.getScrollDegreesPerScreen()));
            a.recycle();
        }
    }

    private void setupCenteredPadding() {
        if (getChildCount() < 1 || !mEdgeItemsCenteringEnabled) {
            return;
        }
        // All the children in the view are the same size, as we set setHasFixedSize
        // to true, so the height of the first child is the same as all of them.
        View child = getChildAt(0);
        int height = child.getHeight();
        // This is enough padding to center the child view in the parent.
        int desiredPadding = (int) ((getHeight() * 0.5f) - (height * 0.5f));

        if (getPaddingTop() != desiredPadding) {
            mOriginalPaddingTop = getPaddingTop();
            mOriginalPaddingBottom = getPaddingBottom();
            // The view is symmetric along the vertical axis, so the top and bottom
            // can be the same.
            setPadding(getPaddingLeft(), desiredPadding, getPaddingRight(), desiredPadding);

            // The focused child should be in the center, so force a scroll to it.
            View focusedChild = getFocusedChild();
            int focusedPosition =
                    (focusedChild != null) ? getLayoutManager().getPosition(
                            focusedChild) : 0;
            getLayoutManager().scrollToPosition(focusedPosition);
        }
    }

    private void setupOriginalPadding() {
        if (mOriginalPaddingTop == NO_VALUE) {
            return;
        } else {
            setPadding(getPaddingLeft(), mOriginalPaddingTop, getPaddingRight(),
                    mOriginalPaddingBottom);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCircularScrollingEnabled && mScrollManager.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Point screenSize = new Point();
        getDisplay().getSize(screenSize);
        mScrollManager.setRecyclerView(this, screenSize.x, screenSize.y);
        getViewTreeObserver().addOnPreDrawListener(mPaddingPreDrawListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mScrollManager.clearRecyclerView();
        getViewTreeObserver().removeOnPreDrawListener(mPaddingPreDrawListener);
    }

    /**
     * Enables/disables circular touch scrolling for this view. When enabled, circular touch
     * gestures around the edge of the screen will cause the view to scroll up or down. Related
     * methods let you specify the characteristics of the scrolling, like the speed of the scroll
     * or the are considered for the start of this scrolling gesture.
     *
     * @see #setScrollDegreesPerScreen(float)
     * @see #setBezelFraction(float)
     */
    public void setCircularScrollingGestureEnabled(boolean circularScrollingGestureEnabled) {
        mCircularScrollingEnabled = circularScrollingGestureEnabled;
    }

    /**
     * Returns whether circular scrolling is enabled for this view.
     *
     * @see #setCircularScrollingGestureEnabled(boolean)
     */
    public boolean isCircularScrollingGestureEnabled() {
        return mCircularScrollingEnabled;
    }

    /**
     * Sets how many degrees the user has to rotate by to scroll through one screen height when they
     * are using the circular scrolling gesture.The default value equates 180 degrees scroll to one
     * screen.
     *
     * @see #setCircularScrollingGestureEnabled(boolean)
     *
     * @param degreesPerScreen the number of degrees to rotate by to scroll through one whole
     *                         height of the screen,
     */
    public void setScrollDegreesPerScreen(float degreesPerScreen) {
        mScrollManager.setScrollDegreesPerScreen(degreesPerScreen);
    }

    /**
     * Returns how many degrees does the user have to rotate for to scroll through one screen
     * height.
     *
     * @see #setCircularScrollingGestureEnabled(boolean)
     * @see #setScrollDegreesPerScreen(float).
     */
    public float getScrollDegreesPerScreen() {
        return mScrollManager.getScrollDegreesPerScreen();
    }

    /**
     * Taps within this radius and the radius of the screen are considered close enough to the
     * bezel to be candidates for circular scrolling. Expressed as a fraction of the screen's
     * radius. The default is the whole screen i.e 1.0f.
     */
    public void setBezelFraction(float fraction) {
        mScrollManager.setBezelWidth(fraction);
    }

    /**
     * Returns the current bezel width for circular scrolling as a fraction of the screen's
     * radius.
     *
     * @see #setBezelFraction(float)
     */
    public float getBezelFraction() {
        return mScrollManager.getBezelWidth();
    }

    /**
     * Use this method to configure the {@link WearableRecyclerView} to always align the first and
     * last items with the vertical center of the screen. This effectively moves the start and end
     * of the list to the middle of the screen if the user has scrolled so far. It takes the height
     * of the children into account so that they are correctly centered.
     *
     * @param isEnabled set to true if you wish to align the edge children (first and last)
     *                        with the center of the screen.
     */
    public void setEdgeItemsCenteringEnabled(boolean isEnabled) {
        mEdgeItemsCenteringEnabled = isEnabled;
        if (mEdgeItemsCenteringEnabled) {
            if (getChildCount() > 0) {
                setupCenteredPadding();
            } else {
                mCenterEdgeItemsWhenThereAreChildren = true;
            }
        } else {
            setupOriginalPadding();
            mCenterEdgeItemsWhenThereAreChildren = false;
        }
    }

    /**
     * Returns whether the view is currently configured to center the edge children. See {@link
     * #setEdgeItemsCenteringEnabled} for details.
     */
    public boolean isEdgeItemsCenteringEnabled() {
        return mEdgeItemsCenteringEnabled;
    }
}
