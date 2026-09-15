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

@file:OptIn(ExperimentalSpatialAnnotationsApi::class)

package androidx.xr.arcore.testapp.helloar.ui

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.xr.arcore.SpatialAnnotationQuadAlignment
import androidx.xr.runtime.ExperimentalSpatialAnnotationsApi

/**
 * Configuration parameters defining tracking mode, dimensions, and dot placement.
 *
 * @property alignment spatial alignment strategy for annotations.
 * @property boxWidth width of the tracking region in sensor pixels.
 * @property boxHeight height of the tracking region in sensor pixels.
 * @property isDotCenter whether tracking dot should be placed at the center.
 * @property isDotTop whether tracking dot should be placed at the top.
 */
internal data class ViewfinderTrackingConfig(
    val alignment: SpatialAnnotationQuadAlignment,
    val boxWidth: Int,
    val boxHeight: Int,
    val isDotCenter: Boolean,
    val isDotTop: Boolean,
)

private object ViewfinderDefaults {
    const val DEFAULT_BOX_WIDTH_PIXELS = 960
    const val DEFAULT_BOX_HEIGHT_PIXELS = 540
    const val MAX_TRACKING_SIZE_PIXELS = 1920
    const val MIN_TRACKING_SIZE_PIXELS = 50
    const val SIZE_STEP_PIXELS = 50
    const val MIN_TEXT_DISPLAY_SIZE_PIXELS = 80

    val BORDER_THICKNESS_NORMAL = 2.dp
    val BORDER_THICKNESS_HOVERED = 4.dp
    val BORDER_THICKNESS_PRESSED = 8.dp

    val ACCENT_COLOR = Color.Green
    val ACCENT_COLOR_PRESSED = ACCENT_COLOR.copy(alpha = 0.3f)
    val ACCENT_COLOR_DOT_PRESSED = ACCENT_COLOR.copy(alpha = 0.5f)
    val OVERLAY_BACKGROUND_COLOR = Color.Black.copy(alpha = 0.5f)
}

/**
 * Root Compose screen displaying camera preview, interactive viewfinder, and tracking controls.
 *
 * @param cameraPreviewUseCase CameraX preview use case bound to the camera lifecycle.
 * @param activeFrameWidth width of the active camera frame in pixels.
 * @param activeFrameHeight height of the active camera frame in pixels.
 * @param onStartTrackingClick callback triggered when the viewfinder box is tapped to start
 *   tracking.
 * @param modifier modifier to be applied to the root container layout.
 */
