/*
 * Copyright 2024 The Android Open Source Project
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

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.TestSdkConfigs
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.client.loader.CatchingClientImportanceListener
import androidx.privacysandbox.sdkruntime.client.loader.CatchingSdkActivityHandler
import androidx.privacysandbox.sdkruntime.client.loader.VersionHandshake
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.client.loader.extractSdkProviderFieldValue
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.privacysandbox.sdkruntime.core.controller.LoadSdkCallback
import androidx.privacysandbox.sdkruntime.core.internal.ClientApiVersion
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class LocalSdkRegistryTest {

    private lateinit var context: Context
    private lateinit var localSdkRegistry: LocalSdkRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        localSdkRegistry = LocalSdkRegistry.create(context, LocalAppOwnedSdkRegistry())
    }

    @Test
    fun isResponsibleFor_LocalSdk_returnsTrue() {
        val result = localSdkRegistry.isResponsibleFor(TestSdkConfigs.CURRENT.packageName)
        assertThat(result).isTrue()
    }

    @Test
    fun isResponsibleFor_NonLocalSdk_returnsFalse() {
        val result = localSdkRegistry.isResponsibleFor("non-local-sdk")
        assertThat(result).isFalse()
    }

    @Test
    fun loadSdk_whenLocalSdkExists_returnsLocallyLoadedSdk() {
        val result = localSdkRegistry.loadTestSdk()

        assertThat(result.getInterface()!!.javaClass.classLoader)
            .isNotSameInstanceAs(localSdkRegistry.javaClass.classLoader)

        assertThat(result.getSdkInfo())
            .isEqualTo(SandboxedSdkInfo(name = TestSdkConfigs.CURRENT.packageName, version = 42))

        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(result)
    }

    @Test
    fun loadSdk_fromNonMainThread_withLoadSdkInOnLoadSdk_loadBothSdksWithoutDeadlocks() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        val dependencyPackageName = TestSdkConfigs.forSdkName("v8").packageName

        localSdkRegistry.loadTestSdk(params = sdkDependencyParams(dependencyPackageName))

        val loadedSdkNames = localSdkRegistry.getLoadedSdks().map { sdk -> sdk.getSdkInfo()!!.name }
        assertThat(loadedSdkNames).containsExactly(sdkName, dependencyPackageName)
        assertThat(localSdkRegistry.isLoading(sdkName)).isFalse()
        assertThat(localSdkRegistry.isLoading(dependencyPackageName)).isFalse()
    }

    @Test
    fun loadSdk_fromMainThread_withLoadSdkInOnLoadSdk_loadBothSdksWithoutDeadlocks() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        val dependencyPackageName = TestSdkConfigs.forSdkName("v8").packageName

        localSdkRegistry.loadTestSdk(params = sdkDependencyParams(dependencyPackageName))

        val loadedSdkNames = localSdkRegistry.getLoadedSdks().map { sdk -> sdk.getSdkInfo()!!.name }
        assertThat(loadedSdkNames).containsExactly(sdkName, dependencyPackageName)
        assertThat(localSdkRegistry.isLoading(sdkName)).isFalse()
        assertThat(localSdkRegistry.isLoading(dependencyPackageName)).isFalse()
    }

    @Test
    fun loadSdk_whenLocalSdkExists_rethrowsExceptionFromLocallyLoadedSdk() {
        val params = failParams()

        val result =
            assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadTestSdk(params = params)
            }

        assertThat(result.extraInformation).isEqualTo(params)
        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_SDK_DEFINED_ERROR)
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_whenLocalSdkFailedToLoad_throwsInternalErrorException() {
        val result =
            assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadTestSdk(
                    TestSdkConfigs.forSdkName("invalidEntryPoint").packageName
                )
            }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR)
        assertThat(result.message).isEqualTo("Failed to instantiate local SDK")
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_whenSdkAlreadyLoaded_throwsSdkAlreadyLoadedException() {
        val firstTimeLoadedSdk = localSdkRegistry.loadTestSdk()

        val result =
            assertThrows(LoadSdkCompatException::class.java) { localSdkRegistry.loadTestSdk() }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_ALREADY_LOADED)
        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(firstTimeLoadedSdk)
    }

    @Test
    fun loadSdk_whenSdkLoading_throwsSdkAlreadyLoadedException() {
        val registry =
            LocalSdkRegistry.create(
                context,
                LocalAppOwnedSdkRegistry(),
                mainThreadExecutor = {
                    // do nothing to simulate sdk loading
                },
            )
        val sdkName = TestSdkConfigs.CURRENT.packageName
        registry.loadSdk(
            sdkName,
            Bundle(),
            Runnable::run,
            object : LoadSdkCallback {
                override fun onResult(result: SandboxedSdkCompat) {
                    Assert.fail("Unexpected result")
                }

                override fun onError(error: LoadSdkCompatException) {
                    Assert.fail("Unexpected exception")
                }
            },
        )
        assertThat(registry.isLoading(sdkName)).isTrue()
        assertThat(registry.getLoadedSdks()).isEmpty()

        val result = assertThrows(LoadSdkCompatException::class.java) { registry.loadTestSdk() }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_ALREADY_LOADED)
        assertThat(registry.isLoading(sdkName)).isTrue()
        assertThat(registry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_whenNoLocalSdkExists_throwsSdkNotFoundException() {
        val result =
            assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadTestSdk("sdk-doesnt-exist")
            }

        assertThat(result.loadSdkErrorCode).isEqualTo(LoadSdkCompatException.LOAD_SDK_NOT_FOUND)
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_afterUnloading_loadSdkAgain() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        val sdkToUnload = localSdkRegistry.loadTestSdk()

        localSdkRegistry.unloadSdk(sdkName)
        val reloadedSdk = localSdkRegistry.loadTestSdk()

        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(reloadedSdk)
        assertThat(reloadedSdk.getInterface()).isNotEqualTo(sdkToUnload.getInterface())
    }

    @Test
    fun loadSdk_afterFailedLoad_loadSdk() {
        assertThrows(LoadSdkCompatException::class.java) {
            localSdkRegistry.loadTestSdk(params = failParams())
        }
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()

        val loadedSdk = localSdkRegistry.loadTestSdk()

        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(loadedSdk)
    }

    @Test
    fun unloadSdk_whenLocalSdkLoaded_unloadLocallyLoadedSdk() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        localSdkRegistry.loadTestSdk()
        val sdkProvider = localSdkRegistry.getLoadedSdkProvider(sdkName)!!

        localSdkRegistry.unloadSdk(sdkName)

        val isBeforeUnloadSdkCalled =
            sdkProvider.extractSdkProviderFieldValue<Boolean>(fieldName = "isBeforeUnloadSdkCalled")
        assertThat(isBeforeUnloadSdkCalled).isTrue()
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun unloadSdk_whenSdkLoading_throwsIllegalArgumentException() {
        val registry =
            LocalSdkRegistry.create(
                context,
                LocalAppOwnedSdkRegistry(),
                mainThreadExecutor = {
                    // do nothing to simulate sdk loading
                },
            )

        val sdkName = TestSdkConfigs.CURRENT.packageName
        registry.loadSdk(
            sdkName,
            Bundle(),
            Runnable::run,
            object : LoadSdkCallback {
                override fun onResult(result: SandboxedSdkCompat) {
                    Assert.fail("Unexpected result")
                }

                override fun onError(error: LoadSdkCompatException) {
                    Assert.fail("Unexpected exception")
                }
            },
        )
        assertThat(registry.isLoading(sdkName)).isTrue()
        assertThat(registry.getLoadedSdks()).isEmpty()

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { registry.unloadSdk(sdkName) }
        }

        assertThat(registry.isLoading(sdkName)).isTrue()
    }

    @Test
    fun unloadSdk_whenNoLocalSdkLoaded_doesntThrow() {
        localSdkRegistry.unloadSdk(TestSdkConfigs.CURRENT.packageName)
    }

    @Test
    fun unloadSdk_unregisterActivityHandlers() {
        val packageName = TestSdkConfigs.CURRENT.packageName
        val localSdk = localSdkRegistry.loadTestSdk()

        val testSdk = localSdk.asTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(CatchingSdkActivityHandler())

        val registeredBefore = LocalSdkActivityHandlerRegistry.isRegistered(token)
        assertThat(registeredBefore).isTrue()

        localSdkRegistry.unloadSdk(packageName)

        val registeredAfter = LocalSdkActivityHandlerRegistry.isRegistered(token)
        assertThat(registeredAfter).isFalse()
    }

    @Test
    fun unloadSdk_unregisterClientImportanceListeners() {
        val packageName = TestSdkConfigs.CURRENT.packageName
        val localSdk = runBlocking {
            localSdkRegistry.loadSdkWithFeature(ClientFeature.CLIENT_IMPORTANCE_LISTENER)
        }
        val testSdk = localSdk.asTestSdk()

        val listener = CatchingClientImportanceListener()
        testSdk.registerSdkSandboxClientImportanceListener(listener)

        val registeredBefore = LocalClientImportanceListenerRegistry.hasListenersForSdk(packageName)
        assertThat(registeredBefore).isTrue()

        localSdkRegistry.unloadSdk(packageName)

        val registeredAfter = LocalClientImportanceListenerRegistry.hasListenersForSdk(packageName)
        assertThat(registeredAfter).isFalse()
    }

    private fun LocalSdkRegistry.loadTestSdk(
        sdkName: String = TestSdkConfigs.CURRENT.packageName,
        params: Bundle = Bundle(),
    ): SandboxedSdkCompat = runBlocking { loadSdk(sdkName, params) }

    private fun failParams(): Bundle {
        val result = Bundle()
        result.putBoolean("needFail", true)
        return result
    }

    private fun sdkDependencyParams(dependencyPackageName: String): Bundle {
        val result = Bundle()
        result.putString("dependencySdkToLoad", dependencyPackageName)
        return result
    }

    private suspend fun LocalSdkRegistry.loadSdkWithFeature(
        clientFeature: ClientFeature
    ): SandboxedSdkCompat {
        return if (clientFeature.availableFrom <= ClientApiVersion.CURRENT_VERSION) {
            loadSdk(TestSdkConfigs.CURRENT.packageName, Bundle())
        } else {
            val customHandshake =
                VersionHandshake(
                    overrideClientVersion = clientFeature.availableFrom.apiLevel,
                    overrideSdkVersion = clientFeature.availableFrom.apiLevel,
                )
            loadSdk(TestSdkConfigs.CURRENT.packageName, Bundle(), customHandshake)
        }
    }
}
