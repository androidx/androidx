/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun ScreenCoordinatesDemo(navigateBack: () -> Unit) {
    var lastPointerPositionInScreen: Offset by remember { mutableStateOf(Offset.Unspecified) }
    var pointerDown by remember { mutableStateOf(false) }
    var useMatrixToConvertToScreenCoordinates by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            "This screen is covered by a transparent window (red border). Drag around to have " +
                "that window record pointer events in screen coordinates, which will then be " +
                "converted back to coordinates in this window and drawn here.",
            Modifier.padding(16.dp)
        )

        var coords: LayoutCoordinates? by remember { mutableStateOf(null, neverEqualPolicy()) }

        Canvas(modifier = Modifier.fillMaxSize().onGloballyPositioned { coords = it }) {
            if (lastPointerPositionInScreen.isUnspecified) return@Canvas
            val lastPointerPositionInLocal =
                coords?.screenToLocal(lastPointerPositionInScreen) ?: return@Canvas
            if (pointerDown) {
                drawLine(
                    Color.Black,
                    start = lastPointerPositionInLocal.copy(x = 0f),
                    end = lastPointerPositionInLocal.copy(x = size.width)
                )
                drawLine(
                    Color.Black,
                    start = lastPointerPositionInLocal.copy(y = 0f),
                    end = lastPointerPositionInLocal.copy(y = size.height)
                )
            }
        }
    }

    var popupOffset by remember { mutableStateOf(IntOffset(100, 100)) }
    // TODO(b/292257547) Workaround for popup not updating offset when state read from
    //  calculatePosition changes. Remove when fixed.
    @Suppress("UNUSED_EXPRESSION") popupOffset
    Popup(
        popupPositionProvider =
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset = popupOffset
            },
        properties =
            PopupProperties(
                focusable = true,
                clippingEnabled = false,
                dismissOnClickOutside = false,
            ),
        onDismissRequest = navigateBack
    ) {
        var windowCoords: LayoutCoordinates? by remember {
            mutableStateOf(null, neverEqualPolicy())
        }
        var gestureAreaCoords: LayoutCoordinates? by remember {
            mutableStateOf(null, neverEqualPolicy())
        }
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier =
                Modifier.onGloballyPositioned { windowCoords = it }
                    .fitSquare(fraction = 0.9f)
                    .border(2.dp, Color.Red.copy(alpha = 0.5f))
                    // Ensure the gesture area is actually offset from the window.
                    .padding(16.dp)
                    .background(Color.Magenta.copy(alpha = 0.1f))
                    .onGloballyPositioned { gestureAreaCoords = it }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { positionInLocal ->
                                pointerDown = true
                                lastPointerPositionInScreen =
                                    windowCoords?.localToScreen(positionInLocal)
                                        ?: Offset.Unspecified
                            },
                            onDrag = { change, delta ->
                                val positionInLocal = change.position
                                lastPointerPositionInScreen =
                                    if (useMatrixToConvertToScreenCoordinates) {
                                        val matrix = Matrix()
                                        gestureAreaCoords?.transformToScreen(matrix)
                                        matrix.map(positionInLocal)
                                    } else {
                                        gestureAreaCoords?.localToScreen(positionInLocal)
                                            ?: Offset.Unspecified
                                    }
                                popupOffset += delta.round()
                            },
                            onDragEnd = { pointerDown = false },
                            onDragCancel = { pointerDown = false }
                        )
                    }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Use matrix to convert to screen:")
                    Spacer(Modifier.weight(1f, fill = true))
                    Switch(
                        checked = useMatrixToConvertToScreenCoordinates,
                        onCheckedChange = { useMatrixToConvertToScreenCoordinates = it }
                    )
                }
                Spacer(Modifier.weight(1f, fill = true))
                Text(
                    "Red border positionOnScreen: ${windowCoords?.positionOnScreen()}",
                    style = MaterialTheme.typography.body2
                )
                Text(
                    "Gesture area (red bg) positionOnScreen: " +
                        "${gestureAreaCoords?.positionOnScreen()}",
                    style = MaterialTheme.typography.body2
                )
                TextField(value = "Tap to show keyboard", onValueChange = {})
                Button(onClick = navigateBack) { Text("Close") }

                // Hack to get the window offset to update while keyboard is animating.
                val imeOffset = WindowInsets.ime
                LaunchedEffect(imeOffset) {
                    snapshotFlow { imeOffset.getBottom(Density(1f)) }
                        .collect {
                            windowCoords = windowCoords
                            gestureAreaCoords = gestureAreaCoords
                        }
                }
            }
        }
    }
}

private fun Modifier.fitSquare(fraction: Float) = layout { measurable, constraints ->
    val minConstraint = (minOf(constraints.maxWidth, constraints.maxHeight) * fraction).roundToInt()
    val childConstraints = Constraints.fixed(minConstraint, minConstraint)
    val placeable = measurable.measure(childConstraints)
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}
