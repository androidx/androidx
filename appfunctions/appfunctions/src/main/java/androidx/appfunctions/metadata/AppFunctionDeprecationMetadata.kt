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

/** Contains deprecation details for an AppFunction. */
public class AppFunctionDeprecationMetadata(
    /** The message explaining the deprecation and recommending an alternative API to use. */
    public val message: String
) {
    internal fun toAppFunctionDeprecationMetadataDocument():
        AppFunctionDeprecationMetadataDocument {
        return AppFunctionDeprecationMetadataDocument(message = message)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionDeprecationMetadata

        return message == other.message
    }

    override fun hashCode(): Int {
        return message.hashCode()
    }

    override fun toString(): String {
        return "AppFunctionDeprecationMetadata(message='$message')"
    }
}

@Document
internal data class AppFunctionDeprecationMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    @Document.StringProperty val message: String,
) {
    fun toAppFunctionDeprecationMetadata(): AppFunctionDeprecationMetadata {
        return AppFunctionDeprecationMetadata(message = message)
    }
}
