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
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.nio.IntBuffer;

/**
 * A {@link DeferrableSurface} which verifies the {@link SurfaceTexture} that backs the {@link
 * Surface} is unreleased before returning the Surface.
 */
final class CheckedSurfaceTexture extends DeferrableSurface {
    private final OnTextureChangedListener mOutputChangedListener;
    private final Handler mMainThreadHandler;
    @Nullable
    SurfaceTexture mSurfaceTexture;
    @Nullable
    Surface mSurface;
    @Nullable
    private Size mResolution;
    CheckedSurfaceTexture(
            OnTextureChangedListener outputChangedListener, Handler mainThreadHandler) {
        mOutputChangedListener = outputChangedListener;
        mMainThreadHandler = mainThreadHandler;
    }

    private static SurfaceTexture createDetachedSurfaceTexture(Size resolution) {
        IntBuffer buffer = IntBuffer.allocate(1);
        GLES20.glGenTextures(1, buffer);
        SurfaceTexture surfaceTexture = new FixedSizeSurfaceTexture(buffer.get(), resolution);
        surfaceTexture.detachFromGLContext();
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
        mSurface = new Surface(mSurfaceTexture);
        mOutputChangedListener.onTextureChanged(mSurfaceTexture, mResolution);
    }

    boolean surfaceTextureReleased(SurfaceTexture surfaceTexture) {
        boolean released = false;

        // TODO(b/121196683) Refactor workaround into a compatibility module
        if (26 <= android.os.Build.VERSION.SDK_INT) {
            released = surfaceTexture.isReleased();
        } else {
            // WARNING: This relies on some implementation details of the ViewFinderOutput native
            // code.
            // If the ViewFinderOutput is released, we should get a RuntimeException. If not, we
            // should
            // get an IllegalStateException since we are not in the same EGL context as the
            // consumer.
            Exception exception = null;
            try {
                // TODO(b/121198329) Make sure updateTexImage() isn't called on consumer EGL context
                surfaceTexture.updateTexImage();
            } catch (IllegalStateException e) {
                exception = e;
                released = false;
            } catch (RuntimeException e) {
                exception = e;
                released = true;
            }

            if (!released && exception == null) {
                throw new RuntimeException("Unable to determine if ViewFinderOutput is released");
            }
        }

        return released;
    }

    /**
     * Returns the {@link Surface} that is backed by a {@link SurfaceTexture}.
     *
     * <p>If the {@link SurfaceTexture} has already been released then the surface will be reset
     * using a new {@link SurfaceTexture}.
     */
    @Override
    public ListenableFuture<Surface> getSurface() {
        final ResolvableFuture<Surface> deferredSurface = ResolvableFuture.create();
        Runnable checkAndSetRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        if (CheckedSurfaceTexture.this.surfaceTextureReleased(mSurfaceTexture)) {
                            // Reset the surface texture and notify the listener
                            CheckedSurfaceTexture.this.resetSurfaceTexture();
                        }

                        deferredSurface.set(mSurface);
                    }
                };

        if (Looper.myLooper() == mMainThreadHandler.getLooper()) {
            checkAndSetRunnable.run();
        } else {
            mMainThreadHandler.post(checkAndSetRunnable);
        }

        return deferredSurface;
    }

    void release() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
    }

    interface OnTextureChangedListener {
        void onTextureChanged(SurfaceTexture newOutput, Size newResolution);
    }
}
