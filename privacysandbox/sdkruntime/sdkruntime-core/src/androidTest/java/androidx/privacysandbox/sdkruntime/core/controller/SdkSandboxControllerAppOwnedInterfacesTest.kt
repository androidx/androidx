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

import android.annotation.SuppressLint
import android.app.sdksandbox.sdkprovider.SdkSandboxController
import android.content.Context
import android.os.Binder
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.annotation.RequiresExtension
import androidx.privacysandbox.sdkruntime.core.AdServicesInfo
import androidx.privacysandbox.sdkruntime.core.AppOwnedInterfaceConverter
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
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
class SdkSandboxControllerAppOwnedInterfacesTest {

    @Test
    fun getAppOwnedSdkSandboxInterfaces_whenControllerNotAvailable_returnsEmptyList() {
        assumeFalse(
            "Requires SandboxController not available",
            isSandboxControllerAvailable()
        )

        val context = ApplicationProvider.getApplicationContext<Context>()
        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).isEmpty()
    }

    @Test
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun getAppOwnedSdkSandboxInterfaces_whenApiNotAvailable_returnsEmptyList() {
        assumeTrue(
            "Requires SandboxController available",
            isSandboxControllerAvailable()
        )
        assumeFalse(
            "Requires AppOwnedInterfaces API not available",
            isAppOwnedInterfacesApiAvailable()
        )

        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val sdkSandboxController = mock(SdkSandboxController::class.java)
        doReturn(sdkSandboxController)
            .`when`(context).getSystemService(SdkSandboxController::class.java)

        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).isEmpty()
        verifyZeroInteractions(sdkSandboxController)
    }

    @Test
    @SdkSuppress(minSdkVersion = 35, codeName = "UpsideDownCakePrivacySandbox")
    fun getAppOwnedSdkSandboxInterfaces_whenApiAvailable_delegateToPlatform() {
        val context = spy(ApplicationProvider.getApplicationContext<Context>())
        val controllerMock = mock(SdkSandboxController::class.java)
        doReturn(controllerMock)
            .`when`(context).getSystemService(SdkSandboxController::class.java)

        val expectedObj = AppOwnedSdkSandboxInterfaceCompat(
            name = "test",
            version = 1,
            binder = Binder()
        )
        setupMockedMethodResult(controllerMock, expectedObj)

        val controllerCompat = SdkSandboxControllerCompat.from(context)

        val appOwnedInterfaces = controllerCompat.getAppOwnedSdkSandboxInterfaces()
        assertThat(appOwnedInterfaces).hasSize(1)
        val resultObj = appOwnedInterfaces[0]

        assertThat(resultObj.getName()).isEqualTo(expectedObj.getName())
        assertThat(resultObj.getVersion()).isEqualTo(expectedObj.getVersion())
        assertThat(resultObj.getInterface()).isEqualTo(expectedObj.getInterface())
    }

    private fun isSandboxControllerAvailable() =
        AdServicesInfo.isAtLeastV5()

    private fun isAppOwnedInterfacesApiAvailable() =
        AdServicesInfo.isDeveloperPreview()

    companion object AppOwnedInterfacesApi { // to avoid fail if SdkSandboxController not present
        @SuppressLint("NewApi", "ClassVerificationFailure") // UpsideDownCakePrivacySandbox is UDC
        private fun setupMockedMethodResult(
            controllerMock: SdkSandboxController,
            result: AppOwnedSdkSandboxInterfaceCompat
        ) {
            val getAppOwnedInterfacesMethod = SdkSandboxController::class.java.getDeclaredMethod(
                "getAppOwnedSdkSandboxInterfaces"
            )

            val platformObj = AppOwnedInterfaceConverter().toPlatform(result)

            // Mockito require method call to setup result. Reflection call works same way as normal.
            `when`(getAppOwnedInterfacesMethod.invoke(controllerMock))
                .thenReturn(listOf(platformObj))
        }
    }
}
