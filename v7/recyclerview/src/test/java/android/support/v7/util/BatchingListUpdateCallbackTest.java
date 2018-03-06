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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@SmallTest
public class BatchingListUpdateCallbackTest {
    BatchingListUpdateCallback mBatching;
    ListUpdateCallback mCallback;

    @Before
    public void setup() {
        mCallback = mock(ListUpdateCallback.class);
        mBatching = new BatchingListUpdateCallback(mCallback);
    }

    @Test
    public void addSimple() {
        mBatching.onInserted(3, 2);
        mBatching.dispatchLastEvent();
        verify(mCallback).onInserted(3, 2);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void addToSamePos() {
        mBatching.onInserted(3, 2);
        mBatching.onInserted(3, 1);
        mBatching.dispatchLastEvent();
        verify(mCallback).onInserted(3, 3);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void addInsidePrevious() {
        mBatching.onInserted(3, 5);
        mBatching.onInserted(5, 1);
        mBatching.dispatchLastEvent();
        verify(mCallback).onInserted(3, 6);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void addBefore() {
        mBatching.onInserted(3, 5);
        mBatching.onInserted(2, 1);
        mBatching.dispatchLastEvent();
        verify(mCallback).onInserted(3, 5);
        verify(mCallback).onInserted(2, 1);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void removeSimple() {
        mBatching.onRemoved(3, 2);
        mBatching.dispatchLastEvent();
        verify(mCallback).onRemoved(3, 2);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void removeSamePosition() {
        mBatching.onRemoved(3, 2);
        mBatching.onRemoved(3, 1);
        mBatching.dispatchLastEvent();
        verify(mCallback).onRemoved(3, 3);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void removeInside() {
        mBatching.onRemoved(3, 5);
        mBatching.onRemoved(4, 2);
        mBatching.dispatchLastEvent();
        verify(mCallback).onRemoved(3, 5);
        verify(mCallback).onRemoved(4, 2);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void removeBefore() {
        mBatching.onRemoved(3, 2);
        mBatching.onRemoved(2, 1);
        mBatching.dispatchLastEvent();
        verify(mCallback).onRemoved(2, 3);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void removeBefore2() {
        mBatching.onRemoved(3, 2);
        mBatching.onRemoved(2, 4);
        mBatching.dispatchLastEvent();
        verify(mCallback).onRemoved(2, 6);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void removeBefore3() {
        mBatching.onRemoved(3, 2);
        mBatching.onRemoved(1, 1);
        mBatching.dispatchLastEvent();
        verify(mCallback).onRemoved(3, 2);
        verify(mCallback).onRemoved(1, 1);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void moveSimple() {
        mBatching.onMoved(3, 2);
        mBatching.dispatchLastEvent();
        verify(mCallback).onMoved(3, 2);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void moveTwice() {
        mBatching.onMoved(3, 2);
        mBatching.onMoved(5, 6);
        mBatching.dispatchLastEvent();
        verify(mCallback).onMoved(3, 2);
        verify(mCallback).onMoved(5, 6);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeSimple() {
        mBatching.onChanged(3, 2, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeConsecutive() {
        mBatching.onChanged(3, 2, null);
        mBatching.onChanged(5, 2, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 4, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeTheSame() {
        mBatching.onChanged(3, 2, null);
        mBatching.onChanged(4, 2, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 3, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeTheSame2() {
        mBatching.onChanged(3, 2, null);
        mBatching.onChanged(3, 2, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeBefore() {
        mBatching.onChanged(3, 2, null);
        mBatching.onChanged(2, 1, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(2, 3, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeBeforeOverlap() {
        mBatching.onChanged(3, 2, null);
        mBatching.onChanged(2, 2, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(2, 3, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeSimpleWithPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, payload);
    }

    @Test
    public void changeConsecutiveWithPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.onChanged(5, 2, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 4, payload);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeTheSameWithPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.onChanged(4, 2, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 3, payload);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeTheSame2WithPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.onChanged(3, 2, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, payload);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeBeforeWithPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.onChanged(2, 1, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(2, 3, payload);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeBeforeOverlapWithPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.onChanged(2, 2, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(2, 3, payload);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeWithNewPayload() {
        Object payload1 = new Object();
        Object payload2 = new Object();
        mBatching.onChanged(3, 2, payload1);
        mBatching.onChanged(2, 2, payload2);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, payload1);
        verify(mCallback).onChanged(2, 2, payload2);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeWithEmptyPayload() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, payload);
        mBatching.onChanged(2, 2, null);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, payload);
        verify(mCallback).onChanged(2, 2, null);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    public void changeWithEmptyPayload2() {
        Object payload = new Object();
        mBatching.onChanged(3, 2, null);
        mBatching.onChanged(2, 2, payload);
        mBatching.dispatchLastEvent();
        verify(mCallback).onChanged(3, 2, null);
        verify(mCallback).onChanged(2, 2, payload);
        verifyNoMoreInteractions(mCallback);
    }
}
