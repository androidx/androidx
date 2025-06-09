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

package androidx.camera.core.featurecombination.impl.resolver

import androidx.camera.core.ExperimentalSessionConfig
import androidx.camera.core.UseCase
import androidx.camera.core.featurecombination.Feature
import androidx.camera.core.featurecombination.impl.ResolvedFeatureCombination

/**
 * Represents the result of resolving to a [ResolvedFeatureCombination].
 *
 * @see FeatureCombinationResolver
 */
public sealed interface FeatureCombinationResolutionResult {
    public data class Supported(val resolvedFeatureCombination: ResolvedFeatureCombination) :
        FeatureCombinationResolutionResult

    @OptIn(ExperimentalSessionConfig::class)
    public data class UseCaseMissing(val requiredUseCases: String, val featureRequiring: Feature) :
        FeatureCombinationResolutionResult

    public data class UnsupportedUseCase(val unsupportedUseCase: UseCase) :
        FeatureCombinationResolutionResult

    public object Unsupported : FeatureCombinationResolutionResult
}
