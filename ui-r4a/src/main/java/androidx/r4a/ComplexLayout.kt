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

package androidx.r4a

import androidx.ui.core.Constraints
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Dimension
import androidx.ui.core.adapter.ComplexMeasureBox
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.compareTo
import androidx.ui.core.dp
import androidx.ui.core.plus
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.Recompose

@Composable
fun ComplexRectangle(color: Color) {
    <ComplexMeasureBox> measureOperations ->
        measureOperations.collect {
            <DrawRectangle color />
        }
        measureOperations.layout { constraints, _, _, layoutResult ->
            layoutResult(constraints.maxWidth, constraints.maxHeight) {}
        }
        measureOperations.minIntrinsicWidth { _, _ -> 20.dp }
        measureOperations.maxIntrinsicWidth { _, _ -> 20.dp }
        measureOperations.minIntrinsicHeight { _, _ -> 20.dp }
        measureOperations.maxIntrinsicHeight { _, _ -> 20.dp }
    </ComplexMeasureBox>
}

@Composable
fun SimpleRectangle(color: Color) {
    <MeasureBox> constraints, measureOperations ->
        measureOperations.collect {
            <DrawRectangle color />
        }
        measureOperations.layout(constraints.maxWidth, constraints.maxHeight) {}
    </MeasureBox>
}

@Composable
fun IntrinsicWidth(@Children() children: () -> Unit) {
    <ComplexMeasureBox> measureOperations ->
        val child = measureOperations.collect(children).first()

        measureOperations.layout { constraints, measure, intrinsics, layoutResult ->
            // Force child be as wide as its min intrinsic width.
            val width = intrinsics.minIntrinsicWidth(child, constraints.minHeight)
            val childConstraints = Constraints(
                width,
                width,
                constraints.minHeight,
                constraints.maxHeight
            )
            val childPlaceable = measure(child, childConstraints)
            layoutResult(childPlaceable.width, childPlaceable.height) {
                childPlaceable.place(0.dp, 0.dp)
            }
        }

        measureOperations.minIntrinsicWidth { h, intrinsics ->
            intrinsics.minIntrinsicWidth(child, h)
        }
        measureOperations.maxIntrinsicWidth { h, intrinsics ->
            intrinsics.minIntrinsicWidth(child, h)
        }
        measureOperations.minIntrinsicHeight { w, intrinsics ->
            intrinsics.minIntrinsicHeight(child, w)
        }
        measureOperations.maxIntrinsicHeight { w, intrinsics ->
            intrinsics.maxIntrinsicHeight(child, w)
        }
    </ComplexMeasureBox>
}

@Composable
fun ComplexLayout() {
    <CraneWrapper>
        <Recompose> recompose ->
            <Wrapper>
                <HorizontalLinearLayout>
                    <RectangleWithIntrinsics color=Color(0xFFFF0000.toInt()) />
                    <RectangleWithIntrinsics color=Color(0xFF00FF00.toInt()) />
                    <RectangleWithIntrinsics color=Color(0xFF0000FF.toInt()) />
                </HorizontalLinearLayout>
            </Wrapper>
        </Recompose>
    </CraneWrapper>
}

@Composable
fun Wrapper(@Children() children: () -> Unit) {
    <ComplexMeasureBox> measureOperations ->
        val child = measureOperations.collect(children).first()
        measureOperations.layout { constraints, measure, intrinsics, layoutResult ->
            // Check the default intrinsic methods used by MeasureBoxes.
            // TODO(popam): make this a proper test instead
            require(intrinsics.minIntrinsicWidth(child, Float.POSITIVE_INFINITY.dp) == 90.dp)
            require(intrinsics.maxIntrinsicWidth(child, Float.POSITIVE_INFINITY.dp) == 450.dp)
            require(intrinsics.minIntrinsicHeight(child, Float.POSITIVE_INFINITY.dp) == 30.dp)
            require(intrinsics.maxIntrinsicHeight(child, Float.POSITIVE_INFINITY.dp) == 150.dp)
            val placeable = measure(child, constraints)
            layoutResult(placeable.width, placeable.height) {
                placeable.place(0.dp, 0.dp)
            }
        }
    </ComplexMeasureBox>
}

fun List<Dimension>.max(): Dimension {
    var max = 0.dp
    forEach {
        if (it > max) {
            max = it
        }
    }
    return max
}

@Composable
fun HorizontalLinearLayout(@Children() children: () -> Unit) {
    <MeasureBox> constraints, operations ->
        val measurables = operations.collect(children)
        val childConstraints = Constraints(
            0.dp,
            Float.POSITIVE_INFINITY.dp,
            constraints.minHeight,
            constraints.maxHeight
        )
        val placeables = measurables.map { m -> operations.measure(m, childConstraints) }
        val width = placeables.map { it.width }.fold(0.dp, Dimension::plus)
        val height = placeables.map { it.height }.max()
        operations.layout(width, height) {
            var currentLeft = 0.dp
            placeables.forEach { placeable ->
                placeable.place(currentLeft, 0.dp)
                currentLeft += placeable.width
            }
        }
    </MeasureBox>
}

@Composable
fun RectangleWithIntrinsics(color: Color) {
    <ComplexMeasureBox> measureOperations ->
        measureOperations.collect {
            <DrawRectangle color />
        }
        measureOperations.layout { _, _, _, layoutResult ->
            layoutResult(80.dp, 80.dp) {}
        }
        measureOperations.minIntrinsicWidth { _, _ -> 30.dp }
        measureOperations.maxIntrinsicWidth { _, _ -> 150.dp }
        measureOperations.minIntrinsicHeight { _, _ -> 30.dp }
        measureOperations.maxIntrinsicHeight { _, _ -> 150.dp }
    </ComplexMeasureBox>
}

