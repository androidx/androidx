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

package androidx.coordinatorlayout.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Small integration tests that verify that {@link CoordinatorLayout} works correctly with older
 * nested scrolling implementations.  This includes methods that are rendered obsolete by
 * nested scrolling v2 or v3, and deprecated methods on {@link CoordinatorLayout.Behavior} that
 * are related to nested scrolling.
 *
 * This test currently only covers calls to
 * {@link CoordinatorLayout#onNestedScroll(View, int, int, int, int, int)} and
 * {@link CoordinatorLayout.Behavior#onNestedScroll(View, int, int, int, int, int)}
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CoordinatorLayoutNestedScrollingParentCompatTest {

    private static final int WIDTH_AND_HEIGHT = 500;

    private CoordinatorLayout mCoordinatorLayout;
    private CoordinatorLayout.Behavior<View> mSpyBehavior;
    private View mBehaviorChild;
    private View mChild;

    @Test
    public void onNestedScroll_callIsV3BehaviorIsV2_behaviorV2VersionCalledCorrectly() {
        setup(false);
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH,
                new int[]{1, 2});
        verify(mSpyBehavior).onNestedScroll(mCoordinatorLayout, mBehaviorChild, mChild, 3, 4, 5,
                6, ViewCompat.TYPE_TOUCH);
    }

    @Test
    public void onNestedScroll_callIsV3BehaviorIsV2_allDistanceConsumed() {
        setup(false);
        int[] consumed = new int[]{10, 20};
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH,
                consumed);
        assertThat(consumed, is(new int[]{15, 26}));
    }

    @Test
    public void onNestedScroll_callIsV2behaviorIsV3_behaviorV3VersionCalledCorrectly() {
        setup(true);
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH);
        verify(mSpyBehavior).onNestedScroll(eq(mCoordinatorLayout), eq(mBehaviorChild), eq(mChild),
                eq(3), eq(4), eq(5), eq(6), eq(ViewCompat.TYPE_TOUCH), any(int[].class));
    }

    @Test
    public void onNestedScroll_callIsV2BehaviorIsV2_behaviorV2VersionCalledCorrectly() {
        setup(false);
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH);
        verify(mSpyBehavior).onNestedScroll(mCoordinatorLayout, mBehaviorChild, mChild, 3, 4, 5,
                6, ViewCompat.TYPE_TOUCH);
    }

    @Test
    public void onNestedScroll_callIsV2BehaviorIsV3_behaviorV2IsNotCalled() {
        setup(true);
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH);
        verify(mSpyBehavior, never()).onNestedScroll(any(CoordinatorLayout.class), any(View.class),
                any(View.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void onNestedScroll_callIsV3BehaviorIsV3_behaviorV2IsNotCalled() {
        setup(true);
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH, new int[2]);
        verify(mSpyBehavior, never()).onNestedScroll(any(CoordinatorLayout.class), any(View.class),
                any(View.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    private void setup(boolean behaviorIsV3) {
        Context context = ApplicationProvider.getApplicationContext();

        mSpyBehavior = spy(behaviorIsV3 ? new V3TestBehavior() : new NonV3TestBehavior());
        when(mSpyBehavior.onStartNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt()))
                .thenReturn(true);

        CoordinatorLayout.LayoutParams layoutParams1 =
                new CoordinatorLayout.LayoutParams(WIDTH_AND_HEIGHT, WIDTH_AND_HEIGHT);
        layoutParams1.setBehavior(mSpyBehavior);

        mBehaviorChild = new View(context);
        mBehaviorChild.setLayoutParams(layoutParams1);

        mChild = new View(context);

        mCoordinatorLayout = new CoordinatorLayout(context);
        mCoordinatorLayout.addView(mBehaviorChild);
        mCoordinatorLayout.addView(mChild);

        int measureSpec =
                View.MeasureSpec.makeMeasureSpec(WIDTH_AND_HEIGHT, View.MeasureSpec.EXACTLY);
        mCoordinatorLayout.measure(measureSpec, measureSpec);
        mCoordinatorLayout.layout(0, 0, WIDTH_AND_HEIGHT, WIDTH_AND_HEIGHT);

        mCoordinatorLayout.onStartNestedScroll(mBehaviorChild, mBehaviorChild,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
    }

    public static class NonV3TestBehavior extends CoordinatorLayout.Behavior<View> {

    }

    public static class V3TestBehavior extends CoordinatorLayout.Behavior<View> {
        @Override
        public void onNestedScroll(CoordinatorLayout coordinatorLayout, View child, View target,
                int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type,
                int[] consumed) {
        }
    }


}
