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

import android.annotation.SuppressLint
import android.os.CancellationSignal
import androidx.annotation.RestrictTo
import androidx.appfunctions.internal.AppFunctionInventory
import androidx.appfunctions.internal.Dependencies
import androidx.appfunctions.internal.Dispatchers
import com.android.extensions.appfunctions.AppFunctionService
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** The implementation of [AppFunctionService] from extension library. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("NewApi")
public class ExtensionAppFunctionService : ExtensionsAppFunctionService() {

    private lateinit var delegate: AppFunctionServiceDelegate

    private lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        delegate =
            AppFunctionServiceDelegate(
                this@ExtensionAppFunctionService,
                Dispatchers.Main,
                checkNotNull(Dependencies.aggregatedAppFunctionInventory),
                Dependencies.aggregatedAppFunctionInvoker,
                Dependencies.translatorSelector,
            )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        cancellationSignal: CancellationSignal,
        callback: Consumer<ExecuteAppFunctionResponse>,
    ) {
        val job =
            scope.launch {
                val response = executeFunction(request)
                // We don't check isActive here since AppFunction implementation is expected
                // to return ERROR_CANCELLED when the operation is caneled.
                callback.accept(response)
            }
        cancellationSignal.setOnCancelListener { job.cancel() }
    }

    private suspend fun executeFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse =
        try {
            delegate.executeFunction(request)
        } catch (e: AppFunctionException) {
            ExecuteAppFunctionResponse.Error(e)
        } catch (e: Exception) {
            ExecuteAppFunctionResponse.Error(AppFunctionAppUnknownException(e.message))
        }

    override fun resolveInventory(): AppFunctionInventory? {
        return Dependencies.aggregatedAppFunctionInventory
    }
}
