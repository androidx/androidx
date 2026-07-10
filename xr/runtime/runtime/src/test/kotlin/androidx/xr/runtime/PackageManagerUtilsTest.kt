/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.runtime

import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.runtime.PackageManagerUtils.XR_PROJECTED_SYSTEM_FEATURE
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class PackageManagerUtilsTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun hasXrProjectedSystemFeature_whenFeatureExists_returnsTrue() {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(XR_PROJECTED_SYSTEM_FEATURE, true)

        assertThat(PackageManagerUtils.hasXrProjectedSystemFeature(context)).isTrue()
    }

    @Test
    fun hasXrProjectedSystemFeature_whenFeatureDoesNotExist_returnsFalse() {
        val shadowPackageManager = shadowOf(context.packageManager)
        shadowPackageManager.setSystemFeature(XR_PROJECTED_SYSTEM_FEATURE, false)

        assertThat(PackageManagerUtils.hasXrProjectedSystemFeature(context)).isFalse()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun getProjectedPlatformApiVersion_whenPropertyMissing_throwsIllegalStateException() {
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(PROJECTED_SERVICE_COMPONENT_NAME)
            addIntentFilterForService(
                PROJECTED_SERVICE_COMPONENT_NAME,
                IntentFilter(PackageManagerUtils.ACTION_BIND),
            )
            installPackage(PROJECTED_PACKAGE_INFO)
        }

        assertThrows(IllegalStateException::class.java) {
            PackageManagerUtils.getProjectedPlatformApiVersion(context)
        }
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.S)
    fun getProjectedPlatformApiVersion_whenServiceMissing_throwsIllegalStateException() {
        assertThrows(IllegalStateException::class.java) {
            PackageManagerUtils.getProjectedPlatformApiVersion(context)
        }
    }

    @Test
    fun hasXrProjectedSystemService_whenServiceExists_returnsTrue() {
        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(PROJECTED_SERVICE_COMPONENT_NAME)
            addIntentFilterForService(
                PROJECTED_SERVICE_COMPONENT_NAME,
                IntentFilter(PackageManagerUtils.ACTION_BIND),
            )
            installPackage(PROJECTED_PACKAGE_INFO)
        }

        assertThat(PackageManagerUtils.hasXrProjectedSystemService(context)).isTrue()
    }

    @Test
    fun hasXrProjectedSystemService_whenServiceDoesNotExist_returnsFalse() {
        assertThat(PackageManagerUtils.hasXrProjectedSystemService(context)).isFalse()
    }

    @Test
    fun hasXrProjectedSystemService_whenServiceExistsButNotSystem_returnsFalse() {
        val nonSystemPackageName = "com.non_system.service"
        val nonSystemClassName = "com.non_system.service.ProjectedService"
        val nonSystemComponentName = ComponentName(nonSystemPackageName, nonSystemClassName)
        val nonSystemServiceInfo =
            ServiceInfo().apply {
                packageName = nonSystemPackageName
                name = nonSystemClassName
            }
        val nonSystemPackageInfo =
            PackageInfo().apply {
                packageName = nonSystemPackageName
                services = arrayOf(nonSystemServiceInfo)
                applicationInfo = ApplicationInfo().apply { flags = 0 }
            }

        shadowOf(context.packageManager).apply {
            addServiceIfNotPresent(nonSystemComponentName)
            addIntentFilterForService(
                nonSystemComponentName,
                IntentFilter(PackageManagerUtils.ACTION_BIND),
            )
            installPackage(nonSystemPackageInfo)
        }

        assertThat(PackageManagerUtils.hasXrProjectedSystemService(context)).isFalse()
    }

    companion object {
        private const val PROJECTED_SERVICE_PACKAGE_NAME = "com.system.service"
        private const val PROJECTED_SERVICE_CLASS_NAME = "com.system.service.ProjectedService"

        private val PROJECTED_SERVICE_COMPONENT_NAME: ComponentName =
            ComponentName(PROJECTED_SERVICE_PACKAGE_NAME, PROJECTED_SERVICE_CLASS_NAME)
        private val PROJECTED_SERVICE_INFO =
            ServiceInfo().apply {
                packageName = PROJECTED_SERVICE_PACKAGE_NAME
                name = PROJECTED_SERVICE_CLASS_NAME
            }
        private val PROJECTED_PACKAGE_INFO =
            PackageInfo().apply {
                packageName = PROJECTED_SERVICE_PACKAGE_NAME
                services = arrayOf(PROJECTED_SERVICE_INFO)
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }
    }
}
