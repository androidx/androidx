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

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.xr.projected.BatteryState
import androidx.xr.projected.ProjectedDeviceController
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Shows information about the battery state. */
@OptIn(ExperimentalProjectedApi::class)
class BatteryActivity : ComponentActivity() {

    private var projectedDeviceController: ProjectedDeviceController? = null
    private var batteryState by mutableStateOf<BatteryState?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            try {
                val controller = ProjectedDeviceController.create(this@BatteryActivity)
                projectedDeviceController = controller
                if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                    controller.addBatteryStateChangedListener(
                        Dispatchers.Default,
                        batteryStateListener,
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create ProjectedDeviceController", e)
            }
        }

        setContent { BatteryScreen(batteryState) }
    }

    override fun onResume() {
        super.onResume()
        projectedDeviceController?.addBatteryStateChangedListener(
            Dispatchers.Main,
            batteryStateListener,
        )
    }

    override fun onPause() {
        super.onPause()
        projectedDeviceController?.removeBatteryStateChangedListener(batteryStateListener)
    }

    private val batteryStateListener: (BatteryState) -> Unit = { state -> batteryState = state }

    @Composable
    private fun BatteryScreen(state: BatteryState?) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state == null) {
                Text("Waiting for battery state...", fontSize = 20.sp)
            } else {
                Text("Battery Level: ${state.batteryLevel}%", fontSize = 30.sp)
                Text("Charging: ${if (state.isCharging) "Yes" else "No"}", fontSize = 30.sp)
            }
        }
    }

    companion object {
        private const val TAG = "BatteryActivity"
    }
}
