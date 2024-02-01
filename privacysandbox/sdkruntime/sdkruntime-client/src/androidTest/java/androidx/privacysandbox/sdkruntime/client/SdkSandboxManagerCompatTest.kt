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

import android.content.Context
import android.content.ContextWrapper
import android.os.Binder
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.activity.LocalSdkActivityHandlerRegistry
import androidx.privacysandbox.sdkruntime.client.activity.SdkActivity
import androidx.privacysandbox.sdkruntime.client.loader.CatchingSdkActivityHandler
import androidx.privacysandbox.sdkruntime.client.loader.asTestSdk
import androidx.privacysandbox.sdkruntime.client.loader.extractSdkProviderFieldValue
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_INTERNAL_ERROR
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException.Companion.LOAD_SDK_SDK_DEFINED_ERROR
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

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
    @SdkSuppress(maxSdkVersion = 33)
    fun loadSdk_whenNoLocalSdkExistsAndApiBelow34_throwsSdkNotFoundException() {
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
            managerCompat.loadSdk(
                TestSdkConfigs.CURRENT.packageName,
                Bundle()
            )
        }

        assertThat(result.getInterface()!!.javaClass.classLoader)
            .isNotSameInstanceAs(managerCompat.javaClass.classLoader)

        assertThat(result.getSdkInfo())
            .isEqualTo(
                SandboxedSdkInfo(
                    name = TestSdkConfigs.CURRENT.packageName,
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
                managerCompat.loadSdk(
                    TestSdkConfigs.CURRENT.packageName,
                    params
                )
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
                    TestSdkConfigs.forSdkName("invalidEntryPoint").packageName,
                    Bundle()
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

        val sdkName = TestSdkConfigs.CURRENT.packageName

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
    @SdkSuppress(maxSdkVersion = 33)
    fun unloadSdk_whenNoLocalSdkLoadedAndApiBelow34_doesntThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)
        managerCompat.unloadSdk("sdk-not-loaded")
    }

    @Test
    fun unloadSdk_whenLocalSdkLoaded_unloadLocalSdk() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val sdkName = TestSdkConfigs.CURRENT.packageName

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
    fun unloadSdk_unregisterActivityHandlers() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val packageName = TestSdkConfigs.forSdkName("v4").packageName
        val localSdk = runBlocking {
            managerCompat.loadSdk(
                packageName,
                Bundle()
            )
        }

        val testSdk = localSdk.asTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(CatchingSdkActivityHandler())

        val registeredBefore = LocalSdkActivityHandlerRegistry.isRegistered(token)
        assertThat(registeredBefore).isTrue()

        managerCompat.unloadSdk(packageName)

        val registeredAfter = LocalSdkActivityHandlerRegistry.isRegistered(token)
        assertThat(registeredAfter).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun addSdkSandboxProcessDeathCallback_whenApiBelow34_doesntThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        managerCompat.addSdkSandboxProcessDeathCallback(Runnable::run, object :
            SdkSandboxProcessDeathCallbackCompat {
            override fun onSdkSandboxDied() {
            }
        })
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun removeSdkSandboxProcessDeathCallback_whenApiBelow34_doesntThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        managerCompat.removeSdkSandboxProcessDeathCallback(object :
            SdkSandboxProcessDeathCallbackCompat {
            override fun onSdkSandboxDied() {
            }
        })
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun getSandboxedSdks_whenApiBelow34_returnsLocallyLoadedSdkList() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val localSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.CURRENT.packageName,
                Bundle()
            )
        }

        val sandboxedSdks = managerCompat.getSandboxedSdks()

        assertThat(sandboxedSdks).containsExactly(localSdk)
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33)
    fun startSdkSandboxActivity_whenNoHandlerRegisteredAndApiBelow34_doesntThrow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                managerCompat.startSdkSandboxActivity(this, Binder())
            }
        }
    }

    @Test
    fun startSdkSandboxActivity_startLocalSdkActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val localSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.forSdkName("v4").packageName,
                Bundle()
            )
        }

        val handler = CatchingSdkActivityHandler()

        val testSdk = localSdk.asTestSdk()
        val token = testSdk.registerSdkSandboxActivityHandler(handler)

        with(ActivityScenario.launch(EmptyActivity::class.java)) {
            withActivity {
                managerCompat.startSdkSandboxActivity(this, token)
            }
        }

        val activityHolder = handler.waitForActivity()
        assertThat(activityHolder.getActivity()).isInstanceOf(SdkActivity::class.java)
    }

    @Test
    fun sdkController_getSandboxedSdks_returnsLocallyLoadedSdks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val managerCompat = SdkSandboxManagerCompat.from(context)

        val localSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.forSdkName("v2").packageName,
                Bundle()
            )
        }

        val anotherLocalSdk = runBlocking {
            managerCompat.loadSdk(
                TestSdkConfigs.CURRENT.packageName,
                Bundle()
            )
        }

        val testSdk = localSdk.asTestSdk()

        val interfaces = testSdk.getSandboxedSdks()
            .map { it.getInterface() }

        assertThat(interfaces).containsExactly(
            localSdk.getInterface(),
            anotherLocalSdk.getInterface(),
        )
    }
}
