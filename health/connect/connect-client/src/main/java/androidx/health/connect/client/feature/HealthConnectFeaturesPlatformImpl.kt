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

@file:RequiresApi(api = Build.VERSION_CODES.R)

package androidx.health.connect.client.feature

import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_STATUS_AVAILABLE
import androidx.health.connect.client.HealthConnectFeatures.Companion.FEATURE_STATUS_UNAVAILABLE
import androidx.health.connect.client.HealthConnectFeatures.Companion.Feature
import androidx.health.connect.client.HealthConnectFeatures.Companion.FeatureStatus

/**
 * Implementation of [HealthConnectFeatures] that performs the availability checks of features in
 * Android Platform.
 */
internal class HealthConnectFeaturesPlatformImpl : HealthConnectFeatures {

    @FeatureStatus
    override fun getFeatureStatus(@Feature feature: Int): Int = Companion.getFeatureStatus(feature)

    internal companion object {
        /**
         * Checks whether the given feature is available.
         *
         * @param feature the feature to be checked. One of the "FEATURE_" constants in this class.
         * @return one of [FEATURE_STATUS_UNAVAILABLE] or [FEATURE_STATUS_AVAILABLE]
         */
        fun getFeatureStatus(@Feature feature: Int): Int {
            return HealthConnectFeatures.FEATURE_TO_VERSION_INFO_MAP.getFeatureStatus(feature)
        }

        @VisibleForTesting
        internal fun Map<Int, HealthConnectVersionInfo>.getFeatureStatus(
            @Feature feature: Int
        ): Int {
            val minimumRequiredVersion =
                this[feature]?.platformVersion ?: return FEATURE_STATUS_UNAVAILABLE

            if (minimumRequiredVersion.buildVersionCode > Build.VERSION.SDK_INT) {
                return FEATURE_STATUS_UNAVAILABLE
            }

            // If there is no sdkExtension for the feature, then it is supported across all
            // extension versions of the platform build version.
            if (minimumRequiredVersion.sdkExtensionVersion == null) {
                return FEATURE_STATUS_AVAILABLE
            }

            if (
                minimumRequiredVersion.sdkExtensionVersion <=
                    SdkExtensions.getExtensionVersion(minimumRequiredVersion.buildVersionCode)
            ) {
                return FEATURE_STATUS_AVAILABLE
            }

            return FEATURE_STATUS_UNAVAILABLE
        }
    }
}
