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

package androidx.camera.core;

import android.graphics.SurfaceTexture;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Consumer;

/**
 * Interface to implement a GPU-based post-processing effect.
 *
 * <p>This interface is for implementing a GPU effect for the {@link Preview} and/or
 * {@code VideoCapture} {@link UseCase}. Both the input and the output of the implementation
 * are {@link Surface}s. It's recommended to use graphics API such as OpenGL or Vulkan to access
 * the {@link Surface}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SurfaceEffect extends CameraEffect {

    /**
     * Bitmask option to indicate that CameraX applies this effect to {@link Preview}.
     */
    int PREVIEW = 1;

    /**
     * Bitmask option to indicate that CameraX applies this effect to {@code VideoCapture}.
     */
    int VIDEO_CAPTURE = 1 << 1;

    /**
     * Invoked when CameraX requires an input {@link Surface} for reading original frames.
     *
     * <p>With OpenGL, the implementation should create a {@link Surface} backed by
     * {@link SurfaceTexture} with the size of {@link SurfaceRequest#getResolution()}, then
     * listen for the {@link SurfaceTexture#setOnFrameAvailableListener} to get the incoming
     * frames.
     *
     * <p>The value of the {@link SurfaceTexture#getTransformMatrix} will need an additional
     * transformation. CameraX calculates the additional transformation based on {@link UseCase}
     * configurations such as {@link ViewPort} and target rotation, and provide the value via
     * {@link SurfaceOutput#updateTransformMatrix(float[], float[])}.
     *
     * @param request a request to provide {@link Surface} for input.
     * @see SurfaceRequest
     */
    void onInputSurface(@NonNull SurfaceRequest request);

    /**
     * Invoked when CameraX provides output Surface(s) for drawing processed frames.
     *
     * <p>The provided {@link Surface}s are for drawing processed frames. The implementation must
     * get the {@link Surface} via {@link SurfaceOutput#getSurface} and provide a
     * {@link Consumer<SurfaceOutput.Event>} listening to the end-of-life event of the
     * {@link Surface}. Then, the implementation should call {@link SurfaceOutput#close()} after it
     * stops drawing to the {@link Surface}.
     *
     * <p> When drawing to the {@link Surface}, the implementation should apply an additional
     * transformation to the input {@link Surface} by calling
     * {@link SurfaceOutput#updateTransformMatrix(float[], float[])} with the value of
     * {@link SurfaceTexture#getTransformMatrix(float[])}} from the input {@link Surface}.
     *
     * @param surfaceOutput contains a {@link Surface} for drawing processed frames.
     */
    void onOutputSurface(@NonNull SurfaceOutput surfaceOutput);
}
