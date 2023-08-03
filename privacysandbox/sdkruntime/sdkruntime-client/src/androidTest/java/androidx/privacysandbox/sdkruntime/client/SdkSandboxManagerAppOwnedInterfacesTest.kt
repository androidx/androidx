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

import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Binder
import android.os.Bundle
import androidx.core.content.getSystemService
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedInterfaceConverter
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @After
    fun tearDown() {
        SdkSandboxManagerCompat.reset()
        if (isAppOwnedInterfacesApiAvailable()) {
            val registeredInterfaces = getRegisteredInterfaces()
            unregisterInterfaces(registeredInterfaces)
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
        val platformRegisteredInterfaces = getRegisteredInterfaces()
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

        val platformRegisteredInterfaces = getRegisteredInterfaces()
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

    private fun getRegisteredInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        val converter = AppOwnedInterfaceConverter()

        val sandboxManager = context.getSystemService<SdkSandboxManager>()!!
        val getInterfacesMethod = sandboxManager.javaClass.getMethod(
            "getAppOwnedSdkSandboxInterfaces"
        )

        val results = getInterfacesMethod.invoke(sandboxManager) as List<*>
        return results.map { converter.toCompat(it!!) }
    }

    private fun unregisterInterfaces(appOwnedInterfaces: List<AppOwnedSdkSandboxInterfaceCompat>) {
        val sandboxManager = context.getSystemService<SdkSandboxManager>()!!
        val unregisterMethod = sandboxManager.javaClass.getMethod(
            "unregisterAppOwnedSdkSandboxInterface",
            /* parameter1 */ String::class.java
        )

        appOwnedInterfaces.forEach { unregisterMethod.invoke(sandboxManager, it.getName()) }
    }

    private fun isAppOwnedInterfacesApiAvailable() =
        AdServicesInfo.isDeveloperPreview()
}
