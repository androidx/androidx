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

package androidx.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AlignmentLinesTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    @get:Rule val excessiveAssertions = AndroidOwnerExtraAssertionsRule()
    private lateinit var activity: TestActivity
    private lateinit var density: Density

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        density = Density(activity)
    }

    @Test
    fun testAlignmentLines() {
        val testVerticalLine = VerticalAlignmentLine(::min)
        val testHorizontalLine = HorizontalAlignmentLine(::max)
        rule.setContent {
            val child1 =
                @Composable {
                    Wrap {
                        Layout(content = {}) { _, _ ->
                            layout(0, 0, mapOf(testVerticalLine to 10, testHorizontalLine to 20)) {}
                        }
                    }
                }
            val child2 =
                @Composable {
                    Wrap {
                        Layout(content = {}) { _, _ ->
                            layout(0, 0, mapOf(testVerticalLine to 20, testHorizontalLine to 10)) {}
                        }
                    }
                }
            val inner =
                @Composable {
                    Layout({
                        child1()
                        child2()
                    }) { measurables, constraints ->
                        val placeable1 = measurables[0].measure(constraints)
                        val placeable2 = measurables[1].measure(constraints)
                        assertEquals(10, placeable1[testVerticalLine])
                        assertEquals(20, placeable1[testHorizontalLine])
                        assertEquals(20, placeable2[testVerticalLine])
                        assertEquals(10, placeable2[testHorizontalLine])
                        layout(0, 0) {
                            placeable1.place(0, 0)
                            placeable2.place(0, 0)
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                assertEquals(10, placeable[testVerticalLine])
                assertEquals(20, placeable[testHorizontalLine])
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun testAlignmentLines_areNotInheritedFromInvisibleChildren() {
        val testLine1 = VerticalAlignmentLine(::min)
        val testLine2 = VerticalAlignmentLine(::min)
        rule.setContent {
            val child1 =
                @Composable {
                    Layout(content = {}) { _, _ -> layout(0, 0, mapOf(testLine1 to 10)) {} }
                }
            val child2 =
                @Composable {
                    Layout(content = {}) { _, _ -> layout(0, 0, mapOf(testLine2 to 20)) {} }
                }
            val inner =
                @Composable {
                    Layout({
                        child1()
                        child2()
                    }) { measurables, constraints ->
                        val placeable1 = measurables[0].measure(constraints)
                        measurables[1].measure(constraints)
                        layout(0, 0) {
                            // Only place the first child.
                            placeable1.place(0, 0)
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                assertEquals(10, placeable[testLine1])
                assertEquals(AlignmentLine.Unspecified, placeable[testLine2])
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun testAlignmentLines_doNotCauseMultipleMeasuresOrLayouts() {
        val testLine1 = VerticalAlignmentLine(::min)
        val testLine2 = VerticalAlignmentLine(::min)
        var child1Measures = 0
        var child2Measures = 0
        var child1Layouts = 0
        var child2Layouts = 0
        rule.setContent {
            val child1 =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        ++child1Measures
                        layout(0, 0, mapOf(testLine1 to 10)) { ++child1Layouts }
                    }
                }
            val child2 =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        ++child2Measures
                        layout(0, 0, mapOf(testLine2 to 20)) { ++child2Layouts }
                    }
                }
            val inner =
                @Composable {
                    Layout({
                        child1()
                        child2()
                    }) { measurables, constraints ->
                        val placeable1 = measurables[0].measure(constraints)
                        val placeable2 = measurables[1].measure(constraints)
                        layout(0, 0) {
                            placeable1.place(0, 0)
                            placeable2.place(0, 0)
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                assertEquals(10, placeable[testLine1])
                assertEquals(20, placeable[testLine2])
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        rule.runOnIdle {
            assertEquals(1, child1Measures)
            assertEquals(1, child2Measures)
            assertEquals(1, child1Layouts)
            assertEquals(1, child2Layouts)
        }
    }

    @Test
    fun testAlignmentLines_onlyLayoutEarlyWhenNeeded() {
        val testLine1 = VerticalAlignmentLine(::min)
        val testLine2 = VerticalAlignmentLine(::min)
        var child1Measures = 0
        var child2Measures = 0
        var child1Layouts = 0
        var child2Layouts = 0
        rule.setContent {
            val child1 =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        ++child1Measures
                        layout(0, 0, mapOf(testLine1 to 10)) { ++child1Layouts }
                    }
                }
            val child2 =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        ++child2Measures
                        layout(0, 0, mapOf(testLine2 to 20)) { ++child2Layouts }
                    }
                }
            val inner =
                @Composable {
                    Layout({
                        child1()
                        child2()
                    }) { measurables, constraints ->
                        val placeable1 = measurables[0].measure(constraints)
                        assertEquals(10, placeable1[testLine1])
                        val placeable2 = measurables[1].measure(constraints)
                        layout(0, 0) {
                            placeable1.place(0, 0)
                            placeable2.place(0, 0)
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {}
            }
        }
        rule.runOnIdle {
            assertEquals(1, child1Measures)
            assertEquals(1, child2Measures)
            assertEquals(1, child1Layouts)
            assertEquals(0, child2Layouts)
        }
    }

    @Test
    fun testAlignmentLines_canBeQueriedInThePositioningBlock() {
        val testLine = VerticalAlignmentLine(::min)
        rule.setContent {
            val child1 =
                @Composable {
                    Layout(content = {}) { _, _ -> layout(0, 0, mapOf(testLine to 10)) {} }
                }
            val child2 =
                @Composable {
                    Layout(content = {}) { _, _ -> layout(0, 0, mapOf(testLine to 20)) {} }
                }
            val inner =
                @Composable {
                    Layout({
                        child1()
                        child2()
                    }) { measurables, constraints ->
                        val placeable1 = measurables[0].measure(constraints)
                        layout(0, 0) {
                            assertEquals(10, placeable1[testLine])
                            val placeable2 = measurables[1].measure(constraints)
                            assertEquals(20, placeable2[testLine])
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {}
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun testAlignmentLines_doNotCauseExtraLayout_whenQueriedAfterPositioning() {
        val testLine = VerticalAlignmentLine(::min)
        var childLayouts = 0
        rule.setContent {
            val child =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        layout(0, 0, mapOf(testLine to 10)) { ++childLayouts }
                    }
                }
            val inner =
                @Composable {
                    Layout({ child() }) { measurables, constraints ->
                        val placeable = measurables[0].measure(constraints)
                        layout(0, 0) {
                            assertEquals(10, placeable[testLine])
                            placeable.place(0, 0)
                            assertEquals(10, placeable[testLine])
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        rule.runOnIdle { assertEquals(1, childLayouts) }
    }

    @Test
    fun testAlignmentLines_recomposeCorrectly() {
        val testLine = VerticalAlignmentLine(::min)
        val offset = mutableStateOf(10)
        var measure = 0
        var layout = 0
        var linePosition: Int? = null
        rule.setContent {
            val child =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        layout(0, 0, mapOf(testLine to offset.value)) {}
                    }
                }
            Layout(child) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                linePosition = placeable[testLine]
                ++measure
                layout(placeable.width, placeable.height) { ++layout }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measure)
            assertEquals(1, layout)
            assertEquals(10, linePosition)
            offset.value = 20
        }

        rule.runOnIdle {
            assertEquals(2, measure)
            assertEquals(2, layout)
            assertEquals(20, linePosition)
        }
    }

    @Test
    fun testAlignmentLines_recomposeCorrectly_whenQueriedInLayout() {
        val testLine = VerticalAlignmentLine(::min)
        val offset = mutableStateOf(10)
        var measure = 0
        var layout = 0
        var linePosition: Int? = null
        rule.setContent {
            val child =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        layout(0, 0, mapOf(testLine to offset.value)) {}
                    }
                }
            Layout(child) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                ++measure
                layout(placeable.width, placeable.height) {
                    linePosition = placeable[testLine]
                    ++layout
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measure)
            assertEquals(1, layout)
            assertEquals(10, linePosition)
            offset.value = 20
        }

        rule.runOnIdle {
            assertEquals(1, measure)
            assertEquals(2, layout)
            assertEquals(20, linePosition)
        }
    }

    @Test
    fun testAlignmentLines_recomposeCorrectly_whenMeasuredAndQueriedInLayout() {
        val testLine = VerticalAlignmentLine(::min)
        val offset = mutableStateOf(10)
        var measure = 0
        var layout = 0
        var linePosition: Int? = null
        rule.setContent {
            val child =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        layout(0, 0, mapOf(testLine to offset.value)) {}
                    }
                }
            Layout(child) { measurables, constraints ->
                ++measure
                layout(1, 1) {
                    val placeable = measurables.first().measure(constraints)
                    linePosition = placeable[testLine]
                    ++layout
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measure)
            assertEquals(1, layout)
            assertEquals(10, linePosition)

            offset.value = 20
        }
        rule.runOnIdle {
            assertEquals(1, measure)
            assertEquals(2, layout)
            assertEquals(20, linePosition)
        }
    }

    @Test
    fun testAlignmentLines_onlyComputesAlignmentLinesWhenNeeded() {
        val offset = mutableStateOf(10)
        var alignmentLinesCalculations = 0
        val testLine = VerticalAlignmentLine { _, _ ->
            ++alignmentLinesCalculations
            0
        }
        var linePosition by mutableStateOf(10)
        rule.setContent {
            val innerChild =
                @Composable {
                    offset.value // Artificial remeasure.
                    Layout(content = {}) { _, _ ->
                        layout(0, 0, mapOf(testLine to linePosition)) {}
                    }
                }
            val child =
                @Composable {
                    Layout({
                        innerChild()
                        innerChild()
                    }) { measurables, constraints ->
                        offset.value // Artificial remeasure.
                        val placeable1 = measurables[0].measure(constraints)
                        val placeable2 = measurables[1].measure(constraints)
                        layout(0, 0) {
                            placeable1.place(0, 0)
                            placeable2.place(0, 0)
                        }
                    }
                }
            Layout(child) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                if (offset.value < 15) {
                    placeable[testLine]
                }
                layout(0, 0) { placeable.place(0, 0) }
            }
        }
        rule.runOnIdle {
            assertEquals(1, alignmentLinesCalculations)
            offset.value = 20
            linePosition = 20
        }
        rule.runOnIdle {
            assertEquals(1, alignmentLinesCalculations)
            offset.value = 10
            linePosition = 30
        }
        rule.runOnIdle { assertEquals(2, alignmentLinesCalculations) }
    }

    @Test
    fun testAlignmentLines_providedLinesOverrideInherited() {
        val testLine = VerticalAlignmentLine(::min)
        rule.setContent {
            val innerChild =
                @Composable {
                    Layout(content = {}) { _, _ -> layout(0, 0, mapOf(testLine to 10)) {} }
                }
            val child =
                @Composable {
                    Layout({ innerChild() }) { measurables, constraints ->
                        val placeable = measurables.first().measure(constraints)
                        layout(0, 0, mapOf(testLine to 20)) { placeable.place(0, 0) }
                    }
                }
            Layout(child) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                assertEquals(20, placeable[testLine])
                layout(0, 0) { placeable.place(0, 0) }
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun testAlignmentLines_areRecalculatedCorrectlyOnRelayout_withNoRemeasure() {
        val testLine = VerticalAlignmentLine(::min)
        var innerChildMeasures = 0
        var innerChildLayouts = 0
        var outerChildMeasures = 0
        var outerChildLayouts = 0
        val offset = mutableStateOf(0)
        rule.setContent {
            val child =
                @Composable {
                    Layout(content = {}) { _, _ ->
                        ++innerChildMeasures
                        layout(0, 0, mapOf(testLine to 10)) { ++innerChildLayouts }
                    }
                }
            val inner =
                @Composable {
                    Layout({ Wrap { Wrap { child() } } }) { measurables, constraints ->
                        ++outerChildMeasures
                        val placeable = measurables[0].measure(constraints)
                        layout(0, 0) {
                            ++outerChildLayouts
                            placeable.place(offset.value, 0)
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                val width = placeable.width.coerceAtLeast(10)
                val height = placeable.height.coerceAtLeast(10)
                layout(width, height) {
                    assertEquals(offset.value + 10, placeable[testLine])
                    placeable.place(0, 0)
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, innerChildMeasures)
            assertEquals(1, innerChildLayouts)
            assertEquals(1, outerChildMeasures)
            assertEquals(1, outerChildLayouts)
            offset.value = 10
        }

        rule.runOnIdle {
            assertEquals(1, innerChildMeasures)
            assertEquals(1, innerChildLayouts)
            assertEquals(1, outerChildMeasures)
            assertEquals(2, outerChildLayouts)
        }
    }

    @Test
    fun testAlignmentLines_whenQueriedAfterPlacing() {
        val testLine = VerticalAlignmentLine(::min)
        var childLayouts = 0
        rule.setContent {
            val child =
                @Composable {
                    Layout(content = {}) { _, constraints ->
                        layout(constraints.minWidth, constraints.minHeight, mapOf(testLine to 10)) {
                            ++childLayouts
                        }
                    }
                }
            val inner =
                @Composable {
                    Layout({ Wrap { Wrap { child() } } }) { measurables, constraints ->
                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0, 0)
                            assertEquals(10, placeable[testLine])
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }
        rule.runOnIdle { assertEquals(1, childLayouts) }
    }

    @Test
    fun testAlignmentLines_whenQueriedAfterPlacing_haveCorrectNumberOfLayouts() {
        var childLayouts = 0
        var childAlignmentLinesCalculations = 0
        val testLine = VerticalAlignmentLine { v1, _ ->
            ++childAlignmentLinesCalculations
            v1
        }
        val offset = mutableStateOf(10)
        var linePositionState by mutableStateOf(10)
        var linePosition = 10
        fun changeLinePosition() {
            linePosition = 30 - linePosition
            linePositionState = 30 - linePositionState
        }
        rule.setContent {
            val childChild =
                @Composable {
                    Layout(content = {}) { _, constraints ->
                        layout(
                            constraints.minWidth,
                            constraints.minHeight,
                            mapOf(testLine to linePositionState),
                        ) {
                            offset.value // To ensure relayout.
                        }
                    }
                }
            val child =
                @Composable {
                    Layout(
                        content = {
                            childChild()
                            childChild()
                        }
                    ) { measurables, constraints ->
                        val placeables = measurables.map { it.measure(constraints) }
                        layout(constraints.minWidth, constraints.minHeight) {
                            offset.value // To ensure relayout.
                            placeables.forEach { it.place(0, 0) }
                            ++childLayouts
                        }
                    }
                }
            val inner =
                @Composable {
                    Layout({ WrapForceRelayout(offset) { child() } }) { measurables, constraints ->
                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            if (offset.value > 15) assertEquals(linePosition, placeable[testLine])
                            placeable.place(0, 0)
                            if (offset.value > 5) assertEquals(linePosition, placeable[testLine])
                        }
                    }
                }
            Layout(inner) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                val width = placeable.width.coerceAtLeast(10)
                val height = placeable.height.coerceAtLeast(10)
                layout(width, height) {
                    offset.value // To ensure relayout.
                    placeable.place(0, 0)
                }
            }
        }
        rule.runOnIdle {
            assertEquals(2, childLayouts + childAlignmentLinesCalculations)
            offset.value = 1
        }

        rule.runOnIdle {
            assertEquals(3, childLayouts + childAlignmentLinesCalculations)
            offset.value = 10
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(5, childLayouts + childAlignmentLinesCalculations)
            offset.value = 12
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(7, childLayouts + childAlignmentLinesCalculations)
            offset.value = 17
            changeLinePosition()
        }
        rule.runOnIdle {
            assertEquals(9, childLayouts + childAlignmentLinesCalculations)
            offset.value = 12
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(11, childLayouts + childAlignmentLinesCalculations)
            offset.value = 1
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(13, childLayouts + childAlignmentLinesCalculations)
            offset.value = 10
            changeLinePosition()
        }

        rule.runOnIdle { assertEquals(15, childLayouts + childAlignmentLinesCalculations) }
    }

    @Test
    fun testAlignmentLines_readFromModifier_duringMeasurement() =
        with(density) {
            val testVerticalLine = VerticalAlignmentLine(::min)
            val testHorizontalLine = HorizontalAlignmentLine(::max)

            val assertLines: Modifier.(Int, Int) -> Modifier = { vertical, horizontal ->
                this.then(
                    object : LayoutModifier {
                        override fun MeasureScope.measure(
                            measurable: Measurable,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurable.measure(constraints)
                            assertEquals(vertical, placeable[testVerticalLine])
                            assertEquals(horizontal, placeable[testHorizontalLine])
                            return layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                            }
                        }
                    }
                )
            }

            testAlignmentLinesReads(testVerticalLine, testHorizontalLine, assertLines)
        }

    @Test
    fun testAlignmentLines_readFromModifier_duringPositioning_before() =
        with(density) {
            val testVerticalLine = VerticalAlignmentLine(::min)
            val testHorizontalLine = HorizontalAlignmentLine(::max)

            val assertLines: Modifier.(Int, Int) -> Modifier = { vertical, horizontal ->
                this.then(
                    object : LayoutModifier {
                        override fun MeasureScope.measure(
                            measurable: Measurable,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurable.measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                assertEquals(vertical, placeable[testVerticalLine])
                                assertEquals(horizontal, placeable[testHorizontalLine])
                                placeable.place(0, 0)
                            }
                        }
                    }
                )
            }

            testAlignmentLinesReads(testVerticalLine, testHorizontalLine, assertLines)
        }

    @Test
    fun testAlignmentLines_readFromModifier_duringPositioning_after() =
        with(density) {
            val testVerticalLine = VerticalAlignmentLine(::min)
            val testHorizontalLine = HorizontalAlignmentLine(::max)

            val assertLines: Modifier.(Int, Int) -> Modifier = { vertical, horizontal ->
                this.then(
                    object : LayoutModifier {
                        override fun MeasureScope.measure(
                            measurable: Measurable,
                            constraints: Constraints,
                        ): MeasureResult {
                            val placeable = measurable.measure(constraints)
                            return layout(placeable.width, placeable.height) {
                                placeable.place(0, 0)
                                assertEquals(vertical, placeable[testVerticalLine])
                                assertEquals(horizontal, placeable[testHorizontalLine])
                            }
                        }
                    }
                )
            }

            testAlignmentLinesReads(testVerticalLine, testHorizontalLine, assertLines)
        }

    @Test
    fun alignmentLinesInheritedCorrectlyByParents_withModifiedPosition() {
        val testLine = HorizontalAlignmentLine(::min)
        val alignmentLinePosition = 10
        val padding = 20
        rule.setContent {
            val child =
                @Composable {
                    Wrap {
                        Layout(content = {}, modifier = Modifier.padding(padding)) { _, _ ->
                            layout(0, 0, mapOf(testLine to alignmentLinePosition)) {}
                        }
                    }
                }

            Layout(child) { measurables, constraints ->
                assertEquals(
                    padding + alignmentLinePosition,
                    measurables[0].measure(constraints)[testLine],
                )
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }

    private fun Density.testAlignmentLinesReads(
        testVerticalLine: VerticalAlignmentLine,
        testHorizontalLine: HorizontalAlignmentLine,
        assertLines: Modifier.(Int, Int) -> Modifier,
    ) {
        rule.setContent {
            val layout =
                @Composable { modifier: Modifier ->
                    Layout(modifier = modifier, content = {}) { _, _ ->
                        layout(0, 0, mapOf(testVerticalLine to 10, testHorizontalLine to 20)) {}
                    }
                }

            layout(Modifier.assertLines(10, 20))
            layout(Modifier.assertLines(30, 30).offset(20.toDp(), 10.toDp()))
            layout(Modifier.assertLines(30, 30).graphicsLayer().offset(20.toDp(), 10.toDp()))
            layout(
                Modifier.assertLines(30, 30)
                    .background(Color.Blue)
                    .graphicsLayer()
                    .offset(20.toDp(), 10.toDp())
                    .graphicsLayer()
                    .background(Color.Blue)
            )
            layout(
                Modifier.background(Color.Blue)
                    .assertLines(30, 30)
                    .background(Color.Blue)
                    .graphicsLayer()
                    .offset(20.toDp(), 10.toDp())
                    .graphicsLayer()
                    .background(Color.Blue)
            )
            Wrap(
                Modifier.background(Color.Blue)
                    .assertLines(30, 30)
                    .background(Color.Blue)
                    .graphicsLayer()
                    .offset(20.toDp(), 10.toDp())
                    .graphicsLayer()
                    .background(Color.Blue)
            ) {
                layout(Modifier)
            }
            Wrap(
                Modifier.background(Color.Blue)
                    .assertLines(40, 50)
                    .background(Color.Blue)
                    .graphicsLayer()
                    .offset(20.toDp(), 10.toDp())
                    .graphicsLayer()
                    .background(Color.Blue)
            ) {
                layout(Modifier.offset(10.toDp(), 20.toDp()))
            }
        }
        rule.waitForIdle()
    }
}

@Composable
private fun WrapForceRelayout(
    model: State<Int>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val width = placeables.maxByOrNull { it.width }?.width ?: 0
        val height = placeables.maxByOrNull { it.height }?.height ?: 0
        layout(width, height) {
            model.value
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}
