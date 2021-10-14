/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.ImageReaderProxy;

import java.util.concurrent.Executor;

/**
 * An {@link ImageReaderProxy} that wraps another ImageReaderProxy to safely wait until all
 * produced {@link ImageProxy} are closed before closing the ImageReaderProxy.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class SafeCloseImageReaderProxy implements ImageReaderProxy {
    // Lock to synchronize acquired ImageProxys and close.
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mOutstandingImages = 0;
    @GuardedBy("mLock")
    private boolean mIsClosed = false;

    // The wrapped instance of ImageReaderProxy
    @GuardedBy("mLock")
    private final ImageReaderProxy mImageReaderProxy;

    @Nullable
    private final Surface mSurface;

    // Called after images are closed to check if the ImageReaderProxy should be closed
    private final ForwardingImageProxy.OnImageCloseListener mImageCloseListener = (image) -> {
        synchronized (mLock) {
            mOutstandingImages--;
            if (mIsClosed && mOutstandingImages == 0) {
                close();
            }
        }
    };

    SafeCloseImageReaderProxy(@NonNull ImageReaderProxy imageReaderProxy) {
        mImageReaderProxy = imageReaderProxy;
        mSurface = imageReaderProxy.getSurface();
    }

    @Nullable
    @Override
    public ImageProxy acquireLatestImage() {
        synchronized (mLock) {
            return wrapImageProxy(mImageReaderProxy.acquireLatestImage());
        }
    }

    @Nullable
    @Override
    public ImageProxy acquireNextImage() {
        synchronized (mLock) {
            return wrapImageProxy(mImageReaderProxy.acquireNextImage());
        }
    }

    /**
     * @inheritDoc <p>This will directly close the wrapped {@link ImageReaderProxy} without
     * waiting for
     * outstanding {@link ImageProxy} to be closed. Typically, when using {@link
     * SafeCloseImageReaderProxy} this should not be directly called. Instead call {@link
     * #safeClose()} which safely waits for all ImageProxy to close before closing the wrapped
     * ImageReaderProxy.
     */
    @Override
    public void close() {
        synchronized (mLock) {
            if (mSurface != null) {
                mSurface.release();
            }
            mImageReaderProxy.close();
        }
    }

    @GuardedBy("mLock")
    @Nullable
    private ImageProxy wrapImageProxy(@Nullable ImageProxy imageProxy) {
        if (imageProxy != null) {
            mOutstandingImages++;
            SingleCloseImageProxy singleCloseImageProxy =
                    new SingleCloseImageProxy(imageProxy);
            singleCloseImageProxy.addOnImageCloseListener(mImageCloseListener);
            return singleCloseImageProxy;
        } else {
            return null;
        }
    }

    /**
     * Close the underlying {@link ImageReaderProxy} safely by deferring the close until the last
     * {@link ImageProxy} has been closed.
     *
     * <p>Once this has been called, no more additional ImageProxy can be acquired from the
     * {@link SafeCloseImageReaderProxy}.
     */
    void safeClose() {
        synchronized (mLock) {
            mIsClosed = true;
            mImageReaderProxy.clearOnImageAvailableListener();

            if (mOutstandingImages == 0) {
                close();
            }
        }
    }

    @Override
    public int getHeight() {
        synchronized (mLock) {
            return mImageReaderProxy.getHeight();
        }
    }

    @Override
    public int getWidth() {
        synchronized (mLock) {
            return mImageReaderProxy.getWidth();
        }
    }

    @Override
    public int getImageFormat() {
        synchronized (mLock) {
            return mImageReaderProxy.getImageFormat();
        }
    }

    @Override
    public int getMaxImages() {
        synchronized (mLock) {
            return mImageReaderProxy.getMaxImages();
        }
    }

    @Nullable
    @Override
    public Surface getSurface() {
        synchronized (mLock) {
            return mImageReaderProxy.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(@NonNull OnImageAvailableListener listener,
            @NonNull Executor executor) {
        synchronized (mLock) {
            mImageReaderProxy.setOnImageAvailableListener(
                    imageReader -> listener.onImageAvailable(this), executor);
        }
    }

    @Override
    public void clearOnImageAvailableListener() {
        synchronized (mLock) {
            mImageReaderProxy.clearOnImageAvailableListener();
        }
    }
}
