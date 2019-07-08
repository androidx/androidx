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
import android.os.Build.VERSION_CODES;
import android.util.Size;

import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

/**
 * An implementation of {@link SurfaceTexture} with a fixed default buffer size.
 *
 * <p>The fixed default buffer size used at construction time cannot be changed through the {@link
 * #setDefaultBufferSize(int, int)} method.
 */
final class FixedSizeSurfaceTexture extends SurfaceTexture {

    private final Owner mOwner;

    // For testing purpose, cannot find a better way to verify super.release() is called.
    // SurfaceTexture.isRelease() is not a good way to check since it is not supported in some
    // api levels.
    @VisibleForTesting
    boolean mIsSuperReleased = false;

    private static final Owner SELF_OWNER = new Owner() {
        @Override
        public boolean requestRelease() {
            return true;
        }
    };

    /**
     * Construct a new SurfaceTexture to stream images to a given OpenGL texture.
     *
     * @param texName   the OpenGL texture object name (e.g. generated via glGenTextures)
     * @param fixedSize the fixed default buffer size
     * @throws android.view.Surface.OutOfResourcesException If the SurfaceTexture cannot be created.
     */
    FixedSizeSurfaceTexture(int texName, Size fixedSize) {
        this(texName, fixedSize, SELF_OWNER);
    }

    /**
     * Construct a new SurfaceTexture to stream images to a given OpenGL texture.
     *
     * @param texName   the OpenGL texture object name (e.g. generated via glGenTextures)
     * @param fixedSize the fixed default buffer size
     * @param owner     the {@link Owner} which owns this instance.
     * @throws android.view.Surface.OutOfResourcesException If the SurfaceTexture cannot be created.
     */
    FixedSizeSurfaceTexture(int texName, Size fixedSize, Owner owner) {
        super(texName);
        super.setDefaultBufferSize(fixedSize.getWidth(), fixedSize.getHeight());
        mOwner = owner;
    }

    /**
     * Construct a new SurfaceTexture to stream images to a given OpenGL texture.
     *
     * <p>In single buffered mode the application is responsible for serializing access to the image
     * content buffer. Each time the image content is to be updated, the {@link #releaseTexImage()}
     * method must be called before the image content producer takes ownership of the buffer. For
     * example, when producing image content with the NDK ANativeWindow_lock and
     * ANativeWindow_unlockAndPost functions, {@link #releaseTexImage()} must be called before each
     * ANativeWindow_lock, or that call will fail. When producing image content with OpenGL ES,
     * {@link #releaseTexImage()} must be called before the first OpenGL ES function call each
     * frame.
     *
     * @param texName          the OpenGL texture object name (e.g. generated via glGenTextures)
     * @param singleBufferMode whether the SurfaceTexture will be in single buffered mode.
     * @param fixedSize        the fixed default buffer size
     * @throws android.view.Surface.OutOfResourcesException If the SurfaceTexture cannot be created.
     */
    FixedSizeSurfaceTexture(int texName, boolean singleBufferMode, Size fixedSize) {
        super(texName, singleBufferMode);
        super.setDefaultBufferSize(fixedSize.getWidth(), fixedSize.getHeight());
        mOwner = SELF_OWNER;
    }

    /**
     * Construct a new SurfaceTexture to stream images to a given OpenGL texture.
     *
     * <p>In single buffered mode the application is responsible for serializing access to the image
     * content buffer. Each time the image content is to be updated, the {@link #releaseTexImage()}
     * method must be called before the image content producer takes ownership of the buffer. For
     * example, when producing image content with the NDK ANativeWindow_lock and
     * ANativeWindow_unlockAndPost functions, {@link #releaseTexImage()} must be called before each
     * ANativeWindow_lock, or that call will fail. When producing image content with OpenGL ES,
     * {@link #releaseTexImage()} must be called before the first OpenGL ES function call each
     * frame.
     *
     * <p>Unlike {@link SurfaceTexture(int, boolean)}, which takes an OpenGL texture object name,
     * this constructor creates the SurfaceTexture in detached mode. A texture name must be passed
     * in using {@link #attachToGLContext} before calling {@link #releaseTexImage()} and producing
     * image content using OpenGL ES.
     *
     * @param singleBufferMode whether the SurfaceTexture will be in single buffered mode.
     * @param fixedSize        the fixed default buffer size
     * @throws android.view.Surface.OutOfResourcesException If the SurfaceTexture cannot be created.
     */
    @RequiresApi(api = VERSION_CODES.O)
    FixedSizeSurfaceTexture(boolean singleBufferMode, Size fixedSize) {
        super(singleBufferMode);
        super.setDefaultBufferSize(fixedSize.getWidth(), fixedSize.getHeight());
        mOwner = SELF_OWNER;
    }

    /**
     * This method has no effect.
     *
     * <p>Unlike {@link SurfaceTexture}, this method does not affect the default buffer size. The
     * default buffer size will remain what it was set to during construction.
     *
     * @param width  ignored width
     * @param height ignored height
     */
    @Override
    public void setDefaultBufferSize(int width, int height) {
        // No-op
    }

    /*
     * Overrides the release() to request Owner's permission before releasing it.
     */
    @Override
    public void release() {
        if (mOwner.requestRelease()) {
            super.release();
            mIsSuperReleased = true;
        }
    }

    /**
     * An interface for specifying the ownership of a resource.
     *
     * It is used in condition that some resource cannot be released by itself and would like to be
     * controlled by a OWNER. The resource can only be release()'d when Owner's requestRelease
     * returns
     * true.
     */
    interface Owner {
        /** request release permission from owner */
        boolean requestRelease();
    }


}
