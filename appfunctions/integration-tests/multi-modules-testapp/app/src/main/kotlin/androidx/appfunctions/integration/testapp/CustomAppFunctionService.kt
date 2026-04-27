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

package androidx.appfunctions.integration.testapp

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionFunctionNotFoundException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.ExecuteAppFunctionRequest
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionIntTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata

@RequiresApi(Build.VERSION_CODES.BAKLAVA)
class CustomAppFunctionService : AppFunctionService() {
    override suspend fun executeFunction(
        request: ExecuteAppFunctionRequest
    ): ExecuteAppFunctionResponse {
        return when (request.functionIdentifier) {
            "androidx.appfunctions.integration.testapp.CustomAppFunctionService#add" -> {
                val a = request.functionParameters.getInt("a")
                val b = request.functionParameters.getInt("b")
                ExecuteAppFunctionResponse.Success(
                    AppFunctionData.Builder(
                            AppFunctionResponseMetadata(
                                AppFunctionIntTypeMetadata(isNullable = false)
                            ),
                            AppFunctionComponentsMetadata(),
                        )
                        .setInt(ExecuteAppFunctionResponse.Success.PROPERTY_RETURN_VALUE, a + b)
                        .build()
                )
            }
            else -> {
                throw AppFunctionFunctionNotFoundException(
                    "Unable to find ${request.functionIdentifier}"
                )
            }
        }
    }
}
