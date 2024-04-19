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

package androidx.compose.foundation.layout

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import kotlin.math.ceil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class RowColumnModifierTest() {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testRow_updatesOnAlignmentChange() {
        var positionInParentY = 0f
        var alignment by mutableStateOf(Alignment.Top)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp(), if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }
                                    )
                                    .align(alignment)
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(0)
            alignment = Alignment.CenterVertically
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(5)
        }
    }

    @Test
    fun testRow_updatesOnAlignByBlockChange() {
        var positionInParentY = 0f
        val alignByBlock: (Measured) -> Int = { _ -> 5 }
        val alignByNewBlock: (Measured) -> Int = { _ -> 0 }
        var alignment by mutableStateOf(alignByBlock)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp(), if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }
                                    )
                                    .alignBy(
                                        if (index == 4) {
                                            alignment
                                        } else {
                                            alignByBlock
                                        }
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(0)
            alignment = alignByNewBlock
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentY).isEqualTo(5)
        }
    }

    @Test
    fun testRow_updatesOnWeightChange() {
        var width = 0
        var fill by mutableStateOf(false)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp()
                                    )
                                    .weight(1f, fill)
                                    .onSizeChanged {
                                        if (index > 0) {
                                            Truth
                                                .assertThat(it.width)
                                                .isEqualTo(width)
                                        } else {
                                            width = it.width
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(20)
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
        }
    }

    @Test
    fun testRow_updatesOnWeightAndAlignmentChange() {
        var width = 0
        var fill by mutableStateOf(false)
        var positionInParentY = 0f
        var alignment by mutableStateOf(Alignment.Top)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Row(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp(), if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }
                                    )
                                    .weight(1f, fill)
                                    .onSizeChanged {
                                        if (index > 0) {
                                            Truth
                                                .assertThat(it.width)
                                                .isEqualTo(width)
                                        } else {
                                            width = it.width
                                        }
                                    }
                                    .align(alignment)
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentY = positionInParent.y
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(20)
            Truth.assertThat(positionInParentY).isEqualTo(0)
            alignment = Alignment.CenterVertically
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(width).isEqualTo(40)
            Truth.assertThat(positionInParentY).isEqualTo(5)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun testRow_correctlyCalculatesIntrinsicCrossAxis() {
        var totalFakeTextPlaced = 0

        rule.setContent {
            Row(
                Modifier
                    .width(200.dp)
                    .background(Color.Green)
                    .height(IntrinsicSize.Max)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color.Blue)
                )

                Column(Modifier.wrapContentHeight()) {
                    FakeText(modifier = Modifier.onPlaced {
                        totalFakeTextPlaced++
                    }, text = "Text")
                    FlowRow(Modifier) {
                        FakeText(
                            modifier = Modifier.onPlaced {
                                totalFakeTextPlaced++
                        }, text = "Really long text 1")
                        FakeText(modifier = Modifier.onPlaced {
                            totalFakeTextPlaced++
                        }, text = "Really long text 2")
                    }
                }

                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(60.dp)
                        .background(Color.Red)
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalFakeTextPlaced).isEqualTo(3)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun testColumn_correctlyCalculatesIntrinsicCrossAxis() {
        var totalFakeTextPlaced = 0
        val forRow = false
        rule.setContent {
            Column(
                Modifier
                    .height(176.dp)
                    .background(Color.Green)
                    .width(IntrinsicSize.Max)
            ) {
                Row(Modifier.wrapContentWidth()) {
                    FakeText(modifier = Modifier.onPlaced {
                        totalFakeTextPlaced++
                    }, text = "Text", forRow)
                    FlowColumn(Modifier) {
                        FakeText(
                            modifier = Modifier.onPlaced {
                                totalFakeTextPlaced++
                            }, text = "Really long text 1", forRow)
                        FakeText(modifier = Modifier.onPlaced {
                            totalFakeTextPlaced++
                        }, text = "Really long text 2", forRow)
                    }
                }

                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(120.dp)
                        .background(Color.Red)
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(totalFakeTextPlaced).isEqualTo(3)
        }
    }

    /**
     * @param forRow creates the bug setting for row. Otherwise, make it work for Column
     * by laying out the text top to bottom.
     */
    @Composable
    fun FakeText(modifier: Modifier = Modifier, text: String, forRow: Boolean = true) {
        val characterSizeMainAxis = 8.dp
        val textCrossAxisSize = 30.dp

        val maxIntrinsicMainAxisSize = (characterSizeMainAxis * text.length)
        val orientation = if (forRow) LayoutOrientation.Horizontal else LayoutOrientation.Vertical
        Layout(
            content = {},
            modifier = modifier,
            measurePolicy = object : MeasurePolicy {
                override fun MeasureScope.measure(
                    measurables: List<Measurable>,
                    constraints: Constraints
                ): MeasureResult {
                    val constraintsIndependent = OrientationIndependentConstraints(
                        constraints,
                        orientation
                    )
                    val maxMainAxis = constraintsIndependent.mainAxisMax
                    val lengthNeeded = text.length * characterSizeMainAxis.roundToPx()
                    val crossAxis = getCrossAxisNeeded(maxMainAxis)
                    val mainAxis = lengthNeeded.coerceAtMost(maxMainAxis)

                    var width: Int
                    var height: Int
                    if (forRow) {
                        width = mainAxis
                        height = crossAxis
                    } else {
                        width = crossAxis
                        height = mainAxis
                    }

                    return layout(width, height) {
                        measurables.forEach { measurable ->
                            val placeable = measurable.measure(constraints)
                            placeable.place(0, 0)
                        }
                    }
                }

                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    return if (forRow) {
                        getCrossAxisNeeded(width)
                    } else {
                        maxIntrinsicMainAxisSize.roundToPx()
                    }
                }

                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int
                ): Int {
                    return if (forRow) {
                        maxIntrinsicMainAxisSize.roundToPx()
                    } else {
                        getCrossAxisNeeded(height)
                    }
                }

                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                    measurables: List<IntrinsicMeasurable>,
                    width: Int
                ): Int {
                    return if (forRow) {
                        getCrossAxisNeeded(width)
                    } else {
                        characterSizeMainAxis.roundToPx()
                    }
                }

                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                    measurables: List<IntrinsicMeasurable>,
                    height: Int
                ): Int {
                    return if (forRow) {
                        characterSizeMainAxis.roundToPx()
                    } else {
                        getCrossAxisNeeded(height)
                    }
                }

                private fun IntrinsicMeasureScope.getCrossAxisNeeded(mainAxisSize: Int): Int {
                    val lengthNeeded = text.length * characterSizeMainAxis.roundToPx()
                    val noOfLines = if (mainAxisSize == Constraints.Infinity) 1 else
                        ceil((lengthNeeded.toFloat() / mainAxisSize).toDouble()).toInt()
                    return (textCrossAxisSize.roundToPx() * noOfLines)
                }
            }
        )
    }

    @Test
    fun testColumn_updatesOnAlignmentChange() {
        var positionInParentX = 0f
        var alignment by mutableStateOf(Alignment.Start)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Column(
                        Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        },
                                        20.toDp(),
                                    )
                                    .align(alignment)
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentX = positionInParent.x
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(0)
            alignment = Alignment.CenterHorizontally
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(5)
        }
    }

    @Test
    fun testColumn_updatesOnAlignByBlockChange() {
        var positionInParentX = 0f
        val alignByBlock: (Measured) -> Int = { _ -> 5 }
        val alignByNewBlock: (Measured) -> Int = { _ -> 0 }
        var alignment by mutableStateOf(alignByBlock)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(100.toDp())) {
                    Column(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        }, 20.toDp()
                                    )
                                    .alignBy(
                                        if (index == 4) {
                                            alignment
                                        } else {
                                            alignByBlock
                                        }
                                    )
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentX = positionInParent.x
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(0)
            alignment = alignByNewBlock
        }

        rule.runOnIdle {
            Truth.assertThat(positionInParentX).isEqualTo(5)
        }
    }

    @Test
    fun testColumn_updatesOnWeightChange() {
        var height = 0
        var fill by mutableStateOf(false)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Column(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        20.toDp()
                                    )
                                    .weight(1f, fill)
                                    .onSizeChanged {
                                        if (index > 0) {
                                            Truth
                                                .assertThat(it.height)
                                                .isEqualTo(height)
                                        } else {
                                            height = it.height
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(20)
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
        }
    }

    @Test
    fun testColumn_updatesOnWeightAndAlignmentChange() {
        var height = 0
        var fill by mutableStateOf(false)
        var positionInParentX = 0f
        var alignment by mutableStateOf(Alignment.Start)

        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.size(200.toDp())) {
                    Column(
                        Modifier.wrapContentHeight()
                    ) {
                        repeat(5) { index ->
                            Box(
                                Modifier
                                    .size(
                                        if (index == 4) {
                                            10.toDp()
                                        } else {
                                            20.toDp()
                                        },
                                        20.toDp(),
                                    )
                                    .weight(1f, fill)
                                    .onSizeChanged {
                                        if (index > 0) {
                                            Truth
                                                .assertThat(it.height)
                                                .isEqualTo(height)
                                        } else {
                                            height = it.height
                                        }
                                    }
                                    .align(alignment)
                                    .onPlaced {
                                        if (index == 4) {
                                            val positionInParent = it.positionInParent()
                                            positionInParentX = positionInParent.x
                                        }
                                    })
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(20)
            Truth.assertThat(positionInParentX).isEqualTo(0)
            alignment = Alignment.CenterHorizontally
            fill = true
        }

        rule.runOnIdle {
            Truth.assertThat(height).isEqualTo(40)
            Truth.assertThat(positionInParentX).isEqualTo(5)
        }
    }
}
