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

package androidx.r4a

import androidx.ui.core.CraneWrapper
import androidx.ui.core.adapter.Draw
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.div
import androidx.ui.core.dp
import androidx.ui.core.minus
import androidx.ui.core.plus
import androidx.ui.core.tightConstraints
import androidx.ui.core.times
import androidx.ui.engine.geometry.Rect
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer


@Composable
fun ColoredRect(color: Color) {
    <MeasureBox> constraints, operations ->
        operations.collect {
            val paint = Paint()
            paint.color = color
            <Draw> canvas, parentSize ->
                canvas.drawRect(Rect(0f, 0f, parentSize.width, parentSize.height), paint)
            </Draw>
        }
        operations.layout(constraints.maxWidth, constraints.maxHeight) {
        }
    </MeasureBox>
}

@Composable
fun HeaderFooterLayout(
    header: () -> Unit,
    footer: () -> Unit,
    @Children children: () -> Unit
) {
    <MeasureBox> constraints, measureOperations ->
        val headerMeasurable = measureOperations.collect { <header /> }.first()
        val contentMeasurables = measureOperations.collect { <children /> }
        val footerMeasurable = measureOperations.collect { <footer /> }.first()
        measureOperations.layout(constraints.maxWidth, constraints.maxHeight) {
            val headerPlaceable =
                measureOperations.measure(headerMeasurable, tightConstraints(constraints.maxWidth, 100.dp))
            headerPlaceable.place(0.dp, 0.dp)

            val footerPadding = 50.dp
            val footerPlaceable = measureOperations.measure(footerMeasurable,
                tightConstraints(constraints.maxWidth - footerPadding * 2, 100.dp))
            footerPlaceable.place(footerPadding, constraints.maxHeight - footerPlaceable.height)

            val itemHeight =
                (constraints.maxHeight - headerPlaceable.height - footerPlaceable.height) /
                        contentMeasurables.size
            val itemConstraint = tightConstraints(constraints.maxWidth, itemHeight)
            var top = headerPlaceable.height
            contentMeasurables.map { measureOperations.measure(it, itemConstraint) }.forEach {
                it.place(0.dp, top)
                top += itemHeight
            }
        }
    </MeasureBox>
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
            <ColoredRect color=Color(android.graphics.Color.GREEN)/>
            <ColoredRect color=Color(android.graphics.Color.YELLOW) />
        </HeaderFooterLayout>
    </CraneWrapper>
}