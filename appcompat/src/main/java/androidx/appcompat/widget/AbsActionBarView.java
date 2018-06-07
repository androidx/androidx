/*
 * Copyright (C) 2011 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.R;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;
import androidx.core.view.ViewPropertyAnimatorListener;

abstract class AbsActionBarView extends ViewGroup {
    private static final int FADE_DURATION = 200;

    protected final VisibilityAnimListener mVisAnimListener = new VisibilityAnimListener();

    /** Context against which to inflate popup menus. */
    protected final Context mPopupContext;

    protected ActionMenuView mMenuView;
    protected ActionMenuPresenter mActionMenuPresenter;
    protected int mContentHeight;

    protected ViewPropertyAnimatorCompat mVisibilityAnim;

    private boolean mEatingTouch;
    private boolean mEatingHover;

    AbsActionBarView(Context context) {
        this(context, null);
    }

    AbsActionBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    AbsActionBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarPopupTheme, tv, true)
                && tv.resourceId != 0) {
            mPopupContext = new ContextThemeWrapper(context, tv.resourceId);
        } else {
            mPopupContext = context;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Action bar can change size on configuration changes.
        // Reread the desired height from the theme-specified style.
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.ActionBar_height, 0));
        a.recycle();

        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // ActionBarViews always eat touch events, but should still respect the touch event dispatch
        // contract. If the normal View implementation doesn't want the events, we'll just silently
        // eat the rest of the gesture without reporting the events to the default implementation
        // since that's what it expects.

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            mEatingTouch = false;
        }

        if (!mEatingTouch) {
            final boolean handled = super.onTouchEvent(ev);
            if (action == MotionEvent.ACTION_DOWN && !handled) {
                mEatingTouch = true;
            }
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mEatingTouch = false;
        }

        return true;
    }

    @Override
    public boolean onHoverEvent(MotionEvent ev) {
        // Same deal as onTouchEvent() above. Eat all hover events, but still
        // respect the touch event dispatch contract.

        final int action = ev.getActionMasked();
        if (action == MotionEvent.ACTION_HOVER_ENTER) {
            mEatingHover = false;
        }

        if (!mEatingHover) {
            final boolean handled = super.onHoverEvent(ev);
            if (action == MotionEvent.ACTION_HOVER_ENTER && !handled) {
                mEatingHover = true;
            }
        }

        if (action == MotionEvent.ACTION_HOVER_EXIT
                || action == MotionEvent.ACTION_CANCEL) {
            mEatingHover = false;
        }

        return true;
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
        requestLayout();
    }

    public int getContentHeight() {
        return mContentHeight;
    }

    /**
     * @return Current visibility or if animating, the visibility being animated to.
     */
    public int getAnimatedVisibility() {
        if (mVisibilityAnim != null) {
            return mVisAnimListener.mFinalVisibility;
        }
        return getVisibility();
    }

    public ViewPropertyAnimatorCompat setupAnimatorToVisibility(int visibility, long duration) {
        if (mVisibilityAnim != null) {
            mVisibilityAnim.cancel();
        }

        if (visibility == VISIBLE) {
            if (getVisibility() != VISIBLE) {
                setAlpha(0f);
            }
            ViewPropertyAnimatorCompat anim = ViewCompat.animate(this).alpha(1f);
            anim.setDuration(duration);
            anim.setListener(mVisAnimListener.withFinalVisibility(anim, visibility));
            return anim;
        } else {
            ViewPropertyAnimatorCompat anim = ViewCompat.animate(this).alpha(0f);
            anim.setDuration(duration);
            anim.setListener(mVisAnimListener.withFinalVisibility(anim, visibility));
            return anim;
        }
    }

    public void animateToVisibility(int visibility) {
        ViewPropertyAnimatorCompat anim = setupAnimatorToVisibility(visibility, FADE_DURATION);
        anim.start();
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility != getVisibility()) {
            if (mVisibilityAnim != null) {
                mVisibilityAnim.cancel();
            }
            super.setVisibility(visibility);
        }
    }

    public boolean showOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.showOverflowMenu();
        }
        return false;
    }

    public void postShowOverflowMenu() {
        post(new Runnable() {
            @Override
            public void run() {
                showOverflowMenu();
            }
        });
    }

    public boolean hideOverflowMenu() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.hideOverflowMenu();
        }
        return false;
    }

    public boolean isOverflowMenuShowing() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowing();
        }
        return false;
    }

    public boolean isOverflowMenuShowPending() {
        if (mActionMenuPresenter != null) {
            return mActionMenuPresenter.isOverflowMenuShowPending();
        }
        return false;
    }

    public boolean isOverflowReserved() {
        return mActionMenuPresenter != null && mActionMenuPresenter.isOverflowReserved();
    }

    public boolean canShowOverflowMenu() {
        return isOverflowReserved() && getVisibility() == VISIBLE;
    }

    public void dismissPopupMenus() {
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.dismissPopupMenus();
        }
    }

    protected int measureChildView(View child, int availableWidth, int childSpecHeight,
            int spacing) {
        child.measure(MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.AT_MOST),
                childSpecHeight);

        availableWidth -= child.getMeasuredWidth();
        availableWidth -= spacing;

        return Math.max(0, availableWidth);
    }

    static protected int next(int x, int val, boolean isRtl) {
        return isRtl ? x - val : x + val;
    }

    protected int positionChild(View child, int x, int y, int contentHeight, boolean reverse) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        if (reverse) {
            child.layout(x - childWidth, childTop, x, childTop + childHeight);
        } else {
            child.layout(x, childTop, x + childWidth, childTop + childHeight);
        }

        return  (reverse ? -childWidth : childWidth);
    }

    protected class VisibilityAnimListener implements ViewPropertyAnimatorListener {
        private boolean mCanceled = false;
        int mFinalVisibility;

        public VisibilityAnimListener withFinalVisibility(ViewPropertyAnimatorCompat animation,
                int visibility) {
            mVisibilityAnim = animation;
            mFinalVisibility = visibility;
            return this;
        }

        @Override
        public void onAnimationStart(View view) {
            AbsActionBarView.super.setVisibility(VISIBLE);
            mCanceled = false;
        }

        @Override
        public void onAnimationEnd(View view) {
            if (mCanceled) return;

            mVisibilityAnim = null;
            AbsActionBarView.super.setVisibility(mFinalVisibility);
        }

        @Override
        public void onAnimationCancel(View view) {
            mCanceled = true;
        }
    }
}
