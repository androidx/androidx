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

package androidx.camera.previewview;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.previewview.PreviewSurfaceRequest.Result;
import androidx.camera.previewview.internal.utils.Logger;
import androidx.camera.previewview.internal.utils.executor.CameraExecutors;
import androidx.camera.previewview.internal.utils.futures.FutureCallback;
import androidx.camera.previewview.internal.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * The {@link TextureView} implementation for {@link CameraViewFinder}
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class TextureViewImplementation extends PreviewViewImplementation {

    private static final String TAG = "TextureViewImpl";

    @SuppressWarnings("WeakerAccess")
    @Nullable
    PreviewSurfaceRequest mSurfaceRequest;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    SurfaceTexture mDetachedSurfaceTexture;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    SurfaceTexture mSurfaceTexture;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    ListenableFuture<Result> mSurfaceReleaseFuture;

    boolean mIsSurfaceTextureDetachedFromView = false;

    // Synthetic Accessor
    @SuppressWarnings("WeakerAccess")
    @Nullable
    TextureView mTextureView;

    TextureViewImplementation(@NonNull FrameLayout parent,
            @NonNull PreviewTransformation previewTransform) {
        super(parent, previewTransform);
    }

    @Override
    void initializePreview() {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkNotNull(mResolution);
        mTextureView = new TextureView(mParent.getContext());
        mTextureView.setLayoutParams(
                new FrameLayout.LayoutParams(mResolution.getWidth(), mResolution.getHeight()));
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull final SurfaceTexture surfaceTexture,
                    final int width, final int height) {
                Logger.d(TAG, "SurfaceTexture available. Size: "
                        + width + "x" + height);
                mSurfaceTexture = surfaceTexture;

                // If a new SurfaceTexture becomes available yet the camera is still using a
                // previous SurfaceTexture, invalidate its surface to notify the camera to
                // request a new surface.
                if (mSurfaceReleaseFuture != null && mSurfaceRequest != null) {
                    Preconditions.checkNotNull(mSurfaceRequest);
                    Logger.d(TAG, "Surface invalidated " + mSurfaceRequest);
                    mSurfaceRequest.getPreviewSurface().close();
                } else {
                    tryToProvidePreviewSurface();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull final SurfaceTexture surfaceTexture,
                    final int width, final int height) {
                Logger.d(TAG, "SurfaceTexture size changed: " + width + "x" + height);
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull final SurfaceTexture surfaceTexture) {
                mSurfaceTexture = null;

                // If the camera is still using the surface, prevent the TextureView from
                // releasing the SurfaceTexture, and instead manually handle it once the camera's
                // no longer using the Surface.
                if (mSurfaceReleaseFuture != null && mTextureView != null) {
                    Futures.addCallback(mSurfaceReleaseFuture,
                            new FutureCallback<Result>() {
                                @Override
                                public void onSuccess(Result result) {
                                    Preconditions.checkState(result.getResultCode()
                                                    != Result.RESULT_SURFACE_ALREADY_PROVIDED,
                                            "Unexpected result from SurfaceRequest. Surface was "
                                                    + "provided twice.");

                                    Logger.d(TAG, "SurfaceTexture about to manually be destroyed");
                                    surfaceTexture.release();

                                    if (mDetachedSurfaceTexture != null) {
                                        mDetachedSurfaceTexture = null;
                                    }
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    throw new IllegalStateException("SurfaceReleaseFuture did not "
                                            + "complete nicely.", t);
                                }
                            }, ContextCompat.getMainExecutor(mTextureView.getContext()));

                    mDetachedSurfaceTexture = surfaceTexture;
                    return false;
                } else {
                    return true;
                }
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull final SurfaceTexture surfaceTexture) {}
        });

        mParent.removeAllViews();
        mParent.addView(mTextureView);
    }

    @Override
    void onSurfaceRequested(@NonNull PreviewSurfaceRequest surfaceRequest) {
        mResolution = surfaceRequest.getResolution();
        initializePreview();
        if (mSurfaceRequest != null) {
            mSurfaceRequest.willNotProvideSurface();
        }

        mSurfaceRequest = surfaceRequest;
        surfaceRequest.addRequestCancellationListener(
                ContextCompat.getMainExecutor(mTextureView.getContext()), () -> {
                    if (mSurfaceRequest != null && mSurfaceRequest == surfaceRequest) {
                        mSurfaceRequest = null;
                        mSurfaceReleaseFuture = null;
                    }
                });

        tryToProvidePreviewSurface();
    }

    @Override
    void onAttachedToWindow() {
        reattachSurfaceTexture();
    }

    @Override
    void onDetachedFromWindow() {
        mIsSurfaceTextureDetachedFromView = true;
    }

    @Nullable
    @Override
    View getPreview() {
        return mTextureView;
    }

    /**
     * Provides a {@link Surface} for preview to the camera only if the {@link TextureView}'s
     * {@link SurfaceTexture} is available, and the {@link PreviewSurfaceRequest} was received from
     * the
     * camera.
     */
    @SuppressWarnings({"WeakerAccess", "ObjectToString"})
    void tryToProvidePreviewSurface() {
        if (mResolution == null || mSurfaceTexture == null || mSurfaceRequest == null) {
            return;
        }

        mSurfaceTexture.setDefaultBufferSize(mResolution.getWidth(), mResolution.getHeight());
        final Surface surface = new Surface(mSurfaceTexture);

        final PreviewSurfaceRequest surfaceRequest = mSurfaceRequest;
        final ListenableFuture<Result> surfaceReleaseFuture = CallbackToFutureAdapter.getFuture(
                completer -> {
                    Logger.d(TAG, "Surface set on Preview.");
                    mSurfaceRequest.provideSurface(surface,
                            CameraExecutors.directExecutor(), new Consumer<Result>() {
                                @Override
                                public void accept(Result result) {
                                    Logger.d(TAG, "provide surface result = "
                                            + result);
                                    completer.set(result);
                                }
                            });
                    return "provideSurface[request=" + mSurfaceRequest + " surface=" + surface
                            + "]";
                });

        mSurfaceReleaseFuture = surfaceReleaseFuture;

        mSurfaceReleaseFuture.addListener(() -> {
            Logger.d(TAG, "Safe to release surface.");
            surface.release();
            if (mSurfaceReleaseFuture == surfaceReleaseFuture) {
                mSurfaceReleaseFuture = null;
            }
            if (mSurfaceRequest == surfaceRequest) {
                mSurfaceRequest = null;
            }
        }, ContextCompat.getMainExecutor(mTextureView.getContext()));

        onSurfaceProvided();
    }

    private void reattachSurfaceTexture() {
        if (mIsSurfaceTextureDetachedFromView
                && mDetachedSurfaceTexture != null
                && mTextureView.getSurfaceTexture() != mDetachedSurfaceTexture) {
            mTextureView.setSurfaceTexture(mDetachedSurfaceTexture);
            mDetachedSurfaceTexture = null;
            mIsSurfaceTextureDetachedFromView = false;
        }
    }
}
