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

package androidx.appfunctions.compiler.core.metadata

data class AppFunctionParameterMetadata(
    val name: String,
    val isRequired: Boolean,
    val dataType: AppFunctionDataTypeMetadata,
    val description: String,
) {
    fun toAppFunctionParameterMetadataDocument(): AppFunctionParameterMetadataDocument {
        return AppFunctionParameterMetadataDocument(
            name = name,
            isRequired = isRequired,
            dataTypeMetadata = dataType.toAppFunctionDataTypeMetadataDocument(),
            description = description,
        )
    }
}

data class AppFunctionParameterMetadataDocument(
    val namespace: String = APP_FUNCTION_NAMESPACE,
    val id: String = APP_FUNCTION_ID_EMPTY,
    val name: String,
    val isRequired: Boolean,
    val dataTypeMetadata: AppFunctionDataTypeMetadataDocument,
    val description: String,
)
