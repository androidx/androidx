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

package androidx.xr.compose.testapp.rtlawareness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAbsoluteAlignment
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.absoluteOffset
import androidx.xr.compose.subspace.layout.absolutePadding
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.unit.DpVolumeSize

class RtlAwareSubspaceModifierActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntegrationTestsAppTheme { AppContent() } }
    }

    @Composable
    private fun AppContent() {
        Subspace { SpatialContent() }
    }

    private enum class ModifierToCheck {
        PADDING,
        OFFSET,
        ABSOLUTE_PADDING,
        ABSOLUTE_OFFSET,
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        var absoluteBehavior by remember { mutableStateOf(false) }
        var layoutDirection by remember { mutableStateOf(LayoutDirection.Ltr) }
        SpatialRow(horizontalArrangement = SpatialArrangement.spacedBy(40.dp)) {
            SpatialColumn {
                CommonTestPanel(
                    size = DpVolumeSize(500.dp, 400.dp, 0.dp),
                    title = getString(R.string.subspace_modifiers_rtl_awareness_test_case),
                    showBottomBar = true,
                    onClickBackArrow = { this@RtlAwareSubspaceModifierActivity.finish() },
                    onClickRecreate = { this@RtlAwareSubspaceModifierActivity.recreate() },
                ) { padding ->
                    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color.White)
                                .padding(padding)
                                .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                    ) {
                        Text(text = "View Absolute behavior", color = Color.Black)
                        Switch(
                            checked = absoluteBehavior,
                            onCheckedChange = { absoluteBehavior = it },
                        )
                        Text(text = "View LayoutDirection RTL", color = Color.Black)
                        Switch(
                            checked = layoutDirection == LayoutDirection.Rtl,
                            onCheckedChange = {
                                layoutDirection =
                                    if (it) LayoutDirection.Rtl else LayoutDirection.Ltr
                            },
                        )
                    }
                }
            }
            ColumnContent(
                absoluteBehavior = absoluteBehavior,
                layoutDirectionToCheck = layoutDirection,
            )
        }
    }

    @Composable
    @SubspaceComposable
    private fun ColumnContent(absoluteBehavior: Boolean, layoutDirectionToCheck: LayoutDirection) {
        SpatialColumn(verticalArrangement = SpatialArrangement.spacedBy(40.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirectionToCheck) {
                SpatialPanel(SubspaceModifier.width(300.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Layout Direction - ${layoutDirectionToCheck.name}",
                            style =
                                MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                        )
                    }
                }
                BoxContent(
                    absoluteBehavior = absoluteBehavior,
                    modifierToCheck =
                        if (absoluteBehavior) ModifierToCheck.ABSOLUTE_PADDING
                        else ModifierToCheck.PADDING,
                )
                BoxContent(
                    absoluteBehavior = absoluteBehavior,
                    modifierToCheck =
                        if (absoluteBehavior) ModifierToCheck.ABSOLUTE_OFFSET
                        else ModifierToCheck.OFFSET,
                )
            }
        }
    }

    @Composable
    @SubspaceComposable
    private fun BoxContent(absoluteBehavior: Boolean, modifierToCheck: ModifierToCheck) {
        SpatialBox(
            modifier = SubspaceModifier.size(300.dp),
            alignment =
                if (absoluteBehavior) SpatialAbsoluteAlignment.CenterLeft
                else SpatialAlignment.CenterStart,
        ) {
            SpatialPanel(modifier = getFinalModifier(modifierToCheck)) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Gray),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        modifier = Modifier,
                        text = modifierToCheck.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            SpatialPanel(modifier = SubspaceModifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White))
            }
        }
    }

    private fun getFinalModifier(modifierToCheck: ModifierToCheck): SubspaceModifier {
        val finalModifier = SubspaceModifier.size(150.dp)
        return when (modifierToCheck) {
            ModifierToCheck.PADDING -> finalModifier.padding(start = 20.dp)
            ModifierToCheck.OFFSET -> finalModifier.offset(x = 20.dp)
            ModifierToCheck.ABSOLUTE_PADDING -> finalModifier.absolutePadding(left = 20.dp)
            ModifierToCheck.ABSOLUTE_OFFSET -> finalModifier.absoluteOffset(x = 20.dp)
        }
    }
}
