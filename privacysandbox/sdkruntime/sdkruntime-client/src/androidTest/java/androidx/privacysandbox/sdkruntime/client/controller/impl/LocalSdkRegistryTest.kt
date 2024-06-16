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
import androidx.privacysandbox.sdkruntime.client.loader.CatchingSdkActivityHandler
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.client.loader.extractSdkProviderFieldValue
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
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
        val result = localSdkRegistry.loadSdk(TestSdkConfigs.CURRENT.packageName, Bundle())

        assertThat(result.getInterface()!!.javaClass.classLoader)
            .isNotSameInstanceAs(localSdkRegistry.javaClass.classLoader)

        assertThat(result.getSdkInfo())
            .isEqualTo(SandboxedSdkInfo(name = TestSdkConfigs.CURRENT.packageName, version = 42))

        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(result)
    }

    @Test
    fun loadSdk_whenLocalSdkExists_rethrowsExceptionFromLocallyLoadedSdk() {
        val params = Bundle()
        params.putBoolean("needFail", true)

        val result =
            Assert.assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadSdk(TestSdkConfigs.CURRENT.packageName, params)
            }

        assertThat(result.extraInformation).isEqualTo(params)
        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_SDK_DEFINED_ERROR)
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_whenLocalSdkFailedToLoad_throwsInternalErrorException() {
        val result =
            Assert.assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadSdk(
                    TestSdkConfigs.forSdkName("invalidEntryPoint").packageName,
                    Bundle()
                )
            }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR)
        assertThat(result.message).isEqualTo("Failed to instantiate local SDK")
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_whenSdkAlreadyLoaded_throwsSdkAlreadyLoadedException() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        val firstTimeLoadedSdk = localSdkRegistry.loadSdk(sdkName, Bundle())

        val result =
            Assert.assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadSdk(sdkName, Bundle())
            }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_ALREADY_LOADED)
        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(firstTimeLoadedSdk)
    }

    @Test
    fun loadSdk_whenNoLocalSdkExists_throwsSdkNotFoundException() {
        val result =
            Assert.assertThrows(LoadSdkCompatException::class.java) {
                localSdkRegistry.loadSdk("sdk-doesnt-exist", Bundle())
            }

        assertThat(result.loadSdkErrorCode).isEqualTo(LoadSdkCompatException.LOAD_SDK_NOT_FOUND)
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun loadSdk_afterUnloading_loadSdkAgain() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        val sdkToUnload = localSdkRegistry.loadSdk(sdkName, Bundle())

        localSdkRegistry.unloadSdk(sdkName)
        val reloadedSdk = localSdkRegistry.loadSdk(sdkName, Bundle())

        assertThat(localSdkRegistry.getLoadedSdks()).containsExactly(reloadedSdk)
        assertThat(reloadedSdk.getInterface()).isNotEqualTo(sdkToUnload.getInterface())
    }

    @Test
    fun unloadSdk_whenLocalSdkLoaded_unloadLocallyLoadedSdk() {
        val sdkName = TestSdkConfigs.CURRENT.packageName
        localSdkRegistry.loadSdk(sdkName, Bundle())
        val sdkProvider = localSdkRegistry.getLoadedSdkProvider(sdkName)!!

        localSdkRegistry.unloadSdk(sdkName)

        val isBeforeUnloadSdkCalled =
            sdkProvider.extractSdkProviderFieldValue<Boolean>(fieldName = "isBeforeUnloadSdkCalled")
        assertThat(isBeforeUnloadSdkCalled).isTrue()
        assertThat(localSdkRegistry.getLoadedSdks()).isEmpty()
    }

    @Test
    fun unloadSdk_whenNoLocalSdkLoaded_doesntThrow() {
        localSdkRegistry.unloadSdk(TestSdkConfigs.CURRENT.packageName)
    }

    @Test
    fun unloadSdk_unregisterActivityHandlers() {
        val packageName = TestSdkConfigs.CURRENT.packageName
        val localSdk = localSdkRegistry.loadSdk(packageName, Bundle())

        val testSdk = localSdk.asTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(CatchingSdkActivityHandler())

        val registeredBefore = LocalSdkActivityHandlerRegistry.isRegistered(token)
        assertThat(registeredBefore).isTrue()

        localSdkRegistry.unloadSdk(packageName)

        val registeredAfter = LocalSdkActivityHandlerRegistry.isRegistered(token)
        assertThat(registeredAfter).isFalse()
    }
}
