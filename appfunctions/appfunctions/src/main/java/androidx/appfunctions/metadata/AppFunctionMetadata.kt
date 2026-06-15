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

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.internal.GenericDocumentUtils
import androidx.appfunctions.internal.GenericDocumentUtils.safeCastToDocumentClass
import androidx.appfunctions.internal.SchemaAppFunctionInventory
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
// TODO(b/500667251): Replace this constructor with the secondary one once migrated all usages.
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
    /**
     * Deprecation details about the function, if the AppFunction is deprecated. This will be `null`
     * if the function is not deprecated.
     */
    public val deprecation: AppFunctionDeprecationMetadata? = null,
    /** The name of the AppFunction. */
    internal val name: AppFunctionName = AppFunctionName(packageName, id),
    /** The metadata of the package providing this AppFunction. */
    internal val packageMetadata: AppFunctionPackageMetadata =
        AppFunctionPackageMetadata(
            packageName = packageName,
            appFunctions = listOf(),
            components = components,
        ),
) {
    @JvmOverloads
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(
        /** The name of the AppFunction. */
        name: AppFunctionName,
        /**
         * The predefined schema of the AppFunction. If null, it indicates this function is not
         * implement a particular predefined schema.
         */
        schema: AppFunctionSchemaMetadata?,
        /** The parameters of the AppFunction. */
        parameters: List<AppFunctionParameterMetadata>,
        /** The response of the AppFunction. */
        response: AppFunctionResponseMetadata,
        /** The metadata of the package providing this AppFunction. */
        packageMetadata: AppFunctionPackageMetadata,
        // TODO(b/500667251): remove isEnabled property. AppFunctionMetadata should now contain
        //  static info only, in line with platform class, hence using a default false value until
        //  we migrate.
        /** Indicates whether the function is enabled currently or not. */
        isEnabled: Boolean,
        /** A description of the AppFunction and its intended use. */
        description: String = "",
        /**
         * Deprecation details about the function, if the AppFunction is deprecated. This will be
         * `null` if the function is not deprecated.
         */
        deprecation: AppFunctionDeprecationMetadata? = null,
    ) : this(
        id = name.functionIdentifier,
        packageName = name.packageName,
        isEnabled = isEnabled,
        schema = schema,
        parameters = parameters,
        response = response,
        components = packageMetadata.components,
        description = description,
        deprecation = deprecation,
        packageMetadata = packageMetadata,
        name = name,
    )

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
        if (deprecation != other.deprecation) return false
        if (name != other.name) return false
        if (packageMetadata != other.packageMetadata) return false

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
            deprecation,
            name,
            packageMetadata,
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
        append("components=$components, ")
        append("description='$description', ")
        append("deprecation=$deprecation, ")
        append("packageMetadata=$packageMetadata, ")
        append("name=$name")
        append(")")
    }

    internal fun copy(
        id: String = this.id,
        packageName: String = this.packageName,
        isEnabled: Boolean = this.isEnabled,
        schema: AppFunctionSchemaMetadata? = this.schema,
        parameters: List<AppFunctionParameterMetadata> = this.parameters,
        response: AppFunctionResponseMetadata = this.response,
        components: AppFunctionComponentsMetadata = this.components,
        description: String = this.description,
        deprecation: AppFunctionDeprecationMetadata? = this.deprecation,
        name: AppFunctionName = this.name,
        packageMetadata: AppFunctionPackageMetadata = this.packageMetadata,
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
            deprecation = deprecation,
            name = name,
            packageMetadata = packageMetadata,
        )
    }

    /** Specifies the lifecycle scope of an AppFunction. */
    @Retention(AnnotationRetention.SOURCE)
    @androidx.annotation.StringDef(SCOPE_GLOBAL, SCOPE_ACTIVITY)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class AppFunctionScope

    public companion object {

        // TODO(b/501032667): Update links to the androidx verions of
        // ExecuteAppFunctionRequest.setActivityId,
        // getAppFunctionStates, getAppFunctionActivityStates,
        // registerAppFunction
        /**
         * Indicates it is a globally-scoped app function.
         *
         * There can be at most one app function implementation with the same name available with
         * this scope. This is useful for functions that are tied to a singleton component, such as
         * a foreground service.
         *
         * When using [android.app.appfunctions.AppFunctionManager.registerAppFunction], the
         * function remains registered until it is explicitly unregistered or the calling context is
         * destroyed.
         *
         * To execute a globally-scoped function, the caller of
         * [androidx.appfunctions.AppFunctionManager.executeAppFunction] must not use
         * [android.app.appfunctions.ExecuteAppFunctionRequest#setActivityId] (or set it to null),
         * otherwise [androidx.appfunctions.AppFunctionFunctionNotFoundException] will be returned.
         *
         * This is always the scope for [androidx.appfunctions.AppFunctionService]-based functions.
         *
         * **IMPORTANT:** Functions provided with
         * [android.app.appfunctions.AppFunctionManager.registerAppFunction] called from an
         * [android.app.Activity] context should prefer [SCOPE_ACTIVITY]. Only use [SCOPE_GLOBAL]
         * for such functions if you are absolutely sure there can be only one instance of that
         * activity.
         */
        @Suppress("InlinedApi")
        public const val SCOPE_GLOBAL: String =
            android.app.appfunctions.AppFunctionMetadata.PROPERTY_VALUE_SCOPE_GLOBAL

        /**
         * Indicates it is an activity-scoped app function.
         *
         * Multiple app function implementations with the same name can exist simultaneously, each
         * registered from a different [android.app.Activity] instance, which is identified by an
         * [android.app.appfunctions.AppFunctionActivityId].
         *
         * Functions with this scope must be registered by an
         * [androidx.appfunctions.AppFunctionManager] that is created from an [android.app.Activity]
         * context.
         *
         * To execute an activity-scoped function, the caller of
         * [androidx.appfunctions.AppFunctionManager.executeAppFunction] must use
         * [android.app.appfunctions.ExecuteAppFunctionRequest#setActivityId], otherwise
         * [androidx.appfunctions.AppFunctionFunctionNotFoundException] will be returned.
         *
         * To discover the specific activities where an activity-scoped function is currently
         * registered, see [android.app.appfunctions.AppFunctionManager.getAppFunctionStates] and
         * [android.app.appfunctions.AppFunctionManager.getAppFunctionActivityStates].
         *
         * The function remains registered until it is explicitly unregistered or the activity is
         * destroyed.
         *
         * **IMPORTANT:** Functions provided with
         * [android.app.appfunctions.AppFunctionManager.registerAppFunction] called from an
         * [android.app.Activity] context should prefer [SCOPE_ACTIVITY]. Only use [SCOPE_GLOBAL]
         * for such functions if you are absolutely sure there can be only one instance of that
         * activity.
         */
        @Suppress("InlinedApi")
        public const val SCOPE_ACTIVITY: String =
            android.app.appfunctions.AppFunctionMetadata.PROPERTY_VALUE_SCOPE_ACTIVITY

        /**
         * Converts [android.app.appfunctions.AppFunctionMetadata] to
         * [androidx.appfunctions.metadata.AppFunctionMetadata].
         */
        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        internal fun fromPlatformAppFunctionMetadata(
            platformMetadata: android.app.appfunctions.AppFunctionMetadata,
            schemaAppFunctionInventory: SchemaAppFunctionInventory? = null,
        ): AppFunctionMetadata? {
            val document =
                GenericDocumentUtils.fromPlatformToJetpackGenericDocument(
                    platformMetadata.metadataDocument
                )
            val staticMetadataDocument =
                safeCastToDocumentClass<AppFunctionMetadataDocument>(document) ?: return null

            val schemaMetadata =
                platformMetadata.schemaMetadata?.let { platformSchema ->
                    AppFunctionSchemaMetadata(
                        category = platformSchema.category,
                        name = platformSchema.name,
                        version = platformSchema.version,
                    )
                }

            val packageMetadata =
                AppFunctionPackageMetadata.fromPlatformAppFunctionPackageMetadata(
                    platformPackageMetadata = platformMetadata.packageMetadata,
                    schemaAppFunctionInventory = schemaAppFunctionInventory,
                    schemaMetadata = schemaMetadata,
                    isFromDynamicIndexer =
                        isAppFunctionMetadataDocumentFromDynamicIndexer(staticMetadataDocument),
                )

            return create(
                appFunctionName =
                    AppFunctionName.fromPlatformAppFunctionName(platformMetadata.name),
                staticMetadataDocument = staticMetadataDocument,
                isEnabled = staticMetadataDocument.isEnabledByDefault,
                packageMetadata = packageMetadata,
                schemaAppFunctionInventory = schemaAppFunctionInventory,
            )
        }

        /** Creates an [AppFunctionMetadata] from static metadata details. */
        internal fun create(
            appFunctionName: AppFunctionName,
            staticMetadataDocument: AppFunctionMetadataDocument,
            isEnabled: Boolean,
            packageMetadata: AppFunctionPackageMetadata,
            schemaAppFunctionInventory: SchemaAppFunctionInventory? = null,
        ): AppFunctionMetadata? {
            val schemaName = staticMetadataDocument.schemaName
            val schemaCategory = staticMetadataDocument.schemaCategory
            val schemaVersion = staticMetadataDocument.schemaVersion ?: 0L
            val schemaMetadata =
                if (schemaName != null && schemaCategory != null && schemaVersion > 0) {
                    AppFunctionSchemaMetadata(
                        category = schemaCategory,
                        name = schemaName,
                        version = schemaVersion,
                    )
                } else {
                    if (schemaName != null || schemaCategory != null || schemaVersion != 0L) {
                        Log.e(
                            APP_FUNCTIONS_TAG,
                            "Unexpected state: schemaName=$schemaName, " +
                                "schemaCategory=$schemaCategory, " +
                                "schemaVersion=$schemaVersion",
                        )
                    }
                    null
                }

            val parameterMetadata =
                getAppFunctionParameterMetadata(
                    staticMetadataDocument,
                    schemaMetadata,
                    schemaAppFunctionInventory,
                ) ?: return null
            val responseMetadata =
                getAppFunctionResponseMetadata(
                    staticMetadataDocument,
                    schemaMetadata,
                    schemaAppFunctionInventory,
                ) ?: return null

            val deprecationMetadata = getAppFunctionDeprecationMetadata(staticMetadataDocument)

            return AppFunctionMetadata(
                name = appFunctionName,
                schema = schemaMetadata,
                parameters = parameterMetadata,
                response = responseMetadata,
                packageMetadata = packageMetadata,
                isEnabled = isEnabled,
                description = staticMetadataDocument.description ?: "",
                deprecation = deprecationMetadata,
            )
        }

        private fun getAppFunctionParameterMetadata(
            appFunctionMetadataDocument: AppFunctionMetadataDocument,
            schemaMetadata: AppFunctionSchemaMetadata?,
            schemaAppFunctionInventory: SchemaAppFunctionInventory? = null,
        ): List<AppFunctionParameterMetadata>? {
            if (isAppFunctionMetadataDocumentFromDynamicIndexer(appFunctionMetadataDocument)) {
                return appFunctionMetadataDocument.parameters?.map(
                    AppFunctionParameterMetadataDocument::toAppFunctionParameterMetadata
                ) ?: emptyList()
            }

            return if (schemaMetadata == null) {
                null
            } else {
                schemaAppFunctionInventory?.schemaFunctionsMap?.get(schemaMetadata)?.parameters
            }
        }

        private fun getAppFunctionResponseMetadata(
            appFunctionMetadataDocument: AppFunctionMetadataDocument,
            schemaMetadata: AppFunctionSchemaMetadata?,
            schemaAppFunctionInventory: SchemaAppFunctionInventory? = null,
        ): AppFunctionResponseMetadata? {
            if (isAppFunctionMetadataDocumentFromDynamicIndexer(appFunctionMetadataDocument)) {
                return checkNotNull(appFunctionMetadataDocument.response)
                    .toAppFunctionResponseMetadata()
            }

            return if (schemaMetadata == null) {
                null
            } else {
                schemaAppFunctionInventory?.schemaFunctionsMap?.get(schemaMetadata)?.response
            }
        }

        private fun getAppFunctionDeprecationMetadata(
            appFunctionMetadataDocument: AppFunctionMetadataDocument
        ): AppFunctionDeprecationMetadata? {
            return appFunctionMetadataDocument.deprecation?.toAppFunctionDeprecationMetadata()
        }

        internal fun isAppFunctionMetadataDocumentFromDynamicIndexer(
            document: AppFunctionMetadataDocument
        ): Boolean {
            return document.response != null
        }
    }
}

