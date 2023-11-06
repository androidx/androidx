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

package androidx.privacysandbox.sdkruntime.client

import android.annotation.SuppressLint
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresExtension
import androidx.core.content.getSystemService
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests related to AppOwnedInterfaces support in SdkSandboxManagerCompat.
 * Later most of them will be extracted to E2E test with separate test app/sdk.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkSandboxManagerAppOwnedInterfacesTest {

    private lateinit var context: Context
    private lateinit var sandboxManagerCompat: SdkSandboxManagerCompat

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sandboxManagerCompat = SdkSandboxManagerCompat.from(context)
    }

    @SuppressLint("NewApi", "ClassVerificationFailure") // For supporting DP Builds
    @After
    fun tearDown() {
        SdkSandboxManagerCompat.reset()
        if (isAppOwnedInterfacesApiAvailable()) {
            val registeredInterfaces = getRegisteredInterfaces(context)
            unregisterInterfaces(context, registeredInterfaces)
        }
    }

    @Test
    fun registerAppOwnedSdkSandboxInterfaceTest() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sandboxManagerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val registeredInterfaces = sandboxManagerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(registeredInterfaces).hasSize(1)
        val result = registeredInterfaces[0]

        assertThat(result.getName()).isEqualTo(appOwnedInterface.getName())
        assertThat(result.getVersion()).isEqualTo(appOwnedInterface.getVersion())
        assertThat(result.getInterface()).isEqualTo(appOwnedInterface.getInterface())
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun registerAppOwnedSdkSandboxInterface_whenApiAvailable_registerInPlatform() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            isAppOwnedInterfacesApiAvailable()
        )

        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sandboxManagerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)
        val platformRegisteredInterfaces = getRegisteredInterfaces(context)
        assertThat(platformRegisteredInterfaces).hasSize(1)
        assertThat(platformRegisteredInterfaces[0].getName()).isEqualTo(appOwnedInterface.getName())
    }

    @Test
    fun unregisterAppOwnedSdkSandboxInterfaceTest() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sandboxManagerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)
        sandboxManagerCompat.unregisterAppOwnedSdkSandboxInterface(appOwnedInterface.getName())

        val registeredInterfaces = sandboxManagerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(registeredInterfaces).isEmpty()
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun unregisterAppOwnedSdkSandboxInterface_whenApiAvailable_unregisterFromPlatform() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            isAppOwnedInterfacesApiAvailable()
        )

        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sandboxManagerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)
        sandboxManagerCompat.unregisterAppOwnedSdkSandboxInterface(appOwnedInterface.getName())

        val platformRegisteredInterfaces = getRegisteredInterfaces(context)
        assertThat(platformRegisteredInterfaces).isEmpty()
    }

    @Test
    fun sdkController_getAppOwnedSdkSandboxInterfaces_returnsRegisteredAppOwnedInterfaces() {
        val localSdk = runBlocking {
            sandboxManagerCompat.loadSdk(
                TestSdkConfigs.forSdkName("v4").packageName,
                Bundle()
            )
        }

        val registeredAppOwnedSdk = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )
        sandboxManagerCompat.registerAppOwnedSdkSandboxInterface(registeredAppOwnedSdk)

        val testSdk = localSdk.asTestSdk()

        val apiResult = testSdk.getAppOwnedSdkSandboxInterfaces()
        assertThat(apiResult).hasSize(1)
        val appOwnedSdkResult = apiResult[0]

        assertThat(appOwnedSdkResult.getName()).isEqualTo(registeredAppOwnedSdk.getName())
        assertThat(appOwnedSdkResult.getVersion()).isEqualTo(registeredAppOwnedSdk.getVersion())
        assertThat(appOwnedSdkResult.getInterface()).isEqualTo(registeredAppOwnedSdk.getInterface())
    }

    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    companion object AppOwnedInterfacesApi { // to avoid class verification fails
        @DoNotInline
        fun getRegisteredInterfaces(
            context: Context
        ): List<AppOwnedSdkSandboxInterfaceCompat> {
            val sandboxManager = context.getSystemService<SdkSandboxManager>()!!
            val results = sandboxManager.getAppOwnedSdkSandboxInterfaces()
            return results.map { AppOwnedSdkSandboxInterfaceCompat(it) }
        }

        @DoNotInline
        fun unregisterInterfaces(
            context: Context,
            appOwnedInterfaces: List<AppOwnedSdkSandboxInterfaceCompat>
        ) {
            val sandboxManager = context.getSystemService<SdkSandboxManager>()!!
            appOwnedInterfaces.forEach {
                sandboxManager.unregisterAppOwnedSdkSandboxInterface(it.getName())
            }
        }
    }

    private fun isAppOwnedInterfacesApiAvailable() =
        BuildCompat.AD_SERVICES_EXTENSION_INT >= 8 || AdServicesInfo.isDeveloperPreview()
}
