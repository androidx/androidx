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

import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.camera.core.impl.ImageReaderProxy;
import androidx.camera.core.impl.utils.MainThreadAsyncHandler;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An {@link ImageReaderProxy} which wraps around an {@link ImageReader}.
 *
 * <p>All methods map one-to-one between this {@link ImageReaderProxy} and the wrapped {@link
 * ImageReader}.
 */
class AndroidImageReaderProxy implements ImageReaderProxy {
    @GuardedBy("mLock")
    private final ImageReader mImageReader;
    private final Object mLock = new Object();

    /**
     * The active flag of the currently registered listener, or {@code null} if no listener is
     * registered. Each registration gets its own flag so that a callback of a replaced or cleared
     * listener can be skipped without acquiring {@link #mLock}.
     */
    @GuardedBy("mLock")
    private @Nullable AtomicBoolean mActiveListener = null;

    /**
     * Creates a new instance which wraps the given image reader.
     *
     * @param imageReader to wrap
     * @return new {@link AndroidImageReaderProxy} instance
     */
    AndroidImageReaderProxy(ImageReader imageReader) {
        mImageReader = imageReader;
    }

    @Override
    public @Nullable ImageProxy acquireLatestImage() {
        synchronized (mLock) {
            Image image;
            try {
                image = mImageReader.acquireLatestImage();
            } catch (RuntimeException e) {
            /* In API level 23 or below,  it will throw "java.lang.RuntimeException:
               ImageReaderContext is not initialized" when ImageReader is closed. To make the
               behavior consistent as newer API levels,  we make it return null Image instead.*/
                if (isImageReaderContextNotInitializedException(e)) {
                    image = null;
                } else {
                    throw e;  // only catch RuntimeException:ImageReaderContext is not initialized
                }
            }

            if (image == null) {
                return null;
            }
            return new AndroidImageProxy(image);
        }
    }

    @Override
    public @Nullable ImageProxy acquireNextImage() {
        synchronized (mLock) {
            Image image;
            try {
                image = mImageReader.acquireNextImage();
            } catch (RuntimeException e) {
            /* In API level 23 or below,  it will throw "java.lang.RuntimeException:
               ImageReaderContext is not initialized" when ImageReader is closed. To make the
               behavior consistent as newer API levels,  we make it return null Image instead.*/
                if (isImageReaderContextNotInitializedException(e)) {
                    image = null;
                } else {
                    throw e;  // only catch RuntimeException:ImageReaderContext is not initialized
                }
            }

            if (image == null) {
                return null;
            }
            return new AndroidImageProxy(image);
        }
    }

    private boolean isImageReaderContextNotInitializedException(RuntimeException e) {
        return "ImageReaderContext is not initialized".equals(e.getMessage());
    }

    @Override
    public void close() {
        synchronized (mLock) {
            mImageReader.close();
        }
    }

    @Override
    public int getHeight() {
        synchronized (mLock) {
            return mImageReader.getHeight();
        }
    }

    @Override
    public int getWidth() {
        synchronized (mLock) {
            return mImageReader.getWidth();
        }
    }

    @Override
    public int getImageFormat() {
        synchronized (mLock) {
            return mImageReader.getImageFormat();
        }
    }

    @Override
    public int getMaxImages() {
        synchronized (mLock) {
            return mImageReader.getMaxImages();
        }
    }

    @Override
    public @Nullable Surface getSurface() {
        synchronized (mLock) {
            return mImageReader.getSurface();
        }
    }

    @Override
    public void setOnImageAvailableListener(
            @NonNull OnImageAvailableListener listener,
            @NonNull Executor executor) {
        // ImageReader does not accept an executor. If the executor is backed by a background
        // Handler, the callback is delivered on that Handler and the listener is invoked
        // directly. Otherwise, the callback is run on the main handler and then posted to the
        // executor.
        Handler executorHandler = CameraXExecutors.unwrapHandler(executor);
        Looper executorLooper = executorHandler != null ? executorHandler.getLooper() : null;
        // The main looper is excluded: ImageReader posts asynchronous messages, which can
        // overtake synchronous main thread tasks (e.g. CaptureNode#onCaptureStarted) across UI
        // VSYNC sync barriers. Posting to the executor preserves the existing ordering.
        final boolean dispatchDirectly =
                executorLooper != null && executorLooper != Looper.getMainLooper();
        Handler callbackHandler =
                dispatchDirectly ? executorHandler : MainThreadAsyncHandler.getInstance();
        final AtomicBoolean active = new AtomicBoolean(true);
        // The listener and executor are invoked outside of mLock to avoid lock-ordering
        // deadlocks with wrappers that call back into this reader. The callback might still be
        // delivered after the listener is cleared or replaced, so it is skipped if inactive.
        ImageReader.OnImageAvailableListener transformedListener =
                (imageReader) -> {
                    if (!active.get()) {
                        return;
                    }
                    // ImageReader.ListenerHandler reads mListener dynamically without draining
                    // messages already queued on a previous Looper. Verify the callback is
                    // actually running on executorLooper before invoking directly; otherwise post
                    // to executor so the listener never runs on a stale Looper thread.
                    if (dispatchDirectly && Looper.myLooper() == executorLooper) {
                        listener.onImageAvailable(AndroidImageReaderProxy.this);
                        return;
                    }
                    try {
                        executor.execute(
                                () -> {
                                    if (active.get()) {
                                        listener.onImageAvailable(AndroidImageReaderProxy.this);
                                    }
                                });
                    } catch (RejectedExecutionException e) {
                        // b/242470184: the executor may be shut down right after the listener
                        // is cleared. Only ignore the rejection if that is the case.
                        if (active.get()) {
                            throw e;
                        }
                    }
                };
        synchronized (mLock) {
            if (mActiveListener != null) {
                mActiveListener.set(false);
            }
            mActiveListener = active;
            mImageReader.setOnImageAvailableListener(transformedListener, callbackHandler);
        }
    }

    @Override
    public void clearOnImageAvailableListener() {
        synchronized (mLock) {
            if (mActiveListener != null) {
                mActiveListener.set(false);
                mActiveListener = null;
            }
            mImageReader.setOnImageAvailableListener(null, null);
        }
    }
}
