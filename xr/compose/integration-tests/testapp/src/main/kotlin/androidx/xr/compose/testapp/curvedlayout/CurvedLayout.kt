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

package androidx.xr.compose.testapp.curvedlayout

import android.graphics.Color.BLACK
import android.graphics.Color.LTGRAY
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialAndroidViewPanel
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.fillMaxWidth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.Purple80

class CurvedLayout : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntegrationTestsAppTheme { CurvedLayoutApp() } }
    }

    @Composable
    private fun CurvedLayoutApp() {
        PanelContent(
            true,
            title = getString(R.string.curve_panel_row_test),
            "Panel Center - main task window",
        )
        Subspace { PanelGrid() }
    }

    @Composable
    private fun PanelGrid() {
        var curvePercent by remember { mutableFloatStateOf(0.625f) }
        val curveRadius by remember { derivedStateOf { (1000.dp * curvePercent) + 200.dp } }
        val sidePanelModifier = SubspaceModifier.fillMaxWidth().height(200.dp)
        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(40.dp)) {
            SpatialRow {
                // Curve radius adjustment panel.
                SpatialPanel(
                    modifier = SubspaceModifier.width(400.dp),
                    shape = SpatialRoundedCornerShape(CornerSize(0.dp)),
                ) {
                    Column {
                        Text(
                            modifier = Modifier.testTag("curve_radius"),
                            text = "Curve radius: $curveRadius",
                            fontSize = 20.sp,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Slider(
                            modifier = Modifier.testTag("curve_slider"),
                            value = curvePercent,
                            onValueChange = { curvePercent = it },
                            colors = SliderDefaults.colors(activeTrackColor = Purple80),
                        )
                    }
                }
            }

            SpatialCurvedRow(
                modifier = SubspaceModifier.width(2000.dp).height(600.dp),
                alignment = SpatialAlignment.BottomCenter,
                curveRadius = curveRadius,
                horizontalArrangement = SpatialArrangement.SpaceEvenly,
            ) {
                SpatialColumn(
                    modifier = SubspaceModifier.width(200.dp).fillMaxHeight(),
                    verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                ) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Left")
                    ViewBasedAppPanel(
                        modifier = sidePanelModifier,
                        text = "Panel Bottom Left (View)",
                    )
                }
                SpatialColumn(modifier = SubspaceModifier.width(800.dp).fillMaxHeight()) {
                    SpatialMainPanel(modifier = SubspaceModifier.fillMaxSize())
                }
                SpatialColumn(
                    modifier = SubspaceModifier.width(200.dp).fillMaxHeight(),
                    verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                ) {
                    AppPanel(modifier = sidePanelModifier, text = "Panel Top Right")
                    AppPanel(modifier = sidePanelModifier, text = "Panel Bottom Right")
                }
            }
        }
    }

    @Composable
    private fun AppPanel(modifier: SubspaceModifier = SubspaceModifier, text: String = "") {
        SpatialPanel(modifier = modifier) { PanelContent(false, title = "Side Panel", text) }
    }

    @Composable
    private fun PanelContent(backButton: Boolean = false, title: String, vararg text: String) {
        var addHighlight by remember { mutableStateOf(false) }
        val borderWidth by remember { derivedStateOf { if (addHighlight) 4.dp else 0.dp } }
        val lambda = { this@CurvedLayout.finish() }
        val recreateLambda = { this@CurvedLayout.recreate() }
        CommonTestScaffold(
            title = title,
            showBottomBar = backButton,
            onClickBackArrow = if (backButton) lambda else null,
            onClickRecreate = if (backButton) recreateLambda else null,
        ) { padding ->
            Box(
                modifier =
                    Modifier.background(Purple80)
                        .padding(padding)
                        .fillMaxSize()
                        .border(width = borderWidth, color = Color.Cyan),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (item in text) {
                        Text(text = item, fontSize = 20.sp, color = Color.Black)
                    }
                }
                Orbiter(position = ContentEdge.End, offset = 30.dp) {
                    IconButton(
                        onClick = { addHighlight = !addHighlight },
                        modifier = Modifier.background(Purple40),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Add highlight",
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ViewBasedAppPanel(
        modifier: SubspaceModifier = SubspaceModifier,
        text: String = "",
    ) {
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
        SpatialAndroidViewPanel(factory = { textView }, modifier = modifier)
    }
}
