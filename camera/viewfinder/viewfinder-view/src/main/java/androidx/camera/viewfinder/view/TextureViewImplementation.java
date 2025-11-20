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
import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.UiThread;
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest;
import androidx.camera.viewfinder.core.impl.RefCounted;
import androidx.camera.viewfinder.view.internal.utils.Logger;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import kotlin.Unit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The {@link TextureView} implementation for {@link ViewfinderView}
 */
final class TextureViewImplementation extends ViewfinderImplementation {

    private static final String TAG = "TextureViewImpl";

    private ViewfinderSurfaceRequest mSurfaceRequest = null;

    // SurfaceRequest to check when the target size is met.
    // Guarded by the UI thread.
    private CallbackToFutureAdapter.Completer<RefCounted<Surface>> mSurfaceResponse = null;

    private RefCounted<Surface> mActiveSurface = null;

    private int mCurrentSurfaceWidth = -1;
    private int mCurrentSurfaceHeight = -1;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture,
                        int width, int height) {
                    Logger.d(TAG, "SurfaceTexture available. Size: "
                            + width + "x" + height);
                    mActiveSurface = new RefCounted<>(false, (surfaceToRelease) -> {
                        surfaceToRelease.release();
                        surfaceTexture.release();
                        return Unit.INSTANCE;
                    });
                    mActiveSurface.initialize(new Surface(surfaceTexture));
                    mCurrentSurfaceWidth = width;
                    mCurrentSurfaceHeight = height;

                    tryToComplete();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture,
                        int width, int height) {
                    Logger.d(TAG, "SurfaceTexture size changed: " + width + "x" + height);
                    mCurrentSurfaceWidth = width;
                    mCurrentSurfaceHeight = height;

                    tryToComplete();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                    Logger.d(TAG, "SurfaceTexture destroyed.");

                    // If a surface was already provided to the camera, invalidate it so that it
                    // requests
                    // a new valid one. Otherwise, cancel the surface request.
                    if (mActiveSurface != null) {
                        invalidateSurface();
                        mActiveSurface.release();
                    } else {
                        cancelPreviousRequest();
                    }

                    // Reset state
                    mActiveSurface = null;
                    mCurrentSurfaceWidth = -1;
                    mCurrentSurfaceHeight = -1;

                    // Always return false since releasing the SurfaceTexture will always be handled
                    // by RefCounted
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                }
            };

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @Nullable
    TextureView mTextureView;

    TextureViewImplementation(@NonNull FrameLayout parent,
            @NonNull ViewfinderTransformation viewfinderTransformation) {
        super(parent, viewfinderTransformation);
    }

    void initializeViewfinder(int width, int height) {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkArgument(width > 0);
        Preconditions.checkArgument(height > 0);
        mTextureView = new TextureView(mParent.getContext());
        mTextureView.setLayoutParams(
                new FrameLayout.LayoutParams(width, height));
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mParent.removeAllViews();
        mParent.addView(mTextureView);
    }

    @UiThread
    @Override
    void onSurfaceRequested(@NonNull ViewfinderSurfaceRequest surfaceRequest,
            CallbackToFutureAdapter.Completer<RefCounted<Surface>> surfaceResponse) {
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
                        Objects.requireNonNull(mTextureView).getContext())
        );

        if (!tryToComplete()) {
            Logger.d(TAG, "Wait for new Surface creation.");
        }
    }

    @Override
    void onImplementationReplaced() {
        cancelPreviousRequest();
    }

    @Override
    @Nullable
    View getViewfinder() {
        return mTextureView;
    }

    @Override
    @Nullable
    Bitmap getViewfinderBitmap() {
        // If textureView is still null or its SurfaceTexture isn't available yet, return null
        if (mTextureView == null || !mTextureView.isAvailable()) {
            return null;
        }

        // Get bitmap of the SurfaceTexture's display contents
        return mTextureView.getBitmap();
    }

    /**
     * Sets the completer if size matches.
     *
     * @return true if the completer is set.
     */
    @UiThread
    private boolean tryToComplete() {
        if (mTextureView == null || mSurfaceResponse == null) {
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
