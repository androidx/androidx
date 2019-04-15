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
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An {@link ImageReaderProxy} which matches the incoming {@link android.media.Image} with its
 * {@link ImageInfo}.
 *
 * <p>MetadataImageReader holds an ImageReaderProxy and listens to
 * {@link CameraCaptureCallback}. Then compose them into an {@link ImageProxy} with same
 * timestamp and output it to
 * {@link androidx.camera.core.ImageReaderProxy.OnImageAvailableListener}. User who acquires the
 * ImageProxy is responsible for closing it after use. A limited number of ImageProxy may be
 * acquired at one time as defined by <code>maxImages</code> in the constructor. Any ImageProxy
 * produced after that will be dropped unless one of the ImageProxy currently acquired is closed.
 */
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
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReaderProxy reader) {
                    imageIncoming(reader);
                }
            };

    @GuardedBy("mLock")
    private boolean mClosed = false;

    @GuardedBy("mLock")
    private final ImageReaderProxy mImageReaderProxy;

    @GuardedBy("mLock")
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    @GuardedBy("mLock")
    @Nullable
    private Handler mHandler;

    /** ImageInfos haven't been matched with Image. */
    @GuardedBy("mLock")
    private final Map<Long, ImageInfo> mPendingImageInfos = new HashMap<>();

    /** Images haven't been matched with ImageInfo. */
    @GuardedBy("mLock")
    private final Map<Long, ImageProxy> mPendingImages = new HashMap<>();

    @GuardedBy("mLock")
    private int mImageProxiesIndex;

    /** ImageProxies with matched Image and ImageInfo and are ready to be acquired. */
    @GuardedBy("mLock")
    private List<ImageProxy> mMatchedImageProxies;

    /** ImageProxies which are already acquired. */
    @GuardedBy("mLock")
    private final List<ImageProxy> mAcquiredImageProxies = new ArrayList<>();

    /**
     * Create a {@link MetadataImageReader} with specific configurations.
     *
     * @param width     Width of the ImageReader
     * @param height    Height of the ImageReader
     * @param format    Image format
     * @param maxImages Maximum Image number the ImageReader can hold
     * @param handler   Handler for executing {@link ImageReaderProxy.OnImageAvailableListener}
     */
    MetadataImageReader(int width, int height, int format, int maxImages,
            @Nullable Handler handler) {
        mImageReaderProxy = new AndroidImageReaderProxy(
                ImageReader.newInstance(width, height, format, maxImages));

        init(handler);
    }

    /**
     * Create a {@link MetadataImageReader} with a already created {@link ImageReaderProxy}.
     *
     * @param imageReaderProxy The existed ImageReaderProxy to be set underlying this
     *                         MetadataImageReader.
     * @param handler          Handler for executing
     * {@link ImageReaderProxy.OnImageAvailableListener}
     */
    MetadataImageReader(ImageReaderProxy imageReaderProxy, @Nullable Handler handler) {
        mImageReaderProxy = imageReaderProxy;

        init(handler);
    }

    private void init(Handler handler) {
        mHandler = handler;
        mImageReaderProxy.setOnImageAvailableListener(mTransformedListener, handler);
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

    @Override
    public Surface getSurface() {
        synchronized (mLock) {
            return mImageReaderProxy.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(
            @Nullable final ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler) {
        synchronized (mLock) {
            mListener = listener;
            mHandler = handler;
            mImageReaderProxy.setOnImageAvailableListener(mTransformedListener, handler);
        }
    }

    @Override
    public void onImageClose(ImageProxy image) {
        synchronized (mLock) {
            dequeImageProxy(image);
        }
    }

    private void enqueueImageProxy(SettableImageProxy image) {
        synchronized (mLock) {
            if (mMatchedImageProxies.size() < getMaxImages()) {
                image.addOnImageCloseListener(this);
                mMatchedImageProxies.add(image);
                if (mListener != null) {
                    if (mHandler != null) {
                        mHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mListener.onImageAvailable(MetadataImageReader.this);
                                    }
                                });
                    } else {
                        mListener.onImageAvailable(MetadataImageReader.this);
                    }
                }
            } else {
                Log.d("TAG", "Maximum image number reached.");
                image.close();
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

    @Nullable
    Handler getHandler() {
        return mHandler;
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

            ImageProxy image = null;
            try {
                image = imageReader.acquireNextImage();
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to acquire latest image.", e);
            } finally {
                if (image != null) {
                    // Add the incoming Image to pending list and do the matching logic.
                    mPendingImages.put(image.getTimestamp(), image);

                    matchImages();
                }
            }
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

    // Match incoming Image from the ImageReader with the corresponding ImageInfo.
    private void matchImages() {
        synchronized (mLock) {
            List<Long> toRemove = new ArrayList<>();
            for (Map.Entry<Long, ImageInfo> entry : mPendingImageInfos.entrySet()) {
                ImageInfo imageInfo = entry.getValue();
                long timestamp = imageInfo.getTimestamp();

                if (mPendingImages.containsKey(timestamp)) {
                    ImageProxy image = mPendingImages.get(timestamp);
                    mPendingImages.remove(timestamp);
                    Long key = entry.getKey();
                    toRemove.add(key);
                    // Got a match. Add the ImageProxy to matched list and invoke
                    // onImageAvailableListener.
                    enqueueImageProxy(new SettableImageProxy(image, imageInfo));
                }
            }

            for (Long key : toRemove) {
                mPendingImageInfos.remove(key);
            }
        }
    }
}
