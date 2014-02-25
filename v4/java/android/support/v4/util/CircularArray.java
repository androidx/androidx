/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v4.util;

/**
 * A circular array implementation that provides O(1) random read and O(1)
 * prepend and O(1) append.
 */
public class CircularArray<E>
{
    private E[] mElements;
    private int mHead;
    private int mTail;
    private int mCapacityBitmask;

    private void doubleCapacity() {
        int n = mElements.length;
        int r = n - mHead;
        int newCapacity = n << 1;
        if (newCapacity < 0) {
            throw new RuntimeException("Too big");
        }
        Object[] a = new Object[newCapacity];
        System.arraycopy(mElements, mHead, a, 0, r);
        System.arraycopy(mElements, 0, a, r, mHead);
        mElements = (E[])a;
        mHead = 0;
        mTail = n;
        mCapacityBitmask = newCapacity - 1;
    }

    /**
     * Create a CircularArray with default capacity.
     */
    public CircularArray() {
        this(8);
    }

    /**
     * Create a CircularArray with capacity for at least minCapacity elements.
     *
     * @param minCapacity The minimum capacity required for the circular array.
     */
    public CircularArray(int minCapacity) {
        if (minCapacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        int arrayCapacity = minCapacity;
        // If minCapacity isn't a power of 2, round up to the next highest power
        // of 2.
        if (Integer.bitCount(minCapacity) != 1) {
            arrayCapacity = 1 << (Integer.highestOneBit(minCapacity) + 1);
        }
        mCapacityBitmask = arrayCapacity - 1;
        mElements = (E[]) new Object[arrayCapacity];
    }

    public final void addFirst(E e) {
        mHead = (mHead - 1) & mCapacityBitmask;
        mElements[mHead] = e;
        if (mHead == mTail) {
            doubleCapacity();
        }
    }

    public final void addLast(E e) {
        mElements[mTail] = e;
        mTail = (mTail + 1) & mCapacityBitmask;
        if (mTail == mHead) {
            doubleCapacity();
        }
    }

    public final E popFirst() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        E result = mElements[mHead];
        mElements[mHead] = null;
        mHead = (mHead + 1) & mCapacityBitmask;
        return result;
    }

    public final E popLast() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        int t = (mTail - 1) & mCapacityBitmask;
        E result = mElements[t];
        mElements[t] = null;
        mTail = t;
        return result;
    }

    public final E getFirst() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        return mElements[mHead];
    }

    public final E getLast() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        return mElements[(mTail - 1) & mCapacityBitmask];
    }

    public final E get(int i) {
        if (i < 0 || i >= size()) throw new ArrayIndexOutOfBoundsException();
        int p = (mHead + i) & mCapacityBitmask;
        return mElements[p];
    }

    public final int size() {
        return (mTail - mHead) & mCapacityBitmask;
    }

    public final boolean isEmpty() {
        return mHead == mTail;
    }

}
