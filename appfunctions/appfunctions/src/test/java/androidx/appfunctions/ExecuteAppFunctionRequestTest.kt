/*
 * Copyright 2026 The Android Open Source Project
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

import android.os.Bundle
import android.os.Parcel
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.EXTRA_INPUT_RESPONSES
import androidx.appfunctions.ExecuteAppFunctionRequest.Companion.toCompatExecuteAppFunctionRequest
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionMetadata
import androidx.appfunctions.metadata.AppFunctionPackageMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import androidx.appfunctions.metadata.AppFunctionUnitTypeMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 36)
class ExecuteAppFunctionRequestTest {

    @Test
    fun inputResponses_roundTripThroughPlatformRequestAfterParceling() {
        val formResponse = createFormDataResponse("Groceries")
        val actionResponse = AppFunctionElicitationResponse.ActionFinished()
        val request =
            ExecuteAppFunctionRequest.Builder("pkg", "method", createTestParameters())
                .setInputResponses(mapOf(KEY_FORM to formResponse, KEY_ACTION to actionResponse))
                .build()

        val platformRequest = request.toPlatformExecuteAppFunctionRequest().parceled()
        val roundTripped = platformRequest.toCompatExecuteAppFunctionRequest(TEST_METADATA)

        assertThat(roundTripped.inputResponses.keys).containsExactly(KEY_FORM, KEY_ACTION)
        val roundTrippedForm =
            roundTripped.inputResponses[KEY_FORM] as AppFunctionElicitationResponse.FormData
        assertThat(
                roundTrippedForm.data.getString(
                    AppFunctionElicitationResponse.FormData.PROPERTY_ELICITATION_VALUE
                )
            )
            .isEqualTo("Groceries")
        assertThat(roundTripped.inputResponses[KEY_ACTION]).isEqualTo(actionResponse)
    }

    @Test
    fun toPlatformExecuteAppFunctionRequest_emptyInputResponses_omitsExtra() {
        val request = ExecuteAppFunctionRequest("pkg", "method", createTestParameters())

        val platformRequest = request.toPlatformExecuteAppFunctionRequest()

        assertThat(platformRequest.extras.containsKey(EXTRA_INPUT_RESPONSES)).isFalse()
        assertThat(platformRequest.toCompatExecuteAppFunctionRequest(TEST_METADATA).inputResponses)
            .isEmpty()
    }

    @Test
    fun toCompatExecuteAppFunctionRequest_skipsInvalidAndUnknownResponses() {
        val platformRequest =
            ExecuteAppFunctionRequest.Builder("pkg", "method", createTestParameters())
                .setInputResponses(
                    mapOf(KEY_ACTION to AppFunctionElicitationResponse.ActionFinished())
                )
                .build()
                .toPlatformExecuteAppFunctionRequest()
        val extras = Bundle(platformRequest.extras)
        val responses = extras.getBundle(EXTRA_INPUT_RESPONSES)!!
        responses.putBundle(
            "unknown",
            Bundle().apply { putInt(AppFunctionInputResponse.KEY_RESPONSE_TYPE, Int.MAX_VALUE) },
        )
        responses.putBundle(
            "malformed",
            Bundle().apply {
                putInt(
                    AppFunctionInputResponse.KEY_RESPONSE_TYPE,
                    AppFunctionInputResponse.RESPONSE_TYPE_ELICITATION,
                )
                putInt("elicitationType", 1)
            },
        )
        responses.putString("notABundle", "unexpected")
        responses.putBundle(null, AppFunctionElicitationResponse.ActionFinished().toBundle())
        val modifiedPlatformRequest =
            android.app.appfunctions.ExecuteAppFunctionRequest.Builder("pkg", "method")
                .setParameters(platformRequest.parameters)
                .setExtras(extras)
                .build()

        val result = modifiedPlatformRequest.toCompatExecuteAppFunctionRequest(TEST_METADATA)

        assertThat(result.inputResponses)
            .containsExactly(KEY_ACTION, AppFunctionElicitationResponse.ActionFinished())
    }

    private fun createTestParameters(): AppFunctionData =
        AppFunctionData.Builder(TEST_PARAMETERS, AppFunctionComponentsMetadata())
            .setString("testString", "value")
            .build()

    private fun createFormDataResponse(value: String): AppFunctionElicitationResponse.FormData {
        val form =
            AppFunctionElicitationRequest.Form(
                instruction = "Choose a note",
                responseDataTypeMetadata = AppFunctionStringTypeMetadata(isNullable = false),
            )
        val data =
            AppFunctionData.Builder(form)
                .setString(
                    AppFunctionElicitationResponse.FormData.PROPERTY_ELICITATION_VALUE,
                    value,
                )
                .build()
        return AppFunctionElicitationResponse.FormData(data)
    }

    private fun android.app.appfunctions.ExecuteAppFunctionRequest.parceled():
        android.app.appfunctions.ExecuteAppFunctionRequest {
        val parcel = Parcel.obtain()
        try {
            writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return android.app.appfunctions.ExecuteAppFunctionRequest.CREATOR.createFromParcel(
                parcel
            )
        } finally {
            parcel.recycle()
        }
    }

    private companion object {
        const val KEY_FORM = "form"
        const val KEY_ACTION = "action"

        val TEST_PARAMETERS =
            listOf(
                AppFunctionParameterMetadata(
                    name = "testString",
                    isRequired = true,
                    dataType = AppFunctionStringTypeMetadata(isNullable = false),
                )
            )

        val TEST_METADATA =
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
