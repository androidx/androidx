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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.LargeTest;
import android.view.View;

import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@LargeTest
@RunWith(Parameterized.class)
public class StaggeredGridLayoutManagerSnappingTest extends BaseStaggeredGridLayoutManagerTest {

    final Config mConfig;
    final boolean mReverseScroll;

    public StaggeredGridLayoutManagerSnappingTest(Config config, boolean reverseScroll) {
        mConfig = config;
        mReverseScroll = reverseScroll;
    }

    @Parameterized.Parameters(name = "config:{0},reverseScroll:{1}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        List<Config> configs = createBaseVariations();
        for (Config config : configs) {
            for (boolean reverseScroll : new boolean[] {true, false}) {
                result.add(new Object[]{config, reverseScroll});
            }
        }
        return result;
    }

    @Test
    public void snapOnScrollSameViewFixedSize() throws Throwable {
        // This test is a special case for fixed sized children.
        final Config config = ((Config) mConfig.clone()).itemCount(10);
        setupByConfig(config);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(1000, 950);
        mRecyclerView.setLayoutParams(lp);
        mAdapter.mOnBindCallback = new OnBindCallback() {
            @Override
            void onBoundItem(TestViewHolder vh, int position) {
                StaggeredGridLayoutManager.LayoutParams slp = getLayoutParamsForPosition(position);
                vh.itemView.setLayoutParams(slp);
            }

            @Override
            boolean assignRandomSize() {
                return false;
            }
        };
        waitFirstLayout();
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);
        // This number comes from the sizes of the fixed views that are created for this config/
        // See getLayoutParamsForPosition(int) below. Obtained manually.
        int scrollDistance = mLayoutManager.canScrollHorizontally() ? 52 : 52;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);

        // Views have not changed
        View viewAfterScroll = findCenterView(mLayoutManager);
        assertSame("The view should NOT have scrolled", view, viewAfterScroll);
        assertCenterAligned(viewAfterScroll);
    }

    @Test
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config);
        waitFirstLayout();
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);
        // For a staggered grid layout manager with unknown item size we need to keep the distance
        // small enough to ensure we do not scroll over to an offset view in a different span.
        int scrollDistance = findMinSafeScrollDistance();
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);

        // Views have not changed
        View viewAfterScroll = findCenterView(mLayoutManager);
        assertSame("The view should NOT have scrolled", view, viewAfterScroll);
        assertCenterAligned(viewAfterScroll);
    }

    @Test
    public void snapOnScrollNextItem() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config);
        waitFirstLayout();
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);
        int scrollDistance = getViewDimension(view) + 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        smoothScrollBy(scrollDist);
        waitForIdleScroll(mRecyclerView);
        waitForIdleScroll(mRecyclerView);

        View viewAfterScroll = findCenterView(mLayoutManager);

        assertNotSame("The view should have scrolled", view, viewAfterScroll);
        assertCenterAligned(viewAfterScroll);
    }

    @Test
    public void snapOnFlingSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config);
        waitFirstLayout();
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        // Velocity small enough to not scroll to the next view.
        int velocity = (int) (1.000001 * mRecyclerView.getMinFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;
        mLayoutManager.expectIdleState(2);
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
        setupByConfig(config);
        waitFirstLayout();
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        // Velocity high enough to scroll beyond the current view.
        int velocity = (int) (0.2 * mRecyclerView.getMaxFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;

        mLayoutManager.expectIdleState(1);
        assertTrue(fling(velocityDir, velocityDir));
        mLayoutManager.waitForSnap(100);
        getInstrumentation().waitForIdleSync();

        View viewAfterFling = findCenterView(mLayoutManager);

        assertNotSame("The view should have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    private StaggeredGridLayoutManager.LayoutParams getLayoutParamsForPosition(int position) {
        // Only enabled fixed sizes if the config says so.
        if (mLayoutManager.canScrollHorizontally()) {
            int width = 400 + position * 70;
            return new StaggeredGridLayoutManager.LayoutParams(width, 300);
        } else {
            int height = 300 + position * 70;
            return new StaggeredGridLayoutManager.LayoutParams(300, height);
        }
    }

    @Nullable View findCenterView(RecyclerView.LayoutManager layoutManager) {
        return mLayoutManager.findFirstVisibleItemClosestToCenter();
    }

    private void setupSnapHelper() throws Throwable {
        SnapHelper snapHelper = new LinearSnapHelper();
        mLayoutManager.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);
        mLayoutManager.waitForSnap(10);

        mLayoutManager.expectLayouts(1);
        scrollToPosition(mConfig.mItemCount / 2);
        mLayoutManager.waitForLayout(2);

        View view = findCenterView(mLayoutManager);
        int scrollDistance = distFromCenter(view) / 2;
        if (scrollDistance == 0) {
            return;
        }

        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);
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

    private int findMinSafeScrollDistance() {
        int minDist = Integer.MAX_VALUE;
        for (int i = mLayoutManager.getChildCount() - 1; i >= 0; i--) {
            final View child = mLayoutManager.getChildAt(i);
            int dist = distFromCenter(child);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist / 2 - 1;
    }

    private int distFromCenter(View view) {
        if (mLayoutManager.canScrollHorizontally()) {
            return Math.abs(mRecyclerView.getWidth() / 2 -
                    mLayoutManager.getViewBounds(view).centerX());
        } else {
            return Math.abs(mRecyclerView.getHeight() / 2 -
                    mLayoutManager.getViewBounds(view).centerY());
        }
    }

    private boolean fling(final int velocityX, final int velocityY)
            throws Throwable {
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
}
