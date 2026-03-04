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

package androidx.xr.compose.samples

import android.util.Log
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.spatial.FollowingSubspace
import androidx.xr.compose.subspace.FollowBehavior
import androidx.xr.compose.subspace.FollowTarget
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.AnchorEntity

@Sampled
@OptIn(ExperimentalFollowingSubspaceApi::class)
public fun FollowingSubspaceSample() {
    val TAG = "AnchoredSubspaceSample"
    @Composable
    fun MainPanelContent() {
        Text("Main panel")
    }

    @Composable
    fun AppContent() {
        MainPanelContent()

        val session: Session? = LocalSession.current
        if (session == null) return
        session.configure(
            config = session.config.copy(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN)
        )
        FollowingSubspace(
            target = FollowTarget.ArDevice(session),
            behavior = FollowBehavior.Soft(durationMs = 500),
        ) {
            SpatialPanel(SubspaceModifier.height(100.dp).width(200.dp)) {
                Text(
                    modifier =
                        Modifier.fillMaxWidth()
                            .fillMaxHeight()
                            .background(Color.White)
                            .padding(all = 32.dp),
                    text = "This panel will follow AR device movement.",
                )
            }
        }

        val anchor =
            remember(session) {
                when (val anchorResult = Anchor.create(session, Pose.Identity)) {
                    is AnchorCreateSuccess -> AnchorEntity.create(session, anchorResult.anchor)
                    else -> {
                        Log.e(TAG, "Failed to create anchor: ${anchorResult::class.simpleName}")
                        null
                    }
                }
            }
        if (anchor != null) {
            FollowingSubspace(
                target = FollowTarget.Anchor(anchorEntity = anchor),
                behavior = FollowBehavior.Tight,
                modifier = SubspaceModifier.rotate(pitch = -90f, yaw = 0f, roll = 0f),
            ) {
                SpatialRow {
                    SpatialPanel { Text("Spatial panel") }
                    SpatialMainPanel()
                }
            }
            DisposableEffect(anchor) { onDispose { anchor.dispose() } }
        }
    }
}
