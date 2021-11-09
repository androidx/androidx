/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.recyclerview.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.test.espresso.action.GeneralLocation.CENTER;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.testutils.SwipeInjector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Large integration tests that verify that a {@link RecyclerView} interacts with nested scrolling
 * correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewNestedScrolling3RequestDisallowInterceptTouchTest {

    private static final int ITEM_HEIGHT = 20;
    private static final int NSV_HEIGHT = 400;
    private static final int PARENT_HEIGHT = 400;
    private static final int WIDTH = 400;
    private static final int SWIPE_DURATION = 300;

    private RecyclerView mRecyclerView;
    private NestedScrollingSpyView mParent;
    private int mTouchSlop;

    @Rule
    public final ActivityTestRule<TestContentViewActivity> mActivityTestRule;

    public RecyclerViewNestedScrolling3RequestDisallowInterceptTouchTest() {
        mActivityTestRule = new ActivityTestRule<>(TestContentViewActivity.class);
    }

    @Before
    public void setUp() throws Throwable {
        setup();
        attachToActivity();
    }

    @Test
    @FlakyTest(bugId = 190192628)
    public void parentConsumes1pxRvConsumes0px() {
        mParent.consumeY = 1;

        // RecyclerView consumes nothing because we scroll up, and we're already at the top
        swipeVertically(mTouchSlop + 100);

        verify(mParent, atLeastOnce()).requestDisallowInterceptTouchEvent(eq(true));
    }

    @Test
    @FlakyTest(bugId = 190192628)
    public void parentConsumes0pxRvConsumes1px() {
        // RecyclerView consumes all because we scroll down, and we're already at the top
        swipeVertically(-mTouchSlop - 1);

        verify(mParent, atLeastOnce()).requestDisallowInterceptTouchEvent(eq(true));
    }

    @Test
    @FlakyTest(bugId = 190192628)
    public void parentConsumes0pxRvConsumes0px() {
        // RecyclerView consumes nothing because we scroll up, and we're already at the top
        swipeVertically(mTouchSlop + 100);

        verify(mParent, never()).requestDisallowInterceptTouchEvent(eq(true));
    }

    @Test
    @FlakyTest(bugId = 190192628)
    public void parentPreConsumes1pxRvConsumes0px() {
        mParent.consumePreY = 1;

        swipeVertically(-mTouchSlop - 1);

        verify(mParent, atLeastOnce()).requestDisallowInterceptTouchEvent(eq(true));
    }

    private void setup() {
        Context context = mActivityTestRule.getActivity();

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, NSV_HEIGHT));
        mRecyclerView.setBackgroundColor(0xFF0000FF);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(new TestAdapter(100, ITEM_HEIGHT));

        mParent = spy(new NestedScrollingSpyView(context));
        mParent.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, PARENT_HEIGHT));
        mParent.setBackgroundColor(0xFF0000FF);
        mParent.addView(mRecyclerView);

        mParent.consumeX = 0;
        mParent.consumeY = 0;
        mParent.consumePreX = 0;
        mParent.consumePreY = 0;

        mTouchSlop = ViewConfiguration.get(mRecyclerView.getContext()).getScaledTouchSlop();
    }

    private void attachToActivity() throws Throwable {
        final TestContentView testContentView = mActivityTestRule.getActivity().getContentView();
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mParent);
            }
        });
        testContentView.awaitLayouts(2);
    }

    private void swipeVertically(int dy) {
        SwipeInjector swiper = new SwipeInjector(InstrumentationRegistry.getInstrumentation());
        swiper.startDrag(CENTER, mRecyclerView);
        swiper.dragBy(0, dy, SWIPE_DURATION);
        swiper.finishDrag();
    }

    @SuppressWarnings("NullableProblems")
    public static class NestedScrollingSpyView extends FrameLayout implements
            NestedScrollingParent3 {

        private final NestedScrollingParentHelper mNestedScrollingParentHelper;
        private final int[] mNestedScrollingV2ConsumedCompat = new int[2];

        // The amount of pixels to consume during a nested scroll
        // Use only positive values, value is used in both directions
        public int consumeX = 0;
        public int consumeY = 0;
        // The amount of pixels to consume during a nested pre scroll
        // Use only positive values, value is used in both directions
        public int consumePreX = 0;
        public int consumePreY = 0;

        public NestedScrollingSpyView(Context context) {
            super(context);
            mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);
        }

        // NestedScrollingParent3

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @NonNull int[] consumed) {
            if (consumeX != 0) {
                consumed[0] += dxUnconsumed > 0
                        ? Math.min(consumeX, dxUnconsumed)
                        : Math.max(-consumeX, dxUnconsumed);
            }
            if (consumeY != 0) {
                consumed[1] += dyUnconsumed > 0
                        ? Math.min(consumeY, dyUnconsumed)
                        : Math.max(-consumeY, dyUnconsumed);
            }
        }

        // NestedScrollingParent2

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return onStartNestedScroll(child, target, axes);
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {
            onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {
            onStopNestedScroll(target);
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {
            onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type,
                    mNestedScrollingV2ConsumedCompat);
        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {
            if (consumePreX != 0) {
                consumed[0] += dx > 0
                        ? Math.min(consumePreX, dx)
                        : Math.max(-consumePreX, dx);
            }
            if (consumePreY != 0) {
                consumed[1] += dy > 0
                        ? Math.min(consumePreY, dy)
                        : Math.max(-consumePreY, dy);
            }
        }

        // NestedScrollingParent1

        @Override
        public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
            return true;
        }

        @Override
        public void onNestedScrollAccepted(View child, View target, int axes) {
            mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        }

        @Override
        public void onStopNestedScroll(View child) {
            mNestedScrollingParentHelper.onStopNestedScroll(child);
        }

        @Override
        public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed) {
            onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                    ViewCompat.TYPE_TOUCH, mNestedScrollingV2ConsumedCompat);
        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy,
                @NonNull int[] consumed) {
            ViewCompat.dispatchNestedPreScroll(this, dx, dy, consumed, null);
        }

        @Override
        public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY,
                boolean consumed) {
            return ViewCompat.dispatchNestedFling(this, velocityX, velocityY, consumed);
        }

        @Override
        public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
            return ViewCompat.dispatchNestedPreFling(this, velocityX, velocityY);
        }

        @Override
        public int getNestedScrollAxes() {
            return mNestedScrollingParentHelper.getNestedScrollAxes();
        }
    }

    private class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private int mItemHeight;
        private int mItemCount;

        TestAdapter(int itemCount, int itemHeight) {
            mItemHeight = itemHeight;
            mItemCount = itemCount;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = new View(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(MATCH_PARENT, mItemHeight));
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

}
