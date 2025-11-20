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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.AnchoredSubspace
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.AnchorEntity

@Sampled
public fun AnchoredSubspaceSample() {
    val TAG = "AnchoredSubspaceSample"
    @Composable
    fun MainPanelContent() {
        Text("Main panel")
    }

    @Composable
    fun AppContent() {
        MainPanelContent()

        var session: Session? = LocalSession.current
        if (session == null) return

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
            AnchoredSubspace(
                lockTo = anchor,
                modifier = SubspaceModifier.rotate(pitch = -90f, 0f, 0f),
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
