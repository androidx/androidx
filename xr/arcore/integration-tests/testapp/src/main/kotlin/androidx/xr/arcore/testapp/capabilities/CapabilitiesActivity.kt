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

package androidx.xr.arcore.testapp.capabilities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.transformingMovable
import androidx.xr.compose.subspace.layout.transformingResizable
import androidx.xr.compose.unit.DpVolumeSize
import androidx.xr.runtime.DepthEstimationMode
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.RenderingMode
import androidx.xr.runtime.XrDevice

class CapabilitiesActivity : ComponentActivity() {

    private lateinit var xrDevice: XrDevice
    private val handTrackingCaps = listOf(HandTrackingMode.DISABLED, HandTrackingMode.BOTH)
    private val eyeTrackingCaps =
        listOf(
            EyeTrackingMode.DISABLED,
            EyeTrackingMode.FINE_TRACKING,
            EyeTrackingMode.COARSE_TRACKING,
        )
    private val depthEstimationCaps =
        listOf(
            DepthEstimationMode.DISABLED,
            DepthEstimationMode.RAW_ONLY,
            DepthEstimationMode.SMOOTH_ONLY,
            DepthEstimationMode.SMOOTH_AND_RAW,
        )
    private val geospatialCaps = listOf(GeospatialMode.DISABLED, GeospatialMode.SPATIAL)
    private val renderingCaps = listOf(RenderingMode.MONO, RenderingMode.STEREO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        xrDevice = XrDevice.getCurrentDevice(this)

        setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.size(DpVolumeSize(640.dp, 480.dp, 0.dp))
                            .transformingMovable()
                            .transformingResizable()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize().padding(0.dp),
                        topBar = {
                            Row(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(0.dp)
                                        .background(color = GoogleYellow),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                BackToMainActivityButton()
                                Text(
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = "Device Capabilities",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                )
                            }
                        },
                    ) { innerPadding ->
                        Column(
                            modifier =
                                Modifier.padding(innerPadding).background(color = Color.White)
                        ) {
                            CapabilitiesRow(
                                "Hand Tracking Caps:",
                                handTrackingCaps,
                                xrDevice::isHandTrackingModeSupported,
                            )
                            CapabilitiesRow(
                                "Eye Tracking Caps:",
                                eyeTrackingCaps,
                                xrDevice::isEyeTrackingModeSupported,
                            )
                            CapabilitiesRow(
                                "Depth Estimation Caps:",
                                depthEstimationCaps,
                                xrDevice::isDepthEstimationModeSupported,
                            )
                            CapabilitiesRow(
                                "Geospatial Caps:",
                                geospatialCaps,
                                xrDevice::isGeospatialModeSupported,
                            )
                            CapabilitiesRow(
                                "Rendering Caps:",
                                renderingCaps,
                                xrDevice::isRenderingModeSupported,
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    inline fun <reified T> CapabilitiesRow(
        identifier: String,
        caps: Collection<T>,
        isSupportedFn: (T) -> Boolean,
    ) {
        var enabledCapabilitiesList = ""
        caps
            .filter { mode -> isSupportedFn(mode) }
            .forEach { supportedMode ->
                val modeString = getModeString(supportedMode)
                if (enabledCapabilitiesList == "") {
                    enabledCapabilitiesList = modeString
                } else {
                    enabledCapabilitiesList = enabledCapabilitiesList.plus(", $modeString")
                }
            }
        Row(Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(start = 10.dp).weight(1f),
                text = identifier,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                modifier = Modifier.padding(start = 10.dp).weight(3f),
                text = enabledCapabilitiesList,
                fontSize = 20.sp,
            )
        }
    }

    inline fun <reified T> getModeString(mode: T): String {
        return when (mode) {
            // Hand Tracking Modes
            HandTrackingMode.DISABLED -> "DISABLED"
            HandTrackingMode.BOTH -> "BOTH"
            // Eye Tracking Modes
            EyeTrackingMode.DISABLED -> "DISABLED"
            EyeTrackingMode.FINE_TRACKING -> "FINE_TRACKING"
            EyeTrackingMode.COARSE_TRACKING -> "COARSE_TRACKING"
            // Depth Estimation Modes
            DepthEstimationMode.DISABLED -> "DISABLED"
            DepthEstimationMode.RAW_ONLY -> "RAW_ONLY"
            DepthEstimationMode.SMOOTH_ONLY -> "SMOOTH_ONLY"
            DepthEstimationMode.SMOOTH_AND_RAW -> "SMOOTH_AND_RAW"
            // Geospatial  Modes
            GeospatialMode.DISABLED -> "DISABLED"
            GeospatialMode.SPATIAL -> "SPATIAL"
            // Rendering Modes
            RenderingMode.MONO -> "MONO"
            RenderingMode.STEREO -> "STEREO"
            else -> mode.toString()
        }
    }
}
