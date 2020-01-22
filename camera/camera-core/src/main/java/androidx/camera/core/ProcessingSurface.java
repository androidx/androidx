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
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A {@link DeferrableSurface} that does processing and outputs a {@link SurfaceTexture}.
 */
final class ProcessingSurface extends DeferrableSurface implements SurfaceHolder {
    private static final String TAG = "ProcessingSurfaceTextur";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final Object mLock = new Object();

    // Callback when Image is ready from InputImageReader.
    private final ImageReaderProxy.OnImageAvailableListener mTransformedListener =
            new ImageReaderProxy.OnImageAvailableListener() {
                // TODO(b/141958189): Suppressed during upgrade to AGP 3.6.
                @SuppressWarnings("GuardedBy")
                @Override
                public void onImageAvailable(@NonNull ImageReaderProxy reader) {
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

    // The output SurfaceTexture
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    SurfaceTexture mSurfaceTexture;

    // The Surface that is backed by mSurfaceTexture
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @GuardedBy("mLock")
    Surface mSurfaceTextureSurface;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final CaptureStage mCaptureStage;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    @GuardedBy("mLock")
    final CaptureProcessor mCaptureProcessor;

    private final CameraCaptureCallback mCameraCaptureCallback;
    private final CallbackDeferrableSurface mCallbackDeferrableSurface;

    /**
     * Create a {@link ProcessingSurface} with specific configurations.
     *
     * @param width                     Width of the ImageReader
     * @param height                    Height of the ImageReader
     * @param format                    Image format
     * @param handler                   Handler for executing
     *                                  {@link ImageReaderProxy.OnImageAvailableListener}. If
     *                                  this is
     *                                  {@code null} then execution will be done on the calling
     *                                  thread's
     *                                  {@link Looper}.
     * @param captureStage              The {@link CaptureStage} includes the processing information
     * @param captureProcessor          The {@link CaptureProcessor} to be invoked when the
     *                                  Images are ready
     * @param callbackDeferrableSurface the {@link CallbackDeferrableSurface} wrapping user
     *                                  provided {@link Surface} and {@link Executor}
     */
    ProcessingSurface(int width, int height, int format, @Nullable Handler handler,
            @NonNull CaptureStage captureStage, @NonNull CaptureProcessor captureProcessor,
            @NonNull CallbackDeferrableSurface callbackDeferrableSurface) {

        mResolution = new Size(width, height);

        if (handler != null) {
            mImageReaderHandler = handler;
        } else {
            Looper looper = Looper.myLooper();

            if (looper == null) {
                throw new IllegalStateException(
                        "Creating a ProcessingSurfaceTexture requires a non-null Handler, or be "
                                + "created on a thread with a Looper.");
            }

            mImageReaderHandler = new Handler(looper);
        }

        // input
        mInputImageReader = new MetadataImageReader(
                width,
                height,
                format,
                MAX_IMAGES,
                mImageReaderHandler);
        mInputImageReader.setOnImageAvailableListener(mTransformedListener, mImageReaderHandler);
        mInputSurface = mInputImageReader.getSurface();
        mCameraCaptureCallback = mInputImageReader.getCameraCaptureCallback();

        // processing
        mCaptureProcessor = captureProcessor;
        mCaptureProcessor.onResolutionUpdate(mResolution);
        mCaptureStage = captureStage;

        // output
        mCallbackDeferrableSurface = callbackDeferrableSurface;

        Futures.addCallback(callbackDeferrableSurface.getSurface(),
                new FutureCallback<Surface>() {
                    @Override
                    public void onSuccess(@Nullable Surface surface) {
                        synchronized (mLock) {
                            mCaptureProcessor.onOutputSurface(surface, PixelFormat.RGBA_8888);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        Log.e(TAG, "Failed to extract Listenable<Surface>.", t);
                    }
                }, directExecutor());

    }

    @SuppressWarnings("GuardedBy") // TODO(b/141958189): Suppressed during upgrade to AGP 3.6.
    @Override
    @NonNull
    public ListenableFuture<Surface> provideSurface() {
        return Futures.immediateFuture(mInputSurface);
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
                throw new IllegalStateException("ProcessingSurfaceTexture already released!");
            }

            return mCameraCaptureCallback;
        }
    }

    /**
     * Close the {@link ProcessingSurface}.
     *
     * <p> After closing the ProcessingSurfaceTexture it should not be used again. A new instance
     * should be created. This should only be called by the consumer thread.
     */
    @Override
    public void release() {
        synchronized (mLock) {
            if (mReleased) {
                return;
            }
            if (mCallbackDeferrableSurface == null) {
                mSurfaceTexture.release();
                mSurfaceTexture = null;
                mSurfaceTextureSurface.release();
                mSurfaceTextureSurface = null;
            } else {
                mCallbackDeferrableSurface.release();
            }

            mReleased = true;

            // Remove the previous listener so that if an image is queued it will not be processed.
            mInputImageReader.setOnImageAvailableListener(
                    new ImageReaderProxy.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(@NonNull ImageReaderProxy imageReaderProxy) {
                            try (ImageProxy image = imageReaderProxy.acquireLatestImage()) {
                                // Do nothing with image since simply emptying the queue
                            } catch (IllegalStateException e) {
                                // This might be thrown because mInputImageReader.close() might be
                                // called on another thread. However, we can ignore because we are
                                // simply emptying the queue.
                            }
                        }
                    },
                    AsyncTask.THREAD_POOL_EXECUTOR
            );

            // Need to wait for Surface has been detached before closing it
            getTerminationFuture().addListener(this::closeInputs, directExecutor());
            close();
        }
    }

    // Inputs need to wait until DeferrableSurfaces are detached before closing
    @SuppressWarnings("WeakerAccess")
    void closeInputs() {
        synchronized (mLock) {
            mInputImageReader.close();
            mInputSurface.release();
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
            Log.e(TAG, "Failed to acquire next image.", e);
        }

        if (image == null) {
            return;
        }

        ImageInfo imageInfo = image.getImageInfo();
        if (imageInfo == null) {
            image.close();
            return;
        }

        Object tagObject = imageInfo.getTag();
        if (tagObject == null) {
            image.close();
            return;
        }

        if (!(tagObject instanceof Integer)) {
            image.close();
            return;
        }

        Integer tag = (Integer) tagObject;

        if (mCaptureStage.getId() != tag) {
            Log.w(TAG, "ImageProxyBundle does not contain this id: " + tag);
            image.close();
        } else {
            SingleImageProxyBundle imageProxyBundle = new SingleImageProxyBundle(image);
            mCaptureProcessor.process(imageProxyBundle);
            imageProxyBundle.close();
        }
    }
}
