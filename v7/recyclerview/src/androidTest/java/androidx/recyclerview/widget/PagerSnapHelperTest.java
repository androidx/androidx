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
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@LargeTest
@RunWith(Parameterized.class)
public class PagerSnapHelperTest extends BaseLinearLayoutManagerTest {

    final Config mConfig;
    final boolean mReverseScroll;

    public PagerSnapHelperTest(Config config, boolean reverseScroll) {
        mConfig = config;
        mReverseScroll = reverseScroll;
    }

    @Parameterized.Parameters(name = "config:{0},reverseScroll:{1}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        List<Config> configs = createBaseVariations();
        for (Config config : configs) {
            for (boolean reverseScroll : new boolean[] {false, true}) {
                result.add(new Object[]{config, reverseScroll});
            }
        }
        return result;
    }

    @Test
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true,
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT),
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        setupSnapHelper();

        // Record the current center view.
        TextView view = (TextView) findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int scrollDistance = (getViewDimension(view) / 2) - 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(3);
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
        setupByConfig(config, true,
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT),
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int scrollDistance = (getViewDimension(view) / 2) + 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(3);
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
        setupByConfig(config, true,
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT),
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
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
        setupByConfig(config, true,
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT),
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        setupSnapHelper();
        runSnapOnMaxFlingNextView((int) (0.2 * mRecyclerView.getMaxFlingVelocity()));
    }

    @Test
    public void snapOnMaxFlingNextView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true,
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT),
                new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        setupSnapHelper();
        runSnapOnMaxFlingNextView(mRecyclerView.getMaxFlingVelocity());
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
        getInstrumentation().waitForIdleSync();

        View viewAfterFling = findCenterView(mLayoutManager);

        assertNotSame("The view should have scrolled", view, viewAfterFling);
        int expectedPosition = mConfig.mItemCount / 2 + (mConfig.mReverseLayout
                ? (mReverseScroll ? 1 : -1)
                : (mReverseScroll ? -1 : 1));
        assertEquals(expectedPosition, mLayoutManager.getPosition(viewAfterFling));
        assertCenterAligned(viewAfterFling);
    }

    private void setupSnapHelper() throws Throwable {
        SnapHelper snapHelper = new PagerSnapHelper();
        mLayoutManager.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);

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
}
