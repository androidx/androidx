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

import static androidx.camera.core.impl.utils.executor.CameraXExecutors.directExecutor;

import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.impl.CameraCaptureCallback;
import androidx.camera.core.impl.CaptureProcessor;
import androidx.camera.core.impl.CaptureStage;
import androidx.camera.core.impl.DeferrableSurface;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.SingleImageProxyBundle;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A {@link DeferrableSurface} that does processing and outputs a {@link SurfaceTexture}.
 */
final class ProcessingSurface extends DeferrableSurface {
    private static final String TAG = "ProcessingSurfaceTextur";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final Object mLock = new Object();

    // Callback when Image is ready from InputImageReader.
    private final ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            reader -> {
                synchronized (mLock) {
                    imageIncoming(reader);
                }
            };

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    boolean mReleased = false;

    @NonNull
    private final Size mResolution;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    final MetadataImageReader mInputImageReader;

    // The Surface that is backed by mInputImageReader
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    final Surface mInputSurface;

    private final Handler mImageReaderHandler;

    // Maximum number of images in the input ImageReader
    private static final int MAX_IMAGES = 2;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final CaptureStage mCaptureStage;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    @GuardedBy("mLock")
    final CaptureProcessor mCaptureProcessor;

    private final CameraCaptureCallback mCameraCaptureCallback;
    private final DeferrableSurface mOutputDeferrableSurface;

    private String mTagBundleKey;
    /**
     * Create a {@link ProcessingSurface} with specific configurations.
     * @param width            Width of the ImageReader
     * @param height           Height of the ImageReader
     * @param format           Image format
     * @param handler          Handler for executing
     *                         {@link ImageReaderProxy.OnImageAvailableListener}. If
     *                         this is
     *                         {@code null} then execution will be done on the calling
     *                         thread's
     *                         {@link Looper}.
     * @param captureStage     The {@link CaptureStage} includes the processing information
     * @param captureProcessor The {@link CaptureProcessor} to be invoked when the
     *                         Images are ready
     * @param outputSurface    The {@link DeferrableSurface} used as the output of
     *                         processing.
     * @param tagBundleKey     The key for tagBundle to get correct image. Usually the key comes
     *                         from the CaptureStage's hash code.
     */
    ProcessingSurface(int width, int height, int format, @Nullable Handler handler,
            @NonNull CaptureStage captureStage, @NonNull CaptureProcessor captureProcessor,
            @NonNull DeferrableSurface outputSurface, @NonNull String tagBundleKey) {
        super(new Size(width, height), format);
        mResolution = new Size(width, height);

        if (handler != null) {
            mImageReaderHandler = handler;
        } else {
            Looper looper = Looper.myLooper();

            if (looper == null) {
                throw new IllegalStateException(
                        "Creating a ProcessingSurface requires a non-null Handler, or be created "
                                + " on a thread with a Looper.");
            }

            mImageReaderHandler = new Handler(looper);
        }

        Executor executor = CameraXExecutors.newHandlerExecutor(mImageReaderHandler);

        // input
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                MAX_IMAGES);
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, executor);
        mInputSurface = mInputImageReader.getSurface();
        mCameraCaptureCallback = mInputImageReader.getCameraCaptureCallback();

        // processing
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onResolutionUpdate(mResolution);
        mCaptureStage = captureStage;

        // output
        mOutputDeferrableSurface = outputSurface;

        mTagBundleKey = tagBundleKey;

        Futures.addCallback(outputSurface.getSurface(),
                new FutureCallback<Surface>() {
                    @Override
                    public void onSuccess(@Nullable Surface surface) {
                        synchronized (mLock) {
                            mCaptureProcessor.onOutputSurface(surface, PixelFormat.RGBA_8888);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Logger.e(TAG, "Failed to extract Listenable<Surface>.", t);
                    }
                }, directExecutor());

        getTerminationFuture().addListener(this::release, directExecutor());
    }

    @Override
    @NonNull
    public ListenableFuture<Surface> provideSurface() {
        synchronized (mLock) {
            return Futures.immediateFuture(mInputSurface);
        }
    }

    /**
     * Returns necessary camera callbacks to retrieve metadata from camera result.
     *
     * @throws IllegalStateException if {@link #release()} has already been called
     */
    @Nullable
    CameraCaptureCallback getCameraCaptureCallback() {
        synchronized (mLock) {
            if (mReleased) {
                throw new IllegalStateException("ProcessingSurface already released!");
            }

            return mCameraCaptureCallback;
        }
    }

    /**
     * Close the {@link ProcessingSurface}.
     *
     * <p> After closing the ProcessingSurface it should not be used again. A new instance
     * should be created.
     *
     * <p>This should only be called once the ProcessingSurface has been terminated, i.e., it's
     * termination future retrieved via {@link #getTerminationFuture()}} has completed.
     */
    private void release() {
        synchronized (mLock) {
            if (mReleased) {
                return;
            }

            // Since the ProcessingSurface DeferrableSurface has been terminated, it is safe to
            // close the inputs.
            mInputImageReader.close();
            mInputSurface.release();

            // Now that the inputs are closed, we can close the output surface.
            mOutputDeferrableSurface.close();

            mReleased = true;
        }
    }

    // Incoming Image from InputImageReader. Acquires it and add to SettableImageProxyBundle.
    @SuppressWarnings("WeakerAccess")
    @GuardedBy("mLock")
    void imageIncoming(ImageReaderProxy imageReader) {
        if (mReleased) {
            return;
        }

        ImageProxy image = null;
        try {
            image = imageReader.acquireNextImage();
        } catch (IllegalStateException e) {
            Logger.e(TAG, "Failed to acquire next image.", e);
        }

        if (image == null) {
            return;
        }

        ImageInfo imageInfo = image.getImageInfo();
        if (imageInfo == null) {
            image.close();
            return;
        }

        Integer tagValue = (Integer) imageInfo.getTagBundle().getTag(mTagBundleKey);
        if (tagValue == null) {
            image.close();
            return;
        }

        if (mCaptureStage.getId() != tagValue) {
            Logger.w(TAG, "ImageProxyBundle does not contain this id: " + tagValue);
            image.close();
        } else {
            SingleImageProxyBundle imageProxyBundle = new SingleImageProxyBundle(image,
                    mTagBundleKey);
            mCaptureProcessor.process(imageProxyBundle);
            imageProxyBundle.close();
        }
    }
}
