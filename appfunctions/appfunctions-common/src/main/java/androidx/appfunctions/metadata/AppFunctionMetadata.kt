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

package androidx.appfunctions.metadata

import androidx.annotation.RestrictTo
import androidx.appsearch.annotation.Document
import java.util.Objects

internal const val APP_FUNCTION_NAMESPACE = "appfunctions"
internal const val APP_FUNCTION_ID_EMPTY = "unused"

/**
 * Represents an AppFunction's metadata.
 *
 * The class provides the essential information to call an AppFunction. The caller has two options
 * to invoke a function:
 * * Using function schema to identify input/output: The function schema defines the input and
 *   output of a function. If [schema] is not null, the caller can look up the input/output
 *   information based on the schema definition, and call the function accordingly.
 * * Examine [parameters] and [response]: A function metadata also has parameters and response
 *   properties describe the input and output of a function. The caller can examine these fields to
 *   obtain the input/output information, and call the function accordingly.
 */
public class AppFunctionMetadata
@JvmOverloads
constructor(
    /**
     * The ID used in an [androidx.appfunctions.ExecuteAppFunctionRequest] to refer to this
     * AppFunction.
     */
    public val id: String,

    /**
     * Indicates whether the function is enabled by default.
     *
     * This represents the initial configuration and might not represent the current enabled state,
     * as it could be modified at runtime.
     */
    public val isEnabledByDefault: Boolean,
    /**
     * The predefined schema of the AppFunction. If null, it indicates this function is not
     * implement a particular predefined schema.
     */
    public val schema: AppFunctionSchemaMetadata?,
    /** The parameters of the AppFunction. */
    public val parameters: List<AppFunctionParameterMetadata>,
    /** The response of the AppFunction. */
    public val response: AppFunctionResponseMetadata,
    /** Reusable components that could be shared within the function specification. */
    public val components: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionMetadata

        if (id != other.id) return false
        if (isEnabledByDefault != other.isEnabledByDefault) return false
        if (schema != other.schema) return false
        if (parameters != other.parameters) return false
        if (response != other.response) return false
        if (components != other.components) return false

        return true
    }

    override fun hashCode(): Int =
        Objects.hash(id, isEnabledByDefault, schema, parameters, response, components)

    override fun toString(): String {
        return "AppFunctionMetadata(isEnabledByDefault=$isEnabledByDefault, schema=$schema, parameters=$parameters, response=$response, components=$components)"
    }

    /**
     * Converts the [AppFunctionMetadata] to an [AppFunctionMetadataDocument].
     *
     * This method is used to persist the [AppFunctionMetadata] in a database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toAppFunctionMetadataDocument(): AppFunctionMetadataDocument {
        return AppFunctionMetadataDocument(
            id = id,
            isEnabledByDefault = isEnabledByDefault,
            schema =
                if (schema != null) {
                    val functionSchemaMetadata = checkNotNull(schema)
                    functionSchemaMetadata.toAppFunctionSchemaMetadataDocument()
                } else {
                    null
                },
            parameters = parameters.map { it.toAppFunctionParameterMetadataDocument() },
            response = response.toAppFunctionResponseMetadataDocument(),
            components = components.toAppFunctionComponentsMetadataDocument()
        )
    }
}

/** Represents the persistent storage format of [AppFunctionMetadata]. */
@Document
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class AppFunctionMetadataDocument(
    @Document.Namespace public val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the AppFunction. */
    @Document.Id public val id: String = APP_FUNCTION_ID_EMPTY,
    /**
     * Indicates whether the function is enabled by default.
     *
     * This represents the initial configuration and might not represent the current enabled state,
     * as it could be modified at runtime.
     */
    @Document.BooleanProperty public val isEnabledByDefault: Boolean,
    /** The predefined schema of the AppFunction. */
    @Document.DocumentProperty public val schema: AppFunctionSchemaMetadataDocument?,
    /** The parameters of the AppFunction. */
    @Document.DocumentProperty public val parameters: List<AppFunctionParameterMetadataDocument>,
    /** The response of the AppFunction. */
    @Document.DocumentProperty public val response: AppFunctionResponseMetadataDocument,
    /** The reusable components for the AppFunction. */
    @Document.DocumentProperty public val components: AppFunctionComponentsMetadataDocument,
)
