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
import androidx.appfunctions.ExecuteAppFunctionResponse.Success.Companion.toCompatExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.AssumptionViolatedException
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class ExecuteAppFunctionResponseTest {
    @Test
    fun toPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = TEST_APP_FUNCTION_DATA
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
    fun toPlatformExecuteAppFunctionResponse_success() {
        val appFunctionData = TEST_APP_FUNCTION_DATA
        val response = ExecuteAppFunctionResponse.Success(appFunctionData)
        val platformResponse = response.toPlatformExecuteAppFunctionResponse()

        assertThat(platformResponse.resultDocument).isEqualTo(appFunctionData.genericDocument)
        assertThat(platformResponse.extras.isEmpty()).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras = AppFunctionData(appFunctionData.genericDocument, bundle)
        val responseWithExtras = ExecuteAppFunctionResponse.Success(appFunctionDataWithExtras)
        val platformResponseWithExtras = responseWithExtras.toPlatformExecuteAppFunctionResponse()

        assertThat(platformResponseWithExtras.resultDocument)
            .isEqualTo(appFunctionData.genericDocument)
        assertThat(platformResponseWithExtras.extras).isEqualTo(bundle)
    }

    @Test
    fun fromPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val appFunctionData = TEST_APP_FUNCTION_DATA
        val platformResponse =
            com.android.extensions.appfunctions.ExecuteAppFunctionResponse(
                appFunctionData.genericDocument
            )
        val response =
            ExecuteAppFunctionResponse.Success.fromPlatformExtensionClass(
                platformResponse,
                TEST_APP_FUNCTION_METADATA,
            )

        assertThat(response.returnValue.genericDocument).isEqualTo(appFunctionData.genericDocument)
        assertThat(response.returnValue.extras.isEmpty).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun toCompatExecuteAppFunctionResponse_success() {
        val appFunctionData = TEST_APP_FUNCTION_DATA
        val platformResponse =
            android.app.appfunctions.ExecuteAppFunctionResponse(appFunctionData.genericDocument)
        val response =
            platformResponse.toCompatExecuteAppFunctionResponse(TEST_APP_FUNCTION_METADATA)

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

    companion object {
        private val TEST_APP_FUNCTION_METADATA =
            AppFunctionMetadata(
                id = "testId",
                packageName = "testPackage",
                components = AppFunctionComponentsMetadata(),
                schema = null,
                parameters =
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "testString",
                            isRequired = true,
                            dataType = AppFunctionStringTypeMetadata(isNullable = false),
                        )
                    ),
                response = AppFunctionResponseMetadata(AppFunctionUnitTypeMetadata(false)),
                isEnabled = true,
            )

        private val TEST_APP_FUNCTION_DATA: AppFunctionData =
            AppFunctionData.Builder(
                    TEST_APP_FUNCTION_METADATA.parameters,
                    AppFunctionComponentsMetadata(),
                )
                .setString("testString", "value")
                .build()
    }
}
