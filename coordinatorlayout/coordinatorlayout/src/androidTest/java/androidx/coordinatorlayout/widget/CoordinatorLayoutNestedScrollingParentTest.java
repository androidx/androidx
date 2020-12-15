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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Small integration tests that verify that {@link CoordinatorLayout} implements most up to date
 * nested scrolling correctly.  "Most up to date" means that only the latest version of nested
 * scrolling (v3) and CoordinatorLayouts interaction with non-deprecated calls to
 * {@link CoordinatorLayout.Behavior} are tested.
 *
 * This test currently only covers calls to
 * {@link CoordinatorLayout#onNestedScroll(View, int, int, int, int, int, int[])} and
 * {@link CoordinatorLayout.Behavior#onNestedScroll(View, int, int, int, int, int, int[])}
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class CoordinatorLayoutNestedScrollingParentTest {

    private static final int WIDTH_AND_HEIGHT = 500;

    private CoordinatorLayout mCoordinatorLayout;
    private CoordinatorLayout.Behavior<View> mMockBehavior1;
    private CoordinatorLayout.Behavior<View> mMockBehavior2;
    private View mBehaviorChild1;
    private View mBehaviorChild2;
    private View mChild;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();

        mMockBehavior1 = setupMockBehavior();
        mMockBehavior2 = setupMockBehavior();
        mBehaviorChild1 = setupChildView(context, mMockBehavior1);
        mBehaviorChild2 = setupChildView(context, mMockBehavior2);

        mChild = new View(context);

        mCoordinatorLayout = new CoordinatorLayout(context);
        mCoordinatorLayout.addView(mBehaviorChild1);
        mCoordinatorLayout.addView(mBehaviorChild2);
        mCoordinatorLayout.addView(mChild);

        int measureSpec =
                View.MeasureSpec.makeMeasureSpec(WIDTH_AND_HEIGHT, View.MeasureSpec.EXACTLY);
        mCoordinatorLayout.measure(measureSpec, measureSpec);
        mCoordinatorLayout.layout(0, 0, WIDTH_AND_HEIGHT, WIDTH_AND_HEIGHT);

        mCoordinatorLayout.onStartNestedScroll(mBehaviorChild1, mBehaviorChild1,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
    }

    @Test
    public void onNestedScroll_behaviorsOnNestedScrollCalledCorrectly() {
        mCoordinatorLayout.onNestedScroll(mChild, 3, 4, 5, 6, ViewCompat.TYPE_TOUCH,
                new int[]{1, 2});
        verify(mMockBehavior1).onNestedScroll(mCoordinatorLayout, mBehaviorChild1, mChild, 3, 4, 5,
                6, ViewCompat.TYPE_TOUCH, new int[]{0, 0});
        verify(mMockBehavior2).onNestedScroll(mCoordinatorLayout, mBehaviorChild2, mChild, 3, 4, 5,
                6, ViewCompat.TYPE_TOUCH, new int[]{0, 0});
    }

    @Test
    public void onNestedScroll_behaviorConsumesScroll_consumedIsMutatedCorrectly() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int[] arg = (int[]) args[8];
                arg[0] = 11;
                arg[1] = 222;
                return null;
            }
        }).when(mMockBehavior1).onNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        int[] consumed = new int[]{0, 0};

        mCoordinatorLayout.onNestedScroll(mChild, 0, 0, 22, 333, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, is(new int[]{11, 222}));
    }

    @Test
    public void onNestedScroll_behaviorConsumesNoScroll_consumedIsNotMutated() {
        int[] consumed = new int[]{0, 0};
        mCoordinatorLayout.onNestedScroll(mChild, 0, 0, 22, 333, ViewCompat.TYPE_TOUCH, consumed);
        assertThat(consumed, is(new int[]{0, 0}));
    }

    @Test
    public void onNestedScroll_behaviorConsumesScroll_consumedIsResultOfAddition() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int[] arg = (int[]) args[8];
                arg[0] = 11;
                arg[1] = 111;
                return null;
            }
        }).when(mMockBehavior1).onNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        int[] consumed = new int[]{2, 3};

        mCoordinatorLayout.onNestedScroll(mChild, 0, 0, 33, 555, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, is(new int[]{13, 114}));
    }

    @Test
    public void onNestedScroll_bothConsumePartScroll_consumedIsLargestInBothDimensions() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int[] arg = (int[]) args[8];
                arg[0] = 22;
                arg[1] = 111;
                return null;
            }
        }).when(mMockBehavior1).onNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int[] arg = (int[]) args[8];
                arg[0] = 11;
                arg[1] = 222;
                return null;
            }
        }).when(mMockBehavior2).onNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        int[] consumed = new int[]{0, 0};

        mCoordinatorLayout.onNestedScroll(mChild, 0, 0, 33, 555, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, is(new int[]{22, 222}));
    }

    @Test
    public void onNestedScroll_bothConsumePartScroll_consumedIsResultOfAddition() {
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int[] arg = (int[]) args[8];
                arg[0] = 22;
                arg[1] = 111;
                return null;
            }
        }).when(mMockBehavior1).onNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        doAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                int[] arg = (int[]) args[8];
                arg[0] = 11;
                arg[1] = 222;
                return null;
            }
        }).when(mMockBehavior2).onNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        int[] consumed = new int[]{2, 3};

        mCoordinatorLayout.onNestedScroll(mChild, 0, 0, 33, 555, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, is(new int[]{24, 225}));
    }

    @SuppressWarnings("unchecked")
    private static CoordinatorLayout.Behavior<View> setupMockBehavior() {
        CoordinatorLayout.Behavior<View> mockBehavior = mock(CoordinatorLayout.Behavior.class);
        when(mockBehavior.onStartNestedScroll(
                any(CoordinatorLayout.class),
                any(View.class),
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt()))
                .thenReturn(true);
        return mockBehavior;
    }

    private static View setupChildView(Context context, CoordinatorLayout.Behavior<View> behavior) {
        CoordinatorLayout.LayoutParams layoutParams1 =
                new CoordinatorLayout.LayoutParams(WIDTH_AND_HEIGHT, WIDTH_AND_HEIGHT);
        layoutParams1.setBehavior(behavior);

        View view = new View(context);
        view.setLayoutParams(layoutParams1);

        return view;
    }


}
