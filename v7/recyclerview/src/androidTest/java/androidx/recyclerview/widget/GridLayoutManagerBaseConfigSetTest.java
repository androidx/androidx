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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.MediumTest;
import android.util.Log;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

@MediumTest
@RunWith(Parameterized.class)
public class GridLayoutManagerBaseConfigSetTest extends BaseGridLayoutManagerTest {
    @Parameterized.Parameters(name = "{0}")
    public static List<Config> params() {
        return createBaseVariations();
    }

    private final Config mConfig;

    public GridLayoutManagerBaseConfigSetTest(Config config) {
        mConfig = config;
    }

    @Test
    public void scrollBackAndPreservePositions() throws Throwable {
        Config config = (Config) mConfig.clone();
        config.mItemCount = 150;
        scrollBackAndPreservePositionsTest(config);
    }

    public void scrollBackAndPreservePositionsTest(final Config config) throws Throwable {
        final RecyclerView rv = setupBasic(config);
        for (int i = 1; i < mAdapter.getItemCount(); i += config.mSpanCount + 2) {
            mAdapter.setFullSpan(i);
        }
        waitForFirstLayout(rv);
        final int[] globalPositions = new int[mAdapter.getItemCount()];
        Arrays.fill(globalPositions, Integer.MIN_VALUE);
        final int scrollStep = (mGlm.mOrientationHelper.getTotalSpace() / 20)
                * (config.mReverseLayout ? -1 : 1);
        final String logPrefix = config.toString();
        final int[] globalPos = new int[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertSame("test sanity", mRecyclerView, rv);
                int globalScrollPosition = 0;
                int visited = 0;
                while (visited < mAdapter.getItemCount()) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        final int pos = mRecyclerView.getChildLayoutPosition(child);
                        if (globalPositions[pos] != Integer.MIN_VALUE) {
                            continue;
                        }
                        visited++;
                        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams)
                                child.getLayoutParams();
                        if (config.mReverseLayout) {
                            globalPositions[pos] = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedEnd(child);
                        } else {
                            globalPositions[pos] = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedStart(child);
                        }
                        assertEquals(logPrefix + " span index should match",
                                mGlm.getSpanSizeLookup().getSpanIndex(pos, mGlm.getSpanCount()),
                                lp.getSpanIndex());
                    }
                    int scrolled = mGlm.scrollBy(scrollStep,
                            mRecyclerView.mRecycler, mRecyclerView.mState);
                    globalScrollPosition += scrolled;
                    if (scrolled == 0) {
                        assertEquals(
                                logPrefix + " If scroll is complete, all views should be visited",
                                visited, mAdapter.getItemCount());
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "done recording positions " + Arrays.toString(globalPositions));
                }
                globalPos[0] = globalScrollPosition;
            }
        });
        checkForMainThreadException();
        // test sanity, ensure scroll happened
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int childCount = mGlm.getChildCount();
                final BitSet expectedPositions = new BitSet();
                for (int i = 0; i < childCount; i ++) {
                    expectedPositions.set(mAdapter.getItemCount() - i - 1);
                }
                for (int i = 0; i <childCount; i ++) {
                    final View view = mGlm.getChildAt(i);
                    int position = mGlm.getPosition(view);
                    assertTrue("child position should be in last page", expectedPositions.get(position));
                }
            }
        });
        getInstrumentation().waitForIdleSync();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int globalScrollPosition = globalPos[0];
                // now scroll back and make sure global positions match
                BitSet shouldTest = new BitSet(mAdapter.getItemCount());
                shouldTest.set(0, mAdapter.getItemCount() - 1, true);
                String assertPrefix = config
                        + " global pos must match when scrolling in reverse for position ";
                int scrollAmount = Integer.MAX_VALUE;
                while (!shouldTest.isEmpty() && scrollAmount != 0) {
                    for (int i = 0; i < mRecyclerView.getChildCount(); i++) {
                        View child = mRecyclerView.getChildAt(i);
                        int pos = mRecyclerView.getChildLayoutPosition(child);
                        if (!shouldTest.get(pos)) {
                            continue;
                        }
                        GridLayoutManager.LayoutParams lp = (GridLayoutManager.LayoutParams)
                                child.getLayoutParams();
                        shouldTest.clear(pos);
                        int globalPos;
                        if (config.mReverseLayout) {
                            globalPos = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedEnd(child);
                        } else {
                            globalPos = globalScrollPosition +
                                    mGlm.mOrientationHelper.getDecoratedStart(child);
                        }
                        assertEquals(assertPrefix + pos,
                                globalPositions[pos], globalPos);
                        assertEquals("span index should match",
                                mGlm.getSpanSizeLookup().getSpanIndex(pos, mGlm.getSpanCount()),
                                lp.getSpanIndex());
                    }
                    scrollAmount = mGlm.scrollBy(-scrollStep,
                            mRecyclerView.mRecycler, mRecyclerView.mState);
                    globalScrollPosition += scrollAmount;
                }
                assertTrue("all views should be seen", shouldTest.isEmpty());
            }
        });
        checkForMainThreadException();
    }
}
