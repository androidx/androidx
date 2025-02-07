/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility class for handling Android permissions. SceneCore applications should use this before
 * creating Anchors.
 */
public object PermissionHelper {
    public const val SCENE_UNDERSTANDING_PERMISSION_CODE: Int = 0
    public const val SCENE_UNDERSTANDING_PERMISSION: String =
        "android.permission.SCENE_UNDERSTANDING"

    public fun hasPermission(activity: Activity, permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    public fun requestPermission(
        activity: Activity,
        permission: String,
        permissionCode: Int
    ): Unit = ActivityCompat.requestPermissions(activity, arrayOf(permission), permissionCode)

    public fun shouldShowRequestPermissionRationale(
        activity: Activity,
        permission: String
    ): Boolean = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    public fun launchPermissionSettings(activity: Activity) {
        val intent = Intent()
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.fromParts("package", activity.packageName, null))
        activity.startActivity(intent)
    }
}
