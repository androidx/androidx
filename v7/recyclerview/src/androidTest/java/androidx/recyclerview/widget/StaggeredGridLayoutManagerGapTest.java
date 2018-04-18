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

import static androidx.recyclerview.widget.StaggeredGridLayoutManager.GAP_HANDLING_NONE;

import static org.junit.Assert.assertNull;

import android.graphics.Rect;
import android.support.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(Parameterized.class)
@LargeTest
public class StaggeredGridLayoutManagerGapTest extends BaseStaggeredGridLayoutManagerTest {
    private final Config mConfig;
    private final int mDeletePosition;
    private final int mDeleteCount;

    public StaggeredGridLayoutManagerGapTest(Config config, int deletePosition, int deleteCount) {
        mConfig = config;
        mDeletePosition = deletePosition;
        mDeleteCount = deleteCount;
    }

    @Parameterized.Parameters(name = "config={0},deletePos={1},deleteCount={2}")
    public static List<Object[]> getParams() throws CloneNotSupportedException {
        List<Config> variations = createBaseVariations();
        List<Object[]> params = new ArrayList<>();
        for (Config config : variations) {
            for (int deleteCount = 1; deleteCount < config.mSpanCount * 2; deleteCount++) {
                for (int deletePosition = config.mSpanCount - 1;
                        deletePosition < config.mSpanCount + 2; deletePosition++) {
                    params.add(new Object[]{config.clone(), deletePosition, deleteCount});
                }
            }
        }
        return params;
    }

    @Test
    public void gapAtTheBeginningOfTheListTest() throws Throwable {
        if (mConfig.mSpanCount < 2 || mConfig.mGapStrategy == GAP_HANDLING_NONE) {
            return;
        }
        if (mConfig.mItemCount < 100) {
            mConfig.itemCount(100);
        }
        setupByConfig(mConfig);
        final RecyclerView.Adapter adapter = mAdapter;
        waitFirstLayout();
        // scroll far away
        smoothScrollToPosition(mConfig.mItemCount / 2);
        checkForMainThreadException();
        // assert to be deleted child is not visible
        assertNull(" test sanity, to be deleted child should be invisible",
                mRecyclerView.findViewHolderForLayoutPosition(mDeletePosition));
        // delete the child and notify
        mAdapter.deleteAndNotify(mDeletePosition, mDeleteCount);
        getInstrumentation().waitForIdleSync();
        mLayoutManager.expectLayouts(1);
        smoothScrollToPosition(0);
        mLayoutManager.waitForLayout(2);
        checkForMainThreadException();
        // due to data changes, first item may become visible before others which will cause
        // smooth scrolling to stop. Triggering it twice more is a naive hack.
        // Until we have time to consider it as a bug, this is the only workaround.
        smoothScrollToPosition(0);
        Thread.sleep(500);
        checkForMainThreadException();
        smoothScrollToPosition(0);
        Thread.sleep(500);
        checkForMainThreadException();
        // some animations should happen and we should recover layout
        final Map<Item, Rect> actualCoords = mLayoutManager.collectChildCoordinates();

        // now layout another RV with same adapter
        removeRecyclerView();
        setupByConfig(mConfig);
        mRecyclerView.setAdapter(adapter);// use same adapter so that items can be matched
        waitFirstLayout();
        final Map<Item, Rect> desiredCoords = mLayoutManager.collectChildCoordinates();
        assertRectSetsEqual(" when an item from the start of the list is deleted, "
                        + "layout should recover the state once scrolling is stopped",
                desiredCoords, actualCoords);
        checkForMainThreadException();
    }
}
