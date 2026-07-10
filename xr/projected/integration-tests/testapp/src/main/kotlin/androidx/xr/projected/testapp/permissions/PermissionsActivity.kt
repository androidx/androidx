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
package androidx.xr.projected.testapp.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.xr.projected.ProjectedActivityCompat
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A sample activity that uses the [ProjectedActivityCompat] API to request runtime permissions.
 * This activity needs to be run on a Projected device.
 */
@OptIn(ExperimentalProjectedApi::class)
class PermissionsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissions =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
            )

        val deniedPermissions =
            permissions.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }

        if (deniedPermissions.isEmpty()) {
            Log.i(TAG, "All permissions already granted")
            return
        }

        if (deniedPermissions.any { shouldShowRequestPermissionRationale(it) }) {
            Log.i(TAG, "Should show rationale for $deniedPermissions")
        }

        CoroutineScope(Dispatchers.Default).launch {
            ProjectedActivityCompat.requestPermissions(
                this@PermissionsActivity,
                deniedPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE,
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (i in permissions.indices) {
                val isGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED
                if (!isGranted) {
                    allGranted = false
                }
                Log.i(TAG, "onRequestPermissionsResult: ${permissions[i]}: $isGranted")
            }
            Log.i(TAG, "onRequestPermissionsResult: all results received")

            val message =
                if (allGranted) {
                    "All permissions granted"
                } else {
                    "Some permissions denied"
                }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val TAG = "PermissionsActivity"
        const val PERMISSION_REQUEST_CODE = 1234
    }
}
