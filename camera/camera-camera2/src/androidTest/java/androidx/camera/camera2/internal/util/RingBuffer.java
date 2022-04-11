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

package androidx.camera.camera2.internal.util;

import androidx.annotation.NonNull;

import java.util.NoSuchElementException;

/**
 * Acts as a queue with limited space.
 *
 * <p>Older entries get dequeued to make space for newer
 * entries.</p>
 *
 * @param <T> the type of elements stored in the RingBuffer.
 */
public interface RingBuffer<T> {

    /**
     * Enqueues an element into the RingBuffer.
     *
     * @param element the element to be enqueued
     */
    void enqueue(@NonNull T element);

    /**
     * Dequeues element from RingBuffer.
     *
     * @throws NoSuchElementException if the buffer is empty.
     *
     * @return dequeued element.
     */
    @NonNull  T dequeue();

    /**
     * Returns the max capacity of the RingBuffer.
     */
    int getMaxCapacity();

    /**
     * Called when an element is removed from the buffer due to capacity.
     *
     * @param <T> the type of element stored in the RingBuffer.
     */
    interface OnRemoveCallback<T> {
        void onRemove(T element);
    }
}
