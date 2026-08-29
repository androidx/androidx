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

package androidx.xr.projected.testapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.projected.ProjectedContext
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import androidx.xr.projected.testapp.audio.AudioActivity
import androidx.xr.projected.testapp.battery.BatteryActivity
import androidx.xr.projected.testapp.camera.CameraActivity
import androidx.xr.projected.testapp.controller.DisplayControllerHostActivity
import androidx.xr.projected.testapp.input.InputHostActivity
import androidx.xr.projected.testapp.inputorchestration.InputOrchestrationActivity
import androidx.xr.projected.testapp.permissions.PermissionsHostActivity
import androidx.xr.projected.testapp.projectedcontext.ProjectedContextHostActivity
import kotlinx.coroutines.Dispatchers

/** The MainActivity is used to launch the various projected test activities. */
@OptIn(ExperimentalProjectedApi::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isConnected by
                produceState(initialValue = false) {
                    try {
                        ProjectedContext.isProjectedDeviceConnected(
                                this@MainActivity,
                                Dispatchers.Default,
                            )
                            .collect { value = it }
                    } catch (_: Exception) {
                        value = false
                    }
                }
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ActivityList(this, isConnected)
                }
            }
        }
    }

    @Composable
    fun ActivityList(context: Context, isConnected: Boolean) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "XR Projected Test App",
                fontSize = 24.sp,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Status: ${if (isConnected) "Connected" else "Not Connected"}",
                fontSize = 16.sp,
                color =
                    if (isConnected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            Text(
                text = "Test Activities",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            for (activity in ACTIVITY_MAP) {
                CreateTestActivityRow(activity.key, activity.value, context)
            }
        }
    }

    @Composable
    private fun CreateTestActivityRow(name: String, activityClass: Class<*>, context: Context) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Button(onClick = { launchActivity(activityClass, context) }) {
                Text("Run Test", fontSize = 14.sp)
            }
        }
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }

    private fun launchActivity(activityClass: Class<*>, context: Context) {
        startActivity(Intent(context, activityClass))
    }

    companion object {
        const val TAG = "MainActivity"

        val ACTIVITY_MAP: Map<String, Class<*>> =
            mapOf(
                "Audio" to AudioActivity::class.java,
                "Battery" to BatteryActivity::class.java,
                "Camera" to CameraActivity::class.java,
                "Display Controller" to DisplayControllerHostActivity::class.java,
                "Input" to InputHostActivity::class.java,
                "Input Orchestration" to InputOrchestrationActivity::class.java,
                "Permission" to PermissionsHostActivity::class.java,
                "Projected Context" to ProjectedContextHostActivity::class.java,
            )
    }
}
