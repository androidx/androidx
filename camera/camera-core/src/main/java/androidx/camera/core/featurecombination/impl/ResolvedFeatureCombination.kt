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

package androidx.camera.core.featurecombination.impl

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Logger
import androidx.camera.core.SessionConfig
import androidx.camera.core.featurecombination.Feature
import androidx.camera.core.featurecombination.impl.resolver.DefaultFeatureCombinationResolver
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Supported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.Unsupported
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UnsupportedUseCase
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolutionResult.UseCaseMissing
import androidx.camera.core.featurecombination.impl.resolver.FeatureCombinationResolver
import androidx.camera.core.impl.CameraInfoInternal

/**
 * A feature combination data class where the exact features to use have been resolved.
 *
 * In future, this can be used to contain resolved use case configs or stream specs as well.
 */
public class ResolvedFeatureCombination(public val features: Set<Feature>) {
    override fun toString(): String {
        return "ResolvedFeatureCombination(features=$features)"
    }

    public companion object {
        private const val TAG = "ResolvedFeatureCombination"

        /**
         * Resolves a [SessionConfig] to a [ResolvedFeatureCombination] based on the provided
         * preferred feature priority and camera capabilities.
         *
         * <p>This function uses a [DefaultFeatureCombinationResolver] to determine the highest
         * priority and supported feature set, along with the correct use case resolutions for the
         * given combination.
         *
         * @param cameraInfoInternal The [CameraInfoInternal] providing device-specific camera
         *   capabilities.
         * @param resolver The [FeatureCombinationResolver] used to resolve the feature,
         *   [DefaultFeatureCombinationResolver] by default.
         * @return The [ResolvedFeatureCombination] containing the resolved features and use case
         *   resolutions, null if no feature is provided in input and thus resolving is not
         *   applicable.
         * @throws IllegalArgumentException If resolving the feature combination fails for some
         *   reason.
         */
        @JvmOverloads
        @JvmStatic
        public fun SessionConfig.resolveFeatureCombination(
            cameraInfoInternal: CameraInfoInternal,
            resolver: FeatureCombinationResolver =
                DefaultFeatureCombinationResolver(cameraInfoInternal),
        ): ResolvedFeatureCombination? {
            Logger.d(
                TAG,
                "resolveFeatureCombination: sessionConfig = $this," +
                    " lensFacing = ${cameraInfoInternal.lensFacing}",
            )

            if (requiredFeatures.isEmpty() && preferredFeatures.isEmpty()) {
                // nothing to resolve
                return null
            }

            val result = resolver.resolveFeatureCombination(this)

            when (result) {
                is Supported -> {
                    val resolvedFeatureCombination = result.resolvedFeatureCombination
                    Logger.d(TAG, "resolvedFeatureCombination = $resolvedFeatureCombination")

                    return resolvedFeatureCombination
                }
                is Unsupported -> {
                    throw IllegalArgumentException("Feature combination is not supported")
                }
                is UnsupportedUseCase -> {
                    throw IllegalArgumentException("${result.unsupportedUseCase} is not supported")
                }
                is UseCaseMissing -> {
                    throw IllegalArgumentException(
                        "${result.requiredUseCases} must be added for" +
                            " ${result.featureRequiring}"
                    )
                }
            }
        }
    }
}
