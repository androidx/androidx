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
        Shadows.shadowOf(packageManager).removePackage(PROVIDER_PACKAGE_NAME)
        assertThat(HealthConnectClient.isApiSupported()).isTrue()
        assertThat(HealthConnectClient.isProviderAvailable(context, PROVIDER_PACKAGE_NAME))
            .isFalse()
        assertThat(HealthConnectClient.sdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_notEnabled_unavailable() {
        installPackage(context, PROVIDER_PACKAGE_NAME, versionCode = 35001, enabled = false)
        assertThat(HealthConnectClient.isProviderAvailable(context, PROVIDER_PACKAGE_NAME))
            .isFalse()
        assertThat(HealthConnectClient.sdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_enabledNoService_unavailable() {
        installPackage(context, PROVIDER_PACKAGE_NAME, versionCode = 35001, enabled = true)
        assertThat(HealthConnectClient.isProviderAvailable(context, PROVIDER_PACKAGE_NAME))
            .isFalse()
        assertThat(HealthConnectClient.sdkStatus(context, PROVIDER_PACKAGE_NAME))
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
            enabled = true)
        installService(context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)

        assertThat(HealthConnectClient.isProviderAvailable(context)).isFalse()
        assertThat(HealthConnectClient.sdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        assertThrows(IllegalStateException::class.java) {
            HealthConnectClient.getOrCreate(context)
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.P])
    @Suppress("Deprecation")
    fun backingImplementation_enabledSupportedVersion_isAvailable() {
        installPackage(
            context,
            HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME,
            versionCode = HealthConnectClient.DEFAULT_PROVIDER_MIN_VERSION_CODE,
            enabled = true)
        installService(context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME)

        assertThat(HealthConnectClient.isProviderAvailable(context)).isTrue()
        assertThat(HealthConnectClient.sdkStatus(
            context, HealthConnectClient.DEFAULT_PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_AVAILABLE)
        HealthConnectClient.getOrCreate(context)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    @Suppress("Deprecation")
    fun sdkVersionTooOld_unavailable() {
        assertThat(HealthConnectClient.isApiSupported()).isFalse()
        assertThat(HealthConnectClient.isProviderAvailable(context, PROVIDER_PACKAGE_NAME))
            .isFalse()
        assertThat(HealthConnectClient.sdkStatus(context, PROVIDER_PACKAGE_NAME))
            .isEqualTo(HealthConnectClient.SDK_UNAVAILABLE)
        assertThrows(UnsupportedOperationException::class.java) {
            HealthConnectClient.getOrCreate(context, PROVIDER_PACKAGE_NAME)
        }
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
