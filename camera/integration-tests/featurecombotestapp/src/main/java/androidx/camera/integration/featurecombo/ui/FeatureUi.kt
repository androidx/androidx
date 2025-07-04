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

package androidx.camera.integration.featurecombo.ui

import androidx.camera.integration.featurecombo.AppFeatures
import androidx.camera.integration.featurecombo.DynamicRange
import androidx.camera.integration.featurecombo.Fps
import androidx.camera.integration.featurecombo.ImageFormat
import androidx.camera.integration.featurecombo.StabilizationMode

data class FeatureUi(
    val title: AppFeatureTitle,
    val selectedValue: String,
    val unsupportedValues: List<String>,
    val possibleValues: List<String>,
)

/** The title of the feature, listed according to priority in descending order. */
enum class AppFeatureTitle(val value: String) {
    HDR("HDR"),
    FPS("FPS"),
    STABILIZATION("Stabilization Mode"),
    IMAGE_FORMAT("Image Format"),
}

fun AppFeatures.toFeatureUiList(isVideoMode: Boolean): List<FeatureUi> {
    return if (isVideoMode) {
        listOf<FeatureUi>(
            FeatureUi(
                title = AppFeatureTitle.HDR,
                selectedValue = dynamicRange.text,
                unsupportedValues = unsupportedDynamicRanges.map { it.text },
                possibleValues = DynamicRange.values().map { it.text },
            ),
            FeatureUi(
                title = AppFeatureTitle.FPS,
                selectedValue = fps.text,
                unsupportedValues = unsupportedFps.map { it.text },
                possibleValues = Fps.values().map { it.text },
            ),
            FeatureUi(
                title = AppFeatureTitle.STABILIZATION,
                selectedValue = stabilizationMode.text,
                unsupportedValues = unsupportedStabilizationModes.map { it.text },
                possibleValues = StabilizationMode.values().map { it.text },
            ),
        )
    } else {
        listOf<FeatureUi>(
            FeatureUi(
                title = AppFeatureTitle.IMAGE_FORMAT,
                selectedValue = imageFormat.text,
                unsupportedValues = unsupportedImageFormats.map { it.text },
                possibleValues = ImageFormat.values().map { it.text },
            ),
            FeatureUi(
                title = AppFeatureTitle.HDR,
                selectedValue = dynamicRange.text,
                unsupportedValues = unsupportedDynamicRanges.map { it.text },
                possibleValues = DynamicRange.values().map { it.text },
            ),
            FeatureUi(
                title = AppFeatureTitle.STABILIZATION,
                selectedValue = stabilizationMode.text,
                unsupportedValues = unsupportedStabilizationModes.map { it.text },
                possibleValues = StabilizationMode.values().map { it.text },
            ),
            FeatureUi(
                title = AppFeatureTitle.FPS,
                selectedValue = fps.text,
                unsupportedValues = unsupportedFps.map { it.text },
                possibleValues = Fps.values().map { it.text },
            ),
        )
    }
}
