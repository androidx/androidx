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

package androidx.xr.scenecore.testapp.common

import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice

const val HAND_TRACKING_PERMISSION = "android.permission.HAND_TRACKING"
const val SCENE_UNDERSTANDING_PERMISSION = "android.permission.SCENE_UNDERSTANDING_COARSE"
const val HEAD_TRACKING_PERMISSION = "android.permission.HEAD_TRACKING"
const val READ_MEDIA_AUDIO_PERMISSION = "android.permission.READ_MEDIA_AUDIO"

fun createSession(activity: AppCompatActivity): Session? {
    var session: Session? = null
    try {
        when (val sessionCreateResult = Session.create(activity)) {
            is SessionCreateSuccess -> {
                session = sessionCreateResult.session
                obtainUserPermissions(activity)
            }
            is SessionCreateApkRequired -> {
                Toast.makeText(activity, "Please update to the latest APK.", Toast.LENGTH_LONG)
                    .show()
                activity.finish()
            }

            is SessionCreateUnsupportedDevice -> {
                Toast.makeText(activity, "Unsupported device.", Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    } catch (e: SecurityException) {
        obtainUserPermissions(activity)
    }

    return session
}

private fun obtainUserPermissions(activity: AppCompatActivity) {
    val permissionsLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissions ->
            if (permissions.values.contains(false)) {
                Toast.makeText(activity, "Missing required permissions", Toast.LENGTH_LONG).show()
                activity.finish()
            }
        }
    permissionsLauncher.launch(
        arrayOf(
            SCENE_UNDERSTANDING_PERMISSION,
            HAND_TRACKING_PERMISSION,
            HEAD_TRACKING_PERMISSION,
            READ_MEDIA_AUDIO_PERMISSION,
        )
    )
}
