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
import androidx.core.util.Consumer;

/**
 * Interface to implement a GPU-based post-processing effect.
 *
 * <p>This interface is for implementing a GPU effect for the {@link Preview} {@link UseCase}.
 * Both the input and the output of the implementation are {@link Surface}s. It's recommended to
 * use graphics API such as OpenGL or Vulkan to access the {@link Surface}.
 *
 * <p>If the implementation fails to process frames, for example, fails to allocate
 * the resources, it should throw a {@link ProcessingException} in either {@link #onInputSurface} or
 * {@link #onOutputSurface} to notify CameraX. If the implementation encounters an error after the
 * pipeline is running, it should invalidate the input {@link Surface} by calling
 * {@link SurfaceRequest#invalidate()}, then throwing a {@link ProcessingException} when
 * {@link SurfaceProcessor#onInputSurface} is invoked again.
 *
 * <p>Once the implementation throws an exception, CameraX will treat it as an unrecoverable error
 * and abort the pipeline. If the {@link SurfaceOutput#getTargets()} is
 * {@link CameraEffect#PREVIEW}, CameraX will not propagate the error to the app. It's the
 * implementation's responsibility to notify the app. For example:
 *
 * <pre><code>
 * class SurfaceProcessorImpl implements SurfaceProcessor {
 *
 *     Consumer<Exception> mErrorListener;
 *
 *     SurfaceProcessorImpl(@NonNull Consumer<Exception> errorListener) {
 *         mErrorListener = errorListener;
 *     }
 *
 *     void onInputSurface(@NonNull SurfaceRequest request) throws ProcessingException {
 *         try {
 *             // Setup the input stream.
 *         } catch (Exception e) {
 *             // Notify the app before throwing a ProcessingException.
 *             mErrorListener.accept(e)
 *             throw new ProcessingException(e);
 *         }
 *     }
 *
 *     void onOutputSurface(@NonNull SurfaceRequest request) throws ProcessingException {
 *         try {
 *             // Setup the output streams.
 *         } catch (Exception e) {
 *             // Notify the app before throwing a ProcessingException.
 *             mErrorListener.accept(e)
 *             throw new ProcessingException(e);
 *         }
 *     }
 * }
 * </code></pre>
 */
public interface SurfaceProcessor {

    /**
     * Invoked when CameraX requires an input {@link Surface} for reading original frames.
     *
     * <p>CameraX requests {@link Surface}s when the upstream pipeline is reconfigured. For
     * example, when {@link UseCase}s are bound to lifecycle.
     *
     * <p>With OpenGL, the implementation should create a {@link Surface} backed by
     * {@link SurfaceTexture} with the size of {@link SurfaceRequest#getResolution()}, then
     * listen for the {@link SurfaceTexture#setOnFrameAvailableListener} to get the incoming
     * frames. The {@link Surface} should not be released until the callback provided in
     * {@link SurfaceRequest#provideSurface} is invoked. CameraX may request new input
     * {@link Surface} before releasing the existing one.
     *
     * <p>If the implementation encounters errors in creating the input {@link Surface}, it
     * should throw an {@link ProcessingException} to notify CameraX.
     *
     * <p>The implementation can replace a previously provided {@link Surface} by invoking
     * {@link SurfaceRequest#invalidate()}. Once invoked, CameraX will restart the camera
     * pipeline and call {@link #onInputSurface} again with another {@link SurfaceRequest}.
     *
     * <p>The value of the {@link SurfaceTexture#getTransformMatrix} will need an additional
     * transformation. CameraX calculates the additional transformation based on {@link UseCase}
     * configurations such as {@link ViewPort} and target rotation, and provide the value via
     * {@link SurfaceOutput#updateTransformMatrix(float[], float[])}.
     *
     * Code sample:
     * <pre><code>
     * // Create Surface based on the request.
     * SurfaceTexture surfaceTexture = SurfaceTexture(textureName);
     * surfaceTexture.setDefaultBufferSize(request.resolution.width, request.resolution.height);
     * Surface surface = Surface(surfaceTexture);
     *
     * // Provide the Surface to CameraX, and cleanup when it's no longer used.
     * surfaceRequest.provideSurface(surface, executor, result -> {
     *     surfaceTexture.setOnFrameAvailableListener(null)
     *     surfaceTexture.release()
     *     surface.release()
     * });
     *
     * // Listen to the incoming frames.
     * surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> {
     *     // Process the incoming frames and draw to the output Surface from #onOutputSurface
     * }, handler);
     * </code></pre>
     *
     * @param request a request to provide {@link Surface} for input.
     * @throws ProcessingException if the implementation fails to fulfill the
     *                             {@link SurfaceRequest}.
     * @see SurfaceRequest
     */
    void onInputSurface(@NonNull SurfaceRequest request) throws ProcessingException;

    /**
     * Invoked when CameraX provides output Surface(s) for drawing processed frames.
     *
     * <p>CameraX provides the output {@link Surface}s when the downstream pipeline is
     * reconfigured, for example, when {@link UseCase}s are bound or the preview viewfinder is
     * reset.
     *
     * <p>The provided {@link Surface}s are for drawing processed frames. The implementation must
     * get the {@link Surface} via {@link SurfaceOutput#getSurface} and provide a
     * {@link Consumer<SurfaceOutput.Event>} listening to the end-of-life event of the
     * {@link Surface}. Then, the implementation should call {@link SurfaceOutput#close()} after it
     * stops drawing to the {@link Surface}. CameraX may provide new output {@link Surface}
     * before requesting to close the existing one.
     *
     * <p>If the implementation encounters an error and cannot consume the {@link Surface},
     * it should throw an {@link ProcessingException} to notify CameraX.
     *
     * <p>When drawing to the {@link Surface}, the implementation should apply an additional
     * transformation to the input {@link Surface} by calling
     * {@link SurfaceOutput#updateTransformMatrix(float[], float[])} with the value of
     * {@link SurfaceTexture#getTransformMatrix(float[])}} from the input {@link Surface}.
     *
     * @param surfaceOutput contains a {@link Surface} for drawing processed frames.
     * @throws ProcessingException if the implementation fails to consume the {@link SurfaceOutput}.
     * @see SurfaceOutput
     */
    void onOutputSurface(@NonNull SurfaceOutput surfaceOutput) throws ProcessingException;
}
