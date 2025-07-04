/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core.featuregroup.impl.feature

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.featuregroup.GroupableFeature

/**
 * Denotes the image format applied to [ImageCapture] use case.
 *
 * This feature can not be instantiated directly for usage, instead use the
 * [GroupableFeature.IMAGE_ULTRA_HDR] object.
 */
@OptIn(ExperimentalSessionConfig::class)
internal class ImageFormatFeature(val imageCaptureOutputFormat: Int) : GroupableFeature() {
    override val featureTypeInternal: FeatureTypeInternal = FeatureTypeInternal.IMAGE_FORMAT

    override fun toString(): String {
        return "ImageFormatFeature(imageCaptureOutputFormat=${getOutputFormatLabel()})"
    }

    private fun getOutputFormatLabel(): String {
        return when (imageCaptureOutputFormat) {
            ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR -> "JPEG_R"
            ImageCapture.OUTPUT_FORMAT_JPEG -> "JPEG"
            else -> "UNDEFINED($imageCaptureOutputFormat)"
        }
    }

    companion object {
        const val DEFAULT_IMAGE_CAPTURE_OUTPUT_FORMAT = ImageCapture.OUTPUT_FORMAT_JPEG
    }
}
