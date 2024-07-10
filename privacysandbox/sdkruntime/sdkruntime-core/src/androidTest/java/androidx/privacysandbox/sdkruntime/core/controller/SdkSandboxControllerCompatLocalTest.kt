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

package androidx.privacysandbox.sdkruntime.core.controller

import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.internal.ClientApiVersion
import androidx.privacysandbox.sdkruntime.core.internal.ClientFeature
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SdkSandboxControllerCompatLocalTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        // Emulate loading via client lib with only base features available
        clientHandShakeForMinSupportedVersion()

        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Reset version back to avoid failing non-compat tests
        Versions.resetClientVersion()
        SdkSandboxControllerCompat.resetLocalImpl()
    }

    @Test
    fun from_withoutLocalImpl_throwsUnsupportedOperationException() {
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            SdkSandboxControllerCompat.from(context)
        }
    }

    @Test
    fun loadSdk_whenNotAvailable_throwsLoadSdkNotFoundException() {
        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val exception =
            Assert.assertThrows(LoadSdkCompatException::class.java) {
                runBlocking { controllerCompat.loadSdk("SDK", Bundle()) }
            }

        assertThat(exception.loadSdkErrorCode).isEqualTo(LoadSdkCompatException.LOAD_SDK_NOT_FOUND)
    }

    @Test
    fun loadSdk_returnsLoadedSdkFromLocalImpl() {
        clientHandShakeForVersionIncluding(ClientFeature.LOAD_SDK)

        val expectedResult = SandboxedSdkCompat(Binder())
        val stubLocalImpl = TestStubImpl(loadSdkResult = expectedResult)
        SdkSandboxControllerCompat.injectLocalImpl(stubLocalImpl)
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val sdkName = "SDK"
        val sdkParams = Bundle()
        val result = runBlocking { controllerCompat.loadSdk(sdkName, sdkParams) }

        assertThat(result).isSameInstanceAs(expectedResult)
        assertThat(stubLocalImpl.lastLoadSdkName).isEqualTo(sdkName)
        assertThat(stubLocalImpl.lastLoadSdkParams).isEqualTo(sdkParams)
    }

    @Test
    fun loadSdk_rethrowsExceptionFromLocalImpl() {
        clientHandShakeForVersionIncluding(ClientFeature.LOAD_SDK)

        val expectedError = LoadSdkCompatException(RuntimeException(), Bundle())
        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl(loadSdkError = expectedError))
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val exception =
            Assert.assertThrows(LoadSdkCompatException::class.java) {
                runBlocking { controllerCompat.loadSdk("SDK", Bundle()) }
            }

        assertThat(exception).isSameInstanceAs(expectedError)
    }

    @Test
    fun getSandboxedSdks_returnsListFromLocalImpl() {
        val expectedResult = listOf(SandboxedSdkCompat(Binder()))
        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl(sandboxedSdks = expectedResult))

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val sandboxedSdks = controllerCompat.getSandboxedSdks()
        assertThat(sandboxedSdks).isEqualTo(expectedResult)
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_returnsListFromLocalImpl() {
        clientHandShakeForVersionIncluding(ClientFeature.APP_OWNED_INTERFACES)

        val expectedResult =
            listOf(
                AppOwnedSdkSandboxInterfaceCompat(
                    name = "TestSdk",
                    version = 42,
                    binder = Binder(),
                )
            )
        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl(appOwnedSdks = expectedResult))

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).isEqualTo(expectedResult)
    }

    @Test
    fun registerSdkSandboxActivityHandler_registerItInLocalImpl() {
        clientHandShakeForVersionIncluding(ClientFeature.SDK_ACTIVITY_HANDLER)

        val localImpl = TestStubImpl()
        SdkSandboxControllerCompat.injectLocalImpl(localImpl)

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val handlerCompat =
            object : SdkSandboxActivityHandlerCompat {
                override fun onActivityCreated(activityHolder: ActivityHolder) {}
            }
        val token = controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)
        assertThat(token).isEqualTo(localImpl.token)
    }

    @Test
    fun unregisterSdkSandboxActivityHandler_unregisterItFromLocalImpl() {
        clientHandShakeForVersionIncluding(ClientFeature.SDK_ACTIVITY_HANDLER)

        val localImpl = TestStubImpl()
        SdkSandboxControllerCompat.injectLocalImpl(localImpl)

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val handlerCompat =
            object : SdkSandboxActivityHandlerCompat {
                override fun onActivityCreated(activityHolder: ActivityHolder) {}
            }
        val token = controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)
        assertThat(token).isEqualTo(localImpl.token)

        controllerCompat.unregisterSdkSandboxActivityHandler(handlerCompat)
        assertThat(localImpl.token).isNull()
    }

    @Test
    fun getClientPackageName_whenNotAvailable_returnsContextPackageName() {
        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val result = controllerCompat.getClientPackageName()

        assertThat(result).isEqualTo(context.getPackageName())
    }

    @Test
    fun getClientPackageName_returnsPackageNameFromLocalImpl() {
        clientHandShakeForVersionIncluding(ClientFeature.GET_CLIENT_PACKAGE_NAME)

        val expectedResult = "test.client.package.name"
        val stubLocalImpl = TestStubImpl(clientPackageName = expectedResult)
        SdkSandboxControllerCompat.injectLocalImpl(stubLocalImpl)
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val result = controllerCompat.getClientPackageName()

        assertThat(result).isEqualTo(expectedResult)
    }

    /**
     * Call [Versions.handShake] to emulate loading via client lib. Using version where
     * [clientFeature] available.
     */
    private fun clientHandShakeForVersionIncluding(clientFeature: ClientFeature) {
        Versions.handShake(clientFeature.availableFrom.apiLevel)
    }

    /**
     * Call [Versions.handShake] to emulate loading via client lib. Using
     * [ClientApiVersion.MIN_SUPPORTED] - to check features available in all client versions.
     */
    private fun clientHandShakeForMinSupportedVersion() {
        Versions.handShake(ClientApiVersion.MIN_SUPPORTED.apiLevel)
    }

    internal class TestStubImpl(
        private val sandboxedSdks: List<SandboxedSdkCompat> = emptyList(),
        private val appOwnedSdks: List<AppOwnedSdkSandboxInterfaceCompat> = emptyList(),
        private val loadSdkResult: SandboxedSdkCompat? = null,
        private val loadSdkError: LoadSdkCompatException? = null,
        private val clientPackageName: String = ""
    ) : SdkSandboxControllerCompat.SandboxControllerImpl {
        var token: IBinder? = null

        var lastLoadSdkName: String? = null
        var lastLoadSdkParams: Bundle? = null

        override fun loadSdk(
            sdkName: String,
            params: Bundle,
            executor: Executor,
            callback: LoadSdkCallback
        ) {
            lastLoadSdkName = sdkName
            lastLoadSdkParams = params

            if (loadSdkResult != null) {
                executor.execute { callback.onResult(loadSdkResult) }
            } else {
                executor.execute {
                    callback.onError(
                        loadSdkError
                            ?: LoadSdkCompatException(
                                LoadSdkCompatException.LOAD_SDK_INTERNAL_ERROR,
                                "Shouldn't be called without setting result or error"
                            )
                    )
                }
            }
        }

        override fun getSandboxedSdks() = sandboxedSdks

        override fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkSandboxInterfaceCompat> =
            appOwnedSdks

        override fun registerSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ): IBinder {
            token = Binder()
            return token!!
        }

        override fun unregisterSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        ) {
            token = null
        }

        override fun getClientPackageName(): String = clientPackageName
    }
}
