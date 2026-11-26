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
import androidx.appfunctions.metadata.AppFunctionStringTypeMetadata
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 33)
class AppFunctionElicitationResponseTest {

    @Test
    fun formData_toBundleAndFromBundle_afterParceling() {
        val stringForm =
            AppFunctionElicitationRequest.Form(
                instruction = "Choose a note",
                responseDataTypeMetadata =
                    AppFunctionStringTypeMetadata(
                        isNullable = false,
                        enumValues = setOf("Groceries", "Tax Return"),
                    ),
            )
        val stringData =
            AppFunctionData.Builder(stringForm)
                .setString(
                    AppFunctionElicitationResponse.FormData.PROPERTY_ELICITATION_VALUE,
                    "Groceries",
                )
                .build()
        val response = AppFunctionElicitationResponse.FormData(stringData)

        val result = AppFunctionInputResponse.fromBundle(response.toBundle().parceled())

        assertThat(result).isInstanceOf(AppFunctionElicitationResponse.FormData::class.java)
        val formData = result as AppFunctionElicitationResponse.FormData
        assertThat(
                formData.data.getString(
                    AppFunctionElicitationResponse.FormData.PROPERTY_ELICITATION_VALUE
                )
            )
            .isEqualTo("Groceries")
    }

    @Test
    fun actionFinished_toBundleAndFromBundle_afterParceling() {
        val actionFinished = AppFunctionElicitationResponse.ActionFinished()

        val result = AppFunctionInputResponse.fromBundle(actionFinished.toBundle().parceled())

        assertThat(result).isEqualTo(actionFinished)
    }

    @Test
    fun fromBundle_unknownResponseType_returnsNull() {
        val bundle =
            Bundle().apply { putInt(AppFunctionInputResponse.KEY_RESPONSE_TYPE, Int.MAX_VALUE) }

        assertThat(AppFunctionInputResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_unknownElicitationType_returnsNull() {
        val bundle =
            Bundle().apply {
                putInt(
                    AppFunctionInputResponse.KEY_RESPONSE_TYPE,
                    AppFunctionInputResponse.RESPONSE_TYPE_ELICITATION,
                )
                putInt("elicitationType", Int.MAX_VALUE)
            }

        assertThat(AppFunctionInputResponse.fromBundle(bundle)).isNull()
    }

    @Test
    fun fromBundle_malformedFormData_throws() {
        val bundle =
            Bundle().apply {
                putInt(
                    AppFunctionInputResponse.KEY_RESPONSE_TYPE,
                    AppFunctionInputResponse.RESPONSE_TYPE_ELICITATION,
                )
                putInt("elicitationType", 1)
            }

        assertFailsWith<IllegalArgumentException> { AppFunctionInputResponse.fromBundle(bundle) }
    }

    private fun Bundle.parceled(): Bundle {
        val parcel = Parcel.obtain()
        try {
            parcel.writeBundle(this)
            parcel.setDataPosition(0)
            return requireNotNull(parcel.readBundle(javaClass.classLoader))
        } finally {
            parcel.recycle()
        }
    }
}
