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

package androidx.recyclerview.widget;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import static androidx.recyclerview.widget.RecyclerView.HORIZONTAL;
import static androidx.recyclerview.widget.RecyclerView.VERTICAL;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.filters.LargeTest;
import androidx.testutils.SwipeToLocation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@LargeTest
@RunWith(Parameterized.class)
public class PagerSnapHelperTest extends BaseLinearLayoutManagerTest {

    private static final int RECYCLERVIEW_SIZE = 1000;

    private enum ChildSize {
        SMALLER((int) (0.6 * RECYCLERVIEW_SIZE)),
        SAME(MATCH_PARENT),
        LARGER((int) (1.4 * RECYCLERVIEW_SIZE));

        private final int mSizeParam;
        ChildSize(int size) {
            mSizeParam = size;
        }
    }

    final Config mConfig;
    private final boolean mReverseScroll;
    private final ChildSize mChildSize;

    public PagerSnapHelperTest(Config config, boolean reverseScroll, ChildSize childSize) {
        mConfig = config;
        mReverseScroll = reverseScroll;
        mChildSize = childSize;
    }

    @Parameterized.Parameters(name = "config:{0},reverseScroll:{1},mChildSize:{2}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        List<Config> configs = createBaseVariations();
        for (Config config : configs) {
            for (boolean reverseScroll : new boolean[] {false, true}) {
                for (ChildSize childSize : ChildSize.values()) {
                    if (!config.mWrap) {
                        result.add(new Object[]{config, reverseScroll, childSize});
                    }
                }
            }
        }
        return result;
    }

