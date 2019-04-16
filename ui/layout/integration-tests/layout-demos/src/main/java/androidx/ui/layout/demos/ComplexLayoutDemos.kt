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

package androidx.ui.layout.demos

import android.os.Handler
import androidx.ui.core.ComplexLayout
import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dp
import androidx.ui.core.Draw
import androidx.ui.core.IntPx
import androidx.ui.core.IntPxSize
import androidx.ui.core.Layout
import androidx.ui.core.WithConstraints
import androidx.ui.core.constrain
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.max
import androidx.ui.core.toRect
import androidx.ui.layout.Align
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.ConstrainedBox
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.FlexColumn
import androidx.ui.layout.FlexRow
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.Model
import com.google.r4a.composer
import com.google.r4a.effectOf
import com.google.r4a.memo
import com.google.r4a.onCommit
import com.google.r4a.unaryPlus

/**
 * Draws a rectangle of a specified dimension, or to its max incoming constraints if
 * dimensions are not specified.
 */
@Composable
fun SizedRectangle(color: Color, width: Dp? = null, height: Dp? = null) {
    <Layout children={ <DrawRectangle color /> }> _, constraints ->
        val widthPx = width?.toIntPx() ?: constraints.maxWidth
        val heightPx = height?.toIntPx() ?: constraints.maxHeight
        layout(widthPx, heightPx) {}
    </Layout>
}

/**
 * A widget that forces its only child to be as wide as its min intrinsic width.
 */
@Composable
fun IntrinsicWidth(@Children() children: () -> Unit) {
    <ComplexLayout
        layoutBlock = { measurables, constraints ->
            // Force child be as wide as its min intrinsic width.
            val width = measurables.first().minIntrinsicWidth(constraints.minHeight)
            val childConstraints = Constraints(
                width,
                width,
                constraints.minHeight,
                constraints.maxHeight
            )
            val childPlaceable = measurables.first().measure(childConstraints)
            layoutResult(childPlaceable.width, childPlaceable.height) {
                childPlaceable.place(IntPx.Zero, IntPx.Zero)
            }
        }
        minIntrinsicWidthBlock = { measurables, h ->
            measurables.first().minIntrinsicWidth(h)
        }
        maxIntrinsicWidthBlock = { measurables, h ->
            measurables.first().minIntrinsicWidth(h)
        }
        minIntrinsicHeightBlock = { measurables, w ->
            measurables.first().minIntrinsicHeight(w)
        }
        maxIntrinsicHeightBlock = { measurables, w ->
            measurables.first().maxIntrinsicHeight(w)
        }
        children />
}

/**
 * Draws an rectangle of fixed (80.dp, 80.dp) size, while providing intrinsic dimensions as well.
 */
@Composable
fun RectangleWithIntrinsics(color: Color) {
    <ComplexLayout
        layoutBlock = { _, _ ->
            layoutResult(80.ipx, 80.ipx) {}
        }
        minIntrinsicWidthBlock = { _, _ -> 30.ipx }
        maxIntrinsicWidthBlock = { _, _ -> 150.ipx }
        minIntrinsicHeightBlock = { _, _ -> 30.ipx }
        maxIntrinsicHeightBlock = { _, _ -> 150.ipx }>
        <DrawRectangle color />
    </ComplexLayout>
}

@Composable
fun FlexRowUsage() {
    <FlexRow>
        expanded(flex=2f) {
            <Center>
                <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
            </Center>
            <SizedRectangle color=Color(0xFF0000FF.toInt()) height=40.dp />
        }
        inflexible {
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp />
            <SizedRectangle color=Color(0xFF00FF00.toInt()) width=50.dp />
            <SizedRectangle color=Color(0xFF0000FF.toInt()) width=60.dp />
        }
        expanded(flex=1f) {
            <SizedRectangle color=Color(0xFF00FF00.toInt()) />
        }
    </FlexRow>
}

