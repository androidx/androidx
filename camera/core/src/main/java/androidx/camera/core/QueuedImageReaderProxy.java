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

import android.os.Handler;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An {@link ImageReaderProxy} which maintains a queue of recently available images.
 *
 * <p>Like a conventional {@link android.media.ImageReader}, when the queue becomes full and the
 * user does not close older images quickly enough, newly available images will not be added to the
 * queue and become lost. The user is responsible for setting a listener for newly available images
 * and closing the acquired images quickly enough.
 */
final class QueuedImageReaderProxy
        implements ImageReaderProxy, ForwardingImageProxy.OnImageCloseListener {
    private final int mWidth;
    private final int mHeight;
    private final int mFormat;
    private final int mMaxImages;

    @GuardedBy("this")
    private final Surface mSurface;

    // mMaxImages is not expected to be large, because images consume a lot of memory and there
    // cannot
    // co-exist too many images simultaneously. So, just use a List to simplify the implementation.
    @GuardedBy("this")
    private final List<ImageProxy> mImages;

    @GuardedBy("this")
    private final Set<ImageProxy> mAcquiredImages = new HashSet<>();
    @GuardedBy("this")
    private final Set<OnReaderCloseListener> mOnReaderCloseListeners = new HashSet<>();
    // Current access position in the queue.
    @GuardedBy("this")
    private int mCurrentPosition;
    @GuardedBy("this")
    @Nullable
    private ImageReaderProxy.OnImageAvailableListener mOnImageAvailableListener;
    @GuardedBy("this")
    @Nullable
    private Handler mOnImageAvailableHandler;
    @GuardedBy("this")
    private boolean mClosed;

    /**
     * Creates a new instance of a queued image reader proxy.
     *
     * @param width     of the images
     * @param height    of the images
     * @param format    of the images
     * @param maxImages capacity of the queue
     * @param surface   to which the reader is attached
     * @return new {@link QueuedImageReaderProxy} instance
     */
    QueuedImageReaderProxy(int width, int height, int format, int maxImages, Surface surface) {
        mWidth = width;
        mHeight = height;
        mFormat = format;
        mMaxImages = maxImages;
        mSurface = surface;
        mImages = new ArrayList<>(maxImages);
        mCurrentPosition = 0;
        mClosed = false;
    }

    @Override
    @Nullable
    public synchronized ImageProxy acquireLatestImage() {
        throwExceptionIfClosed();
        if (mImages.isEmpty()) {
            return null;
        }
        if (mCurrentPosition >= mImages.size()) {
            throw new IllegalStateException("Max images have already been acquired without close.");
        }

        // Close all images up to the tail of the list, except for already acquired images.
        List<ImageProxy> imagesToClose = new ArrayList<>();
        for (int i = 0; i < mImages.size() - 1; ++i) {
            if (!mAcquiredImages.contains(mImages.get(i))) {
                imagesToClose.add(mImages.get(i));
            }
        }
        for (ImageProxy image : imagesToClose) {
            // Calling image.close() will cause this.onImageClosed(image) to be called.
            image.close();
        }

        // Move the current position to the tail of the list.
        mCurrentPosition = mImages.size() - 1;
        ImageProxy acquiredImage = mImages.get(mCurrentPosition++);
        mAcquiredImages.add(acquiredImage);
        return acquiredImage;
    }

    @Override
    @Nullable
    public synchronized ImageProxy acquireNextImage() {
        throwExceptionIfClosed();
        if (mImages.isEmpty()) {
            return null;
        }
        if (mCurrentPosition >= mImages.size()) {
            throw new IllegalStateException("Max images have already been acquired without close.");
        }
        ImageProxy acquiredImage = mImages.get(mCurrentPosition++);
        mAcquiredImages.add(acquiredImage);
        return acquiredImage;
    }

    /**
     * Adds an image to the tail of the queue.
     *
     * <p>If the queue already contains the max number of images, the given image is not added to
     * the queue and is closed. This is consistent with the documented behavior of an {@link
     * android.media.ImageReader}, where new images may be lost if older images are not closed
     * quickly enough.
     *
     * <p>If the image is added to the queue and an on-image-available listener has been previously
     * set, the listener is notified that the new image is available.
     *
     * @param image to add
     */
    synchronized void enqueueImage(ForwardingImageProxy image) {
        throwExceptionIfClosed();
        if (mImages.size() < mMaxImages) {
            mImages.add(image);
            image.addOnImageCloseListener(this);
            if (mOnImageAvailableListener != null && mOnImageAvailableHandler != null) {
                final OnImageAvailableListener listener = mOnImageAvailableListener;
                mOnImageAvailableHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (!QueuedImageReaderProxy.this.isClosed()) {
                                    listener.onImageAvailable(QueuedImageReaderProxy.this);
                                }
                            }
                        });
            }
        } else {
            image.close();
        }
    }

    @Override
    public synchronized void close() {
        if (!mClosed) {
            setOnImageAvailableListener(null, null);
            // We need to copy into a different list, because closing an image triggers the on-close
            // listener which in turn modifies the original list.
            List<ImageProxy> imagesToClose = new ArrayList<>(mImages);
            for (ImageProxy image : imagesToClose) {
                image.close();
            }
            mImages.clear();
            mClosed = true;
            notifyOnReaderCloseListeners();
        }
    }

    @Override
    public int getHeight() {
        throwExceptionIfClosed();
        return mHeight;
    }

    @Override
    public int getWidth() {
        throwExceptionIfClosed();
        return mWidth;
    }

    @Override
    public int getImageFormat() {
        throwExceptionIfClosed();
        return mFormat;
    }

    @Override
    public int getMaxImages() {
        throwExceptionIfClosed();
        return mMaxImages;
    }

    @Override
    public synchronized Surface getSurface() {
        throwExceptionIfClosed();
        return mSurface;
    }

    @Override
    public synchronized void setOnImageAvailableListener(
            @Nullable OnImageAvailableListener onImageAvailableListener,
            @Nullable Handler onImageAvailableHandler) {
        throwExceptionIfClosed();
        mOnImageAvailableListener = onImageAvailableListener;
        mOnImageAvailableHandler = onImageAvailableHandler;
    }

    @Override
    public synchronized void onImageClose(ImageProxy image) {
        int index = mImages.indexOf(image);
        if (index >= 0) {
            mImages.remove(index);
            if (index <= mCurrentPosition) {
                mCurrentPosition--;
            }
        }
        mAcquiredImages.remove(image);
    }

    /** Returns the current number of images in the queue. */
    synchronized int getCurrentImages() {
        throwExceptionIfClosed();
        return mImages.size();
    }

    /** Returns true if the reader is already closed. */
    synchronized boolean isClosed() {
        return mClosed;
    }

    /**
     * Adds a listener for close calls on this reader.
     *
     * @param listener to add
     */
    synchronized void addOnReaderCloseListener(OnReaderCloseListener listener) {
        mOnReaderCloseListeners.add(listener);
    }

    private synchronized void throwExceptionIfClosed() {
        if (mClosed) {
            throw new IllegalStateException("This reader is already closed.");
        }
    }

    private synchronized void notifyOnReaderCloseListeners() {
        for (OnReaderCloseListener listener : mOnReaderCloseListeners) {
            listener.onReaderClose(this);
        }
    }

    /** Listener for the reader close event. */
    interface OnReaderCloseListener {
        /**
         * Callback for reader close.
         *
         * @param imageReader which is closed
         */
        void onReaderClose(ImageReaderProxy imageReader);
    }
}
