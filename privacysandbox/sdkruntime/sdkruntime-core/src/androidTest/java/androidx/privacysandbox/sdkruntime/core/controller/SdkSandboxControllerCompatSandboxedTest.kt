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

import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.Mockito.`when`

// TODO(b/249982507) Rewrite test to use real SDK in sandbox instead of mocking controller
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class SdkSandboxControllerCompatSandboxedTest {

    @Test
    fun getSandboxedSdks_whenApiNotAvailable_notDelegateToSandbox() {
        assumeFalse(
            "Requires SandboxController API not available",
            isSandboxControllerAvailable()
        )

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        controllerCompat.getSandboxedSdks()

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

    private fun isSandboxControllerAvailable() =
        AdServicesInfo.isAtLeastV5()
}
