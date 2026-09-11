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

package androidx.xr.arcore.projected.testapp.tracking

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.arcore.projected.testapp.getOrientationDescription
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Text
import java.util.Locale

class TrackingProjectedActivity : ComponentActivity() {

    internal var viewModel: TrackingViewModel? = null
        set(value) {
            field = value
            value?.let { vm ->
                setContent { ProjectedView(vm) }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        viewModel?.let { vm ->
            setContent { ProjectedView(vm) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel?.let { vm ->
            setContent { ProjectedView(vm) }
        }
    }

    @Composable
    private fun ProjectedView(viewModel: TrackingViewModel) {
        val currentMode by viewModel.currentMode.collectAsState()
        val deviceTrackingState by viewModel.deviceTrackingState.collectAsState()
        val devicePose by viewModel.devicePose.collectAsState()
        val geoState by viewModel.geospatialTrackingState.collectAsState()
        val geoPose by viewModel.geospatialPose.collectAsState()
        val message by viewModel.message.collectAsState()

        GlimmerTheme {
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .background(GlimmerTheme.colors.background)
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = "[${currentMode.displayName}]",
                    style = GlimmerTheme.typography.titleMedium,
                    color = GlimmerTheme.colors.primary,
                )

                when (currentMode) {
                    TestMode.DEVICE_3DOF -> {
                        val r = devicePose.rotation
                        TextEntry(
                            "Device (3DoF): $deviceTrackingState\nRot: ${"%.3f".format(Locale.US, r.x)}, ${"%.3f".format(Locale.US, r.y)}, ${"%.3f".format(Locale.US, r.z)}, ${"%.3f".format(Locale.US, r.w)}"
                        )
                    }
                    TestMode.ALL_DISABLED -> {
                        TextEntry("Tracking: DISABLED")
                    }
                    TestMode.GEO_ONLY -> {
                        TextEntry("Device Tracking: DISABLED")
                        TextEntry(
                            "Geo: $geoState | Lat: ${"%.5f".format(Locale.US, geoPose.latitude)}, Lon: ${"%.5f".format(Locale.US, geoPose.longitude)}"
                        )
                        TextEntry(getOrientationDescription(geoPose.eastUpSouthQuaternion))
                    }
                    else -> {
                        val t = devicePose.translation
                        TextEntry(
                            "Device: $deviceTrackingState | Pos: ${"%.3f".format(Locale.US, t.x)}, ${"%.3f".format(Locale.US, t.y)}, ${"%.3f".format(Locale.US, t.z)}"
                        )
                        if (
                            currentMode == TestMode.HIGH_ACCURACY_GEO ||
                                currentMode == TestMode.LOW_POWER_GEO
                        ) {
                            TextEntry(
                                "Geo: $geoState | Lat: ${"%.5f".format(Locale.US, geoPose.latitude)}, Lon: ${"%.5f".format(Locale.US, geoPose.longitude)}"
                            )
                            TextEntry(getOrientationDescription(geoPose.eastUpSouthQuaternion))
                        }
                    }
                }

                TextEntry("Info: $message")
            }
        }
    }

    @Composable
    private fun TextEntry(text: String) {
        Text(text = text, style = GlimmerTheme.typography.bodyLarge)
    }
}
