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
package androidx.health.data.client

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

private const val PROVIDER_PACKAGE_NAME = "com.example.fake.provider"

@RunWith(AndroidJUnit4::class)
class HealthDataClientTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun noBackingImplementation_unavailable() {
        val packageManager = context.packageManager
        Shadows.shadowOf(packageManager).removePackage(PROVIDER_PACKAGE_NAME)
        assertThat(HealthDataClient.isAvailable(context, listOf(PROVIDER_PACKAGE_NAME))).isFalse()
        assertThrows(IllegalStateException::class.java) {
            HealthDataClient.getOrCreate(context, listOf(PROVIDER_PACKAGE_NAME))
        }
    }

    @Test
    fun backingImplementation_notEnabled_unavailable() {
        installPackage(context, PROVIDER_PACKAGE_NAME, enabled = false)
        assertThat(HealthDataClient.isAvailable(context, listOf(PROVIDER_PACKAGE_NAME))).isFalse()
        assertThrows(IllegalStateException::class.java) {
            HealthDataClient.getOrCreate(context, listOf(PROVIDER_PACKAGE_NAME))
        }
    }

    @Test
    fun backingImplementation_enabled_isAvailable() {
        installPackage(context, PROVIDER_PACKAGE_NAME, enabled = true)
        assertThat(HealthDataClient.isAvailable(context, listOf(PROVIDER_PACKAGE_NAME))).isTrue()
        HealthDataClient.getOrCreate(context, listOf(PROVIDER_PACKAGE_NAME))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O_MR1])
    fun sdkVersionTooOld_unavailable() {
        assertThat(HealthDataClient.isAvailable(context, listOf(PROVIDER_PACKAGE_NAME))).isFalse()
        assertThrows(UnsupportedOperationException::class.java) {
            HealthDataClient.getOrCreate(context, listOf(PROVIDER_PACKAGE_NAME))
        }
    }

    private fun installPackage(context: Context, packageName: String, enabled: Boolean) {
        val packageInfo = PackageInfo()
        packageInfo.packageName = packageName
        packageInfo.applicationInfo = ApplicationInfo()
        packageInfo.applicationInfo.enabled = enabled
        val packageManager = context.packageManager
        Shadows.shadowOf(packageManager).installPackage(packageInfo)
    }
}
