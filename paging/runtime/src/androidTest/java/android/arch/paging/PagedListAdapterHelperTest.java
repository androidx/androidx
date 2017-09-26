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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.v7.recyclerview.extensions.DiffCallback;
import android.support.v7.recyclerview.extensions.ListAdapterConfig;
import android.support.v7.util.ListUpdateCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

@SmallTest
@RunWith(JUnit4.class)
public class PagedListAdapterHelperTest {
    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mDiffThread = new TestExecutor();
    private TestExecutor mPageLoadingThread = new TestExecutor();

    private static final ArrayList<String> ALPHABET_LIST = new ArrayList<>();
    static {
        for (int i = 0; i < 26; i++) {
            ALPHABET_LIST.add("" + 'a' + i);
        }
    }

    private static final DiffCallback<String> STRING_DIFF_CALLBACK = new DiffCallback<String>() {
        @Override
        public boolean areItemsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }

        @Override
        public boolean areContentsTheSame(@NonNull String oldItem, @NonNull String newItem) {
            return oldItem.equals(newItem);
        }
    };

    private static final ListUpdateCallback IGNORE_CALLBACK = new ListUpdateCallback() {
        @Override
        public void onInserted(int position, int count) {
        }

        @Override
        public void onRemoved(int position, int count) {
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
        }

        @Override
        public void onChanged(int position, int count, Object payload) {
        }
    };


    private <T> PagedListAdapterHelper<T> createHelper(
            ListUpdateCallback listUpdateCallback, DiffCallback<T> diffCallback) {
        return new PagedListAdapterHelper<T>(listUpdateCallback,
                new ListAdapterConfig.Builder<T>()
                        .setDiffCallback(diffCallback)
                        .setMainThreadExecutor(mMainThread)
                        .setBackgroundThreadExecutor(mDiffThread)
                        .build());
    }

    private <V> PagedList<V> createPagedListFromListAndPos(
            PagedList.Config config, List<V> data, int initialKey) {
        return new PagedList.Builder<Integer, V>()
                .setInitialKey(initialKey)
                .setConfig(config)
                .setMainThreadExecutor(mMainThread)
                .setBackgroundThreadExecutor(mPageLoadingThread)
                .setDataSource(new ListDataSource<>(data))
                .build();
    }

    @Test
    public void initialState() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);
        assertEquals(null, helper.getCurrentList());
        assertEquals(0, helper.getItemCount());
        verifyZeroInteractions(callback);
    }

    @Test
    public void setFullList() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);
        helper.setList(new StringPagedList(0, 0, "a", "b"));

        assertEquals(2, helper.getItemCount());
        assertEquals("a", helper.getItem(0));
        assertEquals("b", helper.getItem(1));

        verify(callback).onInserted(0, 2);
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getEmpty() {
        PagedListAdapterHelper<String> helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK);
        helper.getItem(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNegative() {
        PagedListAdapterHelper<String> helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK);
        helper.setList(new StringPagedList(0, 0, "a", "b"));
        helper.getItem(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getPastEnd() {
        PagedListAdapterHelper<String> helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK);
        helper.setList(new StringPagedList(0, 0, "a", "b"));
        helper.getItem(2);
    }

    @Test
    public void simpleStatic() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);

        assertEquals(0, helper.getItemCount());

        helper.setList(new StringPagedList(2, 2, "a", "b"));

        verify(callback).onInserted(0, 6);
        verifyNoMoreInteractions(callback);
        assertEquals(6, helper.getItemCount());

        assertNull(helper.getItem(0));
        assertNull(helper.getItem(1));
        assertEquals("a", helper.getItem(2));
        assertEquals("b", helper.getItem(3));
        assertNull(helper.getItem(4));
        assertNull(helper.getItem(5));
    }

    @Test
    public void pagingInContent() {
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(4)
                .setPageSize(2)
                .setPrefetchDistance(2)
                .build();

        final ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);

        helper.setList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2));
        verify(callback).onInserted(0, ALPHABET_LIST.size());
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);

        // get without triggering prefetch...
        helper.getItem(1);
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);

        // get triggering prefetch...
        helper.getItem(2);
        verifyNoMoreInteractions(callback);
        drain();
        verify(callback).onChanged(4, 2, null);
        verifyNoMoreInteractions(callback);

        // get with no data loaded nearby...
        helper.getItem(12);
        verifyNoMoreInteractions(callback);
        drain();
        verify(callback).onChanged(10, 2, null);
        verify(callback).onChanged(12, 2, null);
        verify(callback).onChanged(14, 2, null);
        verifyNoMoreInteractions(callback);

        // finally, clear
        helper.setList(null);
        verify(callback).onRemoved(0, 26);
        drain();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void simpleSwap() {
        // Page size large enough to load
        PagedList.Config config = new PagedList.Config.Builder()
                .setPageSize(50)
                .build();

        final ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);

        // initial list missing one item (immediate)
        helper.setList(createPagedListFromListAndPos(config, ALPHABET_LIST.subList(0, 25), 0));
        verify(callback).onInserted(0, 25);
        verifyNoMoreInteractions(callback);
        assertEquals(helper.getItemCount(), 25);
        drain();
        verifyNoMoreInteractions(callback);

        // pass second list with full data
        helper.setList(createPagedListFromListAndPos(config, ALPHABET_LIST, 0));
        verifyNoMoreInteractions(callback);
        drain();
        verify(callback).onInserted(25, 1);
        verifyNoMoreInteractions(callback);
        assertEquals(helper.getItemCount(), 26);

        // finally, clear (immediate)
        helper.setList(null);
        verify(callback).onRemoved(0, 26);
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void newPageWhileDiffing() {
        PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(4)
                .setPageSize(2)
                .setPrefetchDistance(2)
                .build();

        final ListUpdateCallback callback = mock(ListUpdateCallback.class);
        PagedListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);

        helper.setList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2));
        verify(callback).onInserted(0, ALPHABET_LIST.size());
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);
        assertNotNull(helper.getCurrentList());
        assertFalse(helper.getCurrentList().isImmutable());

        // trigger page loading
        helper.getItem(10);
        helper.setList(createPagedListFromListAndPos(config, ALPHABET_LIST, 2));
        verifyNoMoreInteractions(callback);

        // drain page fetching, but list became immutable, page will be ignored
        drainExceptDiffThread();
        verifyNoMoreInteractions(callback);
        assertNotNull(helper.getCurrentList());
        assertTrue(helper.getCurrentList().isImmutable());

        // finally full drain, which signals nothing, since 1st pagedlist == 2nd pagedlist
        drain();
        verifyNoMoreInteractions(callback);
        assertNotNull(helper.getCurrentList());
        assertFalse(helper.getCurrentList().isImmutable());
    }

    private void drainExceptDiffThread() {
        boolean executed;
        do {
            executed = mPageLoadingThread.executeAll();
            executed |= mMainThread.executeAll();
        } while (executed);
    }

    private void drain() {
        boolean executed;
        do {
            executed = mPageLoadingThread.executeAll();
            executed |= mDiffThread.executeAll();
            executed |= mMainThread.executeAll();
        } while (executed);
    }
}
