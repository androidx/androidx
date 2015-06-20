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
 * Testing SingleRow algorithm
 * @hide
 */
public class SingleRowTest extends GridTest {

    SingleRow mSingleRow;

    public void testAppendPrependRemove() throws Throwable {
        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setMargin(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.appendVisibleItems(200);
        assertEquals(dump(mSingleRow) + " Should filled 2 items", 1, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(201);
        assertEquals(dump(mSingleRow) + " Should filled 3 items",
                2, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(251);
        assertEquals(dump(mSingleRow) + " Should filled 4 items",
                3, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(Integer.MAX_VALUE);
        assertEquals(dump(mSingleRow) + " Should filled 6 items",
               5, mSingleRow.mLastVisibleIndex);
        assertEquals(mProvider.getEdge(0), 0);
        assertEquals(mProvider.getEdge(1), 100);
        assertEquals(mProvider.getEdge(2), 200);
        assertEquals(mProvider.getEdge(3), 250);
        assertEquals(mProvider.getEdge(4), 370);
        assertEquals(mProvider.getEdge(5), 430);

        mSingleRow.removeInvisibleItemsAtEnd(0, 200);
        assertEquals(dump(mSingleRow) + " Should filled 2 items", 1, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(Integer.MAX_VALUE);
        assertEquals(dump(mSingleRow) + " Should filled 6 items",
               5, mSingleRow.mLastVisibleIndex);

        mSingleRow.removeInvisibleItemsAtFront(1000, 80);
        assertEquals(dump(mSingleRow) + " visible index should start from 1",
                1, mSingleRow.mFirstVisibleIndex);

        mSingleRow.prependVisibleItems(0);
        assertEquals(dump(mSingleRow) + " visible index should start from 0",
                0, mSingleRow.mFirstVisibleIndex);
    }

    public void testAppendPrependRemoveReversed() throws Throwable {
        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setMargin(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.setReversedFlow(true);
        mSingleRow.appendVisibleItems(-200);
        assertEquals(dump(mSingleRow) + " Should filled 2 items", 1, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(-201);
        assertEquals(dump(mSingleRow) + " Should filled 3 items",
                2, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(-251);
        assertEquals(dump(mSingleRow) + " Should filled 4 items",
                3, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(Integer.MIN_VALUE);
        assertEquals(dump(mSingleRow) + " Should filled 6 items",
               5, mSingleRow.mLastVisibleIndex);
        assertEquals(mProvider.getEdge(0), 0);
        assertEquals(mProvider.getEdge(1), -100);
        assertEquals(mProvider.getEdge(2), -200);
        assertEquals(mProvider.getEdge(3), -250);
        assertEquals(mProvider.getEdge(4), -370);
        assertEquals(mProvider.getEdge(5), -430);

        mSingleRow.removeInvisibleItemsAtEnd(0, -200);
        assertEquals(dump(mSingleRow) + " Should filled 2 items", 1, mSingleRow.mLastVisibleIndex);

        mSingleRow.appendVisibleItems(Integer.MIN_VALUE);
        assertEquals(dump(mSingleRow) + " Should filled 6 items",
               5, mSingleRow.mLastVisibleIndex);

        mSingleRow.removeInvisibleItemsAtFront(1000, -80);
        assertEquals(dump(mSingleRow) + " Should filled 6 items",
                1, mSingleRow.mFirstVisibleIndex);
    }
}
