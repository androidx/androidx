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

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.appfunctions.metadata.AppFunctionMetadata

/** Represents a response of an execution of an app function. */
public sealed interface ExecuteAppFunctionResponse {

    /** Represents a successful execution of an app function. */
    public class Success(
        /**
         * The return value of the executed function. An [AppFunctionData.EMPTY] indicates that the
         * function does not produce a return value.
         */
        public val returnValue: AppFunctionData
    ) : ExecuteAppFunctionResponse {
        internal fun toPlatformExtensionClass():
            com.android.extensions.appfunctions.ExecuteAppFunctionResponse {
            return com.android.extensions.appfunctions.ExecuteAppFunctionResponse(
                returnValue.genericDocument,
                returnValue.extras,
            )
        }

        /**
         * Converts [ExecuteAppFunctionResponse] to
         * [android.app.appfunctions.ExecuteAppFunctionResponse].
         *
         * @return The converted [android.app.appfunctions.ExecuteAppFunctionResponse].
         */
        @RequiresApi(Build.VERSION_CODES.BAKLAVA)
        public fun toPlatformExecuteAppFunctionResponse():
            android.app.appfunctions.ExecuteAppFunctionResponse {
            return android.app.appfunctions.ExecuteAppFunctionResponse(
                returnValue.genericDocument,
                returnValue.extras,
            )
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        internal fun grantUriAccess(context: Context, callingPackageName: String) {
            returnValue.visitAppFunctionUriGrants { uriGrant ->
                context.grantUriPermission(
                    callingPackageName,
                    uriGrant.uri,
                    @Suppress("WrongConstant") // modeFlags is a subset of Intent flags
                    uriGrant.modeFlags,
                )
            }
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public fun copy(returnValue: AppFunctionData = this.returnValue): Success =
            Success(returnValue)

        public companion object {
            /**
             * The key name of the property that stores the function return value within `result`.
             *
             * See [AppFunctionData] documentation on retrieving expected fields.
             */
            public const val PROPERTY_RETURN_VALUE: String =
                android.app.appfunctions.ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE

            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
            internal fun fromPlatformExtensionClass(
                response: com.android.extensions.appfunctions.ExecuteAppFunctionResponse,
                functionMetadata: AppFunctionMetadata,
            ): Success {
                return Success(
                    AppFunctionData(response.resultDocument, response.extras)
                        .replaceSpecWith(functionMetadata.response, functionMetadata.components)
                )
            }

            /**
             * Creates [ExecuteAppFunctionResponse] from
             * [android.app.appfunctions.ExecuteAppFunctionResponse].
             *
             * The resulting response object is validated against the provided
             * [AppFunctionMetadata].
             *
             * @param functionMetadata the [AppFunctionMetadata] of the function that was executed.
             * @return The created [ExecuteAppFunctionResponse].
             */
            @RequiresApi(Build.VERSION_CODES.BAKLAVA)
            @JvmStatic
            public fun android.app.appfunctions.ExecuteAppFunctionResponse
                .toCompatExecuteAppFunctionResponse(
                functionMetadata: AppFunctionMetadata
            ): Success {
                return Success(
                    AppFunctionData(this.resultDocument, this.extras)
                        .replaceSpecWith(functionMetadata.response, functionMetadata.components)
                )
            }
        }
    }

    /** Represents a failed execution of an app function. */
    public class Error(
        /** The [AppFunctionException] when the function execution failed. */
        public val error: AppFunctionException
    ) : ExecuteAppFunctionResponse {
        override fun toString(): String {
            return "AppFunctionResponse.Error(error=$error)"
        }
    }
}
