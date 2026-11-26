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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.appfunctions.internal.AppFunctionLibraryConfiguration.isLoggingEnabled
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
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
    /**
     * The attribution that can be used by the privacy setting to provide transparency to the user
     * about why an app function was invoked.
     */
    @get:RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    public val attribution: AppInteractionAttribution? = null,
    /**
     * Responses to the [AppFunctionInputRequiredException.inputRequests] from the previous call,
     * keyed by the key of the request that each response answers.
     *
     * The map contains one entry for each request that the caller answers. If the caller answers
     * only some of the requests, the map has no entry for each unanswered request.
     *
     * The map is empty if the request does not respond to an [AppFunctionInputRequiredException].
     */
    public val inputResponses: Map<String, AppFunctionInputResponse> = emptyMap(),
    /** Indicates whether to enable AppFunction execution's safety evaluation or not. */
    internal val enableSafetyEvaluation: Boolean = false,
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
     * @see Builder
     */
    public constructor(
        targetPackageName: String,
        functionIdentifier: String,
        functionParameters: AppFunctionData,
    ) : this(
        targetPackageName = targetPackageName,
        functionIdentifier = functionIdentifier,
        functionParameters = functionParameters,
        activityId = null,
        attribution = null,
        inputResponses = emptyMap(),
        enableSafetyEvaluation = false,
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
        targetPackageName = targetPackageName,
        functionIdentifier = functionIdentifier,
        functionParameters = functionParameters,
        activityId = activityId,
        attribution = attribution,
        inputResponses = emptyMap(),
        enableSafetyEvaluation = false,
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
                    putInputResponses(inputResponses)
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
                    putInputResponses(inputResponses)
                    if (enableSafetyEvaluation) {
                        putBoolean(
                            EXTRA_ENABLE_APP_FUNCTION_EVALUATION,
                            true,
                        )
                    }
                }
            )
            .apply {
                if (Build.VERSION.SDK_INT >= 37) {
                    attribution?.let { setAttribution(it) }
                    setActivityId(activityId)
                }
            }
            .build()
    }

    override fun toString(): String {
        return "ExecuteAppFunctionRequest(functionMetadata.packageName=$targetPackageName, " +
            "functionMetadata.id=$functionIdentifier, functionParameters=$functionParameters, " +
            "activityId=$activityId, inputResponses=$inputResponses)"
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    @RestrictTo(LIBRARY_GROUP)
    public fun copy(
        targetPackageName: String = this.targetPackageName,
        functionIdentifier: String = this.functionIdentifier,
        functionParameters: AppFunctionData = this.functionParameters,
        activityId: AppFunctionActivityId? = this.activityId,
        enableSafetyEvaluation: Boolean = this.enableSafetyEvaluation,
        inputResponses: Map<String, AppFunctionInputResponse> = this.inputResponses,
    ): ExecuteAppFunctionRequest =
        ExecuteAppFunctionRequest(
            targetPackageName = targetPackageName,
            functionIdentifier = functionIdentifier,
            functionParameters = functionParameters,
            activityId = activityId,
            attribution = attribution,
            inputResponses = inputResponses,
            enableSafetyEvaluation = enableSafetyEvaluation,
        )

    public companion object {
        internal const val EXTRA_PARAMETERS = "androidXAppfunctionsExtraParameters"
        internal const val EXTRA_INPUT_RESPONSES = "androidx.appfunctions.extra.INPUT_RESPONSES"
        private const val EXTRA_ENABLE_APP_FUNCTION_EVALUATION =
            "com.android.extensions.safetyevaluator.events.extra.ENABLE_APP_FUNCTION_EVALUATION"

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
                inputResponses = request.extras.readInputResponses(),
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
                activityId =
                    if (Build.VERSION.SDK_INT >= 37) {
                        this.activityId
                    } else {
                        null
                    },
                attribution =
                    if (Build.VERSION.SDK_INT >= 37) {
                        this.attribution
                    } else {
                        null
                    },
                inputResponses = this.extras.readInputResponses(),
            )

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun createAppFunctionDataWithParameterSpec(
            functionMetadata: AppFunctionMetadata,
            parametersAfd: AppFunctionData,
        ): AppFunctionData =
            parametersAfd.replaceSpecWith(
                functionMetadata.parameters,
                functionMetadata.packageMetadata.components,
            )

        private fun Bundle.putInputResponses(
            inputResponses: Map<String, AppFunctionInputResponse>
        ) {
            if (inputResponses.isEmpty()) return
            val responsesBundle = Bundle()
            for ((key, response) in inputResponses) {
                responsesBundle.putBundle(key, response.toBundle())
            }
            putBundle(EXTRA_INPUT_RESPONSES, responsesBundle)
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private fun Bundle.readInputResponses(): Map<String, AppFunctionInputResponse> {
            val responsesBundle =
                try {
                    getBundle(EXTRA_INPUT_RESPONSES)
                } catch (e: RuntimeException) {
                    if (isLoggingEnabled) {
                        Log.w(APP_FUNCTIONS_TAG, "Malformed input responses extras", e)
                    }
                    return emptyMap()
                } ?: return emptyMap()
            val keys =
                try {
                    responsesBundle.keySet()
                } catch (e: RuntimeException) {
                    if (isLoggingEnabled) {
                        Log.w(APP_FUNCTIONS_TAG, "Malformed input responses bundle", e)
                    }
                    return emptyMap()
                }
            return buildMap {
                for (key in keys) {
                    if (key == null) {
                        if (isLoggingEnabled) {
                            Log.w(APP_FUNCTIONS_TAG, "Skipping input response with null key")
                        }
                        continue
                    }
                    val response =
                        try {
                            val responseBundle = responsesBundle.getBundle(key)
                            if (responseBundle != null) {
                                AppFunctionInputResponse.fromBundle(responseBundle)
                            } else {
                                null
                            }
                        } catch (e: RuntimeException) {
                            if (isLoggingEnabled) {
                                Log.w(APP_FUNCTIONS_TAG, "Malformed input response $key", e)
                            }
                            continue
                        }
                    if (response != null) {
                        put(key, response)
                    } else if (isLoggingEnabled) {
                        Log.w(APP_FUNCTIONS_TAG, "Skipping unsupported input response $key")
                    }
                }
            }
        }
    }

    /**
     * Builder for creating an [ExecuteAppFunctionRequest].
     *
     * @param targetPackageName The package name of the app that hosts the function.
     * @param functionIdentifier The identifier of the function to call.
     * @param functionParameters The parameters to call the function with.
     */
    public class Builder(
        private val targetPackageName: String,
        private val functionIdentifier: String,
        private val functionParameters: AppFunctionData,
    ) {
        private var activityId: AppFunctionActivityId? = null
        private var attribution: AppInteractionAttribution? = null
        private var inputResponses: Map<String, AppFunctionInputResponse> = emptyMap()

        /**
         * Sets the [AppFunctionActivityId] for this request.
         *
         * This identifier is used to disambiguate between instances of the same app function
         * running in different activities when the function's [AppFunctionMetadata.scope] is
         * [AppFunctionMetadata.SCOPE_ACTIVITY].
         *
         * If `null`, the request targets an app function that is not
         * [AppFunctionMetadata.SCOPE_ACTIVITY]. Defaults to `null`.
         */
        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        public fun setActivityId(activityId: AppFunctionActivityId?): Builder = apply {
            this.activityId = activityId
        }

        /**
         * Sets the attribution that can be used by the privacy setting to provide transparency to
         * the user about why an app function was invoked.
         *
         * Defaults to `null`.
         */
        @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
        public fun setAttribution(attribution: AppInteractionAttribution?): Builder = apply {
            this.attribution = attribution
        }

        /**
         * Sets responses to the [AppFunctionInputRequiredException.inputRequests] from the previous
         * call, keyed by the key of the request that each response answers.
         *
         * The map contains one entry for each request that the caller answers. If the caller
         * answers only some of the requests, the map has no entry for each unanswered request.
         *
         * Defaults to an empty map.
         */
        public fun setInputResponses(
            inputResponses: Map<String, AppFunctionInputResponse>
        ): Builder = apply { this.inputResponses = inputResponses.toMap() }

        /** Builds the [ExecuteAppFunctionRequest]. */
        public fun build(): ExecuteAppFunctionRequest =
            ExecuteAppFunctionRequest(
                targetPackageName = targetPackageName,
                functionIdentifier = functionIdentifier,
                functionParameters = functionParameters,
                activityId = activityId,
                attribution = attribution,
                inputResponses = inputResponses,
                enableSafetyEvaluation = false,
            )
    }
}
