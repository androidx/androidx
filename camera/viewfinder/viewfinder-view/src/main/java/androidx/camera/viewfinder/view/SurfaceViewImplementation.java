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

package androidx.camera.viewfinder.view;

import android.graphics.Bitmap;
import android.view.PixelCopy;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.UiThread;
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest;
import androidx.camera.viewfinder.core.impl.PixelCopyCompat;
import androidx.camera.viewfinder.core.impl.RefCounted;
import androidx.camera.viewfinder.core.impl.SurfaceControlCompat;
import androidx.camera.viewfinder.view.internal.utils.Logger;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import kotlin.Unit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The SurfaceView implementation for {@link ViewfinderView}.
 */
final class SurfaceViewImplementation extends ViewfinderImplementation {

    private static final String TAG = "SurfaceViewImpl";

    private static final int SCREENSHOT_TIMEOUT_MILLIS = 500;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @Nullable
    SurfaceView mSurfaceView;

    // Target SurfaceRequest. Only complete the SurfaceResponse when the size of the Surface
    // matches this request.
    // Guarded by the UI thread.
    private ViewfinderSurfaceRequest mSurfaceRequest = null;

    // SurfaceRequest to check when the target size is met.
    // Guarded by the UI thread.
    private CallbackToFutureAdapter.Completer<RefCounted<Surface>> mSurfaceResponse = null;

    // Surface that is active between the surfaceCreated and surfaceDestroyed callbacks.
    // Guarded by the UI thread.
    private RefCounted<Surface> mActiveSurface = null;

    // SurfaceControl that is active between the surfaceCreated and surfaceDestroyed callbacks.
    // Allows for finer-grained control over releasing of resources. On API < 29, this is a stub
    // and has no effect.
    // Guarded by the UI thread.
    private SurfaceControlCompat mActiveSurfaceControl = null;

