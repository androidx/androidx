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

package androidx.xr.compose.samples

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.Sampled
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.ExperimentalSpatialContinuityApi
import androidx.xr.compose.subspace.layout.SpatialContinuityCapturer
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.spatialContinuityTarget

/** A sample demonstrating how to use [SubspaceModifier.spatialContinuityTarget]. */
@OptIn(ExperimentalSpatialContinuityApi::class)
@Sampled
@SubspaceComposable
@Composable
public fun SpatialContinuitySample(intent: Intent) {
    val context = LocalContext.current
    val leftPanelCapturer = remember { SpatialContinuityCapturer() }
    val rightPanelCapturer = remember { SpatialContinuityCapturer() }

    SpatialRow {
        SpatialPanel(
            modifier =
                SubspaceModifier.spatialContinuityTarget(
                    spatialContinuityCapturer = leftPanelCapturer
                )
        ) {
            Button(
                onClick = {
                    // Explicitly set the left panel as the point of continuity before launching
                    leftPanelCapturer.captureContinuity()
                    context.startActivity(intent)
                }
            ) {
                Text("Launch from Left Panel")
            }
        }

        SpatialPanel(
            modifier =
                SubspaceModifier.spatialContinuityTarget(
                    spatialContinuityCapturer = rightPanelCapturer
                )
        ) {
            Button(
                onClick = {
                    // Explicitly set the right panel as the point of continuity before launching
                    rightPanelCapturer.captureContinuity()
                    context.startActivity(intent)
                }
            ) {
                Text("Launch from Right Panel")
            }
        }
    }
}

/**
 * A sample demonstrating how to use [SpatialContinuityCapturer.resetContinuity] with a
 * [BackHandler] to reset the spatial continuity target when navigating back.
 */
@OptIn(ExperimentalSpatialContinuityApi::class)
@Sampled
@SubspaceComposable
@Composable
public fun ResetSpatialContinuitySample() {
    val session = LocalSession.current
    val activity = LocalActivity.current
    val spatialContinuityCapturer = remember { SpatialContinuityCapturer() }

    SpatialPanel(
        modifier =
            SubspaceModifier.spatialContinuityTarget(
                spatialContinuityCapturer = spatialContinuityCapturer
            )
    ) {
        BackHandler {
            if (session != null) {
                SpatialContinuityCapturer.resetContinuity(session)
            }
            activity?.finish()
        }
        Button(onClick = { spatialContinuityCapturer.captureContinuity() }) {
            Text("Capture Continuity")
        }
    }
}
