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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
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
        // Emulate loading via client lib
        Versions.handShake(Versions.API_VERSION)

        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Reset version back to avoid failing non-compat tests
        Versions.resetClientVersion()
        SdkSandboxControllerCompat.resetLocalImpl()
    }

    @Test
    fun loadSdk_withoutLocalImpl_throwsLoadSdkCompatException() {
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        Assert.assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                controllerCompat.loadSdk("SDK", Bundle())
            }
        }
    }

    @Test
    fun loadSdk_withLocalImpl_throwsLoadSdkCompatException() {
        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        Assert.assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                controllerCompat.loadSdk("SDK", Bundle())
            }
        }
    }

    @Test
    fun getSandboxedSdks_withoutLocalImpl_returnsEmptyList() {
        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val sandboxedSdks = controllerCompat.getSandboxedSdks()
        assertThat(sandboxedSdks).isEmpty()
    }

    @Test
    fun getSandboxedSdks_withLocalImpl_returnsListFromLocalImpl() {
        val expectedResult = listOf(SandboxedSdkCompat(Binder()))
        SdkSandboxControllerCompat.injectLocalImpl(
            TestStubImpl(
                sandboxedSdks = expectedResult
            )
        )

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val sandboxedSdks = controllerCompat.getSandboxedSdks()
        assertThat(sandboxedSdks).isEqualTo(expectedResult)
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_withoutLocalImpl_returnsEmptyList() {
        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).isEmpty()
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_clientApiBelow4_returnsEmptyList() {
        // Emulate loading via client lib with version below 4
        Versions.handShake(3)

        SdkSandboxControllerCompat.injectLocalImpl(
            TestStubImpl(
                appOwnedSdks = listOf(
                    AppOwnedSdkSandboxInterfaceCompat(
                        name = "TestSdk",
                        version = 42,
                        binder = Binder(),
                    )
                )
            )
        )

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).isEmpty()
    }

    @Test
    fun getAppOwnedSdkSandboxInterfaces_withLocalImpl_returnsListFromLocalImpl() {
        val expectedResult = listOf(
            AppOwnedSdkSandboxInterfaceCompat(
                name = "TestSdk",
                version = 42,
                binder = Binder(),
            )
        )
        SdkSandboxControllerCompat.injectLocalImpl(
            TestStubImpl(
                appOwnedSdks = expectedResult
            )
        )

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).isEqualTo(expectedResult)
    }

    @Test
    fun registerSdkSandboxActivityHandler_withLocalImpl_registerItInLocalImpl() {
        val localImpl = TestStubImpl()
        SdkSandboxControllerCompat.injectLocalImpl(localImpl)

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val handlerCompat = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {}
        }
        val token = controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)
        assertThat(token).isEqualTo(localImpl.token)
    }

    @Test
    fun unregisterSdkSandboxActivityHandler_withLocalImpl_unregisterItFromLocalImpl() {
        val localImpl = TestStubImpl()
        SdkSandboxControllerCompat.injectLocalImpl(localImpl)

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val handlerCompat = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {}
        }
        val token = controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)
        assertThat(token).isEqualTo(localImpl.token)

        controllerCompat.unregisterSdkSandboxActivityHandler(handlerCompat)
        assertThat(localImpl.token).isNull()
    }

    @Test
    fun registerSdkSandboxActivityHandler_clientApiBelow3_throwsUnsupportedOperationException() {
        // Emulate loading via client lib with version below 3
        Versions.handShake(2)

        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        Assert.assertThrows(UnsupportedOperationException::class.java) {
            controllerCompat.registerSdkSandboxActivityHandler(
                object : SdkSandboxActivityHandlerCompat {
                    override fun onActivityCreated(activityHolder: ActivityHolder) {}
                }
            )
        }
    }

    @Test
    fun unregisterSdkSandboxActivityHandler_clientApiBelow3_throwsUnsupportedOperationException() {
        // Emulate loading via client lib with version below 3
        Versions.handShake(2)

        SdkSandboxControllerCompat.injectLocalImpl(TestStubImpl())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        Assert.assertThrows(UnsupportedOperationException::class.java) {
            controllerCompat.unregisterSdkSandboxActivityHandler(
                object : SdkSandboxActivityHandlerCompat {
                    override fun onActivityCreated(activityHolder: ActivityHolder) {}
                }
            )
        }
    }

    internal class TestStubImpl(
        private val sandboxedSdks: List<SandboxedSdkCompat> = emptyList(),
        private val appOwnedSdks: List<AppOwnedSdkSandboxInterfaceCompat> = emptyList()
    ) : SdkSandboxControllerCompat.SandboxControllerImpl {
        var token: IBinder? = null

        override suspend fun loadSdk(sdkName: String, params: Bundle): SandboxedSdkCompat {
            throw UnsupportedOperationException("Shouldn't be called")
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
    }
}
