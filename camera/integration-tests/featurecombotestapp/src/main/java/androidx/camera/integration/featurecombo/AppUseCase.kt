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

package androidx.camera.integration.featurecombo

import android.util.Log
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.integration.featurecombo.PrimitiveCollections.increment
import androidx.camera.integration.featurecombo.PrimitiveCollections.keyList
import androidx.camera.video.GroupableFeatures
import androidx.collection.buildObjectLongMap

/**
 * An enum representing the use cases supported by this test app, along with the features that each
 * use case supports, sorted by priority.
 *
 * @param uiName The name of the use case to be displayed in the UI.
 * @param prioritizedFeatures The list of features that this use case supports, sorted by priority.
 */
enum class AppUseCase(val uiName: String, val prioritizedFeatures: List<GroupableFeature>) {
    PREVIEW(
        "Preview",
        listOf(
            GroupableFeature.FPS_60,
            GroupableFeature.PREVIEW_STABILIZATION,
            GroupableFeature.HDR_HLG10,
        ),
    ),
    IMAGE_CAPTURE("ImageCapture", listOf(GroupableFeature.IMAGE_ULTRA_HDR)),
    VIDEO_CAPTURE(
        "VideoCapture",
        listOf(
            GroupableFeature.HDR_HLG10,
            GroupableFeatures.UHD_RECORDING,
            GroupableFeatures.FHD_RECORDING,
            GroupableFeatures.HD_RECORDING,
            GroupableFeatures.SD_RECORDING,
            GroupableFeature.FPS_60,
            GroupableFeature.PREVIEW_STABILIZATION,
            GroupableFeatures.VIDEO_STABILIZATION,
        ),
    ),
    IMAGE_ANALYSIS(
        "ImageAnalysis",
        listOf(GroupableFeature.PREVIEW_STABILIZATION, GroupableFeature.FPS_60),
    );

    companion object {
        private const val TAG = "CamXFcqMSupportedUseCase"

        /**
         * Returns a list of [GroupableFeature] that this test app supports for a given list of use
         * cases, sorted by priority.
         *
         * The priority is determined by a scoring system that takes into account the priority of
         * the use cases and the priority of the features within each use case.
         */
        fun Collection<AppUseCase>.getSupportedGroupableFeatures(): List<GroupableFeature> {
            // The priority of the use cases, from highest to lowest.
            val useCasesPrioritized =
                AppUseCase.entries.toList().sortedBy {
                    when (it) {
                        VIDEO_CAPTURE -> 1
                        IMAGE_CAPTURE -> 2
                        IMAGE_ANALYSIS -> 3
                        PREVIEW -> 4
                    }
                }

            val useCaseWeightMagnitude = AppUseCase.entries.maxOf { it.prioritizedFeatures.size }

            // Assign a weight to each use case, based on its priority. The weight is used to
            // calculate the score of each feature.
            var magnitude: Long = 1
            val useCaseWeights = buildObjectLongMap {
                useCasesPrioritized.reversed().forEach {
                    put(it, magnitude)
                    magnitude *= useCaseWeightMagnitude
                }
            }

            Log.d(TAG, "supportsFeatures: useCaseWeights = $useCaseWeights")

            // Calculate the score of each feature, based on the use cases that support it, the
            // priority of those use cases, and the priority of the features for each use case.
            val featureScores =
                buildObjectLongMap<GroupableFeature> {
                    AppUseCase.entries.forEach { useCase ->
                        if (contains(useCase)) {
                            useCase.prioritizedFeatures.forEachIndexed { index, feature ->
                                increment(
                                    key = feature,
                                    value =
                                        useCaseWeights[useCase] *
                                            (useCase.prioritizedFeatures.size - index),
                                )
                            }
                        }
                    }
                }

            Log.d(TAG, "supportsFeatures: featureScores = $featureScores")

            // Sort the features by their score in descending order.
            return featureScores
                .keyList()
                .sortedByDescending { featureScores[it] }
                .apply { Log.d(TAG, "supportsFeatures: sortedFeatures = $this") }
        }
    }
}
