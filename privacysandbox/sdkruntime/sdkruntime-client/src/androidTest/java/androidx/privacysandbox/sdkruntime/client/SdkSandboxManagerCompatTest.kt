/*
 * Copyright 2022 The Android Open Source Project
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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Binder
import android.os.Build
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.client.loader.extractSdkProviderFieldValue
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_INTERNAL_ERROR
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_SDK_DEFINED_ERROR
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkSandboxManagerCompatTest {

    @After
    fun tearDown() {
        SdkSandboxManagerCompat.reset()
    }

    @Test
    fun from_whenCalledOnSameContext_returnSameManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val managerCompat = SdkSandboxManagerCompat.from(context)
        val managerCompat2 = SdkSandboxManagerCompat.from(context)

        assertThat(managerCompat2).isSameInstanceAs(managerCompat)
    }

    @Test
    fun from_whenCalledOnDifferentContext_returnDifferentManager() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val context2 = ContextWrapper(context)

        val managerCompat = SdkSandboxManagerCompat.from(context)
        val managerCompat2 = SdkSandboxManagerCompat.from(context2)

        assertThat(managerCompat2).isNotSameInstanceAs(managerCompat)
    }

    @Test
    // TODO(b/249982507) DexmakerMockitoInline requires P+. Rewrite to support P-
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun loadSdk_whenNoLocalSdkExistsAndSandboxNotAvailable_dontDelegateToSandbox() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = SdkSandboxManagerCompat.from(context)

        assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("sdk-not-exists", Bundle())
            }
        }

        verify(context, Mockito.never()).getSystemService(any())
    }

    @Test
    fun loadSdk_whenNoLocalSdkExistsAndSandboxNotAvailable_throwsSdkNotFoundException() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("sdk-not-exists", Bundle())
            }
        }

        assertThat(result.loadSdkErrorCode)
            .isEqualTo(LoadSdkCompatException.LOAD_SDK_NOT_FOUND)
    }

    @Test
    fun loadSdk_whenLocalSdkExists_returnResultFromCompatLoadSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val result = runBlocking {
            managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", Bundle())
        }

        assertThat(result.getInterface()!!.javaClass.classLoader)
            .isNotSameInstanceAs(managerCompat.javaClass.classLoader)

        assertThat(result.getSdkInfo())
            .isEqualTo(
                SandboxedSdkInfo(
                    name = "androidx.privacysandbox.sdkruntime.test.v1",
                    version = 42
                )
            )
    }

    @Test
    fun loadSdk_whenLocalSdkExists_rethrowsExceptionFromCompatLoadSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val params = Bundle()
        params.putBoolean("needFail", true)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", params)
            }
        }

        assertThat(result.extraInformation).isEqualTo(params)
        assertThat(result.loadSdkErrorCode).isEqualTo(LOAD_SDK_SDK_DEFINED_ERROR)
    }

    @Test
    fun loadSdk_whenLocalSdkFailedToLoad_throwsInternalErrorException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val result = assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                managerCompat.loadSdk(
                    sdkName = "androidx.privacysandbox.sdkruntime.test.invalidEntryPoint",
                    params = Bundle()
                )
            }
        }

        assertThat(result.loadSdkErrorCode).isEqualTo(LOAD_SDK_INTERNAL_ERROR)
        assertThat(result.message).isEqualTo("Failed to instantiate local SDK")
    }

    @Test
    fun loadSdk_afterUnloading_loadSdkAgain() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val sdkName = "androidx.privacysandbox.sdkruntime.test.v1"

        val sdkToUnload = runBlocking {
            managerCompat.loadSdk(sdkName, Bundle())
        }

        managerCompat.unloadSdk(sdkName)

        val reloadedSdk = runBlocking {
            managerCompat.loadSdk(sdkName, Bundle())
        }

        assertThat(managerCompat.getSandboxedSdks())
            .containsExactly(reloadedSdk)
        assertThat(reloadedSdk.getInterface())
            .isNotEqualTo(sdkToUnload.getInterface())
    }

    @Test
    // TODO(b/249982507) DexmakerMockitoInline requires P+. Rewrite to support P-
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun unloadSdk_whenNoLocalSdkLoadedAndSandboxNotAvailable_dontDelegateToSandbox() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = SdkSandboxManagerCompat.from(context)

        managerCompat.unloadSdk("sdk-not-loaded")

        verify(context, Mockito.never()).getSystemService(any())
    }

    @Test
    fun unloadSdk_whenLocalSdkLoaded_unloadLocalSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val sdkName = "androidx.privacysandbox.sdkruntime.test.v1"

        runBlocking {
            managerCompat.loadSdk(sdkName, Bundle())
        }
        val sdkProvider = managerCompat.getLocallyLoadedSdk(sdkName)!!.sdkProvider

        managerCompat.unloadSdk(sdkName)

        val isBeforeUnloadSdkCalled = sdkProvider.extractSdkProviderFieldValue<Boolean>(
            fieldName = "isBeforeUnloadSdkCalled"
        )

        assertThat(isBeforeUnloadSdkCalled)
            .isTrue()

        assertThat(managerCompat.getSandboxedSdks())
            .isEmpty()
    }

    @Test
    // TODO(b/249982507) DexmakerMockitoInline requires P+. Rewrite to support P-
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun addSdkSandboxProcessDeathCallback_whenSandboxNotAvailable_dontDelegateToSandbox() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = SdkSandboxManagerCompat.from(context)

        managerCompat.addSdkSandboxProcessDeathCallback(Runnable::run, object :
            SdkSandboxProcessDeathCallbackCompat {
            override fun onSdkSandboxDied() {
            }
        })

        verify(context, Mockito.never()).getSystemService(any())
    }

    @Test
    // TODO(b/249982507) DexmakerMockitoInline requires P+. Rewrite to support P-
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun removeSdkSandboxProcessDeathCallback_whenSandboxNotAvailable_dontDelegateToSandbox() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = SdkSandboxManagerCompat.from(context)

        managerCompat.removeSdkSandboxProcessDeathCallback(object :
            SdkSandboxProcessDeathCallbackCompat {
            override fun onSdkSandboxDied() {
            }
        })

        verify(context, Mockito.never()).getSystemService(any())
    }

    @Test
    // TODO(b/249982507) DexmakerMockitoInline requires P+. Rewrite to support P-
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun getSandboxedSdks_whenSandboxNotAvailable_dontDelegateToSandbox() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = SdkSandboxManagerCompat.from(context)

        managerCompat.getSandboxedSdks()

        verify(context, Mockito.never()).getSystemService(any())
    }

    @Ignore("b/277764220")
    @Test
    fun startSdkSandboxActivity_whenSandboxNotAvailable_dontDelegateToSandbox() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val fromActivitySpy = Mockito.mock(Activity::class.java)
        managerCompat.startSdkSandboxActivity(fromActivitySpy, Binder())

        verify(context, Mockito.never()).getSystemService(any())
    }

    @Test
    fun sdkController_getSandboxedSdks_returnsLocallyLoadedSdks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val localSdk = runBlocking {
            managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v2", Bundle())
        }

        val anotherLocalSdk = runBlocking {
            managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", Bundle())
        }

        val testSdk = localSdk.asTestSdk()

        val interfaces = testSdk.getSandboxedSdks()
            .map { it.getInterface() }

        assertThat(interfaces).containsExactly(
            localSdk.getInterface(),
            anotherLocalSdk.getInterface(),
        )
    }

    @Test
    fun getSandboxedSdks_whenSandboxNotAvailable_returnsLocallyLoadedSdkList() {
        // TODO(b/262577044) Replace with @SdkSuppress after supporting maxExtensionVersion
        assumeTrue("Requires Sandbox API not available", isSandboxApiNotAvailable())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val localSdk = runBlocking {
            managerCompat.loadSdk("androidx.privacysandbox.sdkruntime.test.v1", Bundle())
        }

        val sandboxedSdks = managerCompat.getSandboxedSdks()

        assertThat(sandboxedSdks).containsExactly(localSdk)
    }

    private fun isSandboxApiNotAvailable() =
        !AdServicesInfo.isAtLeastV4()
}