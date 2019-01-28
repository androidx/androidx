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
package androidx.core.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollingChildHelperTest {

    private NestedScrollingChildHelper mNestedScrollingChildHelper;
    private View mNestedScrollingChild;
    private NestedScrollingParentImpl mNestedScrollingParentImpl;

    @Before
    public void setup() {
        mNestedScrollingParentImpl =
                spy(new NestedScrollingParentImpl(ApplicationProvider.getApplicationContext()));
        mNestedScrollingParentImpl.setLayoutParams(
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mNestedScrollingChild = spy(new View(ApplicationProvider.getApplicationContext()));
        mNestedScrollingChild.setLayoutParams(
                new NestedScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));


        mNestedScrollingParentImpl.addView(mNestedScrollingChild);

        int measureSpec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY);
        mNestedScrollingParentImpl.measure(measureSpec, measureSpec);
        mNestedScrollingParentImpl.layout(0, 0, 400, 400);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(mNestedScrollingChild);
    }

    @Test
    public void dispatchNestedScroll_callsThroughToParent() {

        doReturn(true).when(mNestedScrollingParentImpl)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollingChildHelper.setNestedScrollingEnabled(true);
        mNestedScrollingChildHelper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollingChildHelper.dispatchNestedScroll(
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{11, 22});

        verify(mNestedScrollingParentImpl).onNestedScroll(
                eq(mNestedScrollingChild),
                eq(1),
                eq(2),
                eq(3),
                eq(4),
                eq(ViewCompat.TYPE_TOUCH),
                eq(new int[]{11, 22}));
    }

    @Test
    public void dispatchNestedScroll_nestedScrollingDisabled_doesntCallOnNestedScroll() {

        doReturn(true).when(mNestedScrollingParentImpl)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollingChildHelper.setNestedScrollingEnabled(false);
        mNestedScrollingChildHelper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollingChildHelper.dispatchNestedScroll(
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{11, 22});

        verify(mNestedScrollingParentImpl, never()).onNestedScroll(any(View.class),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(int[].class));
    }

    @Test
    public void dispatchNestedScroll_parentDoesNotSupportAxis_doesntCallOnNestedScroll() {

        doReturn(false).when(mNestedScrollingParentImpl)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollingChildHelper.setNestedScrollingEnabled(true);
        mNestedScrollingChildHelper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollingChildHelper.dispatchNestedScroll(
                1,
                2,
                3,
                4,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{11, 22});

        verify(mNestedScrollingParentImpl, never()).onNestedScroll(any(View.class),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(int[].class));
    }

    @Test
    public void dispatchNestedScroll_scrollDistancesAre0_doesNotCallOnNestedScroll() {

        doReturn(true).when(mNestedScrollingParentImpl)
                .onStartNestedScroll(any(View.class), any(View.class), anyInt(), anyInt());
        mNestedScrollingChildHelper.setNestedScrollingEnabled(true);
        mNestedScrollingChildHelper.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        mNestedScrollingChildHelper.dispatchNestedScroll(
                0,
                0,
                0,
                0,
                new int[]{5, 6},
                ViewCompat.TYPE_TOUCH,
                new int[]{11, 22});

        verify(mNestedScrollingParentImpl, never()).onNestedScroll(any(View.class),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), any(int[].class));
    }

    public static class NestedScrollingParentImpl extends FrameLayout
            implements NestedScrollingParent3 {
        public NestedScrollingParentImpl(Context context) {
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
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @Nullable int[] consumed) {
        }
    }

}
