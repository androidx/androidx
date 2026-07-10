/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.testapp.performance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialMainPanel
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.draw.scale
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testapp.ui.components.CommonTestScaffold
import androidx.xr.compose.testapp.ui.components.FpsCounterScreen
import androidx.xr.compose.testapp.ui.components.initializePanelRotationData

class LayoutPerformance : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MainPanelContent()
            Subspace { SpatialScene() }
        }
    }

    @Composable
    private fun MainPanelContent() {
        val title = intent.getStringExtra("TITLE") ?: "Layout Performance Test"
        CommonTestScaffold(
            title = title,
            showBottomBar = true,
            onClickBackArrow = { this@LayoutPerformance.finish() },
            onClickRecreate = { this@LayoutPerformance.recreate() },
        ) {
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF0F0F0)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "2D Control Panel (Main Activity)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }

    @Composable
    private fun SpatialScene() {
        var showRow by remember { mutableStateOf(false) }
        var showColumn by remember { mutableStateOf(false) }
        var showSubspace by remember { mutableStateOf(false) }
        var addRotationRow by remember { mutableStateOf(false) }
        var addRotationColumn by remember { mutableStateOf(false) }
        var addSizeAnimationRow by remember { mutableStateOf(false) }
        var addSizeAnimationColumn by remember { mutableStateOf(false) }
        var addInfiniteSizeAnimationRow by remember { mutableStateOf(false) }
        var addInfiniteSizeAnimationColumn by remember { mutableStateOf(false) }
        var showMainPanel by remember { mutableStateOf(false) }
        var numberOfRows by remember { mutableIntStateOf(1) }
        var numberOfColumns by remember { mutableIntStateOf(1) }
        var numberOfSubspaces by remember { mutableIntStateOf(1) }
        var showPanelsInColumn by remember { mutableStateOf(false) }
        var showPanelsInRow by remember { mutableStateOf(false) }
        var numberOfPanelsInColumn by remember { mutableIntStateOf(1) }
        var numberOfPanelsInRow by remember { mutableIntStateOf(1) }

        var addRotationPanelsInRow by remember { mutableStateOf(false) }
        var addRotationPanelsInColumn by remember { mutableStateOf(false) }
        var addSizeAnimationPanelsInRow by remember { mutableStateOf(false) }
        var addSizeAnimationPanelsInColumn by remember { mutableStateOf(false) }
        var addInfiniteSizeAnimationPanelsInRow by remember { mutableStateOf(false) }
        var addInfiniteSizeAnimationPanelsInColumn by remember { mutableStateOf(false) }

        val darkBackground = Color(0xFF2C2C2C)
        val accentColor = Color(0xFF03DAC6)
        val textColor = Color.White
        val controlWidth = 825.dp
        val controlHeight = 1450.dp

        val animatedScaleRow = animateSingleShotScale(addSizeAnimationRow)
        val animatedScaleColumn = animateSingleShotScale(addSizeAnimationColumn)
        val animatedScalePanelsRow = animateSingleShotScale(addSizeAnimationPanelsInRow)
        val animatedScalePanelsColumn = animateSingleShotScale(addSizeAnimationPanelsInColumn)

        val animatedInfiniteScaleRow = animateInfiniteScale(1.0f, 2.0f)
        val animatedInfiniteScaleColumn = animateInfiniteScale(1.0f, 2.0f)
        val animatedInfiniteScalePanelsRow = animateInfiniteScale(1.0f, 2.0f)
        val animatedInfiniteScalePanelsColumn = animateInfiniteScale(1.0f, 2.0f)
        val (rotation, axisAngle) = initializePanelRotationData()

        SpatialRow(
            modifier = SubspaceModifier.width(6000.dp).height(6000.dp),
            verticalAlignment = SpatialAlignment.CenterVertically,
            horizontalArrangement = SpatialArrangement.SpaceAround,
        ) {
            SpatialPanel(modifier = SubspaceModifier.width(controlWidth).height(controlHeight)) {
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(darkBackground, shape = RoundedCornerShape(16.dp))
                            .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Layout Performance Controls",
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ControlGroup(
                                title = "SpatialRow Factory",
                                count = numberOfRows,
                                onIncrease = { numberOfRows++ },
                                onDecrease = { if (numberOfRows > 1) numberOfRows-- },
                                onReset = { numberOfRows = 1 },
                                onToggle = { showRow = !showRow },
                                isToggled = showRow,
                                toggleText = if (showRow) "Hide Rows" else "Show Rows",
                                accentColor = accentColor,
                                isRotationToggled = addRotationRow,
                                onRotationToggle = { addRotationRow = !addRotationRow },
                                isAnimationToggled = addSizeAnimationRow,
                                onAnimationToggle = { addSizeAnimationRow = !addSizeAnimationRow },
                                isInfiniteAnimationToggled = addInfiniteSizeAnimationRow,
                                onInfiniteAnimationToggle = {
                                    addInfiniteSizeAnimationRow = !addInfiniteSizeAnimationRow
                                },
                            )

                            ControlGroup(
                                title = "SpatialColumn Factory",
                                count = numberOfColumns,
                                onIncrease = { numberOfColumns++ },
                                onDecrease = { if (numberOfColumns > 1) numberOfColumns-- },
                                onReset = { numberOfColumns = 1 },
                                onToggle = { showColumn = !showColumn },
                                isToggled = showColumn,
                                toggleText = if (showColumn) "Hide Columns" else "Show Columns",
                                accentColor = accentColor,
                                isRotationToggled = addRotationColumn,
                                onRotationToggle = { addRotationColumn = !addRotationColumn },
                                isAnimationToggled = addSizeAnimationColumn,
                                onAnimationToggle = {
                                    addSizeAnimationColumn = !addSizeAnimationColumn
                                },
                                isInfiniteAnimationToggled = addInfiniteSizeAnimationColumn,
                                onInfiniteAnimationToggle = {
                                    addInfiniteSizeAnimationColumn = !addInfiniteSizeAnimationColumn
                                },
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            ControlGroup(
                                title = "Panels in Column Factory",
                                count = numberOfPanelsInColumn,
                                onIncrease = { numberOfPanelsInColumn++ },
                                onDecrease = {
                                    if (numberOfPanelsInColumn > 1) numberOfPanelsInColumn--
                                },
                                onReset = { numberOfPanelsInColumn = 1 },
                                onToggle = { showPanelsInColumn = !showPanelsInColumn },
                                isToggled = showPanelsInColumn,
                                toggleText =
                                    if (showPanelsInColumn) {
                                        "Hide Panels (Column)"
                                    } else {
                                        "Show Panels (Column)"
                                    },
                                accentColor = Color(0xFFADD8E6),
                                isRotationToggled = addRotationPanelsInColumn,
                                onRotationToggle = {
                                    addRotationPanelsInColumn = !addRotationPanelsInColumn
                                },
                                isAnimationToggled = addSizeAnimationPanelsInColumn,
                                onAnimationToggle = {
                                    addSizeAnimationPanelsInColumn = !addSizeAnimationPanelsInColumn
                                },
                                isInfiniteAnimationToggled = addInfiniteSizeAnimationPanelsInColumn,
                                onInfiniteAnimationToggle = {
                                    addInfiniteSizeAnimationPanelsInColumn =
                                        !addInfiniteSizeAnimationPanelsInColumn
                                },
                            )

                            ControlGroup(
                                title = "Panels in Row Factory",
                                count = numberOfPanelsInRow,
                                onIncrease = { numberOfPanelsInRow++ },
                                onDecrease = { if (numberOfPanelsInRow > 1) numberOfPanelsInRow-- },
                                onReset = { numberOfPanelsInRow = 1 },
                                onToggle = { showPanelsInRow = !showPanelsInRow },
                                isToggled = showPanelsInRow,
                                toggleText =
                                    if (showPanelsInRow) {
                                        "Hide Panels (Row)"
                                    } else {
                                        "Show Panels (Row)"
                                    },
                                accentColor = Color(0xFFADD8E6),
                                isRotationToggled = addRotationPanelsInRow,
                                onRotationToggle = {
                                    addRotationPanelsInRow = !addRotationPanelsInRow
                                },
                                isAnimationToggled = addSizeAnimationPanelsInRow,
                                onAnimationToggle = {
                                    addSizeAnimationPanelsInRow = !addSizeAnimationPanelsInRow
                                },
                                isInfiniteAnimationToggled = addInfiniteSizeAnimationPanelsInRow,
                                onInfiniteAnimationToggle = {
                                    addInfiniteSizeAnimationPanelsInRow =
                                        !addInfiniteSizeAnimationPanelsInRow
                                },
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            SpatialMainPanelControl(
                                showMainPanel = showMainPanel,
                                onToggle = { showMainPanel = !showMainPanel },
                                accentColor = Color(0xFF64B5F6),
                            )

                            ControlGroup(
                                title = "Subspace Factory",
                                count = numberOfSubspaces,
                                onIncrease = { numberOfSubspaces++ },
                                onDecrease = { if (numberOfSubspaces > 1) numberOfSubspaces-- },
                                onReset = { numberOfSubspaces = 1 },
                                onToggle = { showSubspace = !showSubspace },
                                isToggled = showSubspace,
                                toggleText =
                                    if (showSubspace) {
                                        "Hide Subspaces"
                                    } else {
                                        "Show Subspaces"
                                    },
                                accentColor = accentColor,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    FpsCounterScreen()
                }
            }

            if (showRow) {
                var modifier: SubspaceModifier = SubspaceModifier.width(100.dp).height(100.dp)

                if (addRotationRow) {
                    modifier = modifier.rotate(rotation = rotation, axisAngle = axisAngle)
                }

                val scaleFactor =
                    when {
                        addInfiniteSizeAnimationRow -> animatedInfiniteScaleRow
                        addSizeAnimationRow -> animatedScaleRow
                        else -> 1.0f
                    }

                if (scaleFactor != 1.0f) {
                    modifier = modifier.scale(scaleFactor)
                }

                SpatialRowFactory(numberOfRows, modifier)
            }
            if (showColumn) {
                var modifier: SubspaceModifier = SubspaceModifier.width(100.dp).height(100.dp)

                if (addRotationColumn) {
                    modifier = modifier.rotate(rotation = rotation, axisAngle = axisAngle)
                }

                val scaleFactor =
                    when {
                        addInfiniteSizeAnimationColumn -> animatedInfiniteScaleColumn
                        addSizeAnimationColumn -> animatedScaleColumn
                        else -> 1.0f
                    }

                if (scaleFactor != 1.0f) {
                    modifier = modifier.scale(scaleFactor)
                }

                SpatialColumn { SpatialColumnFactory(numberOfColumns, modifier) }
            }

            if (showSubspace) {
                SpatialColumn { SubspaceFactory(numberOfSubspaces) }
            }
            if (showMainPanel) {
                SpatialMainPanel(modifier = SubspaceModifier.height(600.dp).width(600.dp))
            }
            if (showPanelsInColumn) {
                var modifierPanelsColumn: SubspaceModifier =
                    SubspaceModifier.width(100.dp).height(100.dp)

                if (addRotationPanelsInColumn) {
                    modifierPanelsColumn =
                        modifierPanelsColumn.rotate(rotation = rotation, axisAngle = axisAngle)
                }

                val scaleFactor =
                    when {
                        addInfiniteSizeAnimationPanelsInColumn -> animatedInfiniteScalePanelsColumn
                        addSizeAnimationPanelsInColumn -> animatedScalePanelsColumn
                        else -> 1.0f
                    }

                if (scaleFactor != 1.0f) {
                    modifierPanelsColumn = modifierPanelsColumn.scale(scaleFactor)
                }

                SpatialPanelsInSpatialColumnFactory(numberOfPanelsInColumn, modifierPanelsColumn)
            }
            if (showPanelsInRow) {
                var modifierPanelsRow: SubspaceModifier =
                    SubspaceModifier.width(100.dp).height(100.dp)

                if (addRotationPanelsInRow) {
                    modifierPanelsRow =
                        modifierPanelsRow.rotate(rotation = rotation, axisAngle = axisAngle)
                }

                val scaleFactor =
                    when {
                        addInfiniteSizeAnimationPanelsInRow -> animatedInfiniteScalePanelsRow
                        addSizeAnimationPanelsInRow -> animatedScalePanelsRow
                        else -> 1.0f
                    }

                if (scaleFactor != 1.0f) {
                    modifierPanelsRow = modifierPanelsRow.scale(scaleFactor)
                }

                SpatialPanelsInSpatialRowFactory(numberOfPanelsInRow, modifierPanelsRow)
            }
        }
    }

    @Composable
    private fun ControlGroup(
        title: String,
        count: Int,
        onIncrease: () -> Unit,
        onDecrease: () -> Unit,
        onReset: () -> Unit,
        onToggle: () -> Unit,
        isToggled: Boolean,
        toggleText: String,
        accentColor: Color,
        isRotationToggled: Boolean? = null,
        onRotationToggle: (() -> Unit)? = null,
        isAnimationToggled: Boolean? = null,
        onAnimationToggle: (() -> Unit)? = null,
        isInfiniteAnimationToggled: Boolean? = null,
        onInfiniteAnimationToggle: (() -> Unit)? = null,
    ) {
        val textColor = Color.White
        val buttonColor = if (isToggled) Color(0xFFCF6679) else accentColor

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.background(Color(0xFF383838), RoundedCornerShape(8.dp)).padding(16.dp),
        ) {
            Text(
                title,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                "Count: $count",
                color = textColor.copy(alpha = 0.8f),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onDecrease,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Decrease", color = Color.Black)
                }

                Button(
                    onClick = onIncrease,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBB86FC)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Increase", color = Color.Black)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onReset,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFCC80).copy(alpha = 0.8f)
                    ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(200.dp),
            ) {
                Text("Reset Count to 1", color = Color.Black)
            }

            Spacer(Modifier.height(12.dp))

            if (
                isRotationToggled != null ||
                    isAnimationToggled != null ||
                    isInfiniteAnimationToggled != null
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                ) {
                    if (isRotationToggled != null && onRotationToggle != null) {
                        val rotationButtonColor =
                            if (isRotationToggled) {
                                Color(0xFFFFCC80)
                            } else {
                                Color(0xFFBB86FC).copy(alpha = 0.7f)
                            }

                        Button(
                            onClick = onRotationToggle,
                            colors =
                                ButtonDefaults.buttonColors(containerColor = rotationButtonColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isRotationToggled) "Rotation: ON" else "Rotation: OFF",
                                color = Color.Black,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    if (isAnimationToggled != null && onAnimationToggle != null) {
                        val animationButtonColor =
                            if (isAnimationToggled) {
                                Color(0xFF81C784)
                            } else {
                                Color(0xFFBB86FC).copy(alpha = 0.7f)
                            }

                        Button(
                            onClick = onAnimationToggle,
                            colors =
                                ButtonDefaults.buttonColors(containerColor = animationButtonColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isAnimationToggled) {
                                    "Animate: ON (Single)"
                                } else {
                                    "Animate: OFF (Single)"
                                },
                                color = Color.Black,
                                fontSize = 14.sp,
                            )
                        }
                    }

                    if (isInfiniteAnimationToggled != null && onInfiniteAnimationToggle != null) {
                        val infiniteAnimationButtonColor =
                            if (isInfiniteAnimationToggled) {
                                Color(0xFF64B5F6)
                            } else {
                                Color(0xFFBB86FC).copy(alpha = 0.7f)
                            }

                        Button(
                            onClick = onInfiniteAnimationToggle,
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = infiniteAnimationButtonColor
                                ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (isInfiniteAnimationToggled) {
                                    "Animate: ON (Infinite)"
                                } else {
                                    "Animate: OFF (Infinite)"
                                },
                                color = Color.Black,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(200.dp),
            ) {
                Text(toggleText, color = Color.Black)
            }
        }
    }

    @Composable
    private fun SpatialColumnFactory(numberOfColumns: Int, modifier: SubspaceModifier) {
        if (numberOfColumns <= 0) return
        for (i in 1..numberOfColumns) {
            SpatialColumn {
                SpatialPanel(modifier = modifier) {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color(0xFF4DB6AC), shape = RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Col $i",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SpatialMainPanelControl(
        showMainPanel: Boolean,
        onToggle: () -> Unit,
        accentColor: Color,
    ) {
        val textColor = Color.White
        val buttonColor = if (showMainPanel) Color(0xFFCF6679) else accentColor

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier =
                Modifier.background(Color(0xFF383838), RoundedCornerShape(8.dp)).padding(16.dp),
        ) {
            Text(
                "SpatialMainPanel",
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Text(
                "Status: Singleton",
                color = textColor.copy(alpha = 0.8f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onToggle,
                colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.width(200.dp),
            ) {
                Text(if (showMainPanel) "Hide MainPanel" else "Show MainPanel", color = Color.Black)
            }
        }
    }

    @Composable
    private fun SpatialRowFactory(numberOfRows: Int, modifier: SubspaceModifier) {
        if (numberOfRows <= 0) return
        for (i in 1..numberOfRows) {
            SpatialRow {
                SpatialPanel(modifier = modifier) {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color(0xFFFFB74D), shape = RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Row $i",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SubspaceFactory(numberOfSubspaces: Int) {
        if (numberOfSubspaces <= 0) return
        for (i in 1..numberOfSubspaces) {
            Subspace {
                SpatialPanel(modifier = SubspaceModifier.width(100.dp).height(100.dp)) {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(Color(0xFF9575CD), shape = RoundedCornerShape(8.dp)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Sub $i",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SpatialPanelsInSpatialRowFactory(
        numberOfPanels: Int,
        panelModifier: SubspaceModifier,
    ) {
        if (numberOfPanels <= 0) return
        SpatialRow {
            for (i in 1..numberOfPanels) {
                SpatialPanel(modifier = panelModifier) {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(
                                    color = Color(0xFF9575CD),
                                    shape = RoundedCornerShape(8.dp),
                                ),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Panel $i",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SpatialPanelsInSpatialColumnFactory(
        numberOfPanels: Int,
        panelModifier: SubspaceModifier,
    ) {
        if (numberOfPanels <= 0) return
        SpatialColumn {
            for (i in 1..numberOfPanels) {
                SpatialPanel(modifier = panelModifier) {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .background(
                                    color = Color(0xFF9575CD),
                                    shape = RoundedCornerShape(8.dp),
                                ),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Panel $i",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun animateSingleShotScale(isToggled: Boolean): Float {
        val targetDp = if (isToggled) 150.dp else 100.dp

        val animatedDp: Dp by
            animateDpAsState(
                targetValue = targetDp,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                label = "AnimatedScaleDp",
            )

        return animatedDp.value / 100f
    }

    @Composable
    fun animateInfiniteScale(
        minScale: Float = 1.0f,
        maxScale: Float = 2.0f,
        durationMillis: Int = 3000,
    ): Float {
        val infiniteTransition = rememberInfiniteTransition(label = "InfiniteScaleAnimation")

        val scaleAnimationSpec =
            infiniteRepeatable<Float>(
                animation = tween(durationMillis, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            )

        val animatedScale by
            infiniteTransition.animateFloat(
                initialValue = minScale,
                targetValue = maxScale,
                animationSpec = scaleAnimationSpec,
                label = "AnimatedScale",
            )

        return animatedScale
    }
}
