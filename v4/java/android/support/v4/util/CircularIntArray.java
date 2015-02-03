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
 * CircularIntArray is a circular integer array data structure that provides O(1) random read, O(1)
 * prepend and O(1) append. The CircularIntArray automatically grows its capacity when number of
 * added integers is over its capacity.
 */
public final class CircularIntArray
{
    private int[] mElements;
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
        int[] a = new int[newCapacity];
        System.arraycopy(mElements, mHead, a, 0, r);
        System.arraycopy(mElements, 0, a, r, mHead);
        mElements = a;
        mHead = 0;
        mTail = n;
        mCapacityBitmask = newCapacity - 1;
    }

    /**
     * Create a CircularIntArray with default capacity.
     */
    public CircularIntArray() {
        this(8);
    }

    /**
     * Create a CircularIntArray with capacity for at least minCapacity elements.
     *
     * @param minCapacity The minimum capacity required for the CircularIntArray.
     */
    public CircularIntArray(int minCapacity) {
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
        mElements = new int[arrayCapacity];
    }

    /**
     * Add an integer in front of the CircularIntArray.
     * @param e  Integer to add.
     */
    public void addFirst(int e) {
        mHead = (mHead - 1) & mCapacityBitmask;
        mElements[mHead] = e;
        if (mHead == mTail) {
            doubleCapacity();
        }
    }

    /**
     * Add an integer at end of the CircularIntArray.
     * @param e  Integer to add.
     */
    public void addLast(int e) {
        mElements[mTail] = e;
        mTail = (mTail + 1) & mCapacityBitmask;
        if (mTail == mHead) {
            doubleCapacity();
        }
    }

    /**
     * Remove first integer from front of the CircularIntArray and return it.
     * @return  The integer removed.
     * @throws {@link ArrayIndexOutOfBoundsException} if CircularIntArray is empty.
     */
    public int popFirst() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        int result = mElements[mHead];
        mHead = (mHead + 1) & mCapacityBitmask;
        return result;
    }

    /**
     * Remove last integer from end of the CircularIntArray and return it.
     * @return  The integer removed.
     * @throws {@link ArrayIndexOutOfBoundsException} if CircularIntArray is empty.
     */
    public int popLast() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        int t = (mTail - 1) & mCapacityBitmask;
        int result = mElements[t];
        mTail = t;
        return result;
    }

    /**
     * Remove all integers from the CircularIntArray.
     */
    public void clear() {
        mTail = mHead;
    }

    /**
     * Remove multiple integers from front of the CircularIntArray, ignore when numOfElements
     * is less than or equals to 0.
     * @param numOfElements  Number of integers to remove.
     * @throws {@link ArrayIndexOutOfBoundsException} if numOfElements is larger than
     *         {@link #size()}
     */
    public void removeFromStart(int numOfElements) {
        if (numOfElements <= 0) {
            return;
        }
        if (numOfElements > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mHead = (mHead + numOfElements) & mCapacityBitmask;
    }

    /**
     * Remove multiple elements from end of the CircularIntArray, ignore when numOfElements
     * is less than or equals to 0.
     * @param numOfElements  Number of integers to remove.
     * @throws {@link ArrayIndexOutOfBoundsException} if numOfElements is larger than
     *         {@link #size()}
     */
    public void removeFromEnd(int numOfElements) {
        if (numOfElements <= 0) {
            return;
        }
        if (numOfElements > size()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        mTail = (mTail - numOfElements) & mCapacityBitmask;
    }

    /**
     * Get first integer of the CircularIntArray.
     * @return The first integer.
     * @throws {@link ArrayIndexOutOfBoundsException} if CircularIntArray is empty.
     */
    public int getFirst() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        return mElements[mHead];
    }

    /**
     * Get last integer of the CircularIntArray.
     * @return The last integer.
     * @throws {@link ArrayIndexOutOfBoundsException} if CircularIntArray is empty.
     */
    public int getLast() {
        if (mHead == mTail) throw new ArrayIndexOutOfBoundsException();
        return mElements[(mTail - 1) & mCapacityBitmask];
    }

    /**
     * Get nth (0 <= n <= size()-1) integer of the CircularIntArray.
     * @param n  The zero based element index in the CircularIntArray.
     * @return The nth integer.
     * @throws {@link ArrayIndexOutOfBoundsException} if n < 0 or n >= size().
     */
    public int get(int n) {
        if (n < 0 || n >= size()) throw new ArrayIndexOutOfBoundsException();
        return mElements[(mHead + n) & mCapacityBitmask];
    }

    /**
     * Get number of integers in the CircularIntArray.
     * @return Number of integers in the CircularIntArray.
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