/**
 * Represents the computed compile-time metadata of an AppFunction.
 *
 * This class is used to generate AppFunctionInventory and an intermediate representation to persist
 * the metadata in AppSearch.
 */
// TODO(b/438412432): Hide this API as internal.
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
    // TODO: b/444163595 - Remove once components are moved to package metadata
    /** Reusable components that could be shared within the function specification. */
    public val components: AppFunctionComponentsMetadata = AppFunctionComponentsMetadata(),
    /** A description of the AppFunction and its intended use. */
    public val description: String = "",
    /**
     * Deprecation details about the function, if the AppFunction is deprecated. This will be `null`
     * if the function is not deprecated.
     */
    public val deprecation: AppFunctionDeprecationMetadata? = null,
) {

    internal fun copy(
        id: String? = null,
        isEnabledByDefault: Boolean? = null,
        schema: AppFunctionSchemaMetadata? = null,
        parameters: List<AppFunctionParameterMetadata>? = null,
        response: AppFunctionResponseMetadata? = null,
        components: AppFunctionComponentsMetadata? = null,
        description: String? = null,
        deprecation: AppFunctionDeprecationMetadata? = null,
    ): CompileTimeAppFunctionMetadata {
        return CompileTimeAppFunctionMetadata(
            id = id ?: this.id,
            isEnabledByDefault = isEnabledByDefault ?: this.isEnabledByDefault,
            schema = schema ?: this.schema,
            parameters = parameters ?: this.parameters,
            response = response ?: this.response,
            components = components ?: this.components,
            description = description ?: this.description,
            deprecation = deprecation ?: this.deprecation,
        )
    }

    /**
     * Converts the [CompileTimeAppFunctionMetadata] to an [AppFunctionMetadataDocument].
     *
     * This method is used to persist the [CompileTimeAppFunctionMetadata] in a database.
     */
    internal fun toAppFunctionMetadataDocument(): AppFunctionMetadataDocument {
        return AppFunctionMetadataDocument(
            id = id,
            isEnabledByDefault = isEnabledByDefault,
            schemaName = schema?.name,
            schemaCategory = schema?.category,
            schemaVersion = schema?.version,
            parameters = parameters.map { it.toAppFunctionParameterMetadataDocument() },
            response = response.toAppFunctionResponseMetadataDocument(),
            description = description,
            deprecation = deprecation?.toAppFunctionDeprecationMetadataDocument(),
        )
    }
}

