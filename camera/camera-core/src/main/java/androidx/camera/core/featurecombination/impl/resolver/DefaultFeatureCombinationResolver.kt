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

@file:OptIn(ExperimentalSessionConfig::class)

package androidx.camera.core.featurecombination.impl.resolver

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.Logger
import androidx.camera.core.Preview
import androidx.camera.core.SessionConfig
import androidx.camera.core.featurecombination.Feature
import androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination
import androidx.camera.core.featurecombination.impl.UseCaseType
import androidx.camera.core.featurecombination.impl.UseCaseType.Companion.getFeatureComboUseCaseType
import androidx.camera.core.featurecombination.impl.UseCaseType.IMAGE_CAPTURE
import androidx.camera.core.featurecombination.impl.UseCaseType.PREVIEW
import androidx.camera.core.featurecombination.impl.UseCaseType.VIDEO_CAPTURE
import androidx.camera.core.featurecombination.impl.feature.DynamicRangeFeature
import androidx.camera.core.featurecombination.impl.feature.FpsRangeFeature
import androidx.camera.core.featurecombination.impl.feature.ImageFormatFeature
import androidx.camera.core.featurecombination.impl.feature.VideoStabilizationFeature
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Supported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Unsupported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UnsupportedUseCase
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UseCaseMissing
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.internal.CameraUseCaseAdapter.isVideoCapture

/**
 * A [FeatureCombinationResolver] that recursively tries out all combinations of features (according
 * to the preference order) until a supported combination is found.
 *
 * If there are three features {A, B, C} which are ordered with descending priority (i.e. A has
 * highest priority and C has lowest), this class will try out the following combinations in the
 * same order as shown.
 * 1. A, B, C
 * 2. A, B
 * 3. A, C
 * 4. A
 * 5. B, C
 * 6. B
 * 7. C
 *
 * If the first two feature combinations (#1 and #2) are not supported while third combination is
 * supported (with any resolution), the [resolveFeatureCombination] method in this class will return
 * a result using the third feature combination i.e. {A, C}.
 *
 * @property cameraInfoInternal A [CameraInfoInternal] instance to query if a feature combination is
 *   supported.
 */
internal class DefaultFeatureCombinationResolver(
    private val cameraInfoInternal: CameraInfoInternal
) : FeatureCombinationResolver {
    override fun resolveFeatureCombination(
        sessionConfig: SessionConfig
    ): FeatureCombinationResolutionResult {
        val useCases = sessionConfig.useCases
        val requiredFeatures = sessionConfig.requiredFeatures
        val orderedPreferredFeatures = sessionConfig.preferredFeatures

        require(requiredFeatures.isNotEmpty() || orderedPreferredFeatures.isNotEmpty()) {
            "Must have at least one required or preferred feature"
        }

        val supportsImageFeature = useCases.any { it is ImageCapture }
        val supportsStreamFeature = useCases.any { it is Preview || isVideoCapture(it) }

        // Return early if given use case combination is known to be unsupported
        useCases.forEach {
            val useCaseType = it.getFeatureComboUseCaseType()
            if (useCaseType == UseCaseType.UNDEFINED) {
                return UnsupportedUseCase(it)
            }
        }

        // Return early if a required feature is known to fail with given use case combination
        requiredFeatures.forEach { feature ->
            when (feature) {
                // UltraHDR requires ImageCapture use case
                is ImageFormatFeature -> {
                    if (!supportsImageFeature) {
                        return UseCaseMissing(
                            requiredUseCases = IMAGE_CAPTURE.toString(),
                            featureRequiring = feature,
                        )
                    }
                }
                is DynamicRangeFeature,
                is FpsRangeFeature,
                is VideoStabilizationFeature -> {
                    if (!supportsStreamFeature) {
                        return UseCaseMissing(
                            requiredUseCases = "$PREVIEW or $VIDEO_CAPTURE",
                            featureRequiring = feature,
                        )
                    }
                }
            }
        }

        val filteredPreferredFeatures =
            orderedPreferredFeatures.filter { feature ->
                when (feature) {
                    // Filter out UltraHDR if there's no image stream
                    is ImageFormatFeature -> supportsImageFeature
                    else -> true
                }
            }

        return getFeatureListResolvedByPriority(
            sessionConfig = sessionConfig,
            orderedPreferredFeatures = filteredPreferredFeatures,
        )
    }

    /**
     * Recursively backtracking function to find best supported combination according to priority.
     *
     * Note that this can be O(2^n) in worst-case where n is size of features.
     *
     * @param orderedPreferredFeatures A list of features which is ordered according to priority.
     *   The feature with most priority has a lower index in this ordering.
     * @return A list of features that is best supported according to [orderedPreferredFeatures].
     */
    private fun getFeatureListResolvedByPriority(
        sessionConfig: SessionConfig,
        orderedPreferredFeatures: List<Feature>,
        index: Int = 0,
        currentOptionalFeatures: List<Feature> = emptyList(),
    ): FeatureCombinationResolutionResult {
        // TODO: Use bitmap iteration instead of recursion to optimize this further.
        if (index >= orderedPreferredFeatures.size) {
            // End of recursion, need to test the feature combination now
            val features = sessionConfig.requiredFeatures + currentOptionalFeatures

            Logger.d(
                TAG,
                "getFeatureListResolvedByPriority: features = $features" +
                    ", useCases = ${sessionConfig.useCases}",
            )

            return if (
                cameraInfoInternal.isResolvedFeatureCombinationSupported(
                    ResolvedFeatureCombination(features),
                    sessionConfig,
                )
            ) {
                // TODO: Store the whole UseCase to StreamSpecs map in ResolvedFeatureCombination so
                //  that we can skip this step while binding with a resolved feature combination.
                Supported(ResolvedFeatureCombination(features))
            } else {
                Unsupported
            }
        }

        val resultTakingCurrentFeature =
            getFeatureListResolvedByPriority(
                sessionConfig,
                orderedPreferredFeatures,
                index + 1,
                currentOptionalFeatures + orderedPreferredFeatures[index],
            )

        if (resultTakingCurrentFeature is Supported) {
            return resultTakingCurrentFeature
        }

        return getFeatureListResolvedByPriority(
            sessionConfig,
            orderedPreferredFeatures,
            index + 1,
            currentOptionalFeatures,
        )
    }

    private companion object {
        private const val TAG = "DefaultFeatureCombinationResolver"
    }
}
