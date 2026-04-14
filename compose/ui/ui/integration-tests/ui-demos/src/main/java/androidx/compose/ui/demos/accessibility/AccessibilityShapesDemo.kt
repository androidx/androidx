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

package androidx.compose.ui.demos.accessibility

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

@Composable
fun AccessibilityShapeOffscreenDemo() {
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Use the a11y inspector on each shape to see if the a11y view of the shape" +
                " matches its actual shape. The shadow shapes are for reference only and" +
                " shouldn't show up under the a11y inspector."
        )

        @Composable
        fun RepeatedLayoutRow(clipShape: Shape, color: Color) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(2) { Box(Modifier.requiredSize(128.dp).clip(clipShape).background(color)) }
            }
        }

        @Composable
        fun ShapeRows(modifier: Modifier = Modifier, color: Color = Color.Blue) {
            Column(
                modifier.wrapContentSize(unbounded = true),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RepeatedLayoutRow(RectangleShape, color)
                RepeatedLayoutRow(RoundedCornerShape(32.dp), color)
                RepeatedLayoutRow(TriangleShape, color)
            }
        }

        Box(Modifier.fillMaxSize().wrapContentSize().requiredSize(200.dp, 300.dp)) {
            ShapeRows(Modifier.matchParentSize().border(1.dp, Color.LightGray).clipToBounds())
            ShapeRows(Modifier.clearAndSetSemantics {}, color = Color.Blue.copy(alpha = 0.1f))
        }
    }
}

private object TriangleShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
        Outline.Generic(
            Path().apply {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
        )
}
