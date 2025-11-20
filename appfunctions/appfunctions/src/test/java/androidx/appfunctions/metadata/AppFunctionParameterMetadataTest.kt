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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionParameterMetadataTest {

    @Test
    fun appFunctionParameterMetadata_equalsAndHashCode() {
        val dataType1 = AppFunctionIntTypeMetadata(false)
        val dataType2 = AppFunctionStringTypeMetadata(true)

        val parameter1a =
            AppFunctionParameterMetadata("param1", false, dataType1, "Test description")
        val parameter1b =
            AppFunctionParameterMetadata("param1", false, dataType1, "Test description")
        val parameter2 =
            AppFunctionParameterMetadata("param2", true, dataType2, "Another description")

        assertThat(parameter1a).isEqualTo(parameter1b)
        assertThat(parameter1a.hashCode()).isEqualTo(parameter1b.hashCode())

        assertThat(parameter1a).isNotEqualTo(parameter2)
        assertThat(parameter1a.hashCode()).isNotEqualTo(parameter2.hashCode())
    }

    @Test
    fun toAppFunctionParameterMetadataDocument_returnsCorrectDocument() {
        val dataType = AppFunctionIntTypeMetadata(false)
        val parameter = AppFunctionParameterMetadata("param1", false, dataType, "Test description")

        val document = parameter.toAppFunctionParameterMetadataDocument()

        assertThat(document)
            .isEqualTo(
                AppFunctionParameterMetadataDocument(
                    name = "param1",
                    isRequired = false,
                    dataTypeMetadata =
                        AppFunctionDataTypeMetadataDocument(
                            type = AppFunctionDataTypeMetadata.TYPE_INT,
                            isNullable = false,
                        ),
                    description = "Test description",
                )
            )
    }

    @Test
    fun document_toAppFunctionParameterMetadata_returnsCorrectMetadata() {
        val document =
            AppFunctionParameterMetadataDocument(
                name = "param1",
                isRequired = false,
                dataTypeMetadata =
                    AppFunctionDataTypeMetadataDocument(
                        type = AppFunctionDataTypeMetadata.TYPE_INT,
                        isNullable = false,
                    ),
                description = "Test description",
            )

        val parameter = document.toAppFunctionParameterMetadata()

        assertThat(parameter)
            .isEqualTo(
                AppFunctionParameterMetadata(
                    name = "param1",
                    isRequired = false,
                    dataType = AppFunctionIntTypeMetadata(false),
                    description = "Test description",
                )
            )
    }
}
