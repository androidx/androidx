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

package androidx.xr.projected.testapp.controller

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testapp.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Controller hosting business logic and telemetry routing for [DisplayControllerHostActivity]. */
@OptIn(ExperimentalProjectedApi::class)
class DisplayTestController(
    private val context: Context,
    private val viewModel: DisplayControllerViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    private var deviceController: ProjectedDeviceController? = null
    private var mediaPlayer: MediaPlayer? = null

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
                            initializeDeviceTelemetry()
                            launchProjectedActivity()
                        } else {
                            viewModel.setStatusMessage("Projected device is not connected.")
                            cleanupDeviceTelemetry()
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring connection", e)
                viewModel.setStatusMessage("Connection error: ${e.message}")
            }
        }
    }

    private fun initializeDeviceTelemetry() {
        scope.launch {
            try {
                cleanupDeviceTelemetry()
                val controller = ProjectedDeviceController.create(context)
                deviceController = controller
                val caps = controller.capabilities.map { it.toString() }.toSet()
                viewModel.setCapabilities(caps)
                if (
                    ProjectedDeviceController.Capability.CAPABILITY_VISUAL_UI in
                        controller.capabilities
                ) {
                    viewModel.setPresentationModes("VISUALS_ON")
                } else {
                    viewModel.setPresentationModes("None")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error querying device capabilities", e)
            }
        }
    }

    private fun cleanupDeviceTelemetry() {
        try {
            deviceController?.close()
        } catch (_: Exception) {}
        deviceController = null
    }

    fun launchProjectedActivity() {
        try {
            val intent =
                Intent(context, DisplayControllerProjectedActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            val options = ProjectedContext.createProjectedActivityOptions(context).toBundle()
            context.startActivity(intent, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching projected activity", e)
            viewModel.setStatusMessage("Launch failed: ${e.message}")
        }
    }

    fun toggleKeepScreenOn() {
        val newState = !viewModel.keepScreenOn.value
        viewModel.setKeepScreenOn(newState)
    }

    fun playSound() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.display_detected)
            mediaPlayer?.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound", e)
        }
    }

    override fun close() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null

        cleanupDeviceTelemetry()
    }

    private companion object {
        const val TAG = "DisplayTestController"
    }
}
