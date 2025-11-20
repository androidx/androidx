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

class AppFunctionMetadataTest {

    @Test
    fun appFunctionMetadata_equalsAndHashCode() {
        val schema =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val parameters = emptyList<AppFunctionParameterMetadata>()
        val response =
            AppFunctionResponseMetadata(
                valueType = AppFunctionStringTypeMetadata(false),
                description = "The response description",
            )
        val description = "The function's description"
        val deprecation = AppFunctionDeprecationMetadata(message = "Function is deprecated")

        val metadata1 =
            AppFunctionMetadata(
                id = " id",
                packageName = "testPackage",
                isEnabled = true,
                schema = schema,
                parameters = parameters,
                response = response,
                description = description,
                deprecation = deprecation,
            )
        val metadata2 =
            AppFunctionMetadata(
                id = " id",
                packageName = "testPackage",
                isEnabled = true,
                schema = schema,
                parameters = parameters,
                response = response,
                description = description,
                deprecation = deprecation,
            )
        val metadata3 =
            AppFunctionMetadata(
                id = " id",
                packageName = "testPackage",
                isEnabled = false,
                schema = schema,
                parameters = parameters,
                response = response,
                description = description,
                deprecation = deprecation,
            )

        assertThat(metadata1).isEqualTo(metadata2)
        assertThat(metadata1.hashCode()).isEqualTo(metadata2.hashCode())
        assertThat(metadata1).isNotEqualTo(metadata3)
        assertThat(metadata1.hashCode()).isNotEqualTo(metadata3.hashCode())
    }

    @Test
    fun appFunctionMetadata_toAppFunctionMetadataDocument_returnsCorrectDocument() {
        val id = "fakeFunctionIdentifier"
        val isEnabledByDefault = true
        val schemaMetadata =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val primitiveTypeInt = AppFunctionIntTypeMetadata(true)
        val primitiveTypeLong = AppFunctionLongTypeMetadata(true)
        val parameters =
            listOf<AppFunctionParameterMetadata>(
                AppFunctionParameterMetadata(
                    name = "prop1",
                    isRequired = false,
                    dataType = primitiveTypeInt,
                    description = "test int parameter",
                ),
                AppFunctionParameterMetadata(
                    name = "prop2",
                    isRequired = true,
                    dataType = primitiveTypeLong,
                    description = "test long parameter",
                ),
            )
        val response =
            AppFunctionResponseMetadata(
                valueType = AppFunctionStringTypeMetadata(false),
                description = "The response description",
            )
        val description = "The function's description"
        val deprecation = AppFunctionDeprecationMetadata(message = "Function is deprecated")
        val appFunctionMetadata =
            CompileTimeAppFunctionMetadata(
                id = id,
                isEnabledByDefault = isEnabledByDefault,
                schema = schemaMetadata,
                parameters = parameters,
                response = response,
                description = description,
                deprecation = deprecation,
            )

        val actualAppFunctionMetadataDocument = appFunctionMetadata.toAppFunctionMetadataDocument()

        val expectedAppFunctionMetadataDocument =
            AppFunctionMetadataDocument(
                id = id,
                isEnabledByDefault = isEnabledByDefault,
                schemaName = schemaMetadata.name,
                schemaCategory = schemaMetadata.category,
                schemaVersion = schemaMetadata.version,
                parameters = parameters.map { it.toAppFunctionParameterMetadataDocument() },
                response = response.toAppFunctionResponseMetadataDocument(),
                description = description,
                deprecation = deprecation.toAppFunctionDeprecationMetadataDocument(),
            )
        assertThat(actualAppFunctionMetadataDocument).isEqualTo(expectedAppFunctionMetadataDocument)
    }
}
