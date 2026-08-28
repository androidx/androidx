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

import android.app.AppInteractionAttribution
import android.app.appfunctions.AppFunctionActivityId
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.appfunctions.metadata.AppFunctionMetadata

/** Represents a request to execute a specific app function. */
public class ExecuteAppFunctionRequest
@RestrictTo(LIBRARY_GROUP)
constructor(
    /** The package name of the app that hosts the function. */
    public val targetPackageName: String,
    /** The unique string identifier of the app function to be executed. */
    public val functionIdentifier: String,
    /**
     * The parameters required to invoke this function. Within this [AppFunctionData], the property
     * names are the names of the function parameters and the property values are the values of
     * those parameters.
     *
     * The data object may have missing parameters. Developers are advised to implement defensive
     * handling measures.
     */
    public val functionParameters: AppFunctionData,
    /**
     * The attribution that can be used by the privacy setting to provide transparency to the user
     * about why an app function was invoked.
     */
    @get:RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public val attribution: AppInteractionAttribution? = null,
    /** Whether the parameters in this request is encoded in the jetpack format or not. */
    @get:RestrictTo(LIBRARY_GROUP) public val useJetpackSchema: Boolean,
    /**
     * The [AppFunctionActivityId] for this request.
     *
     * This identifier is used to disambiguate between instances of the same app function running in
     * different activities when the function's [AppFunctionMetadata.scope] is
     * [AppFunctionMetadata.SCOPE_ACTIVITY].
     *
     * If the property's value is `null`, the request targets an app function that is not
     * [AppFunctionMetadata.SCOPE_ACTIVITY].
     */
    @get:RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public val activityId: AppFunctionActivityId? = null,
) {
    /**
     * Creates a new [ExecuteAppFunctionRequest].
     *
     * @param targetPackageName The package name of the app that hosts the function.
     * @param functionIdentifier The unique string identifier of the app function to be executed.
     * @param functionParameters The parameters required to invoke this function. Within this
     *   [AppFunctionData], the property names are the names of the function parameters and the
     *   property values are the values of those parameters. The data object may have missing
     *   parameters. Developers are advised to implement defensive handling measures.
     */
    public constructor(
        targetPackageName: String,
        functionIdentifier: String,
        functionParameters: AppFunctionData,
    ) : this(
        targetPackageName,
        functionIdentifier,
        functionParameters,
        attribution = null,
        useJetpackSchema = true,
        activityId = null,
    )

    /**
     * Creates a new [ExecuteAppFunctionRequest] with attribution.
     *
     * @param targetPackageName The package name of the app that hosts the function.
     * @param functionIdentifier The unique string identifier of the app function to be executed.
     * @param functionParameters The parameters required to invoke this function. Within this
     *   [AppFunctionData], the property names are the names of the function parameters and the
     *   property values are the values of those parameters. The data object may have missing
     *   parameters. Developers are advised to implement defensive handling measures.
     * @param attribution The attribution that can be used by the privacy setting to provide
     *   transparency to the user about why an app function was invoked.
     * @param activityId The [AppFunctionActivityId] for this request. This identifier is used to
     *   disambiguate between instances of the same app function running in different activities
     *   when the function's [AppFunctionMetadata.scope] is [AppFunctionMetadata.SCOPE_ACTIVITY]. If
     *   the property's value is `null`, the request targets an app function that is not
     *   [AppFunctionMetadata.SCOPE_ACTIVITY].
     */
    @RequiresApi(37)
    public constructor(
        targetPackageName: String,
        functionIdentifier: String,
        functionParameters: AppFunctionData,
        attribution: AppInteractionAttribution,
        activityId: AppFunctionActivityId? = null,
    ) : this(
        targetPackageName,
        functionIdentifier,
        functionParameters,
        attribution = attribution,
        useJetpackSchema = true,
        activityId = activityId,
    )

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
            .apply {
                if (Build.VERSION.SDK_INT >= 37) {
                    if (attribution != null) {
                        setAttribution(attribution)
                    }
                    setActivityId(activityId)
                }
            }
            .build()
    }

    override fun toString(): String {
        return "ExecuteAppFunctionRequest(functionMetadata.packageName=$targetPackageName, " +
            "functionMetadata.id=$functionIdentifier, functionParameters=$functionParameters, " +
            "activityId=$activityId)"
    }

    @RestrictTo(LIBRARY_GROUP)
    public fun copy(
        targetPackageName: String = this.targetPackageName,
        functionIdentifier: String = this.functionIdentifier,
        functionParameters: AppFunctionData = this.functionParameters,
        useJetpackSchema: Boolean = this.useJetpackSchema,
        activityId: AppFunctionActivityId? = this.activityId,
    ): ExecuteAppFunctionRequest =
        ExecuteAppFunctionRequest(
            targetPackageName,
            functionIdentifier,
            functionParameters,
            attribution,
            useJetpackSchema,
            activityId,
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
                activityId = null,
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
                attribution =
                    if (Build.VERSION.SDK_INT >= 37) {
                        this.attribution
                    } else {
                        null
                    },
                activityId =
                    if (Build.VERSION.SDK_INT >= 37) {
                        this.activityId
                    } else {
                        null
                    },
            )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun createAppFunctionDataWithParameterSpec(
            functionMetadata: AppFunctionMetadata,
            parametersAfd: AppFunctionData,
        ): AppFunctionData =
            parametersAfd.replaceSpecWith(functionMetadata.parameters, functionMetadata.components)
    }
}
