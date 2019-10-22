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

package androidx.camera.core;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Helper class to be used with {@link Preview}.
 */
public final class PreviewUtil {

    private static final String TAG = "PreviewUtil";

    private PreviewUtil() {
    }

    /**
     * Creates a {@link Preview.PreviewSurfaceCallback} that allocates and deallocates
     * {@link SurfaceTexture}.
     *
     * @param surfaceTextureCallback listener that will be triggered when the SurfaceTexture is
     *                                    ready.
     * @return {@link Preview.PreviewSurfaceCallback} to be used with {@link Preview}.
     */
    @NonNull
    public static Preview.PreviewSurfaceCallback createPreviewSurfaceCallback(
            @NonNull SurfaceTextureCallback surfaceTextureCallback) {
        return new Preview.PreviewSurfaceCallback() {

            Map<Surface, SurfaceTexture> mSurfaceTextureMap = new HashMap<>();

            @NonNull
            @Override
            public ListenableFuture<Surface> createSurfaceFuture(@NonNull Size resolution,
                    int imageFormat) {
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(),
                        resolution.getHeight());
                surfaceTexture.detachFromGLContext();
                surfaceTextureCallback.onSurfaceTextureReady(surfaceTexture);
                Surface surface = new Surface(surfaceTexture);
                mSurfaceTextureMap.put(surface, surfaceTexture);
                return Futures.immediateFuture(surface);
            }

            @Override
            public void onSafeToRelease(@NonNull ListenableFuture<Surface> surfaceFuture) {
                try {
                    Surface surface = surfaceFuture.get();
                    SurfaceTexture surfaceTexture = mSurfaceTextureMap.get(surface);
                    if (surfaceTexture != null) {
                        surfaceTextureCallback.onSafeToRelease(surfaceTexture);
                    }
                    surface.release();
                } catch (ExecutionException | InterruptedException e) {
                    Log.w(TAG, "Failed to release the Surface.", e);
                }
            }
        };
    }

    /**
     * Callback that is triggered when {@link SurfaceTexture} is ready.
     */
    public interface SurfaceTextureCallback {

        /**
         * Triggered when {@link SurfaceTexture} is ready.
         *
         * @param surfaceTexture {@link SurfaceTexture} created for {@link Preview}.
         */
        void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture);

        /**
         * Called when the {@link SurfaceTexture} is safe to release.
         *
         * <p> This method is called when the {@link SurfaceTexture} previously
         * returned from {@link #onSurfaceTextureReady(SurfaceTexture)} is safe to be released.
         *
         * @param surfaceTexture the {@link SurfaceTexture} to release.
         */
        void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture);
    }
}
