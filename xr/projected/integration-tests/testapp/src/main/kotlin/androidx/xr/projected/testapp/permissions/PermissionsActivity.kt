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
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract

/**
 * A sample activity that uses the [ProjectedPermissionsResultContract] API from a non-Jetpack
 * Android Activity. This activity needs to be run on a Projected device.
 */
@OptIn(ExperimentalProjectedApi::class)
class PermissionsActivity : Activity() {

    private val projectedPermissionsResultContract = ProjectedPermissionsResultContract()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    private fun requestPermissions() {
        val data =
            ProjectedPermissionsRequestParams(
                listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                ),
                "Some good rationale.",
            )
        intent = projectedPermissionsResultContract.createIntent(this, listOf(data))
        startActivityForResult(intent, PERMISSION_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val results = projectedPermissionsResultContract.parseResult(resultCode, data)
            for ((permission, isGranted) in results) {
                Log.i(TAG, "onActivityResult: $permission: $isGranted")
            }
            Log.i(TAG, "onActivityResult: all results received")
        }
    }

    private companion object {
        const val TAG = "SampleActivity"
        const val PERMISSION_REQUEST_CODE = 1234
    }
}
