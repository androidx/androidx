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
import androidx.ui.core.Dp
import androidx.ui.core.Px
import androidx.ui.core.adapter.Align
import androidx.ui.core.adapter.Alignment
import androidx.ui.core.adapter.Center
import androidx.ui.core.adapter.Column
import androidx.ui.core.adapter.ComplexMeasureBox
import androidx.ui.core.adapter.ConstrainedBox
import androidx.ui.core.adapter.Container
import androidx.ui.core.adapter.FlexColumn
import androidx.ui.core.adapter.FlexRow
import androidx.ui.core.adapter.MeasureBox
import androidx.ui.core.adapter.Padding
import androidx.ui.core.adapter.Row
import androidx.ui.core.adapter.Stack
import androidx.ui.core.dp
import androidx.ui.core.px
import androidx.ui.core.toRoundedPixels
import androidx.ui.layout.EdgeInsets
import androidx.ui.painting.Color
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

/**
 * Draws a rectangle of a specified dimension, or to its max incoming constraints if
 * dimensions are not specified.
 */
@Composable
fun SizedRectangle(color: Color, width: Dp? = null, height: Dp? = null) {
    <MeasureBox> constraints ->
        collect {
            <DrawRectangle color />
        }
        val widthPx = width?.toPx() ?: constraints.maxWidth
        val heightPx = height?.toPx() ?: constraints.maxHeight
        layout(widthPx, heightPx) {}
    </MeasureBox>
}

/**
 * A widget that forces its only child to be as wide as its min intrinsic width.
 */
@Composable
fun IntrinsicWidth(@Children() children: () -> Unit) {
    <ComplexMeasureBox>
        val child = collect(children).first()

        layout { constraints ->
            // Force child be as wide as its min intrinsic width.
            val width = child.minIntrinsicWidth(constraints.minHeight)
            val childConstraints = Constraints(
                width.px,
                width.px,
                constraints.minHeight,
                constraints.maxHeight
            )
            val childPlaceable = child.measure(childConstraints)
            layoutResult(childPlaceable.width, childPlaceable.height) {
                childPlaceable.place(0, 0)
            }
        }

        minIntrinsicWidth { h ->
            child.minIntrinsicWidth(h)
        }
        maxIntrinsicWidth { h ->
            child.minIntrinsicWidth(h)
        }
        minIntrinsicHeight { w ->
            child.minIntrinsicHeight(w)
        }
        maxIntrinsicHeight { w ->
            child.maxIntrinsicHeight(w)
        }
    </ComplexMeasureBox>
}

@Composable
fun Wrapper(@Children() children: () -> Unit) {
    <ComplexMeasureBox>
        val child = collect(children).first()
        layout { constraints ->
            // Check the default intrinsic methods used by MeasureBoxes.
            // TODO(popam): make this a proper test instead
            require(child.minIntrinsicWidth(Px.Infinity) == 90)
            require(child.maxIntrinsicWidth(Px.Infinity) == 450)
            require(child.minIntrinsicHeight(Px.Infinity) == 30)
            require(child.maxIntrinsicHeight(Px.Infinity) == 150)
            val placeable = child.measure(constraints)
            layoutResult(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    </ComplexMeasureBox>
}

/**
 * Draws an rectangle of fixed (80.dp, 80.dp) size, while providing intrinsic dimensions as well.
 */
@Composable
fun RectangleWithIntrinsics(color: Color) {
    <ComplexMeasureBox>
        collect {
            <DrawRectangle color />
        }
        layout {
            layoutResult(80, 80) {}
        }
        minIntrinsicWidth { 30 }
        maxIntrinsicWidth { 150 }
        minIntrinsicHeight { 30 }
        maxIntrinsicHeight { 150 }
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

@Composable
fun AlignUsage() {
    <Align alignment=Alignment.BottomRight>
        <SizedRectangle color=Color(0xFF0000FF.toInt()) width=40.dp height=40.dp />
    </Align>
}

@Composable
fun StackUsage() {
    <Stack defaultAlignment=Alignment.BottomRight> children ->
        children.aligned(Alignment.Center) {
            <SizedRectangle color=Color(0xFF0000FF.toInt()) width=300.dp height=300.dp />
        }
        children.aligned(Alignment.TopLeft) {
            <SizedRectangle color=Color(0xFF00FF00.toInt()) width=150.dp height=150.dp />
        }
        children.aligned(Alignment.BottomRight) {
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=150.dp height=150.dp />
        }
        // TODO(popam): insets should be named arguments
        children.positioned(null, 20.dp, null, 20.dp) {
            <SizedRectangle color=Color(0xFFFFA500.toInt()) width=80.dp />
            <SizedRectangle color=Color(0xFFA52A2A.toInt()) width=20.dp />
        }
        children.positioned(40.dp, null, null, null) {
            <SizedRectangle color=Color(0xFFB22222.toInt()) width=20.dp />
        }
        children.positioned(null, null, 40.dp, null) {
            <SizedRectangle color=Color(0xFFFFFF00) width=40.dp />
         }
    </Stack>
}

@Composable
fun ConstrainedBoxUsage() {
    <Align alignment=Alignment.Center>
        <ConstrainedBox additionalConstraints=Constraints.tightConstraints(50.dp, 50.dp)>
            <SizedRectangle color=Color(0xFFFF0000) />
        </ConstrainedBox>
    </Align>
}

fun PaddingUsage() {
    <Row>
        <Padding padding=EdgeInsets(20.dp)>
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=20.dp height= 20.dp />
        </Padding>
        <Padding padding=EdgeInsets(20.dp)>
            <SizedRectangle color=Color(0xFFFF0000.toInt()) width=20.dp height= 20.dp />
        </Padding>
    </Row>
}

@Composable
fun ContainerUsage() {
    <Align alignment=Alignment.Center>
        <Container padding=null color=Color(0xFF0000FF.toInt()) alignment=Alignment.BottomRight
               constraints=null width=100.dp height=100.dp margin=EdgeInsets(20.dp)>
            <Container padding=EdgeInsets(20.dp) color=Color(0xFF000000.toInt())
                       alignment=Alignment.BottomRight constraints=null width=50.dp height=50.dp
                       margin=null>
                <SizedRectangle color=Color(0xFFFFFFFF.toInt()) />
            </Container>
        </Container>
    </Align>
}

/**
 * Entry point for the activity.
 */
@Composable
fun ComplexLayout() {
    <CraneWrapper>
        <ContainerUsage />
    </CraneWrapper>
}
