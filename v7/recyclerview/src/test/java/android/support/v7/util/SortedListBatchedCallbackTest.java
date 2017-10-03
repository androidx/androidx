/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v7.util;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
@SmallTest
public class SortedListBatchedCallbackTest {
    SortedList.BatchedCallback mBatchedCallback;
    SortedList.Callback mMockCallback;
    @Before
    public void init() {
        mMockCallback = Mockito.mock(SortedList.Callback.class);
        mBatchedCallback = new SortedList.BatchedCallback(mMockCallback);
    }

    @Test
    public void onChange() {
        mBatchedCallback.onChanged(1, 2);
        verifyZeroInteractions(mMockCallback);
        mBatchedCallback.dispatchLastEvent();
        verify(mMockCallback).onChanged(1, 2, null);
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void onRemoved() {
        mBatchedCallback.onRemoved(2, 3);
        verifyZeroInteractions(mMockCallback);
        mBatchedCallback.dispatchLastEvent();
        verify(mMockCallback).onRemoved(2, 3);
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void onInserted() {
        mBatchedCallback.onInserted(3, 4);
        verifyNoMoreInteractions(mMockCallback);
        mBatchedCallback.dispatchLastEvent();
        verify(mMockCallback).onInserted(3, 4);
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void onMoved() {
        mBatchedCallback.onMoved(5, 6);
        // moves are not merged
        verify(mMockCallback).onMoved(5, 6);
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void compare() {
        Object o1 = new Object();
        Object o2 = new Object();
        mBatchedCallback.compare(o1, o2);
        verify(mMockCallback).compare(o1, o2);
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void areContentsTheSame() {
        Object o1 = new Object();
        Object o2 = new Object();
        mBatchedCallback.areContentsTheSame(o1, o2);
        verify(mMockCallback).areContentsTheSame(o1, o2);
        verifyNoMoreInteractions(mMockCallback);
    }

    @Test
    public void areItemsTheSame() {
        Object o1 = new Object();
        Object o2 = new Object();
        mBatchedCallback.areItemsTheSame(o1, o2);
        verify(mMockCallback).areItemsTheSame(o1, o2);
        verifyNoMoreInteractions(mMockCallback);
    }
}