@Composable
fun FlexColumnUsage() {
    <FlexColumn>
        expanded(flex=2f) {
            <Center>
                <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
            </Center>
            <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp />
        }
        inflexible {
            <SizedRectangle color=Color(0xFFFF0000.toInt()) height=40.dp />
            <SizedRectangle color=Color(0xFF00FF00.toInt()) height=50.dp />
            <SizedRectangle color=Color(0xFF0000FF.toInt()) height=60.dp />
        }
        expanded(flex=1f) {
            <SizedRectangle color=Color(0xFF00FF00.toInt()) />
        }
    </FlexColumn>
}

@Composable
fun RowUsage() {
    <Row>
        <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
        <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp height=80.dp />
        <SizedRectangle color=Color(0xFF00FF00.toInt()) width=80.dp height=70.dp />
    </Row>
}

@Composable
fun ColumnUsage() {
    <Column>
        <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
        <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp height=80.dp />
        <SizedRectangle color=Color(0xFF00FF00.toInt()) width=80.dp height=70.dp />
    </Column>
}

@Composable
fun AlignUsage() {
    <Align alignment=Alignment.BottomRight>
        <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
    </Align>
}

@Composable
fun StackUsage() {
    <Stack defaultAlignment=Alignment.BottomRight>
        aligned(Alignment.Center) {
            <SizedRectangle color=Color(0xFF0000FF.toInt()) width=300.dp height=300.dp />
        }
        aligned(Alignment.TopLeft) {
            <SizedRectangle color=Color(0xFF00FF00.toInt()) width=150.dp height=150.dp />
        }
        aligned(Alignment.BottomRight) {
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=150.dp height=150.dp />
        }
        // TODO(popam): insets should be named arguments
        positioned(null, 20.dp, null, 20.dp) {
            <SizedRectangle color=Color(0xFFFFA500.toInt()) width=80.dp />
            <SizedRectangle color=Color(0xFFA52A2A.toInt()) width=20.dp />
        }
        positioned(40.dp, null, null, null) {
            <SizedRectangle color=Color(0xFFB22222.toInt()) width=20.dp />
        }
        positioned(null, null, 40.dp, null) {
            <SizedRectangle color=Color(0xFFFFFF00) width=40.dp />
        }
    </Stack>
}

@Composable
fun ConstrainedBoxUsage() {
    <Align alignment=Alignment.Center>
        <ConstrainedBox constraints=DpConstraints.tightConstraints(50.dp, 50.dp)>
            <SizedRectangle color=Color(0xFFFF0000) />
        </ConstrainedBox>
    </Align>
}

fun PaddingUsage() {
    <Row>
        <Padding padding=EdgeInsets(20.dp)>
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=20.dp height=20.dp />
        </Padding>
        <Padding padding=EdgeInsets(20.dp)>
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=20.dp height=20.dp />
        </Padding>
    </Row>
}

@Composable
fun SingleCompositionRow(@Children children: () -> Unit) {
    <Layout layoutBlock = { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minWidth = 0.ipx, maxWidth = IntPx.Infinity))
        }
        val width = placeables.fold(0.ipx) { sum, placeable -> sum + placeable.width }
        val height = placeables.fold(0.ipx) { max, placeable -> max(max, placeable.height) }

        layout(width, height) {
            var left = 0.ipx
            placeables.forEach { placeable ->
                placeable.place(left, 0.ipx)
                left += placeable.width
            }
        }
    } children />
}

@Composable
fun SingleCompositionColumn(@Children children: () -> Unit) {
    <Layout layoutBlock = { measurables, constraints ->
        val placeables = measurables.map {
            it.measure(constraints.copy(minHeight = 0.ipx, maxHeight = IntPx.Infinity))
        }
        val width = placeables.fold(0.ipx) { max, placeable -> max(max, placeable.width) }
        val height = placeables.fold(0.ipx) { sum, placeable -> sum + placeable.height }

        layout(width, height) {
            var top = 0.ipx
            placeables.forEach { placeable ->
                placeable.place(0.ipx, top)
                top += placeable.height
            }
        }
    } children />
}

@Composable
fun SingleCompositionRect() {
    val Rectangle = @Composable {
            <Draw> canvas, parentSize ->
                val paint = Paint().apply { this.color = rectColorModel.color }
                canvas.drawRect(parentSize.toRect(), paint)
            </Draw>
        }
    <Layout layoutBlock = { measurables, constraints ->
        val size = constraints.constrain(IntPxSize(30.ipx, 30.ipx))
        layout(size.width, size.height) {}
    } children=Rectangle />
}

