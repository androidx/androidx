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
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * This class creates implementations of PreviewSurfaceCallback that provide Surfaces that have been
 * pre-configured for specific work flows.
 */
public final class PreviewSurfaceProviders {

    private static final String TAG = "PreviewUtil";

    private PreviewSurfaceProviders() {
    }

    /**
     * Creates a {@link Preview.PreviewSurfaceCallback} that is backed by a {@link SurfaceTexture}.
     *
     * <p>This is a convenience method for creating a {@link Preview.PreviewSurfaceCallback}
     * whose {@link Surface} is backed by a {@link SurfaceTexture}. The returned
     * {@link Preview.PreviewSurfaceCallback} is responsible for creating the {@link SurfaceTexture}
     * and propagating {@link Preview.PreviewSurfaceCallback#onSafeToRelease(ListenableFuture)}
     * back to the implementer. The {@link SurfaceTexture} is usually used with a
     * {@link TextureView}.
     * Example:
     *
     * <pre><code>
     * preview.setPreviewSurfaceCallback(createPreviewSurfaceCallback(
     *         new PreviewUtil.SurfaceTextureCallback() {
     *             &#64;Override
     *             public void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture) {
     *                 // Maybe remove and re-add the TextureView to its parent.
     *                 textureView.setSurfaceTexture(surfaceTexture);
     *             }
     *
     *             &#64;Override
     *             public void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture) {
     *                 surfaceTexture.release();
     *             }
     *         }));
     * </code></pre>
     *
     * <p> Note that the TextureView needs to be removed and re-added from the parent view for the
     * SurfaceTexture to be attached, because TextureView's existing SurfaceTexture is only
     * correctly detached once the parent TextureView is removed from the view hierarchy.
     *
     * @param surfaceTextureCallback callback called when the SurfaceTexture is ready to be
     *                               set/released.
     * @return a {@link Preview.PreviewSurfaceCallback} to be used with
     * {@link Preview#setPreviewSurfaceCallback(Preview.PreviewSurfaceCallback)}.
     */
    @NonNull
    public static Preview.PreviewSurfaceCallback createSurfaceTextureProvider(
            @NonNull SurfaceTextureCallback surfaceTextureCallback) {
        return new Preview.PreviewSurfaceCallback() {

            Map<Surface, SurfaceTexture> mSurfaceTextureMap = new HashMap<>();

            @NonNull
            @Override
            public ListenableFuture<Surface> createSurfaceFuture(@NonNull Size resolution) {
                SurfaceTexture surfaceTexture = new SurfaceTexture(0);
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(),
                        resolution.getHeight());
                surfaceTexture.detachFromGLContext();
                surfaceTextureCallback.onSurfaceTextureReady(surfaceTexture, resolution);
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
                        mSurfaceTextureMap.remove(surface);
                    }
                    surface.release();
                } catch (ExecutionException | InterruptedException e) {
                    Log.w(TAG, "Failed to release the Surface.", e);
                }
            }
        };
    }

    /**
     * Callback that is called when the {@link SurfaceTexture} is ready to be set/released.
     *
     * <p> Implement this interface to receive the updates on  {@link SurfaceTexture} used in
     * {@link Preview}. See {@link #createSurfaceTextureProvider(SurfaceTextureCallback)} for
     * code example.
     */
    public interface SurfaceTextureCallback {

        /**
         * Called when {@link SurfaceTexture} is ready to be set.
         *
         * <p> This is called when the preview {@link SurfaceTexture} is created and ready. The
         * most common usage is to set it to a {@link TextureView}. Example:
         * <pre><code>textureView.setSurfaceTexture(surfaceTexture)</code></pre>.
         *
         * <p> To display the {@link SurfaceTexture} without a {@link TextureView},
         * {@link SurfaceTexture#getTransformMatrix(float[])} can be used to transform the
         * preview to natural orientation. For {@link TextureView}, it handles the transformation
         * automatically so that no additional work is needed.
         *
         * @param surfaceTexture {@link SurfaceTexture} created for {@link Preview}.
         * @param resolution     the resolution of the created {@link SurfaceTexture}.
         */
        void onSurfaceTextureReady(@NonNull SurfaceTexture surfaceTexture,
                @NonNull Size resolution);

        /**
         * Called when the {@link SurfaceTexture} is safe to be released.
         *
         * <p> This method is called when the {@link SurfaceTexture} previously provided in
         * {@link #onSurfaceTextureReady(SurfaceTexture, Size)} is no longer being used by the
         * camera system, and it's safe to be released during or after this is called. The
         * implementer is responsible to release the {@link SurfaceTexture} when it's also no
         * longer being used by the app.
         *
         * @param surfaceTexture the {@link SurfaceTexture} to be released.
         */
        void onSafeToRelease(@NonNull SurfaceTexture surfaceTexture);
    }
}
