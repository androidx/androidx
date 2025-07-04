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

/** Represents an AppFunction's response metadata. */
public class AppFunctionResponseMetadata(
    /** The schema of the return value type. */
    public val valueType: AppFunctionDataTypeMetadata
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionResponseMetadata

        if (valueType != other.valueType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = valueType.hashCode()
        return result
    }

    override fun toString(): String {
        return "AppFunctionResponseMetadata(valueType=$valueType)"
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toAppFunctionResponseMetadataDocument(): AppFunctionResponseMetadataDocument {
        return AppFunctionResponseMetadataDocument(
            valueType = valueType.toAppFunctionDataTypeMetadataDocument()
        )
    }
}

/** Represents the persistent storage format of [AppFunctionResponseMetadata]. */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AppFunctionResponseMetadataDocument(
    @Document.Namespace public val namespace: String = APP_FUNCTION_NAMESPACE,
    @Document.Id public val id: String = APP_FUNCTION_ID_EMPTY,
    /** The schema of the return type. */
    @Document.DocumentProperty public val valueType: AppFunctionDataTypeMetadataDocument,
) {
    public fun toAppFunctionResponseMetadata(): AppFunctionResponseMetadata =
        AppFunctionResponseMetadata(valueType = valueType.toAppFunctionDataTypeMetadata())
}
