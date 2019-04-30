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

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link ImageReaderProxy} which takes one or more {@link android.media.Image}, processes it,
 * then output the final result {@link ImageProxy} to
 * {@link androidx.camera.core.ImageReaderProxy.OnImageAvailableListener}.
 *
 * <p>ProcessingImageReader takes {@link CaptureBundle} as the expected set of
 * {@link CaptureStage}. Once all the ImageProxy from the captures are ready. It invokes
 * the {@link CaptureProcessor} set, then returns a single output ImageProxy to
 * OnImageAvailableListener.
 */
class ProcessingImageReader implements ImageReaderProxy {
    private static final String TAG = "ProcessingImageReader";
    private final Object mLock = new Object();

    // Callback when Image is ready from InputImageReader.
    private ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReaderProxy reader) {
                    imageIncoming(reader);
                }
            };

    // Callback when Image is ready from OutputImageReader.
    private ImageReader.OnImageAvailableListener mImageProcessedListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    // Callback the output OnImageAvailableListener.
                    if (mHandler != null) {
                        mHandler.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        mListener.onImageAvailable(ProcessingImageReader.this);
                                    }
                                });
                    } else {
                        mListener.onImageAvailable(ProcessingImageReader.this);
                    }
                    // Resets SettableImageProxyBundle after the processor finishes processing.
                    mSettableImageProxyBundle.reset();
                    setupSettableImageProxyBundleCallbacks();
                }
            };

    // Callback when all the ImageProxies in SettableImageProxyBundle are ready.
    private FutureCallback<List<ImageProxy>> mCaptureStageReadyCallback =
            new FutureCallback<List<ImageProxy>>() {
                @Override
                public void onSuccess(@Nullable List<ImageProxy> imageProxyList) {
                    mCaptureProcessor.process(mSettableImageProxyBundle);
                }

                @Override
                public void onFailure(Throwable throwable) {

                }
            };

    @GuardedBy("mLock")
    private boolean mClosed = false;

    @GuardedBy("mLock")
    private final ImageReaderProxy mInputImageReader;

    @GuardedBy("mLock")
    private final ImageReader mOutputImageReader;

    @GuardedBy("mLock")
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    @GuardedBy("mLock")
    @Nullable
    Handler mHandler;

    @NonNull
    CaptureProcessor mCaptureProcessor;

    @GuardedBy("mLock")
    SettableImageProxyBundle mSettableImageProxyBundle = null;

    private final List<Integer> mCaptureIdList = new ArrayList<>();

    /**
     * Create a {@link ProcessingImageReader} with specific configurations.
     *
     * @param width            Width of the ImageReader
     * @param height           Height of the ImageReader
     * @param format           Image format
     * @param maxImages        Maximum Image number the ImageReader can hold. The capacity should
     *                        be greater than the captureBundle size in order to hold all the
     *                         Images needed with this processing.
     * @param handler          Handler for executing
     * {@link ImageReaderProxy.OnImageAvailableListener}
     * @param captureBundle    The {@link CaptureBundle} includes the processing information
     * @param captureProcessor The {@link CaptureProcessor} to be invoked when the Images are ready
     */
    ProcessingImageReader(int width, int height, int format, int maxImages,
            @Nullable Handler handler,
            @NonNull CaptureBundle captureBundle, @NonNull CaptureProcessor captureProcessor) {
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                maxImages,
                handler);
        mOutputImageReader = ImageReader.newInstance(width, height, format, maxImages);

        init(handler, captureBundle, captureProcessor);
    }

    ProcessingImageReader(ImageReaderProxy imageReader, @Nullable Handler handler,
            @NonNull CaptureBundle captureBundle,
            @NonNull CaptureProcessor captureProcessor) {
        if (imageReader.getMaxImages() < captureBundle.getCaptureStages().size()) {
            throw new IllegalArgumentException(
                    "MetadataImageReader is smaller than CaptureBundle.");
        }
        mInputImageReader = imageReader;
        mOutputImageReader = ImageReader.newInstance(imageReader.getWidth(),
                imageReader.getHeight(), imageReader.getImageFormat(), imageReader.getMaxImages());

        init(handler, captureBundle, captureProcessor);
    }

    private void init(@Nullable Handler handler, @NonNull CaptureBundle captureBundle,
            @NonNull CaptureProcessor captureProcessor) {
        mHandler = handler;
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
        mOutputImageReader.setOnImageAvailableListener(mImageProcessedListener, handler);
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onOutputSurface(mOutputImageReader.getSurface(), getImageFormat());
        mCaptureProcessor.onResolutionUpdate(
                new Size(mInputImageReader.getWidth(), mInputImageReader.getHeight()));

        setCaptureBundle(captureBundle);
    }

    @Override
    @Nullable
    public ImageProxy acquireLatestImage() {
        synchronized (mLock) {
            Image image = mOutputImageReader.acquireLatestImage();
            if (image == null) {
                return null;
            }

            return new AndroidImageProxy(image);
        }
    }

    @Override
    @Nullable
    public ImageProxy acquireNextImage() {
        synchronized (mLock) {
            Image image = mOutputImageReader.acquireNextImage();
            if (image == null) {
                return null;
            }

            return new AndroidImageProxy(image);
        }
    }

    @Override
    public void close() {
        synchronized (mLock) {
            if (mClosed) {
                return;
            }

            mInputImageReader.close();
            mOutputImageReader.close();
            mSettableImageProxyBundle.close();
            mClosed = true;
        }
    }

    @Override
    public int getHeight() {
        synchronized (mLock) {
            return mInputImageReader.getHeight();
        }
    }

    @Override
    public int getWidth() {
        synchronized (mLock) {
            return mInputImageReader.getWidth();
        }
    }

    @Override
    public int getImageFormat() {
        synchronized (mLock) {
            return mInputImageReader.getImageFormat();
        }
    }

    @Override
    public int getMaxImages() {
        synchronized (mLock) {
            return mInputImageReader.getMaxImages();
        }
    }

    @Override
    public Surface getSurface() {
        synchronized (mLock) {
            return mInputImageReader.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(
            @Nullable final ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler) {
        synchronized (mLock) {
            mListener = listener;
            mHandler = handler;
            mInputImageReader.setOnImageAvailableListener(mTransformedListener, handler);
            mOutputImageReader.setOnImageAvailableListener(mImageProcessedListener, handler);
        }
    }

    /** Sets a CaptureBundle */
    public void setCaptureBundle(@NonNull CaptureBundle captureBundle) {
        synchronized (mLock) {
            if (captureBundle.getCaptureStages() != null) {
                if (mInputImageReader.getMaxImages() < captureBundle.getCaptureStages().size()) {
                    throw new IllegalArgumentException(
                            "CaptureBundle is lager than InputImageReader.");
                }

                mCaptureIdList.clear();

                for (CaptureStage captureStage : captureBundle.getCaptureStages()) {
                    if (captureStage != null) {
                        mCaptureIdList.add(captureStage.getId());
                    }
                }
            }

            mSettableImageProxyBundle = new SettableImageProxyBundle(mCaptureIdList);
            setupSettableImageProxyBundleCallbacks();
        }
    }

    /** Returns necessary camera callbacks to retrieve metadata from camera result. */
    @Nullable
    CameraCaptureCallback getCameraCaptureCallback() {
        if (mInputImageReader instanceof MetadataImageReader) {
            return ((MetadataImageReader) mInputImageReader).getCameraCaptureCallback();
        } else {
            return null;
        }
    }

    void setupSettableImageProxyBundleCallbacks() {
        List<ListenableFuture<ImageProxy>> futureList = new ArrayList<>();
        for (Integer id : mCaptureIdList) {
            futureList.add(mSettableImageProxyBundle.getImageProxy((id)));
        }
        Futures.addCallback(Futures.allAsList(futureList), mCaptureStageReadyCallback,
                CameraXExecutors.directExecutor());
    }

    // Incoming Image from InputImageReader. Acquires it and add to SettableImageProxyBundle.
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
                    Integer tag = (Integer) image.getImageInfo().getTag();
                    if (!mCaptureIdList.contains(tag)) {
                        Log.w(TAG, "ImageProxyBundle does not contain this id: " + tag);
                        image.close();
                        return;
                    }

                    mSettableImageProxyBundle.addImageProxy(image);
                }
            }
        }
    }
}
