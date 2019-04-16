/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("PLUGIN_WARNING")

package androidx.ui.demos

import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.MultiChildLayout
import androidx.ui.core.ipx
import androidx.ui.core.toRect
import androidx.ui.core.vectorgraphics.Brush
import androidx.ui.core.vectorgraphics.SolidColor
import androidx.ui.layout.Container
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

// TODO(malkov): for now it's copypasted ColoredRect from material, remove later
@Composable
fun ColoredRect(brush: Brush, width: Dp? = null, height: Dp? = null) {
    <Container width height>
        <DrawFillRect brush />
    </Container>
}

@Composable
fun ColoredRect(color: Color, width: Dp? = null, height: Dp? = null) {
    <ColoredRect brush=SolidColor(color) width height />
}

@Composable
private fun DrawFillRect(brush: Brush) {
    <Draw> canvas, parentSize ->
        val paint = Paint()
        brush.applyBrush(paint)
        canvas.drawRect(parentSize.toRect(), paint)
    </Draw>
}

@Composable
fun HeaderFooterLayout(
    header: () -> Unit,
    footer: () -> Unit,
    @Children content: () -> Unit
) {
    <MultiChildLayout childrenArray=arrayOf(header, content, footer)> measurables, constraints ->
        val headerPlaceable = measurables[header].first().measure(
            Constraints.tightConstraints(constraints.maxWidth, 100.ipx)
        )
        val footerPadding = 50.ipx
        val footerPlaceable = measurables[footer].first().measure(
            Constraints.tightConstraints(constraints.maxWidth - footerPadding * 2, 100.ipx)
        )
        val itemHeight =
            (constraints.maxHeight - headerPlaceable.height - footerPlaceable.height) /
                    measurables[content].size
        val contentPlaceables = measurables[content].map { measurable ->
            measurable.measure(Constraints.tightConstraints(constraints.maxWidth, itemHeight))
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.place(0.ipx, 0.ipx)
            footerPlaceable.place(footerPadding, constraints.maxHeight - footerPlaceable.height)
            var top = headerPlaceable.height
            contentPlaceables.forEach { placeable ->
                placeable.place(0.ipx, top)
                top += itemHeight
            }
        }
    </MultiChildLayout>
}

@Composable
fun MultipleCollectTest() {
    <CraneWrapper>
        val header = {
            <ColoredRect color=Color(android.graphics.Color.GRAY) />
        }
        val footer = {
            <ColoredRect color=Color(android.graphics.Color.BLUE) />
        }
        <HeaderFooterLayout header footer>
            <ColoredRect color=Color(android.graphics.Color.GREEN) />
            <ColoredRect color=Color(android.graphics.Color.YELLOW) />
        </HeaderFooterLayout>
    </CraneWrapper>
}
