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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
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
import androidx.core.view.NestedScrollingParent3;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Large integration test that verifies that {@link RecyclerView} does not participate in nested \
 * scrolling when the scrolling is triggered by calls to any of the
 * {@link RecyclerView#smoothScrollBy} methods.
 */
@RunWith(Parameterized.class)
@LargeTest
public class RecyclerViewNestedScrollingSmoothScrollByTest {

    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule =
            new ActivityTestRule<>(TestActivity.class);

    private enum SmoothScrollType {
        TWO_PARAMS, THREE_PARAMS, FOUR_PARAMS
    }

    @Parameterized.Parameters(name =
            "orientationVertical:{0}, scrollForwards:{1}, smoothScrollType:{2}")
    public static Collection<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        for (boolean vertical : new boolean[]{true, false}) {
            for (boolean scrollForwards : new boolean[]{true, false}) {
                for (SmoothScrollType smoothScrollType : SmoothScrollType.values()) {
                    result.add(new Object[]{vertical, scrollForwards, smoothScrollType});
                }
            }
        }
        return result;
    }

    private static final int RV_SCROLL_BY_MAGNITUDE = 200;
    private static final int RV_ORIENTATION_DIMENSION = 200;
    // The RV's single child is 100 pixels larger than the parent so when the RV scrolls it will
    // only be able to actually scroll 100.
    private static final int ITEM_ORIENTATION_DIMENSION = 300;
    // The test will wait for this scroll distance to be completed before it verifies.
    private static final int EXPECTED_SCROLL_OFFSET = 100;

    // These values aren't relevant, just need something reasonable.
    private static final int PARENT_ORIENTATION_DIMENSION = 100;
    private static final int OTHER_DIMENSION = 100;

    private RecyclerView mRecyclerView;
    private NestedScrollingSpyView mParent;

    private boolean mVertical;
    private boolean mScrollForwards;
    private SmoothScrollType mSmoothScrollType;

    public RecyclerViewNestedScrollingSmoothScrollByTest(boolean vertical, boolean scrollForwards,
            SmoothScrollType smoothScrollType) {
        mVertical = vertical;
        mScrollForwards = scrollForwards;
        mSmoothScrollType = smoothScrollType;
    }

    @Before
    public void setup() throws Throwable {
        Context context = mActivityTestRule.getActivity();

        // Create view hierarchy.
        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        mVertical ? OTHER_DIMENSION : RV_ORIENTATION_DIMENSION,
                        mVertical ? RV_ORIENTATION_DIMENSION : OTHER_DIMENSION));
        mRecyclerView.setBackgroundColor(0xFF0000FF);
        int orientation = mVertical ? RecyclerView.VERTICAL : RecyclerView.HORIZONTAL;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context, orientation, false));
        mRecyclerView.setAdapter(
                new TestAdapter(context, ITEM_ORIENTATION_DIMENSION, mVertical));

        mParent = spy(new NestedScrollingSpyView(context));
        mParent.setLayoutParams(
                new ViewGroup.LayoutParams(
                        mVertical ? OTHER_DIMENSION : PARENT_ORIENTATION_DIMENSION,
                        mVertical ? PARENT_ORIENTATION_DIMENSION : OTHER_DIMENSION));
        mParent.setBackgroundColor(0xFF0000FF);
        mParent.addView(mRecyclerView);

        // Attach view hierarchy to activity and wait for first layout.
        final TestedFrameLayout testContentView =
                mActivityTestRule.getActivity().getContainer();
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mParent);
            }
        });
        testContentView.waitForLayout(2);
    }

    @Test
    public void smoothScrollBy_doesNotParticipateInNestedScrolling()
            throws Throwable {

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final int[] totalScrolled = new int[1];

        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                        anyInt(), anyInt());

                int dx, dy;
                if (mScrollForwards) {
                    if (mVertical) {
                        dx = 0;
                        dy = RV_SCROLL_BY_MAGNITUDE;
                    } else {
                        dx = RV_SCROLL_BY_MAGNITUDE;
                        dy = 0;
                    }
                } else {
                    mRecyclerView.scrollBy(mVertical ? 0 : 100, mVertical ? 100 : 0);
                    if (mVertical) {
                        dx = 0;
                        dy = -RV_SCROLL_BY_MAGNITUDE;
                    } else {
                        dx = -RV_SCROLL_BY_MAGNITUDE;
                        dy = 0;
                    }
                }

                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        totalScrolled[0] += mVertical ? dy : dx;
                        int target = mScrollForwards ? EXPECTED_SCROLL_OFFSET
                                : -EXPECTED_SCROLL_OFFSET;
                        if (totalScrolled[0] == target) {
                            countDownLatch.countDown();
                        }
                    }
                });

                switch (mSmoothScrollType) {
                    case TWO_PARAMS:
                        mRecyclerView.smoothScrollBy(dx, dy);
                        break;
                    case THREE_PARAMS:
                        mRecyclerView.smoothScrollBy(dx, dy, null);
                        break;
                    case FOUR_PARAMS:
                        mRecyclerView.smoothScrollBy(dx, dy, null, RecyclerView.UNDEFINED_DURATION);
                        break;
                }
            }
        });
        assertThat(countDownLatch.await(2, TimeUnit.SECONDS), is(true));

        // Verify all of the following TYPE_TOUCH nested scrolling methods are called.
        verify(mParent, never()).onStartNestedScroll(
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt());
        verify(mParent, never()).onNestedScrollAccepted(
                any(View.class),
                any(View.class),
                anyInt(),
                anyInt());
        verify(mParent, never()).onNestedPreScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                any(int[].class),
                anyInt());
        verify(mParent, never()).onNestedScroll(
                any(View.class),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                anyInt(),
                any(int[].class));
        verify(mParent, never()).onNestedPreFling(
                any(View.class),
                anyFloat(),
                anyFloat());
        verify(mParent, never()).onNestedFling(
                any(View.class),
                anyFloat(),
                anyFloat(),
                anyBoolean());
        verify(mParent, never()).onStopNestedScroll(
                any(View.class),
                anyInt());
    }

    // Implementation of NestedScrollingParent3 that we can spy on.
    public static class NestedScrollingSpyView extends FrameLayout implements
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
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed,
                int dyUnconsumed) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy,
                @NonNull int[] consumed) {

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
            return 0;
        }
    }

    // Simple adapter that only creates one child with a provided size for the dimension that
    // matches the scrolling orientation, and MATCH_PARENT for the other dimension.
    private class TestAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private Context mContext;
        private int mOrientationSize;
        private boolean mVertical;

        TestAdapter(Context context, int orientationSize, boolean vertical) {
            mContext = context;
            mOrientationSize = orientationSize;
            mVertical = vertical;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                int viewType) {
            View view = new View(mContext);

            int width;
            int height;
            if (mVertical) {
                width = ViewGroup.LayoutParams.MATCH_PARENT;
                height = mOrientationSize;
            } else {
                width = mOrientationSize;
                height = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
            view.setMinimumHeight(mOrientationSize);
            return new RecyclerView.ViewHolder(view) {
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }
}
