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

package androidx.camera.integration.extensions.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Pair
import androidx.camera.integration.extensions.PERMISSIONS_REQUEST_CODE
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture

private const val TAG = "PermissionUtil"

/** Permissions setup utility. */
object PermissionUtil {
    /** Setup required permissions. */
    @JvmStatic
    fun setupPermissions(
        activity: Activity
    ): Pair<ListenableFuture<Boolean>, CallbackToFutureAdapter.Completer<Boolean>> {
        var permissionCompleter: CallbackToFutureAdapter.Completer<Boolean>? = null
        val future =
            CallbackToFutureAdapter.getFuture {
                completer: CallbackToFutureAdapter.Completer<Boolean> ->
                permissionCompleter = completer
                if (!allPermissionsGranted(activity)) {
                    makePermissionRequest(activity)
                } else {
                    permissionCompleter!!.set(true)
                }
                "get_permissions"
            }
        return Pair.create(future, permissionCompleter!!)
    }

    private fun makePermissionRequest(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            getRequiredPermissions(activity),
            PERMISSIONS_REQUEST_CODE
        )
    }

    /** Returns true if all the necessary permissions have been granted already. */
    private fun allPermissionsGranted(activity: Activity): Boolean {
        for (permission in getRequiredPermissions(activity)) {
            if (
                ContextCompat.checkSelfPermission(activity, permission!!) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /** Tries to acquire all the necessary permissions through a dialog. */
    private fun getRequiredPermissions(activity: Activity): Array<String?> {
        val info: PackageInfo
        try {
            info =
                activity.packageManager.getPackageInfo(
                    activity.packageName,
                    PackageManager.GET_PERMISSIONS
                )
        } catch (exception: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Failed to obtain all required permissions.", exception)
            return arrayOfNulls(0)
        }

        if (info.requestedPermissions == null || info.requestedPermissions.isEmpty()) {
            return arrayOfNulls(0)
        }

        val requiredPermissions: MutableList<String?> = mutableListOf()

        // From Android T, skips the permission check of WRITE_EXTERNAL_STORAGE since it won't be
        // granted any more. When querying the requested permissions from PackageManager,
        // READ_EXTERNAL_STORAGE will also be included if we specify WRITE_EXTERNAL_STORAGE
        // requirement in AndroidManifest.xml. Therefore, also need to skip the permission check
        // of READ_EXTERNAL_STORAGE.
        for (permission in info.requestedPermissions) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    (Manifest.permission.WRITE_EXTERNAL_STORAGE == permission ||
                        Manifest.permission.READ_EXTERNAL_STORAGE == permission)
            ) {
                continue
            }

            requiredPermissions.add(permission)
        }

        val permissions = requiredPermissions.toTypedArray<String?>()

        return if (permissions.isNotEmpty()) {
            permissions
        } else {
            arrayOfNulls(0)
        }
    }
}
