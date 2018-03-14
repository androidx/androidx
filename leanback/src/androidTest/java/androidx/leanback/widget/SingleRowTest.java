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
package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.recyclerview.widget.RecyclerView;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Testing SingleRow algorithm
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class SingleRowTest extends GridTest {

    private SingleRow mSingleRow;

    @Test
    public void testAppendPrependRemove() {
        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
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

    @Test
    public void testAppendPrependRemoveReversed() {
        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
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

    @Test
    public void testPrependWithSpacing() {

        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.appendVisibleItems(200);
        assertEquals(dump(mSingleRow) + " Should filled 2 items", 1, mSingleRow.mLastVisibleIndex);

        mProvider.scroll(90);
        mSingleRow.removeInvisibleItemsAtFront(Integer.MAX_VALUE, 0);
        mSingleRow.appendVisibleItems(200);
        assertEquals(dump(mSingleRow) + " Should filled 1 ~ 3", 1, mSingleRow.mFirstVisibleIndex);
        assertEquals(dump(mSingleRow) + " Should filled 1 ~ 3", 3, mSingleRow.mLastVisibleIndex);
        assertEquals(mProvider.getEdge(1), 10);

        mSingleRow.prependVisibleItems(0);
        assertEquals(dump(mSingleRow) + " Should not prepend 0", 1, mSingleRow.mFirstVisibleIndex);
    }

    @Test
    public void testPrependWithSpacingReversed() {

        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.setReversedFlow(true);
        mSingleRow.appendVisibleItems(-200);
        assertEquals(dump(mSingleRow) + " Should filled 2 items", 1, mSingleRow.mLastVisibleIndex);

        mProvider.scroll(-90);
        mSingleRow.removeInvisibleItemsAtFront(Integer.MAX_VALUE, 0);
        mSingleRow.appendVisibleItems(-200);
        assertEquals(dump(mSingleRow) + " Should filled 1 ~ 3", 1, mSingleRow.mFirstVisibleIndex);
        assertEquals(dump(mSingleRow) + " Should filled 1 ~ 3", 3, mSingleRow.mLastVisibleIndex);
        assertEquals(mProvider.getEdge(1), -10);

        mSingleRow.prependVisibleItems(0);
        assertEquals(dump(mSingleRow) + " Should not prepend 0", 1, mSingleRow.mFirstVisibleIndex);
    }

    public void validatePrefetch(int fromLimit, int delta, Integer[]... positionData) {
        // duplicates logic in support.v7.widget.CacheUtils#verifyPositionsPrefetched
        RecyclerView.LayoutManager.LayoutPrefetchRegistry registry
                = mock(RecyclerView.LayoutManager.LayoutPrefetchRegistry.class);
        mSingleRow.collectAdjacentPrefetchPositions(fromLimit, delta, registry);

        verify(registry, times(positionData.length)).addPosition(anyInt(), anyInt());
        for (Integer[] aPositionData : positionData) {
            verify(registry).addPosition(aPositionData[0], aPositionData[1]);
        }
    }

    @Test
    public void testPrefetchBounds() {
        mProvider = new Provider(new int[]{100, 100});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.appendVisibleItems(150);

        validatePrefetch(0, -10);
        validatePrefetch(-150, 10);
    }

    @Test
    public void testPrefetchBoundsReversed() {
        mProvider = new Provider(new int[]{100, 100});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.setReversedFlow(true);
        mSingleRow.appendVisibleItems(-150);

        validatePrefetch(0, -10);
        validatePrefetch(150, 10);
    }

    @Test
    public void testPrefetchItems() {
        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.appendVisibleItems(200);

        // next item, 2, is 0 pixels away
        validatePrefetch(200, 10, new Integer[] {2, 0});

        // nothing above
        validatePrefetch(0, -10);

        mProvider.scroll(90);
        mSingleRow.removeInvisibleItemsAtFront(Integer.MAX_VALUE, 0);
        mSingleRow.appendVisibleItems(200);

        // next item, 4, is 80 pixels away
        validatePrefetch(200, 10, new Integer[] {4, 80});

        // next item, 0, is 10 pixels away
        validatePrefetch(0, -10, new Integer[] {0, 10});
    }

    @Test
    public void testPrefetchItemsReversed() {
        mProvider = new Provider(new int[]{80, 80, 30, 100, 40, 10});

        mSingleRow = new SingleRow();
        mSingleRow.setSpacing(20);
        mSingleRow.setProvider(mProvider);
        mSingleRow.setReversedFlow(true);
        mSingleRow.appendVisibleItems(-200);

        // next item, 2, is 0 pixels away
        validatePrefetch(-200, -10, new Integer[] {2, 0});

        // nothing above
        validatePrefetch(0, 10);

        mProvider.scroll(-90);
        mSingleRow.removeInvisibleItemsAtFront(Integer.MAX_VALUE, 0);
        mSingleRow.appendVisibleItems(-200);

        // next item, 4, is 80 pixels away
        validatePrefetch(-200, -10, new Integer[] {4, 80});

        // one above, 0, is 10 pixels away
        validatePrefetch(0, 10, new Integer[] {0, 10});

    }
}