@Model
class RectColor(var color: Color = Color(0xFF00FF00.toInt()), var cnt: Int = 4)
val rectColorModel = RectColor()

@Composable
fun SingleCompositionRectWithIntrinsics() {
    val Rectangle = @Composable {
        <Draw> canvas, parentSize ->
            val paint = Paint().apply { this.color = rectColorModel.color }
            canvas.drawRect(parentSize.toRect(), paint)
        </Draw>
    }
    <ComplexLayout
        layoutBlock={ _, constraints ->
            layoutResult(50.ipx, 50.ipx) {}
        }
        minIntrinsicWidthBlock={ _, _ -> 50.ipx }
        maxIntrinsicWidthBlock={ _, _ -> 50.ipx }
        minIntrinsicHeightBlock={ _, _ -> 50.ipx }
        maxIntrinsicHeightBlock={ _, _ -> 50.ipx }
        children=Rectangle />
}

@Composable
fun SingleCompositionRowWithIntrinsics(@Children children: () -> Unit) {
    <ComplexLayout
        layoutBlock={ measurables, constraints ->
            val placeables = measurables.map { measurable ->
                val childWidth = measurable.maxIntrinsicWidth(constraints.maxHeight)
                measurable.measure(Constraints(
                    childWidth, childWidth, 0.ipx, constraints.maxHeight
                ))
            }
            val width = placeables.map { it.width }.sum()
            val height = placeables.map { it.height }.max()

            layoutResult(width, height) {
                var left = 0.ipx
                placeables.forEach { placeable ->
                    placeable.place(left, 0.ipx)
                    left += placeable.width
                }
            }
        }
        minIntrinsicWidthBlock={ measurables, h ->
            measurables.map { it.minIntrinsicWidth(h) }.sum()
        }
        maxIntrinsicWidthBlock={ measurables, h ->
            measurables.map { it.maxIntrinsicWidth(h) }.sum()
        }
        minIntrinsicHeightBlock={ measurables, w ->
            measurables.map { it.minIntrinsicHeight(w) }.max()
        }
        maxIntrinsicHeightBlock={ measurables, w ->
            measurables.map { it.maxIntrinsicHeight(w) }.max()
        }
        children />
}

@Composable
fun FillWithRectangles() {
    <WithConstraints> constraints ->
        val rectangles = (constraints.maxWidth / 50).value
        <Row>
            for (i in 0 until rectangles) {
                <Padding padding=EdgeInsets(1.dp)>
                    <SingleCompositionRect />
                </Padding>
            }
        </Row>
    </WithConstraints>
}

@Composable
fun ComplexLayoutDemos() {
    +runDelayed(3000, 6000, 9000, 12000, 15000) {
        rectColorModel.color = Color(0xFF0000FF.toInt())
        rectColorModel.cnt++
    }
    <CraneWrapper>
        <Center>
            val rectangleWidth = 30.dp * rectColorModel.cnt
            <ConstrainedBox constraints=DpConstraints.tightConstraintsForWidth(rectangleWidth)>
                <FillWithRectangles />
            </ConstrainedBox>
        </Center>
    </CraneWrapper>
}

fun runDelayed(vararg millis: Int, block: () -> Unit) = effectOf<Unit> {
    val handler = +memo { Handler() }
    +onCommit {
        val runnable = object : Runnable {
            override fun run() {
                block()
            }
        }
        millis.forEach { millis ->
            handler.postDelayed(runnable, millis.toLong())
        }
        onDispose {
            handler.removeCallbacks(runnable)
        }
    }
}

fun Collection<IntPx>.sum(): IntPx {
    var result = 0.ipx
    for (item in this) {
        result += item
    }
    return result
}

fun Collection<IntPx>.max(): IntPx {
    var result = 0.ipx
    for (item in this) {
        result = max(result, item)
    }
    return result
}

@Composable
fun DrawRectangle(color: Color) {
    val paint = Paint()
    paint.color = color
    <Draw> canvas, parentSize ->
        canvas.drawRect(parentSize.toRect(), paint)
    </Draw>
}
