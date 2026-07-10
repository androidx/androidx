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

/** Represents an AppFunction's response metadata. */
public class AppFunctionResponseMetadata
@JvmOverloads
constructor(
    /** The schema of the return value type. */
    public val valueType: AppFunctionDataTypeMetadata,
    /** A description of the response and its intended use. */
    public val description: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionResponseMetadata

        if (valueType != other.valueType) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        var result = valueType.hashCode()
        result = 31 * result + description.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionResponseMetadata(valueType=$valueType, description=$description)"
    }

    internal fun toAppFunctionResponseMetadataDocument(): AppFunctionResponseMetadataDocument {
        return AppFunctionResponseMetadataDocument(
            valueType = valueType.toAppFunctionDataTypeMetadataDocument(),
            description = description,
        )
    }
}

/** Represents the persistent storage format of [AppFunctionResponseMetadata]. */
@Document
internal data class AppFunctionResponseMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    /** The schema of the return type. */
    @Document.DocumentProperty val valueType: AppFunctionDataTypeMetadataDocument,
    @Document.StringProperty val description: String? = null,
) {
    fun toAppFunctionResponseMetadata(): AppFunctionResponseMetadata =
        AppFunctionResponseMetadata(
            valueType = valueType.toAppFunctionDataTypeMetadata(),
            description = description ?: "",
        )
}
