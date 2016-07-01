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
 * CircularArray is a generic circular array data structure that provides O(1) random read, O(1)
 * prepend and O(1) append. The CircularArray automatically grows its capacity when number of added
 * items is over its capacity.
 */
public final class CircularArray<E> {
    private E[] mElements;
    private int mHead;
    private int mTail;
    private int mCapacityBitmask;

    private void doubleCapacity() {
        int n = mElements.length;
        int r = n - mHead;
        int newCapacity = n << 1;
        if (newCapacity < 0) {
            throw new RuntimeException("Max array capacity exceeded");
        }
        Object[] a = new Object[newCapacity];
        System.arraycopy(mElements, mHead, a, 0, r);
        System.arraycopy(mElements, 0, a, r, mHead);
        mElements = (E[]) a;
        mHead = 0;
        mTail = n;
        mCapacityBitmask = newCapacity - 1;
    }

    /**
     * Creates a circular array with default capacity.
     */
    public CircularArray() {
        this(8);
    }

    /**
     * Creates a circular array with capacity for at least {@code minCapacity}
     * elements.
     *
     * @param minCapacity the minimum capacity, between 1 and 2^30 inclusive
     */
    public CircularArray(int minCapacity) {
        if (minCapacity < 1) {
            throw new IllegalArgumentException("capacity must be >= 1");
        }
        if (minCapacity > (2 << 29)) {
            throw new IllegalArgumentException("capacity must be <= 2^30");
        }

        // If minCapacity isn't a power of 2, round up to the next highest
        // power of 2.
        final int arrayCapacity;
        if (Integer.bitCount(minCapacity) != 1) {
            arrayCapacity = Integer.highestOneBit(minCapacity - 1) << 1;
        } else {
            arrayCapacity = minCapacity;
        }

        mCapacityBitmask = arrayCapacity - 1;
        mElements = (E[]) new Object[arrayCapacity];
    }

    /**
     * Add an element in front of the CircularArray.
     * @param e  Element to add.
     */
    public void addFirst(E e) {
        mHead = (mHead - 1) & mCapacityBitmask;
        mElements[mHead] = e;
        if (mHead == mTail) {
            doubleCapacity();
        }
    }

    /**
     * Add an element at end of the CircularArray.
     * @param e  Element to add.
     */
    public void addLast(E e) {
        mElements[mTail] = e;
        mTail = (mTail + 1) & mCapacityBitmask;
        if (mTail == mHead) {
            doubleCapacity();
        }
    }

    /**
     * Remove first element from front of the CircularArray and return it.
     * @return  The element removed.
     * @throws ArrayIndexOutOfBoundsException if CircularArray is empty.
     */
    public E popFirst() {
        if (mHead == mTail) {
            throw new ArrayIndexOutOfBoundsException();
        }
        E result = mElements[mHead];
        mElements[mHead] = null;
        mHead = (mHead + 1) & mCapacityBitmask;
        return result;
    }

    /**
     * Remove last element from end of the CircularArray and return it.
     * @return  The element removed.
     * @throws ArrayIndexOutOfBoundsException if CircularArray is empty.
     */
    public E popLast() {
        if (mHead == mTail) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int t = (mTail - 1) & mCapacityBitmask;
        E result = mElements[t];
        mElements[t] = null;
        mTail = t;
        return result;
    }

    /**
     * Remove all elements from the CircularArray.
     */
    public void clear() {
        removeFromStart(size());
    }

    /**
     * Remove multiple elements from front of the CircularArray, ignore when numOfElements
     * is less than or equals to 0.
     * @param numOfElements  Number of elements to remove.
     * @throws ArrayIndexOutOfBoundsException if numOfElements is larger than
     *         {@link #size()}
     */
    public void removeFromStart(int numOfElements) {
        if (numOfElements <= 0) {
            return;
        }
        if (numOfElements > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int end = mElements.length;
        if (numOfElements < end - mHead) {
            end = mHead + numOfElements;
        }
        for (int i = mHead; i < end; i++) {
            mElements[i] = null;
        }
        int removed = (end - mHead);
        numOfElements -= removed;
        mHead = (mHead + removed) & mCapacityBitmask;
        if (numOfElements > 0) {
            // mHead wrapped to 0
            for (int i = 0; i < numOfElements; i++) {
                mElements[i] = null;
            }
            mHead = numOfElements;
        }
    }

    /**
     * Remove multiple elements from end of the CircularArray, ignore when numOfElements
     * is less than or equals to 0.
     * @param numOfElements  Number of elements to remove.
     * @throws ArrayIndexOutOfBoundsException if numOfElements is larger than
     *         {@link #size()}
     */
    public void removeFromEnd(int numOfElements) {
        if (numOfElements <= 0) {
            return;
        }
        if (numOfElements > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int start = 0;
        if (numOfElements < mTail) {
            start = mTail - numOfElements;
        }
        for (int i = start; i < mTail; i++) {
            mElements[i] = null;
        }
        int removed = (mTail - start);
        numOfElements -= removed;
        mTail = mTail - removed;
        if (numOfElements > 0) {
            // mTail wrapped to mElements.length
            mTail = mElements.length;
            int newTail = mTail - numOfElements;
            for (int i = newTail; i < mTail; i++) {
                mElements[i] = null;
            }
            mTail = newTail;
        }
    }

    /**
     * Get first element of the CircularArray.
     * @return The first element.
     * @throws {@link ArrayIndexOutOfBoundsException} if CircularArray is empty.
     */
    public E getFirst() {
        if (mHead == mTail) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return mElements[mHead];
    }

    /**
     * Get last element of the CircularArray.
     * @return The last element.
     * @throws {@link ArrayIndexOutOfBoundsException} if CircularArray is empty.
     */
    public E getLast() {
        if (mHead == mTail) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return mElements[(mTail - 1) & mCapacityBitmask];
    }

    /**
     * Get nth (0 <= n <= size()-1) element of the CircularArray.
     * @param n  The zero based element index in the CircularArray.
     * @return The nth element.
     * @throws {@link ArrayIndexOutOfBoundsException} if n < 0 or n >= size().
     */
    public E get(int n) {
        if (n < 0 || n >= size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return mElements[(mHead + n) & mCapacityBitmask];
    }

    /**
     * Get number of elements in the CircularArray.
     * @return Number of elements in the CircularArray.
     */
    public int size() {
        return (mTail - mHead) & mCapacityBitmask;
    }

    /**
     * Return true if size() is 0.
     * @return true if size() is 0.
     */
    public boolean isEmpty() {
        return mHead == mTail;
    }

}
