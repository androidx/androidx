/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.slidingpanelayout.widget;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.animation.PathInterpolatorCompat;
import androidx.customview.view.AbsSavedState;
import androidx.customview.widget.Openable;
import androidx.customview.widget.ViewDragHelper;
import androidx.transition.ChangeBounds;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.window.layout.FoldingFeature;
import androidx.window.layout.WindowInfoTracker;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

/**
 * SlidingPaneLayout provides a horizontal, multi-pane layout for use at the top level
 * of a UI. A left (or start) pane is treated as a content list or browser, subordinate to a
 * primary detail view for displaying content.
 *
 * <p>Child views overlap if their combined width exceeds the available width
 * in the SlidingPaneLayout. Each of child views is expand out to fill the available width in
 * the SlidingPaneLayout. When this occurs, the user may slide the topmost view out of the way
 * by dragging it, and dragging back it from the very edge.</p>
 *
 * <p>Thanks to this sliding behavior, SlidingPaneLayout may be suitable for creating layouts
 * that can smoothly adapt across many different screen sizes, expanding out fully on larger
 * screens and collapsing on smaller screens.</p>
 *
 * <p>SlidingPaneLayout is distinct from a navigation drawer as described in the design
 * guide and should not be used in the same scenarios. SlidingPaneLayout should be thought
 * of only as a way to allow a two-pane layout normally used on larger screens to adapt to smaller
 * screens in a natural way. The interaction patterns expressed by SlidingPaneLayout imply
 * a physicality and direct information hierarchy between panes that does not necessarily exist
 * in a scenario where a navigation drawer should be used instead.</p>
 *
 * <p>Appropriate uses of SlidingPaneLayout include pairings of panes such as a contact list and
 * subordinate interactions with those contacts, or an email thread list with the content pane
 * displaying the contents of the selected thread. Inappropriate uses of SlidingPaneLayout include
 * switching between disparate functions of your app, such as jumping from a social stream view
 * to a view of your personal profile - cases such as this should use the navigation drawer
 * pattern instead. ({@link androidx.drawerlayout.widget.DrawerLayout DrawerLayout} implements
 * this pattern.)</p>
 *
 * <p>Like {@link android.widget.LinearLayout LinearLayout}, SlidingPaneLayout supports
 * the use of the layout parameter <code>layout_weight</code> on child views to determine
 * how to divide leftover space after measurement is complete. It is only relevant for width.
 * When views do not overlap weight behaves as it does in a LinearLayout.</p>
 */
public class SlidingPaneLayout extends ViewGroup implements Openable {
    private static final String TAG = "SlidingPaneLayout";

    /**
     * Minimum velocity that will be detected as a fling
     */
    private static final int MIN_FLING_VELOCITY = 400; // dips per second

    /** Class name may be obfuscated by Proguard. Hardcode the string for accessibility usage. */
    private static final String ACCESSIBILITY_CLASS_NAME =
            "androidx.slidingpanelayout.widget.SlidingPaneLayout";

    /**
     * This field is only used to support the setter and getter. It is not used by
     * SlidingPaneLayout.
     */
    private int mSliderFadeColor = 0;

    /**
     * This field is only used to support the setter and getter. It is not used by
     * SlidingPaneLayout.
     */
    private int mCoveredFadeColor;

    /**
     * Drawable used to draw the shadow between panes by default.
     */
    private Drawable mShadowDrawableLeft;

    /**
     * Drawable used to draw the shadow between panes to support RTL (right to left language).
     */
    private Drawable mShadowDrawableRight;

    /**
     * True if a panel can slide with the current measurements
     */
    private boolean mCanSlide;

    /**
     * The child view that can slide, if any.
     */
    View mSlideableView;

    /**
     * How far the panel is offset from its usual position.
     * range [0, 1] where 0 = open, 1 = closed.
     */
    float mSlideOffset = 1.f;

    /**
     * How far the non-sliding panel is parallaxed from its usual position when open.
     * range [0, 1]
     */
    private float mParallaxOffset;

    /**
     * How far in pixels the slideable panel may move.
     */
    int mSlideRange;

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    boolean mIsUnableToDrag;

    /**
     * Distance in pixels to parallax the fixed pane by when fully closed
     */
    private int mParallaxBy;

    private float mInitialMotionX;
    private float mInitialMotionY;

    private final List<SlideableStateListener> mSlideableStateListeners =
            new CopyOnWriteArrayList<>();

    private final List<PanelSlideListener> mPanelSlideListeners = new CopyOnWriteArrayList<>();
    private @Nullable PanelSlideListener mPanelSlideListener;

    final ViewDragHelper mDragHelper;

    /**
     * Stores whether or not the pane was open the last time it was slideable.
     * If open/close operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    boolean mPreservedOpenState;
    private boolean mFirstLayout = true;

    private final Rect mTmpRect = new Rect();

    @LockMode
    private int mLockMode;

    /**
     * User can freely swipe between list and detail panes.
     */
    public static final int LOCK_MODE_UNLOCKED = 0;

    /**
     * The detail pane is locked in an open position. The user cannot swipe to close the detail
     * pane, but the app can close the detail pane programmatically.
     */
    public static final int LOCK_MODE_LOCKED_OPEN = 1;

    /**
     * The detail pane is locked in a closed position. The user cannot swipe to open the detail
     * pane, but the app can open the detail pane programmatically.
     */
    public static final int LOCK_MODE_LOCKED_CLOSED = 2;

