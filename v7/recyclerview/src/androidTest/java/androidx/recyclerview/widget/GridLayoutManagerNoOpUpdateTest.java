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
import static org.junit.Assert.assertNotNull;

import android.graphics.Rect;
import android.support.test.filters.MediumTest;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests dispatching no-op updates to the GLM and ensures it re-lays out items in the same location
 */
@MediumTest
@RunWith(Parameterized.class)
public class GridLayoutManagerNoOpUpdateTest extends BaseGridLayoutManagerTest {
    @Parameterized.Parameters(name = "conf:{0},rtl={1}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        for (BaseGridLayoutManagerTest.Config config : createBaseVariations()) {
            result.add(new Object[]{
                    config,
                    true
            });
            result.add(new Object[]{
                    config,
                    false
            });
        }
        return result;
    }

    private final Config mConfig;
    private final boolean mRtl;

    public GridLayoutManagerNoOpUpdateTest(Config config, boolean rtl) {
        mConfig = config;
        mRtl = rtl;
    }

    @Test
    public void rtlChanges() throws Throwable {
        RecyclerView rv = createRecyclerView();
        mGlm.setFakeRtl(mRtl);
        waitForFirstLayout(rv);
        Map<Long, Rect> before = takeSnapshot();

        View chosen = mGlm.findViewByPosition(1);
        assertNotNull("test sanity", chosen);
        mGlm.expectLayout(2);
        mAdapter.changeAndNotify(1, 1);
        mGlm.waitForLayout(2);
        Map<Long, Rect> after = takeSnapshot();
        assertSnapshotsEqual(before, after);
    }

    private void assertSnapshotsEqual(Map<Long, Rect> before, Map<Long, Rect> after) {
        for (Map.Entry<Long, Rect> entry : before.entrySet()) {
            Rect newPosition = after.get(entry.getKey());
            assertNotNull("cannot find " + entry.getKey() + " in after map", newPosition);
            assertEquals("position should be the same", entry.getValue(), newPosition);
        }
        assertEquals("visible view count should be equal", before.size(), after.size());
    }

    private Map<Long, Rect> takeSnapshot() {
        Rect rvBounds = new Rect();
        if (mRecyclerView.getClipToPadding()) {
            rvBounds.set(mRecyclerView.getPaddingLeft(), mRecyclerView.getPaddingTop(),
                    mRecyclerView.getWidth() - mRecyclerView.getPaddingRight(),
                    mRecyclerView.getHeight() - mRecyclerView.getPaddingBottom());
        } else {
            rvBounds.set(0, 0, mRecyclerView.getWidth(), mRecyclerView.getHeight());
        }
        Map<Long, Rect> positionMap = new HashMap<>();
        for (int i = 0; i < mGlm.getChildCount(); i++) {
            View child = mGlm.getChildAt(i);
            Rect childBounds = getChildBounds(mRecyclerView, child, true);
            long id = mRecyclerView.getChildViewHolder(child).getItemId();
            if (rvBounds.intersect(childBounds)) {
                positionMap.put(id, childBounds);
            }
        }
        return positionMap;
    }

    private Rect getChildBounds(RecyclerView recyclerView, View child, boolean offset) {
        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        Rect rect = new Rect(layoutManager.getDecoratedLeft(child) - lp.leftMargin,
                layoutManager.getDecoratedTop(child) - lp.topMargin,
                layoutManager.getDecoratedRight(child) + lp.rightMargin,
                layoutManager.getDecoratedBottom(child) + lp.bottomMargin);
        return rect;
    }

    private RecyclerView createRecyclerView() throws Throwable {
        GridTestAdapter adapter = new GridTestAdapter(mConfig.mItemCount);
        adapter.setHasStableIds(true);
        return setupBasic(mConfig, adapter);
    }
}
