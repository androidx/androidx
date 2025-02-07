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

import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo

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
public class ExecuteAppFunctionRequest(
    public val targetPackageName: String,
    public val functionIdentifier: String,
    public val functionParameters: AppFunctionData,
) {

    @RequiresApi(36)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toPlatformClass(): com.android.extensions.appfunctions.ExecuteAppFunctionRequest {
        return com.android.extensions.appfunctions.ExecuteAppFunctionRequest.Builder(
                targetPackageName,
                functionIdentifier,
            )
            .setParameters(functionParameters.genericDocument)
            .setExtras(functionParameters.extras)
            .build()
    }

    override fun toString(): String {
        return "ExecuteAppFunctionRequest(targetPackageName=$targetPackageName, " +
            "functionIdentifier=$functionIdentifier, functionParameters=$functionParameters)"
    }

    public companion object {
        @RequiresApi(36)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromPlatformClass(
            request: com.android.extensions.appfunctions.ExecuteAppFunctionRequest
        ): ExecuteAppFunctionRequest {
            return ExecuteAppFunctionRequest(
                targetPackageName = request.targetPackageName,
                functionIdentifier = request.functionIdentifier,
                functionParameters =
                    AppFunctionData(
                        request.parameters,
                        request.extras,
                    ),
            )
        }
    }
}
