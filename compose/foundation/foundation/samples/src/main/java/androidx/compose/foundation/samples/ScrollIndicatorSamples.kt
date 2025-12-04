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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.ScrollIndicatorFactory
import androidx.compose.foundation.ScrollIndicatorState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.scrollIndicator
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun VisualScrollbarSample() {
    // A basic implementation of ScrollIndicator that draws a simple visual scrollbar.
    data class BasicVisualScrollbarFactory(
        val thumbThickness: Dp = 8.dp,
        val padding: Dp = 4.dp,
        val thumbColor: Color = Color.Gray,
        val thumbAlpha: Float = 0.5f,
    ) : ScrollIndicatorFactory {
        // The node is the core of the ScrollIndicator, handling the drawing logic.
        override fun createNode(
            state: ScrollIndicatorState,
            orientation: Orientation,
        ): DelegatableNode {
            return object : Modifier.Node(), DrawModifierNode {
                override fun ContentDrawScope.draw() {
                    // Draw the original content.
                    drawContent()

                    // Don't draw the scrollbar if the content fits within the viewport.
                    if (state.contentSize <= state.viewportSize) return

                    val visibleContentRatio = state.viewportSize.toFloat() / state.contentSize

                    // Calculate the thumb's size and position along the scrolling axis.
                    val thumbLength = state.viewportSize * visibleContentRatio
                    val thumbPosition = state.scrollOffset * visibleContentRatio

                    val thumbThicknessPx = thumbThickness.toPx()
                    val paddingPx = padding.toPx()

                    // Determine the scrollbar size and thumb position based on the orientation.
                    val (topLeft, size) =
                        when (orientation) {
                            Orientation.Vertical -> {
                                val x = size.width - thumbThicknessPx - paddingPx
                                Offset(x, thumbPosition) to Size(thumbThicknessPx, thumbLength)
                            }
                            Orientation.Horizontal -> {
                                val y = size.height - thumbThicknessPx - paddingPx
                                Offset(thumbPosition, y) to Size(thumbLength, thumbThicknessPx)
                            }
                        }

                    // Draw the scrollbar thumb.
                    drawRect(color = thumbColor, topLeft = topLeft, size = size, alpha = thumbAlpha)
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    val scrollbarFactory = remember { BasicVisualScrollbarFactory() }

    val scrollbarModifier =
        scrollState.scrollIndicatorState?.let {
            Modifier.scrollIndicator(
                factory = scrollbarFactory,
                state = it,
                orientation = Orientation.Vertical,
            )
        } ?: Modifier

    Column(modifier = Modifier.fillMaxSize().then(scrollbarModifier).verticalScroll(scrollState)) {
        repeat(50) { Text(text = "Item ${it + 1}", modifier = Modifier.padding(8.dp)) }
    }
}
