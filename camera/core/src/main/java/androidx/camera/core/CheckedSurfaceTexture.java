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
import android.opengl.GLES20;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.VisibleForTesting;
import androidx.camera.core.impl.utils.executor.CameraXExecutors;
import androidx.concurrent.futures.CallbackToFutureAdapter;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A {@link DeferrableSurface} which verifies the {@link SurfaceTexture} that backs the {@link
 * Surface} is unreleased before returning the Surface.
 */
final class CheckedSurfaceTexture extends DeferrableSurface {
    private final OnTextureChangedListener mOutputChangedListener;
    final List<Surface> mSurfaceToReleaseList = new ArrayList<>();
    @Nullable
    FixedSizeSurfaceTexture mSurfaceTexture;
    @Nullable
    Surface mSurface;
    @Nullable
    private Size mResolution;

    Object mLock = new Object();

    @VisibleForTesting
    @GuardedBy("mLock")
    final Map<SurfaceTexture, Resource> mResourceMap = new HashMap<>();

    CheckedSurfaceTexture(
            OnTextureChangedListener outputChangedListener) {
        mOutputChangedListener = outputChangedListener;
    }

    private FixedSizeSurfaceTexture createDetachedSurfaceTexture(Size resolution) {
        IntBuffer buffer = IntBuffer.allocate(1);
        GLES20.glGenTextures(1, buffer);
        Resource resource = new Resource();
        FixedSizeSurfaceTexture surfaceTexture = new FixedSizeSurfaceTexture(buffer.get(),
                resolution, resource);
        surfaceTexture.detachFromGLContext();
        resource.setSurfaceTexture(surfaceTexture);

        synchronized (mLock) {
            mResourceMap.put(surfaceTexture, resource);
        }

        return surfaceTexture;
    }

    @UiThread
    void setResolution(Size resolution) {
        mResolution = resolution;
    }

    @UiThread
    void resetSurfaceTexture() {
        if (mResolution == null) {
            throw new IllegalStateException(
                    "setResolution() must be called before resetSurfaceTexture()");
        }

        release();

        mSurfaceTexture = createDetachedSurfaceTexture(mResolution);
        mOutputChangedListener.onTextureChanged(mSurfaceTexture, mResolution);
    }


    @UiThread
    boolean isSurfaceTextureReleasing(FixedSizeSurfaceTexture surfaceTexture) {
        synchronized (mLock) {
            Resource resource = mResourceMap.get(surfaceTexture);
            if (resource == null) {
                return true;
            }

            return resource.isReleasing();
        }
    }

    /**
     * Returns the {@link Surface} that is backed by a {@link SurfaceTexture}.
     *
     * <p>If the {@link SurfaceTexture} has already been released then the surface will be reset
     * using a new {@link SurfaceTexture}.
     */
    @Override
    public ListenableFuture<Surface> getSurface() {
        return CallbackToFutureAdapter.getFuture(
                new CallbackToFutureAdapter.Resolver<Surface>() {
                    @Override
                    public Object attachCompleter(
                            @NonNull final CallbackToFutureAdapter.Completer<Surface> completer) {
                        Runnable checkAndSetRunnable =
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (isSurfaceTextureReleasing(mSurfaceTexture)) {
                                            // Reset the surface texture and notify the listener
                                            CheckedSurfaceTexture.this.resetSurfaceTexture();
                                        }

                                        if (mSurface == null) {
                                            mSurface = createSurfaceFrom(mSurfaceTexture);
                                        }
                                        completer.set(mSurface);
                                    }
                                };
                        runOnMainThread(checkAndSetRunnable);
                        return "CheckSurfaceTexture";
                    }
                });
    }

    @UiThread
    Surface createSurfaceFrom(FixedSizeSurfaceTexture surfaceTexture) {
        Surface surface = new Surface(surfaceTexture);

        synchronized (mLock) {
            Resource resource = mResourceMap.get(surfaceTexture);
            if (resource == null) {
                resource = new Resource();
                resource.setSurfaceTexture(surfaceTexture);
                mResourceMap.put(surfaceTexture, resource);
            }

            resource.setSurface(surface);
        }
        return surface;
    }

    @Override
    public void refresh() {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (isSurfaceTextureReleasing(mSurfaceTexture)) {
                    // Reset the surface texture and notify the listener
                    CheckedSurfaceTexture.this.resetSurfaceTexture();
                }
                // To fix the incorrect preview orientation for devices running on legacy camera,
                // it needs to attach a new Surface instance to the newly created camera capture
                // session.
                if (mSurface != null) {
                    mSurfaceToReleaseList.add(mSurface);
                }
                mSurface = createSurfaceFrom(mSurfaceTexture);
            }
        });
    }

    @UiThread
    void release() {
        if (mSurface == null && mSurfaceTexture == null) {
            return;
        }

        Resource resource;
        synchronized (mLock) {
            resource = mResourceMap.get(mSurfaceTexture);
        }

        if (resource != null) {
            releaseResourceWhenDetached(resource);
        }
        mSurfaceTexture = null;
        mSurface = null;

        for (Surface surface : mSurfaceToReleaseList) {
            surface.release();
        }
        mSurfaceToReleaseList.clear();
    }

    void releaseResourceWhenDetached(final Resource resource) {
        synchronized (mLock) {
            resource.setReleasing(true);
        }

        setOnSurfaceDetachedListener(CameraXExecutors.mainThreadExecutor(),
                new OnSurfaceDetachedListener() {
                    @Override
                    public void onSurfaceDetached() {
                        List<Resource> resourcesToRelease = new ArrayList<>();

                        synchronized (mLock) {
                            for (Resource resource : mResourceMap.values()) {
                                if (resource.isReleasing()) {
                                    resourcesToRelease.add(resource);
                                }
                            }

                            // Removes the resource from the map since it is of no use.
                            for (Resource resourceToRemove : resourcesToRelease) {
                                mResourceMap.remove(resourceToRemove.mSurfaceTexture);
                            }
                        }

                        for (Resource resource : resourcesToRelease) {
                            resource.release();
                        }
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

    interface OnTextureChangedListener {
        void onTextureChanged(SurfaceTexture newOutput, Size newResolution);
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
