/*
 * Copyright 2024 The Android Open Source Project
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

/**
 * ImageAnalysisCapabilities is used to query {@link ImageAnalysis} use case capabilities on the
 * device.
 */
public interface ImageAnalysisCapabilities {
    /**
     * Returns whether the given output image format is supported.
     *
     * <p>The possible values are {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_YUV_420_888},
     * {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_RGBA_8888},
     * {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_NV21}, or
     * {@link ImageAnalysis#OUTPUT_IMAGE_FORMAT_PRIVATE}.
     *
     * @param format the output image format.
     * @return true if the output image format is supported.
     *
     * @see ImageAnalysis.Builder#setOutputImageFormat(int)
     */
    boolean isOutputFormatSupported(@ImageAnalysis.OutputImageFormat int format);
}
