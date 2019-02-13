/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.camera.core;

import android.media.Image;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

/**
 * An {@link ImageProxy} which allows forking images with reference counting.
 *
 * <p>When a new instance is constructed, it starts with a reference count of 1. When {@link
 * #fork()} is called, the reference count increments by 1. When {@link #close()} is called on a
 * forked image reference, the reference count decrements by 1. When the reference count reaches 0
 * after a call to {@link #close()}, the underlying {@link Image} is closed.
 */
final class ReferenceCountedImageProxy extends ForwardingImageProxy {
    @GuardedBy("this")
    private int mReferenceCount = 1;

    /**
     * Creates a new instance which wraps the given image and sets the reference count to 1.
     *
     * @param image to wrap
     * @return a new {@link ReferenceCountedImageProxy} instance
     */
    ReferenceCountedImageProxy(ImageProxy image) {
        super(image);
    }

    /**
     * Forks a copy of the image.
     *
     * <p>If the reference count is 0, meaning the image has already been closed previously, null is
     * returned. Otherwise, a forked copy is returned and the reference count is incremented.
     */
    @Nullable
    synchronized ImageProxy fork() {
        if (mReferenceCount <= 0) {
            return null;
        } else {
            mReferenceCount++;
            return new SingleCloseImageProxy(this);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>When the image is closed, the reference count is decremented. If the reference count
     * becomes 0 after this close call, the underlying {@link Image} is also closed.
     */
    @Override
    public synchronized void close() {
        if (mReferenceCount > 0) {
            mReferenceCount--;
            if (mReferenceCount <= 0) {
                super.close();
            }
        }
    }

    /** Returns the current reference count. */
    synchronized int getReferenceCount() {
        return mReferenceCount;
    }
}
