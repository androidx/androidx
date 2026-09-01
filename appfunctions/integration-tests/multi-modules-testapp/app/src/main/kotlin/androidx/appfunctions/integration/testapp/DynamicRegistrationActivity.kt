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

import android.annotation.SuppressLint
import android.app.Activity
import android.app.appfunctions.AppFunctionRegistration
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionManager
import androidx.appfunctions.CallbackAppFunction
import androidx.appfunctions.ExecuteAppFunctionResponse
import androidx.appfunctions.ExperimentalAppFunctionsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

@OptIn(ExperimentalAppFunctionsApi::class)
@RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
@SuppressLint("RestrictedApiAndroidX")
class DynamicRegistrationActivity : Activity() {

    private lateinit var appFunctionManager: AppFunctionManager
    private var registration: AppFunctionRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appFunctionManager =
            AppFunctionManager.getInstance(this)
                ?: throw IllegalStateException("AppFunctionManager is null")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_REGISTER_ACTIVITY_SCOPED) {
            val appFunction = CallbackAppFunction { _, _, callback ->
                callback.accept(ExecuteAppFunctionResponse.Success(AppFunctionData.EMPTY))
            }
            registration =
                appFunctionManager.registerAppFunction(
                    ACTIVITY_SCOPE_DYNAMIC_FUNCTION_ID,
                    Dispatchers.Default.asExecutor(),
                    appFunction,
                )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        registration?.unregister()
        registration = null
    }

    companion object {
        const val ACTIVITY_SCOPE_DYNAMIC_FUNCTION_ID =
            "androidx.appfunctions.integration.testapp.DynamicActivityScopeSignature#processVoid"
        const val ACTION_REGISTER_ACTIVITY_SCOPED =
            "androidx.appfunctions.integration.action.REGISTER_ACTIVITY_SCOPED"
    }
}
