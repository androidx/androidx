/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.sdkruntime.client.controller.impl

import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresExtension
import androidx.core.content.getSystemService
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
@RequiresExtension(extension = AD_SERVICES, version = 8)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class PlatformAppOwnedSdkRegistryTest {

    private lateinit var context: Context
    private lateinit var sdkRegistry: AppOwnedSdkRegistry

    @Before
    fun setUp() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            isAppOwnedInterfacesApiAvailable()
        )
        context = ApplicationProvider.getApplicationContext()
        sdkRegistry = PlatformAppOwnedSdkRegistry(context)
    }

    @After
    fun tearDown() {
        if (isAppOwnedInterfacesApiAvailable()) {
            val registeredInterfaces = getRegisteredInterfaces()
            unregisterInterfaces(registeredInterfaces)
        }
    }

    @Test
    fun registerAppOwnedSdkSandboxInterface_registerInPlatform() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val registeredInterfaces = getRegisteredInterfaces()
        assertThat(registeredInterfaces).hasSize(1)
        val result = registeredInterfaces[0]

        assertThat(result.getName()).isEqualTo(appOwnedInterface.getName())
        assertThat(result.getVersion()).isEqualTo(appOwnedInterface.getVersion())
        assertThat(result.getInterface()).isEqualTo(appOwnedInterface.getInterface())
    }

    @Test
    fun unregisterAppOwnedSdkSandboxInterface_unregisterFromPlatform() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedInterface)
        sdkRegistry.unregisterAppOwnedSdkSandboxInterface(appOwnedInterface.getName())

        val registeredInterfaces = getRegisteredInterfaces()
        assertThat(registeredInterfaces).isEmpty()
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_returnsRegisteredSdkInterfaces() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )
        sdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val results = sdkRegistry.getAppOwnedSdkSandboxInterfaces()
        assertThat(results).hasSize(1)
        val result = results[0]

        assertThat(result.getName()).isEqualTo(appOwnedInterface.getName())
        assertThat(result.getVersion()).isEqualTo(appOwnedInterface.getVersion())
        assertThat(result.getInterface()).isEqualTo(appOwnedInterface.getInterface())
    }

    private fun getRegisteredInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        val sandboxManager = context.getSystemService<SdkSandboxManager>()!!
        val results = sandboxManager.getAppOwnedSdkSandboxInterfaces()
        return results.map { AppOwnedSdkSandboxInterfaceCompat(it) }
    }

    private fun unregisterInterfaces(appOwnedInterfaces: List<AppOwnedSdkSandboxInterfaceCompat>) {
        val sandboxManager = context.getSystemService<SdkSandboxManager>()!!
        appOwnedInterfaces.forEach {
            sandboxManager.unregisterAppOwnedSdkSandboxInterface(it.getName())
        }
    }

    private fun isAppOwnedInterfacesApiAvailable() =
        BuildCompat.AD_SERVICES_EXTENSION_INT >= 8 || AdServicesInfo.isDeveloperPreview()
}
