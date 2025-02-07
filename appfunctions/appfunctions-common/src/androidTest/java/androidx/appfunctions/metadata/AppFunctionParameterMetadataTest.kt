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

package androidx.appfunctions.metadata

import androidx.appfunctions.metadata.AppFunctionDataTypeMetadata.Companion.TYPE_INT
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionParameterMetadataTest {
    @Test
    fun appFunctionParameterMetadata_equalsAndHashCode() {
        val parameter1a =
            AppFunctionParameterMetadata(
                name = "parameter1",
                isRequired = true,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(
                        type = TYPE_INT,
                        isNullable = false,
                    )
            )
        val parameter1b =
            AppFunctionParameterMetadata(
                name = "parameter1",
                isRequired = true,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(
                        type = TYPE_INT,
                        isNullable = false,
                    )
            )
        val parameter2 =
            AppFunctionParameterMetadata(
                name = "parameter1",
                isRequired = true,
                dataType =
                    AppFunctionObjectTypeMetadata(
                        properties = emptyMap(),
                        required = emptyList(),
                        qualifiedName = "qualifedName",
                        isNullable = true,
                    )
            )

        assertThat(parameter1a).isEqualTo(parameter1b)
        assertThat(parameter1a.hashCode()).isEqualTo(parameter1b.hashCode())
        assertThat(parameter1a).isNotEqualTo(parameter2)
        assertThat(parameter1a.hashCode()).isNotEqualTo(parameter2.hashCode())
    }

    @Test
    fun appFunctionParameterMetadata_toAppFunctionParameterMetadataDocument_returnsCorrectDocument() {
        val parameter =
            AppFunctionParameterMetadata(
                name = "parameter1",
                isRequired = false,
                dataType =
                    AppFunctionPrimitiveTypeMetadata(
                        type = TYPE_INT,
                        isNullable = false,
                    )
            )

        val document = parameter.toAppFunctionParameterMetadataDocument()

        assertThat(document)
            .isEqualTo(
                AppFunctionParameterMetadataDocument(
                    name = "parameter1",
                    isRequired = false,
                    dataTypeMetadata =
                        AppFunctionDataTypeMetadataDocument(
                            type = TYPE_INT,
                            isNullable = false,
                        )
                )
            )
    }
}
