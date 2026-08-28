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

package androidx.xr.projected.testapp.battery

import android.content.Context
import android.util.Log
import androidx.xr.projected.BatteryState
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Controller hosting business logic, listeners, and device controller for battery testing. */
@OptIn(ExperimentalProjectedApi::class)
class BatteryController(
    private val context: Context,
    private val viewModel: BatteryViewModel,
    private val scope: CoroutineScope,
) : AutoCloseable {

    private var deviceController: ProjectedDeviceController? = null
    private var batteryListener: ((BatteryState) -> Unit)? = null

    init {
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
                            initializeDeviceController()
                        } else {
                            viewModel.setStatusMessage("Projected device is not connected.")
                            cleanupDeviceController()
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring connection", e)
                viewModel.setStatusMessage("Error monitoring connection: ${e.message}")
            }
        }
    }

    private fun initializeDeviceController() {
        scope.launch {
            try {
                cleanupDeviceController()
                val controller = ProjectedDeviceController.create(context)
                deviceController = controller

                val listener: (BatteryState) -> Unit = { state ->
                    viewModel.setBatteryState(state.batteryLevel, state.isCharging)
                }
                batteryListener = listener
                controller.addBatteryStateChangedListener(scope.coroutineContext, listener)
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing ProjectedDeviceController", e)
                viewModel.setStatusMessage("Failed to start device controller: ${e.message}")
            }
        }
    }

    private fun cleanupDeviceController() {
        batteryListener?.let { deviceController?.removeBatteryStateChangedListener(it) }
        batteryListener = null
        try {
            deviceController?.close()
        } catch (_: Exception) {}
        deviceController = null
    }

    override fun close() {
        cleanupDeviceController()
    }

    private companion object {
        const val TAG = "BatteryController"
    }
}
