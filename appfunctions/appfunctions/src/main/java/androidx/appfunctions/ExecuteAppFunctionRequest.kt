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

package androidx.appfunctions

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.appfunctions.metadata.AppFunctionMetadata

/**
 * Represents a request to execute a specific app function.
 *
 * @property targetPackageName The package name of the app that hosts the function.
 * @property functionIdentifier The unique string identifier of the app function to be executed.
 * @property functionParameters The parameters required to invoke this function. Within this
 *   [AppFunctionData], the property names are the names of the function parameters and the property
 *   values are the values of those parameters. The data object may have missing parameters.
 *   Developers are advised to implement defensive handling measures.
 */
public class ExecuteAppFunctionRequest
@RestrictTo(LIBRARY_GROUP)
constructor(
    public val targetPackageName: String,
    public val functionIdentifier: String,
    public val functionParameters: AppFunctionData,
    /** Whether the parameters in this request is encoded in the jetpack format or not. */
    @get:RestrictTo(LIBRARY_GROUP) public val useJetpackSchema: Boolean,
) {
    public constructor(
        targetPackageName: String,
        functionIdentifier: String,
        functionParameters: AppFunctionData,
    ) : this(targetPackageName, functionIdentifier, functionParameters, useJetpackSchema = true)

    internal fun toPlatformExtensionClass():
        com.android.extensions.appfunctions.ExecuteAppFunctionRequest {
        return com.android.extensions.appfunctions.ExecuteAppFunctionRequest.Builder(
                targetPackageName,
                functionIdentifier,
            )
            .setParameters(functionParameters.genericDocument)
            .setExtras(
                Bundle().apply {
                    putBundle(EXTRA_PARAMETERS, functionParameters.extras)
                    putBoolean(EXTRA_USE_JETPACK_SCHEMA, useJetpackSchema)
                }
            )
            .build()
    }

    /**
     * Converts [androidx.appfunctions.ExecuteAppFunctionRequest] to
     * [android.app.appfunctions.ExecuteAppFunctionRequest].
     *
     * @return The converted [android.app.appfunctions.ExecuteAppFunctionRequest].
     */
    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    public fun toPlatformExecuteAppFunctionRequest():
        android.app.appfunctions.ExecuteAppFunctionRequest {
        return android.app.appfunctions.ExecuteAppFunctionRequest.Builder(
                targetPackageName,
                functionIdentifier,
            )
            .setParameters(functionParameters.genericDocument)
            .setExtras(
                Bundle().apply {
                    putBundle(EXTRA_PARAMETERS, functionParameters.extras)
                    putBoolean(EXTRA_USE_JETPACK_SCHEMA, useJetpackSchema)
                }
            )
            .build()
    }

    override fun toString(): String {
        return "ExecuteAppFunctionRequest(functionMetadata.packageName=$targetPackageName, " +
            "functionMetadata.id=$functionIdentifier, functionParameters=$functionParameters)"
    }

    @RestrictTo(LIBRARY_GROUP)
    public fun copy(
        targetPackageName: String = this.targetPackageName,
        functionIdentifier: String = this.functionIdentifier,
        functionParameters: AppFunctionData = this.functionParameters,
        useJetpackSchema: Boolean = this.useJetpackSchema,
    ): ExecuteAppFunctionRequest =
        ExecuteAppFunctionRequest(
            targetPackageName,
            functionIdentifier,
            functionParameters,
            useJetpackSchema,
        )

    public companion object {
        internal const val EXTRA_PARAMETERS = "androidXAppfunctionsExtraParameters"
        internal const val EXTRA_USE_JETPACK_SCHEMA = "androidXAppfunctionsExtraUseJetpackSchema"

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        internal fun fromPlatformExtensionClass(
            request: com.android.extensions.appfunctions.ExecuteAppFunctionRequest,
            functionMetadata: AppFunctionMetadata,
        ): ExecuteAppFunctionRequest =
            ExecuteAppFunctionRequest(
                targetPackageName = request.targetPackageName,
                functionIdentifier = request.functionIdentifier,
                functionParameters =
                    createAppFunctionDataWithParameterSpec(
                        functionMetadata,
                        AppFunctionData(
                            request.parameters,
                            request.extras.getBundle(EXTRA_PARAMETERS) ?: Bundle.EMPTY,
                        ),
                    ),
                useJetpackSchema = request.extras.getBoolean(EXTRA_USE_JETPACK_SCHEMA, false),
            )

        /**
         * Creates a [androidx.appfunctions.ExecuteAppFunctionRequest] from
         * [android.app.appfunctions.ExecuteAppFunctionRequest].
         *
         * The provided [AppFunctionMetadata] is used to validate the created
         * [androidx.appfunctions.ExecuteAppFunctionRequest].
         *
         * @param functionMetadata the [AppFunctionMetadata] of the function to be executed.
         * @return The created [androidx.appfunctions.ExecuteAppFunctionRequest].
         */
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        @JvmStatic
        public fun android.app.appfunctions.ExecuteAppFunctionRequest
            .toCompatExecuteAppFunctionRequest(
            functionMetadata: AppFunctionMetadata
        ): ExecuteAppFunctionRequest =
            ExecuteAppFunctionRequest(
                targetPackageName = this.targetPackageName,
                functionIdentifier = this.functionIdentifier,
                functionParameters =
                    createAppFunctionDataWithParameterSpec(
                        functionMetadata,
                        AppFunctionData(
                            this.parameters,
                            this.extras.getBundle(EXTRA_PARAMETERS) ?: Bundle.EMPTY,
                        ),
                    ),
                useJetpackSchema = this.extras.getBoolean(EXTRA_USE_JETPACK_SCHEMA, false),
            )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun createAppFunctionDataWithParameterSpec(
            functionMetadata: AppFunctionMetadata,
            parametersAfd: AppFunctionData,
        ): AppFunctionData =
            parametersAfd.replaceSpecWith(functionMetadata.parameters, functionMetadata.components)
    }
}
