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
package android.support.v7.internal.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.appcompat.R;
import android.support.v7.internal.view.menu.ActionMenuPresenter;
import android.support.v7.internal.view.menu.ActionMenuView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

abstract class AbsActionBarView extends ViewGroup {

    protected ActionMenuView mMenuView;

    protected ActionMenuPresenter mActionMenuPresenter;

    protected ActionBarContainer mSplitView;

    protected boolean mSplitActionBar;

    protected boolean mSplitWhenNarrow;

    protected int mContentHeight;

    private static final int FADE_DURATION = 200;

    AbsActionBarView(Context context) {
        super(context);
    }

    AbsActionBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    AbsActionBarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (Build.VERSION.SDK_INT >= 8) {
            super.onConfigurationChanged(newConfig);
        }

        // Action bar can change size on configuration changes.
        // Reread the desired height from the theme-specified style.
        TypedArray a = getContext().obtainStyledAttributes(null, R.styleable.ActionBar,
                R.attr.actionBarStyle, 0);
        setContentHeight(a.getLayoutDimension(R.styleable.ActionBar_height, 0));
        a.recycle();
        if (mSplitWhenNarrow) {
            setSplitActionBar(getContext().getResources().getBoolean(
                    R.bool.abc_split_action_bar_is_narrow));
        }
        if (mActionMenuPresenter != null) {
            mActionMenuPresenter.onConfigurationChanged(newConfig);
        }
    }

    /**
     * Sets whether the bar should be split right now, no questions asked.
     *
     * @param split true if the bar should split
     */
    public void setSplitActionBar(boolean split) {
        mSplitActionBar = split;
    }

    /**
     * Sets whether the bar should split if we enter a narrow screen configuration.
     *
     * @param splitWhenNarrow true if the bar should check to split after a config change
     */
    public void setSplitWhenNarrow(boolean splitWhenNarrow) {
        mSplitWhenNarrow = splitWhenNarrow;
    }

    public void setContentHeight(int height) {
        mContentHeight = height;
        requestLayout();
    }

    public int getContentHeight() {
        return mContentHeight;
    }

    public void setSplitView(ActionBarContainer splitView) {
        mSplitView = splitView;
    }

    /**
     * @return Current visibility or if animating, the visibility being animated to.
     */
    public int getAnimatedVisibility() {
        return getVisibility();
    }

    public void animateToVisibility(int visibility) {
        clearAnimation();

        if (visibility != getVisibility()) {
            Animation anim = AnimationUtils.loadAnimation(getContext(),
                    visibility == View.VISIBLE ? R.anim.abc_fade_in : R.anim.abc_fade_out);

            startAnimation(anim);
            setVisibility(visibility);

            if (mSplitView != null && mMenuView != null) {
                mMenuView.startAnimation(anim);
                mMenuView.setVisibility(visibility);
            }
        }
    }

    @Override
    public void setVisibility(int visibility) {
        if (visibility != getVisibility()) {
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

    public boolean isOverflowReserved() {
        return mActionMenuPresenter != null && mActionMenuPresenter.isOverflowReserved();
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

    protected int positionChild(View child, int x, int y, int contentHeight) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        child.layout(x, childTop, x + childWidth, childTop + childHeight);

        return childWidth;
    }

    protected int positionChildInverse(View child, int x, int y, int contentHeight) {
        int childWidth = child.getMeasuredWidth();
        int childHeight = child.getMeasuredHeight();
        int childTop = y + (contentHeight - childHeight) / 2;

        child.layout(x - childWidth, childTop, x, childTop + childHeight);

        return childWidth;
    }

}
