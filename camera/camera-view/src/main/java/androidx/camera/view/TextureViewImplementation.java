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

import static androidx.camera.core.SurfaceRequest.Result;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.SurfaceRequest;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.camera.core.impl.utils.futures.FutureCallback;
import androidx.camera.core.impl.utils.futures.Futures;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@link TextureView} implementation for {@link PreviewView}
 */
final class TextureViewImplementation extends PreviewViewImplementation {

    private static final String TAG = "TextureViewImpl";

    TextureView mTextureView;
    SurfaceTexture mSurfaceTexture;
    ListenableFuture<Result> mSurfaceReleaseFuture;
    SurfaceRequest mSurfaceRequest;
    boolean mIsSurfaceTextureDetachedFromView = false;
    SurfaceTexture mDetachedSurfaceTexture;

    AtomicReference<CallbackToFutureAdapter.Completer<Void>> mNextFrameCompleter =
            new AtomicReference<>();

    @Nullable
    OnSurfaceNotInUseListener mOnSurfaceNotInUseListener;

    @Nullable
    @Override
    View getPreview() {
        return mTextureView;
    }

    @Override
    void onAttachedToWindow() {
        reattachSurfaceTexture();
    }

    @Override
    void onDetachedFromWindow() {
        mIsSurfaceTextureDetachedFromView = true;
    }

    @Override
    void onSurfaceRequested(@NonNull SurfaceRequest surfaceRequest,
            @Nullable OnSurfaceNotInUseListener onSurfaceNotInUseListener) {
        mResolution = surfaceRequest.getResolution();
        mOnSurfaceNotInUseListener = onSurfaceNotInUseListener;
        initializePreview();
        if (mSurfaceRequest != null) {
            mSurfaceRequest.willNotProvideSurface();
        }

        mSurfaceRequest = surfaceRequest;

        surfaceRequest.addRequestCancellationListener(
                ContextCompat.getMainExecutor(mTextureView.getContext()),
                () -> {
                    if (mSurfaceRequest != null && mSurfaceRequest == surfaceRequest) {
                        mSurfaceRequest = null;
                        mSurfaceReleaseFuture = null;
                    }

                    notifySurfaceNotInUse();
                });

        tryToProvidePreviewSurface();
    }

    private void notifySurfaceNotInUse() {
        if (mOnSurfaceNotInUseListener != null) {
            mOnSurfaceNotInUseListener.onSurfaceNotInUse();
            mOnSurfaceNotInUseListener = null;
        }
    }

    @Override
    public void initializePreview() {
        Preconditions.checkNotNull(mParent);
        Preconditions.checkNotNull(mResolution);

        mTextureView = new TextureView(mParent.getContext());
        mTextureView.setLayoutParams(
                new FrameLayout.LayoutParams(mResolution.getWidth(), mResolution.getHeight()));
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(final SurfaceTexture surfaceTexture,
                    final int width, final int height) {
                mSurfaceTexture = surfaceTexture;
                tryToProvidePreviewSurface();
            }

            @Override
            public void onSurfaceTextureSizeChanged(final SurfaceTexture surfaceTexture,
                    final int width, final int height) {
            }

            /**
             * If a surface has been provided to the camera (meaning
             * {@link TextureViewImplementation#mSurfaceRequest} is null), but the camera
             * is still using it (meaning {@link TextureViewImplementation#mSurfaceReleaseFuture} is
             * not null), a listener must be added to
             * {@link TextureViewImplementation#mSurfaceReleaseFuture} to ensure the surface
             * is properly released after the camera is done using it.
             *
             * @param surfaceTexture The {@link SurfaceTexture} about to be destroyed.
             * @return false if the camera is not done with the surface, true otherwise.
             */
            @Override
            public boolean onSurfaceTextureDestroyed(final SurfaceTexture surfaceTexture) {
                mSurfaceTexture = null;
                if (mSurfaceRequest == null && mSurfaceReleaseFuture != null) {
                    Futures.addCallback(mSurfaceReleaseFuture,
                            new FutureCallback<Result>() {
                                @Override
                                public void onSuccess(Result result) {
                                    Preconditions.checkState(result.getResultCode()
                                                    != Result.RESULT_SURFACE_ALREADY_PROVIDED,
                                            "Unexpected result from SurfaceRequest. Surface was "
                                                    + "provided twice.");
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
            public void onSurfaceTextureUpdated(final SurfaceTexture surfaceTexture) {
                CallbackToFutureAdapter.Completer<Void> completer =
                        mNextFrameCompleter.getAndSet(null);

                if (completer != null) {
                    completer.set(null);
                }
            }
        });

        // Even though PreviewView calls `removeAllViews()` before calling init(), it should be
        // called again here in case `getPreviewSurfaceProvider()` is called more than once on
        // the same TextureViewImplementation instance.
        mParent.removeAllViews();
        mParent.addView(mTextureView);
    }

    @SuppressWarnings("WeakerAccess")
    void tryToProvidePreviewSurface() {
        if (mResolution == null || mSurfaceTexture == null || mSurfaceRequest == null) {
            return;
        }

        mSurfaceTexture.setDefaultBufferSize(mResolution.getWidth(), mResolution.getHeight());

        final Surface surface = new Surface(mSurfaceTexture);
        final ListenableFuture<Result> surfaceReleaseFuture =
                CallbackToFutureAdapter.getFuture(completer -> {
                    mSurfaceRequest.provideSurface(surface,
                            CameraXExecutors.directExecutor(), completer::set);
                    return "provideSurface[request=" + mSurfaceRequest + " surface=" + surface
                            + "]";
                });
        mSurfaceReleaseFuture = surfaceReleaseFuture;
        mSurfaceReleaseFuture.addListener(() -> {
            notifySurfaceNotInUse();
            surface.release();
            if (mSurfaceReleaseFuture == surfaceReleaseFuture) {
                mSurfaceReleaseFuture = null;
            }
        }, ContextCompat.getMainExecutor(mTextureView.getContext()));

        mSurfaceRequest = null;
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

    @Override
    @NonNull
    ListenableFuture<Void> waitForNextFrame() {
        return CallbackToFutureAdapter.getFuture(
                completer -> {
                    mNextFrameCompleter.set(completer);
                    return "textureViewImpl_waitForNextFrame";
                }
        );
    }
}
