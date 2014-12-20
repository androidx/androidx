/*
 * Copyright (C) 2014 The Android Open Source Project
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

import junit.framework.TestCase;

import static android.support.v7.util.SortedList.BatchedCallback.TYPE_NONE;
import static android.support.v7.util.SortedList.BatchedCallback.TYPE_ADD;
import static android.support.v7.util.SortedList.BatchedCallback.TYPE_REMOVE;
import static android.support.v7.util.SortedList.BatchedCallback.TYPE_CHANGE;
import static android.support.v7.util.SortedList.BatchedCallback.TYPE_MOVE;

public class SortedListAdapterCallbackWrapperTest extends TestCase {

    private int lastReceivedType = TYPE_NONE;
    private int lastReceivedPosition = -1;
    private int lastReceivedCount = -1;

    private SortedList.Callback<Object> mCallback = new SortedList.Callback<Object>() {
        @Override
        public int compare(Object o1, Object o2) {
            return 0;
        }

        @Override
        public void onInserted(int position, int count) {
            lastReceivedType = TYPE_ADD;
            lastReceivedPosition = position;
            lastReceivedCount = count;
        }

        @Override
        public void onRemoved(int position, int count) {
            lastReceivedType = TYPE_REMOVE;
            lastReceivedPosition = position;
            lastReceivedCount = count;
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            lastReceivedType = TYPE_MOVE;
            lastReceivedPosition = fromPosition;
            lastReceivedCount = toPosition;
        }

        @Override
        public void onChanged(int position, int count) {
            lastReceivedType = TYPE_CHANGE;
            lastReceivedPosition = position;
            lastReceivedCount = count;
        }

        @Override
        public boolean areContentsTheSame(Object oldItem, Object newItem) {
            return false;
        }

        @Override
        public boolean areItemsTheSame(Object item1, Object item2) {
            return false;
        }
    };

    private SortedList.BatchedCallback<Object> mBatched =
            new SortedList.BatchedCallback<Object>(mCallback);

    public void testAdd() throws Throwable {
        mBatched.onInserted(0, 3);
        assertPending(TYPE_ADD, 0, 3);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 0, 3);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testRemove() throws Throwable {
        mBatched.onRemoved(0, 3);
        assertPending(TYPE_REMOVE, 0, 3);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_REMOVE, 0, 3);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testChange() throws Throwable {
        mBatched.onChanged(0, 3);
        assertPending(TYPE_CHANGE, 0, 3);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_CHANGE, 0, 3);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testMove() throws Throwable {
        mBatched.onMoved(0, 3);
        assertLast(TYPE_MOVE, 0, 3);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAdd1() throws Throwable {
        mBatched.onInserted(3, 5);
        mBatched.onInserted(3, 2);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 3, 7);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAdd2() throws Throwable {
        mBatched.onInserted(3, 5);
        mBatched.onInserted(1, 2);
        assertLast(TYPE_ADD, 3, 5);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 1, 2);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAdd3() throws Throwable {
        mBatched.onInserted(3, 5);
        mBatched.onInserted(8, 2);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 3, 7);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAdd4() throws Throwable {
        mBatched.onInserted(3, 5);
        mBatched.onInserted(9, 2);
        assertLast(TYPE_ADD, 3, 5);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 9, 2);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAdd5() throws Throwable {
        mBatched.onInserted(3, 5);
        mBatched.onInserted(4, 1);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 3, 6);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAdd6() throws Throwable {
        mBatched.onInserted(3, 5);
        mBatched.onInserted(4, 1);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.onInserted(4, 1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 3, 7);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchAddLoop() throws Throwable {
        for (int i = 0; i < 10; i ++) {
            mBatched.onInserted(4 + i, 1);
            assertLast(TYPE_NONE, -1, -1);
            assertPending(TYPE_ADD, 4, i + 1);
        }
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 4, 10);
    }

    public void testBatchAddReverseLoop() throws Throwable {
        for (int i = 10; i >= 0; i --) {
            mBatched.onInserted(4, 1);
            assertLast(TYPE_NONE, -1, -1);
            assertPending(TYPE_ADD, 4, 10 - i + 1);
        }
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 4, 11);
    }

    public void testBadBatchAddReverseLoop() throws Throwable {
        for (int i = 10; i >= 0; i --) {
            mBatched.onInserted(4 + i, 1);
            if (i < 10) {
                assertLast(TYPE_ADD, 4 + i + 1, 1);
            }

        }
        mBatched.dispatchLastEvent();
        assertLast(TYPE_ADD, 4, 1);
    }

    public void testBatchRemove1() throws Throwable {
        mBatched.onRemoved(3, 5);
        mBatched.onRemoved(3, 1);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_REMOVE, 3, 6);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchRemove2() throws Throwable {
        mBatched.onRemoved(3, 5);
        mBatched.onRemoved(4, 1);
        assertLast(TYPE_REMOVE, 3, 5);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_REMOVE, 4, 1);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchRemove3() throws Throwable {
        mBatched.onRemoved(3, 5);
        mBatched.onRemoved(2, 3);
        assertLast(TYPE_REMOVE, 3, 5);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_REMOVE, 2, 3);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchChange1() throws Throwable {
        mBatched.onChanged(3, 5);
        mBatched.onChanged(3, 1);
        assertPending(TYPE_CHANGE, 3, 5);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_CHANGE, 3, 5);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchChange2() throws Throwable {
        mBatched.onChanged(3, 5);
        mBatched.onChanged(2, 7);
        assertPending(TYPE_CHANGE, 2, 7);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.dispatchLastEvent();
        assertLast(TYPE_CHANGE, 2, 7);
        assertPending(TYPE_NONE, -1, -1);
    }

    public void testBatchChange3() throws Throwable {
        mBatched.onChanged(3, 5);
        mBatched.onChanged(2, 1);
        assertLast(TYPE_NONE, -1, -1);
        mBatched.onChanged(8, 2);
        assertLast(TYPE_NONE, -1, -1);
        assertPending(TYPE_CHANGE, 2, 8);
    }

    public void testBatchChange4() throws Throwable {
        mBatched.onChanged(3, 5);
        mBatched.onChanged(1, 1);
        assertLast(TYPE_CHANGE, 3, 5);
        assertPending(TYPE_CHANGE, 1, 1);
    }

    public void testBatchChange5() throws Throwable {
        mBatched.onChanged(3, 5);
        mBatched.onChanged(9, 1);
        assertLast(TYPE_CHANGE, 3, 5);
        assertPending(TYPE_CHANGE, 9, 1);
    }

    private void assertLast(int type, int position, int count) throws Throwable {
        try {
            assertEquals(lastReceivedType, type);
            if (position >= 0) {
                assertEquals(lastReceivedPosition, position);
            }
            if (count >= 0) {
                assertEquals(lastReceivedCount, count);
            }
        } catch (Throwable t) {
            throw new Throwable("last event: expected=" + log(type, position, count)
                    + " found=" + log(lastReceivedType, lastReceivedPosition,
                    lastReceivedCount), t);
        }
    }
    private void assertPending(int type, int position, int count) throws Throwable {
        try {
            assertEquals(mBatched.mLastEventType, type);
            if (position >= 0) {
                assertEquals(mBatched.mLastEventPosition, position);
            }
            if (count >= 0) {
                assertEquals(mBatched.mLastEventCount, count);
            }
        } catch (Throwable t) {
            throw new Throwable("pending event: expected=" + log(type, position, count)
                    + " found=" + log(mBatched.mLastEventType, mBatched.mLastEventPosition,
                    mBatched.mLastEventCount), t);
        }
    }

    private String log(int type, int position, int count) {
        return TYPES_NAMES[type]
                + ", p:" + position
                + ", c:" + count;
    }

    private static final String[] TYPES_NAMES = new String[]{"none", "add", "remove", "change",
            "move"};
}
