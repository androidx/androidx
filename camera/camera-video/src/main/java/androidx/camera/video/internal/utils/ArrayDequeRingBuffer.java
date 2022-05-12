/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.video.internal.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Logger;
import androidx.core.util.Preconditions;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Implements {@link RingBuffer} with an {@link ArrayDeque}.
 *
 * <p> A {@link ArrayDequeRingBuffer} must be configured with a maximum size. Each time an element
 * is added to the {@link ArrayDequeRingBuffer} and its size reaches maximum, the element at the
 * head of the {@link ArrayDequeRingBuffer} will be removed.
 *
 * <p> This class is not thread-safe, and does not accept null elements.
 *
 * @param <E> the type of elements stored in the RingBuffer
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class ArrayDequeRingBuffer<E> implements RingBuffer<E> {

    private static final String TAG = "ArrayDequeRingBuffer";

    private final int mCapacity;
    private final Queue<E> mQueue = new ArrayDeque<>();

    public ArrayDequeRingBuffer(int capacity) {
        Preconditions.checkArgument(capacity > 0, "The capacity should be greater than 0.");

        mCapacity = capacity;
    }

    /** {@inheritDoc}
     *
     * @throws NullPointerException if {@code data} is {@code null}.
     */
    @Override
    public boolean offer(@NonNull E data) {
        Preconditions.checkNotNull(data);

        if (!mQueue.offer(data)) {
            return false;
        }

        while (size() > mCapacity) {
            poll();
            Logger.d(TAG, "Head data is dropped because the ring buffer reached its capacity.");
        }

        return true;
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public E poll() {
        return mQueue.poll();
    }

    /** {@inheritDoc} */
    @Nullable
    @Override
    public E peek() {
        return mQueue.peek();
    }

    /** {@inheritDoc} */
    @Override
    public int capacity() {
        return mCapacity;
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return mQueue.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return mQueue.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        mQueue.clear();
    }
}
