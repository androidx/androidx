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

package androidx.health.connect.client.feature

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.annotation.VisibleForTesting
import androidx.core.content.pm.PackageInfoCompat
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.HealthConnectFeatures.Companion.Feature
import androidx.health.connect.client.HealthConnectFeatures.Companion.FeatureStatus

@ExperimentalFeatureAvailabilityApi
internal class HealthConnectFeaturesApkImpl(
    private val context: Context,
    private val providerPackageName: String
) : HealthConnectFeatures {

    @FeatureStatus
    override fun getFeatureStatus(@Feature feature: Int): Int {
        return HealthConnectFeatures.FEATURE_TO_VERSION_INFO_MAP.getFeatureStatus(
            context,
            providerPackageName,
            feature
        )
    }

    @VisibleForTesting
    internal companion object {

        // TODO(b/271840604): Cache version code. getPackageInfo is an expensive operation.
        @VisibleForTesting
        internal fun Map<Int, HealthConnectVersionInfo>.getFeatureStatus(
            context: Context,
            providerPackageName: String,
            @Feature feature: Int
        ): Int {
            val packageInfo: PackageInfo =
                try {
                    context.packageManager.getPackageInfo(providerPackageName, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    error("Provider APK not installed!")
                }

            val installedVersion = PackageInfoCompat.getLongVersionCode(packageInfo)
            val minimumRequiredVersion = this[feature]?.apkVersionCode

            if (minimumRequiredVersion != null && minimumRequiredVersion <= installedVersion) {
                return HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
            }

            return HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE
        }
    }
}
