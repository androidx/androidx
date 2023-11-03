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

import android.app.Activity
import android.app.Application
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.sdkprovider.SdkSandboxActivityHandler
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresExtension
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.`when`

// TODO(b/249982507) Rewrite test to use real SDK in sandbox instead of mocking controller
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SdkSandboxControllerCompatSandboxedTest {

    @Test
    fun controllerAPIs_whenApiNotAvailable_notDelegateToSandbox() {
        assumeFalse(
            "Requires SandboxController API not available",
            isSandboxControllerAvailable()
        )

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        controllerCompat.getSandboxedSdks()
        val handlerCompat = object : SdkSandboxActivityHandlerCompat {
            override fun onActivityCreated(activityHolder: ActivityHolder) {}
        }
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)
        }
        Assert.assertThrows(UnsupportedOperationException::class.java) {
            controllerCompat.unregisterSdkSandboxActivityHandler(handlerCompat)
        }
        verifyZeroInteractions(context)
    }

    @Test
    fun getSandboxedSdks_whenApiNotAvailable_returnsEmptyList() {
        assumeFalse(
            "Requires SandboxController API not available",
            isSandboxControllerAvailable()
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val sandboxedSdks = controllerCompat.getSandboxedSdks()

        assertThat(sandboxedSdks).isEmpty()
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun getSandboxedSdks_whenApiAvailable_returnsListFromPlatformApi() {
        assumeTrue(
            "Requires SandboxController API available",
            isSandboxControllerAvailable()
        )

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val sdkSandboxController = mock(SdkSandboxController::class.java)
        doReturn(sdkSandboxController)
            .`when`(context).getSystemService(SdkSandboxController::class.java)

        val sandboxedSdk = SandboxedSdk(Binder())
        `when`(sdkSandboxController.sandboxedSdks)
            .thenReturn(listOf(sandboxedSdk))

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val sandboxedSdks = controllerCompat.getSandboxedSdks()
        assertThat(sandboxedSdks).hasSize(1)
        val result = sandboxedSdks[0]

        assertThat(result.getInterface()).isEqualTo(sandboxedSdk.getInterface())
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    fun registerSdkSandboxHandlerCompat_whenApiAvailable_registerItToPlatform() {
        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val sdkSandboxController = mock(SdkSandboxController::class.java)
        doReturn(sdkSandboxController)
            .`when`(context).getSystemService(SdkSandboxController::class.java)

        val platformRegisteredHandlerCaptor = ArgumentCaptor.forClass(
            SdkSandboxActivityHandler::class.java)
        doReturn(Binder())
            .`when`(sdkSandboxController).registerSdkSandboxActivityHandler(
                platformRegisteredHandlerCaptor.capture())

        val handlerCompat = mock(SdkSandboxActivityHandlerCompat::class.java)
        val controllerCompat = SdkSandboxControllerCompat.from(context)
        controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)

        verify(sdkSandboxController).registerSdkSandboxActivityHandler(
            platformRegisteredHandlerCaptor.value
        )

        val activityMock = mock(Activity::class.java)
        val onBackInvokedDispatcher = mock(OnBackInvokedDispatcher::class.java)
        doReturn(onBackInvokedDispatcher).`when`(activityMock).onBackInvokedDispatcher

        platformRegisteredHandlerCaptor.value.onActivityCreated(activityMock)
        var activityHolderCaptor: ArgumentCaptor<ActivityHolder> =
            ArgumentCaptor.forClass(ActivityHolder::class.java)
        verify(handlerCompat).onActivityCreated(capture(activityHolderCaptor))
        assertThat(activityHolderCaptor.value.getActivity()).isEqualTo(activityMock)

        assertThat(activityHolderCaptor.value.getOnBackPressedDispatcher()).isNotNull()

        assertThat(activityHolderCaptor.value.lifecycle).isNotNull()
        var activityLifecycleCallbackCaptor:
            ArgumentCaptor<Application.ActivityLifecycleCallbacks> =
            ArgumentCaptor.forClass(Application.ActivityLifecycleCallbacks::class.java)
        verify(activityMock).registerActivityLifecycleCallbacks(
            activityLifecycleCallbackCaptor.capture()
        )
        var bundleMock = mock(Bundle::class.java)
        UiThreadStatement.runOnUiThread {
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.INITIALIZED)

            activityLifecycleCallbackCaptor.value.onActivityPostCreated(activityMock, bundleMock)
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.CREATED)

            activityLifecycleCallbackCaptor.value.onActivityPostStarted(activityMock)
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.STARTED)

            activityLifecycleCallbackCaptor.value.onActivityPostResumed(activityMock)
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.RESUMED)

            activityLifecycleCallbackCaptor.value.onActivityPrePaused(activityMock)
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.STARTED)

            activityLifecycleCallbackCaptor.value.onActivityPreStopped(activityMock)
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.CREATED)

            activityLifecycleCallbackCaptor.value.onActivityPreDestroyed(activityMock)
            assertThat(activityHolderCaptor.value.lifecycle.currentState).isEqualTo(
                Lifecycle.State.DESTROYED)
        }
    }

    @Test
    // TODO(b/262577044) Remove RequiresExtension after extensions support in @SdkSuppress
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE, codeName = "UpsideDownCake")
    fun unregisterSdkSandboxHandlerCompat_whenApiAvailable_unregisterItToPlatform() {
        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val sdkSandboxController = mock(SdkSandboxController::class.java)
        doReturn(sdkSandboxController)
            .`when`(context).getSystemService(SdkSandboxController::class.java)

        val registeredHandlerCaptor = ArgumentCaptor.forClass(SdkSandboxActivityHandler::class.java)
        doReturn(Binder())
            .`when`(sdkSandboxController).registerSdkSandboxActivityHandler(
                registeredHandlerCaptor.capture())

        val controllerCompat = SdkSandboxControllerCompat.from(context)
        val handlerCompat = mock(SdkSandboxActivityHandlerCompat::class.java)

        controllerCompat.registerSdkSandboxActivityHandler(handlerCompat)
        verify(sdkSandboxController).registerSdkSandboxActivityHandler(
            registeredHandlerCaptor.value)

        val unregisteredHandlerCaptor = ArgumentCaptor.forClass(
            SdkSandboxActivityHandler::class.java
        )
        controllerCompat.unregisterSdkSandboxActivityHandler(handlerCompat)
        verify(sdkSandboxController).unregisterSdkSandboxActivityHandler(
            unregisteredHandlerCaptor.capture())

        assertThat(unregisteredHandlerCaptor.value).isEqualTo(registeredHandlerCaptor.value)
    }

    private fun isSandboxControllerAvailable() =
        AdServicesInfo.isAtLeastV5()

    // To capture non null arguments.

    private fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}
