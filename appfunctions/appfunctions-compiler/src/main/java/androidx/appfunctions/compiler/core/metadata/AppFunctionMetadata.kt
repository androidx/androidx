/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.appfunctions.compiler.core.metadata

internal const val APP_FUNCTION_NAMESPACE = "appfunctions"
internal const val APP_FUNCTION_ID_EMPTY = "unused"

data class AppFunctionMetadata(
    val id: String,
    val packageName: String,
    val isEnabled: Boolean,
    val schema: AppFunctionSchemaMetadata?,
    val parameters: List<AppFunctionParameterMetadata>,
    val response: AppFunctionResponseMetadata,
    val components: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(),
)

data class CompileTimeAppFunctionMetadata(
    val id: String,
    val isEnabledByDefault: Boolean,
    val schema: AppFunctionSchemaMetadata?,
    val parameters: List<AppFunctionParameterMetadata>,
    val response: AppFunctionResponseMetadata,
    val components: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(),
    val description: String = "",
) {
    fun toAppFunctionMetadataDocument(): AppFunctionMetadataDocument {
        return AppFunctionMetadataDocument(
            id = id,
            isEnabledByDefault = isEnabledByDefault,
            schemaName = schema?.name,
            schemaCategory = schema?.category,
            schemaVersion = schema?.version,
            parameters = parameters.map { it.toAppFunctionParameterMetadataDocument() },
            response = response.toAppFunctionResponseMetadataDocument(),
            description = description,
        )
    }
}

data class AppFunctionMetadataDocument(
    val namespace: String = APP_FUNCTION_NAMESPACE,
    val id: String = APP_FUNCTION_ID_EMPTY,
    val isEnabledByDefault: Boolean,
    val schemaCategory: String?,
    val schemaName: String?,
    val schemaVersion: Long?,
    val parameters: List<AppFunctionParameterMetadataDocument>?,
    val response: AppFunctionResponseMetadataDocument?,
    val description: String = "",
)