    /**
     * The user cannot swipe between list and detail panes, though the app can open or close the
     * detail pane programmatically.
     */
    public static final int LOCK_MODE_LOCKED = 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_OPEN, LOCK_MODE_LOCKED_CLOSED,
            LOCK_MODE_LOCKED})
    @interface LockMode {
    }

    FoldingFeature mFoldingFeature;

    private static boolean sEdgeSizeUsingSystemGestureInsets = Build.VERSION.SDK_INT >= 29;

    /**
     * Set the lock mode that controls how the user can swipe between the panes.
     *
     * @param lockMode The new lock mode for the detail pane.
     */
    public final void setLockMode(@LockMode int lockMode) {
        mLockMode = lockMode;
    }

    /**
     * Get the lock mode used to control over the swipe behavior.
     *
     * @see #setLockMode(int)
     */
    @LockMode
    public final int getLockMode() {
        return mLockMode;
    }

    /**
     * Listener to whether the SlidingPaneLayout is slideable or is a fixed width.
     */
    public interface SlideableStateListener {

        /**
         * Called when onMeasure has measured out the total width of the added layouts
         * within SlidingPaneLayout
         * @param isSlideable  Returns true if the current SlidingPaneLayout has the ability to
         *                     slide, returns false if the SlidingPaneLayout is a fixed width.
         */
        void onSlideableStateChanged(boolean isSlideable);
    }

    /**
     * Listener for monitoring events about sliding panes.
     */
    public interface PanelSlideListener {
        /**
         * Called when a detail view's position changes.
         *
         * @param panel       The child view that was moved
         * @param slideOffset The new offset of this sliding pane within its range, from 0-1
         */
        void onPanelSlide(@NonNull View panel, float slideOffset);

        /**
         * Called when a detail view becomes slid completely open.
         *
         * @param panel The detail view that was slid to an open position
         */
        void onPanelOpened(@NonNull View panel);

        /**
         * Called when a detail view becomes slid completely closed.
         *
         * @param panel The detail view that was slid to a closed position
         */
        void onPanelClosed(@NonNull View panel);
    }

    /**
     * No-op stubs for {@link PanelSlideListener}. If you only want to implement a subset
     * of the listener methods you can extend this instead of implement the full interface.
     */
    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override
        public void onPanelSlide(@NonNull View panel, float slideOffset) {
        }

        @Override
        public void onPanelOpened(@NonNull View panel) {
        }

        @Override
        public void onPanelClosed(@NonNull View panel) {
        }
    }

    private FoldingFeatureObserver.OnFoldingFeatureChangeListener mOnFoldingFeatureChangeListener =
            new FoldingFeatureObserver.OnFoldingFeatureChangeListener() {
                @Override
                public void onFoldingFeatureChange(@NonNull FoldingFeature foldingFeature) {
                    mFoldingFeature = foldingFeature;
                    // Start transition animation when folding feature changed
                    Transition changeBounds = new ChangeBounds();
                    changeBounds.setDuration(300L);
                    changeBounds.setInterpolator(PathInterpolatorCompat.create(0.2f, 0, 0, 1));
                    TransitionManager.beginDelayedTransition(SlidingPaneLayout.this, changeBounds);
                    requestLayout();
                }
            };

    private FoldingFeatureObserver mFoldingFeatureObserver;

    public SlidingPaneLayout(@NonNull Context context) {
        this(context, null);
    }

    public SlidingPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SlidingPaneLayout(@NonNull Context context, @Nullable AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);

        final float density = context.getResources().getDisplayMetrics().density;

        setWillNotDraw(false);

        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());
        ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

        mDragHelper = ViewDragHelper.create(this, 0.5f, new DragHelperCallback());
        mDragHelper.setMinVelocity(MIN_FLING_VELOCITY * density);

        WindowInfoTracker repo = WindowInfoTracker.getOrCreate(context);
        Executor mainExecutor = ContextCompat.getMainExecutor(context);
        FoldingFeatureObserver foldingFeatureObserver =
                new FoldingFeatureObserver(repo, mainExecutor);
        setFoldingFeatureObserver(foldingFeatureObserver);
    }

    private void setFoldingFeatureObserver(
            FoldingFeatureObserver foldingFeatureObserver) {
        mFoldingFeatureObserver = foldingFeatureObserver;
        mFoldingFeatureObserver.setOnFoldingFeatureChangeListener(
                mOnFoldingFeatureChangeListener);
    }

    /**
     * Set a distance to parallax the lower pane by when the upper pane is in its
     * fully closed state. The lower pane will scroll between this position and
     * its fully open state.
     *
     * @param parallaxBy Distance to parallax by in pixels
     */
    public void setParallaxDistance(@Px int parallaxBy) {
        mParallaxBy = parallaxBy;
        requestLayout();
    }

    /**
     * @return The distance the lower pane will parallax by when the upper pane is fully closed.
     * @see #setParallaxDistance(int)
     */
    @Px
    public int getParallaxDistance() {
        return mParallaxBy;
    }

    /**
     * Set the color used to fade the sliding pane out when it is slid most of the way offscreen.
     *
     * @param color An ARGB-packed color value
     * @deprecated SlidingPaneLayout no longer uses this field.
     */
    @Deprecated
    public void setSliderFadeColor(@ColorInt int color) {
        mSliderFadeColor = color;
    }

    /**
     * @return The ARGB-packed color value used to fade the sliding pane
     * @deprecated This field is no longer populated by SlidingPaneLayout.
     */
    @Deprecated
    @ColorInt
    public int getSliderFadeColor() {
        return mSliderFadeColor;
    }

    /**
     * Set the color used to fade the pane covered by the sliding pane out when the pane
     * will become fully covered in the closed state.
     *
     * @param color An ARGB-packed color value
     * @deprecated SlidingPaneLayout no longer uses this field.
     */
    @Deprecated
    public void setCoveredFadeColor(@ColorInt int color) {
        mCoveredFadeColor = color;
    }

    /**
     * @return The ARGB-packed color value used to fade the fixed pane
     * @deprecated This field is no longer populated by SlidingPaneLayout
     */
    @Deprecated
    @ColorInt
    public int getCoveredFadeColor() {
        return mCoveredFadeColor;
    }

    /**
     * Set a listener to be notified of panel slide events. Note that this method is deprecated
     * and you should use {@link #addPanelSlideListener(PanelSlideListener)} to add a listener and
     * {@link #removePanelSlideListener(PanelSlideListener)} to remove a registered listener.
     *
     * @param listener Listener to notify when drawer events occur
     * @see PanelSlideListener
     * @see #addPanelSlideListener(PanelSlideListener)
     * @see #removePanelSlideListener(PanelSlideListener)
     * @deprecated Use {@link #addPanelSlideListener(PanelSlideListener)}
     */
    @Deprecated
    public void setPanelSlideListener(@Nullable PanelSlideListener listener) {
        // The logic in this method emulates what we had before support for multiple
        // registered listeners.
        if (mPanelSlideListener != null) {
            removePanelSlideListener(mPanelSlideListener);
        }
        if (listener != null) {
            addPanelSlideListener(listener);
        }
        // Update the deprecated field so that we can remove the passed listener the next
        // time we're called
        mPanelSlideListener = listener;
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of sliding
     * state events.
     * @param listener  Listener to notify when sliding state events occur.
     * @see #removeSlideableStateListener(SlideableStateListener)
     */
    public void addSlideableStateListener(@NonNull SlideableStateListener listener) {
        mSlideableStateListeners.add(listener);
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of sliding
     * state events.
     * @param listener Listener to notify when sliding state events occur
     */
    public void removeSlideableStateListener(@NonNull SlideableStateListener listener) {
        mSlideableStateListeners.remove(listener);
    }

    /**
     * Adds the specified listener to the list of listeners that will be notified of
     * panel slide events.
     *
     * @param listener Listener to notify when panel slide events occur.
     * @see #removePanelSlideListener(PanelSlideListener)
     */
    public void addPanelSlideListener(@NonNull PanelSlideListener listener) {
        mPanelSlideListeners.add(listener);
    }

    /**
     * Removes the specified listener from the list of listeners that will be notified of
     * panel slide events.
     *
     * @param listener Listener to remove from being notified of panel slide events
     * @see #addPanelSlideListener(PanelSlideListener)
     */
    public void removePanelSlideListener(@NonNull PanelSlideListener listener) {
        mPanelSlideListeners.remove(listener);
    }

    void dispatchOnPanelSlide(@NonNull View panel) {
        for (PanelSlideListener listener : mPanelSlideListeners) {
            listener.onPanelSlide(panel, mSlideOffset);
        }
    }

    void dispatchOnPanelOpened(@NonNull View panel) {
        for (PanelSlideListener listener : mPanelSlideListeners) {
            listener.onPanelOpened(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void dispatchOnPanelClosed(@NonNull View panel) {
        for (PanelSlideListener listener : mPanelSlideListeners) {
            listener.onPanelClosed(panel);
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
    }

    void updateObscuredViewsVisibility(View panel) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final int startBound = isLayoutRtl ? (getWidth() - getPaddingRight()) : getPaddingLeft();
        final int endBound = isLayoutRtl ? getPaddingLeft() : (getWidth() - getPaddingRight());
        final int topBound = getPaddingTop();
        final int bottomBound = getHeight() - getPaddingBottom();
        final int left;
        final int right;
        final int top;
        final int bottom;
        if (panel != null && viewIsOpaque(panel)) {
            left = panel.getLeft();
            right = panel.getRight();
            top = panel.getTop();
            bottom = panel.getBottom();
        } else {
            left = right = top = bottom = 0;
        }

        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);

            if (child == panel) {
                // There are still more children above the panel but they won't be affected.
                break;
            } else if (child.getVisibility() == GONE) {
                continue;
            }

            final int clampedChildLeft = Math.max(
                    (isLayoutRtl ? endBound : startBound), child.getLeft());
            final int clampedChildTop = Math.max(topBound, child.getTop());
            final int clampedChildRight = Math.min(
                    (isLayoutRtl ? startBound : endBound), child.getRight());
            final int clampedChildBottom = Math.min(bottomBound, child.getBottom());
            final int vis;
            if (clampedChildLeft >= left && clampedChildTop >= top
                    && clampedChildRight <= right && clampedChildBottom <= bottom) {
                vis = INVISIBLE;
            } else {
                vis = VISIBLE;
            }
            child.setVisibility(vis);
        }
    }

    void setAllChildrenVisible() {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == INVISIBLE) {
                child.setVisibility(VISIBLE);
            }
        }
    }

    @SuppressWarnings("deprecation")
    // Remove suppression once b/120984816 is addressed.
    private static boolean viewIsOpaque(View v) {
        if (v.isOpaque()) {
            return true;
        }

        // View#isOpaque didn't take all valid opaque scrollbar modes into account
        // before API 18 (JB-MR2). On newer devices rely solely on isOpaque above and return false
        // here. On older devices, check the view's background drawable directly as a fallback.
        if (Build.VERSION.SDK_INT >= 18) {
            return false;
        }

        final Drawable bg = v.getBackground();
        if (bg != null) {
            return bg.getOpacity() == PixelFormat.OPAQUE;
        }
        return false;
    }

    @Override
    public void addView(@NonNull View child, int index, @Nullable ViewGroup.LayoutParams params) {
        if (getChildCount() == 1) {
            // Wrap detail view inside a touch blocker container
            View detailView = new TouchBlocker(child);
            super.addView(detailView, index, params);
            return;
        }
        super.addView(child, index, params);
    }

    @Override
    public void removeView(@NonNull View view) {
        if (view.getParent() instanceof TouchBlocker) {
            super.removeView((View) view.getParent());
            return;
        }
        super.removeView(view);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mFirstLayout = true;
        if (mFoldingFeatureObserver != null) {
            Activity activity = getActivityOrNull(getContext());
            if (activity != null) {
                mFoldingFeatureObserver.registerLayoutStateChangeCallback(activity);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mFirstLayout = true;
        if (mFoldingFeatureObserver != null) {
            mFoldingFeatureObserver.unregisterLayoutStateChangeCallback();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int layoutHeight = 0;
        int maxLayoutHeight = 0;
        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                layoutHeight = maxLayoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
                break;
            case MeasureSpec.AT_MOST:
                maxLayoutHeight = heightSize - getPaddingTop() - getPaddingBottom();
                break;
        }

        float weightSum = 0;
        boolean canSlide = false;
        final int widthAvailable = Math.max(widthSize - getPaddingLeft() - getPaddingRight(), 0);
        int widthRemaining = widthAvailable;
        final int childCount = getChildCount();

        if (childCount > 2) {
            Log.e(TAG, "onMeasure: More than two child views are not supported.");
        }

        // We'll find the current one below.
        mSlideableView = null;

        // First pass. Measure based on child LayoutParams width/height.
        // Weight will incur a second pass.
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (child.getVisibility() == GONE) {
                lp.dimWhenOffset = false;
                continue;
            }

            if (lp.weight > 0) {
                weightSum += lp.weight;

                // If we have no width, weight is the only contributor to the final size.
                // Measure this view on the weight pass only.
                if (lp.width == 0) continue;
            }

            int childWidthSpec;
            final int horizontalMargin = lp.leftMargin + lp.rightMargin;

            int childWidthSize = Math.max(widthAvailable - horizontalMargin, 0);
            // When the parent width spec is UNSPECIFIED, measure each of child to get its
            // desired width.
            if (lp.width == LayoutParams.WRAP_CONTENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(childWidthSize,
                        widthMode == MeasureSpec.UNSPECIFIED ? widthMode : MeasureSpec.AT_MOST);
            } else if (lp.width == LayoutParams.MATCH_PARENT) {
                childWidthSpec = MeasureSpec.makeMeasureSpec(childWidthSize, widthMode);
            } else {
                childWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            }

            int childHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom(), lp.height);
            child.measure(childWidthSpec, childHeightSpec);
            final int childWidth = child.getMeasuredWidth();
            final int childHeight = child.getMeasuredHeight();

            if (childHeight > layoutHeight) {
                if (heightMode == MeasureSpec.AT_MOST) {
                    layoutHeight = Math.min(childHeight, maxLayoutHeight);
                } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                    layoutHeight = childHeight;
                }
            }

            widthRemaining -= childWidth;
            // Skip first child (list pane), the list pane is always a non-sliding pane.
            if (i == 0) {
                continue;
            }
            canSlide |= lp.slideable = widthRemaining < 0;
            if (lp.slideable) {
                mSlideableView = child;
            }
        }
        // Second pass. Resolve weight.
        // Child views overlap when the combined width of child views cannot fit into the
        // available width. Each of child views is sized to fill all available space. If there is
        // no overlap, distribute the extra width proportionally to weight.
        if (canSlide || weightSum > 0) {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE) {
                    continue;
                }

                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                final boolean skippedFirstPass = lp.width == 0 && lp.weight > 0;
                final int measuredWidth = skippedFirstPass ? 0 : child.getMeasuredWidth();
                int newWidth = measuredWidth;
                int childWidthSpec = 0;
                if (canSlide) {
                    // Child view consumes available space if the combined width cannot fit into
                    // the layout available width.
                    final int horizontalMargin = lp.leftMargin + lp.rightMargin;
                    newWidth = widthAvailable - horizontalMargin;
                    childWidthSpec = MeasureSpec.makeMeasureSpec(
                            newWidth, MeasureSpec.EXACTLY);

                } else if (lp.weight > 0) {
                    // Distribute the extra width proportionally similar to LinearLayout
                    final int widthToDistribute = Math.max(0, widthRemaining);
                    final int addedWidth = (int) (lp.weight * widthToDistribute / weightSum);
                    newWidth = measuredWidth + addedWidth;
                    childWidthSpec = MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY);
                }
                final int childHeightSpec = measureChildHeight(child, heightMeasureSpec,
                        getPaddingTop() + getPaddingBottom());
                if (measuredWidth != newWidth) {
                    child.measure(childWidthSpec, childHeightSpec);
                    final int childHeight = child.getMeasuredHeight();
                    if (childHeight > layoutHeight) {
                        if (heightMode == MeasureSpec.AT_MOST) {
                            layoutHeight = Math.min(childHeight, maxLayoutHeight);
                        } else if (heightMode == MeasureSpec.UNSPECIFIED) {
                            layoutHeight = childHeight;
                        }
                    }
                }
            }
        }

        // At this point, all child views have been measured. Calculate the device fold position
        // in the view. Update the split position to where the fold when it exists.
        ArrayList<Rect> splitViews = splitViewPositions();

        if (splitViews != null && !canSlide) {
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);

                if (child.getVisibility() == GONE) {
                    continue;
                }

                final Rect splitView = splitViews.get(i);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                // If child view cannot fit in the separating view, expand the child view to fill
                // available space.
                final int horizontalMargin = lp.leftMargin + lp.rightMargin;
                final int childHeightSpec = MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(),
                        MeasureSpec.EXACTLY);
                int childWidthSpec = MeasureSpec.makeMeasureSpec(splitView.width(),
                        MeasureSpec.AT_MOST);
                child.measure(childWidthSpec, childHeightSpec);
                if ((child.getMeasuredWidthAndState() & MEASURED_STATE_TOO_SMALL) == 1 || (
                        getMinimumWidth(child) != 0
                                && splitView.width() < getMinimumWidth(child))) {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(widthAvailable - horizontalMargin,
                            MeasureSpec.EXACTLY);
                    child.measure(childWidthSpec, childHeightSpec);
                    // Skip first child (list pane), the list pane is always a non-sliding pane.
                    if (i == 0) {
                        continue;
                    }
                    canSlide = lp.slideable = true;
                    mSlideableView = child;
                } else {
                    childWidthSpec = MeasureSpec.makeMeasureSpec(splitView.width(),
                            MeasureSpec.EXACTLY);
                    child.measure(childWidthSpec, childHeightSpec);
                }
            }
        }

        final int measuredWidth = widthSize;
        final int measuredHeight = layoutHeight + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(measuredWidth, measuredHeight);
        if (canSlide != mCanSlide) {
            mCanSlide = canSlide;
            for (SlideableStateListener listener : mSlideableStateListeners) {
                listener.onSlideableStateChanged(mCanSlide);
            }
        }

        if (mDragHelper.getViewDragState() != ViewDragHelper.STATE_IDLE && !canSlide) {
            // Cancel scrolling in progress, it's no longer relevant.
            mDragHelper.abort();
        }
    }

    private static int getMinimumWidth(View child) {
        if (child instanceof TouchBlocker) {
            return ViewCompat.getMinimumWidth(((TouchBlocker) child).getChildAt(0));
        }
        return ViewCompat.getMinimumWidth(child);
    }

    private static int measureChildHeight(@NonNull View child,
            int spec, int padding) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int childHeightSpec;
        final boolean skippedFirstPass = lp.width == 0 && lp.weight > 0;
        if (skippedFirstPass) {
            // This was skipped the first time; figure out a real height spec.
            childHeightSpec = getChildMeasureSpec(spec, padding, lp.height);

        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(
                    child.getMeasuredHeight(), MeasureSpec.EXACTLY);
        }
        return childHeightSpec;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final int width = r - l;
        final int paddingStart = isLayoutRtl ? getPaddingRight() : getPaddingLeft();
        final int paddingEnd = isLayoutRtl ? getPaddingLeft() : getPaddingRight();
        final int paddingTop = getPaddingTop();

        final int childCount = getChildCount();
        int xStart = paddingStart;
        int nextXStart = xStart;

        if (mFirstLayout) {
            mSlideOffset = mCanSlide && mPreservedOpenState ? 0.f : 1.f;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int childWidth = child.getMeasuredWidth();
            int offset = 0;

            if (lp.slideable) {
                final int margin = lp.leftMargin + lp.rightMargin;
                final int range = Math.min(nextXStart, width - paddingEnd) - xStart - margin;
                mSlideRange = range;
                final int lpMargin = isLayoutRtl ? lp.rightMargin : lp.leftMargin;
                lp.dimWhenOffset = xStart + lpMargin + range + childWidth / 2 > width - paddingEnd;
                final int pos = (int) (range * mSlideOffset);
                xStart += pos + lpMargin;
                mSlideOffset = (float) pos / mSlideRange;
            } else if (mCanSlide && mParallaxBy != 0) {
                offset = (int) ((1 - mSlideOffset) * mParallaxBy);
                xStart = nextXStart;
            } else {
                xStart = nextXStart;
            }

            final int childRight;
            final int childLeft;
            if (isLayoutRtl) {
                childRight = width - xStart + offset;
                childLeft = childRight - childWidth;
            } else {
                childLeft = xStart - offset;
                childRight = childLeft + childWidth;
            }

            final int childTop = paddingTop;
            final int childBottom = childTop + child.getMeasuredHeight();
            child.layout(childLeft, paddingTop, childRight, childBottom);

            // If a folding feature separates the content, we use its width as the extra
            // offset for the next child, in order to avoid rendering the content under it.
            int nextXOffset = 0;
            if (mFoldingFeature != null
                    && mFoldingFeature.getOrientation() == FoldingFeature.Orientation.VERTICAL
                    && mFoldingFeature.isSeparating()) {
                nextXOffset = mFoldingFeature.getBounds().width();
            }
            nextXStart += child.getWidth() + Math.abs(nextXOffset);
        }

        if (mFirstLayout) {
            if (mCanSlide) {
                if (mParallaxBy != 0) {
                    parallaxOtherViews(mSlideOffset);
                }
            }
            updateObscuredViewsVisibility(mSlideableView);
        }

        mFirstLayout = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // Recalculate sliding panes and their details
        if (w != oldw) {
            mFirstLayout = true;
        }
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (!isInTouchMode() && !mCanSlide) {
            mPreservedOpenState = child == mSlideableView;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getActionMasked();

        // Preserve the open state based on the last view that was touched.
        if (!mCanSlide && action == MotionEvent.ACTION_DOWN && getChildCount() > 1) {
            // After the first things will be slideable.
            final View secondChild = getChildAt(1);
            if (secondChild != null) {
                mPreservedOpenState = mDragHelper.isViewUnder(secondChild,
                        (int) ev.getX(), (int) ev.getY());
            }
        }

        if (!mCanSlide || (mIsUnableToDrag && action != MotionEvent.ACTION_DOWN)) {
            mDragHelper.cancel();
            return super.onInterceptTouchEvent(ev);
        }

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mDragHelper.cancel();
            return false;
        }

        boolean interceptTap = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mIsUnableToDrag = false;
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;

                if (mDragHelper.isViewUnder(mSlideableView, (int) x, (int) y)
                        && isDimmed(mSlideableView)) {
                    interceptTap = true;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final float x = ev.getX();
                final float y = ev.getY();
                final float adx = Math.abs(x - mInitialMotionX);
                final float ady = Math.abs(y - mInitialMotionY);
                final int slop = mDragHelper.getTouchSlop();
                if (adx > slop && ady > adx) {
                    mDragHelper.cancel();
                    mIsUnableToDrag = true;
                    return false;
                }
            }
        }

        final boolean interceptForDrag = mDragHelper.shouldInterceptTouchEvent(ev);

        return interceptForDrag || interceptTap;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mCanSlide) {
            return super.onTouchEvent(ev);
        }

        mDragHelper.processTouchEvent(ev);

        boolean wantTouchEvents = true;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                mInitialMotionX = x;
                mInitialMotionY = y;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (isDimmed(mSlideableView)) {
                    final float x = ev.getX();
                    final float y = ev.getY();
                    final float dx = x - mInitialMotionX;
                    final float dy = y - mInitialMotionY;
                    final int slop = mDragHelper.getTouchSlop();
                    if (dx * dx + dy * dy < slop * slop
                            && mDragHelper.isViewUnder(mSlideableView, (int) x, (int) y)) {
                        // Taps close a dimmed open pane.
                        closePane(0);
                        break;
                    }
                }
                break;
            }
        }

        return wantTouchEvents;
    }

    private boolean closePane(int initialVelocity) {
        if (!mCanSlide) {
            mPreservedOpenState = false;
        }
        if (mFirstLayout || smoothSlideTo(1.f, initialVelocity)) {
            mPreservedOpenState = false;
            return true;
        }
        return false;
    }

    private boolean openPane(int initialVelocity) {
        if (!mCanSlide) {
            mPreservedOpenState = true;
        }
        if (mFirstLayout || smoothSlideTo(0.f, initialVelocity)) {
            mPreservedOpenState = true;
            return true;
        }
        return false;
    }

    /**
     * @deprecated Renamed to {@link #openPane()} - this method is going away soon!
     */
    @Deprecated
    public void smoothSlideOpen() {
        openPane();
    }

    /**
     * Open the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    @Override
    public void open() {
        openPane();
    }

    /**
     * Open the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now open/in the process of opening
     */
    public boolean openPane() {
        return openPane(0);
    }

    /**
     * @return true if content in this layout can be slid open and closed
     * @deprecated Renamed to {@link #isSlideable()} - this method is going away soon!
     */
    @Deprecated
    public boolean canSlide() {
        return mCanSlide;
    }

    /**
     * @deprecated Renamed to {@link #closePane()} - this method is going away soon!
     */
    @Deprecated
    public void smoothSlideClosed() {
        closePane();
    }

    /**
     * Close the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     */
    @Override
    public void close() {
        closePane();
    }

    /**
     * Close the detail view if it is currently slideable. If first layout
     * has already completed this will animate.
     *
     * @return true if the pane was slideable and is now closed/in the process of closing
     */
    public boolean closePane() {
        return closePane(0);
    }

    /**
     * Check if the detail view is completely open. It can be open either because the slider
     * itself is open revealing the detail view, or if all content fits without sliding.
     *
     * @return true if the detail view is completely open
     */
    @Override
    public boolean isOpen() {
        return !mCanSlide || mSlideOffset == 0;
    }

    /**
     * Check if both the list and detail view panes in this layout can fully fit side-by-side. If
     * not, the content pane has the capability to slide back and forth. Note that the lock mode
     * is not taken into account in this method. This method is typically used to determine
     * whether the layout is showing two-pane or single-pane.
     *
     * @return true if both panes cannot fit side-by-side, and detail pane in this layout has
     * the capability to slide back and forth.
     */
    public boolean isSlideable() {
        return mCanSlide;
    }

    void onPanelDragged(int newLeft) {
        if (mSlideableView == null) {
            // This can happen if we're aborting motion during layout because everything now fits.
            mSlideOffset = 0;
            return;
        }
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

        int childWidth = mSlideableView.getWidth();
        final int newStart = isLayoutRtl ? getWidth() - newLeft - childWidth : newLeft;

        final int paddingStart = isLayoutRtl ? getPaddingRight() : getPaddingLeft();
        final int lpMargin = isLayoutRtl ? lp.rightMargin : lp.leftMargin;
        final int startBound = paddingStart + lpMargin;

        mSlideOffset = (float) (newStart - startBound) / mSlideRange;

        if (mParallaxBy != 0) {
            parallaxOtherViews(mSlideOffset);
        }

        dispatchOnPanelSlide(mSlideableView);
    }

    @Override
    protected boolean drawChild(@NonNull Canvas canvas, View child, long drawingTime) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final boolean enableEdgeLeftTracking = isLayoutRtl ^ isOpen();
        if (enableEdgeLeftTracking) {
            mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_LEFT);
            Insets gestureInsets = getSystemGestureInsets();
            if (gestureInsets != null) {
                // Gesture insets will be 0 if the device doesn't have gesture navigation enabled.
                mDragHelper.setEdgeSize(Math.max(mDragHelper.getDefaultEdgeSize(),
                        gestureInsets.left));
            }
        } else {
            mDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
            Insets gestureInsets = getSystemGestureInsets();
            if (gestureInsets != null) {
                // Gesture insets will be 0 if the device doesn't have gesture navigation enabled.
                mDragHelper.setEdgeSize(Math.max(mDragHelper.getDefaultEdgeSize(),
                        gestureInsets.right));
            }
        }
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        boolean result;
        final int save = canvas.save();

        if (mCanSlide && !lp.slideable && mSlideableView != null) {
            // Clip against the slider; no sense drawing what will immediately be covered.
            canvas.getClipBounds(mTmpRect);
            if (isLayoutRtlSupport()) {
                mTmpRect.left = Math.max(mTmpRect.left, mSlideableView.getRight());
            } else {
                mTmpRect.right = Math.min(mTmpRect.right, mSlideableView.getLeft());
            }
            canvas.clipRect(mTmpRect);
        }

        result = super.drawChild(canvas, child, drawingTime);

        canvas.restoreToCount(save);

        return result;
    }

    // Get system gesture insets when SDK version is larger than 29. Otherwise, return null.
    private Insets getSystemGestureInsets() {
        Insets gestureInsets = null;
        if (sEdgeSizeUsingSystemGestureInsets) {
            WindowInsetsCompat rootInsetsCompat = ViewCompat.getRootWindowInsets(this);
            if (rootInsetsCompat != null) {
                gestureInsets = rootInsetsCompat.getSystemGestureInsets();
            }
        }
        return gestureInsets;
    }

    private Method mGetDisplayList;
    private Field mRecreateDisplayList;
    private boolean mDisplayListReflectionLoaded;

    void invalidateChildRegion(View v) {
        if (Build.VERSION.SDK_INT >= 17) {
            ViewCompat.setLayerPaint(v, ((LayoutParams) v.getLayoutParams()).dimPaint);
            return;
        }

        if (Build.VERSION.SDK_INT >= 16) {
            // Private API hacks! Nasty! Bad!
            //
            // In Jellybean, some optimizations in the hardware UI renderer
            // prevent a changed Paint on a View using a hardware layer from having
            // the intended effect. This twiddles some internal bits on the view to force
            // it to recreate the display list.
            if (!mDisplayListReflectionLoaded) {
                try {
                    mGetDisplayList = View.class.getDeclaredMethod("getDisplayList",
                            (Class<?>[]) null);
                } catch (NoSuchMethodException e) {
                    Log.e(TAG, "Couldn't fetch getDisplayList method; dimming won't work right.",
                            e);
                }
                try {
                    mRecreateDisplayList = View.class.getDeclaredField("mRecreateDisplayList");
                    mRecreateDisplayList.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "Couldn't fetch mRecreateDisplayList field; dimming will be slow.",
                            e);
                }
                mDisplayListReflectionLoaded = true;
            }
            if (mGetDisplayList == null || mRecreateDisplayList == null) {
                // Slow path. REALLY slow path. Let's hope we don't get here.
                v.invalidate();
                return;
            }

            try {
                mRecreateDisplayList.setBoolean(v, true);
                mGetDisplayList.invoke(v, (Object[]) null);
            } catch (Exception e) {
                Log.e(TAG, "Error refreshing display list state", e);
            }
        }

        ViewCompat.postInvalidateOnAnimation(this, v.getLeft(), v.getTop(), v.getRight(),
                v.getBottom());
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity    initial velocity in case of fling, or 0.
     */
    boolean smoothSlideTo(float slideOffset, int velocity) {
        if (!mCanSlide) {
            // Nothing to do.
            return false;
        }

        final boolean isLayoutRtl = isLayoutRtlSupport();
        final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

        int x;
        if (isLayoutRtl) {
            int startBound = getPaddingRight() + lp.rightMargin;
            int childWidth = mSlideableView.getWidth();
            x = (int) (getWidth() - (startBound + slideOffset * mSlideRange + childWidth));
        } else {
            int startBound = getPaddingLeft() + lp.leftMargin;
            x = (int) (startBound + slideOffset * mSlideRange);
        }

        if (mDragHelper.smoothSlideViewTo(mSlideableView, x, mSlideableView.getTop())) {
            setAllChildrenVisible();
            ViewCompat.postInvalidateOnAnimation(this);
            return true;
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            if (!mCanSlide) {
                mDragHelper.abort();
                return;
            }

            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    /**
     * @param d drawable to use as a shadow
     * @deprecated Renamed to {@link #setShadowDrawableLeft(Drawable d)} to support LTR (left to
     * right language) and {@link #setShadowDrawableRight(Drawable d)} to support RTL (right to left
     * language) during opening/closing.
     */
    @Deprecated
    public void setShadowDrawable(Drawable d) {
        setShadowDrawableLeft(d);
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane
     * during opening/closing.
     *
     * @param d drawable to use as a shadow
     */
    public void setShadowDrawableLeft(@Nullable Drawable d) {
        mShadowDrawableLeft = d;
    }

    /**
     * Set a drawable to use as a shadow cast by the left pane onto the right pane
     * during opening/closing to support right to left language.
     *
     * @param d drawable to use as a shadow
     */
    public void setShadowDrawableRight(@Nullable Drawable d) {
        mShadowDrawableRight = d;
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane
     * during opening/closing.
     *
     * @param resId Resource ID of a drawable to use
     * @deprecated Renamed to {@link #setShadowResourceLeft(int)} to support LTR (left to
     * right language) and {@link #setShadowResourceRight(int)} to support RTL (right to left
     * language) during opening/closing.
     */
    @Deprecated
    public void setShadowResource(@DrawableRes int resId) {
        setShadowDrawableLeft(getResources().getDrawable(resId));
    }

    /**
     * Set a drawable to use as a shadow cast by the right pane onto the left pane
     * during opening/closing.
     *
     * @param resId Resource ID of a drawable to use
     */
    public void setShadowResourceLeft(int resId) {
        setShadowDrawableLeft(ContextCompat.getDrawable(getContext(), resId));
    }

    /**
     * Set a drawable to use as a shadow cast by the left pane onto the right pane
     * during opening/closing to support right to left language.
     *
     * @param resId Resource ID of a drawable to use
     */
    public void setShadowResourceRight(int resId) {
        setShadowDrawableRight(ContextCompat.getDrawable(getContext(), resId));
    }

    @Override
    public void draw(@NonNull Canvas c) {
        super.draw(c);
        final boolean isLayoutRtl = isLayoutRtlSupport();
        Drawable shadowDrawable;
        if (isLayoutRtl) {
            shadowDrawable = mShadowDrawableRight;
        } else {
            shadowDrawable = mShadowDrawableLeft;
        }

        final View shadowView = getChildCount() > 1 ? getChildAt(1) : null;
        if (shadowView == null || shadowDrawable == null) {
            // No need to draw a shadow if we don't have one.
            return;
        }

        final int top = shadowView.getTop();
        final int bottom = shadowView.getBottom();

        final int shadowWidth = shadowDrawable.getIntrinsicWidth();
        final int left;
        final int right;
        if (isLayoutRtlSupport()) {
            left = shadowView.getRight();
            right = left + shadowWidth;
        } else {
            right = shadowView.getLeft();
            left = right - shadowWidth;
        }

        shadowDrawable.setBounds(left, top, right, bottom);
        shadowDrawable.draw(c);
    }

    private void parallaxOtherViews(float slideOffset) {
        final boolean isLayoutRtl = isLayoutRtlSupport();
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View v = getChildAt(i);
            if (v == mSlideableView) continue;

            final int oldOffset = (int) ((1 - mParallaxOffset) * mParallaxBy);
            mParallaxOffset = slideOffset;
            final int newOffset = (int) ((1 - slideOffset) * mParallaxBy);
            final int dx = oldOffset - newOffset;

            v.offsetLeftAndRight(isLayoutRtl ? -dx : dx);
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param v      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     *               or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected boolean canScroll(@NonNull View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) v;
            final int scrollX = v.getScrollX();
            final int scrollY = v.getScrollY();
            final int count = group.getChildCount();
            // Count backwards - let topmost views consume scroll distance first.
            for (int i = count - 1; i >= 0; i--) {
                // TODO: Add versioned support here for transformed views.
                // This will not work for transformed views in Honeycomb+
                final View child = group.getChildAt(i);
                if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight()
                        && y + scrollY >= child.getTop() && y + scrollY < child.getBottom()
                        && canScroll(child, true, dx, x + scrollX - child.getLeft(),
                        y + scrollY - child.getTop())) {
                    return true;
                }
            }
        }

        return checkV && v.canScrollHorizontally((isLayoutRtlSupport() ? dx : -dx));
    }

    boolean isDimmed(View child) {
        if (child == null) {
            return false;
        }
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        return mCanSlide && lp.dimWhenOffset && mSlideOffset > 0;
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof MarginLayoutParams
                ? new LayoutParams((MarginLayoutParams) p)
                : new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams && super.checkLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @NonNull
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState ss = new SavedState(superState);
        ss.isOpen = isSlideable() ? isOpen() : mPreservedOpenState;
        ss.mLockMode = mLockMode;

        return ss;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        if (ss.isOpen) {
            openPane();
        } else {
            closePane();
        }
        mPreservedOpenState = ss.isOpen;

        setLockMode(ss.mLockMode);
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {

        DragHelperCallback() {
        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            if (!isDraggable()) {
                return false;
            }

            return ((LayoutParams) child.getLayoutParams()).slideable;
        }

        @Override
        public void onViewDragStateChanged(int state) {
            if (mDragHelper.getViewDragState() == ViewDragHelper.STATE_IDLE) {
                if (mSlideOffset == 1) {
                    updateObscuredViewsVisibility(mSlideableView);
                    dispatchOnPanelClosed(mSlideableView);
                    mPreservedOpenState = false;
                } else {
                    dispatchOnPanelOpened(mSlideableView);
                    mPreservedOpenState = true;
                }
            }
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            // Make all child views visible in preparation for sliding things around
            setAllChildrenVisible();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            onPanelDragged(left);
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            final LayoutParams lp = (LayoutParams) releasedChild.getLayoutParams();

            int left;
            if (isLayoutRtlSupport()) {
                int startToRight = getPaddingRight() + lp.rightMargin;
                if (xvel < 0 || (xvel == 0 && mSlideOffset > 0.5f)) {
                    startToRight += mSlideRange;
                }
                int childWidth = mSlideableView.getWidth();
                left = getWidth() - startToRight - childWidth;
            } else {
                left = getPaddingLeft() + lp.leftMargin;
                if (xvel > 0 || (xvel == 0 && mSlideOffset > 0.5f)) {
                    left += mSlideRange;
                }
            }
            mDragHelper.settleCapturedViewAt(left, releasedChild.getTop());
            invalidate();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mSlideRange;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int newLeft = left;
            final LayoutParams lp = (LayoutParams) mSlideableView.getLayoutParams();

            if (isLayoutRtlSupport()) {
                int startBound = getWidth()
                        - (getPaddingRight() + lp.rightMargin + mSlideableView.getWidth());
                int endBound = startBound - mSlideRange;
                newLeft = Math.max(Math.min(newLeft, startBound), endBound);
            } else {
                int startBound = getPaddingLeft() + lp.leftMargin;
                int endBound = startBound + mSlideRange;
                newLeft = Math.min(Math.max(newLeft, startBound), endBound);
            }
            return newLeft;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            // Make sure we never move views vertically.
            // This could happen if the child has less height than its parent.
            return child.getTop();
        }

        @Override
        public void onEdgeTouched(int edgeFlags, int pointerId) {
            if (!isDraggable()) {
                return;
            }
            mDragHelper.captureChildView(mSlideableView, pointerId);
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            if (!isDraggable()) {
                return;
            }
            mDragHelper.captureChildView(mSlideableView, pointerId);
        }

        private boolean isDraggable() {
            if (mIsUnableToDrag) {
                return false;
            }
            if (getLockMode() == LOCK_MODE_LOCKED) {
                return false;
            }
            if (isOpen() && getLockMode() == LOCK_MODE_LOCKED_OPEN) {
                return false;
            }
            if (!isOpen() && getLockMode() == LOCK_MODE_LOCKED_CLOSED) {
                return false;
            }
            return true;
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = new int[]{
                android.R.attr.layout_weight
        };

        /**
         * The weighted proportion of how much of the leftover space
         * this child should consume after measurement.
         */
        public float weight = 0;

        /**
         * True if this pane is the slideable pane in the layout.
         */
        boolean slideable;

        /**
         * True if this view should be drawn dimmed
         * when it's been offset from its default position.
         */
        boolean dimWhenOffset;

        Paint dimPaint;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(@NonNull android.view.ViewGroup.LayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(@NonNull LayoutParams source) {
            super(source);
            this.weight = source.weight;
        }

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);

            final TypedArray a = c.obtainStyledAttributes(attrs, ATTRS);
            this.weight = a.getFloat(0, 0);
            a.recycle();
        }

    }

    static class SavedState extends AbsSavedState {
        boolean isOpen;
        @LockMode
        int mLockMode;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in, ClassLoader loader) {
            super(in, loader);
            isOpen = in.readInt() != 0;
            mLockMode = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isOpen ? 1 : 0);
            out.writeInt(mLockMode);
        }

        public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in, ClassLoader loader) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in, null);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    class AccessibilityDelegate extends AccessibilityDelegateCompat {
        private final Rect mTmpRect = new Rect();

        @Override
        public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
            final AccessibilityNodeInfoCompat superNode = AccessibilityNodeInfoCompat.obtain(info);
            super.onInitializeAccessibilityNodeInfo(host, superNode);
            copyNodeInfoNoChildren(info, superNode);
            superNode.recycle();

            info.setClassName(ACCESSIBILITY_CLASS_NAME);
            info.setSource(host);

            final ViewParent parent = ViewCompat.getParentForAccessibility(host);
            if (parent instanceof View) {
                info.setParent((View) parent);
            }

            // This is a best-approximation of addChildrenForAccessibility()
            // that accounts for filtering.
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                if (!filter(child) && (child.getVisibility() == View.VISIBLE)) {
                    // Force importance to "yes" since we can't read the value.
                    ViewCompat.setImportantForAccessibility(
                            child, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    info.addChild(child);
                }
            }
        }

        @Override
        public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(host, event);

            event.setClassName(ACCESSIBILITY_CLASS_NAME);
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child,
                AccessibilityEvent event) {
            if (!filter(child)) {
                return super.onRequestSendAccessibilityEvent(host, child, event);
            }
            return false;
        }

        public boolean filter(View child) {
            return isDimmed(child);
        }

        /**
         * This should really be in AccessibilityNodeInfoCompat, but there unfortunately
         * seem to be a few elements that are not easily cloneable using the underlying API.
         * Leave it private here as it's not general-purpose useful.
         */
        private void copyNodeInfoNoChildren(AccessibilityNodeInfoCompat dest,
                AccessibilityNodeInfoCompat src) {
            final Rect rect = mTmpRect;

            src.getBoundsInScreen(rect);
            dest.setBoundsInScreen(rect);

            dest.setVisibleToUser(src.isVisibleToUser());
            dest.setPackageName(src.getPackageName());
            dest.setClassName(src.getClassName());
            dest.setContentDescription(src.getContentDescription());

            dest.setEnabled(src.isEnabled());
            dest.setClickable(src.isClickable());
            dest.setFocusable(src.isFocusable());
            dest.setFocused(src.isFocused());
            dest.setAccessibilityFocused(src.isAccessibilityFocused());
            dest.setSelected(src.isSelected());
            dest.setLongClickable(src.isLongClickable());

            dest.addAction(src.getActions());

            dest.setMovementGranularities(src.getMovementGranularities());
        }
    }

    private static class TouchBlocker extends FrameLayout {
        TouchBlocker(View view) {
            super(view.getContext());
            addView(view);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return true;
        }

        @Override
        public boolean onGenericMotionEvent(MotionEvent event) {
            return true;
        }
    }

    boolean isLayoutRtlSupport() {
        return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * @return A pair of rects define the position of the split, or {@null} if there is no split
     */
    private ArrayList<Rect> splitViewPositions() {
        if (mFoldingFeature == null || !mFoldingFeature.isSeparating()) {
            return null;
        }

        // Don't support horizontal fold in list-detail view layout
        if (mFoldingFeature.getBounds().left == 0) {
            return null;
        }
        // vertical split
        if (mFoldingFeature.getBounds().top == 0) {
            Rect splitPosition = getFoldBoundsInView(mFoldingFeature, this);
            if (splitPosition == null) {
                return null;
            }
            Rect leftRect = new Rect(getPaddingLeft(), getPaddingTop(),
                    Math.max(getPaddingLeft(), splitPosition.left),
                    getHeight() - getPaddingBottom());
            int rightBound = getWidth() - getPaddingRight();
            Rect rightRect = new Rect(Math.min(rightBound, splitPosition.right),
                    getPaddingTop(), rightBound, getHeight() - getPaddingBottom());
            return new ArrayList<>(Arrays.asList(leftRect, rightRect));
        }
        return null;
    }

    private static Rect getFoldBoundsInView(@NonNull FoldingFeature foldingFeature, View view) {
        int[] viewLocationInWindow = new int[2];
        view.getLocationInWindow(viewLocationInWindow);

        Rect viewRect = new Rect(viewLocationInWindow[0], viewLocationInWindow[1],
                viewLocationInWindow[0] + view.getWidth(),
                viewLocationInWindow[1] + view.getWidth());
        Rect foldRectInView = new Rect(foldingFeature.getBounds());
        // Translate coordinate space of split from window coordinate space to current view
        // position in window
        boolean intersects = foldRectInView.intersect(viewRect);
        // Check if the split is overlapped with the view
        if ((foldRectInView.width() == 0 && foldRectInView.height() == 0) || !intersects) {
            return null;
        }
        foldRectInView.offset(-viewLocationInWindow[0], -viewLocationInWindow[1]);
        return foldRectInView;
    }

    @Nullable
    private static Activity getActivityOrNull(Context context) {
        Context iterator = context;
        while (iterator instanceof ContextWrapper) {
            if (iterator instanceof Activity) {
                return (Activity) iterator;
            }
            iterator = ((ContextWrapper) iterator).getBaseContext();
        }
        return null;
    }
}
