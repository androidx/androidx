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

package android.arch.util.paging;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.v7.util.ListUpdateCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;

@SmallTest
@RunWith(JUnit4.class)
public class LazyListAdapterHelperTest {
    public class TestExecutor implements Executor {
        private Queue<Runnable> mTasks = new LinkedList<>();
        @Override
        public void execute(@NonNull Runnable command) {
            mTasks.add(command);
        }

        boolean executeAll() {
            boolean consumed = !mTasks.isEmpty();
            Runnable task;
            while ((task = mTasks.poll()) != null) {
                task.run();
            }
            return consumed;
        }
    }

    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mBackgroundThread = new TestExecutor();
    private CountedDataSource<Item> mDataSource = new CountedDataSource<Item>() {
        @Override
        public int loadCount() {
            return mItemData.size();
        }

        private List<Item> getClampedRange(int start, int end) {
            start = Math.max(0, start);
            end = Math.min(loadCount(), end);
            return mItemData.subList(start, end);
        }

        @Override
        public List<Item> loadAfterInitial(int position, int pageSize) {
            return getClampedRange(position + 1, position + pageSize + 1);
        }

        @Override
        public List<Item> loadAfter(int currentEndIndex, @NonNull Item currentEndItem,
                int pageSize) {
            return getClampedRange(currentEndIndex + 1, currentEndIndex + 1 + pageSize);
        }

        @Override
        public List<Item> loadBefore(int currentBeginIndex, @NonNull Item currentBeginItem,
                int pageSize) {
            return getClampedRange(currentBeginIndex - 1 - pageSize, currentBeginIndex);
        }
    };

    private static class Item {
        int mId = 0;
        int mGeneration = 0;

        static final DiffCallback<Item> DIFF_CALLBACK = new DiffCallback<Item>() {
            @Override
            public boolean areContentsTheSame(@NonNull Item oldItem,
                    @NonNull Item newItem) {
                return oldItem.mId == newItem.mId;
            }

            @Override
            public boolean areItemsTheSame(@NonNull Item oldItem,
                    @NonNull Item newItem) {
                return oldItem.equals(newItem);
            }
        };
    }

    private List<Item> mItemData = new ArrayList<>();

    @Before
    public void setup() {
        for (int i = 0; i < 100; i++) {
            Item item = new Item();
            item.mId = i;
            item.mGeneration = 0;
            mItemData.add(item);
        }
    }

    private LazyList<Item> createLazyList() {
        return new LazyList<>(mDataSource, mMainThread, mBackgroundThread,
                ListConfig.builder().pageSize(10).prefetchDistance(10).create());
    }

    private LazyListAdapterHelper<Item> createHelper(ListUpdateCallback callback) {
        return LazyListAdapterHelper.<Item>builder()
                .mainThreadExecutor(mMainThread)
                .backgroundThreadExecutor(mBackgroundThread)
                .updateCallback(callback)
                .diffCallback(Item.DIFF_CALLBACK).create();
    }

    @Test
    public void create() {
        ListUpdateCallback callback = Mockito.mock(ListUpdateCallback.class);
        createHelper(callback);
        verifyZeroInteractions(callback);
    }

    @Test
    public void setEmpty() {
        ListUpdateCallback callback = Mockito.mock(ListUpdateCallback.class);
        LazyListAdapterHelper<Item> helper = createHelper(callback);
        verifyZeroInteractions(callback);

        LazyList<Item> list = createLazyList();
        helper.setLazyList(list);
        drain();
        verify(callback).onInserted(0, 100);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void setEmptyAndLoad() {
        ListUpdateCallback callback = Mockito.mock(ListUpdateCallback.class);
        LazyListAdapterHelper<Item> helper = createHelper(callback);
        verifyZeroInteractions(callback);

        LazyList<Item> list = createLazyList();
        helper.setLazyList(list);
        drain();
        verify(callback).onInserted(0, 100);
        verifyNoMoreInteractions(callback);

        helper.get(0);
        verifyNoMoreInteractions(callback);
        drain();
        verify(callback).onChanged(0, 10, null);
        verify(callback).onChanged(10, 10, null);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void setEmptyAndClear() {
        ListUpdateCallback callback = Mockito.mock(ListUpdateCallback.class);
        LazyListAdapterHelper<Item> helper = createHelper(callback);
        verifyZeroInteractions(callback);

        LazyList<Item> list = createLazyList();
        helper.setLazyList(list);
        drain();
        verify(callback).onInserted(0, 100);
        verifyNoMoreInteractions(callback);

        helper.setLazyList(null);
        verify(callback).onRemoved(0, 100);
        drain();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void setNonEmpty() {
        ListUpdateCallback callback = Mockito.mock(ListUpdateCallback.class);
        LazyListAdapterHelper<Item> helper = createHelper(callback);

        LazyList<Item> list = createLazyList();
        list.get(0);
        drain();

        verifyZeroInteractions(callback);

        // setting doesn't trigger callbacks
        helper.setLazyList(list);
        drain();
        verify(callback).onInserted(0, 100);
        verifyNoMoreInteractions(callback);

        // helper has correct data, but will trigger loads on access
        assertNotNull(helper.get(19));
        assertNull(helper.get(20));
        verifyNoMoreInteractions(callback);
        drain();
        verify(callback).onChanged(20, 10, null);
        verify(callback).onChanged(30, 10, null);
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void simpleDiffCalc() {
        ListUpdateCallback callback = Mockito.mock(ListUpdateCallback.class);
        LazyListAdapterHelper<Item> helper = createHelper(callback);
        verifyZeroInteractions(callback);

        LazyList<Item> list = createLazyList();
        helper.setLazyList(list);
        drain();
        verify(callback).onInserted(0, 100);
        verifyNoMoreInteractions(callback);

        list = createLazyList();
        list.get(0); // TODO: also test more interesting number
        drain();
        verifyNoMoreInteractions(callback);

        helper.setLazyList(list);
        verifyNoMoreInteractions(callback);
        drain();

        // NOTE: these aren't ideal, but they're what we currently produce.
        // Should ideally be onChanged(0, 20)
        verify(callback).onRemoved(80, 20);
        verify(callback).onInserted(0, 20);
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
