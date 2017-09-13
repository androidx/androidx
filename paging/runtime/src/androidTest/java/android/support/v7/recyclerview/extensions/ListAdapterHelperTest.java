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

package android.support.v7.recyclerview.extensions;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.arch.paging.TestExecutor;
import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.v7.util.ListUpdateCallback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@SmallTest
@RunWith(JUnit4.class)
public class ListAdapterHelperTest {
    private TestExecutor mMainThread = new TestExecutor();
    private TestExecutor mBackgroundThread = new TestExecutor();


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


    private <T> ListAdapterHelper<T> createHelper(
            ListUpdateCallback listUpdateCallback, DiffCallback<T> diffCallback) {
        return new ListAdapterHelper<T>(listUpdateCallback,
                new ListAdapterConfig.Builder<T>()
                        .setDiffCallback(diffCallback)
                        .setMainThreadExecutor(mMainThread)
                        .setBackgroundThreadExecutor(mBackgroundThread)
                        .build());
    }

    @Test
    public void initialState() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        ListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);
        assertEquals(0, helper.getItemCount());
        verifyZeroInteractions(callback);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getEmpty() {
        ListAdapterHelper<String> helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK);
        helper.getItem(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getNegative() {
        ListAdapterHelper<String> helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK);
        helper.setList(Arrays.asList("a", "b"));
        helper.getItem(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getPastEnd() {
        ListAdapterHelper<String> helper = createHelper(IGNORE_CALLBACK, STRING_DIFF_CALLBACK);
        helper.setList(Arrays.asList("a", "b"));
        helper.getItem(2);
    }

    @Test
    public void setListSimple() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        ListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);

        helper.setList(Arrays.asList("a", "b"));

        assertEquals(2, helper.getItemCount());
        assertEquals("a", helper.getItem(0));
        assertEquals("b", helper.getItem(1));

        verify(callback).onInserted(0, 2);
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);
    }

    @Test
    public void setListUpdate() {
        ListUpdateCallback callback = mock(ListUpdateCallback.class);
        ListAdapterHelper<String> helper = createHelper(callback, STRING_DIFF_CALLBACK);

        // initial list (immediate)
        helper.setList(Arrays.asList("a", "b"));
        verify(callback).onInserted(0, 2);
        verifyNoMoreInteractions(callback);
        drain();
        verifyNoMoreInteractions(callback);

        // update (deferred)
        helper.setList(Arrays.asList("a", "b", "c"));
        verifyNoMoreInteractions(callback);
        drain();
        verify(callback).onInserted(2, 1);
        verifyNoMoreInteractions(callback);

        // clear (immediate)
        helper.setList(null);
        verify(callback).onRemoved(0, 3);
        verifyNoMoreInteractions(callback);
        drain();
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
