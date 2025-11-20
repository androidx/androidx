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

package androidx.xr.compose.testapp.depthstacking

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.alpha
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.onGloballyPositioned
import androidx.xr.compose.subspace.layout.scale
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold

class DepthStacking : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Subspace { SpatialContent() } }
    }

    @Composable
    private fun SpatialContent() {
        val panelSize = SubspaceModifier.width(550.dp).height(300.dp)
        var depthChecked by remember { mutableStateOf(false) }
        var modifierChecked by remember { mutableStateOf(false) }

        SpatialPanel(
            modifier = panelSize.offset((-500).dp, 0.dp, 0.dp),
            dragPolicy = MovePolicy(),
        ) {
            CommonTestScaffold(
                title = "Panel Stacking Layout Tests",
                showBottomBar = true,
                onClickBackArrow = { this@DepthStacking.finish() },
            ) { padding ->
                Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    SwitchRow("Depth Stacking Test", depthChecked) { depthChecked = it }
                    SwitchRow("Modifier Order Test", modifierChecked) { modifierChecked = it }
                }
            }
        }

        if (depthChecked) {
            DepthPanels()
        }

        if (modifierChecked) {
            ModifierPanels()
        }
    }

    @Composable
    private fun SwitchRow(
        switchText: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp, 0.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(switchText, modifier = Modifier.weight(0.7f), fontSize = 24.sp)
            Switch(
                modifier = Modifier.weight(0.3f),
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = true,
                thumbContent =
                    if (checked) {
                        { Icon(Icons.Filled.Check, null) }
                    } else {
                        { Icon(Icons.Filled.Clear, null) }
                    },
            )
        }
    }

    @Composable
    private fun DepthPanels() {
        val panelSize = SubspaceModifier.width(200.dp).height(200.dp)

        SpatialPanel(modifier = panelSize.offset(0.dp, 400.dp, (-50).dp).testTag("BackPanel")) {
            PanelContent(Color.Red, "Back Panel")
        }
        SpatialPanel(modifier = panelSize.offset(0.dp, 400.dp, 0.dp).testTag("MiddlePanel")) {
            PanelContent(Color.White, "Middle Panel")
        }
        SpatialPanel(modifier = panelSize.offset(0.dp, 400.dp, 50.dp).testTag("FrontPanel")) {
            PanelContent(Color.Blue, "Front Panel")
        }
    }

    @Composable
    private fun ModifierPanels() {
        val panelSize = SubspaceModifier.offset(x = 10.dp).width(200.dp).alpha(1f).height(200.dp)

        SpatialPanel(
            modifier =
                panelSize
                    .offset(z = (-50).dp)
                    .scale(1.2f)
                    .onGloballyPositioned {
                        Log.i("ModifierOrderApp", "BackPanel position: ${it.poseInRoot}")
                    }
                    .offset(y = (-100).dp)
                    .testTag("Back Panel"),
            dragPolicy = MovePolicy(),
            resizePolicy = ResizePolicy(),
        ) {
            PanelContent(Color.Red, "Back Panel")
        }
        SpatialPanel(
            modifier =
                panelSize
                    .onGloballyPositioned {
                        Log.i("ModifierOrderApp", "MiddlePanel position: ${it.poseInRoot}")
                    }
                    .scale(0.9f)
                    .testTag("Middle Panel"),
            dragPolicy = MovePolicy(),
            resizePolicy = ResizePolicy(),
        ) {
            PanelContent(Color.White, "Middle Panel")
        }
        SpatialPanel(
            modifier =
                panelSize
                    .scale(0.8f)
                    .offset(z = 50.dp)
                    .onGloballyPositioned {
                        Log.i("ModifierOrderApp", "FrontPanel position: ${it.poseInRoot}")
                    }
                    .offset(y = 100.dp)
                    .testTag("Front Panel"),
            dragPolicy = MovePolicy(),
            resizePolicy = ResizePolicy(),
        ) {
            PanelContent(Color.Blue, "Front Panel")
        }
    }

    @UiComposable
    @Composable
    private fun PanelContent(color: Color, text: String) {
        Box(
            modifier = Modifier.background(color).fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp,
                    color = Color.Black,
                )
            }
        }
    }
}
