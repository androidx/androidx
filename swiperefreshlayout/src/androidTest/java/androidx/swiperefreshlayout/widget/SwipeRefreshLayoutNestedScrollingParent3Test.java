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

package androidx.swiperefreshlayout.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Small integration tests that verifies correctness of {@link SwipeRefreshLayout}'s
 * NestedScrollingParent3 implementation.
 *
 * One area that isn't tested (due to the current difficulty in doing so) is testing that the actual
 * refresh circle is pulled onto the screen when it should be.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SwipeRefreshLayoutNestedScrollingParent3Test {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NestedScrollingSpyView mParent;
    private View mChild;

    @Before
    public void instantiateMembers() {
        mSwipeRefreshLayout = new SwipeRefreshLayout(ApplicationProvider.getApplicationContext());
        mParent = spy(new NestedScrollingSpyView(ApplicationProvider.getApplicationContext()));
        mChild = spy(new View(ApplicationProvider.getApplicationContext()));
    }

    @Test
    public void onNestedScroll_parentOnNestedScrollCalledWithCorrectParams() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.onNestedScroll(
                mChild,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH,
                new int[]{5, 6});

        verify(mParent, times(1)).onNestedScroll(
                mSwipeRefreshLayout,
                1,
                2,
                3,
                4,
                ViewCompat.TYPE_TOUCH,
                new int[]{5, 6});
        verify(mParent, times(1)).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
    }

    @Test
    public void onNestedScroll_parentRejects_parentOnNestedScrollNotCalledNothingConsumed() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(false)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);

        int[] consumed = new int[]{5, 6};
        mSwipeRefreshLayout.onNestedScroll(mChild, 1, 2, 3, 4, ViewCompat.TYPE_TOUCH,
                new int[]{5, 6});

        verify(mParent, never()).onNestedScroll(any(View.class), anyInt(), anyInt(),
                anyInt(), anyInt(), anyInt(), any(int[].class));
        assertThat(consumed, is(new int[]{5, 6}));
    }

    @Test
    public void onNestedScroll_upward_noScrollDistanceConsumed() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild, ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        int[] consumed = new int[]{5, 6};

        mSwipeRefreshLayout.onNestedScroll(mChild, 0, 0, 0, 10, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, is(new int[]{5, 6}));
    }

    @Test
    public void onNestedScroll_downwardParentConsumesSomeChildCantScroll_allDistanceConsumed() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(false)
                .when(mChild)
                .canScrollVertically(anyInt());
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                int[] consumed = (int[]) invocation.getArguments()[6];
                consumed[1] += -3;
                return null;
            }})
                .when(mParent)
                .onNestedScroll(any(View.class), anyInt(), anyInt(), anyInt(), anyInt(),  anyInt(),
                        any(int[].class));
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        int[] consumed = new int[]{0, -2};

        mSwipeRefreshLayout.onNestedScroll(mChild, 0, 0, 0, -10, ViewCompat.TYPE_TOUCH,
                consumed);

        assertThat(consumed, is(new int[]{0, -12}));
    }

    @Test
    public void onNestedScroll_downwardParentConsumesSomeChildCanScroll_noMoreDistanceConsumed() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mChild)
                .canScrollVertically(anyInt());
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                int[] consumed = (int[]) invocation.getArguments()[6];
                consumed[1] += -3;
                return null;
            }})
                .when(mParent)
                .onNestedScroll(any(View.class), anyInt(), anyInt(), anyInt(), anyInt(),  anyInt(),
                        any(int[].class));
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        int[] consumed = new int[]{0, -2};

        mSwipeRefreshLayout.onNestedScroll(mChild, 0, 0, 0, -10, ViewCompat.TYPE_TOUCH,
                consumed);

        assertThat(consumed, is(new int[]{0, -5}));
    }

    @Test
    public void onNestedScroll_downwardParentConsumesAll_allDistanceConsumed() {
        setupNestedScrollViewWithParentAndChild();
        doReturn(true)
                .when(mParent)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                int[] consumed = (int[]) invocation.getArguments()[6];
                consumed[1] += -10;
                return null;
            }})
                .when(mParent)
                .onNestedScroll(any(View.class), anyInt(), anyInt(), anyInt(), anyInt(),  anyInt(),
                        any(int[].class));
        mSwipeRefreshLayout.onNestedScrollAccepted(mChild, mChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        int[] consumed = new int[]{0, -2};

        mSwipeRefreshLayout.onNestedScroll(mChild, 0, 0, 0, -10, ViewCompat.TYPE_TOUCH,
                consumed);

        assertThat(consumed, is(new int[]{0, -12}));
    }

    private void setupNestedScrollViewWithParentAndChild() {

        mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));
        mSwipeRefreshLayout.setMinimumHeight(100);

        mChild.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));
        mChild.setMinimumHeight(100);

        mSwipeRefreshLayout.addView(mChild);
        mParent.addView(mSwipeRefreshLayout);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec =
                View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        mParent.measure(widthMeasureSpec, heightMeasureSpec);
        mParent.layout(0, 0, 100, 100);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingParent3 {

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
        public boolean onStartNestedScroll(View child, View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(View target) {

        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {

        }

        @Override
        public boolean onNestedFling(View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int type, int[] consumed) {

        }
    }
}
