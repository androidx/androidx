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

package androidx.compose.mpp.demo.graphics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LayerOutsets
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Adapted from CompositingStrategyOffscreenLayerOutsets sample (8b9299a94f)
//
// Scenario: an outer Box has alpha < 1 (promoted to offscreen buffer, implicit clip to bounds).
// Its child draws a Rect that extends beyond the outer box via drawBehind.
// Without outsets the overflow is clipped. With outsets it is visible.

private val LayerBoxSize = 100.dp
private val OutsetsAmount = 80.dp  // how far the child rect overflows

@Composable
fun GraphicsLayerOutsets() {
    var alpha by remember { mutableFloatStateOf(0.5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "alpha < 1 promotes the layer to an offscreen buffer, implicitly clipping it to its " +
            "bounds. The child draws a red rect (via drawBehind) that starts at the layer's " +
            "bottom-right corner and extends ${OutsetsAmount} beyond.\n\n" +
            "LayerOutsets expand the offscreen buffer so that overflow remains visible.",
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(16.dp))

        SliderSetting("Alpha", alpha, 0f..1f) { alpha = it }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Without outsets", fontSize = 12.sp, color = Color.DarkGray)
                Spacer(Modifier.height(8.dp))
                OutsetsDemo(alpha = alpha, outsets = LayerOutsets.Zero)
            }

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("With outsets (${OutsetsAmount})", fontSize = 12.sp, color = Color.DarkGray)
                Spacer(Modifier.height(8.dp))
                OutsetsDemo(alpha = alpha, outsets = LayerOutsets(OutsetsAmount))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "• Gray area = scene background\n" +
            "• Blue border = layer layout bounds (${LayerBoxSize} × ${LayerBoxSize})\n" +
            "• Red rect = child content drawn ${OutsetsAmount} beyond the layer's corner\n" +
            "• At alpha = 1: both panels look identical (no offscreen buffer, no clipping)\n" +
            "• At alpha < 1: left panel clips the red rect; right panel keeps it visible",
            fontSize = 12.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
private fun OutsetsDemo(alpha: Float, outsets: LayerOutsets) {
    val sceneSize = LayerBoxSize + OutsetsAmount + 8.dp
    // Gray background large enough to show the overflow region
    Box(
        modifier = Modifier
            .size(sceneSize)
            .background(Color(0xFFBDBDBD))
    ) {
        // The layer: alpha < 1 here promotes to an offscreen buffer, clipping to LayerBoxSize
        Box(
            modifier = Modifier
                .size(LayerBoxSize)
                .graphicsLayer(alpha = alpha, outsets = outsets)
                .background(Color.White)
        ) {
            // Child fills the layer and draws a red rect that extends beyond it via drawBehind
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Rect starts at the bottom-right corner of this child and overflows out
                        drawRect(
                            topLeft = Offset(size.width, size.height),
                            brush = SolidColor(Color.Red),
                            size = Size(OutsetsAmount.toPx(), OutsetsAmount.toPx())
                        )
                    }
            )
        }

        // Blue border overlay always visible, marks the original layer bounds
        Box(
            modifier = Modifier
                .size(LayerBoxSize)
                .border(2.dp, Color.Blue)
        )
    }
}
