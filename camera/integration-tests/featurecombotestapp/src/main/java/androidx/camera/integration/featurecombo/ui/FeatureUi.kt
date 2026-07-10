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

import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.integration.featurecombo.AppFeatures
import androidx.camera.integration.featurecombo.AppUseCase
import androidx.camera.integration.featurecombo.AppUseCase.Companion.getSupportedGroupableFeatures
import androidx.camera.integration.featurecombo.DynamicRange
import androidx.camera.integration.featurecombo.Effect
import androidx.camera.integration.featurecombo.Fps
import androidx.camera.integration.featurecombo.ImageFormat
import androidx.camera.integration.featurecombo.RecordingQuality
import androidx.camera.integration.featurecombo.StabilizationMode
import androidx.camera.integration.featurecombo.effects.BouncyLogoOverlayEffect.Companion.supportsEffect

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
    STABILIZATION("Stabilization"),
    IMAGE_FORMAT("Img Format"),
    RECORDING_QUALITY("Recording"),
    EFFECT("Effect"),
}

fun AppFeatures.toFeatureUiList(useCases: Set<AppUseCase>): List<FeatureUi> {
    return buildList {
        useCases
            .getSupportedGroupableFeatures()
            .map { it.featureType }
            .distinct()
            .forEach { createFeatureUi(it)?.apply { add(this) } }

        if (useCases.supportsEffect()) {
            add(
                FeatureUi(
                    title = AppFeatureTitle.EFFECT,
                    selectedValue = effect.text,
                    unsupportedValues = unsupportedEffects.map { it.text },
                    possibleValues = Effect.entries.map { it.text },
                )
            )
        }
    }
}

fun AppFeatures.createFeatureUi(featureType: Int): FeatureUi? {
    return when (featureType) {
        GroupableFeature.FEATURE_TYPE_DYNAMIC_RANGE ->
            FeatureUi(
                title = AppFeatureTitle.HDR,
                selectedValue = dynamicRange.text,
                unsupportedValues = unsupportedDynamicRanges.map { it.text },
                possibleValues = DynamicRange.entries.map { it.text },
            )
        GroupableFeature.FEATURE_TYPE_RECORDING_QUALITY ->
            FeatureUi(
                title = AppFeatureTitle.RECORDING_QUALITY,
                selectedValue = recordingQuality.text,
                unsupportedValues = unsupportedRecordingQualities.map { it.text },
                possibleValues = RecordingQuality.entries.map { it.text },
            )
        GroupableFeature.FEATURE_TYPE_FPS_RANGE ->
            FeatureUi(
                title = AppFeatureTitle.FPS,
                selectedValue = fps.text,
                unsupportedValues = unsupportedFps.map { it.text },
                possibleValues = Fps.entries.map { it.text },
            )
        GroupableFeature.FEATURE_TYPE_VIDEO_STABILIZATION ->
            FeatureUi(
                title = AppFeatureTitle.STABILIZATION,
                selectedValue = stabilizationMode.text,
                unsupportedValues = unsupportedStabilizationModes.map { it.text },
                possibleValues = StabilizationMode.entries.map { it.text },
            )
        GroupableFeature.FEATURE_TYPE_IMAGE_FORMAT ->
            FeatureUi(
                title = AppFeatureTitle.IMAGE_FORMAT,
                selectedValue = imageFormat.text,
                unsupportedValues = unsupportedImageFormats.map { it.text },
                possibleValues = ImageFormat.entries.map { it.text },
            )
        else -> null
    }
}
