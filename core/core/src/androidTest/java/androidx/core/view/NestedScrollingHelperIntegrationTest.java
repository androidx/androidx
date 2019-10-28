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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class NestedScrollingHelperIntegrationTest {

    private NestedScrollingImpl3 mNestedScrollingImpl3;
    private NestedScrollingImpl2 mNestedScrollingImpl2;
    private NestedScrollingImpl mNestedScrollingImpl;

    @Before
    public void setup() {
        mNestedScrollingImpl3 = new NestedScrollingImpl3(
                ApplicationProvider.getApplicationContext());
        mNestedScrollingImpl2 = new NestedScrollingImpl2(
                ApplicationProvider.getApplicationContext());
        mNestedScrollingImpl = new NestedScrollingImpl(ApplicationProvider.getApplicationContext());
    }

    @Test
    public void dispatchNestedScroll_childIsV1ParentIsV3_returnsTrue() {
        measureAndLayout(mNestedScrollingImpl, mNestedScrollingImpl3);
        mNestedScrollingImpl.setNestedScrollingEnabled(true);
        mNestedScrollingImpl.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);

        boolean retValue = mNestedScrollingImpl.dispatchNestedScroll(0,
                0, 0, 100, null);

        assertThat(retValue, equalTo(true));
    }

    @Test
    public void dispatchNestedScroll_childIsV3ParentIsV1_fullScrollDistancesAddedToConsumed() {
        measureAndLayout(mNestedScrollingImpl3, mNestedScrollingImpl);
        mNestedScrollingImpl3.setNestedScrollingEnabled(true);
        mNestedScrollingImpl3.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        int[] consumed = new int[]{11, 12};

        mNestedScrollingImpl3.dispatchNestedScroll(0,
                0, 0, 100, null,
                ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, equalTo(new int[]{11, 112}));
    }

    @Test
    public void dispatchNestedScroll_childIsV2ParentIsV3_returnsTrue() {
        measureAndLayout(mNestedScrollingImpl2, mNestedScrollingImpl3);
        mNestedScrollingImpl2.setNestedScrollingEnabled(true);
        mNestedScrollingImpl2.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);

        boolean retValue = mNestedScrollingImpl2.dispatchNestedScroll(0,
                0, 0, 100, null,
                ViewCompat.TYPE_TOUCH);

        assertThat(retValue, equalTo(true));
    }

    @Test
    public void dispatchNestedScroll_childIsV3ParentIsV2_fullScrollDistancesAddedToConsumed() {
        measureAndLayout(mNestedScrollingImpl3, mNestedScrollingImpl2);
        mNestedScrollingImpl3.setNestedScrollingEnabled(true);
        mNestedScrollingImpl3.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL,
                ViewCompat.TYPE_TOUCH);
        int[] consumed = new int[]{11, 12};

        mNestedScrollingImpl3.dispatchNestedScroll(
                0, 0,
                0, 100,
                null, ViewCompat.TYPE_TOUCH, consumed);

        assertThat(consumed, equalTo(new int[]{11, 112}));
    }

    private void measureAndLayout(final View child, final ViewGroup parent) {
        parent.addView(child);

        int measureSpec = View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY);
        parent.measure(measureSpec, measureSpec);
        parent.layout(0, 0, 500, 500);
    }

    public static class NestedScrollingImpl extends FrameLayout implements NestedScrollingParent,
            NestedScrollingChild {

        public NestedScrollingChildHelper mNestedScrollingChildHelper;
        public NestedScrollingParentHelper mNestedScrollingParentHelper;

        public NestedScrollingImpl(@NonNull Context context) {
            super(context);
            mNestedScrollingChildHelper =  new NestedScrollingChildHelper(this);
            mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
            return true;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
            mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(@NonNull View target) {
            mNestedScrollingParentHelper.onStopNestedScroll(target);
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed) {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    null);
        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy,
                 @NonNull int[] consumed) {
            dispatchNestedPreScroll(dx, dy, consumed, null);
        }

        @Override
        public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return mNestedScrollingParentHelper.getNestedScrollAxes();
        }

        @Override
        public void setNestedScrollingEnabled(boolean enabled) {
            mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
        }

        @Override
        public boolean isNestedScrollingEnabled() {
            return mNestedScrollingChildHelper.isNestedScrollingEnabled();
        }

        @Override
        public boolean startNestedScroll(int axes) {
            return mNestedScrollingChildHelper.startNestedScroll(axes);
        }

        @Override
        public void stopNestedScroll() {
            mNestedScrollingChildHelper.stopNestedScroll();
        }

        @Override
        public boolean hasNestedScrollingParent() {
            return mNestedScrollingChildHelper.hasNestedScrollingParent();
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow) {
            return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow);
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow) {
            return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed,
                    offsetInWindow);
        }

        @Override
        public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
            return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
        }

        @Override
        public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
            return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
        }
    }

    public static class NestedScrollingImpl2 extends NestedScrollingImpl
            implements NestedScrollingParent2, NestedScrollingChild2 {

        public NestedScrollingImpl2(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return true;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {
            mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes, type);
        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {
            mNestedScrollingParentHelper.onStopNestedScroll(target, type);
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    null, type);
        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {
            dispatchNestedPreScroll(dx, dy, consumed, null);
        }

        @Override
        public boolean startNestedScroll(int axes, int type) {
            return mNestedScrollingChildHelper.startNestedScroll(axes, type);
        }

        @Override
        public void stopNestedScroll(int type) {
            mNestedScrollingChildHelper.stopNestedScroll(type);
        }

        @Override
        public boolean hasNestedScrollingParent(int type) {
            return mNestedScrollingChildHelper.hasNestedScrollingParent(type);
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
            return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow, type);
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow, int type) {
            return mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed,
                    offsetInWindow, type);
        }
    }

    public static class NestedScrollingImpl3 extends NestedScrollingImpl2
            implements NestedScrollingParent3, NestedScrollingChild3 {

        public NestedScrollingImpl3(Context context) {
            super(context);
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
            dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
                    dyUnconsumed, null, type, consumed);
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @NonNull int[] consumed) {
            mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                    dxUnconsumed, dyUnconsumed, offsetInWindow, type, consumed);
        }
    }

}
