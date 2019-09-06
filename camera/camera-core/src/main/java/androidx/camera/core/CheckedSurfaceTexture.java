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

import android.graphics.SurfaceTexture;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

/**
 * A {@link DeferrableSurface} which verifies the {@link SurfaceTexture} that backs the {@link
 * Surface} is unreleased before returning the Surface.
 */
final class CheckedSurfaceTexture extends DeferrableSurface implements SurfaceTextureHolder {
    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    final FixedSizeSurfaceTexture mSurfaceTexture;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    @NonNull
    final Surface mSurface;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    final Resource mResource;

    CheckedSurfaceTexture(Size resolution) {
        mResource = new Resource();

        mSurfaceTexture = new FixedSizeSurfaceTexture(0, resolution, mResource);
        mSurfaceTexture.detachFromGLContext();

        mSurface = new Surface(mSurfaceTexture);
        mResource.setSurfaceTexture(mSurfaceTexture);
        mResource.setSurface(mSurface);
    }

    /**
     * Returns the {@link Surface} that is backed by a {@link SurfaceTexture}.
     *
     * <p>If the {@link SurfaceTexture} has already been released then the surface will be reset
     * using a new {@link SurfaceTexture}.
     */
    @Override
    @NonNull
    public ListenableFuture<Surface> provideSurface() {
        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<Surface>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull final CallbackToFutureAdapter.Completer<Surface> completer) {
                        Runnable checkAndSetRunnable =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mResource.isReleasing()) {
                                            completer.setException(new SurfaceClosedException(
                                                    "Surface already released",
                                                    CheckedSurfaceTexture.this));
                                        } else {
                                            completer.set(mSurface);
                                        }
                                    }
                                };
                        runOnMainThread(checkAndSetRunnable);
                        return "CheckSurfaceTexture";
                    }
                });
    }

    @Override
    @NonNull
    public SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    @UiThread
    @Override
    public void release() {
        releaseResourceWhenDetached(mResource);
    }

    void releaseResourceWhenDetached(final Resource resource) {
        resource.setReleasing(true);

        setOnSurfaceDetachedListener(CameraXExecutors.mainThreadExecutor(),
                new OnSurfaceDetachedListener() {
                    @Override
                    public void onSurfaceDetached() {
                        resource.release();
                    }
                });
    }

    void runOnMainThread(Runnable runnable) {
        Executor executor =
                (Looper.myLooper() == Looper.getMainLooper())
                        ? CameraXExecutors.directExecutor()
                        : CameraXExecutors.mainThreadExecutor();
        executor.execute(runnable);
    }

    /**
     * Contains a pair of SurfaceTexture and Surface and also implements
     * FixedSizeSurfaceTexture.Owner interface to control the release timing of
     * FixedSizeSurfaceTexture.
     */
    class Resource implements FixedSizeSurfaceTexture.Owner {
        FixedSizeSurfaceTexture mSurfaceTexture;
        Surface mSurface;
        boolean mIsReleasing = false;
        boolean mIsReadyToRelease = false;

        @UiThread
        public void setSurfaceTexture(FixedSizeSurfaceTexture surfaceTexture) {
            mSurfaceTexture = surfaceTexture;
        }

        @UiThread
        public void setSurface(Surface surface) {
            mSurface = surface;
        }

        public synchronized boolean isReleasing() {
            return mIsReleasing;
        }

        public synchronized void setReleasing(boolean releasing) {
            mIsReleasing = releasing;
        }

        @Override
        public synchronized boolean requestRelease() {
            if (mIsReadyToRelease) {
                return true;
            }

            releaseResourceWhenDetached(this);
            return false;
        }

        @UiThread
        public synchronized void release() {
            mIsReadyToRelease = true;

            if (mSurfaceTexture != null) {
                mSurfaceTexture.release();
                mSurfaceTexture = null;
            }

            if (mSurface != null) {
                mSurface.release();
                mSurface = null;
            }
        }
    }
}
