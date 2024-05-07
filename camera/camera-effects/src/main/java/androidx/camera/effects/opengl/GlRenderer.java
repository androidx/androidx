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

import static androidx.camera.effects.opengl.Utils.checkGlErrorOrThrow;
import static androidx.camera.effects.opengl.Utils.configureExternalTexture;
import static androidx.camera.effects.opengl.Utils.configureTexture2D;
import static androidx.camera.effects.opengl.Utils.createTextureId;
import static androidx.core.util.Preconditions.checkState;

import android.graphics.Bitmap;
import android.opengl.GLES11Ext;
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
 * <p>It also allows the caller to overlay a texture when rendering to Surface.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class GlRenderer {

    private static final String TAG = "GlRenderer";

    private boolean mInitialized = false;

    private Thread mGlThread = null;
    private final GlContext mGlContext = new GlContext();
    private final int mQueueDepth;
    private final GlProgramOverlay mGlProgramOverlay;
    private final GlProgramCopy mGlProgramCopy = new GlProgramCopy();

    // Texture IDs.
    private int mInputTextureId = -1;
    private int mOverlayTextureId = -1;
    @NonNull
    private int[] mQueueTextureIds = new int[0];
    private int mQueueTextureWidth = -1;
    private int mQueueTextureHeight = -1;

    // --- Public methods ---

    public GlRenderer(int queueDepth) {
        mQueueDepth = queueDepth;
        mGlProgramOverlay = new GlProgramOverlay(queueDepth);
    }

    /**
     * Initializes the renderer.
     *
     * <p>Must be called before any other methods.
     */
    public void init() {
        checkState(!mInitialized, "Already initialized");
        mInitialized = true;
        mGlThread = Thread.currentThread();
        try {
            mGlContext.init();
            mGlProgramCopy.init();
            mGlProgramOverlay.init();
            mInputTextureId = createTextureId();
            configureExternalTexture(mInputTextureId);
            mOverlayTextureId = createTextureId();
            configureExternalTexture(mOverlayTextureId);
        } catch (IllegalStateException | IllegalArgumentException e) {
            release();
            throw e;
        }
    }

    /**
     * Releases the renderer.
     *
     * <p>Once released, it can never be accessed again.
     */
    public void release() {
        checkGlThreadAndInitialized();

        mInitialized = false;
        mGlThread = null;
        mQueueTextureWidth = -1;
        mQueueTextureHeight = -1;

        mGlContext.release();
        mGlProgramOverlay.release();
        mGlProgramCopy.release();

        if (mInputTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{mInputTextureId}, 0);
            checkGlErrorOrThrow("glDeleteTextures");
            mInputTextureId = -1;
        }
        if (mOverlayTextureId != -1) {
            GLES20.glDeleteTextures(1, new int[]{mOverlayTextureId}, 0);
            checkGlErrorOrThrow("glDeleteTextures");
            mOverlayTextureId = -1;
        }
        if (mQueueTextureIds.length > 0) {
            GLES20.glDeleteTextures(mQueueTextureIds.length, mQueueTextureIds, 0);
            checkGlErrorOrThrow("glDeleteTextures");
            mQueueTextureIds = new int[0];
        }
    }

    /**
     * Gets the external input texture ID created during initialization.
     */
    public int getInputTextureId() {
        checkGlThreadAndInitialized();
        return mInputTextureId;
    }

    /**
     * Gets the external overlay texture ID created during initialization.
     */
    public int getOverlayTextureId() {
        checkGlThreadAndInitialized();
        return mOverlayTextureId;
    }

    /**
     * Creates an array of textures and return.
     *
     * <p>This method creates an array of {@link GLES20#GL_TEXTURE_2D} textures with the
     * {@link #mQueueDepth} from constructor, and return their IDs. If the array already exists,
     * calling this method deletes the current array before creating a new one.
     *
     * @param size the size of the texture in this queue. The size usually matches the size
     *             of the input texture.
     */
    @NonNull
    public int[] createBufferTextureIds(@NonNull Size size) {
        checkGlThreadAndInitialized();
        // Delete the current buffer if it exists.
        if (mQueueTextureIds.length > 0) {
            GLES20.glDeleteTextures(mQueueTextureIds.length, mQueueTextureIds, 0);
            checkGlErrorOrThrow("glDeleteTextures");
        }

        mQueueTextureIds = new int[mQueueDepth];
        // If the queue depth is 0, return an empty array. There is no need to create textures.
        if (mQueueDepth == 0) {
            return mQueueTextureIds;
        }

        // Create the textures.
        GLES20.glGenTextures(mQueueDepth, mQueueTextureIds, 0);
        checkGlErrorOrThrow("glGenTextures");
        mQueueTextureWidth = size.getWidth();
        mQueueTextureHeight = size.getHeight();
        for (int textureId : mQueueTextureIds) {
            configureTexture2D(textureId);
            GLES20.glTexImage2D(
                    GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, size.getWidth(), size.getHeight(), 0,
                    GLES20.GL_RGB,
                    GLES20.GL_UNSIGNED_BYTE,
                    null
            );
        }
        return mQueueTextureIds;
    }

    /**
     * Registers an output surface.
     *
     * <p>Once registered, the {@link Surface} can be used by {@link #renderInputToSurface} and
     * {@link #renderQueueTextureToSurface}.
     */
    public void registerOutputSurface(@NonNull Surface surface) {
        checkGlThreadAndInitialized();
        mGlContext.registerSurface(surface);
    }

    /**
     * Unregisters the output surface.
     *
     * <p>Once unregistered, calling {@link #renderInputToSurface} or
     * {@link #renderQueueTextureToSurface} with the {@link Surface} throws an exception.
     */
    public void unregisterOutputSurface(@NonNull Surface surface) {
        checkGlThreadAndInitialized();
        mGlContext.unregisterSurface(surface);
    }

    /**
     * Renders the input texture directly to the output surface.
     *
     * <p>This is used when the queue depth is 0 and no buffer copy is needed. The surface must
     * be registered via {@link #registerOutputSurface}.
     */
    public void renderInputToSurface(long timestampNs, @NonNull float[] textureTransform,
            @NonNull Surface surface) {
        checkGlThreadAndInitialized();
        checkState(mQueueDepth == 0, "Queue depth must be zero");
        mGlProgramOverlay.draw(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mInputTextureId,
                mOverlayTextureId, textureTransform, mGlContext, surface, timestampNs);
    }

    /**
     * Renders a queued texture to the output surface.
     *
     * <p>Draw the content of the texture to the output surface. The surface must be registered via
     * {@link #registerOutputSurface}. The texture ID must be from the latest return value of
     * {@link #createBufferTextureIds}.
     */
    public void renderQueueTextureToSurface(int textureId, long timestampNs,
            @NonNull float[] textureTransform, @NonNull Surface surface) {
        checkGlThreadAndInitialized();
        checkState(mQueueDepth > 0, "Queue depth must be non-zero");
        mGlProgramOverlay.draw(GLES20.GL_TEXTURE_2D, textureId, mOverlayTextureId,
                textureTransform, mGlContext, surface, timestampNs);
    }

    /**
     * Renders the input texture to a texture in the queue.
     *
     * <p>The texture ID must be from the latest return value of{@link #createBufferTextureIds}.
     */
    public void renderInputToQueueTexture(int textureId) {
        checkGlThreadAndInitialized();
        mGlProgramCopy.draw(mInputTextureId, textureId, mQueueTextureWidth, mQueueTextureHeight);
    }

    /**
     * Renders a queued texture to a Bitmap and returns.
     */
    @NonNull
    public Bitmap renderQueueTextureToBitmap(int textureId, int width, int height,
            @NonNull float[] textureTransform) {
        checkGlThreadAndInitialized();
        checkState(mQueueDepth > 0, "Queue depth must be non-zero");
        return mGlProgramOverlay.snapshot(GLES20.GL_TEXTURE_2D, textureId, mOverlayTextureId,
                width, height, textureTransform);
    }

    /**
     * Renders the input texture to a Bitmap and returns.
     */
    @NonNull
    public Bitmap renderInputToBitmap(int width, int height, @NonNull float[] textureTransform) {
        checkGlThreadAndInitialized();
        checkState(mQueueDepth == 0, "Queue depth must be zero");
        return mGlProgramOverlay.snapshot(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mInputTextureId,
                mOverlayTextureId, width, height, textureTransform);
    }

    // --- Private methods ---

    private void checkGlThreadAndInitialized() {
        checkState(mInitialized, "OpenGlRenderer is not initialized");
        checkState(mGlThread == Thread.currentThread(),
                "Method call must be called on the GL thread.");
    }
}
