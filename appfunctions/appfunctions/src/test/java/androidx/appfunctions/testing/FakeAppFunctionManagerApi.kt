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

package androidx.appfunctions.testing

import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.internal.AppFunctionManagerApi

class FakeAppFunctionManagerApi : AppFunctionManagerApi {
    var executeAppFunctionResponse: ExecuteAppFunctionResponse? = null

    override suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse =
        executeAppFunctionResponse
            ?: throw IllegalStateException("Make sure you set the fake response first.")

    override suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean =
        throw UnsupportedOperationException()

    override suspend fun setAppFunctionEnabled(functionId: String, newEnabledState: Int) =
        throw UnsupportedOperationException()
}
