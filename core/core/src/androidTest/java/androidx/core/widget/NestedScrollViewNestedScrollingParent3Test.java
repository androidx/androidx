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

package androidx.core.widget;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Small integration tests that verify that {@link NestedScrollView} implements
 * {@link NestedScrollingParent3} correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewNestedScrollingParent3Test {

    private NestedScrollView mNestedScrollView;
    private NestedScrollingSpyView mParent;
    private View mChild;

    @Test
    public void onNestedScrollView_canScrollDownEntireDistance_consumedIsUpdatedCorrectly() {
        setupNestedScrollViewWithChild(500, 1000);
        int[] consumed = new int[]{0, 0};

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                500, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed[0], equalTo(0));
        assertThat(consumed[1], equalTo(500));
    }

    @Test
    public void onNestedScrollView_canScrollDownPartDistance_consumedIsUpdatedCorrectly() {
        setupNestedScrollViewWithChild(500, 750);
        int[] consumed = new int[]{0, 0};

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                500, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed[0], equalTo(0));
    }

    @Test
    public void onNestedScrollView_cantScrollDown_consumedIsUpdatedCorrectly() {
        setupNestedScrollViewWithChild(500, 500);
        int[] consumed = new int[]{0, 0};

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                500, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed[0], equalTo(0));
        assertThat(consumed[1], equalTo(0));
    }

    @Test
    public void onNestedScrollView_canScrollUpEntireDistance_consumedIsUpdatedCorrectly() {
        setupNestedScrollViewWithChild(500, 1000);
        int[] consumed = new int[]{0, 0};
        mNestedScrollView.scrollBy(0, 500);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                -500, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed[0], equalTo(0));
        assertThat(consumed[1], equalTo(-500));
    }

    @Test
    public void onNestedScrollView_canScrollUpPartDistance_consumedIsUpdatedCorrectly() {
        setupNestedScrollViewWithChild(500, 750);
        int[] consumed = new int[]{0, 0};
        mNestedScrollView.scrollBy(0, 250);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                -500, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed[0], equalTo(0));
        assertThat(consumed[1], equalTo(-250));
    }

    @Test
    public void onNestedScrollView_cantScrollUp_consumedIsUpdatedCorrectly() {
        setupNestedScrollViewWithChild(500, 500);
        int[] consumed = new int[]{0, 0};

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                -500, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed[0], equalTo(0));
        assertThat(consumed[1], equalTo(0));
    }

    @Test
    public void onNestedScrollView_childAbsorbsAllScroll_nestedScrollViewPassesCorrectToParent() {
        setupNestedScrollViewWithParentAndChild(500, 500, 1000);
        int[] consumed = new int[]{0, 0};
        doReturn(true).when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                500, ViewCompat.TYPE_TOUCH, consumed);

        verify(mParent).onNestedScroll(
                eq(mNestedScrollView),
                eq(0),
                eq(500),
                eq(0),
                eq(0),
                eq(ViewCompat.TYPE_TOUCH),
                eq(new int[]{0, 500}));
    }

    @Test
    public void onNestedScrollView_childAbsorbsHalfScroll_nestedScrollViewPassesCorrectToParent() {
        setupNestedScrollViewWithParentAndChild(500, 750, 1000);
        int[] consumed = new int[]{0, 0};
        doReturn(true).when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                500, ViewCompat.TYPE_TOUCH, consumed);

        verify(mParent).onNestedScroll(
                eq(mNestedScrollView),
                eq(0),
                eq(250),
                eq(0),
                eq(250),
                eq(ViewCompat.TYPE_TOUCH),
                eq(new int[]{0, 250}));
    }

    @Test
    public void onNestedScrollView_childAbsorbsNoScroll_nestedScrollViewPassesCorrectToParent() {
        setupNestedScrollViewWithParentAndChild(500, 1000, 1000);
        int[] consumed = new int[]{0, 0};
        doReturn(true).when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0,
                500, ViewCompat.TYPE_TOUCH, consumed);

        verify(mParent).onNestedScroll(
                eq(mNestedScrollView),
                eq(0),
                eq(0),
                eq(0),
                eq(500),
                eq(ViewCompat.TYPE_TOUCH),
                eq(new int[]{0, 0}));
    }

    @Test
    public void onNestedScroll_nsvScrolls() {
        setupNestedScrollViewWithParentAndChild(50, 50, 100);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollView.getScrollY(), is(50));
    }

    @Test
    public void onNestedScroll_canOnlyScrollPartWay_nsvScrollsPartWay() {
        setupNestedScrollViewWithParentAndChild(50, 50, 75);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, 50, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollView.getScrollY(), is(25));
    }

    @Test
    public void onNestedScroll_negativeScroll_nsvScrollsNegative() {
        setupNestedScrollViewWithParentAndChild(50, 50, 100);
        mNestedScrollView.scrollTo(0, 50);

        mNestedScrollView.onNestedScroll(mChild, 0, 0, 0, -50, ViewCompat.TYPE_NON_TOUCH);

        assertThat(mNestedScrollView.getScrollY(), is(0));
    }

    @SuppressWarnings("SameParameterValue")
    private void setupNestedScrollViewWithChild(int nestedScrollViewHeight, int childHeight) {

        mNestedScrollView = new NestedScrollView(ApplicationProvider.getApplicationContext());

        mChild = new View(ApplicationProvider.getApplicationContext());
        mChild.setMinimumHeight(childHeight);
        mChild.setLayoutParams(
                new NestedScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, childHeight));

        mNestedScrollView.addView(mChild);

        int measureSpec =
                View.MeasureSpec.makeMeasureSpec(nestedScrollViewHeight, View.MeasureSpec.EXACTLY);
        mNestedScrollView.measure(measureSpec, measureSpec);
        mNestedScrollView.layout(0, 0, nestedScrollViewHeight, nestedScrollViewHeight);
    }

    @SuppressWarnings("SameParameterValue")
    private void setupNestedScrollViewWithParentAndChild(int parentHeight,
            int nestedScrollViewHeight, int childHeight) {

        mParent = spy(new NestedScrollingSpyView(ApplicationProvider.getApplicationContext()));
        mParent.setMinimumHeight(parentHeight);

        mNestedScrollView = new NestedScrollView(ApplicationProvider.getApplicationContext());
        mNestedScrollView.setMinimumHeight(nestedScrollViewHeight);
        mNestedScrollView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, nestedScrollViewHeight));

        mChild = new View(ApplicationProvider.getApplicationContext());
        mChild.setMinimumHeight(childHeight);
        mChild.setLayoutParams(
                new NestedScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, childHeight));

        mNestedScrollView.addView(mChild);
        mParent.addView(mNestedScrollView);

        int measureSpec =
                View.MeasureSpec.makeMeasureSpec(nestedScrollViewHeight, View.MeasureSpec.EXACTLY);
        mNestedScrollView.measure(measureSpec, measureSpec);
        mNestedScrollView.layout(0, 0, nestedScrollViewHeight, nestedScrollViewHeight);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public NestedScrollingSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {

        }

        @Override
        public boolean startNestedScroll(int axes, int type) {
            return false;
        }

        @Override
        public void stopNestedScroll(int type) {

        }

        @Override
        public boolean hasNestedScrollingParent(int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @Nullable int[] consumed) {
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @NonNull int[] consumed) {
        }
    }

}
