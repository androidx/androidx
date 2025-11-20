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

package androidx.privacysandbox.sdkruntime.core.internal

import androidx.annotation.RestrictTo

/**
 * List of all supported internal API versions (Client-Core communication).
 *
 * NEVER REMOVE / MODIFY RELEASED VERSIONS: That could break loading of SDKs built with
 * previous/future library version.
 *
 * Adding new version here bumps internal API version for next library release:
 * [androidx.privacysandbox.sdkruntime.core.Versions.API_VERSION] When adding a new version, ALL new
 * features from this version should be specified (NO FUTURE CHANGES SUPPORTED).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class ClientApiVersion(
    public val apiLevel: Int,
    public val stable: Boolean = false,
    private val newFeatures: Set<ClientFeature> = emptySet(),
) {
    V5__1_0_ALPHA13(apiLevel = 5),
    V6__1_0_ALPHA14(apiLevel = 6, newFeatures = setOf(ClientFeature.GET_CLIENT_PACKAGE_NAME)),
    V7__1_0_ALPHA16(
        apiLevel = 7,
        /** Temporary mark as stable to run cross version tests only for V7+ */
        stable = true,
        newFeatures = setOf(ClientFeature.CLIENT_IMPORTANCE_LISTENER),
    ),
    V8__1_0_ALPHA18(
        apiLevel = 8,
        newFeatures = setOf(ClientFeature.SDK_SANDBOX_CONTROLLER_BACKEND_HOLDER),
    ),

    /**
     * Unreleased API version. Features not added to other versions will be automatically added here
     * (to allow testing).
     */
    FUTURE_VERSION(apiLevel = Int.MAX_VALUE);

    public companion object {
        /**
         * Minimal version of sdkruntime-client lib that could load SDK built with current version
         * of sdkruntime-provider lib.
         */
        public val MIN_SUPPORTED_CLIENT_VERSION: ClientApiVersion = V6__1_0_ALPHA14

        /**
         * Minimal version of sdkruntime-provider lib that could be loaded by current version of
         * sdkruntime-client lib.
         */
        public val MIN_SUPPORTED_SDK_VERSION: ClientApiVersion = values().minBy { v -> v.apiLevel }

        public val CURRENT_VERSION: ClientApiVersion =
            values().filter { v -> v != FUTURE_VERSION }.maxBy { v -> v.apiLevel }

        public val LATEST_STABLE_VERSION: ClientApiVersion =
            values().filter { v -> v.stable }.maxBy { v -> v.apiLevel }

        private val FEATURE_TO_VERSION_MAP = buildFeatureMap()

        public fun minAvailableVersionFor(clientFeature: ClientFeature): ClientApiVersion {
            return FEATURE_TO_VERSION_MAP[clientFeature] ?: FUTURE_VERSION
        }

        /** Build mapping between [ClientFeature] and version where it first became available. */
        private fun buildFeatureMap(): Map<ClientFeature, ClientApiVersion> {
            if (FUTURE_VERSION.newFeatures.isNotEmpty()) {
                throw IllegalStateException("FUTURE_VERSION MUST NOT define any features")
            }
            if (MIN_SUPPORTED_SDK_VERSION.newFeatures.isNotEmpty()) {
                throw IllegalStateException(
                    "$MIN_SUPPORTED_SDK_VERSION MUST NOT define any features. " +
                        "Features added in this version are available without version checks because " +
                        "it is minimum supported version."
                )
            }
            return buildMap {
                values().forEach { version ->
                    version.newFeatures.forEach { feature ->
                        val oldVersion = put(feature, version)
                        if (oldVersion != null) {
                            throw IllegalStateException(
                                "$feature duplicated in $version and $oldVersion"
                            )
                        }
                    }
                }
            }
        }
    }
}
