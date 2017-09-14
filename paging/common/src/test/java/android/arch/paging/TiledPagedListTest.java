/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.arch.paging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(JUnit4.class)
public class TiledPagedListTest {

    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mBackgroundThread = new TestExecutor();

    private static final ArrayList<Item> ITEMS = new ArrayList<>();

    static {
        for (int i = 0; i < 45; i++) {
            ITEMS.add(new Item(i));
        }
    }

    // use a page size that's not an even divisor of ITEMS.size() to test end conditions
    private static final int PAGE_SIZE = 10;

    private static class Item {
        private Item(int position) {
            this.position = position;
            this.name = "Item " + position;
        }

        @SuppressWarnings("WeakerAccess")
        public final int position;
        public final String name;

        @Override
        public String toString() {
            return name;
        }
    }

    private static class TestTiledSource extends TiledDataSource<Item> {
        @Override
        public int countItems() {
            return ITEMS.size();
        }

        @Override
        public List<Item> loadRange(int startPosition, int count) {
            int endPosition = Math.min(ITEMS.size(), startPosition + count);
            return ITEMS.subList(startPosition, endPosition);
        }
    }

    private void verifyRange(PageArrayList<Item> list, Integer... loadedPages) {
        List<Integer> loadedPageList = Arrays.asList(loadedPages);
        assertEquals(ITEMS.size(), list.size());
        for (int i = 0; i < list.size(); i++) {
            if (loadedPageList.contains(i / PAGE_SIZE)) {
                assertSame(ITEMS.get(i), list.get(i));
            } else {
                assertEquals(null, list.get(i));
            }
        }
    }
    private TiledPagedList<Item> createTiledPagedList(int loadPosition) {
        return createTiledPagedList(loadPosition, PAGE_SIZE);
    }

    private TiledPagedList<Item> createTiledPagedList(int loadPosition, int prefetchDistance) {
        TestTiledSource source = new TestTiledSource();
        return new TiledPagedList<>(
                source, mMainThread, mBackgroundThread,
                new PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setPrefetchDistance(prefetchDistance)
                        .build(),
                loadPosition);
    }

    @Test
    public void initialLoad() {
        TiledPagedList<Item> pagedList = createTiledPagedList(0);
        verifyRange(pagedList, 0);
    }

    @Test
    public void initialLoad_end() {
        TiledPagedList<Item> pagedList = createTiledPagedList(44);
        verifyRange(pagedList, 3, 4);
    }

    @Test
    public void initialLoad_multiple() {
        TiledPagedList<Item> pagedList = createTiledPagedList(9);
        verifyRange(pagedList, 0, 1);
    }

    @Test
    public void initialLoad_offset() {
        TiledPagedList<Item> pagedList = createTiledPagedList(41);
        verifyRange(pagedList, 3, 4);
    }

    @Test
    public void append() {
        TiledPagedList<Item> pagedList = createTiledPagedList(0);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(pagedList, 0);
        verifyZeroInteractions(callback);

        pagedList.loadAround(5);
        drain();

        verifyRange(pagedList, 0, 1);
        verify(callback).onChanged(10, 10);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void prepend() {
        TiledPagedList<Item> pagedList = createTiledPagedList(44);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(pagedList, 3, 4);
        verifyZeroInteractions(callback);

        pagedList.loadAround(35);
        drain();

        verifyRange(pagedList, 2, 3, 4);
        verify(callback).onChanged(20, 10);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void loadWithGap() {
        TiledPagedList<Item> pagedList = createTiledPagedList(0);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(pagedList, 0);
        verifyZeroInteractions(callback);

        pagedList.loadAround(44);
        drain();

        verifyRange(pagedList, 0, 3, 4);
        verify(callback).onChanged(30, 10);
        verify(callback).onChanged(40, 5);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void tinyPrefetchTest() {
        TiledPagedList<Item> pagedList = createTiledPagedList(0, 1);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(pagedList, 0); // just 4 loaded
        verifyZeroInteractions(callback);

        pagedList.loadAround(23);
        drain();

        verifyRange(pagedList, 0, 2);
        verify(callback).onChanged(20, 10);
        verifyNoMoreInteractions(callback);

        pagedList.loadAround(44);
        drain();

        verifyRange(pagedList, 0, 2, 4);
        verify(callback).onChanged(40, 5);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void appendCallbackAddedLate() {
        TiledPagedList<Item> pagedList = createTiledPagedList(0, 0);
        verifyRange(pagedList, 0);

        pagedList.loadAround(15);
        drain();
        verifyRange(pagedList, 0, 1);

        // snapshot at 20 items
        PageArrayList<Item> snapshot = (PageArrayList<Item>) pagedList.snapshot();
        verifyRange(snapshot, 0, 1);


        pagedList.loadAround(25);
        pagedList.loadAround(35);
        drain();
        verifyRange(pagedList, 0, 1, 2, 3);
        verifyRange(snapshot, 0, 1);

        PagedList.Callback callback = mock(
                PagedList.Callback.class);
        pagedList.addWeakCallback(snapshot, callback);
        verify(callback).onChanged(20, 20);
        verifyNoMoreInteractions(callback);
    }


    @Test
    public void prependCallbackAddedLate() {
        TiledPagedList<Item> pagedList = createTiledPagedList(44, 0);
        verifyRange(pagedList, 3, 4);

        pagedList.loadAround(25);
        drain();
        verifyRange(pagedList, 2, 3, 4);

        // snapshot at 30 items
        PageArrayList<Item> snapshot = (PageArrayList<Item>) pagedList.snapshot();
        verifyRange(snapshot, 2, 3, 4);


        pagedList.loadAround(15);
        pagedList.loadAround(5);
        drain();
        verifyRange(pagedList, 0, 1, 2, 3, 4);
        verifyRange(snapshot, 2, 3, 4);

        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(snapshot, callback);
        verify(callback).onChanged(0, 20);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void placeholdersDisabled() {
        // disable placeholders with config, so we create a contiguous version of the pagedlist
        PagedList<Item> pagedList = new PagedList.Builder<Integer, Item>()
                .setDataSource(new TestTiledSource())
                .setMainThreadExecutor(mMainThread)
                .setBackgroundThreadExecutor(mBackgroundThread)
                .setConfig(new PagedList.Config.Builder()
                        .setPageSize(PAGE_SIZE)
                        .setPrefetchDistance(PAGE_SIZE)
                        .setInitialLoadSizeHint(PAGE_SIZE)
                        .setEnablePlaceholders(false)
                        .build())
                .setInitialKey(20)
                .build();

        assertTrue(pagedList.isContiguous());

        ContiguousPagedList<Item> contiguousPagedList = (ContiguousPagedList<Item>) pagedList;
        assertEquals(0, contiguousPagedList.getLeadingNullCount());
        assertEquals(PAGE_SIZE, contiguousPagedList.mList.size());
        assertEquals(0, contiguousPagedList.getTrailingNullCount());
    }

    private void drain() {
        boolean executed;
        do {
            executed = mBackgroundThread.executeAll();
            executed |= mMainThread.executeAll();
        } while (executed);
    }
}
