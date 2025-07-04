/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions

import android.os.Build
import android.os.Bundle
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.AssumptionViolatedException
import org.junit.Test

class ExecuteAppFunctionResponseTest {
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun toPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val response = ExecuteAppFunctionResponse.Success(appFunctionData)
        val platformResponse = response.toPlatformExtensionClass()

        assertThat(platformResponse.resultDocument).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformResponse.extras.isEmpty()).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras = AppFunctionData(appFunctionData.genericDocument, bundle)
        val responseWithExtras = ExecuteAppFunctionResponse.Success(appFunctionDataWithExtras)
        val platformResponseWithExtras = responseWithExtras.toPlatformExtensionClass()

        assertThat(platformResponseWithExtras.resultDocument)
            .isEqualTo(appFunctionData.genericDocument)
        assertThat(platformResponseWithExtras.extras).isEqualTo(bundle)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun toPlatformClass_success() {
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val response = ExecuteAppFunctionResponse.Success(appFunctionData)
        val platformResponse = response.toPlatformClass()

        assertThat(platformResponse.resultDocument).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformResponse.extras.isEmpty()).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras = AppFunctionData(appFunctionData.genericDocument, bundle)
        val responseWithExtras = ExecuteAppFunctionResponse.Success(appFunctionDataWithExtras)
        val platformResponseWithExtras = responseWithExtras.toPlatformClass()

        assertThat(platformResponseWithExtras.resultDocument)
            .isEqualTo(appFunctionData.genericDocument)
        assertThat(platformResponseWithExtras.extras).isEqualTo(bundle)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun fromPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val platformResponse =
            com.android.extensions.appfunctions.ExecuteAppFunctionResponse(
                appFunctionData.genericDocument
            )
        val response =
            ExecuteAppFunctionResponse.Success.fromPlatformExtensionClass(platformResponse)

        assertThat(response.returnValue.genericDocument).isEqualTo(appFunctionData.genericDocument)
        assertThat(response.returnValue.extras.isEmpty).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun fromPlatformClass_success() {
        val appFunctionData = AppFunctionData.Builder("").setString("testString", "value").build()
        val platformResponse =
            android.app.appfunctions.ExecuteAppFunctionResponse(appFunctionData.genericDocument)
        val response = ExecuteAppFunctionResponse.Success.fromPlatformClass(platformResponse)

        assertThat(response.returnValue.genericDocument).isEqualTo(appFunctionData.genericDocument)
        assertThat(response.returnValue.extras.isEmpty).isTrue()
    }

    private fun assumeAppFunctionExtensionLibraryAvailable() {
        try {
            Class.forName("com.android.extensions.appfunctions.ExecuteAppFunctionResponse")
            return
        } catch (e: ClassNotFoundException) {
            throw AssumptionViolatedException("Unable to find AppFunction extension library", e)
        }
    }
}