@Composable
internal fun CameraPreviewScreen(
    cameraPreviewUseCase: Preview,
    activeFrameWidth: Float,
    activeFrameHeight: Float,
    onStartTrackingClick: (config: ViewfinderTrackingConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    var alignmentState by remember { mutableStateOf(SpatialAnnotationQuadAlignment.SCREEN) }
    var boxWidthState by remember { mutableIntStateOf(ViewfinderDefaults.DEFAULT_BOX_WIDTH_PIXELS) }
    var boxHeightState by remember {
        mutableIntStateOf(ViewfinderDefaults.DEFAULT_BOX_HEIGHT_PIXELS)
    }
    var isDotCenterState by remember { mutableStateOf(false) }
    var isDotTopState by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    cameraPreviewUseCase.surfaceProvider = surfaceProvider
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        ViewfinderOverlay(
            activeFrameWidth = activeFrameWidth,
            activeFrameHeight = activeFrameHeight,
            boxWidth = boxWidthState,
            boxHeight = boxHeightState,
            onStartTrackingClick = {
                onStartTrackingClick(
                    ViewfinderTrackingConfig(
                        alignment = alignmentState,
                        boxWidth = boxWidthState,
                        boxHeight = boxHeightState,
                        isDotCenter = isDotCenterState,
                        isDotTop = isDotTopState,
                    )
                )
            },
        )

        TrackingModeSelector(
            alignment = alignmentState,
            onAlignmentChange = { alignmentState = it },
            isDotCenter = isDotCenterState,
            onDotCenterChange = { isDotCenterState = it },
            isDotTop = isDotTopState,
            onDotTopChange = { isDotTopState = it },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        SizeControls(
            boxWidth = boxWidthState,
            onBoxWidthChange = { boxWidthState = it },
            boxHeight = boxHeightState,
            onBoxHeightChange = { boxHeightState = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ViewfinderOverlay(
    activeFrameWidth: Float,
    activeFrameHeight: Float,
    boxWidth: Int,
    boxHeight: Int,
    onStartTrackingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val frameWidth = activeFrameWidth.coerceAtLeast(1f)
        val frameHeight = activeFrameHeight.coerceAtLeast(1f)
        val dpPerPixel = maxOf(maxWidth / frameWidth, maxHeight / frameHeight)
        val boxWidthDp = dpPerPixel * boxWidth.toFloat()
        val boxHeightDp = dpPerPixel * boxHeight.toFloat()
        val boxInteractionSource = remember { MutableInteractionSource() }
        val isBoxHovered by boxInteractionSource.collectIsHoveredAsState()
        val isBoxPressed by boxInteractionSource.collectIsPressedAsState()

        val borderThickness =
            when {
                isBoxPressed -> ViewfinderDefaults.BORDER_THICKNESS_PRESSED
                isBoxHovered -> ViewfinderDefaults.BORDER_THICKNESS_HOVERED
                else -> ViewfinderDefaults.BORDER_THICKNESS_NORMAL
            }
        val boxBackground =
            if (isBoxPressed) ViewfinderDefaults.ACCENT_COLOR_PRESSED else Color.Transparent

        val isDot =
            boxWidth <= ViewfinderDefaults.MIN_TRACKING_SIZE_PIXELS ||
                boxHeight <= ViewfinderDefaults.MIN_TRACKING_SIZE_PIXELS
        if (isDot) {
            val dotSize = minOf(boxWidthDp, boxHeightDp)
            Box(
                modifier =
                    Modifier.size(dotSize)
                        .hoverable(boxInteractionSource)
                        .background(
                            if (isBoxPressed) {
                                ViewfinderDefaults.ACCENT_COLOR_DOT_PRESSED
                            } else {
                                ViewfinderDefaults.ACCENT_COLOR
                            },
                            shape = CircleShape,
                        )
                        .clickable(
                            interactionSource = boxInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Start Tracking",
                            onClick = onStartTrackingClick,
                        )
            )
        } else {
            Box(
                modifier =
                    Modifier.size(width = boxWidthDp, height = boxHeightDp)
                        .hoverable(boxInteractionSource)
                        .background(boxBackground)
                        .border(borderThickness, ViewfinderDefaults.ACCENT_COLOR)
                        .clickable(
                            interactionSource = boxInteractionSource,
                            indication = null,
                            role = Role.Button,
                            onClickLabel = "Start Tracking",
                            onClick = onStartTrackingClick,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                if (
                    boxWidth >= ViewfinderDefaults.MIN_TEXT_DISPLAY_SIZE_PIXELS &&
                        boxHeight >= ViewfinderDefaults.MIN_TEXT_DISPLAY_SIZE_PIXELS
                ) {
                    Text(
                        text = "Start Tracking",
                        color = ViewfinderDefaults.ACCENT_COLOR,
                        fontSize = 10.sp,
                        modifier = Modifier.background(ViewfinderDefaults.OVERLAY_BACKGROUND_COLOR),
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackingModeSelector(
    alignment: SpatialAnnotationQuadAlignment,
    onAlignmentChange: (SpatialAnnotationQuadAlignment) -> Unit,
    isDotCenter: Boolean,
    onDotCenterChange: (Boolean) -> Unit,
    isDotTop: Boolean,
    onDotTopChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val checkboxColors =
        CheckboxDefaults.colors(
            checkedColor = ViewfinderDefaults.ACCENT_COLOR,
            uncheckedColor = Color.White,
            checkmarkColor = Color.Black,
        )
    Column(
        modifier = modifier.background(ViewfinderDefaults.OVERLAY_BACKGROUND_COLOR).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            Text("Tracking Mode", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier =
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        .selectable(
                            selected = alignment == SpatialAnnotationQuadAlignment.OBJECT,
                            role = Role.RadioButton,
                            onClick = { onAlignmentChange(SpatialAnnotationQuadAlignment.OBJECT) },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = alignment == SpatialAnnotationQuadAlignment.OBJECT,
                    onClick = null,
                )
                Text("Object Aligned", color = Color.White, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier =
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        .selectable(
                            selected = alignment == SpatialAnnotationQuadAlignment.SCREEN,
                            role = Role.RadioButton,
                            onClick = { onAlignmentChange(SpatialAnnotationQuadAlignment.SCREEN) },
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = alignment == SpatialAnnotationQuadAlignment.SCREEN,
                    onClick = null,
                )
                Text("Screen Aligned", color = Color.White, fontSize = 14.sp)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier =
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        .toggleable(
                            value = isDotCenter,
                            role = Role.Checkbox,
                            onValueChange = onDotCenterChange,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isDotCenter, onCheckedChange = null, colors = checkboxColors)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Center Dot", color = Color.White, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Row(
                modifier =
                    Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        .toggleable(
                            value = isDotTop,
                            role = Role.Checkbox,
                            onValueChange = onDotTopChange,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isDotTop, onCheckedChange = null, colors = checkboxColors)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Top Dot", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun SizeControls(
    boxWidth: Int,
    onBoxWidthChange: (Int) -> Unit,
    boxHeight: Int,
    onBoxHeightChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(ViewfinderDefaults.OVERLAY_BACKGROUND_COLOR).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DimensionControlRow(label = "Width", value = boxWidth, onValueChange = onBoxWidthChange)
        Spacer(modifier = Modifier.height(8.dp))
        DimensionControlRow(label = "Height", value = boxHeight, onValueChange = onBoxHeightChange)
    }
}

@Composable
private fun DimensionControlRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val decreaseDescription = "Decrease " + label
    val increaseDescription = "Increase " + label

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = {
                onValueChange(
                    (value - ViewfinderDefaults.SIZE_STEP_PIXELS).coerceAtLeast(
                        ViewfinderDefaults.MIN_TRACKING_SIZE_PIXELS
                    )
                )
            },
            modifier = Modifier.semantics { contentDescription = decreaseDescription },
            enabled = value > ViewfinderDefaults.MIN_TRACKING_SIZE_PIXELS,
        ) {
            Text("-")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label + ": " + value, color = Color.White, fontSize = 16.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = {
                onValueChange(
                    (value + ViewfinderDefaults.SIZE_STEP_PIXELS).coerceAtMost(
                        ViewfinderDefaults.MAX_TRACKING_SIZE_PIXELS
                    )
                )
            },
            modifier = Modifier.semantics { contentDescription = increaseDescription },
            enabled = value < ViewfinderDefaults.MAX_TRACKING_SIZE_PIXELS,
        ) {
            Text("+")
        }
    }
}
