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

package androidx.privacysandbox.sdkruntime.client.controller

import android.content.Context
import android.os.Binder
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalControllerTest {

    private lateinit var applicationContext: Context
    private lateinit var localSdkRegistry: StubLocalSdkRegistry
    private lateinit var appOwnedSdkRegistry: StubAppOwnedSdkInterfaceRegistry
    private lateinit var controller: LocalController

    @Before
    fun setUp() {
        applicationContext = ApplicationProvider.getApplicationContext()
        localSdkRegistry = StubLocalSdkRegistry()
        appOwnedSdkRegistry = StubAppOwnedSdkInterfaceRegistry()
        controller =
            LocalController(
                SDK_PACKAGE_NAME,
                applicationContext,
                localSdkRegistry,
                appOwnedSdkRegistry
            )
    }

    @Test
    fun loadSdk_whenSdkRegistryReturnsResult_returnResultFromSdkRegistry() {
        val expectedResult = SandboxedSdkCompat(Binder())
        localSdkRegistry.loadSdkResult = expectedResult

        val sdkParams = Bundle()
        val callback = StubLoadSdkCallback()

        controller.loadSdk(SDK_PACKAGE_NAME, sdkParams, Runnable::run, callback)

        assertThat(callback.lastResult).isEqualTo(expectedResult)
        assertThat(callback.lastError).isNull()

        assertThat(localSdkRegistry.lastLoadSdkName).isEqualTo(SDK_PACKAGE_NAME)
        assertThat(localSdkRegistry.lastLoadSdkParams).isSameInstanceAs(sdkParams)
    }

    @Test
    fun loadSdk_whenSdkRegistryThrowsException_rethrowsExceptionFromSdkRegistry() {
        val expectedError = LoadSdkCompatException(RuntimeException(), Bundle())
        localSdkRegistry.loadSdkError = expectedError

        val callback = StubLoadSdkCallback()

        controller.loadSdk(SDK_PACKAGE_NAME, Bundle(), Runnable::run, callback)

        assertThat(callback.lastError).isEqualTo(expectedError)
        assertThat(callback.lastResult).isNull()
    }

    @Test
    fun getSandboxedSdks_returnsResultsFromLocalSdkRegistry() {
        val sandboxedSdk = SandboxedSdkCompat(Binder())
        localSdkRegistry.getLoadedSdksResult = listOf(sandboxedSdk)

        val result = controller.getSandboxedSdks()
        assertThat(result).containsExactly(sandboxedSdk)
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_returnsResultsFromAppOwnedSdkRegistry() {
        val appOwnedInterface =
            AppOwnedSdkSandboxInterfaceCompat(name = "TestSDK", version = 1, binder = Binder())
        appOwnedSdkRegistry.appOwnedSdks = listOf(appOwnedInterface)

        val result = controller.getAppOwnedSdkSandboxInterfaces()
        assertThat(result).containsExactly(appOwnedInterface)
    }

    @Test
    fun registerSdkSandboxActivityHandler_delegateToLocalSdkActivityHandlerRegistry() {
        val handler =
            object : SdkSandboxActivityHandlerCompat {
                override fun onActivityCreated(activityHolder: ActivityHolder) {
                    // do nothing
                }
            }

        val token = controller.registerSdkSandboxActivityHandler(handler)

        val registeredHandler = LocalSdkActivityHandlerRegistry.getHandlerByToken(token)
        assertThat(registeredHandler).isSameInstanceAs(handler)
    }

    @Test
    fun registerSdkSandboxActivityHandler_registerWithCorrectSdkPackageName() {
        val token =
            controller.registerSdkSandboxActivityHandler(
                object : SdkSandboxActivityHandlerCompat {
                    override fun onActivityCreated(activityHolder: ActivityHolder) {
                        // do nothing
                    }
                }
            )

        val anotherSdkController =
            LocalController(
                "LocalControllerTest.anotherSdk",
                applicationContext,
                localSdkRegistry,
                appOwnedSdkRegistry
            )
        val anotherSdkHandler =
            object : SdkSandboxActivityHandlerCompat {
                override fun onActivityCreated(activityHolder: ActivityHolder) {
                    // do nothing
                }
            }
        val anotherSdkToken =
            anotherSdkController.registerSdkSandboxActivityHandler(anotherSdkHandler)

        LocalSdkActivityHandlerRegistry.unregisterAllActivityHandlersForSdk(SDK_PACKAGE_NAME)

        assertThat(LocalSdkActivityHandlerRegistry.isRegistered(token)).isFalse()
        assertThat(LocalSdkActivityHandlerRegistry.isRegistered(anotherSdkToken)).isTrue()
    }

    @Test
    fun unregisterSdkSandboxActivityHandler_delegateToLocalSdkActivityHandlerRegistry() {
        val handler =
            object : SdkSandboxActivityHandlerCompat {
                override fun onActivityCreated(activityHolder: ActivityHolder) {
                    // do nothing
                }
            }

        val token = controller.registerSdkSandboxActivityHandler(handler)
        controller.unregisterSdkSandboxActivityHandler(handler)

        val registeredHandler = LocalSdkActivityHandlerRegistry.getHandlerByToken(token)
        assertThat(registeredHandler).isNull()
    }

    @Test
    fun getClientPackageName_returnsAppPackageName() {
        val result = controller.getClientPackageName()
        assertThat(result).isEqualTo(applicationContext.getPackageName())
    }

    private class StubLocalSdkRegistry : SdkRegistry {

        var getLoadedSdksResult: List<SandboxedSdkCompat> = emptyList()

        var loadSdkResult: SandboxedSdkCompat? = null
        var loadSdkError: LoadSdkCompatException? = null

        var lastLoadSdkName: String? = null
        var lastLoadSdkParams: Bundle? = null

        override fun isResponsibleFor(sdkName: String): Boolean {
            throw IllegalStateException("Unexpected call")
        }

        override fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat {
            lastLoadSdkName = sdkName
            lastLoadSdkParams = params

            if (loadSdkError != null) {
                throw loadSdkError!!
            }

            return loadSdkResult!!
        }

        override fun unloadSdk(sdkName: String) {
            throw IllegalStateException("Unexpected call")
        }

        override fun getLoadedSdks(): List<SandboxedSdkCompat> = getLoadedSdksResult
    }

    private class StubLoadSdkCallback : LoadSdkCallback {

        var lastResult: SandboxedSdkCompat? = null
        var lastError: LoadSdkCompatException? = null

        override fun onResult(result: SandboxedSdkCompat) {
            lastResult = result
        }

        override fun onError(error: LoadSdkCompatException) {
            lastError = error
        }
    }

    private class StubAppOwnedSdkInterfaceRegistry : AppOwnedSdkRegistry {

        var appOwnedSdks: List<AppOwnedSdkSandboxInterfaceCompat> = emptyList()

        override fun registerAppOwnedSdkSandboxInterface(
            appOwnedSdk: AppOwnedSdkSandboxInterfaceCompat
        ) {
            throw IllegalStateException("Unexpected call")
        }

        override fun unregisterAppOwnedSdkSandboxInterface(sdkName: String) {
            throw IllegalStateException("Unexpected call")
        }

        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
            appOwnedSdks
    }

    companion object {
        private const val SDK_PACKAGE_NAME = "LocalControllerTest.sdk"
    }
}
