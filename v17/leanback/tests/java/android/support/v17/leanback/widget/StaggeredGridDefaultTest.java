/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.widget;

/**
 * Testing StaggeredGridDefault algorithm
 * @hide
 */
public class StaggeredGridDefaultTest extends GridTest {

    StaggeredGridDefault mStaggeredGrid;

    public void testWhenToFillNextRow() throws Throwable {
        mProvider = new Provider(new int[]{100, 100, 100, 100, 40, 100, 100, 30, 100});

        // layout first 8 items then all items
        mStaggeredGrid = new StaggeredGridDefault();
        mStaggeredGrid.setNumRows(3);
        mStaggeredGrid.setMargin(20);
        mStaggeredGrid.setProvider(mProvider);
        mStaggeredGrid.appendVisibleItems(210);
        assertEquals(dump(mStaggeredGrid) + " Should fill 8 items",
                8, mStaggeredGrid.mLocations.size());
        // 2nd fill rest
        mStaggeredGrid.appendVisibleItems(100000);
        assertEquals(dump(mStaggeredGrid) + " Should fill 9 items",
                9, mStaggeredGrid.mLocations.size());
        int row_result1 = mStaggeredGrid.getLocation(8).row;
        assertEquals(dump(mStaggeredGrid) + " last item should be placed on row 1",
                1, row_result1);

        // layout all items together
        mStaggeredGrid = new StaggeredGridDefault();
        mStaggeredGrid.setNumRows(3);
        mStaggeredGrid.setMargin(20);
        mStaggeredGrid.setProvider(mProvider);
        mStaggeredGrid.appendVisibleItems(100000);
        assertEquals(dump(mStaggeredGrid) + " should fill 9 items",
                9, mStaggeredGrid.mLocations.size());
        int row_result2 = mStaggeredGrid.getLocation(8).row;

        assertEquals(dump(mStaggeredGrid) + " last item should be placed on row 1",
                1, row_result2);
    }
}
