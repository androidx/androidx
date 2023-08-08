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

package androidx.camera.view;

import static java.util.Objects.requireNonNull;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The SurfaceView implementation for {@link PreviewView}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SurfaceViewImplementation extends PreviewViewImplementation {

    private static final String TAG = "SurfaceViewImpl";

    // Wait for 100ms for a screenshot. It usually takes <10ms on Pixel 6a / OS 14.
    private static final int SCREENSHOT_TIMEOUT_MILLIS = 100;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    SurfaceView mSurfaceView;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final SurfaceRequestCallback mSurfaceRequestCallback = new SurfaceRequestCallback();

    SurfaceViewImplementation(@NonNull FrameLayout parent,
            @NonNull PreviewTransformation previewTransform) {
        super(parent, previewTransform);
    }

    @Override
    void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest,
            @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener) {
        if (!shouldReusePreview(mSurfaceView, mResolution, surfaceRequest)) {
            mResolution = surfaceRequest.getResolution();
            initializePreview();
        }
        if (onSurfaceNotInUseListener != null) {
            surfaceRequest.addRequestCancellationListener(
                    ContextCompat.getMainExecutor(mSurfaceView.getContext()),
                    onSurfaceNotInUseListener::onSurfaceNotInUse);
        }

        // Note that View.post will add the Runnable to SurfaceView's message queue. This means
        // that if this line is called while the SurfaceView is detached from window,
        // "setSurfaceRequest" will be pending til the SurfaceView is attached to window and its
        // view is prepared. In other words, "setSurfaceRequest" will happen after
        // "surfaceCreated" is triggered.
        mSurfaceView.post(() -> mSurfaceRequestCallback.setSurfaceRequest(surfaceRequest,
                onSurfaceNotInUseListener));
    }

    @Override
    void initializePreview() {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkNotNull(mResolution);

        mSurfaceView = new SurfaceView(mParent.getContext());
        mSurfaceView.setLayoutParams(
                new FrameLayout.LayoutParams(mResolution.getWidth(), mResolution.getHeight()));
        mParent.removeAllViews();
        mParent.addView(mSurfaceView);
        mSurfaceView.getHolder().addCallback(mSurfaceRequestCallback);
    }

    @Nullable
    @Override
    View getPreview() {
        return mSurfaceView;
    }

    @Override
    void onAttachedToWindow() {
        // Do nothing currently.
    }

    @Override
    void onDetachedFromWindow() {
        // Do nothing currently.
    }

    /**
     * Getting a Bitmap from a Surface is achieved using the `PixelCopy#request()` API, which
     * would introduced in API level 24. PreviewView doesn't currently use a SurfaceView on API
     * levels below 24.
     */
    @RequiresApi(24)
    @Nullable
    @Override
    Bitmap getPreviewBitmap() {
        // If the preview surface isn't ready yet or isn't valid, return null
        if (mSurfaceView == null || mSurfaceView.getHolder().getSurface() == null
                || !mSurfaceView.getHolder().getSurface().isValid()) {
            return null;
        }

        Semaphore screenshotLock = new Semaphore(0);

        // Copy display contents of the surfaceView's surface into a Bitmap.
        final Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                Bitmap.Config.ARGB_8888);

        HandlerThread backgroundThread = new HandlerThread("pixelCopyRequest Thread");
        backgroundThread.start();
        Handler backgroundHandler = new Handler(backgroundThread.getLooper());

        Api24Impl.pixelCopyRequest(mSurfaceView, bitmap, copyResult -> {
            if (copyResult == PixelCopy.SUCCESS) {
                Logger.d(TAG, "PreviewView.SurfaceViewImplementation.getBitmap() succeeded");
            } else {
                Logger.e(TAG, "PreviewView.SurfaceViewImplementation.getBitmap() failed with error "
                        + copyResult);
            }
            screenshotLock.release();
        }, backgroundHandler);
        // Blocks the current thread until the screenshot is done or timed out.
        try {
            boolean success = screenshotLock.tryAcquire(1, SCREENSHOT_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS);
            if (!success) {
                // Fail silently if we can't take the screenshot in time. It's unlikely to
                // happen but when it happens, it's better to return a half rendered screenshot
                // than nothing.
                Logger.e(TAG, "Timed out while trying to acquire screenshot.");
            }
        } catch (InterruptedException e) {
            Logger.e(TAG, "Interrupted while trying to acquire screenshot.", e);
        } finally {
            backgroundThread.quitSafely();
        }
        return bitmap;
    }

    /**
     * The {@link SurfaceHolder.Callback} on mSurfaceView.
     *
     * <p> SurfaceView creates Surface on its own before we can do anything. This class makes
     * sure only the Surface with correct size will be returned to Preview.
     */
    @RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
    class SurfaceRequestCallback implements SurfaceHolder.Callback {

        // Target Surface size. Only complete the SurfaceRequest when the size of the Surface
        // matches this value.
        // Guarded by the UI thread.
        @Nullable
        private Size mTargetSize;

        // SurfaceRequest to set when the target size is met.
        // Guarded by the UI thread.
        @Nullable
        private SurfaceRequest mSurfaceRequest;

        @Nullable
        private SurfaceRequest mSurfaceRequestToBeInvalidated;

        @Nullable
        private OnSurfaceNotInUseListener mOnSurfaceNotInUseListener;

        // The cached size of the current Surface.
        // Guarded by the UI thread.
        @Nullable
        private Size mCurrentSurfaceSize;

        // Guarded by the UI thread.
        private boolean mWasSurfaceProvided = false;

        private boolean mNeedToInvalidate = false;

        /**
         * Sets the completer and the size. The completer will only be set if the current size of
         * the Surface matches the target size.
         */
        @UiThread
        void setSurfaceRequest(@NonNull SurfaceRequest surfaceRequest,
                @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener) {
            // Cancel the previous request, if any
            cancelPreviousRequest();

            if (mNeedToInvalidate) {
                // In some edge cases, the DeferrableSurface behind the SurfaceRequest is timed-out.
                // Since we can not tell if the timeout happened, we invalidate the
                // SurfaceRequest to get a new one when the situation is abnormal. (Normally,
                // invalidate is called when the surface is recreated.)
                // It's not ideal to track the "timed out" state of the SurfaceRequest this way.
                // A better way would be making it part of SurfaceRequest. e.g. something like
                // SurfaceRequest.isTimedOut().
                mNeedToInvalidate = false;
                surfaceRequest.invalidate();
            } else {
                mSurfaceRequest = surfaceRequest;
                mOnSurfaceNotInUseListener = onSurfaceNotInUseListener;
                Size targetSize = surfaceRequest.getResolution();
                mTargetSize = targetSize;
                mWasSurfaceProvided = false;

                if (!tryToComplete()) {
                    // The current size is incorrect. Wait for it to change.
                    Logger.d(TAG, "Wait for new Surface creation.");
                    mSurfaceView.getHolder().setFixedSize(targetSize.getWidth(),
                            targetSize.getHeight());
                }
            }
        }

        /**
         * Sets the completer if size matches.
         *
         * @return true if the completer is set.
         */
        @UiThread
        private boolean tryToComplete() {
            final Surface surface = mSurfaceView.getHolder().getSurface();
            if (canProvideSurface()) {
                Logger.d(TAG, "Surface set on Preview.");

                final OnSurfaceNotInUseListener listener = mOnSurfaceNotInUseListener;
                requireNonNull(mSurfaceRequest).provideSurface(surface,
                        ContextCompat.getMainExecutor(mSurfaceView.getContext()),
                        (result) -> {
                            Logger.d(TAG, "Safe to release surface.");
                            if (listener != null) {
                                listener.onSurfaceNotInUse();
                            }
                        }
                );
                mWasSurfaceProvided = true;
                onSurfaceProvided();
                return true;
            }
            return false;
        }

        private boolean canProvideSurface() {
            return !mWasSurfaceProvided && mSurfaceRequest != null && Objects.equals(mTargetSize,
                    mCurrentSurfaceSize);
        }

        @UiThread
        @SuppressWarnings("ObjectToString")
        private void cancelPreviousRequest() {
            if (mSurfaceRequest != null) {
                Logger.d(TAG, "Request canceled: " + mSurfaceRequest);
                mSurfaceRequest.willNotProvideSurface();
            }
        }

        @UiThread
        @SuppressWarnings("ObjectToString")
        private void closeSurface() {
            if (mSurfaceRequest != null) {
                Logger.d(TAG, "Surface closed " + mSurfaceRequest);
                mSurfaceRequest.getDeferrableSurface().close();
            }
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface created.");

            // Invalidate the surface request so that the requester is notified that the previously
            // obtained surface is no longer valid and should request a new one.
            if (mNeedToInvalidate && mSurfaceRequestToBeInvalidated != null) {
                mSurfaceRequestToBeInvalidated.invalidate();
                mSurfaceRequestToBeInvalidated = null;
                mNeedToInvalidate = false;
            }
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width,
                int height) {
            Logger.d(TAG, "Surface changed. Size: " + width + "x" + height);
            mCurrentSurfaceSize = new Size(width, height);
            tryToComplete();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface destroyed.");

            // If a surface was already provided to the camera, close the surface. Otherwise,
            // cancel the surface request.
            if (mWasSurfaceProvided) {
                closeSurface();
            } else {
                cancelPreviousRequest();
            }

            // The surface is no longer valid. The surface request will be invalidated when the new
            // surface is ready so that the requester can get the new one.
            mNeedToInvalidate = true;
            if (mSurfaceRequest != null) {
                mSurfaceRequestToBeInvalidated = mSurfaceRequest;
            }

            // Reset state
            mWasSurfaceProvided = false;
            mSurfaceRequest = null;
            mOnSurfaceNotInUseListener = null;
            mCurrentSurfaceSize = null;
            mTargetSize = null;
        }
    }

    @Override
    @NonNull
    ListenableFuture<Void> waitForNextFrame() {
        return Futures.immediateFuture(null);
    }

    @Override
    void setFrameUpdateListener(@NonNull Executor executor,
            @NonNull PreviewView.OnFrameUpdateListener listener) {
        throw new IllegalArgumentException("SurfaceView doesn't support frame update listener");
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 7.0 (API 24).
     */
    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
        }

        @DoNotInline
        static void pixelCopyRequest(@NonNull SurfaceView source, @NonNull Bitmap dest,
                @NonNull PixelCopy.OnPixelCopyFinishedListener listener, @NonNull Handler handler) {
            PixelCopy.request(source, dest, listener, handler);
        }
    }

    private static boolean shouldReusePreview(@Nullable SurfaceView surfaceView,
            @Nullable Size resolution, @NonNull SurfaceRequest surfaceRequest) {
        boolean isSameResolution = Objects.equals(resolution, surfaceRequest.getResolution());
        return surfaceView != null && isSameResolution;
    }
}
