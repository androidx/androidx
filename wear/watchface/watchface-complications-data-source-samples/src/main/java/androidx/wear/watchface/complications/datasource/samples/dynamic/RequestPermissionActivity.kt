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

package androidx.wear.watchface.complications.datasource.samples.dynamic

import android.Manifest.permission
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Bundle

class RequestPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission =
            requireNotNull(ACTION_TO_PERMISSION[intent.action]) {
                "Unknown action: ${intent.action}"
            }
        if (checkSelfPermission(permission) == PERMISSION_GRANTED) {
            setResult(RESULT_OK)
            finish()
            return
        }
        requestPermissions(arrayOf(permission), 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (grantResults contentEquals intArrayOf(PERMISSION_GRANTED)) {
            setResult(RESULT_OK)
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    companion object {
        private val ACTION_TO_PERMISSION: Map<String, String> =
            mapOf(
                "androidx.wear.watchface.complications.datasource.samples.dynamic.REQUEST_ACTIVITY_RECOGNITION_PERMISSION" to
                    permission.ACTIVITY_RECOGNITION,
                "androidx.wear.watchface.complications.datasource.samples.dynamic.REQUEST_BODY_SENSORS_PERMISSION" to
                    permission.BODY_SENSORS,
            )
    }
}
