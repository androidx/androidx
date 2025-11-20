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

package androidx.xr.compose.testapp.spatialarrangementusage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.components.CommonTestPanel
import androidx.xr.compose.testapp.ui.theme.IntegrationTestsAppTheme
import androidx.xr.compose.unit.DpVolumeSize

class SpatialArrangementUsageActivity : ComponentActivity() {

    enum class RowColumn {
        SpatialRow,
        SpatialColumn,
    }

    object ArrangementValueLists {
        val horizontalArrangementValues: List<SpatialArrangement.Horizontal> =
            listOf(
                SpatialArrangement.Start,
                SpatialArrangement.Center,
                SpatialArrangement.End,
                SpatialArrangement.SpaceBetween,
                SpatialArrangement.SpaceAround,
                SpatialArrangement.SpaceEvenly,
                SpatialArrangement.Absolute.Left,
                SpatialArrangement.Absolute.Center,
                SpatialArrangement.Absolute.Right,
                SpatialArrangement.Absolute.SpaceBetween,
                SpatialArrangement.Absolute.SpaceAround,
                SpatialArrangement.Absolute.SpaceEvenly,
            )

        val verticalArrangementValues: List<SpatialArrangement.Vertical> =
            listOf(
                SpatialArrangement.Top,
                SpatialArrangement.Center,
                SpatialArrangement.Bottom,
                SpatialArrangement.SpaceBetween,
                SpatialArrangement.SpaceAround,
                SpatialArrangement.SpaceEvenly,
            )
    }

    data class SpatialArrangementUiState(
        val mainAxis: String = "500",
        val crossAxis: String = "150",
        val rowColumn: RowColumn = RowColumn.SpatialRow,
        val horizontalArrangement: SpatialArrangement.Horizontal =
            ArrangementValueLists.horizontalArrangementValues[0],
        val verticalArrangement: SpatialArrangement.Vertical =
            ArrangementValueLists.verticalArrangementValues[0],
        val space: Dp = 0.dp,
        val horizontalAlignment: SpatialAlignment.Horizontal = SpatialAlignment.Start,
        val verticalAlignment: SpatialAlignment.Vertical = SpatialAlignment.Top,
        val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { IntegrationTestsAppTheme { SpatialArrangementUsageApp() } }
    }

    @Composable
    private fun SpatialArrangementUsageApp() {
        Subspace { SpatialContent() }
    }

    @Composable
    @SubspaceComposable
    private fun SpatialContent() {
        var uiState by remember { mutableStateOf(SpatialArrangementUiState()) }
        SpatialRow {
            ArrangementOptions(
                uiState = uiState,
                onUiStateChange = { newState -> uiState = newState },
            )

            ArrangedLayout(
                mainAxis = Dp(uiState.mainAxis.toFloat()),
                crossAxis = Dp(uiState.crossAxis.toFloat()),
                rowColumn = uiState.rowColumn,
                horizontalArrangement = uiState.horizontalArrangement,
                verticalArrangement = uiState.verticalArrangement,
                layoutDirection = uiState.layoutDirection,
            )
        }
    }

