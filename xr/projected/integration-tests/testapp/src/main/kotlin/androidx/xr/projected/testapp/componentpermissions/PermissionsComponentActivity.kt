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

package androidx.xr.projected.testapp.componentpermissions

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.permissions.ProjectedPermissionsRequestParams
import androidx.xr.projected.permissions.ProjectedPermissionsResultContract

/**
 * A sample activity that uses the [ProjectedPermissionsResultContract] API from a Jetpack
 * [ComponentActivity]. This activity needs to be run on a Projected device. It is very similar to
 * using a [androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions].
 * The same pattern can be used in [androidx.appcompat.app.AppCompatActivity] (which extends
 * ComponentActivity) and [androidx.fragment.app.Fragment].
 */
@OptIn(ExperimentalProjectedApi::class)
class PermissionsComponentActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ProjectedPermissionsResultContract()) { results ->
            for ((permission, isGranted) in results) {
                Log.i(TAG, "onActivityResult: $permission: $isGranted")
            }
            Log.i(TAG, "onActivityResult: all results received")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            listOf(
                ProjectedPermissionsRequestParams(
                    listOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                    ),
                    "Some good rationale.",
                )
            )
        )
    }

    private companion object {
        const val TAG = "SampleComponentActivity"
    }
}
