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

import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document

/** Represent a function parameter. */
public class AppFunctionParameterMetadata(
    /** The name of the parameter. */
    public val name: String,
    /** Determines whether this parameter is mandatory. */
    public val isRequired: Boolean,
    /** The data type of the parameter. */
    public val dataType: AppFunctionDataTypeMetadata,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionParameterMetadata

        if (name != other.name) return false
        if (isRequired != other.isRequired) return false
        if (dataType != other.dataType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + isRequired.hashCode()
        result = 31 * result + dataType.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionParameterMetadata(" +
            "name=$name, " +
            "isRequired=$isRequired, " +
            "dataType=$dataType" +
            ")"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toAppFunctionParameterMetadataDocument(): AppFunctionParameterMetadataDocument {
        return AppFunctionParameterMetadataDocument(
            name = name,
            isRequired = isRequired,
            dataTypeMetadata = dataType.toAppFunctionDataTypeMetadataDocument()
        )
    }
}

/** Represents the persistent storage format of [AppFunctionParameterMetadata]. */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AppFunctionParameterMetadataDocument(
    @Document.Namespace public val namespace: String = APP_FUNCTION_NAMESPACE,
    @Document.Id public val id: String = APP_FUNCTION_ID_EMPTY,
    @Document.StringProperty public val name: String,
    @Document.BooleanProperty public val isRequired: Boolean,
    @Document.DocumentProperty public val dataTypeMetadata: AppFunctionDataTypeMetadataDocument
)
