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

package androidx.camera.core;

import androidx.annotation.NonNull;

import java.util.Set;

/**
 * ImageCaptureCapabilities is used to query {@link ImageCapture} use case capabilities on the
 * device.
 */
public interface ImageCaptureCapabilities {
    /**
     * Returns if the takePicture() call in {@link ImageCapture} is capable of outputting
     * postview images.
     *
     * <p>A postview image is a low-quality image that's produced earlier during image capture
     * than the final high-quality image, and can be used as a thumbnail or placeholder until the
     * final image is ready.
     *
     * If supported, apps can enable the postview using
     * {@link ImageCapture.Builder#setPostviewEnabled(boolean)}.
     */
    boolean isPostviewSupported();

    /**
     * Returns if the takePicture() call in {@link ImageCapture} is capable of notifying the
     * {@link ImageCapture.OnImageSavedCallback#onCaptureProcessProgressed(int)} or
     * {@link ImageCapture.OnImageCapturedCallback#onCaptureProcessProgressed(int)} callback to
     * the apps.
     */
    boolean isCaptureProcessProgressSupported();

    /**
     * Gets the supported {@link ImageCapture.OutputFormat} set.
     *
     * <p>The set returned will always contain {@link ImageCapture.OutputFormat#OUTPUT_FORMAT_JPEG}
     * format, support for other formats will vary by camera.
     *
     * @return a set of supported output formats.
     *
     * @see ImageCapture.Builder#setOutputFormat(int)
     */
    @ExperimentalImageCaptureOutputFormat
    @NonNull
    Set<@ImageCapture.OutputFormat Integer> getSupportedOutputFormats();
}
