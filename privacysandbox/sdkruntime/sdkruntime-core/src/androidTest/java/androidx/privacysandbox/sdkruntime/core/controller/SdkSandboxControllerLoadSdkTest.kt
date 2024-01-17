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

package androidx.privacysandbox.sdkruntime.core.controller

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.Binder
import android.os.Bundle
import android.os.OutcomeReceiver
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock

// TODO(b/249982507) Rewrite test to use real SDK in sandbox instead of mocking controller
@SdkSuppress(minSdkVersion = 34)
class SdkSandboxControllerLoadSdkTest {

    private lateinit var sdkSandboxController: SdkSandboxController
    private lateinit var controllerCompat: SdkSandboxControllerCompat

    @Before
    fun setUp() {
        sdkSandboxController = mock(SdkSandboxController::class.java)

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        doReturn(sdkSandboxController)
            .`when`(context).getSystemService(SdkSandboxController::class.java)

        controllerCompat = SdkSandboxControllerCompat.from(context)
    }

    @Test
    fun loadSdk_withoutLoadSdkApiAvailable_throwsLoadSdkCompatException() {
        assumeFalse(
            "Requires LoadSdk API not available",
            isLoadSdkApiAvailable()
        )

        Assert.assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                controllerCompat.loadSdk("SDK", Bundle())
            }
        }
    }

    @Test
    @RequiresApi(34)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    fun loadSdk_withLoadSdkApiAvailable_returnResultFromPlatformLoadSdk() {
        assumeTrue(
            "Requires LoadSdk API available",
            isLoadSdkApiAvailable()
        )

        val sdkName = "SDK"
        val params = Bundle()

        val sandboxedSdk = SandboxedSdk(Binder())
        setupLoadSdkAnswer(sandboxedSdk)

        val result = runBlocking {
            controllerCompat.loadSdk(sdkName, params)
        }
        assertThat(result.getInterface()).isEqualTo(sandboxedSdk.getInterface())

        verify(sdkSandboxController).loadSdk(
            eq(sdkName),
            eq(params),
            any(),
            any()
        )
    }

    @Test
    @RequiresApi(34)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    fun loadSdk_withLoadSdkApiAvailable_rethrowsExceptionFromPlatformLoadSdk() {
        assumeTrue(
            "Requires LoadSdk API available",
            isLoadSdkApiAvailable()
        )

        val loadSdkException = LoadSdkException(
            RuntimeException(),
            Bundle()
        )
        setupLoadSdkAnswer(loadSdkException)

        val result = Assert.assertThrows(LoadSdkCompatException::class.java) {
            runBlocking {
                controllerCompat.loadSdk("test", Bundle())
            }
        }

        assertThat(result.cause).isEqualTo(loadSdkException.cause)
        assertThat(result.extraInformation).isEqualTo(loadSdkException.extraInformation)
        assertThat(result.loadSdkErrorCode).isEqualTo(loadSdkException.loadSdkErrorCode)
    }

    @RequiresApi(34)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    private fun setupLoadSdkAnswer(sandboxedSdk: SandboxedSdk) {
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<SandboxedSdk, LoadSdkException>>(3)
            receiver.onResult(sandboxedSdk)
            null
        }
        doAnswer(answer)
            .`when`(sdkSandboxController).loadSdk(
                any(),
                any(),
                any(),
                any()
            )
    }

    @RequiresApi(34)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 10)
    private fun setupLoadSdkAnswer(loadSdkException: LoadSdkException) {
        val answer = { args: InvocationOnMock ->
            val receiver = args.getArgument<OutcomeReceiver<SandboxedSdk, LoadSdkException>>(3)
            receiver.onError(loadSdkException)
            null
        }
        doAnswer(answer)
            .`when`(sdkSandboxController).loadSdk(
                any(),
                any(),
                any(),
                any()
            )
    }

    private fun isLoadSdkApiAvailable() =
        BuildCompat.AD_SERVICES_EXTENSION_INT >= 10
}