    @Composable
    @SubspaceComposable
    private fun ArrangementOptions(
        uiState: SpatialArrangementUiState,
        onUiStateChange: (SpatialArrangementUiState) -> Unit,
    ) {
        CommonTestPanel(
            size = DpVolumeSize(600.dp, 600.dp, 0.dp),
            title = getString(R.string.spatial_arrangement_usage_test_case),
            showBottomBar = true,
            onClickBackArrow = { this@SpatialArrangementUsageActivity.finish() },
            onClickRecreate = { this@SpatialArrangementUsageActivity.recreate() },
        ) { padding ->
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH") // b/446706254
            Column(
                modifier =
                    Modifier.fillMaxSize().background(Color.White).padding(padding).padding(20.dp)
            ) {
                MyRow {
                    var mainAxisTextFieldValue by remember { mutableStateOf(uiState.mainAxis) }
                    MyTextBox(text = "Main Axis:    ")
                    TextField(
                        value = mainAxisTextFieldValue,
                        onValueChange = { mainAxisTextFieldValue = it },
                    )
                    Button(
                        onClick = {
                            if (mainAxisTextFieldValue.isEmpty() || mainAxisTextFieldValue == "0") {
                                mainAxisTextFieldValue = uiState.mainAxis
                            } else {
                                onUiStateChange(uiState.copy(mainAxis = mainAxisTextFieldValue))
                            }
                        }
                    ) {
                        Text("Set")
                    }
                }
                MyRow {
                    var crossAxisTextFieldValue by remember { mutableStateOf(uiState.crossAxis) }
                    MyTextBox(text = "Cross Axis:    ")
                    TextField(
                        value = crossAxisTextFieldValue,
                        onValueChange = { crossAxisTextFieldValue = it },
                    )
                    Button(
                        onClick = {
                            if (
                                crossAxisTextFieldValue.isEmpty() || crossAxisTextFieldValue == "0"
                            ) {
                                crossAxisTextFieldValue = uiState.crossAxis
                            } else {
                                onUiStateChange(uiState.copy(crossAxis = crossAxisTextFieldValue))
                            }
                        }
                    ) {
                        Text("Set")
                    }
                }
                MyRow {
                    MyTextBox(text = "Layout Direction:   ")
                    MyTextBox("LTR")
                    RadioButton(
                        selected = uiState.layoutDirection == LayoutDirection.Ltr,
                        onClick = {
                            onUiStateChange(uiState.copy(layoutDirection = LayoutDirection.Ltr))
                        },
                    )
                    MyTextBox("RTL")
                    RadioButton(
                        selected = uiState.layoutDirection == LayoutDirection.Rtl,
                        onClick = {
                            onUiStateChange(uiState.copy(layoutDirection = LayoutDirection.Rtl))
                        },
                    )
                }
                MyRow {
                    MyTextBox(text = "Arrangement:   ")
                    MyTextBox("SpatialRow")
                    RadioButton(
                        selected = uiState.rowColumn == RowColumn.SpatialRow,
                        onClick = {
                            onUiStateChange(uiState.copy(rowColumn = RowColumn.SpatialRow))
                        },
                    )
                    MyTextBox("SpatialColumn")
                    RadioButton(
                        selected = uiState.rowColumn == RowColumn.SpatialColumn,
                        onClick = {
                            onUiStateChange(uiState.copy(rowColumn = RowColumn.SpatialColumn))
                        },
                    )
                }
                when (uiState.rowColumn) {
                    RowColumn.SpatialRow -> {
                        MyDropDownMenu(
                            label = "Horizontal Arrangement:   ",
                            selectedValue = uiState.horizontalArrangement,
                            values = ArrangementValueLists.horizontalArrangementValues,
                            onValueSelected = {
                                onUiStateChange(uiState.copy(horizontalArrangement = it))
                            },
                        )
                    }
                    RowColumn.SpatialColumn -> {
                        MyDropDownMenu(
                            label = "Vertical Arrangement:   ",
                            selectedValue = uiState.verticalArrangement,
                            values = ArrangementValueLists.verticalArrangementValues,
                            onValueSelected = {
                                onUiStateChange(uiState.copy(verticalArrangement = it))
                            },
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MyRow(content: @Composable () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            content()
        }
    }

    @Composable
    fun MyTextBox(text: String) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = Color.Black,
            style = TextStyle(fontSize = 20.sp),
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun <T> MyDropDownMenu(
        label: String,
        selectedValue: T,
        values: List<T>,
        onValueSelected: (T) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        MyRow {
            MyTextBox(text = label)
            Box {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    TextField(
                        modifier =
                            Modifier.menuAnchor(
                                ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                true,
                            ),
                        readOnly = true,
                        value = selectedValue.toString(),
                        onValueChange = {},
                        label = { Text("Select") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        values.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.toString()) },
                                onClick = {
                                    onValueSelected(item)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    @SubspaceComposable
    private fun ArrangedLayout(
        mainAxis: Dp,
        crossAxis: Dp,
        rowColumn: RowColumn,
        horizontalArrangement: SpatialArrangement.Horizontal = SpatialArrangement.Start,
        verticalArrangement: SpatialArrangement.Vertical = SpatialArrangement.Top,
        layoutDirection: LayoutDirection,
    ) {
        val boxWidth = if (rowColumn == RowColumn.SpatialRow) mainAxis else crossAxis
        val boxHeight = if (rowColumn == RowColumn.SpatialRow) crossAxis else mainAxis
        SpatialBox(modifier = SubspaceModifier.padding(100.dp)) {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                when (rowColumn) {
                    RowColumn.SpatialRow -> {
                        SpatialRow(
                            modifier =
                                SubspaceModifier.width(boxWidth).height(boxHeight).padding(10.dp),
                            horizontalArrangement = horizontalArrangement,
                        ) {
                            PanelBox(text = "1")
                            PanelBox(text = "2")
                            PanelBox(text = "3")
                        }
                    }
                    RowColumn.SpatialColumn -> {
                        SpatialColumn(
                            modifier =
                                SubspaceModifier.width(boxWidth).height(boxHeight).padding(10.dp),
                            verticalArrangement = verticalArrangement,
                        ) {
                            PanelBox(text = "1")
                            PanelBox(text = "2")
                            PanelBox(text = "3")
                        }
                    }
                }
            }
            SpatialPanel(shape = SpatialRoundedCornerShape(CornerSize(4.dp))) {
                Box(
                    modifier = Modifier.width(boxWidth).height(boxHeight).border(10.dp, Color.Black)
                )
            }
        }
    }

    @Composable
    @SubspaceComposable
    private fun PanelBox(modifier: SubspaceModifier = SubspaceModifier, text: String) {
        SpatialPanel(modifier = modifier, shape = SpatialRoundedCornerShape(CornerSize(16.dp))) {
            Box(
                modifier = Modifier.size(100.dp).background(Color.LightGray),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = text, style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}
