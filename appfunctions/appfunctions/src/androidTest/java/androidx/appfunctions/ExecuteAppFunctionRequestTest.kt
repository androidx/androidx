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

import android.app.AppInteractionAttribution
import android.os.Build
import android.os.Bundle
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.EXTRA_PARAMETERS
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.toCompatExecuteAppFunctionRequest
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionName
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
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
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    fun toPlatformExecuteAppFunctionRequestWithAttribution_success() {
        val request = ExecuteAppFunctionRequest("pkg", "method", TEST_APP_FUNCTION_DATA)
        val platformRequest = request.toPlatformExecuteAppFunctionRequest()

        assertThat(platformRequest.targetPackageName).isEqualTo("pkg")
        assertThat(platformRequest.functionIdentifier).isEqualTo("method")
        assertThat(platformRequest.parameters).isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(platformRequest.extras.getBundle(EXTRA_PARAMETERS)?.isEmpty()).isTrue()
        assertThat(platformRequest.attribution).isNull()
        assertThat(platformRequest.activityId).isNull()

        // Test with attribution set
        val attribution =
            AppInteractionAttribution.Builder(AppInteractionAttribution.INTERACTION_TYPE_USER_QUERY)
                .build()
        val binder = android.os.Binder()
        val activityId = Api37Impl.createAppFunctionActivityId(binder)
        val requestWithAttribution =
            ExecuteAppFunctionRequest(
                "pkg2",
                "method2",
                TEST_APP_FUNCTION_DATA,
                attribution,
                activityId,
            )
        val platformRequestWithAttribution =
            requestWithAttribution.toPlatformExecuteAppFunctionRequest()

        assertThat(platformRequestWithAttribution.targetPackageName).isEqualTo("pkg2")
        assertThat(platformRequestWithAttribution.functionIdentifier).isEqualTo("method2")
        assertThat(platformRequestWithAttribution.parameters)
            .isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(platformRequestWithAttribution.extras.getBundle(EXTRA_PARAMETERS)?.isEmpty())
            .isTrue()
        assertThat(platformRequestWithAttribution.attribution).isEqualTo(attribution)
        assertThat(platformRequestWithAttribution.activityId).isEqualTo(activityId)
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
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    fun toCompatExecuteAppFunctionRequestWithAttribution_success() {
        val attribution =
            AppInteractionAttribution.Builder(AppInteractionAttribution.INTERACTION_TYPE_USER_QUERY)
                .build()
        val binder = android.os.Binder()
        val activityId = Api37Impl.createAppFunctionActivityId(binder)
        val platformRequest =
            android.app.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(TEST_APP_FUNCTION_DATA.genericDocument)
                .setAttribution(attribution)
                .setActivityId(activityId)
                .build()

        val request = platformRequest.toCompatExecuteAppFunctionRequest(TEST_APP_FUNCTION_METADATA)

        assertThat(request.targetPackageName).isEqualTo("pkg")
        assertThat(request.functionIdentifier).isEqualTo("method")
        assertThat(request.functionParameters.genericDocument)
            .isEqualTo(TEST_APP_FUNCTION_DATA.genericDocument)
        assertThat(request.functionParameters.extras.isEmpty).isTrue()
        assertThat(request.attribution).isEqualTo(attribution)
        assertThat(request.activityId).isEqualTo(activityId)
    }

    private fun assumeAppFunctionExtensionLibraryAvailable() {
        try {
            Class.forName("com.android.extensions.appfunctions.ExecuteAppFunctionRequest")
            return
        } catch (e: ClassNotFoundException) {
            throw AssumptionViolatedException("Unable to find AppFunction extension library", e)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    private object Api37Impl {
        fun createAppFunctionActivityId(
            binder: android.os.IBinder
        ): android.app.appfunctions.AppFunctionActivityId {
            val parcel = android.os.Parcel.obtain()
            try {
                parcel.writeStrongBinder(binder)
                parcel.setDataPosition(0)
                return android.app.appfunctions.AppFunctionActivityId.CREATOR.createFromParcel(
                    parcel
                )
            } finally {
                parcel.recycle()
            }
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
                name = AppFunctionName(packageName = "pkg", functionIdentifier = "method"),
                schema = null,
                parameters = TEST_PARAMETERS,
                response = AppFunctionResponseMetadata(valueType = AppFunctionUnitTypeMetadata()),
                packageMetadata =
                    AppFunctionPackageMetadata(
                        packageName = "pkg",
                        components = AppFunctionComponentsMetadata(),
                    ),
                scope = AppFunctionMetadata.SCOPE_GLOBAL,
            )
    }
}
