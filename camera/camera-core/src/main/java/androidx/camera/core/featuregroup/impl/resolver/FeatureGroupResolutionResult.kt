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

package androidx.camera.core.featuregroup.impl.resolver

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featuregroup.GroupableFeature
import androidx.camera.core.featuregroup.impl.ResolvedFeatureGroup

/**
 * Represents the result of resolving to a [ResolvedFeatureGroup].
 *
 * @see FeatureGroupResolver
 */
public sealed interface FeatureGroupResolutionResult {
    public data class Supported(val resolvedFeatureGroup: ResolvedFeatureGroup) :
        FeatureGroupResolutionResult

    @OptIn(ExperimentalSessionConfig::class)
    public data class UseCaseMissing(
        val requiredUseCases: String,
        val featureRequiring: GroupableFeature,
    ) : FeatureGroupResolutionResult

    public data class UnsupportedUseCase(val unsupportedUseCase: UseCase) :
        FeatureGroupResolutionResult

    public object Unsupported : FeatureGroupResolutionResult
}
