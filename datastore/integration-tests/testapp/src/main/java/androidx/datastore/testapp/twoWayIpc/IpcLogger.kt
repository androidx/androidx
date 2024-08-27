/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.datastore.testapp.twoWayIpc

import android.app.Application
import android.os.Build
import android.util.Log

/**
 * Used for logging in multi process tests. Multi process tests are really hard to debug, hence it
 * is useful to have logs around when needed.
 */
object IpcLogger {
    fun log(message: Any) {
        if (ENABLED) {
            Log.d("DATASTORE-MULTIPROCESS-${getProcessName()}", message.toString())
        }
    }

    private fun getProcessName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            "notAvailable"
        }
    }

    @Suppress("MayBeConstant") val ENABLED = false
}
