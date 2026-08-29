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

package androidx.xr.projected.testapp.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Controller hosting connection monitoring and permission status checks for Permissions test. */
@OptIn(ExperimentalProjectedApi::class)
class PermissionsController(
    private val context: Context,
    private val viewModel: PermissionsViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    init {
        refreshPermissionsStatus()
        launchProjectedActivity()
        startConnectionMonitoring()
    }

    fun refreshPermissionsStatus() {
        val permissions =
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        val statusMap =
            permissions.associate { perm ->
                val simpleName = perm.substringAfterLast('.')
                val isGranted =
                    ContextCompat.checkSelfPermission(context, perm) ==
                        PackageManager.PERMISSION_GRANTED
                simpleName to isGranted
            }
        viewModel.updatePermissions(statusMap)
    }

    private fun startConnectionMonitoring() {
        scope.launch {
            try {
                ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default)
                    .collectLatest { connected ->
                        viewModel.setConnected(connected)
                        if (connected) {
                            viewModel.setStatusMessage("Projected device connected.")
                            launchProjectedActivity()
                        } else {
                            viewModel.setStatusMessage("Projected device is not connected.")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring connection", e)
                viewModel.setStatusMessage("Connection error: ${e.message}")
            }
        }
    }

    fun launchProjectedActivity() {
        try {
            val intent =
                Intent(context, PermissionsProjectedActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val options = ProjectedContext.createProjectedActivityOptions(context).toBundle()
            context.startActivity(intent, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching PermissionsProjectedActivity", e)
            viewModel.setStatusMessage("Launch failed: ${e.message}")
        }
    }

    override fun close() {}

    private companion object {
        const val TAG = "PermissionsController"
    }
}
