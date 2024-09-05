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

package androidx.health.connect.client

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi
import androidx.health.connect.client.feature.HealthConnectPlatformVersion
import androidx.health.connect.client.feature.HealthConnectVersionInfo

/** Interface for checking availability of features in [HealthConnectClient]. */
@ExperimentalFeatureAvailabilityApi
interface HealthConnectFeatures {

    /**
     * Checks whether the given feature is available.
     *
     * @param feature the feature to be checked. One of the "FEATURE_" constants in this class.
     * @return one of [FEATURE_STATUS_UNAVAILABLE] or [FEATURE_STATUS_AVAILABLE]
     */
    @FeatureStatus fun getFeatureStatus(@Feature feature: Int): Int

    /** Constants related to HealthConnect feature availability. */
    companion object {

        /** Feature constant for reading health data in background. */
        const val FEATURE_READ_HEALTH_DATA_IN_BACKGROUND = 1

        /** Feature constant for skin temperature. */
        @RestrictTo(RestrictTo.Scope.LIBRARY) const val FEATURE_SKIN_TEMPERATURE = 2

        /** Feature constant for planned exercise sessions. */
        @RestrictTo(RestrictTo.Scope.LIBRARY) const val FEATURE_PLANNED_EXERCISE = 3

        /** Feature constant for reading health data history. */
        @RestrictTo(RestrictTo.Scope.LIBRARY) const val FEATURE_HEALTH_DATA_HISTORIC_READ = 4

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            value =
                [
                    FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
                    FEATURE_SKIN_TEMPERATURE,
                    FEATURE_PLANNED_EXERCISE,
                    FEATURE_HEALTH_DATA_HISTORIC_READ
                ]
        )
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        annotation class Feature

        /**
         * Indicates that a feature is unavailable and the corresponding APIs cannot be used at
         * runtime.
         */
        const val FEATURE_STATUS_UNAVAILABLE = 1

        /**
         * Indicates that a feature is available and the corresponding APIs can be used at runtime.
         */
        const val FEATURE_STATUS_AVAILABLE = 2

        @Retention(AnnotationRetention.SOURCE)
        @IntDef(value = [FEATURE_STATUS_UNAVAILABLE, FEATURE_STATUS_AVAILABLE])
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        annotation class FeatureStatus

        // TODO(b/362483564): Change to SDK EXT in API 34 once that's available
        private val API_35_PLATFORM_VERSION: HealthConnectPlatformVersion =
            HealthConnectPlatformVersion(buildVersionCode = 35)

        internal val FEATURE_TO_VERSION_INFO_MAP: Map<Int, HealthConnectVersionInfo> =
            mapOf(
                FEATURE_READ_HEALTH_DATA_IN_BACKGROUND to
                    HealthConnectVersionInfo(platformVersion = API_35_PLATFORM_VERSION)
            )
    }
}
