/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.viewfinder;

import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;
import androidx.camera.viewfinder.internal.utils.Logger;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

/**
 * The SurfaceView implementation for {@link CameraViewfinder}.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class SurfaceViewImplementation extends ViewfinderImplementation {

    private static final String TAG = "SurfaceViewImpl";

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @Nullable
    SurfaceView mSurfaceView;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @NonNull
    final SurfaceRequestCallback mSurfaceRequestCallback = new SurfaceRequestCallback();

    SurfaceViewImplementation(@NonNull FrameLayout parent,
            @NonNull ViewfinderTransformation viewfinderTransformation) {
        super(parent, viewfinderTransformation);
    }

    @Override
    void initializeViewfinder() {
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
    void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest surfaceRequest) {
        mResolution = surfaceRequest.getResolution();
        initializeViewfinder();
        surfaceRequest.addRequestCancellationListener(
                ContextCompat.getMainExecutor(mSurfaceView.getContext()), () -> {});
        mSurfaceView.post(() -> mSurfaceRequestCallback.setSurfaceRequest(surfaceRequest));
    }

    @Override
    void onAttachedToWindow() {
        // Do nothing currently.
    }

    @Override
    void onDetachedFromWindow() {
        // Do nothing currently.
    }

    @Nullable
    @Override
    View getViewfinder() {
        return mSurfaceView;
    }

    /**
     * The {@link SurfaceHolder.Callback} on mSurfaceView.
     *
     * <p> SurfaceView creates Surface on its own before we can do anything. This class makes
     * sure only the Surface with correct size will be returned to viewfinder.
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
        private ViewfinderSurfaceRequest mSurfaceRequest;

        // The cached size of the current Surface.
        // Guarded by the UI thread.
        @Nullable
        private Size mCurrentSurfaceSize;

        // Guarded by the UI thread.
        private boolean mWasSurfaceProvided = false;

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface created.");
            // No-op. Handling surfaceChanged() is enough because it's always called afterwards.
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

            // If a surface was already provided to the camera, invalidate it so that it requests
            // a new valid one. Otherwise, cancel the surface request.
            if (mWasSurfaceProvided) {
                invalidateSurface();
            } else {
                cancelPreviousRequest();
            }

            // Reset state
            mWasSurfaceProvided = false;
            mSurfaceRequest = null;
            mCurrentSurfaceSize = null;
            mTargetSize = null;
        }

        /**
         * Sets the completer and the size. The completer will only be set if the current size of
         * the Surface matches the target size.
         */
        @UiThread
        void setSurfaceRequest(@NonNull ViewfinderSurfaceRequest surfaceRequest) {
            // Cancel the previous request, if any
            cancelPreviousRequest();

            mSurfaceRequest = surfaceRequest;
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

        /**
         * Sets the completer if size matches.
         *
         * @return true if the completer is set.
         */
        @UiThread
        private boolean tryToComplete() {
            if (mSurfaceView == null || mSurfaceRequest == null) {
                return false;
            }
            final Surface surface = mSurfaceView.getHolder().getSurface();
            if (canProvideSurface()) {
                Logger.d(TAG, "Surface set on viewfinder.");
                mSurfaceRequest.provideSurface(surface,
                        ContextCompat.getMainExecutor(mSurfaceView.getContext()),
                        (result) -> {
                            Logger.d(TAG, "provide surface result = " + result);
                        });
                mWasSurfaceProvided = true;
                onSurfaceProvided();
                return true;
            }
            return false;
        }

        private boolean canProvideSurface() {
            return !mWasSurfaceProvided && mSurfaceRequest != null && mTargetSize != null
                    && mTargetSize.equals(mCurrentSurfaceSize);
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
        private void invalidateSurface() {
            if (mSurfaceRequest != null) {
                Logger.d(TAG, "Surface invalidated " + mSurfaceRequest);
                mSurfaceRequest.getViewfinderSurface().close();
            }
        }
    }
}
