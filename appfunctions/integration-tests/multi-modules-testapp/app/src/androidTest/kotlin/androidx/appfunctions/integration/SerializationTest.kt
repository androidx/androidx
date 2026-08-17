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

package androidx.appfunctions.integration

import androidx.appfunction.integration.test.sharedschema.IntEnumSerializable
import androidx.appfunction.integration.test.sharedschema.UriConstraintSerializable
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Ignore
import org.junit.Test

class SerializationTest {
    // Keeping these tests here since the metadata doesn't become part of the schema inventory which
    // allows for skipping the validation.
    // TODO: b/446606781 - Remove or figure out the best place for serialization tests.
    @Test
    @Ignore(
        "b/446606781: Re-enable once serialization no longer relies on aggregation mode to validate"
    )
    fun serializeAppFunctionSerializable_failsForInvalidValues() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.serialize(
                IntEnumSerializable(value = -1),
                IntEnumSerializable::class.java,
            )
        }
    }

    @Test
    fun serializeAppFunctionSerializable_success() {
        val afd =
            AppFunctionData.serialize(
                IntEnumSerializable(value = 10),
                IntEnumSerializable::class.java,
            )

        assertThat(afd.getInt("value")).isEqualTo(10)
    }

    @Test
    @Ignore(
        "b/446606781: Re-enable once serialization no longer relies on aggregation mode to validate"
    )
    fun deserializeAppFunctionSerializable_failsForInvalidValues() {
        assertFailsWith<IllegalArgumentException> {
            AppFunctionData.Builder(
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "value",
                            isRequired = false,
                            dataType = AppFunctionIntTypeMetadata(isNullable = true),
                        )
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setInt("value", -1)
                .build()
                .deserialize(IntEnumSerializable::class.java)
        }
    }

    @Test
    fun deserializeAppFunctionSerializable_success() {
        val intEnumSerializable =
            AppFunctionData.Builder(
                    listOf(
                        AppFunctionParameterMetadata(
                            name = "value",
                            isRequired = false,
                            dataType = AppFunctionIntTypeMetadata(isNullable = true),
                        )
                    ),
                    AppFunctionComponentsMetadata(),
                )
                .setInt("value", 10)
                .build()
                .deserialize(IntEnumSerializable::class.java)

        assertThat(intEnumSerializable.value).isEqualTo(10)
    }

    @Ignore("TODO: b/547884111 - Remove once legacy Uri fallback is resolved.")
    @Test
    fun serializeAppFunctionSerializable_uriConstraint_success() {
        val uriConstraintSerializable =
            UriConstraintSerializable(
                uri = android.net.Uri.parse("content://media/external/images/media/1"),
                numericString = "12345",
            )

        val afd =
            AppFunctionData.serialize(
                uriConstraintSerializable,
                UriConstraintSerializable::class.java,
            )

        assertThat(afd.getString("uri")).isEqualTo("content://media/external/images/media/1")
        assertThat(afd.getString("numericString")).isEqualTo("12345")
    }

    @Test
    fun deserializeAppFunctionSerializable_uriConstraint_success() {
        val uri = android.net.Uri.parse("content://media/external/images/media/1")
        val afd =
            AppFunctionData.serialize(
                UriConstraintSerializable(uri = uri, numericString = "12345"),
                UriConstraintSerializable::class.java,
            )

        val deserialized = afd.deserialize(UriConstraintSerializable::class.java)

        assertThat(deserialized.uri).isEqualTo(uri)
        assertThat(deserialized.numericString).isEqualTo("12345")
    }
}
