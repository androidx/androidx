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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.test.R;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.testutils.Direction;
import androidx.testutils.SimpleGestureGeneratorKt;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Large integration tests that verify correct NestedScrollView flinging behavior related to
 * nested scrolling.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class NestedScrollViewNestedScrollingFlingTest extends
        BaseInstrumentationTestCase<TestContentViewActivity> {

    private static final int CHILD_HEIGHT = 800;
    private static final int NSV_HEIGHT = 400;
    private static final int PARENT_HEIGHT = 400;
    private static final int WIDTH = 400;
    private static final int ORIGIN_X_Y = 200;
    private static final int TOTAL_SCROLL_DISTANCE = CHILD_HEIGHT - NSV_HEIGHT;
    private static final int PARTIAL_SCROLL_DISTANCE = TOTAL_SCROLL_DISTANCE / 10;

    private NestedScrollView mNestedScrollView;
    private View mChild;
    private NestedScrollingSpyView mParent;

    public NestedScrollViewNestedScrollingFlingTest() {
        super(TestContentViewActivity.class);
    }

    @Before
    public void setup() throws Throwable {
        Context context = mActivityTestRule.getActivity();

        mChild = new View(context);
        mChild.setMinimumWidth(WIDTH);
        mChild.setMinimumHeight(CHILD_HEIGHT);
        mChild.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, CHILD_HEIGHT));
        mChild.setBackgroundDrawable(
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0xFFFF0000, 0xFF00FF00}));

        mNestedScrollView = new NestedScrollView(context);
        mNestedScrollView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, NSV_HEIGHT));
        mNestedScrollView.setBackgroundColor(0xFF0000FF);
        mNestedScrollView.addView(mChild);

        mParent = spy(new NestedScrollingSpyView(context));
        mParent.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, PARENT_HEIGHT));
        mParent.setBackgroundColor(0xFF0000FF);
        mParent.addView(mNestedScrollView);

        // Attach to activity and wait for layouts.
        final TestContentView testContentView =
                mActivityTestRule.getActivity().findViewById(R.id.testContentView);
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(() -> testContentView.addView(mParent));
        testContentView.awaitLayouts(2);
    }

    @Test
    public void onNestedFling_consumedIsFalse_animatesScroll() throws Throwable {
        onNestedFling_consumeParamDeterminesScroll(false, true);
    }

    @Test
    public void onNestedFling_consumedIsTrue_animatesScroll() throws Throwable {
        onNestedFling_consumeParamDeterminesScroll(true, false);
    }

    private void onNestedFling_consumeParamDeterminesScroll(final boolean consumeParamValue,
            final boolean scrolls) throws Throwable {
        final Context context = ApplicationProvider.getApplicationContext();
        final int targetVelocity = (int)
                Math.ceil(SimpleGestureGeneratorKt.generateFlingData(context).getVelocity() * 1000);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(() -> {
            mNestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX,
                            oldScrollY) -> {
                        if (scrollY > PARTIAL_SCROLL_DISTANCE) {
                            countDownLatch.countDown();
                        }
                    });

            mNestedScrollView.onNestedFling(mChild, 0, targetVelocity, consumeParamValue);
        });
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS), is(scrolls));
    }

    @Test
    public void uiFlings_dispatchNestedPreFlingReturnsFalse_flings() throws Throwable {
        uiFlings_parentPreFlingReturnDeterminesNestedScrollViewsFling(false, true);
    }

    @Test
    public void uiFlings_dispatchNestedPreFlingReturnsTrue_doesNotFling() throws Throwable {
        uiFlings_parentPreFlingReturnDeterminesNestedScrollViewsFling(true, false);
    }

    private void uiFlings_parentPreFlingReturnDeterminesNestedScrollViewsFling(
            final boolean returnValue, final boolean scrolls) throws Throwable {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(() -> {
            doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                    anyInt(), anyInt());
            doReturn(returnValue).when(mParent).onNestedPreFling(any(View.class), anyFloat(),
                    anyFloat());

            mNestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY,
                            oldScrollX, oldScrollY) -> {
                        // If we have a TYPE_NON_TOUCH nested scrolling parent, we are
                        // animated a fling.
                        if (mNestedScrollView
                                .hasNestedScrollingParent(ViewCompat.TYPE_NON_TOUCH)) {
                            if (scrollY > PARTIAL_SCROLL_DISTANCE) {
                                countDownLatch.countDown();
                            }
                        }
                    });

            SimpleGestureGeneratorKt.simulateFling(
                    mNestedScrollView,
                    SystemClock.uptimeMillis(),
                    ORIGIN_X_Y,
                    ORIGIN_X_Y,
                    Direction.UP);
        });
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS), is(scrolls));
    }

    @Test
    public void uiFling_flingDoesNotReachEnd_fullyParticipatesInNestedScrolling() throws Throwable {
        uiFling_fullyParticipatesInNestedScrolling(false);
    }

    @Test
    public void uiFling_flingReachesEnd_fullyParticipatesInNestedScrolling() throws Throwable {
        uiFling_fullyParticipatesInNestedScrolling(true);
    }

    private void uiFling_fullyParticipatesInNestedScrolling(final boolean hardFling)
            throws Throwable {
        final CountDownLatch countDownLatch = new CountDownLatch(hardFling ? 2 : 1);
        mActivityTestRule.runOnUiThread(() -> {
            doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                    anyInt(), anyInt());

            mParent.mOnStopNestedScrollListener =
                    type -> {
                        if (type == ViewCompat.TYPE_NON_TOUCH) {
                            countDownLatch.countDown();
                        }
                    };

            if (hardFling) {
                mNestedScrollView.setOnScrollChangeListener(
                        (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY,
                                oldScrollX, oldScrollY) -> {
                            if (scrollY == TOTAL_SCROLL_DISTANCE) {
                                countDownLatch.countDown();
                            }
                        });
            }

            ViewConfiguration configuration =
                    ViewConfiguration.get(mActivityTestRule.getActivity());

            float velocity;
            if (hardFling) {
                velocity = configuration.getScaledMaximumFlingVelocity() * .9f;
            } else {
                velocity = configuration.getScaledMinimumFlingVelocity() * 1.1f;
            }

            SimpleGestureGeneratorKt.simulateFling(
                    mNestedScrollView,
                    SystemClock.uptimeMillis(),
                    ORIGIN_X_Y,
                    ORIGIN_X_Y,
                    Direction.UP,
                    velocity);
        });
        assertThat(countDownLatch.await(2, TimeUnit.SECONDS), is(true));

        // Verify all of the following TYPE_TOUCH nested scrolling methods are called.
        verify(mParent, atLeastOnce()).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                anyInt(), anyInt(), eq(ViewCompat.TYPE_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onNestedPreFling(eq(mNestedScrollView), anyFloat(),
                anyFloat());
        verify(mParent, atLeastOnce()).onNestedFling(eq(mNestedScrollView), anyFloat(), anyFloat(),
                eq(true));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mNestedScrollView, ViewCompat.TYPE_TOUCH);

        // Verify all of the following TYPE_NON_TOUCH nested scrolling methods are called
        verify(mParent, atLeastOnce()).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                anyInt(),
                anyInt(), eq(ViewCompat.TYPE_NON_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mNestedScrollView,
                ViewCompat.TYPE_NON_TOUCH);
    }

    @Test
    public void fling_fullyParticipatesInNestedScrolling() throws Throwable {
        final Context context = ApplicationProvider.getApplicationContext();
        final int targetVelocity = (int)
                Math.ceil(SimpleGestureGeneratorKt.generateFlingData(context).getVelocity() * 1000);

        final CountDownLatch countDownLatch = new CountDownLatch(2);
        mActivityTestRule.runOnUiThread(() -> {
            doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                    anyInt(), anyInt());

            mParent.mOnStopNestedScrollListener =
                    type -> {
                        if (type == ViewCompat.TYPE_NON_TOUCH) {
                            countDownLatch.countDown();
                        }
                    };

            mNestedScrollView.setOnScrollChangeListener(
                    (NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX,
                            oldScrollY) -> {
                        if (scrollY == TOTAL_SCROLL_DISTANCE) {
                            countDownLatch.countDown();
                        }
                    });

            mNestedScrollView.fling(targetVelocity);
        });
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS), is(true));

        // Verify all of the following TYPE_NON_TOUCH nested scrolling methods are called
        verify(mParent, atLeastOnce()).onStartNestedScroll(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mNestedScrollView, mNestedScrollView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mNestedScrollView), anyInt(), anyInt(),
                anyInt(),
                anyInt(), eq(ViewCompat.TYPE_NON_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mNestedScrollView,
                ViewCompat.TYPE_NON_TOUCH);

        // Verify all of the following TYPE_TOUCH nested scrolling methods are not called.
        verify(mParent, never()).onStartNestedScroll(any(View.class), any(View.class),
                anyInt(), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedScrollAccepted(any(View.class), any(View.class),
                anyInt(), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedScroll(any(View.class), anyInt(), anyInt(), anyInt(),
                anyInt(), eq(ViewCompat.TYPE_TOUCH), any(int[].class));
        verify(mParent, never()).onNestedPreFling(any(View.class), anyFloat(), anyFloat());
        verify(mParent, never()).onNestedFling(any(View.class), anyFloat(), anyFloat(),
                anyBoolean());
        verify(mParent, never()).onStopNestedScroll(any(View.class), eq(ViewCompat.TYPE_TOUCH));
    }

    public static class NestedScrollingSpyView extends FrameLayout implements
            NestedScrollingParent3 {

        public OnStopNestedScrollListener mOnStopNestedScrollListener;

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
            if (mOnStopNestedScrollListener != null) {
                mOnStopNestedScrollListener.onStopNestedScroll(type);
            }
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

        @Override
        public void setNestedScrollingEnabled(boolean enabled) {

        }

        @Override
        public boolean isNestedScrollingEnabled() {
            return false;
        }

        @Override
        public boolean startNestedScroll(int axes) {
            return false;
        }

        @Override
        public void stopNestedScroll() {

        }

        @Override
        public boolean hasNestedScrollingParent() {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed,
                int[] offsetInWindow) {
            return false;
        }

        @Override
        public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
            return false;
        }

        @Override
        public boolean onStartNestedScroll(@NotNull View child, @NotNull View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NotNull View child, @NotNull View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(@NotNull View target) {

        }

        @Override
        public void onNestedScroll(@NotNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(@NotNull View target, int dx, int dy,
                @NotNull int[] consumed) {

        }

        @Override
        public boolean onNestedFling(@NotNull View target, float velocityX, float velocityY,
                boolean consumed) {
            return false;
        }

        @Override
        public boolean onNestedPreFling(@NotNull View target, float velocityX, float velocityY) {
            return false;
        }

        @Override
        public int getNestedScrollAxes() {
            return 0;
        }

        interface OnStopNestedScrollListener {
            void onStopNestedScroll(int type);
        }
    }
}
