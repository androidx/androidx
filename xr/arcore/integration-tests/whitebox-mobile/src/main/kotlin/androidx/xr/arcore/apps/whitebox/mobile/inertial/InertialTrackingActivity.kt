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

@file:OptIn(ExperimentalInertialTrackingApi::class)

package androidx.xr.arcore.apps.whitebox.mobile.inertial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.apps.whitebox.mobile.common.BackToMainActivityButton
import androidx.xr.arcore.apps.whitebox.mobile.common.SessionLifecycleHelper
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.ExperimentalInertialTrackingApi
import androidx.xr.runtime.Session
import java.util.Locale

/** Activity to test 3DoF Inertial Tracking Mode. */
class InertialTrackingActivity : ComponentActivity() {

    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super<ComponentActivity>.onCreate(savedInstanceState)

        sessionHelper =
            SessionLifecycleHelper(
                this,
                config = Config.Builder().setDeviceTracking(DeviceTrackingMode.INERTIAL).build(),
                onSessionAvailable = { session ->
                    this.session = session
                    setContent { InertialPanel(session) }
                },
            )
        sessionHelper.tryCreateSession()
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
                    BackToMainActivityButton()
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
                        "Position:\n  X: ${"%.4f".format(Locale.US, pose.translation.x)}\n  Y: ${"%.4f".format(Locale.US, pose.translation.y)}\n  Z: ${"%.4f".format(Locale.US, pose.translation.z)}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = Color.Black,
                )
                Text(
                    text =
                        "Rotation:\n  X: ${"%.4f".format(Locale.US, pose.rotation.x)}\n  Y: ${"%.4f".format(Locale.US, pose.rotation.y)}\n  Z: ${"%.4f".format(Locale.US, pose.rotation.z)}\n  W: ${"%.4f".format(Locale.US, pose.rotation.w)}",
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
