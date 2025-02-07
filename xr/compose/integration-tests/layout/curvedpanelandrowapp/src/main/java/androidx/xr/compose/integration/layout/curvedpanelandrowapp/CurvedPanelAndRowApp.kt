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

package androidx.xr.compose.integration.layout.curvedpanelandrowapp

import android.annotation.SuppressLint
import android.graphics.Color.BLACK
import android.graphics.Color.LTGRAY
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterEdge
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MainPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialLayoutSpacer
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width

class CurvedPanelAndRowApp : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PanelContent("Panel Center - main task window", "CL: N/A")
            Subspace { PanelGrid() }
        }
        isDebugInspectorInfoEnabled = true
    }

    @Composable
    fun PanelGrid() {
        var curvePercent by remember { mutableFloatStateOf(0.625f) }
        val curveRadius by remember { derivedStateOf { (1000.dp * curvePercent) + 200.dp } }
        val sidePanelModifier = SubspaceModifier.fillMaxWidth().height(200.dp)
        SpatialColumn {
            SpatialRow(alignment = SpatialAlignment.TopCenter) {
                // Curve radius adjustment panel.
                SpatialPanel(
                    modifier = SubspaceModifier.width(400.dp).height(150.dp),
                    shape = SpatialRoundedCornerShape(CornerSize(0.dp)),
                ) {
                    Column {
                        Text(text = "Curve radius: $curveRadius", fontSize = 20.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(value = curvePercent, onValueChange = { curvePercent = it })
                    }
                }
            }
            SpatialRow(
                modifier = SubspaceModifier.width(2000.dp).height(600.dp),
                alignment = SpatialAlignment.BottomCenter,
                curveRadius = curveRadius,
            ) {
                SpatialColumn(modifier = SubspaceModifier.width(200.dp).fillMaxHeight()) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Left")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    ViewBasedAppPanel(
                        modifier = sidePanelModifier,
                        text = "Panel Bottom Left (View)"
                    )
                }
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(800.dp).fillMaxHeight().padding(horizontal = 20.dp)
                ) {
                    MainPanel(modifier = SubspaceModifier.fillMaxSize())
                }
                SpatialColumn(modifier = SubspaceModifier.width(200.dp).fillMaxHeight()) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Right")
                    SpatialLayoutSpacer(modifier = SubspaceModifier.height(20.dp))
                    AppPanel(modifier = sidePanelModifier, text = "Panel Bottom Right")
                }
            }
        }
    }

    @SubspaceComposable
    @Composable
    fun AppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        SpatialPanel(modifier = modifier) { PanelContent(text) }
    }

    @UiComposable
    @Composable
    fun PanelContent(vararg text: String) {
        var addHighlight by remember { mutableStateOf(false) }
        val borderWidth by remember { derivedStateOf { if (addHighlight) 3.dp else 0.dp } }
        Box(
            modifier =
                Modifier.background(Color.LightGray)
                    .fillMaxSize()
                    .border(width = borderWidth, color = Color.Cyan),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                for (item in text) {
                    Text(text = item, fontSize = 20.sp)
                }
            }
            Orbiter(position = OrbiterEdge.End, offset = 24.dp) {
                IconButton(
                    onClick = { addHighlight = !addHighlight },
                    modifier = Modifier.background(Color.Gray),
                ) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = "Add highlight")
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Composable
    fun ViewBasedAppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        val context = LocalContext.current
        val textView = remember {
            TextView(context).apply {
                setText(text)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                setBackgroundColor(LTGRAY)
                setTextColor(BLACK)
                setGravity(Gravity.CENTER)
            }
        }

        SpatialPanel(view = textView, modifier = modifier)
    }
}
