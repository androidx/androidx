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

import androidx.appsearch.annotation.Document

/** Represent a function parameter. */
public class AppFunctionParameterMetadata
@JvmOverloads
constructor(
    /** The name of the parameter. */
    public val name: String,
    /** Determines whether this parameter is mandatory. */
    public val isRequired: Boolean,
    /** The data type of the parameter. */
    public val dataType: AppFunctionDataTypeMetadata,
    /** Describes the parameter's intended use within the AppFunction, consumed by the LLM. */
    public val description: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionParameterMetadata

        if (name != other.name) return false
        if (isRequired != other.isRequired) return false
        if (dataType != other.dataType) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isRequired.hashCode()
        result = 31 * result + dataType.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionParameterMetadata(" +
            "name=$name, " +
            "isRequired=$isRequired, " +
            "dataType=$dataType," +
            "description=$description" +
            ")"
    }

    internal fun toAppFunctionParameterMetadataDocument(): AppFunctionParameterMetadataDocument {
        return AppFunctionParameterMetadataDocument(
            name = name,
            isRequired = isRequired,
            dataTypeMetadata = dataType.toAppFunctionDataTypeMetadataDocument(),
            description = description,
        )
    }
}

/** Represents the persistent storage format of [AppFunctionParameterMetadata]. */
@Document
internal data class AppFunctionParameterMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    @Document.StringProperty val name: String,
    @Document.BooleanProperty val isRequired: Boolean,
    @Document.DocumentProperty val dataTypeMetadata: AppFunctionDataTypeMetadataDocument,
    @Document.StringProperty val description: String? = null,
) {
    fun toAppFunctionParameterMetadata(): AppFunctionParameterMetadata =
        AppFunctionParameterMetadata(
            name = name,
            isRequired = isRequired,
            dataType = dataTypeMetadata.toAppFunctionDataTypeMetadata(),
            description = description ?: "",
        )
}
