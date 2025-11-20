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

package androidx.appfunctions.service

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionException
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExtensionsAppFunctionService
import androidx.appfunctions.internal.Dependencies
import androidx.appfunctions.internal.Dispatchers
import androidx.appfunctions.service.internal.ServiceDependencies
import com.android.extensions.appfunctions.AppFunctionService

/** The implementation of [AppFunctionService] from extension library. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@SuppressLint("NewApi")
public class ExtensionAppFunctionService : ExtensionsAppFunctionService() {

    private lateinit var delegate: AppFunctionServiceDelegate

    override fun onCreate() {
        super.onCreate()
        delegate =
            AppFunctionServiceDelegate(
                this@ExtensionAppFunctionService,
                Dispatchers.Main,
                checkNotNull(Dependencies.aggregatedAppFunctionInventory),
                ServiceDependencies.aggregatedAppFunctionInvoker,
                Dependencies.translatorSelector,
            )
    }

    override suspend fun executeFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse =
        try {
            delegate.executeFunction(request)
        } catch (e: AppFunctionException) {
            ExecuteAppFunctionResponse.Error(e)
        } catch (e: Exception) {
            ExecuteAppFunctionResponse.Error(AppFunctionAppUnknownException(e.message))
        }
}
