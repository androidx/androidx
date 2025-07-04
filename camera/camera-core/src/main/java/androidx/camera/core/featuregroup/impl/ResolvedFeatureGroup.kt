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

package androidx.camera.core.featuregroup.impl

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.Logger
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.impl.resolver.DefaultFeatureGroupResolver
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.Supported
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.Unsupported
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.UnsupportedUseCase
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolutionResult.UseCaseMissing
import androidx.camera.core.featuregroup.impl.resolver.FeatureGroupResolver
import androidx.camera.core.impl.CameraInfoInternal

/**
 * A feature group data class where the exact features to use have been resolved.
 *
 * In future, this can be used to contain resolved use case configs or stream specs as well.
 */
public class ResolvedFeatureGroup(public val features: Set<GroupableFeature>) {
    override fun toString(): String {
        return "ResolvedFeatureGroup(features=$features)"
    }

    public companion object {
        private const val TAG = "ResolvedFeatureGroup"

        /**
         * Resolves a [SessionConfig] to a [ResolvedFeatureGroup] based on the provided preferred
         * feature priority and camera capabilities.
         *
         * <p>This function uses a [DefaultFeatureGroupResolver] to determine the highest priority
         * and supported feature set, along with the correct use case resolutions for the given
         * combination.
         *
         * @param cameraInfoInternal The [CameraInfoInternal] providing device-specific camera
         *   capabilities.
         * @param resolver The [FeatureGroupResolver] used to resolve the feature,
         *   [DefaultFeatureGroupResolver] by default.
         * @return The [ResolvedFeatureGroup] containing the resolved features and use case
         *   resolutions, null if no feature is provided in input and thus resolving is not
         *   applicable.
         * @throws IllegalArgumentException If resolving the feature combination fails for some
         *   reason.
         */
        @JvmOverloads
        @JvmStatic
        public fun SessionConfig.resolveFeatureGroup(
            cameraInfoInternal: CameraInfoInternal,
            resolver: FeatureGroupResolver = DefaultFeatureGroupResolver(cameraInfoInternal),
        ): ResolvedFeatureGroup? {
            Logger.d(
                TAG,
                "resolveFeatureGroup: sessionConfig = $this," +
                    " lensFacing = ${cameraInfoInternal.lensFacing}",
            )

            if (requiredFeatureGroup.isEmpty() && preferredFeatureGroup.isEmpty()) {
                // nothing to resolve
                return null
            }

            val result = resolver.resolveFeatureGroup(this)

            when (result) {
                is Supported -> {
                    val resolvedFeatureGroup = result.resolvedFeatureGroup
                    Logger.d(TAG, "resolvedFeatureGroup = $resolvedFeatureGroup")

                    return resolvedFeatureGroup
                }
                is Unsupported -> {
                    throw IllegalArgumentException("Feature group is not supported")
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