    // The cached size of the current Surface.
    // Guarded by the UI thread.
    private int mCurrentSurfaceWidth = -1;
    private int mCurrentSurfaceHeight = -1;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final SurfaceHolder.@NonNull Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface created.");
            // No-op. Handling surfaceChanged() is enough because it's always called afterwards.
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width,
                int height) {
            Logger.d(TAG, "Surface changed. Size: " + width + "x" + height);
            mCurrentSurfaceWidth = width;
            mCurrentSurfaceHeight = height;

            if (mActiveSurface == null) {
                SurfaceControlCompat surfaceControl =
                        mActiveSurfaceControl = SurfaceControlCompat.create(
                                mSurfaceView,
                                format,
                                width,
                                height,
                                "SurfaceViewImplementation"
                        );

                mActiveSurface = new RefCounted<>(false, (surface) -> {
                    surface.release();
                    surfaceControl.release();
                    return Unit.INSTANCE;
                });

                Surface activeSurface = surfaceControl.newSurface();
                if (activeSurface != null) {
                    mActiveSurface.initialize(activeSurface);
                } else {
                    mActiveSurface.initialize(surfaceHolder.getSurface());
                }
            } else if (mActiveSurfaceControl != null) {
                mActiveSurfaceControl.setBufferSize(width, height);
            }
            tryToComplete();
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            Logger.d(TAG, "Surface destroyed.");

            // If a surface was already provided to the camera, invalidate it so that it requests
            // a new valid one. Otherwise, cancel the surface request.
            if (mActiveSurface != null) {
                if (mActiveSurfaceControl != null) {
                    mActiveSurfaceControl.detach();
                }

                invalidateSurface();
                mActiveSurface.release();
            } else {
                cancelPreviousRequest();
            }

            // Reset state
            mActiveSurface = null;
            mActiveSurfaceControl = null;
            mCurrentSurfaceWidth = -1;
            mCurrentSurfaceHeight = -1;
        }
    };

    SurfaceViewImplementation(@NonNull FrameLayout parent,
            @NonNull ViewfinderTransformation viewfinderTransformation) {
        super(parent, viewfinderTransformation);
    }

    void initializeViewfinder(int width, int height) {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);
        mSurfaceView = new SurfaceView(mParent.getContext());
        mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(width, height));
        mParent.removeAllViews();
        mParent.addView(mSurfaceView);
        mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
    }

    @Override
    void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest surfaceRequest,
            CallbackToFutureAdapter.Completer<RefCounted<Surface>> surfaceResponse) {
        // Cancel the previous request, if any
        cancelPreviousRequest();

        initializeViewfinder(surfaceRequest.getWidth(), surfaceRequest.getHeight());

        mSurfaceRequest = surfaceRequest;
        mSurfaceResponse = surfaceResponse;
        mSurfaceResponse.addCancellationListener(
                () -> {
                    if (Objects.equals(mSurfaceResponse, surfaceResponse)) {
                        // Clean up the request
                        cancelPreviousRequest();
                    }
                },
                ContextCompat.getMainExecutor(
                        Objects.requireNonNull(mSurfaceView).getContext())
        );

        if (!tryToComplete()) {
            // The current size is incorrect. Wait for it to change.
            Logger.d(TAG, "Wait for new Surface creation.");
            Objects.requireNonNull(mSurfaceView).getHolder().setFixedSize(
                    mSurfaceRequest.getWidth(),
                    mSurfaceRequest.getHeight()
            );
        }
    }

    @Override
    void onImplementationReplaced() {
        cancelPreviousRequest();
    }

    /**
     * Getting a Bitmap from a Surface is achieved using the `PixelCopy#request()` API, which
     * would introduced in API level 24. Below API 24, the bitmap will be blank.
     */
    @Override
    @Nullable
    Bitmap getViewfinderBitmap() {
        // If the viewfinder surface isn't ready yet or isn't valid, return null
        RefCounted<Surface> activeSurface = mActiveSurface;
        if (mSurfaceView == null || activeSurface == null) {
            return null;
        }

        Surface surface = activeSurface.acquire();
        if (surface == null || mCurrentSurfaceWidth == 0 || mCurrentSurfaceHeight == 0) {
            return null;
        }

        try {
            // Copy display contents of the surfaceView's surface into a Bitmap.
            final Bitmap bitmap = Bitmap.createBitmap(mCurrentSurfaceWidth, mCurrentSurfaceHeight,
                    Bitmap.Config.ARGB_8888);
            int copyRes = PixelCopyCompat.requestSync(surface, bitmap, SCREENSHOT_TIMEOUT_MILLIS);
            if (copyRes == PixelCopy.SUCCESS) {
                Logger.d(TAG, "SurfaceViewImplementation.getBitmap() succeeded");
            } else {
                Logger.e(TAG, "SurfaceViewImplementation.getBitmap() failed with error " + copyRes);
            }
            return bitmap;
        } finally {
            mActiveSurface.release();
        }
    }

    @Override
    @Nullable
    View getViewfinder() {
        return mSurfaceView;
    }

    /**
     * Sets the completer if size matches.
     *
     * @return true if the completer is set.
     */
    @UiThread
    private boolean tryToComplete() {
        if (mSurfaceView == null || mSurfaceResponse == null) {
            return false;
        }
        if (canProvideSurface()) {
            Logger.d(TAG, "Surface set on viewfinder.");

            if (mSurfaceResponse.set(mActiveSurface)) {
                onSurfaceProvided();
                return true;
            }

            mSurfaceRequest = null;
            mSurfaceResponse = null;
        }
        return false;
    }

    private boolean canProvideSurface() {
        return mActiveSurface != null
                && mSurfaceRequest != null
                && mSurfaceRequest.getWidth() == mCurrentSurfaceWidth
                && mSurfaceRequest.getHeight() == mCurrentSurfaceHeight;
    }

    @UiThread
    private void cancelPreviousRequest() {
        if (mSurfaceRequest != null) {
            Logger.d(TAG, "Request canceled: " + mSurfaceRequest);
            mSurfaceRequest = null;
            mSurfaceResponse.setCancelled();
            mSurfaceResponse = null;
        }
    }

    @UiThread
    private void invalidateSurface() {
        if (mSurfaceRequest != null) {
            Logger.d(TAG, "Surface invalidated: " + mSurfaceRequest);
            // TODO(b/323226220): Differentiate between surface being released by consumer
            //  vs producer
        }
    }
}

