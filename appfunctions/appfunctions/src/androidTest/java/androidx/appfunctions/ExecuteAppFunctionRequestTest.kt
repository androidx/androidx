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
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.EXTRA_PARAMETERS
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.EXTRA_USE_JETPACK_SCHEMA
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.toCompatExecuteAppFunctionRequest
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

class ExecuteAppFunctionRequestTest {
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun toPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val request = ExecuteAppFunctionRequest("pkg", "method", TEST_APP_FUNCTION_DATA)
        val platformRequest = request.toPlatformExtensionClass()

        assertThat(platformRequest.targetPackageName).isEqualTo("pkg")
        assertThat(platformRequest.functionIdentifier).isEqualTo("method")
        assertThat(platformRequest.parameters).isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(platformRequest.extras.getBundle(EXTRA_PARAMETERS)?.isEmpty()).isTrue()
        assertThat(platformRequest.extras.getBoolean(EXTRA_USE_JETPACK_SCHEMA)).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras =
            AppFunctionData(TEST_APP_FUNCTION_DATA.genericDocument, bundle)
        val requestWithExtras =
            ExecuteAppFunctionRequest("pkg2", "method2", appFunctionDataWithExtras)
        val platformRequestWithExtras = requestWithExtras.toPlatformExtensionClass()

        assertThat(platformRequestWithExtras.targetPackageName).isEqualTo("pkg2")
        assertThat(platformRequestWithExtras.functionIdentifier).isEqualTo("method2")
        assertThat(platformRequestWithExtras.parameters)
            .isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(platformRequestWithExtras.extras.getBundle(EXTRA_PARAMETERS)).isEqualTo(bundle)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun toPlatformExecuteAppFunctionRequest_success() {
        val request = ExecuteAppFunctionRequest("pkg", "method", TEST_APP_FUNCTION_DATA)
        val platformRequest = request.toPlatformExecuteAppFunctionRequest()

        assertThat(platformRequest.targetPackageName).isEqualTo("pkg")
        assertThat(platformRequest.functionIdentifier).isEqualTo("method")
        assertThat(platformRequest.parameters).isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(platformRequest.extras.getBundle(EXTRA_PARAMETERS)?.isEmpty()).isTrue()
        assertThat(platformRequest.extras.getBoolean(EXTRA_USE_JETPACK_SCHEMA)).isTrue()

        // Test with extras set
        val bundle = Bundle()
        bundle.putLong("longKey", 123L)
        val appFunctionDataWithExtras =
            AppFunctionData(TEST_APP_FUNCTION_DATA.genericDocument, bundle)
        val requestWithExtras =
            ExecuteAppFunctionRequest("pkg2", "method2", appFunctionDataWithExtras)
        val platformRequestWithExtras = requestWithExtras.toPlatformExecuteAppFunctionRequest()

        assertThat(platformRequestWithExtras.targetPackageName).isEqualTo("pkg2")
        assertThat(platformRequestWithExtras.functionIdentifier).isEqualTo("method2")
        assertThat(platformRequestWithExtras.parameters)
            .isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(platformRequestWithExtras.extras.getBundle(EXTRA_PARAMETERS)).isEqualTo(bundle)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun fromPlatformExtensionClass_success() {
        assumeAppFunctionExtensionLibraryAvailable()
        val platformRequest =
            com.android.extensions.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(TEST_APP_FUNCTION_DATA.genericDocument)
                .build()

        val request =
            ExecuteAppFunctionRequest.fromPlatformExtensionClass(
                platformRequest,
                TEST_APP_FUNCTION_METADATA,
            )

        assertThat(request.targetPackageName).isEqualTo("pkg")
        assertThat(request.functionIdentifier).isEqualTo("method")
        assertThat(request.functionParameters.genericDocument)
            .isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(request.functionParameters.extras.isEmpty).isTrue()
        assertThat(request.useJetpackSchema).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun toCompatExecuteAppFunctionRequest_success() {
        val platformRequest =
            android.app.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(TEST_APP_FUNCTION_DATA.genericDocument)
                .build()

        val request = platformRequest.toCompatExecuteAppFunctionRequest(TEST_APP_FUNCTION_METADATA)

        assertThat(request.targetPackageName).isEqualTo("pkg")
        assertThat(request.functionIdentifier).isEqualTo("method")
        assertThat(request.functionParameters.genericDocument)
            .isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(request.functionParameters.extras.isEmpty).isTrue()
        assertThat(request.useJetpackSchema).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun fromPlatformExtensionClass_fromJetPackInExtrasIsTrue_fromJetPackIsTrue() {
        assumeAppFunctionExtensionLibraryAvailable()
        val platformRequest =
            com.android.extensions.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(TEST_APP_FUNCTION_DATA.genericDocument)
                .setExtras(Bundle().apply { putBoolean(EXTRA_USE_JETPACK_SCHEMA, true) })
                .build()

        val request =
            ExecuteAppFunctionRequest.fromPlatformExtensionClass(
                platformRequest,
                TEST_APP_FUNCTION_METADATA,
            )

        assertThat(request.useJetpackSchema).isTrue()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun toCompatExecuteAppFunctionRequest_fromJetPackInExtrasIsTrue_fromJetPackIsTrue() {
        val platformRequest =
            android.app.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(TEST_APP_FUNCTION_DATA.genericDocument)
                .setExtras(Bundle().apply { putBoolean(EXTRA_USE_JETPACK_SCHEMA, true) })
                .build()

        val request = platformRequest.toCompatExecuteAppFunctionRequest(TEST_APP_FUNCTION_METADATA)

        assertThat(request.useJetpackSchema).isTrue()
    }

    private fun assumeAppFunctionExtensionLibraryAvailable() {
        try {
            Class.forName("com.android.extensions.appfunctions.ExecuteAppFunctionRequest")
            return
        } catch (e: ClassNotFoundException) {
            throw AssumptionViolatedException("Unable to find AppFunction extension library", e)
        }
    }

    companion object {
        val TEST_PARAMETERS =
            listOf(
                AppFunctionParameterMetadata(
                    name = "testString",
                    isRequired = true,
                    dataType = AppFunctionStringTypeMetadata(isNullable = false),
                )
            )
        val TEST_APP_FUNCTION_DATA =
            @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
            AppFunctionData.Builder(TEST_PARAMETERS, AppFunctionComponentsMetadata())
                .setString("testString", "value")
                .build()

        val TEST_APP_FUNCTION_METADATA =
            AppFunctionMetadata(
                id = "method",
                packageName = "pkg",
                isEnabled = true,
                schema = null,
                parameters = TEST_PARAMETERS,
                response = AppFunctionResponseMetadata(valueType = AppFunctionUnitTypeMetadata()),
                components = AppFunctionComponentsMetadata(),
            )
    }
}
