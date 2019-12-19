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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent2;
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
 * NestedScrollingChild2 implementation.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SwipeRefreshLayoutNestedScrollingChild2Test {

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private NestedScrollingSpyView mParent;

    @Before
    public void instantiateMembers() {
        mSwipeRefreshLayout = new SwipeRefreshLayout(ApplicationProvider.getApplicationContext());
        mParent = spy(new NestedScrollingSpyView(ApplicationProvider.getApplicationContext()));
    }

    @Test
    public void startNestedScroll_noParent_returnsFalse() {
        setupSwipeRefreshLayoutWithNoParent();
        boolean actual = mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        assertThat(actual, is(false));
    }

    @Test
    public void startNestedScroll_parentRejects_parentCalledCorrectlyReturnsFalse() {
        startNestedScroll(false, true, true, false);
    }

    @Test
    public void startNestedScroll_typeNotTouch_parentNotCalledReturnsFalse() {
        startNestedScroll(true, false, false, false);
    }

    @Test
    public void startNestedScroll_parentCalledCorrectlyAndReturnsTrue() {
        startNestedScroll(true, true, true, true);
    }

    private void startNestedScroll(boolean parentAccepts, boolean typeTouch, boolean parentCalled,
            boolean expectedReturnValue) {
        setupSwipeRefreshLayoutWithParent();
        doReturn(parentAccepts).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                anyInt(), anyInt());

        boolean actual = mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                typeTouch ? ViewCompat.TYPE_TOUCH : ViewCompat.TYPE_NON_TOUCH);

        if (parentCalled) {
            verify(mParent).onStartNestedScroll(mSwipeRefreshLayout, mSwipeRefreshLayout,
                    ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        } else {
            verify(mParent, never()).onStartNestedScroll(any(View.class), any(View.class),
                    anyInt(), anyInt());
        }

        assertThat(actual, is(expectedReturnValue));
    }

    @Test
    public void stopNestedScroll_typeTouch_parentCalledCorrectly() {
        setupSwipeRefreshLayoutWithParent();
        doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                anyInt(), anyInt());
        mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.stopNestedScroll(ViewCompat.TYPE_TOUCH);

        verify(mParent).onStopNestedScroll(mSwipeRefreshLayout, ViewCompat.TYPE_TOUCH);
    }

    @Test
    public void stopNestedScroll_typeNotTouch_parentNotCalled() {
        setupSwipeRefreshLayoutWithParent();
        doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                anyInt(), anyInt());
        mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        mSwipeRefreshLayout.stopNestedScroll(ViewCompat.TYPE_NON_TOUCH);

        verify(mParent, never()).onStopNestedScroll(any(View.class), anyInt());
    }

    @Test
    public void hasNestedScrollingParent_noParent_returnsFalse() {
        setupSwipeRefreshLayoutWithNoParent();
        boolean actual = mSwipeRefreshLayout.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
        assertThat(actual, is(false));
    }

    @Test
    public void hasNestedScrollingParent_parentRejectsStart_returnsFalse() {
        hasNestedScrollingParent(false, true, false, false);
    }

    @Test
    public void hasNestedScrollingParent_typeIsNotTouch_returnsFalse() {
        hasNestedScrollingParent(true, false, false, false);
    }

    @Test
    public void hasNestedScrollingParent_typeIsTouchParentAcceptsStart_returnsTrue() {
        hasNestedScrollingParent(true, true, false, true);
    }

    @Test
    public void hasNestedScrollingParent_parentAcceptsStartThenStop_returnsFalse() {
        hasNestedScrollingParent(true, true, true, false);
    }

    private void hasNestedScrollingParent(boolean parentAccepts, boolean typeIsTouch,
            boolean stopCalled, boolean expectedReturnValue) {
        setupSwipeRefreshLayoutWithParent();
        doReturn(parentAccepts).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                anyInt(), anyInt());
        mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                typeIsTouch ? ViewCompat.TYPE_TOUCH : ViewCompat.TYPE_NON_TOUCH);
        if (stopCalled) {
            mSwipeRefreshLayout.stopNestedScroll(ViewCompat.TYPE_TOUCH);
        }

        boolean actual = mSwipeRefreshLayout.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
        assertThat(actual, is(expectedReturnValue));
    }

    @Test
    public void dispatchNestedScroll_noParent_returnsFalse() {
        setupSwipeRefreshLayoutWithNoParent();
        boolean actual = mSwipeRefreshLayout.dispatchNestedScroll(1, 2, 3, 4, new int[]{5, 6},
                ViewCompat.TYPE_TOUCH);
        assertThat(actual, is(false));
    }

    @Test
    public void dispatchNestedScroll_parentRejects_parentNotCalledReturnsFalse() {
        dispatchNestedScroll(false, true, false, false);
    }

    @Test
    public void dispatchNestedScroll_typeNotTouch_parentNotCalledReturnsFalse() {
        dispatchNestedScroll(true, false, false, false);
    }

    @Test
    public void dispatchNestedScroll_parentCalledCorrectlyAndReturnsTrue() {
        dispatchNestedScroll(true, true, true, true);
    }

    private void dispatchNestedScroll(boolean parentReturnsTrueForStarted, boolean callIsTypeTouch,
            boolean mParentCalled, boolean expectedReturnValue) {

        // Arrange

        setupSwipeRefreshLayoutWithParent();
        doReturn(parentReturnsTrueForStarted).when(mParent).onStartNestedScroll(any(View.class),
                any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        // Act

        boolean actual = mSwipeRefreshLayout.dispatchNestedScroll(1, 2, 3, 4, new int[]{5, 6},
                callIsTypeTouch ? ViewCompat.TYPE_TOUCH : ViewCompat.TYPE_NON_TOUCH);

        // Assert

        if (mParentCalled) {
            verify(mParent).onNestedScroll(mSwipeRefreshLayout, 1, 2, 3, 4, ViewCompat.TYPE_TOUCH);
        } else {
            verify(mParent, never()).onNestedScroll(any(View.class), anyInt(), anyInt(), anyInt(),
                    anyInt(), anyInt());
        }
        assertThat(actual, is(expectedReturnValue));
    }

    @Test
    public void dispatchNestedPreScroll_noParent_returnsFalse() {
        setupSwipeRefreshLayoutWithNoParent();
        boolean actual = mSwipeRefreshLayout.dispatchNestedPreScroll(1, 2, new int[]{3, 4},
                new int[]{5, 6}, ViewCompat.TYPE_TOUCH);
        assertThat(actual, is(false));
    }

    @Test
    public void dispatchNestedPreScroll_parentRejects_parentNotCalledReturnsFalse() {
        dispatchPreNestedScroll(false, true, true, false, false);
    }

    @Test
    public void dispatchNestedPreScroll_typeNotTouch_parentNotCalledReturnsFalse() {
        dispatchPreNestedScroll(true, false, true, false, false);
    }

    @Test
    public void dispatchPreNestedScroll_parentDoesNotConsume_parentCalledCorrectlyReturnsFalse() {
        dispatchPreNestedScroll(true, true, false, true, false);
    }

    @Test
    public void dispatchPreNestedScroll_parentCalledCorrectlyAndReturnsTrue() {
        dispatchPreNestedScroll(true, true, true, true, true);
    }

    private void dispatchPreNestedScroll(boolean parentReturnsTrueForStarted,
            boolean callIsTypeTouch, final boolean parentConsumes, boolean mParentCalled,
            boolean expected) {

        // Arrange

        setupSwipeRefreshLayoutWithParent();
        doReturn(parentReturnsTrueForStarted).when(mParent).onStartNestedScroll(any(View.class),
                any(View.class), anyInt(), anyInt());
        mSwipeRefreshLayout.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        // Mutate consumed in call to onNestedPreScroll.
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                int[] consumed = (int[]) invocation.getArguments()[3];

                // Have to assert here because asserting at the end fails due to mutation.
                assertThat(consumed[0], is(0));
                assertThat(consumed[1], is(0));

                consumed[0] = 0;
                consumed[1] = parentConsumes ? 1 : 0;
                return null;
            }}).when(mParent)
                .onNestedPreScroll(any(View.class), anyInt(), anyInt(), any(int[].class), anyInt());

        // Act

        boolean actual = mSwipeRefreshLayout.dispatchNestedPreScroll(1, 2, new int[]{3, 4},
                new int[]{5, 6},
                callIsTypeTouch ? ViewCompat.TYPE_TOUCH : ViewCompat.TYPE_NON_TOUCH);

        // Assert

        if (mParentCalled) {
            // Asserts for int[] occur in 'doAnswer' which must be done due to mutation.
            verify(mParent).onNestedPreScroll(eq(mSwipeRefreshLayout), eq(1), eq(2),
                    any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        } else {
            verify(mParent, never()).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                    any(int[].class), anyInt());
        }

        assertThat(actual, is(expected));
    }

    private void setupSwipeRefreshLayoutWithParent() {
        mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 100));
        mSwipeRefreshLayout.setMinimumHeight(100);

        mParent.addView(mSwipeRefreshLayout);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        mParent.measure(widthMeasureSpec, heightMeasureSpec);
        mParent.layout(0, 0, 100, 100);
    }

    private void setupSwipeRefreshLayoutWithNoParent() {
        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY);
        mSwipeRefreshLayout.measure(widthMeasureSpec, heightMeasureSpec);
        mSwipeRefreshLayout.layout(0, 0, 100, 100);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingParent2 {

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
    }

}
