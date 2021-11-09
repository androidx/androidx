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

import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.HashSet;
import java.util.Set;

/**
 * An {@link ImageProxy} which forwards all calls to another {@link ImageProxy}.
 *
 * <p>This class enables subclasses to override a few methods to achieve a custom behavior, while
 * still delegating calls on the remaining methods to a wrapped {@link ImageProxy} instance.
 *
 * <p>Listeners for the image close call can be added. When the image is closed, the listeners will
 * be notified.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
abstract class ForwardingImageProxy implements ImageProxy {
    @GuardedBy("this")
    protected final ImageProxy mImage;

    @GuardedBy("this")
    private final Set<OnImageCloseListener> mOnImageCloseListeners = new HashSet<>();

    /**
     * Creates a new instance which wraps the given image.
     *
     * @param image to wrap
     * @return new {@link AndroidImageProxy} instance
     */
    protected ForwardingImageProxy(ImageProxy image) {
        mImage = image;
    }

    @Override
    public void close() {
        synchronized (this) {
            mImage.close();
        }
        notifyOnImageCloseListeners();
    }

    @Override
    @NonNull
    public synchronized Rect getCropRect() {
        return mImage.getCropRect();
    }

    @Override
    public synchronized void setCropRect(@Nullable Rect rect) {
        mImage.setCropRect(rect);
    }

    @Override
    public synchronized int getFormat() {
        return mImage.getFormat();
    }

    @Override
    public synchronized int getHeight() {
        return mImage.getHeight();
    }

    @Override
    public synchronized int getWidth() {
        return mImage.getWidth();
    }

    @Override
    @NonNull
    public synchronized ImageProxy.PlaneProxy[] getPlanes() {
        return mImage.getPlanes();
    }

    @Override
    @NonNull
    public synchronized ImageInfo getImageInfo() {
        return mImage.getImageInfo();
    }

    @Override
    @ExperimentalGetImage
    public synchronized Image getImage() {
        return mImage.getImage();
    }

    /**
     * Adds a listener for close calls on this image.
     *
     * @param listener to add
     */
    synchronized void addOnImageCloseListener(OnImageCloseListener listener) {
        mOnImageCloseListeners.add(listener);
    }

    /** Notifies the listeners that this image has been closed. */
    protected void notifyOnImageCloseListeners() {
        Set<OnImageCloseListener> onImageCloseListeners;
        synchronized (this) {
            // Make a copy for thread safety. We want to synchronize the access for member variables
            // but not the actual callbacks to avoid a deadlock between ForwardingImageProxy and
            // QueuedImageReaderProxy. go/deadlock-in-sharedimagereaderproxy
            onImageCloseListeners = new HashSet<>(mOnImageCloseListeners);
        }
        for (OnImageCloseListener listener : onImageCloseListeners) {
            listener.onImageClose(this);
        }
    }

    /** Listener for the image close event. */
    interface OnImageCloseListener {
        /**
         * Callback for image close.
         *
         * @param image which is closed
         */
        void onImageClose(ImageProxy image);
    }
}
