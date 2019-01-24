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
import androidx.ui.core.adapter.Column
import androidx.ui.core.adapter.ComplexMeasureBox
import androidx.ui.core.adapter.Row
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.adapter.Center
import androidx.ui.core.adapter.FlexColumn
import androidx.ui.core.adapter.FlexRow
import androidx.ui.core.dp
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Composable

/**
 * Draws a rectangle of a specified dimension, or to its max incoming constraints if
 * dimensions are not specified.
 */
@Composable
fun SizedRectangle(color: Color, width: Dimension? = null, height: Dimension? = null) {
    <MeasureBox> constraints, measureOperations ->
        measureOperations.collect {
            <DrawRectangle color />
        }
        measureOperations.layout(width ?: constraints.maxWidth, height ?: constraints.maxHeight) {}
    </MeasureBox>
}

/**
 * A widget that forces its only child to be as wide as its min intrinsic width.
 */
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

/**
 * Draws an rectangle of fixed (80.dp, 80.dp) size, while providing intrinsic dimensions as well.
 */
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

@Composable
fun FlexRowUsage() {
    <FlexRow> children ->
        // TODO(popam): named arguments cannot be used because of the adapter hack
        children.expanded(/*flex=*/2f) {
            <Center>
                <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
            </Center>
            <SizedRectangle color=Color(0xFF0000FF.toInt()) height=40.dp />
        }
        children.inflexible {
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=40.dp />
            <SizedRectangle color=Color(0xFF00FF00.toInt()) width=50.dp />
            <SizedRectangle color=Color(0xFF0000FF.toInt()) width=60.dp />
        }
        children.expanded(/*flex=*/1f) {
            <SizedRectangle color=Color(0xFF00FF00.toInt()) />
        }
    </FlexRow>
}

@Composable
fun FlexColumnUsage() {
    <FlexColumn> children ->
        // TODO(popam): named arguments cannot be used because of the adapter hack
        children.expanded(/*flex=*/2f) {
            <Center>
                <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
            </Center>
            <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp />
        }
        children.inflexible {
            <SizedRectangle color=Color(0xFFFF0000.toInt()) height=40.dp />
            <SizedRectangle color=Color(0xFF00FF00.toInt()) height=50.dp />
            <SizedRectangle color=Color(0xFF0000FF.toInt()) height=60.dp />
        }
        children.expanded(/*flex=*/1f) {
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

/**
 * Entry point for the activity.
 */
@Composable
fun ComplexLayout() {
    <CraneWrapper>
        <FlexColumnUsage />
    </CraneWrapper>
}
