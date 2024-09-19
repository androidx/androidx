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
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.health.connect.client.HealthConnectClient.Companion.DEFAULT_PROVIDER_PACKAGE_NAME
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.feature.HealthConnectFeaturesApkImpl.Companion.getFeatureStatus
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

private const val FEATURE_NON_EXISTENT = 1
private const val FEATURE_ADDED_IN_PREVIOUS_APK_VERSION = 2
private const val FEATURE_ADDED_IN_CURRENT_APK_VERSION = 3
private const val FEATURE_ADDED_IN_PLATFORM_ONLY = 4

private const val PREVIOUS_VERSION_CODE = 9_000
private const val CURRENT_VERSION_CODE = 10_000

private val FEATURE_TO_VERSION_INFO_MAP: Map<Int, HealthConnectVersionInfo> =
    mapOf(
        FEATURE_ADDED_IN_PREVIOUS_APK_VERSION to
            HealthConnectVersionInfo(
                apkVersionCode = PREVIOUS_VERSION_CODE.toLong(),
                platformVersion =
                    HealthConnectPlatformVersion(
                        buildVersionCode = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    )
            ),
        FEATURE_ADDED_IN_CURRENT_APK_VERSION to
            HealthConnectVersionInfo(
                apkVersionCode = CURRENT_VERSION_CODE.toLong(),
                platformVersion =
                    HealthConnectPlatformVersion(
                        buildVersionCode = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    )
            ),
        FEATURE_ADDED_IN_PLATFORM_ONLY to
            HealthConnectVersionInfo(
                platformVersion =
                    HealthConnectPlatformVersion(
                        buildVersionCode = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                    )
            )
    )

@OptIn(ExperimentalFeatureAvailabilityApi::class)
@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
class HealthConnectFeaturesApkImplTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getFeatureStatus_currentApkVersion() {
        installPackageWithVersion(CURRENT_VERSION_CODE)

        assertAvailable(FEATURE_ADDED_IN_PREVIOUS_APK_VERSION, FEATURE_ADDED_IN_CURRENT_APK_VERSION)
        assertUnavailable(FEATURE_ADDED_IN_PLATFORM_ONLY, FEATURE_NON_EXISTENT)
    }

    @Test
    fun getFeatureStatus_previousApkVersion() {
        installPackageWithVersion(PREVIOUS_VERSION_CODE)

        assertAvailable(FEATURE_ADDED_IN_PREVIOUS_APK_VERSION)
        assertUnavailable(
            FEATURE_ADDED_IN_CURRENT_APK_VERSION,
            FEATURE_ADDED_IN_PLATFORM_ONLY,
            FEATURE_NON_EXISTENT
        )
    }

    private fun assertAvailable(vararg features: Int) {
        for (feature in features) {
            assertThat(getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_AVAILABLE)
        }
    }

    private fun assertUnavailable(vararg features: Int) {
        for (feature in features) {
            assertThat(getFeatureStatus(feature))
                .isEqualTo(HealthConnectFeatures.FEATURE_STATUS_UNAVAILABLE)
        }
    }

    private fun getFeatureStatus(feature: Int): Int {
        return FEATURE_TO_VERSION_INFO_MAP.getFeatureStatus(
            context,
            DEFAULT_PROVIDER_PACKAGE_NAME,
            feature
        )
    }

    private fun installPackageWithVersion(versionCode: Int) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = DEFAULT_PROVIDER_PACKAGE_NAME
        @Suppress("Deprecation")
        packageInfo.versionCode = versionCode
        packageInfo.applicationInfo = ApplicationInfo()
        packageInfo.applicationInfo!!.enabled = true
        val packageManager = context.packageManager
        Shadows.shadowOf(packageManager).installPackage(packageInfo)
    }
}
