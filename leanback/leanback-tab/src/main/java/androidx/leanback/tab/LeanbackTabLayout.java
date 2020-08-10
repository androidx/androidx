/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.leanback.tab;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

/**
 * {@link TabLayout} with some specific customizations related to focus navigation for TV to be
 * used as
 * top navigation bar. The following modifications have been done on the {@link TabLayout}:
 * <ul>
 * <li> When the focused tab changes the viewpager is also update accordingly. With the default
 *      behavior the viewpager is updated only when tab is clicked. </li>
 * <li> Default behaviour is that focus moves to the tab closest to the last focused item inside
 *      viewpager on DPAD_UP. With the current change the selected tab gets the focus. </li>
 * <li> Allowing change of current tab only when focus changes from an adjacent tab to current
 *      tab or focus changes from an element outside viewpager/tablayout to the
 *      viewpager/tablayout. This prevents change of tabs on DPAD_LEFT on the leftmost element
 *      inside viewpager and DPAD_RIGHT on the rightmost element inside viewpager. </li>
 * </ul>
 *
 * <p> {@link ViewPager} can be used with this class but some of the behaviour of {@link ViewPager}
 * might not be suitable for TV usage. Refer {@link LeanbackViewPager} for the modifications done
 * on {@link ViewPager}.
 */
public class LeanbackTabLayout extends TabLayout {

    ViewPager mViewPager;
    final AdapterDataSetObserver mAdapterDataSetObserver =
            new AdapterDataSetObserver(this);

    /**
     * Constructs LeanbackTabLayout
     * @param context
     */
    public LeanbackTabLayout(@NonNull Context context) {
        super(context);
    }

    /**
     * Constructs LeanbackTabLayout
     * @param context
     * @param attrs
     */
    public LeanbackTabLayout(@NonNull Context context, @NonNull AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Constructs LeanbackTabLayout
     * @param context
     * @param attrs
     * @param defStyleAttr
     */
    public LeanbackTabLayout(@NonNull Context context, @NonNull AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updatePageTabs();
    }

    @Override
    public void setupWithViewPager(@Nullable ViewPager viewPager) {
        super.setupWithViewPager(viewPager);
        if (this.mViewPager != null && this.mViewPager.getAdapter() != null) {
            this.mViewPager.getAdapter().unregisterDataSetObserver(mAdapterDataSetObserver);
        }
        this.mViewPager = viewPager;
        if (this.mViewPager != null && this.mViewPager.getAdapter() != null) {
            this.mViewPager.getAdapter().registerDataSetObserver(mAdapterDataSetObserver);
        }
    }

    @Override
    public void addFocusables(@SuppressLint("ConcreteCollection") @NonNull ArrayList<View> views,
            int direction, int focusableMode) {

        boolean isViewPagerFocused = this.mViewPager != null && this.mViewPager.hasFocus();
        boolean isCurrentlyFocused = this.hasFocus();
        LinearLayout tabStrip = (LinearLayout) this.getChildAt(0);
        if ((direction == View.FOCUS_DOWN || direction == View.FOCUS_UP)
                && tabStrip != null && tabStrip.getChildCount() > 0 && this.mViewPager != null) {
            View selectedTab =  tabStrip.getChildAt(this.mViewPager.getCurrentItem());
            if (selectedTab != null) {
                views.add(selectedTab);
            }
        } else if ((direction == View.FOCUS_RIGHT || direction == View.FOCUS_LEFT)
                && !isCurrentlyFocused && isViewPagerFocused) {
            return;
        } else {
            super.addFocusables(views, direction, focusableMode);
        }
    }

    void updatePageTabs() {
        LinearLayout tabStrip = (LinearLayout) this.getChildAt(0);

        if (tabStrip == null) {
            return;
        }

        int tabCount = tabStrip.getChildCount();
        for (int i = 0; i < tabCount; i++) {
            final View tabView = tabStrip.getChildAt(i);
            tabView.setFocusable(true);
            tabView.setOnFocusChangeListener(
                    new TabFocusChangeListener(this, this.mViewPager));
        }
    }

    private static class AdapterDataSetObserver extends DataSetObserver {

        final LeanbackTabLayout mLeanbackTabLayout;

        AdapterDataSetObserver(LeanbackTabLayout leanbackTabLayout) {
            mLeanbackTabLayout = leanbackTabLayout;
        }

        @Override
        public void onChanged() {
            mLeanbackTabLayout.updatePageTabs();
        }

        @Override
        public void onInvalidated() {
            mLeanbackTabLayout.updatePageTabs();
        }
    }
}
