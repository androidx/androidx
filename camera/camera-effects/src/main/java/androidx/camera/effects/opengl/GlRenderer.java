/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.camera.effects.opengl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * A OpenGL render with a buffer queue and overlay support.
 *
 * <p>It allows 3 types of rendering:
 * <ul>
 * <li>Rendering the input texture directly to the output Surface,
 * <li>Rendering the input texture to a texture in the queue, and
 * <li>Rendering a texture in the queue to the output Surface.
 * </ul>
 *
 * <p>It also allows the caller to upload a bitmap and overlay it when rendering to Surface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class GlRenderer {

    // --- Public methods ---

    /**
     * Initializes the renderer.
     *
     * <p>Must be called before any other methods.
     */
    void init() {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Releases the renderer.
     *
     * <p>Once released, it can never be accessed again.
     */
    void release() {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Gets the external input texture ID created during initialization.
     */
    public int getInputTextureId() {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Creates an array of textures and return.
     *
     * <p>This method creates an array of {@link GLES20#GL_TEXTURE_2D} textures and return their
     * IDs. If the array already exists, calling this method deletes the current array before
     * creating a new one.
     *
     * @param queueDepth the depth of the queue
     * @param size       the size of the texture in this queue. The size usually matches the size
     *                   of the input texture.
     */
    @NonNull
    public int[] createBufferTextureIds(int queueDepth, @NonNull Size size) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Uploads the {@link Bitmap} to the overlay texture.
     */
    public void uploadOverlay(@NonNull Bitmap overlay) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Registers an output surface.
     *
     * <p>Once registered, the {@link Surface} can be used by {@link #renderInputToSurface} and
     * {@link #renderQueueTextureToSurface}.
     */
    public void registerOutputSurface(@NonNull Surface surface) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Unregisters the output surface.
     *
     * <p>Once unregistered, calling {@link #renderInputToSurface} or
     * {@link #renderQueueTextureToSurface} with the {@link Surface} throws an exception.
     */
    public void unregisterOutputSurface(@NonNull Surface surface) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Renders the input texture directly to the output surface.
     *
     * <p>This is used when the queue depth is 0 and no buffer copy is needed. The surface must
     * be registered via {@link #registerOutputSurface}.
     */
    public void renderInputToSurface(long timestampNs, @NonNull float[] textureTransform,
            @NonNull Surface surface) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Renders a queued texture to the output surface.
     *
     * <p>Draw the content of the texture to the output surface. The surface must be registered via
     * {@link #registerOutputSurface}. The texture ID must be from the latest return value of
     * {@link #createBufferTextureIds}.
     */
    public void renderQueueTextureToSurface(int textureId, long timestampNs,
            @NonNull float[] textureTransform,
            @NonNull Surface surface) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    /**
     * Renders the input texture to a texture in the queue.
     *
     * <p>The texture ID must be from the latest return value of{@link #createBufferTextureIds}.
     */
    public void renderInputToQueueTexture(int textureId) {
        throw new UnsupportedOperationException("TODO: implement this");
    }

    // --- Private methods ---
}
