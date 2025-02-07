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
 * Represents a response of an execution of an app function.
 *
 * @property result The return value of the executed function. An empty result indicates that the
 *   function does not produce a return value.
 */
public class ExecuteAppFunctionResponse(public val result: AppFunctionData) {

    @RequiresApi(36)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public fun toPlatformClass(): com.android.extensions.appfunctions.ExecuteAppFunctionResponse {
        return com.android.extensions.appfunctions.ExecuteAppFunctionResponse(
            result.genericDocument,
            result.extras,
        )
    }

    public companion object {
        /**
         * The key name of the property that stores the function return value within `result`.
         *
         * See [AppFunctionData] documentation on retrieving expected fields.
         */
        public const val PROPERTY_RETURN_VALUE: String = "androidAppfunctionsReturnValue"

        @RequiresApi(36)
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun fromPlatformClass(
            response: com.android.extensions.appfunctions.ExecuteAppFunctionResponse
        ): ExecuteAppFunctionResponse {
            return ExecuteAppFunctionResponse(
                AppFunctionData(response.resultDocument, response.extras)
            )
        }
    }
}
