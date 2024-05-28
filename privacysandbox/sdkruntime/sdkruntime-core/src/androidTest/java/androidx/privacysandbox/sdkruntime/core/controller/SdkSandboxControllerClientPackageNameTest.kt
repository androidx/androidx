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

import android.os.ext.SdkExtensions
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.core.os.BuildCompat
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Assume
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

// TODO(b/249982507) Rewrite test to use real SDK in sandbox instead of mocking controller
@SdkSuppress(minSdkVersion = 34)
class SdkSandboxControllerClientPackageNameTest {

    @Rule @JvmField val sdkSandboxControllerMockRule = SdkSandboxControllerMockRule()

    @Test
    fun getClientPackageName_withoutApiAvailable_parseSdkDataDirPath() {
        Assume.assumeFalse(
            "Requires GetClientPackageName API not available",
            isClientPackageNameAvailable()
        )

        val expectedResult = "test.client.parsed.package.name"
        `when`(sdkSandboxControllerMockRule.contextSpy.getDataDir())
            .thenReturn(File("/data/misc_ce/0/sdksandbox/$expectedResult/sdk_random"))

        val controllerCompat = sdkSandboxControllerMockRule.controllerCompat
        val result = controllerCompat.getClientPackageName()

        assertThat(result).isEqualTo(expectedResult)
    }

    @Test
    @RequiresApi(34)
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 8)
    fun getClientPackageName_withApiAvailable_returnsResultFromPlatformApi() {
        Assume.assumeTrue(
            "Requires GetClientPackageName API available",
            isClientPackageNameAvailable()
        )

        val expectedResult = "test.client.package.name"
        val sdkSandboxController = sdkSandboxControllerMockRule.sdkSandboxControllerMock
        `when`(sdkSandboxController.getClientPackageName()).thenReturn(expectedResult)

        val controllerCompat = sdkSandboxControllerMockRule.controllerCompat
        val result = controllerCompat.getClientPackageName()

        assertThat(result).isEqualTo(expectedResult)
        Mockito.verify(sdkSandboxController).getClientPackageName()
    }

    private fun isClientPackageNameAvailable() = BuildCompat.AD_SERVICES_EXTENSION_INT >= 8
}
