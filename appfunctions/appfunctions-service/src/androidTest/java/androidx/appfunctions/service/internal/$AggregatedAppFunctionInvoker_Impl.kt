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

package androidx.appfunctions.service.internal

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.core.AppFunctionMetadataTestHelper

/** Test implementation for [androidx.appfunctions.AppFunctionManagerCompatTest] */
@RequiresApi(Build.VERSION_CODES.S)
class `$AggregatedAppFunctionInvoker_Impl` : AggregatedAppFunctionInvoker() {
    override val invokers: List<AppFunctionInvoker>
        get() = listOf(InternalAppFunctionInvoker())

    private class InternalAppFunctionInvoker : AppFunctionInvoker {
        override val supportedFunctionIds: Set<String>
            get() =
                setOf(
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED,
                    AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL,
                )

        override suspend fun unsafeInvoke(
            appFunctionContext: AppFunctionContext,
            functionIdentifier: String,
            parameters: Map<String, Any?>,
        ): Any? {
            return when (functionIdentifier) {
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED -> {
                    "result"
                }
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL -> {
                    throw IllegalStateException("Should not enter this logic")
                }
                else -> throw IllegalArgumentException("Unknown function id")
            }
        }
    }
}
