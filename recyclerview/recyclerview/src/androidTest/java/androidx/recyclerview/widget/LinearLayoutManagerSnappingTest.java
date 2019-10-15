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

import android.view.View;

import androidx.annotation.Nullable;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@LargeTest
@RunWith(Parameterized.class)
public class LinearLayoutManagerSnappingTest extends BaseLinearLayoutManagerTest {

    final Config mConfig;
    private final boolean mReverseScroll;
    private final boolean mApplyPadding;

    public LinearLayoutManagerSnappingTest(Config config, boolean reverseScroll,
            boolean applyPadding) {
        mConfig = config;
        mReverseScroll = reverseScroll;
        mApplyPadding = applyPadding;
    }

    @Parameterized.Parameters(name = "config:{0},reverseScroll:{1},applyPadding:{2}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        List<Config> configs = createBaseVariations();
        for (Config config : configs) {
            for (boolean reverseScroll : new boolean[] {true, false}) {
                for (boolean applyPadding : new boolean[] {true, false}) {
                    result.add(new Object[]{config, reverseScroll, applyPadding});
                }
            }
        }
        return result;
    }

    @Override
    void setupByConfig(Config config, boolean waitForFirstLayout,
            @Nullable RecyclerView.LayoutParams childLayoutParams,
            @Nullable RecyclerView.LayoutParams parentLayoutParams) throws Throwable {
        super.setupByConfig(config, false, childLayoutParams, parentLayoutParams);
        if (mApplyPadding) {
            mRecyclerView.setPadding(17, 23, 0, 0);
        }
        if (waitForFirstLayout) {
            waitForFirstLayout();
        }
    }

    @Test
    public void snapOnScrollSameViewEdge() throws Throwable {
        final Config config = (Config) mConfig.clone();
        // Ensure that the views are big enough to reach the pathological case when the view closest
        // to the center is an edge view, but it cannot scroll further in order to snap.
        setupByConfig(config, true, new RecyclerView.LayoutParams(1000, 1000),
            new RecyclerView.LayoutParams(1500, 1500));
        SnapHelper snapHelper = new LinearSnapHelper();
        mLayoutManager.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);
        mLayoutManager.waitForSnap(25);

        // Record the current center view.
        View view = findCenterView(mLayoutManager);

        int scrollDistance = (getViewDimension(view) / 2) - 1;
        int scrollDist = config.mStackFromEnd == config.mReverseLayout
            ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(1);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(25);
        mLayoutManager.expectCallbacks(5);
        mLayoutManager.assertNoCallbacks("There should be no callbacks after some time", 3);
    }

    @Test
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int scrollDistance = (getViewDimension(view) / 2) - 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(25);

        // Views have not changed
        View viewAfterFling = findCenterView(mLayoutManager);
        assertSame("The view should NOT have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnScrollNextView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mLayoutManager);
        assertCenterAligned(view);

        int scrollDistance = (getViewDimension(view) / 2) + 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mLayoutManager.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mLayoutManager.waitForSnap(25);

        // Views have not changed
        View viewAfterFling = findCenterView(mLayoutManager);
        assertNotSame("The view should have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnFlingSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        setupByConfig(config, true);
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
        setupByConfig(config, true);
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
        mLayoutManager.waitForSnap(25);

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
        mLayoutManager.waitForSnap(25);
    }

    @Nullable
    private View findCenterView(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager.canScrollHorizontally()) {
            return mRecyclerView.findChildViewUnder(getRvCenterX(), mRecyclerView.getPaddingTop());
        } else {
            return mRecyclerView.findChildViewUnder(mRecyclerView.getPaddingLeft(), getRvCenterY());
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

    private int getWidthMinusPadding(View view) {
        return view.getWidth() - view.getPaddingLeft() - view.getPaddingRight();
    }

    private int getHeightMinusPadding(View view) {
        return view.getHeight() - view.getPaddingTop() - view.getPaddingBottom();
    }

    private int getRvCenterX() {
        return getWidthMinusPadding(mRecyclerView) / 2 + mRecyclerView.getPaddingLeft();
    }

    private int getRvCenterY() {
        return getHeightMinusPadding(mRecyclerView) / 2 + mRecyclerView.getPaddingTop();
    }

    private int getViewCenterX(View view) {
        return mLayoutManager.getViewBounds(view).centerX();
    }

    private int getViewCenterY(View view) {
        return mLayoutManager.getViewBounds(view).centerY();
    }

    private void assertCenterAligned(View view) {
        if (mLayoutManager.canScrollHorizontally()) {
            assertEquals(getRvCenterX(), getViewCenterX(view));
        } else {
            assertEquals(getRvCenterY(), getViewCenterY(view));
        }
    }

    private int distFromCenter(View view) {
        if (mLayoutManager.canScrollHorizontally()) {
            return Math.abs(getRvCenterX() - getViewCenterX(view));
        } else {
            return Math.abs(getRvCenterY() - getViewCenterY(view));
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