    @Test
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true, getChildLayoutParams(), getParentLayoutParams());
        setupSnapHelper();

        // Record the current center view.
        TextView view = (TextView) findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int scrollDistance = (getViewDimension(view) / 2) - 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);

        // Views have not changed
        View viewAfterFling = findCenterView(mLayoutManager);
        assertSame("The view should NOT have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnScrollNextView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true, getChildLayoutParams(), getParentLayoutParams());
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int scrollDistance = (getViewDimension(view) / 2) + 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);

        // Views have not changed
        View viewAfterFling = findCenterView(mLayoutManager);
        assertNotSame("The view should have scrolled", view, viewAfterFling);
        int expectedPosition = mConfig.mItemCount / 2 + (mConfig.mReverseLayout
                ? (mReverseScroll ? 1 : -1)
                : (mReverseScroll ? -1 : 1));
        assertEquals(expectedPosition, mLayoutManager.getPosition(viewAfterFling));
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnFlingSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true, getChildLayoutParams(), getParentLayoutParams());
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        // Velocity small enough to not scroll to the next view.
        int velocity = (int) (1.000001 * mRecyclerView.getMinFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;
        mLayoutManager.expectIdleState(2);
        // Scroll at one pixel in the correct direction to allow fling snapping to the next view.
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollBy(mReverseScroll ? -1 : 1, mReverseScroll ? -1 : 1);
            }
        });
        waitForIdleScroll(mRecyclerView);
        assertTrue(fling(velocityDir, velocityDir));
        // Wait for two settling scrolls: the initial one and the corrective one.
        waitForIdleScroll(mRecyclerView);
        mLayoutManager.waitForSnap(100);

        View viewAfterFling = findCenterView(mLayoutManager);

        assertSame("The view should NOT have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnFlingNextView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true, getChildLayoutParams(), getParentLayoutParams());
        setupSnapHelper();
        runSnapOnMaxFlingNextView((int) (0.2 * mRecyclerView.getMaxFlingVelocity()));
    }

    @Test
    public void snapOnMaxFlingNextView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true, getChildLayoutParams(), getParentLayoutParams());
        setupSnapHelper();
        runSnapOnMaxFlingNextView(mRecyclerView.getMaxFlingVelocity());
    }

    @Test
    public void snapWhenFlingToSnapPosition() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true, getChildLayoutParams(), getParentLayoutParams());
        setupSnapHelper();
        runSnapOnFlingExactlyToNextView();
    }

    private RecyclerView.LayoutParams getParentLayoutParams() {
        return new RecyclerView.LayoutParams(RECYCLERVIEW_SIZE, RECYCLERVIEW_SIZE);
    }

    private RecyclerView.LayoutParams getChildLayoutParams() {
        return new RecyclerView.LayoutParams(
                mConfig.mOrientation == HORIZONTAL ? mChildSize.mSizeParam : MATCH_PARENT,
                mConfig.mOrientation == VERTICAL ? mChildSize.mSizeParam : MATCH_PARENT
        );
    }

    private void runSnapOnMaxFlingNextView(int velocity) throws Throwable {
        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int velocityDir = mReverseScroll ? -velocity : velocity;
        mLayoutManager.expectIdleState(1);

        // Scroll at one pixel in the correct direction to allow fling snapping to the next view.
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.scrollBy(mReverseScroll ? -1 : 1, mReverseScroll ? -1 : 1);
            }
        });
        waitForIdleScroll(mRecyclerView);
        assertTrue(fling(velocityDir, velocityDir));
        mLayoutManager.waitForSnap(100);

        View viewAfterFling = findCenterView(mLayoutManager);

        assertNotSame("The view should have scrolled", view, viewAfterFling);
        int expectedPosition = mConfig.mItemCount / 2 + (mConfig.mReverseLayout
                ? (mReverseScroll ? 1 : -1)
                : (mReverseScroll ? -1 : 1));
        assertEquals(expectedPosition, mLayoutManager.getPosition(viewAfterFling));
        assertCenterAligned(viewAfterFling);
    }

    private void runSnapOnFlingExactlyToNextView() throws Throwable {
        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        // Determine the target item to scroll to
        final int expectedPosition = mConfig.mItemCount / 2 + (mConfig.mReverseLayout
                ? (mReverseScroll ? 1 : -1)
                : (mReverseScroll ? -1 : 1));

        // Smooth scroll in the correct direction to allow fling snapping to the next view.
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecyclerView.smoothScrollToPosition(expectedPosition);
            }
        });
        waitForDistanceToTarget(expectedPosition, .5f);

        // Interrupt scroll and fling to target view, ending exactly when the view is snapped
        mLayoutManager.expectIdleState(1);
        onView(allOf(
                isDescendantOfA(isAssignableFrom(RecyclerView.class)),
                withText(mTestAdapter.getItemAt(expectedPosition).getDisplayText())
        )).perform(SwipeToLocation.flingToCenter());
        waitForIdleScroll(mRecyclerView);

        // Wait until the RecyclerView comes to a rest
        mLayoutManager.waitForSnap(100);

        // Check the result
        View viewAfterFling = findCenterView(mLayoutManager);
        assertNotSame("The view should have scrolled", view, viewAfterFling);
        assertEquals(expectedPosition, mLayoutManager.getPosition(viewAfterFling));
        assertCenterAligned(viewAfterFling);
    }

    private void setupSnapHelper() throws Throwable {
        SnapHelper snapHelper = new PagerSnapHelper();

        // Do we expect a snap when attaching the SnapHelper?
        View centerView = findCenterView(mLayoutManager);
        boolean expectSnap = distFromCenter(centerView) != 0;

        mLayoutManager.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);
        if (expectSnap) {
            mLayoutManager.waitForSnap(2);
        }

        mLayoutManager.expectLayouts(1);
        scrollToPositionWithOffset(mConfig.mItemCount / 2, getScrollOffset());
        mLayoutManager.waitForLayout(2);
    }

    private int getScrollOffset() {
        RecyclerView.LayoutParams params = mTestAdapter.mLayoutParams;
        if (params == null) {
            return 0;
        }
        if (mConfig.mOrientation == HORIZONTAL && params.width == MATCH_PARENT
                || mConfig.mOrientation == VERTICAL && params.height == MATCH_PARENT) {
            return 0;
        }
        // In reverse layouts, the rounding error of x/2 ends up on the other side of the center
        // Instead of fixing all asserts, just move the rounding error to the same side as without
        // reverse layout.
        int reverseAdjustment = (mConfig.mReverseLayout ? 1 : 0)
                // For larger children, the offset becomes negative, so
                // we need to subtract the adjustment rather than add it
                * (mChildSize == ChildSize.LARGER ? -1 : 1);
        if (mConfig.mOrientation == HORIZONTAL) {
            return (mRecyclerView.getWidth() - params.width + reverseAdjustment) / 2;
        } else {
            return (mRecyclerView.getHeight() - params.height + reverseAdjustment) / 2;
        }
    }

    @Nullable
    private View findCenterView(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager.canScrollHorizontally()) {
            return mRecyclerView.findChildViewUnder(mRecyclerView.getWidth() / 2, 0);
        } else {
            return mRecyclerView.findChildViewUnder(0, mRecyclerView.getHeight() / 2);
        }
    }

    private int getViewDimension(View view) {
        OrientationHelper helper;
        if (mLayoutManager.canScrollHorizontally()) {
            helper = OrientationHelper.createHorizontalHelper(mLayoutManager);
        } else {
            helper = OrientationHelper.createVerticalHelper(mLayoutManager);
        }
        return helper.getDecoratedMeasurement(view);
    }

    private void assertCenterAligned(View view) {
        if (mLayoutManager.canScrollHorizontally()) {
            assertEquals(mRecyclerView.getWidth() / 2,
                    mLayoutManager.getViewBounds(view).centerX());
        } else {
            assertEquals(mRecyclerView.getHeight() / 2,
                    mLayoutManager.getViewBounds(view).centerY());
        }
    }

    private int distFromCenter(View view) {
        if (mLayoutManager.canScrollHorizontally()) {
            return Math.abs(mRecyclerView.getWidth() / 2
                    - mLayoutManager.getViewBounds(view).centerX());
        } else {
            return Math.abs(mRecyclerView.getHeight() / 2
                    - mLayoutManager.getViewBounds(view).centerY());
        }
    }

    private boolean fling(final int velocityX, final int velocityY) throws Throwable {
        final AtomicBoolean didStart = new AtomicBoolean(false);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean result = mRecyclerView.fling(velocityX, velocityY);
                didStart.set(result);
            }
        });
        if (!didStart.get()) {
            return false;
        }
        waitForIdleScroll(mRecyclerView);
        return true;
    }

    /**
     * Waits until the RecyclerView has smooth scrolled till within the given margin from the target
     * item. The percentage is relative to the size of the target view.
     *
     * @param targetPosition The adapter position of the view we want to scroll to
     * @param distancePercent The distance from the view when we stop waiting, relative to the
     *                        target view
     */
    private void waitForDistanceToTarget(final int targetPosition, final float distancePercent)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                View target = mLayoutManager.findViewByPosition(targetPosition);
                if (target == null) {
                    return;
                }
                int distancePx = distFromCenter(target);
                int size = mConfig.mOrientation == HORIZONTAL
                        ? target.getWidth()
                        : target.getHeight();
                if ((float) distancePx / size <= distancePercent) {
                    latch.countDown();
                }
            }
        });
        assertTrue("should be close enough to the target view within 10 seconds",
                latch.await(10, TimeUnit.SECONDS));
    }
}
