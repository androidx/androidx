/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.camera.testing.fakes;

import android.graphics.ImageFormat;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.TagBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A fake implementation of ImageReaderProxy where the values are settable and the
 * OnImageAvailableListener can be triggered.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class FakeImageReaderProxy implements ImageReaderProxy {
    private int mWidth = 100;
    private int mHeight = 100;
    private int mImageFormat = ImageFormat.JPEG;
    private final int mMaxImages;

    private Surface mSurface;

    @Nullable
    private Executor mExecutor;

    private boolean mIsClosed = false;

    // Queue of all futures for ImageProxys which have not yet been closed.
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
            BlockingQueue<ListenableFuture<Void>> mImageProxyBlockingQueue;

    // Queue of ImageProxys which have not yet been acquired.
    private BlockingQueue<ImageProxy> mImageProxyAcquisitionQueue;

    // List of all ImageProxy which have been acquired. Close them all once the ImageReader is
    // closed
    private List<ImageProxy> mOutboundImageProxy = new ArrayList<>();

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    /**
     * Create a new {@link FakeImageReaderProxy} instance.
     *
     * @param maxImages The maximum number of images that can be acquired at once
     */
    public FakeImageReaderProxy(int maxImages) {
        mMaxImages = maxImages;
        mImageProxyBlockingQueue = new LinkedBlockingQueue<>(maxImages);
        mImageProxyAcquisitionQueue = new LinkedBlockingQueue<>(maxImages);
    }

    /**
     * Create a new {@link FakeImageReaderProxy} instance.
     *
     * @param maxImages The maximum number of images that can be acquired at once
     */
    @NonNull
    public static FakeImageReaderProxy newInstance(int width, int height, int format,
            int maxImages, long usage) {
        FakeImageReaderProxy fakeImageReaderProxy = new FakeImageReaderProxy(maxImages);
        fakeImageReaderProxy.mWidth = width;
        fakeImageReaderProxy.mHeight = height;
        fakeImageReaderProxy.setImageFormat(format);
        return fakeImageReaderProxy;
    }

    @Override
    public ImageProxy acquireLatestImage() {
        ImageProxy imageProxy = null;

        try {
            // Remove and close all ImageProxy aside from last one
            do {
                if (imageProxy != null) {
                    imageProxy.close();
                }
                imageProxy = mImageProxyAcquisitionQueue.remove();
            } while (mImageProxyAcquisitionQueue.size() > 1);
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(
                    "Unable to acquire latest image from empty FakeImageReader");
        }

        mOutboundImageProxy.add(imageProxy);

        return imageProxy;
    }

    @Override
    public ImageProxy acquireNextImage() {
        ImageProxy imageProxy;

        try {
            imageProxy = mImageProxyAcquisitionQueue.remove();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(
                    "Unable to acquire next image from empty FakeImageReader");
        }

        return imageProxy;
    }

    @Override
    public void close() {
        for (ImageProxy imageProxy : mOutboundImageProxy) {
            imageProxy.close();
        }
        mIsClosed = true;
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getImageFormat() {
        return mImageFormat;
    }

    @Override
    public int getMaxImages() {
        return mMaxImages;
    }

    @Nullable
    @Override
    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public void setOnImageAvailableListener(@NonNull OnImageAvailableListener listener,
            @NonNull Executor executor) {
        mListener = listener;
        mExecutor = executor;
    }

    @Override
    public void clearOnImageAvailableListener() {
        mListener = null;
        mExecutor = null;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setImageFormat(int imageFormat) {
        mImageFormat = imageFormat;
    }

    public boolean isClosed() {
        return mIsClosed;
    }

    /**
     * Manually trigger OnImageAvailableListener to notify the Image is ready.
     *
     * <p> Blocks until successfully added an ImageProxy. This can block if the maximum number of
     * ImageProxy have been triggered without a {@link #acquireLatestImage()} or {@link
     * #acquireNextImage()} being called.
     */
    public void triggerImageAvailable(@NonNull TagBundle tagBundle,
            long timestamp) throws InterruptedException {
        FakeImageProxy fakeImageProxy = generateFakeImageProxy(tagBundle, timestamp);

        final ListenableFuture<Void> future = fakeImageProxy.getCloseFuture();
        mImageProxyBlockingQueue.put(future);
        future.addListener(() -> mImageProxyBlockingQueue.remove(future),
                CameraXExecutors.directExecutor()
        );

        mImageProxyAcquisitionQueue.put(fakeImageProxy);

        triggerImageAvailableListener();
    }

    /**
     * Manually trigger OnImageAvailableListener to notify the Image is ready. If unable to add an
     * {@link ImageProxy} after the timeout occurs then will return false.
     *
     * <p> Blocks until successfully added an ImageProxy. This can block if the maximum number of
     * ImageProxy have been triggered without a {@link #acquireLatestImage()} or {@link
     * #acquireNextImage()} being called.
     *
     * @return true if able to trigger the OnImageAvailableListener. Otherwise will return false if
     * it fails to trigger the callback after the timeout period.
     */
    public boolean triggerImageAvailable(@NonNull TagBundle tagBundle, long timestamp, long timeout,
            @NonNull TimeUnit timeUnit) throws InterruptedException {
        FakeImageProxy fakeImageProxy = generateFakeImageProxy(tagBundle, timestamp);

        final ListenableFuture<Void> future = fakeImageProxy.getCloseFuture();
        if (mImageProxyBlockingQueue.offer(future, timeout, timeUnit)) {
            future.addListener(() -> mImageProxyBlockingQueue.remove(future),
                    CameraXExecutors.directExecutor()
            );

            mImageProxyAcquisitionQueue.put(fakeImageProxy);

            triggerImageAvailableListener();

            return true;
        }

        return false;
    }

    private FakeImageProxy generateFakeImageProxy(TagBundle tagBundle, long timestamp) {
        FakeImageInfo fakeImageInfo = new FakeImageInfo();
        fakeImageInfo.setTag(tagBundle);
        fakeImageInfo.setTimestamp(timestamp);

        FakeImageProxy fakeImageProxy = new FakeImageProxy(fakeImageInfo);
        fakeImageProxy.setFormat(mImageFormat);
        fakeImageProxy.setHeight(mHeight);
        fakeImageProxy.setWidth(mWidth);

        return fakeImageProxy;
    }

    private void triggerImageAvailableListener() {
        if (mListener != null) {
            if (mExecutor != null) {
                mExecutor.execute(() -> mListener.onImageAvailable(FakeImageReaderProxy.this));
            } else {
                mListener.onImageAvailable(FakeImageReaderProxy.this);
            }
        }
    }
}
