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

package androidx.camera.core;

import android.media.ImageReader;
import android.util.LongSparseArray;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CameraCaptureResult;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.internal.CameraCaptureResultImageInfo;
import androidx.core.util.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * An {@link ImageReaderProxy} which matches the incoming {@link android.media.Image} with its
 * {@link ImageInfo}.
 *
 * <p>MetadataImageReader holds an ImageReaderProxy and listens to
 * {@link CameraCaptureCallback}. Then compose them into an {@link ImageProxy} with same
 * timestamp and output it to
 * {@link ImageReaderProxy.OnImageAvailableListener}. User who acquires the
 * ImageProxy is responsible for closing it after use. A limited number of ImageProxy may be
 * acquired at one time as defined by <code>maxImages</code> in the constructor. Any ImageProxy
 * produced after that will be dropped unless one of the ImageProxy currently acquired is closed.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class MetadataImageReader implements ImageReaderProxy, ForwardingImageProxy.OnImageCloseListener {
    private static final String TAG = "MetadataImageReader";
    private final Object mLock = new Object();

    // Callback when camera capture is completed.
    private CameraCaptureCallback mCameraCaptureCallback = new CameraCaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureResult cameraCaptureResult) {
            super.onCaptureCompleted(cameraCaptureResult);
            resultIncoming(cameraCaptureResult);
        }
    };

    // Callback when Image is ready from the underlying ImageReader.
    private ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            (reader) -> imageIncoming(reader);

    @GuardedBy("mLock")
    private boolean mClosed = false;

    @GuardedBy("mLock")
    private final ImageReaderProxy mImageReaderProxy;

    @GuardedBy("mLock")
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    @GuardedBy("mLock")
    @Nullable
    private Executor mExecutor;

    /** ImageInfos haven't been matched with Image. */
    @GuardedBy("mLock")
    private final LongSparseArray<ImageInfo> mPendingImageInfos = new LongSparseArray<>();

    /** Images haven't been matched with ImageInfo. */
    @GuardedBy("mLock")
    private final LongSparseArray<ImageProxy> mPendingImages = new LongSparseArray<>();

    @GuardedBy("mLock")
    private int mImageProxiesIndex;

    /** ImageProxies with matched Image and ImageInfo and are ready to be acquired. */
    @GuardedBy("mLock")
    private final List<ImageProxy> mMatchedImageProxies;

    /** ImageProxies which are already acquired. */
    @GuardedBy("mLock")
    private final List<ImageProxy> mAcquiredImageProxies = new ArrayList<>();

    /**
     * Create a {@link MetadataImageReader} with specific configurations.
     *
     * @param width     Width of the ImageReader
     * @param height    Height of the ImageReader
     * @param format    Image format
     * @param maxImages Maximum Image number the ImageReader can hold.
     */
    MetadataImageReader(int width, int height, int format, int maxImages) {
        this(createImageReaderProxy(width, height, format, maxImages));
    }

    /**
     * To workaround the robolectric issue b/151870335. The line breaks is not allowed in the
     * 'this (...)' keyword in the constructor. This workaround is extracted the operation out of
     * 'this (...)' to avoid the issue happen.
     */
    private static ImageReaderProxy createImageReaderProxy(int width, int height, int format,
            int maxImages) {
        return new AndroidImageReaderProxy(
                ImageReader.newInstance(width, height, format, maxImages));
    }

    /**
     * Create a {@link MetadataImageReader} with a already created {@link ImageReaderProxy}.
     *
     * @param imageReaderProxy The existed ImageReaderProxy to be set underlying this
     *                         MetadataImageReader.
     */
    MetadataImageReader(@NonNull ImageReaderProxy imageReaderProxy) {
        mImageReaderProxy = imageReaderProxy;
        mImageProxiesIndex = 0;
        mMatchedImageProxies = new ArrayList<>(getMaxImages());
    }

    @Override
    @Nullable
    public ImageProxy acquireLatestImage() {
        synchronized (mLock) {
            if (mMatchedImageProxies.isEmpty()) {
                return null;
            }
            if (mImageProxiesIndex >= mMatchedImageProxies.size()) {
                throw new IllegalStateException("Maximum image number reached.");
            }

            // Release those older ImageProxies which haven't been acquired.
            List<ImageProxy> toClose = new ArrayList<>();
            for (int i = 0; i < mMatchedImageProxies.size() - 1; i++) {
                if (!mAcquiredImageProxies.contains(mMatchedImageProxies.get(i))) {
                    toClose.add(mMatchedImageProxies.get(i));
                }
            }
            for (ImageProxy image : toClose) {
                image.close();
            }

            // Pop the latest ImageProxy and set the index to the end of list.
            mImageProxiesIndex = mMatchedImageProxies.size() - 1;
            ImageProxy acquiredImage = mMatchedImageProxies.get(mImageProxiesIndex++);
            mAcquiredImageProxies.add(acquiredImage);

            return acquiredImage;
        }
    }

    @Override
    @Nullable
    public ImageProxy acquireNextImage() {
        synchronized (mLock) {
            if (mMatchedImageProxies.isEmpty()) {
                return null;
            }

            if (mImageProxiesIndex >= mMatchedImageProxies.size()) {
                throw new IllegalStateException("Maximum image number reached.");
            }

            // Pop the next matched ImageProxy.
            ImageProxy acquiredImage = mMatchedImageProxies.get(mImageProxiesIndex++);
            mAcquiredImageProxies.add(acquiredImage);

            return acquiredImage;
        }
    }

    @Override
    public void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            List<ImageProxy> imagesToClose = new ArrayList<>(mMatchedImageProxies);
            for (ImageProxy image : imagesToClose) {
                image.close();
            }
            mMatchedImageProxies.clear();

            mImageReaderProxy.close();
            mClosed = true;
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
            mListener = Preconditions.checkNotNull(listener);
            mExecutor = Preconditions.checkNotNull(executor);
            mImageReaderProxy.setOnImageAvailableListener(mTransformedListener, executor);
        }
    }

    @Override
    public void clearOnImageAvailableListener() {
        synchronized (mLock) {
            mListener = null;
            mExecutor = null;
        }
    }

    @Override
    public void onImageClose(ImageProxy image) {
        synchronized (mLock) {
            dequeImageProxy(image);
        }
    }

    private void enqueueImageProxy(SettableImageProxy image) {
        ImageReaderProxy.OnImageAvailableListener listener;
        Executor executor;
        synchronized (mLock) {
            if (mMatchedImageProxies.size() < getMaxImages()) {
                image.addOnImageCloseListener(this);
                mMatchedImageProxies.add(image);
                listener = mListener;
                executor = mExecutor;
            } else {
                Logger.d("TAG", "Maximum image number reached.");
                image.close();
                listener = null;
                executor = null;
            }
        }
        if (listener != null) {
            if (executor != null) {
                executor.execute(() -> listener.onImageAvailable(this));
            } else {
                listener.onImageAvailable(this);
            }
        }
    }

    private void dequeImageProxy(ImageProxy image) {
        synchronized (mLock) {
            int index = mMatchedImageProxies.indexOf(image);
            if (index >= 0) {
                mMatchedImageProxies.remove(index);
                if (index <= mImageProxiesIndex) {
                    mImageProxiesIndex--;
                }
            }
            mAcquiredImageProxies.remove(image);
        }
    }

    // Return the necessary CameraCaptureCallback, which needs to register to capture session.
    CameraCaptureCallback getCameraCaptureCallback() {
        return mCameraCaptureCallback;
    }

    // Incoming Image from underlying ImageReader. Matches it with pending ImageInfo.
    void imageIncoming(ImageReaderProxy imageReader) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            // Acquire all currently pending images in order to prevent backing up of the queue.
            // However don't use acquireLatestImage() to make sure that all images are matched.
            int numAcquired = 0;
            ImageProxy image;
            do {
                image = null;
                try {
                    image = imageReader.acquireNextImage();
                } catch (IllegalStateException e) {
                    Logger.d(TAG, "Failed to acquire next image.", e);
                } finally {
                    if (image != null) {
                        numAcquired++;
                        // Add the incoming Image to pending list and do the matching logic.
                        mPendingImages.put(image.getImageInfo().getTimestamp(), image);
                        matchImages();
                    }
                }
                // Only acquire maxImages number of images in case the producer pushing images into
                // the queue is faster than the rater at which images are acquired to prevent
                // acquiring images indefinitely.
            } while (image != null && numAcquired < imageReader.getMaxImages());
        }
    }

    // Incoming result from camera callback. Creates corresponding ImageInfo and matches it with
    // pending Image.
    void resultIncoming(CameraCaptureResult cameraCaptureResult) {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            // Add the incoming CameraCaptureResult to pending list and do the matching logic.
            mPendingImageInfos.put(cameraCaptureResult.getTimestamp(),
                    new CameraCaptureResultImageInfo(cameraCaptureResult));

            matchImages();
        }
    }

    // Remove the stale {@link ImageProxy} and {@link ImageInfo} from the pending queue if there are
    // any missing which can happen if the camera is momentarily shut off.
    // The ImageProxy and ImageInfo timestamps are assumed to be monotonically increasing. This
    // means any ImageProxy or ImageInfo which has a timestamp older (smaller in value) than the
    // oldest timestamp in the other queue will never get matched, so they should be removed.
    //
    // This should only be called at the end of matchImages(). The assumption is that there are no
    // matching timestamps.
    private void removeStaleData() {
        synchronized (mLock) {
            // No stale data to remove
            if (mPendingImages.size() == 0 || mPendingImageInfos.size() == 0) {
                return;
            }

            Long minImageProxyTimestamp = mPendingImages.keyAt(0);
            Long minImageInfoTimestamp = mPendingImageInfos.keyAt(0);

            // If timestamps are equal then matchImages did not correctly match up the ImageInfo
            // and ImageProxy
            Preconditions.checkArgument(!minImageInfoTimestamp.equals(minImageProxyTimestamp));

            if (minImageInfoTimestamp > minImageProxyTimestamp) {
                for (int i = mPendingImages.size() - 1; i >= 0; i--) {
                    if (mPendingImages.keyAt(i) < minImageInfoTimestamp) {
                        ImageProxy imageProxy = mPendingImages.valueAt(i);
                        imageProxy.close();
                        mPendingImages.removeAt(i);
                    }
                }
            } else {
                for (int i = mPendingImageInfos.size() - 1; i >= 0; i--) {
                    if (mPendingImageInfos.keyAt(i) < minImageProxyTimestamp) {
                        mPendingImageInfos.removeAt(i);
                    }
                }
            }

        }
    }

    // Match incoming Image from the ImageReader with the corresponding ImageInfo.
    private void matchImages() {
        synchronized (mLock) {
            // Iterate in reverse order so that ImageInfo can be removed in place
            for (int i = mPendingImageInfos.size() - 1; i >= 0; i--) {
                ImageInfo imageInfo = mPendingImageInfos.valueAt(i);
                long timestamp = imageInfo.getTimestamp();

                ImageProxy image = mPendingImages.get(timestamp);

                if (image != null) {
                    mPendingImages.remove(timestamp);
                    mPendingImageInfos.removeAt(i);
                    // Got a match. Add the ImageProxy to matched list and invoke
                    // onImageAvailableListener.
                    enqueueImageProxy(new SettableImageProxy(image, imageInfo));
                }
            }

            removeStaleData();
        }
    }
}
