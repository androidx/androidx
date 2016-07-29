/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v7.widget;

import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;

@RunWith(Parameterized.class)
public class StaggeredGridLayoutManagerSnappingTest extends BaseStaggeredGridLayoutManagerTest {

    final Config mConfig;
    final boolean mReverseScroll;

    public StaggeredGridLayoutManagerSnappingTest(Config config, boolean reverseScroll) {
        mConfig = config;
        mReverseScroll = reverseScroll;
    }

    @Parameterized.Parameters(name = "config:{0}, reverseScroll:{1}")
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

    @MediumTest
    @Test
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config);
        waitFirstLayout();
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);
        // For a staggered grid layout manager we need to keep the distance
        // small enough to ensure we do not scroll over to an offset view in a different span.
        int scrollDistance = 5;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);

        // Views have not changed
        View viewAfterFling = findCenterView(mLayoutManager);
        assertSame("The view should have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
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

    @MediumTest
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


    @MediumTest
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

    private void setupSnapHelper() throws Throwable {
        SnapHelper snapHelper = new LinearSnapHelper();
        mLayoutManager.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);
        mLayoutManager.waitForSnap(10);

        mLayoutManager.expectLayouts(1);
        scrollToPosition(mConfig.mItemCount / 2);
        mLayoutManager.waitForLayout(2);

        View view = findCenterView(mLayoutManager);
        int scrollDistance = (getViewDimension(view) / 2) + 10;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(10);
    }

    @Nullable View findCenterView(RecyclerView.LayoutManager layoutManager) {
        return mLayoutManager.findFirstVisibleItemClosestToCenter();
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

    private boolean fling(final int velocityX, final int velocityY)
            throws Throwable {
        final AtomicBoolean didStart = new AtomicBoolean(false);
        runTestOnUiThread(new Runnable() {
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
