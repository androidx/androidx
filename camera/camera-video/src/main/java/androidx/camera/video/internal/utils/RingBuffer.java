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

/**
 * A limited size FIFO storage, which automatically evicts elements from the head when attempting
 * to add new elements onto the storage and it is full.
 *
 * @param <E> the type of elements stored in the RingBuffer
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public interface RingBuffer<E> {

    /**
     * Adds the specified element to the tail of the {@link RingBuffer}, if the number of elements
     * reaches the capacity, the element at the head of the {@link RingBuffer} will be removed.
     *
     * @param element the element to add
     * @return true if the element was added to the {@link RingBuffer}, else false
     */
    boolean offer(@NonNull E element);

    /**
     * Retrieves and removes the head of the {@link RingBuffer}, or returns null if the
     * {@link RingBuffer} is empty.
     *
     * @return the head of the {@link RingBuffer}, or null if the {@link RingBuffer} is empty
     */
    @Nullable
    E poll();

    /**
     * Retrieves, but does not remove, the head of the {@link RingBuffer}, or returns null if the
     * {@link RingBuffer} is empty.
     *
     * @return the head of the {@link RingBuffer}, or null if the @link RingBuffer} is empty
     */
    @Nullable
    E peek();

    /**
     * Returns the number of elements that the {@link RingBuffer} can hold.
     *
     * @return the number of elements that the {@link RingBuffer} can hold
     */
    int capacity();

    /**
     * Returns the number of elements in the {@link RingBuffer}.
     *
     * @return the number of elements in the {@link RingBuffer}
     */
    int size();

    /**
     * Returns true if the {@link RingBuffer} contains no elements.
     *
     * @return true if the {@link RingBuffer} contains no elements
     */
    boolean isEmpty();

    /**
     * Removes all of the elements. The {@link RingBuffer} will be empty after this method returns.
     */
    void clear();
}
