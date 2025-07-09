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
import androidx.health.connect.client.feature.ExperimentalPersonalHealthRecordApi
import androidx.health.connect.client.feature.HealthConnectPlatformVersion
import androidx.health.connect.client.feature.HealthConnectVersionInfo

/** Interface for checking availability of features in [HealthConnectClient]. */
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
        const val FEATURE_SKIN_TEMPERATURE = 2

        /** Feature constant for planned exercise sessions. */
        const val FEATURE_PLANNED_EXERCISE = 3

        /** Feature constant for reading health data history. */
        const val FEATURE_READ_HEALTH_DATA_HISTORY = 4

        /** Feature constant for mindfulness session. */
        const val FEATURE_MINDFULNESS_SESSION = 5

        /** Feature constant for Personal Health Records APIs. */
        @ExperimentalPersonalHealthRecordApi const val FEATURE_PERSONAL_HEALTH_RECORD = 6

        /** Feature constant for Activity Intensity APIs. */
        const val FEATURE_ACTIVITY_INTENSITY = 7

        @OptIn(ExperimentalPersonalHealthRecordApi::class)
        @Retention(AnnotationRetention.SOURCE)
        @IntDef(
            value =
                [
                    FEATURE_READ_HEALTH_DATA_IN_BACKGROUND,
                    FEATURE_SKIN_TEMPERATURE,
                    FEATURE_PLANNED_EXERCISE,
                    FEATURE_READ_HEALTH_DATA_HISTORY,
                    FEATURE_PERSONAL_HEALTH_RECORD,
                    FEATURE_MINDFULNESS_SESSION,
                    FEATURE_ACTIVITY_INTENSITY,
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

        private val SDK_EXT_13_PLATFORM_VERSION: HealthConnectPlatformVersion =
            HealthConnectPlatformVersion(buildVersionCode = 34, sdkExtensionVersion = 13)
        private val SDK_EXT_15_PLATFORM_VERSION: HealthConnectPlatformVersion =
            HealthConnectPlatformVersion(buildVersionCode = 34, sdkExtensionVersion = 15)
        private val SDK_EXT_16_PLATFORM_VERSION: HealthConnectPlatformVersion =
            HealthConnectPlatformVersion(buildVersionCode = 34, sdkExtensionVersion = 16)

        @OptIn(ExperimentalPersonalHealthRecordApi::class)
        internal val FEATURE_TO_VERSION_INFO_MAP: Map<Int, HealthConnectVersionInfo> =
            mapOf(
                FEATURE_READ_HEALTH_DATA_IN_BACKGROUND to
                    HealthConnectVersionInfo(
                        apkVersionCode = 171302,
                        platformVersion = SDK_EXT_13_PLATFORM_VERSION,
                    ),
                FEATURE_SKIN_TEMPERATURE to
                    HealthConnectVersionInfo(
                        apkVersionCode = 200027,
                        platformVersion = SDK_EXT_13_PLATFORM_VERSION,
                    ),
                FEATURE_READ_HEALTH_DATA_HISTORY to
                    HealthConnectVersionInfo(
                        apkVersionCode = 171302,
                        platformVersion = SDK_EXT_13_PLATFORM_VERSION,
                    ),
                FEATURE_PLANNED_EXERCISE to
                    HealthConnectVersionInfo(platformVersion = SDK_EXT_13_PLATFORM_VERSION),
                FEATURE_MINDFULNESS_SESSION to
                    HealthConnectVersionInfo(
                        apkVersionCode = 194767,
                        platformVersion = SDK_EXT_15_PLATFORM_VERSION,
                    ),
                FEATURE_PERSONAL_HEALTH_RECORD to
                    HealthConnectVersionInfo(platformVersion = SDK_EXT_16_PLATFORM_VERSION),
                FEATURE_ACTIVITY_INTENSITY to
                    HealthConnectVersionInfo(platformVersion = SDK_EXT_16_PLATFORM_VERSION),
            )
    }
}
