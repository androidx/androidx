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

package androidx.privacysandbox.sdkruntime.provider.controller

import android.app.sdksandbox.sdkprovider.SdkSandboxClientImportanceListener
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.privacysandbox.sdkruntime.core.SdkSandboxClientImportanceListenerCompat
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.Executor
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

// TODO(b/249982507) Rewrite test to use real SDK in sandbox instead of mocking controller
@SdkSuppress(minSdkVersion = 34)
class SdkSandboxControllerClientImportanceListenerTest {

    @Rule @JvmField val sdkSandboxControllerMockRule = SdkSandboxControllerMockRule()

    @Test
    fun registerSdkSandboxClientImportanceListener_withoutApiAvailable_doNothing() {
        assumeFalse(
            "Requires ClientImportanceListener API not available",
            isClientImportanceListenerAvailable(),
        )

        val controllerCompat = sdkSandboxControllerMockRule.controllerCompat
        controllerCompat.registerSdkSandboxClientImportanceListener(
            executor = Runnable::run,
            listenerCompat = mock(SdkSandboxClientImportanceListenerCompat::class.java),
        )
    }

    @Test
    fun unregisterSdkSandboxClientImportanceListener_withoutApiAvailable_doNothing() {
        assumeFalse(
            "Requires ClientImportanceListener API not available",
            isClientImportanceListenerAvailable(),
        )

        val controllerCompat = sdkSandboxControllerMockRule.controllerCompat
        controllerCompat.unregisterSdkSandboxClientImportanceListener(
            listenerCompat = mock(SdkSandboxClientImportanceListenerCompat::class.java)
        )
    }

    @Test
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    fun registerSdkSandboxClientImportanceListener_withApiAvailable_delegatesToPlatformApi() {
        assumeTrue(
            "Requires ClientImportanceListener API available",
            isClientImportanceListenerAvailable(),
        )

        val controllerCompat = sdkSandboxControllerMockRule.controllerCompat
        val compatListener = mock(SdkSandboxClientImportanceListenerCompat::class.java)
        val executor = Executor { r -> r.run() }
        controllerCompat.registerSdkSandboxClientImportanceListener(executor, compatListener)

        val platformRegisteredListenerCaptor =
            ArgumentCaptor.forClass(SdkSandboxClientImportanceListener::class.java)
        verify(sdkSandboxControllerMockRule.sdkSandboxControllerMock)
            .registerSdkSandboxClientImportanceListener(
                eq(executor),
                platformRegisteredListenerCaptor.capture(),
            )

        platformRegisteredListenerCaptor.value.onForegroundImportanceChanged(true)
        verify(compatListener).onForegroundImportanceChanged(true)
    }

    @Test
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 14)
    fun unregisterSdkSandboxClientImportanceListener_withApiAvailable_delegatesToPlatformApi() {
        assumeTrue(
            "Requires ClientImportanceListener API available",
            isClientImportanceListenerAvailable(),
        )

        val controllerCompat = sdkSandboxControllerMockRule.controllerCompat
        val compatListener = mock(SdkSandboxClientImportanceListenerCompat::class.java)
        controllerCompat.registerSdkSandboxClientImportanceListener(
            executor = Runnable::run,
            listenerCompat = compatListener,
        )
        val platformRegisteredListenerCaptor =
            ArgumentCaptor.forClass(SdkSandboxClientImportanceListener::class.java)
        verify(sdkSandboxControllerMockRule.sdkSandboxControllerMock)
            .registerSdkSandboxClientImportanceListener(
                any(),
                platformRegisteredListenerCaptor.capture(),
            )

        controllerCompat.unregisterSdkSandboxClientImportanceListener(compatListener)
        val platformUnRegisteredListenerCaptor =
            ArgumentCaptor.forClass(SdkSandboxClientImportanceListener::class.java)
        verify(sdkSandboxControllerMockRule.sdkSandboxControllerMock)
            .unregisterSdkSandboxClientImportanceListener(
                platformUnRegisteredListenerCaptor.capture()
            )

        assertThat(platformUnRegisteredListenerCaptor.value)
            .isEqualTo(platformRegisteredListenerCaptor.value)
    }

    private fun isClientImportanceListenerAvailable() = BuildCompat.AD_SERVICES_EXTENSION_INT >= 14
}
