/*
 * Copyright 2026 The Android Open Source Project
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

import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.appfunctions.core.AppFunctionMetadataTestHelper
import androidx.appfunctions.internal.AppFunctionExecutionDispatcher
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.internal.Dispatchers
import androidx.appfunctions.internal.TestAppFunctionInventory
import androidx.appfunctions.metadata.AppFunctionMetadata
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@RequiresApi(36)
class TestPlatformAppFunctionService : AppFunctionService() {
    private lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        metadata: AppFunctionMetadata,
        cancellationSignal: CancellationSignal,
        callback: Consumer<ExecuteAppFunctionResponse>,
    ) =
        AppFunctionExecutionDispatcher.dispatchExecuteAppFunction(
            scope,
            request,
            metadata,
            cancellationSignal,
            callback,
        ) { _ ->
            when (request.functionIdentifier) {
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_SUCCEED -> "result"
                AppFunctionMetadataTestHelper.FunctionIds.NO_SCHEMA_EXECUTION_FAIL ->
                    throw IllegalStateException("Should not enter this logic")
                else -> throw AppFunctionFunctionNotFoundException("Unknown function id")
            }
        }

    override fun resolveInventory(): AppFunctionInventory = TestAppFunctionInventory()
}
