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
    /** The package name of the Android app called to execute the app function. */
    public val packageName: String,
    /** Indicates whether the function is enabled currently or not. */
    public val isEnabled: Boolean,
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
    /** A description of the AppFunction and its intended use. */
    public val description: String = "",
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppFunctionMetadata

        if (id != other.id) return false
        if (isEnabled != other.isEnabled) return false
        if (packageName != other.packageName) return false
        if (schema != other.schema) return false
        if (parameters != other.parameters) return false
        if (response != other.response) return false
        if (components != other.components) return false
        if (description != other.description) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(
            isEnabled,
            id,
            packageName,
            schema,
            parameters,
            response,
            components,
            description,
        )
    }

    override fun toString(): String = buildString {
        append("AppFunctionMetadata(")
        append("id='$id', ")
        append("packageName='$packageName', ")
        append("isEnabled=$isEnabled, ")
        append("schema=$schema, ")
        append("parameters=$parameters, ")
        append("response=$response, ")
        append("components=$components")
        append("description=$description")
        append(")")
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun copy(
        id: String = this.id,
        packageName: String = this.packageName,
        isEnabled: Boolean = this.isEnabled,
        schema: AppFunctionSchemaMetadata? = this.schema,
        parameters: List<AppFunctionParameterMetadata> = this.parameters,
        response: AppFunctionResponseMetadata = this.response,
        components: AppFunctionComponentsMetadata = this.components,
        description: String = this.description,
    ): AppFunctionMetadata {
        return AppFunctionMetadata(
            id = id,
            packageName = packageName,
            isEnabled = isEnabled,
            schema = schema,
            parameters = parameters,
            response = response,
            components = components,
            description = description,
        )
    }
}

/**
 * Represents the computed compile-time metadata of an AppFunction.
 *
 * This class is used to generate AppFunctionInventory and an intermediate representation to persist
 * the metadata in AppSearch.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class CompileTimeAppFunctionMetadata(
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
    /** A description of the AppFunction and its intended use. */
    public val description: String = "",
) {

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun copy(
        id: String? = null,
        isEnabledByDefault: Boolean? = null,
        schema: AppFunctionSchemaMetadata? = null,
        parameters: List<AppFunctionParameterMetadata>? = null,
        response: AppFunctionResponseMetadata? = null,
        components: AppFunctionComponentsMetadata? = null,
        description: String? = null,
    ): CompileTimeAppFunctionMetadata {
        return CompileTimeAppFunctionMetadata(
            id = id ?: this.id,
            isEnabledByDefault = isEnabledByDefault ?: this.isEnabledByDefault,
            schema = schema ?: this.schema,
            parameters = parameters ?: this.parameters,
            response = response ?: this.response,
            components = components ?: this.components,
            description = description ?: this.description,
        )
    }

    /**
     * Converts the [CompileTimeAppFunctionMetadata] to an [AppFunctionMetadataDocument].
     *
     * This method is used to persist the [CompileTimeAppFunctionMetadata] in a database.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toAppFunctionMetadataDocument(): AppFunctionMetadataDocument {
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

/** Represents the persistent storage format of [AppFunctionMetadata]. */
@Document(name = "AppFunctionStaticMetadata")
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
    @Document.BooleanProperty(name = "enabledByDefault") public val isEnabledByDefault: Boolean,
    /** The category of the schema, used to group related schemas. */
    @Document.StringProperty public val schemaCategory: String?,
    /** The unique name of the schema within its category. */
    @Document.StringProperty public val schemaName: String?,
    /** The version of the schema. This is used to track the changes to the schema over time. */
    @Document.LongProperty public val schemaVersion: Long?,
    // Below properties are nullable as they won't be populated in the underlying GD created by
    // legacy AppSearch indexer.
    /** The parameters of the AppFunction. */
    @Document.DocumentProperty public val parameters: List<AppFunctionParameterMetadataDocument>?,
    /** The response of the AppFunction. */
    @Document.DocumentProperty public val response: AppFunctionResponseMetadataDocument?,
    /** A description of the AppFunction and its intended use. */
    @Document.StringProperty public val description: String?,
)
