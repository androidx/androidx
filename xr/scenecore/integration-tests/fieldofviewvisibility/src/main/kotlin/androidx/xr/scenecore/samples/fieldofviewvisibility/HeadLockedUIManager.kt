/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.xr.scenecore.samples.fieldofviewvisibility

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.samples.commontestview.DebugTextPanel
import androidx.xr.scenecore.scene

/** Manage the Head Locked UI. */
@Suppress("Deprecation")
// TODO - b/421386891: is/setHidden is deprecated; this activity needs to be updated to use
// is/setEnabled.
class HeadLockedUIManager(session: Session, headLockedPanelView: View) {
    private val TAG = "HeadLockedUIManager"
    private val mSession: Session
    private val mHeadLockedPanelView: View
    private val mHeadLockedPanel: PanelEntity
    private var mEnableHeadlock: Boolean by mutableStateOf(true)
    private var mUserForward: Pose by mutableStateOf(Pose(Vector3(0f, 0.00f, -1.3f)))
    private lateinit var mDebugPanel: DebugTextPanel

    init {
        mSession = session
        mHeadLockedPanelView = headLockedPanelView

        this.mHeadLockedPanelView.postOnAnimation(this::updateHeadLockedPose)
        this.mHeadLockedPanel =
            PanelEntity.create(
                session = mSession,
                view = mHeadLockedPanelView,
                pixelDimensions = IntSize2d(800, 360),
                name = "headLockedPanel",
                pose = Pose(Vector3(0f, 0f, 0f)),
            )
        this.mHeadLockedPanel.parent = mSession.scene.activitySpace
    }

    private fun updateHeadLockedPose() {
        if (mSession.scene.spatialUser.head != null && this.mEnableHeadlock) {
            // Since the panel is parented by the activitySpace, we need to inverse its scale
            // so that the panel stays at a fixed size in the view even when ActivitySpace scales.
            this.mHeadLockedPanel.setScale(
                0.5f / mSession.scene.activitySpace.getScale(Space.REAL_WORLD)
            )
            mSession.scene.spatialUser.head
                ?.transformPoseTo(mUserForward, mSession.scene.activitySpace)
                ?.let { this.mHeadLockedPanel.setPose(it) }
        }
        mHeadLockedPanelView.postOnAnimation(this::updateHeadLockedPose)
    }

    @Composable
    fun HeadLockedUISettings() {
        var sliderPositionAlpha by remember { mutableFloatStateOf(0.0f) }
        var modelIsHidden by remember { mutableStateOf(false) }

        Column(verticalArrangement = Arrangement.Top) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Slider for headlocked UI Panel alpha
                Text(
                    text = "Alpha ${sliderPositionAlpha}%",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                )
                Slider(
                    value = sliderPositionAlpha,
                    onValueChange = {
                        sliderPositionAlpha = it
                        mHeadLockedPanel.setAlpha(sliderPositionAlpha)
                    },
                    valueRange = 0.0f..1.0f,
                    steps = 101,
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
                // Button to toggle the visibility of the headlocked UI
                Button(
                    onClick = {
                        modelIsHidden = mHeadLockedPanel.isHidden(true)
                        mHeadLockedPanel.setHidden(!modelIsHidden)
                        modelIsHidden = !modelIsHidden
                    }
                ) {
                    Text(
                        text = (if (modelIsHidden) "Show Panel" else "Hide Panel"),
                        fontSize = 20.sp,
                    )
                }
                // Toggle headlocked UI
                Button(onClick = { mEnableHeadlock = !mEnableHeadlock }) {
                    Text(
                        text = (if (mEnableHeadlock) "Disable Headlock" else "Enable Headlock"),
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}
