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
import android.os.Build
import android.os.ext.SdkExtensions.AD_SERVICES
import androidx.annotation.RequiresExtension
import androidx.core.content.getSystemService
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedInterfaceConverter
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
// TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
@RequiresExtension(extension = AD_SERVICES, version = 4)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SdkSandboxManagerAppOwnedInterfacesTest {

    private lateinit var mContext: Context

    @Before
    fun setUp() {
        assumeTrue("Requires Sandbox API available", AdServicesInfo.isAtLeastV4())
        mContext = ApplicationProvider.getApplicationContext()
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
    fun registerAppOwnedSdkSandboxInterface_whenApiNotAvailable_throwsException() {
        assumeFalse(
            "Requires AppOwnedInterfacesApi API not available",
            isAppOwnedInterfacesApiAvailable()
        )

        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        assertThrows(UnsupportedOperationException::class.java) {
            managerCompat.registerAppOwnedSdkSandboxInterface(
                AppOwnedSdkSandboxInterfaceCompat(
                    name = "TestSDK",
                    version = 1,
                    binder = Binder()
                )
            )
        }
    }

    @Test
    fun registerAppOwnedSdkSandboxInterface_whenApiAvailable_delegateToPlatform() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            isAppOwnedInterfacesApiAvailable()
        )

        val managerCompat = SdkSandboxManagerCompat.from(mContext)
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        managerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val registeredInterfaces = getRegisteredInterfaces()
        assertThat(registeredInterfaces).hasSize(1)
        val result = registeredInterfaces[0]

        assertThat(result.getName()).isEqualTo(appOwnedInterface.getName())
        assertThat(result.getVersion()).isEqualTo(appOwnedInterface.getVersion())
        assertThat(result.getInterface()).isEqualTo(appOwnedInterface.getInterface())
    }

    @Test
    fun unregisterAppOwnedSdkSandboxInterface_whenApiNotAvailable_throwsException() {
        assumeFalse(
            "Requires AppOwnedInterfacesApi API not available",
            isAppOwnedInterfacesApiAvailable()
        )

        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        assertThrows(UnsupportedOperationException::class.java) {
            managerCompat.unregisterAppOwnedSdkSandboxInterface("TestSDK")
        }
    }

    @Test
    fun unregisterAppOwnedSdkSandboxInterface_whenApiAvailable_delegateToPlatform() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            isAppOwnedInterfacesApiAvailable()
        )

        val managerCompat = SdkSandboxManagerCompat.from(mContext)
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        managerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)
        managerCompat.unregisterAppOwnedSdkSandboxInterface(appOwnedInterface.getName())

        val registeredInterfaces = getRegisteredInterfaces()
        assertThat(registeredInterfaces).isEmpty()
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_whenApiNotAvailable_throwsException() {
        assumeFalse(
            "Requires AppOwnedInterfacesApi API not available",
            isAppOwnedInterfacesApiAvailable()
        )

        val managerCompat = SdkSandboxManagerCompat.from(mContext)

        assertThrows(UnsupportedOperationException::class.java) {
            managerCompat.getAppOwnedSdkSandboxInterfaces()
        }
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_whenApiAvailable_delegateToPlatform() {
        assumeTrue(
            "Requires AppOwnedInterfacesApi API available",
            isAppOwnedInterfacesApiAvailable()
        )

        val managerCompat = SdkSandboxManagerCompat.from(mContext)
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )
        managerCompat.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val results = managerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(results).hasSize(1)
        val result = results[0]

        assertThat(result.getName()).isEqualTo(appOwnedInterface.getName())
        assertThat(result.getVersion()).isEqualTo(appOwnedInterface.getVersion())
        assertThat(result.getInterface()).isEqualTo(appOwnedInterface.getInterface())
    }

    private fun getRegisteredInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> {
        val converter = AppOwnedInterfaceConverter()

        val sandboxManager = mContext.getSystemService<SdkSandboxManager>()!!
        val getInterfacesMethod = sandboxManager.javaClass.getMethod(
            "getAppOwnedSdkSandboxInterfaces"
        )

        val results = getInterfacesMethod.invoke(sandboxManager) as List<*>
        return results.map { converter.toCompat(it!!) }
    }

    private fun unregisterInterfaces(appOwnedInterfaces: List<AppOwnedSdkSandboxInterfaceCompat>) {
        val sandboxManager = mContext.getSystemService<SdkSandboxManager>()!!
        val unregisterMethod = sandboxManager.javaClass.getMethod(
            "unregisterAppOwnedSdkSandboxInterface",
            /* parameter1 */ String::class.java
        )

        appOwnedInterfaces.forEach { unregisterMethod.invoke(sandboxManager, it.getName()) }
    }

    private fun isAppOwnedInterfacesApiAvailable() =
        AdServicesInfo.isDeveloperPreview()
}