/** Represents the persistent storage format of [AppFunctionMetadata]. */
@Document(name = "AppFunctionStaticMetadata")
@Suppress("InlinedApi")
internal data class AppFunctionMetadataDocument(
    @Document.Namespace val namespace: String = APP_FUNCTION_NAMESPACE,
    /** The id of the AppFunction. */
    @Document.Id val id: String = APP_FUNCTION_ID_EMPTY,
    /**
     * Indicates whether the function is enabled by default.
     *
     * This represents the initial configuration and might not represent the current enabled state,
     * as it could be modified at runtime.
     */
    @Document.BooleanProperty(name = "enabledByDefault") val isEnabledByDefault: Boolean,
    /** The category of the schema, used to group related schemas. */
    @Document.StringProperty val schemaCategory: String?,
    /** The unique name of the schema within its category. */
    @Document.StringProperty val schemaName: String?,
    /** The version of the schema. This is used to track the changes to the schema over time. */
    @Document.LongProperty val schemaVersion: Long?,
    // Below properties are nullable as they won't be populated in the underlying GD created by
    // legacy AppSearch indexer.
    /** The parameters of the AppFunction. */
    @Document.DocumentProperty val parameters: List<AppFunctionParameterMetadataDocument>?,
    /** The response of the AppFunction. */
    @Document.DocumentProperty val response: AppFunctionResponseMetadataDocument?,
    /** A description of the AppFunction and its intended use. */
    @Document.StringProperty val description: String? = null,
    /** Indicates whether the function is deprecated or not. */
    @Document.DocumentProperty val deprecation: AppFunctionDeprecationMetadataDocument? = null,
    /** The lifecycle scope of the AppFunction. */
    @Document.StringProperty(name = android.app.appfunctions.AppFunctionMetadata.PROPERTY_SCOPE)
    val scope: String? = null,
)
