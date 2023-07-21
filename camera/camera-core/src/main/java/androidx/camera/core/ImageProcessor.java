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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * Post-processing effect for images.
 *
 * <p>This interface is for post-processing images. The input is an image from the camera, with the
 * instructions on how to process it; the output is a processed image. CameraX forwards images to
 * the implementation, and delivers the processed images to back the app.
 *
 * <p>Currently, it can only be used with the {@link ImageCapture} by targeting
 * {@link CameraEffect#IMAGE_CAPTURE}.
 *
 * <p>If the implementation fails to process the input, it should throw
 * {@link ProcessingException}. The error will be caught by CameraX and propagated to the app via
 * error callbacks such as {@link ImageCapture.OnImageSavedCallback#onError} or
 * {@link ImageCapture.OnImageCapturedCallback#onError}.
 *
 * <p>Code sample:
 * <pre><code>
 *  class ImageProcessorImpl implements ImageProcessor {
 *      Response process(Request request) throws ProcessingException {
 *          try {
 *              ImageProxy image = request.getInputImages().get(0);
 *              ByteBuffer byteBuffer = image.getPlanes()[0];
 *              // Process the content of byteBuffer and create a Response object.
 *          } catch(Exception e) {
 *              throws new ProcessingException(e);
 *          }
 *      }
 *  }
 * </code></pre>
 *
 * @see CameraEffect
 */
public interface ImageProcessor {

    /**
     * Accepts original image from CameraX and returns processed image.
     *
     * <p>CameraX invokes this method for each new incoming image. It's invoked on the
     * {@link Executor} provided in {@link CameraEffect}'s constructor. It might be called in
     * parallel, should the {@link Executor} allow multi-threading. The implementation must block
     * the current calling thread until the output image is returned.
     *
     * <p>The implementation must follow the instruction in the {@link Request} to process the
     * input image. For example, it must produce an output image with the format following the
     * JavaDoc of {@link Request#getInputImage()}. Failing to do so might cause the processing to
     * fail. For example, for {@link ImageCapture}, it will cause the
     * {@link ImageCapture#takePicture} call to fail.
     *
     * <p>The implementation must throw a {@link ProcessingException} if it fails to process the
     * {@link Request}. CameraX will catch the error and deliver it to the app via error
     * callbacks. For {@link ImageCapture}, the error callbacks are
     * {@link ImageCapture.OnImageCapturedCallback#onError} or
     * {@link ImageCapture.OnImageSavedCallback#onError}.
     *
     * @param request a {@link Request} that contains the original image.
     * @return a {@link Response} that contains the processed image.
     * @throws ProcessingException if the implementation fails to process the {@link Request}.
     */
    @NonNull
    Response process(@NonNull Request request) throws ProcessingException;

    /**
     * Valid output formats.
     *
     * <p>{@link Request#getOutputFormat()} can only return the formats defined by this annotation.
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(value = {PixelFormat.RGBA_8888})
    @interface OutputFormats {
    }

    /**
     * A request for processing one or multiple {@link ImageProxy}.
     */
    interface Request {

        /**
         * Gets the input images.
         *
         * <p>Return a single image captured by the camera. The implementation should check the
         * format of the image before processing it. For example, checking the value of
         * {@link ImageProxy#getFormat()}, {@link ImageProxy.PlaneProxy#getRowStride()} and/or
         * {@link ImageProxy.PlaneProxy#getPixelStride()}.
         *
         * <p>Currently, the image format is always {@link PixelFormat#RGBA_8888} with pixel
         * stride equals to 4 and row stride equals to width * 4.
         */
        @NonNull
        ImageProxy getInputImage();

        /**
         * Gets the output image format.
         *
         * <p>The return value will one of the values in the table. The implementation must
         * create the {@link Response} {@link ImageProxy} following the corresponding
         * instruction, or the processing may fail.
         *
         * <table>
         * <tr>
         *     <th>Value</th>
         *     <th>Instruction</th>
         * </tr>
         * <tr>
         *     <td>{@link PixelFormat#RGBA_8888}</td>
         *     <td>The output image must contain a single plane with a pixel stride of 4 and a
         *     row stride of width * 4. e.g. each pixel is stored on 4 bytes and each RGBA
         *     channel is stored with 8 bits of precision. For more details, see the JavaDoc of
         *     {@code Bitmap.Config#ARGB_8888}.</td>
         * </tr>
         * </table>
         */
        @OutputFormats
        int getOutputFormat();
    }

    /**
     * A response for returning a processed {@link ImageProxy} to CameraX.
     */
    interface Response {

        /**
         * Gets the output image returned by the {@link ImageProcessor}.
         *
         * <p>{@link ImageProcessor} should implement the {@link ImageProxy} and
         * {@link ImageProxy.PlaneProxy} interfaces to create the return value. Once
         * return, CameraX will inject the image back to the processing pipeline.
         *
         * <p>The {@link ImageProxy} must follow the instruction in the request, or CameraX may
         * throw error. For example, the image format must match the description of the
         * {@link Request#getOutputFormat()}.
         *
         * @return the output image.
         */
        @NonNull
        ImageProxy getOutputImage();
    }
}
