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

package androidx.xr.projected.testapp.projectedcontext

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Controller hosting ProjectedContext creation and lifecycle for [ProjectedContextHostActivity].
 */
@OptIn(ExperimentalProjectedApi::class)
class ProjectedContextController(
    private val context: Context,
    private val viewModel: ProjectedContextViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    init {
        launchProjectedActivity()
        startConnectionMonitoring()
    }

    private fun startConnectionMonitoring() {
        scope.launch {
            try {
                ProjectedContext.isProjectedDeviceConnected(context, Dispatchers.Default)
                    .collectLatest { connected ->
                        viewModel.setConnected(connected)
                        if (connected) {
                            viewModel.setStatusMessage("Projected device connected.")
                            initializeDeviceContext()
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

    private fun initializeDeviceContext() {
        try {
            val projectedContext = ProjectedContext.createProjectedDeviceContext(context)
            viewModel.setDeviceName(projectedContext.display?.name ?: "Connected Device")
            viewModel.setPackageName(projectedContext.packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating projected device context", e)
        }
    }

    fun launchProjectedActivity() {
        try {
            val newCount = viewModel.relaunchCount.value + 1
            viewModel.setRelaunchCount(newCount)
            val intent =
                Intent(context, ProjectedActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(ProjectedActivity.EXTRA_LAUNCH_COUNT, newCount)
                    putExtra(ProjectedActivity.EXTRA_LAUNCH_TYPE, "RELAUNCH")
                }
            val options = ProjectedContext.createProjectedActivityOptions(context).toBundle()
            context.startActivity(intent, options)
            viewModel.setStatusMessage("Launched ProjectedActivity (Relaunch #$newCount)")
        } catch (e: Exception) {
            Log.e(TAG, "Error launching ProjectedActivity", e)
            viewModel.setStatusMessage("Launch failed: ${e.message}")
        }
    }

    fun restartProjectedActivityFreshTask() {
        try {
            val newCount = viewModel.restartCount.value + 1
            viewModel.setRestartCount(newCount)
            val intent =
                Intent(context, ProjectedActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                    putExtra(ProjectedActivity.EXTRA_LAUNCH_COUNT, newCount)
                    putExtra(ProjectedActivity.EXTRA_LAUNCH_TYPE, "FRESH_TASK")
                }
            val options = ProjectedContext.createProjectedActivityOptions(context).toBundle()
            context.startActivity(intent, options)
            viewModel.setStatusMessage("Restarted ProjectedActivity in fresh task (#$newCount)")
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting ProjectedActivity", e)
            viewModel.setStatusMessage("Restart failed: ${e.message}")
        }
    }

    override fun close() {}

    private companion object {
        const val TAG = "ProjectedContextController"
    }
}
