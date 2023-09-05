/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.ComponentName
import android.content.Context
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.health.connect.client.impl.HealthConnectClientImpl
import androidx.health.platform.client.HealthDataService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

private const val PROVIDER_PACKAGE_NAME = "com.example.fake.provider"

@RunWith(AndroidJUnit4::class)
class HealthConnectClientTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun noBackingImplementation_unavailable() {
        val packageManager = context.packageManager
        shadowOf(packageManager).removePackage(PROVIDER_PACKAGE_NAME)
        assertThat(HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_notEnabled_unavailable() {
        installPackage(
            context,
            PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE,
            enabled = false
        )
        assertThat(HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_enabledNoService_unavailable() {
        installPackage(
            context,
            PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE,
            enabled = true
        )
        assertThat(HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_enabledUnsupportedVersion_unavailable() {
        installPackage(
            context,
            HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE - 1,
            enabled = true
        )
        installService(context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)

        assertThat(HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) { HealthConnectClient.getOrCreate(context) }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_enabledSupportedVersion_isAvailable() {
        installPackage(
            context,
            HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE,
            enabled = true
        )
        installService(context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)

        assertThat(
                HealthConnectClient.getSdkStatus(
                    context,
                    HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME
                )
            )
            .isEqualTo(HealthConnectClient.SDK_AVAILABLE)
        assertThat(HealthConnectClient.getOrCreate(context))
            .isInstanceOf(HealthConnectClientImpl::class.java)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementationLegacy_enabledSupportedVersion_isAvailable() {
        installPackage(
            context,
            HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE,
            enabled = true
        )
        installService(context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)

        assertThat(
                HealthConnectClient.getSdkStatusLegacy(
                    context,
                    HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME
                )
            )
            .isEqualTo(HealthConnectClient.SDK_AVAILABLE)
        assertThat(HealthConnectClient.getOrCreateLegacy(context))
            .isInstanceOf(HealthConnectClientImpl::class.java)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    @Suppress("Deprecation")
    fun sdkVersionTooOld_unavailable() {
        assertThat(HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE)
        assertThrows(UnsupportedOperationException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    @Suppress("Deprecation")
    fun sdkVersionTooOld_legacyClient_unavailable() {
        assertThat(HealthConnectClient.getSdkStatusLegacy(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE)
        assertThrows(UnsupportedOperationException::class.java) {
            HealthConnectClient.getOrCreateLegacy(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun getHealthConnectManageDataAction_unsupportedClient_returnsDefaultIntent() {
        installPackage(
            context,
            HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE,
            enabled = true
        )

        assertThat(HealthConnectClient.getHealthConnectManageDataAction(context)).isEqualTo(
            HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS
        )
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    fun getHealthConnectManageDataAction_supportedClient() {
        installPackage(
            context,
            HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.ACTION_MANAGE_DATA_MIN_SUPPORTED_VERSION_CODE,
            enabled = true
        )
        installService(context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)

        assertThat(HealthConnectClient.getHealthConnectManageDataAction(context)).isEqualTo(
            "androidx.health.ACTION_MANAGE_HEALTH_DATA"
        )
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun getHealthConnectManageDataAction_platformSupported() {
        assertThat(HealthConnectClient.getHealthConnectManageDataAction(context)).isEqualTo(
            "android.health.connect.action.MANAGE_HEALTH_DATA"
        )
    }

    private fun installPackage(
        context: Context,
        packageName: String,
        versionCode: Int,
        enabled: Boolean
    ) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        @Suppress("Deprecation")
        packageInfo.versionCode = versionCode
        packageInfo.applicationInfo = ApplicationInfo()
        packageInfo.applicationInfo.enabled = enabled
        val packageManager = context.packageManager
        Shadows.shadowOf(packageManager).installPackage(packageInfo)
    }

    private fun installService(context: Context, packageName: String) {
        val packageManager = context.packageManager
        val serviceIntentFilter =
            IntentFilter(HealthDataService.ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION)
        val serviceComponentName =
            ComponentName(
                packageName,
                HealthDataService.ANDROID_HEALTH_PLATFORM_SERVICE_BIND_ACTION
            )
        shadowOf(packageManager).addServiceIfNotPresent(serviceComponentName)
        shadowOf(packageManager)
            .addIntentFilterForService(serviceComponentName, serviceIntentFilter)
    }
}
