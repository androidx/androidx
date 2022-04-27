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

package androidx.camera.core.internal.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;

/**
 * Implements {@link RingBuffer} with an {@link ArrayDeque}.
 *
 * @param <T> the type of elements stored in the RingBuffer.
 */
public class ArrayRingBuffer<T> implements RingBuffer<T> {

    private final int mRingBufferCapacity;
    private final ArrayDeque<T> mBuffer;
    @Nullable final OnRemoveCallback<T> mOnRemoveCallback;

    public ArrayRingBuffer(int ringBufferCapacity) {
        this(ringBufferCapacity, null);
    }

    public ArrayRingBuffer(int ringBufferCapacity, @Nullable OnRemoveCallback<T> onRemoveCallback) {
        mRingBufferCapacity = ringBufferCapacity;
        mBuffer = new ArrayDeque<>(mRingBufferCapacity);
        mOnRemoveCallback = onRemoveCallback;
    }

    @Override
    public void enqueue(@NonNull T element) {
        if (mBuffer.size() >= mRingBufferCapacity) {
            T removedItem = this.dequeue();
            if (mOnRemoveCallback != null) {
                mOnRemoveCallback.onRemove(removedItem);
            }
        }
        mBuffer.addFirst(element);
    }

    @Override
    public @NonNull T dequeue() {
        T removedElement = mBuffer.removeLast();
        return removedElement;
    }

    @Override
    public int getMaxCapacity() {
        return mRingBufferCapacity;
    }

    @Override
    public boolean isEmpty() {
        return mBuffer.isEmpty();
    }
}
