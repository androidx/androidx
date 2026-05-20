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

package androidx.xr.arcore.projected.testapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Activity to test 3DoF Inertial Tracking Mode on projected devices. */
@OptIn(PreviewSpatialApi::class, androidx.xr.projected.experimental.ExperimentalProjectedApi::class)
class InertialTrackingActivity : ComponentActivity() {
    private lateinit var session: Session

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            when (val result = Session.create(context = this@InertialTrackingActivity)) {
                is SessionCreateSuccess -> {
                    val createdSession = result.session
                    try {
                        createdSession.configure(
                            Config.Builder().setDeviceTracking(DeviceTrackingMode.INERTIAL).build()
                        )
                        session = createdSession

                        lifecycleScope.launch(Dispatchers.Main) {
                            setContent { InertialPanel(session) }
                        }
                    } catch (e: Exception) {
                        Log.e("InertialTrackingActivity", "Session configuration error", e)
                    }
                }
                else -> {
                    Log.e("InertialTrackingActivity", "Session creation failed")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    @Composable
    private fun InertialPanel(session: Session) {
        var isTrackingInertial by remember { mutableStateOf(true) }
        val arDevice = ArDevice.getInstance(session)
        val arDeviceState by arDevice.state.collectAsStateWithLifecycle()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Button(onClick = { finish() }) { Text("Back") }
                    Button(
                        onClick = {
                            isTrackingInertial = !isTrackingInertial
                            val newConfig =
                                Config.Builder(session.config)
                                    .setDeviceTracking(
                                        if (isTrackingInertial) DeviceTrackingMode.INERTIAL
                                        else DeviceTrackingMode.SPATIAL
                                    )
                                    .build()
                            session.configure(newConfig)
                        }
                    ) {
                        Text(if (isTrackingInertial) "Switch to SPATIAL" else "Switch to INERTIAL")
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.background(Color.White)
                        .fillMaxSize()
                        .clickable {
                            isTrackingInertial = !isTrackingInertial
                            lifecycleScope.launch(Dispatchers.IO) {
                                val newConfig =
                                    Config.Builder(session.config)
                                        .setDeviceTracking(
                                            if (isTrackingInertial) DeviceTrackingMode.INERTIAL
                                            else DeviceTrackingMode.SPATIAL
                                        )
                                        .build()
                                session.configure(newConfig)
                            }
                        }
                        .padding(innerPadding)
                        .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val trackingState = arDeviceState.trackingState
                val pose = arDeviceState.devicePose
                Text(
                    text =
                        if (isTrackingInertial) "Source: System Sensor API"
                        else "Source: ARCore (Native)",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color.Black,
                )
                Text(
                    text = "Tracking State: ${trackingState.toFriendlyString()}",
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color.Black,
                )

                Text(
                    text =
                        "Pos: X:${"%.4f".format(Locale.US, pose.translation.x)} Y:${"%.4f".format(Locale.US, pose.translation.y)}\n     Z:${"%.4f".format(Locale.US, pose.translation.z)}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color.Black,
                )
                Text(
                    text =
                        "Rot: X:${"%.4f".format(Locale.US, pose.rotation.x)} Y:${"%.4f".format(Locale.US, pose.rotation.y)}\n     Z:${"%.4f".format(Locale.US, pose.rotation.z)} W:${"%.4f".format(Locale.US, pose.rotation.w)}",
                    fontSize = 16.sp,
                    color = Color.Black,
                )
            }
        }
    }
}

private fun TrackingState.toFriendlyString(): String =
    when (this) {
        TrackingState.TRACKING -> "TRACKING"
        TrackingState.PAUSED -> "PAUSED"
        TrackingState.STOPPED -> "STOPPED"
        TrackingState.TRACKING_DEGRADED -> "TRACKING_DEGRADED"
        else -> "UNKNOWN"
    }
