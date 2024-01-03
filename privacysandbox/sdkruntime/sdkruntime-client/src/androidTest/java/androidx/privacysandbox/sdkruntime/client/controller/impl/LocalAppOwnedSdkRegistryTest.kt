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

import android.os.Binder
import androidx.privacysandbox.sdkruntime.client.controller.AppOwnedSdkRegistry
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalAppOwnedSdkRegistryTest {

    private lateinit var sdkRegistry: AppOwnedSdkRegistry

    @Before
    fun setUp() {
        sdkRegistry = LocalAppOwnedSdkRegistry()
    }

    @Test
    fun registerAppOwnedSdkSandboxInterfaceTest() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val registeredInterfaces = sdkRegistry.getAppOwnedSdkSandboxInterfaces()
        assertThat(registeredInterfaces).hasSize(1)
        val result = registeredInterfaces[0]

        assertThat(result.getName()).isEqualTo(appOwnedInterface.getName())
        assertThat(result.getVersion()).isEqualTo(appOwnedInterface.getVersion())
        assertThat(result.getInterface()).isEqualTo(appOwnedInterface.getInterface())
    }

    @Test
    fun registerAppOwnedSdkSandboxInterface_whenAlreadyRegistered_throwsIllegalStateException() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedInterface)

        val interfaceWithSameName = AppOwnedSdkSandboxInterfaceCompat(
            name = appOwnedInterface.getName(),
            version = 1,
            binder = Binder()
        )
        assertThrows<IllegalStateException> {
            sdkRegistry.registerAppOwnedSdkSandboxInterface(interfaceWithSameName)
        }
    }

    @Test
    fun unregisterAppOwnedSdkSandboxInterfaceTest() {
        val appOwnedInterface = AppOwnedSdkSandboxInterfaceCompat(
            name = "TestSDK",
            version = 1,
            binder = Binder()
        )

        sdkRegistry.registerAppOwnedSdkSandboxInterface(appOwnedInterface)
        sdkRegistry.unregisterAppOwnedSdkSandboxInterface(appOwnedInterface.getName())

        val registeredInterfaces = sdkRegistry.getAppOwnedSdkSandboxInterfaces()
        assertThat(registeredInterfaces).isEmpty()
    }
}
