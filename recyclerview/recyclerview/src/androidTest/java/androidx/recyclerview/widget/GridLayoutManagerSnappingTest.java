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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.TextView;

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
public class GridLayoutManagerSnappingTest extends BaseGridLayoutManagerTest {

    final Config mConfig;
    private final boolean mReverseScroll;
    private final boolean mApplyPadding;

    public GridLayoutManagerSnappingTest(Config config, boolean reverseScroll,
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
    public RecyclerView setupBasic(Config config, GridTestAdapter testAdapter) throws Throwable {
        RecyclerView rv = super.setupBasic(config, testAdapter);
        if (mApplyPadding) {
            rv.setPadding(17, 23, 0, 0);
        }
        return rv;
    }

    @Test
    public void snapOnScrollSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView();
        assertCenterAligned(view);
        int scrollDistance = (getViewDimension(view) / 2) - 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;
        mGlm.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mGlm.waitForSnap(25);

        // Views have not changed
        View viewAfterFling = findCenterView();
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
        View view = findCenterView();
        assertCenterAligned(view);
        CharSequence viewText = ((TextView) view).getText();

        int scrollDistance = getViewDimension(view) + 1;
        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        smoothScrollBy(scrollDist);
        waitForIdleScroll(mRecyclerView);
        waitForIdleScroll(mRecyclerView);

        View viewAfterScroll = findCenterView();
        CharSequence viewAfterFlingText = ((TextView) viewAfterScroll).getText();

        assertNotEquals("The view should have scrolled!", viewText, viewAfterFlingText);
        assertCenterAligned(viewAfterScroll);
    }

    @Test
    public void snapOnFlingSameView() throws Throwable {
        final Config config = (Config) mConfig.clone();
        RecyclerView recyclerView = setupBasic(config);
        waitForFirstLayout(recyclerView);
        setupSnapHelper();

        // Record the current center view.
        View view = findCenterView();
        assertCenterAligned(view);

        // Velocity small enough to not scroll to the next view.
        int velocity = (int) (1.000001 * mRecyclerView.getMinFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;
        mGlm.expectIdleState(2);
        assertTrue(fling(velocityDir, velocityDir));
        // Wait for two settling scrolls: the initial one and the corrective one.
        waitForIdleScroll(mRecyclerView);
        mGlm.waitForSnap(100);

        View viewAfterFling = findCenterView();

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
        View view = findCenterView();
        assertCenterAligned(view);
        CharSequence viewText = ((TextView) view).getText();

        // Velocity high enough to scroll beyond the current view.
        int velocity = (int) (0.25 * mRecyclerView.getMaxFlingVelocity());
        int velocityDir = mReverseScroll ? -velocity : velocity;

        mGlm.expectIdleState(1);
        assertTrue(fling(velocityDir, velocityDir));
        mGlm.waitForSnap(100);
        getInstrumentation().waitForIdleSync();

        View viewAfterFling = findCenterView();
        CharSequence viewAfterFlingText = ((TextView) viewAfterFling).getText();

        assertNotEquals("The view should have scrolled!", viewText, viewAfterFlingText);
        assertCenterAligned(viewAfterFling);
    }

    private void setupSnapHelper() throws Throwable {
        SnapHelper snapHelper = new LinearSnapHelper();
        mGlm.expectIdleState(1);
        snapHelper.attachToRecyclerView(mRecyclerView);
        mGlm.waitForSnap(25);

        mGlm.expectLayout(1);
        scrollToPosition(mConfig.mItemCount / 2);
        mGlm.waitForLayout(2);

        View view = findCenterView();
        int scrollDistance = distFromCenter(view) / 2;
        if (scrollDistance == 0) {
            return;
        }

        int scrollDist = mReverseScroll ? -scrollDistance : scrollDistance;

        mGlm.expectIdleState(2);
        smoothScrollBy(scrollDist);
        mGlm.waitForSnap(25);
    }

    @Nullable
    private View findCenterView() {
        return mRecyclerView.findChildViewUnder(getRvCenterX(), getRvCenterY());
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
        return mGlm.getViewBounds(view).centerX();
    }

    private int getViewCenterY(View view) {
        return mGlm.getViewBounds(view).centerY();
    }

    private void assertCenterAligned(View view) {
        if(mGlm.canScrollHorizontally()) {
            assertEquals("The child should align with the center of the parent",
                    getRvCenterX(), getViewCenterX(view), 1);
        } else {
            assertEquals("The child should align with the center of the parent",
                    getRvCenterY(), getViewCenterY(view), 1);
        }
    }

    private int distFromCenter(View view) {
        if (mGlm.canScrollHorizontally()) {
            return Math.abs(getRvCenterX() - getViewCenterX(view));
        } else {
            return Math.abs(getRvCenterY() - getViewCenterY(view));
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
