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
import androidx.camera.core.SessionConfig
import androidx.camera.core.featuregroup.impl.ResolvedFeatureGroup

/**
 * Defines how feature group options (i.e. preferred features) are resolved based on whether a
 * feature group is supported or not.
 *
 * @see resolveFeatureGroup
 */
public interface FeatureGroupResolver {
    /**
     * Returns a [FeatureGroupResolutionResult] which is either a supported [ResolvedFeatureGroup]
     * instance or some error.
     *
     * @param sessionConfig The [SessionConfig] containing the use cases, required features, and
     *   preferred features to be used for resolving the feature combination.
     * @return A [FeatureGroupResolutionResult] indicating the result. In case of success, it will
     *   contain a supported [ResolvedFeatureGroup]. If no suitable combination is found, it will
     *   contain an error detailing the reason.
     * @throws IllegalStateException If the underlying feature combination resolution logic
     *   encounters an unexpected state.
     */
    @OptIn(ExperimentalSessionConfig::class)
    @Throws(IllegalStateException::class)
    public fun resolveFeatureGroup(sessionConfig: SessionConfig): FeatureGroupResolutionResult
}
