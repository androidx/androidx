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
public class GridLayoutManagerSnappingTest extends BaseGridLayoutManagerTest {

    final Config mConfig;
    final boolean mReverseScroll;

    public GridLayoutManagerSnappingTest(Config config, boolean reverseScroll) {
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
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mGlm);
        assertCenterAligned(view);
        int scrollDistance = (getViewDimension(view) / 2) - 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mGlm.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mGlm.waitForSnap(10);

        // Views have not changed
        View viewAfterFling = findCenterView(mGlm);
        assertSame("The view should have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnScrollNextItem() throws Throwable {
        final Config config = (Config) mConfig.clone();
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mGlm);
        assertCenterAligned(view);
        int scrollDistance = getViewDimension(view) + 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        smoothScrollBy(scrollDist);
        waitForIdleScroll(mRecyclerView);
        waitForIdleScroll(mRecyclerView);

        View viewAfterScroll = findCenterView(mGlm);

        assertNotSame("The view should have scrolled", view, viewAfterScroll);
        assertCenterAligned(viewAfterScroll);
    }

    @Test
    public void snapOnFlingSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mGlm);
        assertCenterAligned(view);

        // Velocity small enough to not scroll to the next view.
        int velocity = (int) (1.000001 * mRecyclerView.getMinFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;
        mGlm.expectIdleState(2);
        assertTrue(fling(velocityDir, velocityDir));
        // Wait for two settling scrolls: the initial one and the corrective one.
        waitForIdleScroll(mRecyclerView);
        mGlm.waitForSnap(100);

        View viewAfterFling = findCenterView(mGlm);

        assertSame("The view should NOT have scrolled", view, viewAfterFling);
        assertCenterAligned(viewAfterFling);
    }

    @Test
    public void snapOnFlingNextView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView(mGlm);
        assertCenterAligned(view);

        // Velocity high enough to scroll beyond the current view.
        int velocity = (int) (0.25 * mRecyclerView.getMaxFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;

        mGlm.expectIdleState(1);
        assertTrue(fling(velocityDir, velocityDir));
        mGlm.waitForSnap(100);
        getInstrumentation().waitForIdleSync();

        View viewAfterFling = findCenterView(mGlm);

        assertNotSame("The view should have scrolled!",
                ((TextView) view).getText(),((TextView) viewAfterFling).getText());
        assertCenterAligned(viewAfterFling);
    }

    private void setupSnapHelper() throws Throwable {
        SnapHelper snapHelper = new LinearSnapHelper();
        mGlm.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);
        mGlm.waitForSnap(10);

        mGlm.expectLayout(1);
        scrollToPosition(mConfig.mItemCount / 2);
        mGlm.waitForLayout(2);

        View view = findCenterView(mGlm);
        int scrollDistance = distFromCenter(view) / 2;
        if (scrollDistance == 0) {
            return;
        }

        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        mGlm.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mGlm.waitForSnap(10);
    }

    @Nullable View findCenterView(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager.canScrollHorizontally()) {
            return mRecyclerView.findChildViewUnder(mRecyclerView.getWidth() / 2, 0);
        } else {
            return mRecyclerView.findChildViewUnder(0, mRecyclerView.getHeight() / 2);
        }
    }

    private int getViewDimension(View view) {
        OrientationHelper helper;
        if (mGlm.canScrollHorizontally()) {
            helper = OrientationHelper.createHorizontalHelper(mGlm);
        } else {
            helper = OrientationHelper.createVerticalHelper(mGlm);
        }
        return helper.getDecoratedMeasurement(view);
    }

    private void assertCenterAligned(View view) {
        if(mGlm.canScrollHorizontally()) {
            assertEquals("The child should align with the center of the parent",
                    mRecyclerView.getWidth() / 2,
                    mGlm.getDecoratedLeft(view) +
                            mGlm.getDecoratedMeasuredWidth(view) / 2, 1);
        } else {
            assertEquals("The child should align with the center of the parent",
                    mRecyclerView.getHeight() / 2,
                    mGlm.getDecoratedTop(view) +
                            mGlm.getDecoratedMeasuredHeight(view) / 2, 1);
        }
    }

    private int distFromCenter(View view) {
        if (mGlm.canScrollHorizontally()) {
            return Math.abs(mRecyclerView.getWidth() / 2 - mGlm.getViewBounds(view).centerX());
        } else {
            return Math.abs(mRecyclerView.getHeight() / 2 - mGlm.getViewBounds(view).centerY());
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
