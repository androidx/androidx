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
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureBundle;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * An {@link ImageReaderProxy} which takes one or more {@link android.media.Image}, processes it,
 * then output the final result {@link ImageProxy} to
 * {@link ImageReaderProxy.OnImageAvailableListener}.
 *
 * <p>ProcessingImageReader takes {@link CaptureBundle} as the expected set of
 * {@link CaptureStage}. Once all the ImageProxy from the captures are ready. It invokes
 * the {@link CaptureProcessor} set, then returns a single output ImageProxy to
 * OnImageAvailableListener.
 */
class ProcessingImageReader implements ImageReaderProxy {
    private static final String TAG = "ProcessingImageReader";
    final Object mLock = new Object();

    // Callback when Image is ready from InputImageReader.
    private ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(@NonNull ImageReaderProxy reader) {
                    imageIncoming(reader);
                }
            };

    // Callback when Image is ready from OutputImageReader.
    private ImageReaderProxy.OnImageAvailableListener mImageProcessedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(@NonNull ImageReaderProxy reader) {
                    // Callback the output OnImageAvailableListener.
                    ImageReaderProxy.OnImageAvailableListener listener;
                    Executor executor;
                    synchronized (mLock) {
                        listener = mListener;
                        executor = mExecutor;

                        // Resets SettableImageProxyBundle after the processor finishes processing.
                        mSettableImageProxyBundle.reset();
                        setupSettableImageProxyBundleCallbacks();
                    }
                    if (listener != null) {
                        if (executor != null) {
                            executor.execute(
                                    () -> listener.onImageAvailable(ProcessingImageReader.this));
                        } else {
                            listener.onImageAvailable(ProcessingImageReader.this);
                        }
                    }
                }
            };

    // Callback when all the ImageProxies in SettableImageProxyBundle are ready.
    private FutureCallback<List<ImageProxy>> mCaptureStageReadyCallback =
            new FutureCallback<List<ImageProxy>>() {
                @Override
                public void onSuccess(@Nullable List<ImageProxy> imageProxyList) {
                    SettableImageProxyBundle settableImageProxyBundle;
                    synchronized (mLock) {
                        settableImageProxyBundle = mSettableImageProxyBundle;
                    }
                    mCaptureProcessor.process(settableImageProxyBundle);
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
    private final ImageReaderProxy mOutputImageReader;

    @GuardedBy("mLock")
    @Nullable
    ImageReaderProxy.OnImageAvailableListener mListener;

    @GuardedBy("mLock")
    @Nullable
    Executor mExecutor;

    /** The Executor to execute the image post processing task. */
    @NonNull
    final Executor mPostProcessExecutor;

    @NonNull
    final CaptureProcessor mCaptureProcessor;

    @GuardedBy("mLock")
    @NonNull
    SettableImageProxyBundle mSettableImageProxyBundle =
            new SettableImageProxyBundle(Collections.emptyList());

    private final List<Integer> mCaptureIdList = new ArrayList<>();

    /**
     * Create a {@link ProcessingImageReader} with specific configurations.
     *
     * @param width               Width of the ImageReader
     * @param height              Height of the ImageReader
     * @param format              Image format
     * @param maxImages           Maximum Image number the ImageReader can hold. The capacity should
     *                            be greater than the captureBundle size in order to hold all the
     *                            Images needed with this processing.
     * @param postProcessExecutor The Executor to execute the post-process of the image result.
     * @param captureBundle       The {@link CaptureBundle} includes the processing information
     * @param captureProcessor    The {@link CaptureProcessor} to be invoked when the Images are
     *                            ready
     */
    ProcessingImageReader(int width, int height, int format, int maxImages,
            @NonNull Executor postProcessExecutor,
            @NonNull CaptureBundle captureBundle, @NonNull CaptureProcessor captureProcessor) {
        this(new MetadataImageReader(width, height, format, maxImages), postProcessExecutor,
                captureBundle, captureProcessor);
    }

    ProcessingImageReader(@NonNull ImageReaderProxy imageReader,
            @NonNull Executor postProcessExecutor,
            @NonNull CaptureBundle captureBundle,
            @NonNull CaptureProcessor captureProcessor) {
        if (imageReader.getMaxImages() < captureBundle.getCaptureStages().size()) {
            throw new IllegalArgumentException(
                    "MetadataImageReader is smaller than CaptureBundle.");
        }
        mInputImageReader = imageReader;
        mOutputImageReader = new AndroidImageReaderProxy(
                ImageReader.newInstance(imageReader.getWidth(),
                        imageReader.getHeight(), imageReader.getImageFormat(),
                        imageReader.getMaxImages()));

        mPostProcessExecutor = postProcessExecutor;
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
            return mOutputImageReader.acquireLatestImage();
        }
    }

    @Override
    @Nullable
    public ImageProxy acquireNextImage() {
        synchronized (mLock) {
            return mOutputImageReader.acquireNextImage();
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

    @NonNull
    @Override
    public Surface getSurface() {
        synchronized (mLock) {
            return mInputImageReader.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(@NonNull OnImageAvailableListener listener,
            @NonNull Executor executor) {
        synchronized (mLock) {
            mListener = Preconditions.checkNotNull(listener);
            mExecutor = Preconditions.checkNotNull(executor);
            mInputImageReader.setOnImageAvailableListener(mTransformedListener, executor);
            mOutputImageReader.setOnImageAvailableListener(mImageProcessedListener, executor);
        }
    }

    @Override
    public void clearOnImageAvailableListener() {
        synchronized (mLock) {
            mListener = null;
            mExecutor = null;
            mInputImageReader.clearOnImageAvailableListener();
            mOutputImageReader.clearOnImageAvailableListener();

            mSettableImageProxyBundle.close();

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
        synchronized (mLock) {
            if (mInputImageReader instanceof MetadataImageReader) {
                return ((MetadataImageReader) mInputImageReader).getCameraCaptureCallback();
            } else {
                return null;
            }
        }
    }

    @GuardedBy("mLock")
    void setupSettableImageProxyBundleCallbacks() {
        List<ListenableFuture<ImageProxy>> futureList = new ArrayList<>();
        for (Integer id : mCaptureIdList) {
            futureList.add(mSettableImageProxyBundle.getImageProxy(id));
        }
        Futures.addCallback(Futures.allAsList(futureList), mCaptureStageReadyCallback,
                mPostProcessExecutor);
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
                    } else {
                        mSettableImageProxyBundle.addImageProxy(image);
                    }
                }
            }
        }
    }
}
