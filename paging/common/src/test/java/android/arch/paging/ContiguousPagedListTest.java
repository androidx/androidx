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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.support.annotation.Nullable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class ContiguousPagedListTest {

    @Parameterized.Parameters(name = "counted:{0}")
    public static List<Object[]> parameters() {
        return Arrays.asList(new Object[][]{{true}, {false}});
    }

    public ContiguousPagedListTest(boolean counted) {
        mCounted = counted;
    }

    private final boolean mCounted;
    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mBackgroundThread = new TestExecutor();

    private static final ArrayList<Item> ITEMS = new ArrayList<>();

    static {
        for (int i = 0; i < 100; i++) {
            ITEMS.add(new Item(i));
        }
    }

    @SuppressWarnings("WeakerAccess")
    private static class Item {
        private Item(int position) {
            this.position = position;
            this.name = "Item " + position;
        }

        public final int position;
        public final String name;

        @Override
        public String toString() {
            return name;
        }
    }

    private class TestSource extends PositionalDataSource<Item> {
        @Override
        public int countItems() {
            if (mCounted) {
                return ITEMS.size();
            } else {
                return COUNT_UNDEFINED;
            }
        }

        private List<Item> getClampedRange(int startInc, int endExc, boolean reverse) {
            startInc = Math.max(0, startInc);
            endExc = Math.min(ITEMS.size(), endExc);
            List<Item> list = ITEMS.subList(startInc, endExc);
            if (reverse) {
                Collections.reverse(list);
            }
            return list;
        }

        @Nullable
        @Override
        public List<Item> loadAfter(int startIndex, int pageSize) {
            return getClampedRange(startIndex, startIndex + pageSize, false);
        }

        @Nullable
        @Override
        public List<Item> loadBefore(int startIndex, int pageSize) {
            return getClampedRange(startIndex - pageSize + 1, startIndex + 1, true);
        }
    }

    private void verifyRange(int start, int count, NullPaddedList<Item> actual) {
        if (mCounted) {
            int expectedLeading = start;
            int expectedTrailing = ITEMS.size() - start - count;
            assertEquals(ITEMS.size(), actual.size());
            assertEquals(ITEMS.size() - expectedLeading - expectedTrailing,
                    actual.getLoadedCount());
            assertEquals(expectedLeading, actual.getLeadingNullCount());
            assertEquals(expectedTrailing, actual.getTrailingNullCount());

            for (int i = 0; i < actual.getLoadedCount(); i++) {
                assertSame(ITEMS.get(i + start), actual.get(i + start));
            }
        } else {
            assertEquals(count, actual.size());
            assertEquals(actual.size(), actual.getLoadedCount());
            assertEquals(0, actual.getLeadingNullCount());
            assertEquals(0, actual.getTrailingNullCount());

            for (int i = 0; i < actual.getLoadedCount(); i++) {
                assertSame(ITEMS.get(i + start), actual.get(i));
            }
        }
    }

    private void verifyCallback(PagedList.Callback callback, int countedPosition,
            int uncountedPosition) {
        if (mCounted) {
            verify(callback).onChanged(countedPosition, 20);
        } else {
            verify(callback).onInserted(uncountedPosition, 20);
        }
    }

    @Test
    public void initialLoad() {
        verifyRange(30, 40,
                new TestSource().loadInitial(50, 40, true));

        verifyRange(0, 10,
                new TestSource().loadInitial(5, 10, true));

        verifyRange(90, 10,
                new TestSource().loadInitial(95, 10, true));
    }


    private ContiguousPagedList<Item> createCountedPagedList(
            PagedList.Config config, int initialPosition) {
        TestSource source = new TestSource();
        return new ContiguousPagedList<>(
                source, mMainThread, mBackgroundThread,
                config,
                initialPosition);
    }

    private ContiguousPagedList<Item> createCountedPagedList(int initialPosition) {
        return createCountedPagedList(
                new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(40)
                        .setPageSize(20)
                        .setPrefetchDistance(20)
                        .build(),
                initialPosition);
    }

    @Test
    public void append() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(0);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(0, 40, pagedList);
        verifyZeroInteractions(callback);

        pagedList.loadAround(35);
        drain();

        verifyRange(0, 60, pagedList);
        verifyCallback(callback, 40, 40);
        verifyNoMoreInteractions(callback);
    }


    @Test
    public void prepend() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(80);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(60, 40, pagedList);
        verifyZeroInteractions(callback);

        pagedList.loadAround(mCounted ? 65 : 5);
        drain();

        verifyRange(40, 60, pagedList);
        verifyCallback(callback, 40, 0);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void outwards() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(50);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(30, 40, pagedList);
        verifyZeroInteractions(callback);

        pagedList.loadAround(mCounted ? 65 : 35);
        drain();

        verifyRange(30, 60, pagedList);
        verifyCallback(callback, 70, 40);
        verifyNoMoreInteractions(callback);

        pagedList.loadAround(mCounted ? 35 : 5);
        drain();

        verifyRange(10, 80, pagedList);
        verifyCallback(callback, 10, 0);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void multiAppend() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(0);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(0, 40, pagedList);
        verifyZeroInteractions(callback);

        pagedList.loadAround(55);
        drain();

        verifyRange(0, 80, pagedList);
        verifyCallback(callback, 40, 40);
        verifyCallback(callback, 60, 60);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void distantPrefetch() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(
                new PagedList.Config.Builder()
                        .setInitialLoadSizeHint(10)
                        .setPageSize(10)
                        .setPrefetchDistance(30)
                        .build(),
                0);
        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(null, callback);
        verifyRange(0, 10, pagedList);
        verifyZeroInteractions(callback);

        pagedList.loadAround(5);
        drain();

        verifyRange(0, 40, pagedList);

        pagedList.loadAround(6);
        drain();

        // although our prefetch window moves forward, no new load triggered
        verifyRange(0, 40, pagedList);
    }

    @Test
    public void appendCallbackAddedLate() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(0);
        verifyRange(0, 40, pagedList);

        pagedList.loadAround(35);
        drain();
        verifyRange(0, 60, pagedList);

        // snapshot at 60 items
        NullPaddedList<Item> snapshot = (NullPaddedList<Item>) pagedList.snapshot();
        verifyRange(0, 60, snapshot);


        pagedList.loadAround(55);
        drain();
        verifyRange(0, 80, pagedList);
        verifyRange(0, 60, snapshot);

        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(snapshot, callback);
        verifyCallback(callback, 60, 60);
        verifyNoMoreInteractions(callback);
    }


    @Test
    public void prependCallbackAddedLate() {
        ContiguousPagedList<Item> pagedList = createCountedPagedList(80);
        verifyRange(60, 40, pagedList);

        pagedList.loadAround(mCounted ? 65 : 5);
        drain();
        verifyRange(40, 60, pagedList);

        // snapshot at 60 items
        NullPaddedList<Item> snapshot = (NullPaddedList<Item>) pagedList.snapshot();
        verifyRange(40, 60, snapshot);


        pagedList.loadAround(mCounted ? 45 : 5);
        drain();
        verifyRange(20, 80, pagedList);
        verifyRange(40, 60, snapshot);

        PagedList.Callback callback = mock(PagedList.Callback.class);
        pagedList.addWeakCallback(snapshot, callback);
        verifyCallback(callback, 40, 0);
        verifyNoMoreInteractions(callback);
    }

    private void drain() {
        boolean executed;
        do {
            executed = mBackgroundThread.executeAll();
            executed |= mMainThread.executeAll();
        } while (executed);
    }
}
