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

package androidx.xr.compose.integration.layout.activitypanelapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.xr.compose.integration.common.AnotherActivity
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialActivityPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.resizable
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.unit.DpVolumeSize

/**
 * App that stress tests the activity panels.
 *
 * This app creates 11 activity panels, 6 on the left side of the screen and 5 on the right side of
 * the screen.
 */
class ActivityPanelApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    /**
     * Spatial content for the app.
     *
     * This content creates 11 activity panels, 6 on the left column and 5 on the right column. With
     * one of the panels in the left column being the AnotherActivityPanel.
     */
    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        val panelWidth = 200.dp
        val panelHeight = 200.dp
        val minimumSize = DpVolumeSize(25.dp, 25.dp, 25.dp)

        SpatialColumn(SubspaceModifier.width(200.dp).height(1400.dp)) {
            SpatialActivityPanel(
                modifier =
                    SubspaceModifier.width(panelWidth)
                        .height(panelHeight)
                        .movable()
                        .resizable(minimumSize = minimumSize)
                        .testTag("AnotherActivityPanel"),
                intent = Intent(this@ActivityPanelApp, AnotherActivity::class.java),
            )
            SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
            for (i in 1..5) {
                SpatialActivityPanel(
                    modifier =
                        SubspaceModifier.width(panelWidth)
                            .height(panelHeight)
                            .movable()
                            .resizable(minimumSize = minimumSize)
                            .testTag("BaseActivityPanel"),
                    intent =
                        Intent(this@ActivityPanelApp, BaseActivity::class.java)
                            .putExtra("activityName", "Activity $i"),
                )
                SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
            }
        }

        SpatialColumn(modifier = SubspaceModifier.offset(x = 200.dp)) {
            for (i in 6..10) {
                SpatialActivityPanel(
                    intent =
                        Intent(this@ActivityPanelApp, BaseActivity::class.java)
                            .putExtra("activityName", "Activity $i"),
                    modifier =
                        SubspaceModifier.width(panelWidth)
                            .height(panelHeight)
                            .movable()
                            .resizable(minimumSize = minimumSize)
                            .testTag("BaseActivityPanel"),
                )
                SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
            }
        }
    }
}
