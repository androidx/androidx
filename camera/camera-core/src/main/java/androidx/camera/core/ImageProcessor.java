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

import android.graphics.PixelFormat;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Interface for injecting a {@link ImageProxy} effect into CameraX.
 *
 * <p>Implement {@link ImageProcessor} to inject an effect into CameraX pipeline. For example, to
 * edit the {@link ImageCapture} result, add a {@link CameraEffect} with the
 * {@link ImageProcessor} targeting {@link CameraEffect#IMAGE_CAPTURE}. Once injected,
 * {@link ImageCapture} forwards camera frames to the implementation, and delivers the processed
 * frames to the app.
 *
 * <p>Code sample for creating a {@link ImageCapture} object:
 * <pre><code>
 * class ImageEffect implements CameraEffect {
 *     ImageEffect(Executor executor, ImageProcessor imageProcessorImpl) {
 *         super(IMAGE_CAPTURE, executor, imageProcessorImpl);
 *     }
 * }
 * </code></pre>
 *
 * <p>Code sample for injecting the effect into CameraX pipeline:
 * <pre><code>
 * UseCaseGroup useCaseGroup = UseCaseGroup.Builder()
 *         .addUseCase(imageCapture)
 *         .addEffect(new ImageEffect())
 *         .build();
 * cameraProvider.bindToLifecycle(lifecycleOwner, cameraFilter, useCaseGroup);
 * </code></pre>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ImageProcessor {

    /**
     * Accepts original frames from CameraX and returns processed frames.
     *
     * <p>CameraX invokes this method for each batch of images from the camera. It's invoked on the
     * {@link Executor} provided in {@link CameraEffect}'s constructor. It might be called in
     * parallel, should the {@link Executor} allow multi-threading. The implementation must block
     * the current calling thread until the output image is returned.
     *
     * <p>The implementation must follow the instruction in the {@link Request} to process the
     * image. For example, it must produce an output image with the format following the JavaDoc of
     * {@link Request#getInputImages()}. Failing to do so might cause the processing to
     * fail. For example, for {@link ImageCapture}, when the processing fails, the app will
     * receive a {@link ImageCapture.OnImageSavedCallback#onError} or
     * {@link ImageCapture.OnImageCapturedCallback#onError} callback.
     *
     * <p>The implementation should throw exceptions if it runs into any unrecoverable errors.
     * CameraX will catch the error and deliver it to the app via the error callbacks.
     *
     * @param request a {@link Request} that contains original images.
     * @return a {@link Response} that contains processed image.
     */
    @NonNull
    Response process(@NonNull Request request);

    /**
     * A request for processing one or many {@link ImageProxy}.
     */
    interface Request {

        /**
         * Gets the input images from Camera.
         *
         * <p>It may return a single image captured by the camera, or multiple images from a
         * burst of capture depending on the configuration in {@link CameraEffect}.
         *
         * <p>Currently this method only returns a single image.
         *
         * @return one or many input images.
         */
        @NonNull
        @AnyThread
        List<ImageProxy> getInputImages();

        /**
         * Gets the output image format.
         *
         * <p>The {@link Response}'s {@link ImageProxy} must follow the instruction in this
         * JavaDoc, or CameraX may throw error.
         *
         * <p>For {@link PixelFormat#RGBA_8888}, the output image must contain a single plane
         * with a pixel stride of 4 and a row stride of width * 4. e.g. each pixel is stored on 4
         * bytes and each RGBA channel is stored with 8 bits of precision. For more details, see the
         * JavaDoc of {@code Bitmap.Config#ARGB_8888}.
         *
         * <p>Currently this method only returns {@link PixelFormat#RGBA_8888}.
         */
        @AnyThread
        int getOutputFormat();
    }

    /**
     * A response for injecting an {@link ImageProxy} back to CameraX.
     */
    interface Response {

        /**
         * Gets the output image of the {@link ImageProcessor}
         *
         * <p>{@link ImageProcessor} should implement the {@link ImageProxy} and
         * {@link ImageProxy.PlaneProxy} interfaces to create the {@link ImageProxy} instance.
         * CameraX will inject the image back to the processing pipeline.
         *
         * <p>The {@link ImageProxy} must follow the instruction in the request, or CameraX may
         * throw error. For example, the format must match the value of
         * {@link Request#getOutputFormat()}, and the pixel stride must match the description for
         * that format in {@link Request#getOutputFormat()}'s JavaDoc.
         *
         * @return the output image.
         */
        @Nullable
        @AnyThread
        ImageProxy getOutputImage();
    }
}
