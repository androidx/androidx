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
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.camera.core.Logger;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * The SurfaceView implementation for {@link PreviewView}.
 */
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

    // Whether mSurfaceView displays the frames through a ContentRotationLayer, which applies the
    // remaining rotation of a stream without the camera transform. Fixed for each SurfaceView, see
    // shouldReusePreview().
    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    boolean mRotatesContent;

    SurfaceViewImplementation(@NonNull FrameLayout parent,
            @NonNull PreviewTransformation previewTransform) {
        super(parent, previewTransform);
    }

    @Override
    void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest,
            @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener) {
        boolean rotatesContent = shouldRotateContent(surfaceRequest);
        if (!shouldReusePreview(mSurfaceView, mResolution, mRotatesContent, surfaceRequest,
                rotatesContent)) {
            mResolution = surfaceRequest.getResolution();
            mRotatesContent = rotatesContent;
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

    @Override
    @Nullable View getPreview() {
        return mSurfaceView;
    }

    @Override
    void redrawPreview() {
        super.redrawPreview();
        mSurfaceRequestCallback.updateContentRotation(/* force= */ false, mSurfaceView);
        if (!mRotatesContent && getPreviewTransform().getSurfaceViewContentRotationDegrees() != 0) {
            Logger.e(TAG, "Custom rotation not supported with SurfaceView/PERFORMANCE mode.");
        }
    }

    /**
     * Returns whether a {@link SurfaceView} can apply the remaining rotation of a stream without
     * the camera transform, which requires {@link android.view.SurfaceControl} on API 29+.
     */
    static boolean canRotateContent() {
        return Build.VERSION.SDK_INT >= 29;
    }

    /**
     * Returns whether the frames of the {@link SurfaceRequest} need a {@link ContentRotationLayer},
     * i.e. the stream has no camera transform, so the remaining rotation must be applied by
     * {@link PreviewView}.
     */
    private static boolean shouldRotateContent(@NonNull SurfaceRequest surfaceRequest) {
        return canRotateContent() && !PreviewView.hasCameraTransform(surfaceRequest);
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
    @Override
    @Nullable Bitmap getPreviewBitmap() {
        // If the preview surface isn't ready yet or isn't valid, return null
        if (mSurfaceView == null || mSurfaceView.getHolder().getSurface() == null
                || !mSurfaceView.getHolder().getSurface().isValid()) {
            return null;
        }
        // The frames are not drawn to the SurfaceView's own surface when they go through a
        // ContentRotationLayer. Copy the unrotated content instead, which is transformed the same
        // way as a TextureView screenshot.
        Surface contentSurface = mSurfaceRequestCallback.getContentSurface();

        Semaphore screenshotLock = new Semaphore(0);

        // Copy display contents of the surfaceView's surface into a Bitmap.
        final Bitmap bitmap = Bitmap.createBitmap(mSurfaceView.getWidth(), mSurfaceView.getHeight(),
                Bitmap.Config.ARGB_8888);

        HandlerThread backgroundThread = new HandlerThread("pixelCopyRequest Thread");
        backgroundThread.start();
        Handler backgroundHandler = new Handler(backgroundThread.getLooper());

        PixelCopy.OnPixelCopyFinishedListener listener = copyResult -> {
            if (copyResult == PixelCopy.SUCCESS) {
                Logger.d(TAG, "PreviewView.SurfaceViewImplementation.getBitmap() succeeded");
            } else {
                Logger.e(TAG, "PreviewView.SurfaceViewImplementation.getBitmap() failed with error "
                        + copyResult);
            }
            screenshotLock.release();
        };
        if (contentSurface != null) {
            Api24Impl.pixelCopyRequest(contentSurface, bitmap, listener, backgroundHandler);
        } else {
            Api24Impl.pixelCopyRequest(mSurfaceView, bitmap, listener, backgroundHandler);
        }
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
    class SurfaceRequestCallback implements SurfaceHolder.Callback {

        // Target Surface size. Only complete the SurfaceRequest when the size of the Surface
        // matches this value.
        // Guarded by the UI thread.
        private @Nullable Size mTargetSize;

        // SurfaceRequest to set when the target size is met.
        // Guarded by the UI thread.
        private @Nullable SurfaceRequest mSurfaceRequest;

        private @Nullable SurfaceRequest mSurfaceRequestToBeInvalidated;

        private @Nullable OnSurfaceNotInUseListener mOnSurfaceNotInUseListener;

        // The cached size of the current Surface.
        // Guarded by the UI thread.
        private @Nullable Size mCurrentSurfaceSize;

        // Guarded by the UI thread.
        private boolean mWasSurfaceProvided = false;

        private boolean mNeedToInvalidate = false;

        // The layer that displays the frames when mRotatesContent is true, from when the
        // SurfaceView's surface is ready until it's destroyed. API 29+ only.
        // Guarded by the UI thread.
        private @Nullable ContentRotationLayer mContentRotationLayer;

        // The clockwise rotation applied to the content of mContentRotationLayer.
        // Guarded by the UI thread.
        private int mContentRotationDegrees = 0;

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
            if (canProvideSurface()) {
                Logger.d(TAG, "Surface set on Preview.");

                final OnSurfaceNotInUseListener listener = mOnSurfaceNotInUseListener;
                final ContentRotationLayer contentRotationLayer = mContentRotationLayer;
                final Surface surface;
                if (Build.VERSION.SDK_INT >= 29 && contentRotationLayer != null) {
                    // Keep the layer until the camera no longer uses its Surface.
                    contentRotationLayer.acquire();
                    surface = contentRotationLayer.getSurface();
                } else {
                    surface = mSurfaceView.getHolder().getSurface();
                }
                requireNonNull(mSurfaceRequest).provideSurface(surface,
                        ContextCompat.getMainExecutor(mSurfaceView.getContext()),
                        (result) -> {
                            Logger.d(TAG, "Safe to release surface.");
                            if (Build.VERSION.SDK_INT >= 29 && contentRotationLayer != null) {
                                contentRotationLayer.release();
                            }
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
                    mCurrentSurfaceSize) && (!mRotatesContent || mContentRotationLayer != null);
        }

        /** Returns the Surface of the active {@link ContentRotationLayer}, if any. */
        @UiThread
        @Nullable Surface getContentSurface() {
            ContentRotationLayer contentRotationLayer = mContentRotationLayer;
            if (Build.VERSION.SDK_INT >= 29 && contentRotationLayer != null) {
                return contentRotationLayer.getSurface();
            }
            return null;
        }

        /**
         * Rotates the content of the active {@link ContentRotationLayer} by the remaining rotation
         * of the stream, consistent with how {@link PreviewTransformation} scales the
         * {@link SurfaceView}.
         *
         * @param force    whether to apply the rotation even if it did not change, e.g. when the
         *                 size of the content changed.
         * @param syncView the view whose next frame the change is synchronized with, or
         *                 {@code null} to apply it immediately, e.g. before the first frame.
         */
        @UiThread
        void updateContentRotation(boolean force, @Nullable View syncView) {
            ContentRotationLayer contentRotationLayer = mContentRotationLayer;
            Size size = mCurrentSurfaceSize;
            if (Build.VERSION.SDK_INT < 29 || contentRotationLayer == null || size == null
                    || size.getWidth() <= 0 || size.getHeight() <= 0) {
                return;
            }
            int rotationDegrees = getPreviewTransform().getSurfaceViewContentRotationDegrees();
            if (!force && rotationDegrees == mContentRotationDegrees) {
                return;
            }
            contentRotationLayer.setContentRotation(rotationDegrees, size.getWidth(),
                    size.getHeight(), syncView);
            mContentRotationDegrees = rotationDegrees;
        }

        /**
         * Hides the content of the active {@link ContentRotationLayer} and releases the owner's
         * reference. The layer is released once the camera no longer uses its Surface.
         */
        @UiThread
        private void releaseContentRotationLayer() {
            ContentRotationLayer contentRotationLayer = mContentRotationLayer;
            mContentRotationLayer = null;
            if (Build.VERSION.SDK_INT >= 29 && contentRotationLayer != null) {
                contentRotationLayer.detach();
                contentRotationLayer.release();
            }
        }

        @UiThread
        @SuppressWarnings("ObjectToString")
        private void cancelPreviousRequest() {
            if (mSurfaceRequest != null) {
                Logger.d(TAG, "Request canceled: " + mSurfaceRequest);
                if (mSurfaceRequest.willNotProvideSurface() && mOnSurfaceNotInUseListener != null) {
                    // Invokes the onSurfaceNotInUse method to ensure the PreviewView can do the
                    // clean flow properly.
                    mOnSurfaceNotInUseListener.onSurfaceNotInUse();
                }
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
            Size previousSize = mCurrentSurfaceSize;
            mCurrentSurfaceSize = new Size(width, height);
            if (mRotatesContent && Build.VERSION.SDK_INT >= 29) {
                boolean isSizeChanged = !mCurrentSurfaceSize.equals(previousSize);
                if (mContentRotationLayer != null && isSizeChanged && !mWasSurfaceProvided) {
                    // Recreate the layer, since the default buffer size of its Surface can't be
                    // updated, e.g. when the SurfaceView is resized to the requested size.
                    releaseContentRotationLayer();
                }
                if (mContentRotationLayer == null) {
                    mContentRotationLayer = new ContentRotationLayer(mSurfaceView, width, height);
                    // Apply the rotation before any frame is produced to the new layer.
                    updateContentRotation(/* force= */ true, /* syncView= */ null);
                } else if (isSizeChanged) {
                    // Refit the rotated content to the new size, with the View transformations.
                    mContentRotationLayer.setBufferSize(width, height);
                    updateContentRotation(/* force= */ true, mSurfaceView);
                }
            }
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

            releaseContentRotationLayer();

            // Reset state
            mWasSurfaceProvided = false;
            mSurfaceRequest = null;
            mOnSurfaceNotInUseListener = null;
            mCurrentSurfaceSize = null;
            mTargetSize = null;
        }
    }

    @Override
    @NonNull ListenableFuture<Void> waitForNextFrame() {
        return Futures.immediateFuture(null);
    }

    @Override
    void setFrameUpdateListener(@NonNull Executor executor,
            PreviewView.@NonNull OnFrameUpdateListener listener) {
        throw new IllegalArgumentException("SurfaceView doesn't support frame update listener");
    }

    /**
     * Nested class to avoid verification errors for methods introduced in Android 7.0 (API 24).
     */
    @RequiresApi(24)
    private static class Api24Impl {

        private Api24Impl() {
        }

        static void pixelCopyRequest(@NonNull SurfaceView source, @NonNull Bitmap dest,
                PixelCopy.@NonNull OnPixelCopyFinishedListener listener, @NonNull Handler handler) {
            PixelCopy.request(source, dest, listener, handler);
        }

        static void pixelCopyRequest(@NonNull Surface source, @NonNull Bitmap dest,
                PixelCopy.@NonNull OnPixelCopyFinishedListener listener, @NonNull Handler handler) {
            PixelCopy.request(source, dest, listener, handler);
        }
    }

    private static boolean shouldReusePreview(@Nullable SurfaceView surfaceView,
            @Nullable Size resolution, boolean rotatesContent,
            @NonNull SurfaceRequest surfaceRequest, boolean newRotatesContent) {
        boolean isSameResolution = Objects.equals(resolution, surfaceRequest.getResolution());
        return surfaceView != null && isSameResolution && rotatesContent == newRotatesContent;
    }
}
