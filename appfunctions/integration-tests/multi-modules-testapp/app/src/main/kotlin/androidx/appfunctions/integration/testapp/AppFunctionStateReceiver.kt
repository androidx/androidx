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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appfunctions.AppFunctionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFunctionStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_SET_STATE) {
            val functionId = intent.getStringExtra(EXTRA_FUNCTION_ID) ?: return
            val state =
                intent.getIntExtra(EXTRA_STATE, AppFunctionManager.APP_FUNCTION_STATE_DEFAULT)
            val manager = AppFunctionManager.getInstance(context) ?: return
            CoroutineScope(Dispatchers.Main).launch {
                manager.setAppFunctionEnabled(functionId, state)
            }
        }
    }

    companion object {
        const val ACTION_SET_STATE = "androidx.appfunctions.integration.testapp.ACTION_SET_STATE"
        const val EXTRA_FUNCTION_ID = "function_id"
        const val EXTRA_STATE = "state"
    }
}
