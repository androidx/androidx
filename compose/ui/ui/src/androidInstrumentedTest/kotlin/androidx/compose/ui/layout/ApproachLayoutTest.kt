/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Node
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.LayoutCoordinatesStub
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import kotlin.test.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ApproachLayoutTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    // Test that measurement approach has no effect on parent or child when
    // isMeasurementApproachComplete returns true
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun toggleIsMeasurementApproachComplete() {
        var isComplete by mutableStateOf(true)
        var parentLookaheadSize = IntSize(-1, -1)
        var childLookaheadConstraints: Constraints? = null
        var childLookaheadSize = IntSize(-1, -1)
        // This fraction change triggers a lookahead pass, which will be required to
        // do a `isMeasurementApproachComplete` after its prior completion.
        var fraction by mutableStateOf(0.5f)
        var lookaheadPositionInParent = androidx.compose.ui.geometry.Offset(Float.NaN, Float.NaN)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier
                    .layout { measurable, constraints ->
                        measurable
                            .measure(constraints)
                            .run {
                                if (isLookingAhead) {
                                    parentLookaheadSize = IntSize(width, height)
                                } else {
                                    // Verify the approach size the same as lookahead when approach
                                    // completes, and that they differ before completion.
                                    if (isComplete) {
                                        assertEquals(parentLookaheadSize.width, width)
                                        assertEquals(parentLookaheadSize.height, height)
                                    } else {
                                        assertNotEquals(parentLookaheadSize.width, width)
                                        assertNotEquals(parentLookaheadSize.height, height)
                                    }
                                }
                                layout(width, height) { place(0, 0) }
                            }
                    }
                    .approachLayout(
                        isMeasurementApproachComplete = { isComplete }
                    ) { measurable, _ ->
                        // Intentionally use different constraints, placement and report different
                        // measure result than lookahead, to verify that they have no effect on
                        // the layout after completion.
                        val constraints = Constraints.fixed(
                            lookaheadSize.width - 20, lookaheadSize.height - 20
                        )
                        measurable
                            .measure(constraints)
                            .run {
                                layout(lookaheadSize.width - 20, lookaheadSize.height - 20) {
                                    place(20, 20)
                                }
                            }
                    }
                    .layout { measurable, constraints ->
                        measurable
                            .measure(constraints)
                            .run {
                                if (isLookingAhead) {
                                    childLookaheadConstraints = constraints
                                    childLookaheadSize = IntSize(width, height)
                                } else {
                                    if (isComplete) {
                                        assertEquals(childLookaheadSize.width, width)
                                        assertEquals(childLookaheadSize.height, height)
                                        assertEquals(childLookaheadConstraints, constraints)
                                    } else {
                                        assertNotEquals(childLookaheadSize.width, width)
                                        assertNotEquals(childLookaheadSize.height, height)
                                        assertNotEquals(childLookaheadConstraints, constraints)
                                    }
                                }
                                layout(width, height) {
                                    if (isLookingAhead) {
                                        lookaheadPositionInParent =
                                            coordinates?.positionInParent()
                                                ?: lookaheadPositionInParent
                                    } else {
                                        coordinates?.let {
                                            if (isComplete) {
                                                assertEquals(
                                                    lookaheadPositionInParent,
                                                    it.positionInParent()
                                                )
                                            } else {
                                                assertNotEquals(
                                                    lookaheadPositionInParent,
                                                    it.positionInParent()
                                                )
                                            }
                                        }
                                    }
                                    place(0, 0)
                                }
                            }
                    }
                    .fillMaxSize()
                )
            }
        }
        rule.runOnIdle {
            fraction = 0.75f
            isComplete = false
        }
        rule.waitForIdle()
        rule.runOnIdle {
            isComplete = true
        }
    }

    // Test that placement approach has no effect when _both measure & place approaches_ complete
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun toggleIsPlacementApproachComplete() {
        var isMeasurementApproachComplete by mutableStateOf(true)
        var isPlacementApproachComplete by mutableStateOf(false)
        var parentLookaheadSize = IntSize(-1, -1)
        var childLookaheadConstraints: Constraints? = null
        var childLookaheadSize = IntSize(-1, -1)
        // This fraction change triggers a lookahead pass, which will be required to
        // do a `isMeasurementApproachComplete` after its prior completion.
        var fraction by mutableStateOf(0.5f)
        var lookaheadPositionInParent = androidx.compose.ui.geometry.Offset(Float.NaN, Float.NaN)
        var approachPositionInParent = androidx.compose.ui.geometry.Offset(Float.NaN, Float.NaN)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier
                    .layout { measurable, constraints ->
                        measurable
                            .measure(constraints)
                            .run {
                                if (isLookingAhead) {
                                    parentLookaheadSize = IntSize(width, height)
                                } else {
                                    // Verify the approach size the same as lookahead when approach
                                    // completes, and that they differ before completion.
                                    if (isMeasurementApproachComplete) {
                                        assertEquals(parentLookaheadSize.width, width)
                                        assertEquals(parentLookaheadSize.height, height)
                                    } else {
                                        assertNotEquals(parentLookaheadSize.width, width)
                                        assertNotEquals(parentLookaheadSize.height, height)
                                    }
                                }
                                layout(width, height) { place(0, 0) }
                            }
                    }
                    .approachLayout(
                        isMeasurementApproachComplete = { isMeasurementApproachComplete },
                        isPlacementApproachComplete = { isPlacementApproachComplete }
                    ) { measurable, _ ->
                        // Intentionally use different constraints, placement and report different
                        // measure result than lookahead, to verify that they have no effect on
                        // the layout after completion.
                        val constraints = Constraints.fixed(
                            lookaheadSize.width - 20, lookaheadSize.height - 20
                        )
                        measurable
                            .measure(constraints)
                            .run {
                                layout(lookaheadSize.width - 20, lookaheadSize.height - 20) {
                                    place(20, 20)
                                }
                            }
                    }
                    .layout { measurable, constraints ->
                        measurable
                            .measure(constraints)
                            .run {
                                if (isLookingAhead) {
                                    childLookaheadConstraints = constraints
                                    childLookaheadSize = IntSize(width, height)
                                } else {
                                    if (isMeasurementApproachComplete) {
                                        assertEquals(childLookaheadSize.width, width)
                                        assertEquals(childLookaheadSize.height, height)
                                        assertEquals(childLookaheadConstraints, constraints)
                                    } else {
                                        assertNotEquals(childLookaheadSize.width, width)
                                        assertNotEquals(childLookaheadSize.height, height)
                                        assertNotEquals(childLookaheadConstraints, constraints)
                                    }
                                }
                                layout(width, height) {
                                    if (isLookingAhead) {
                                        lookaheadPositionInParent =
                                            coordinates?.positionInParent()
                                                ?: lookaheadPositionInParent
                                    } else {
                                        coordinates?.let {
                                            approachPositionInParent =
                                                coordinates?.positionInParent()
                                                    ?: approachPositionInParent
                                        }
                                    }
                                    place(0, 0)
                                }
                            }
                    }
                    .fillMaxSize()
                )
            }
        }

        rule.runOnIdle {
            assertNotEquals(Offset(Float.NaN, Float.NaN), lookaheadPositionInParent)
            assertNotEquals(Offset(Float.NaN, Float.NaN), approachPositionInParent)
            // Initial condition: placement incomplete, measurement complete
            assertNotEquals(
                lookaheadPositionInParent,
                approachPositionInParent
            )
        }

        rule.runOnIdle {
            fraction = 0.75f
            // Reverse placement and measurement completion, expect placement to be re-run
            isPlacementApproachComplete = true
            isMeasurementApproachComplete = false
        }
        rule.runOnIdle {
            // Updated condition: placement complete, measurement incomplete
            assertNotEquals(
                lookaheadPositionInParent,
                approachPositionInParent
            )
        }
        rule.runOnIdle {
            isMeasurementApproachComplete = true
        }
        rule.runOnIdle {
            // Both measurement and placement are complete.
            assertEquals(
                lookaheadPositionInParent,
                approachPositionInParent
            )
        }

        rule.runOnIdle {
            fraction = 0.85f
            // Reverse placement and measurement completion, expect placement to be re-run
            isPlacementApproachComplete = true
            isMeasurementApproachComplete = false
        }
        rule.runOnIdle {
            // Updated condition: placement complete, measurement incomplete
            assertNotEquals(
                lookaheadPositionInParent,
                approachPositionInParent
            )
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun activeParentNestedApproachNode() {
        var parentMeasureApproachComplete by mutableStateOf(false)
        var childMeasureApproachComplete by mutableStateOf(true)
        var parentLookaheadConstraints: Constraints? = null
        var parentApproachConstraints: Constraints? = null
        var parentLookaheadSize: IntSize? = null
        var parentApproachSize: IntSize? = null

        var childLookaheadConstraints: Constraints? = null
        var childApproachConstraints: Constraints? = null
        var childLookaheadSize: IntSize? = null
        var childApproachSize: IntSize? = null
        val parentApproachNode = object : TestApproachLayoutModifierNode() {
            override fun isMeasurementApproachComplete(lookaheadSize: IntSize): Boolean {
                return parentMeasureApproachComplete
            }

            @ExperimentalComposeUiApi
            override fun ApproachMeasureScope.approachMeasure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                return measurable.measure(Constraints.fixed(600, 600)).run {
                    layout(600, 600) {
                        place(0, 0)
                    }
                }
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier
                        .fillMaxSize(
                            // This forces a lookahead pass when approach complete is changed,
                            // because in the future we will only permit complete becoming true
                            // after a lookahead pass.
                            if (childMeasureApproachComplete)
                                1f
                            else if (parentMeasureApproachComplete)
                                0.9f
                            else
                                0.95f
                        )
                        .requiredSize(700.dp, 700.dp)
                        .then(
                            TestApproachElement(parentApproachNode)
                        ),
                    propagateMinConstraints = true
                ) {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                if (isLookingAhead) {
                                    parentLookaheadConstraints = constraints
                                } else {
                                    parentApproachConstraints = constraints
                                }
                                measurable
                                    .measure(constraints)
                                    .run {
                                        if (isLookingAhead) {
                                            parentLookaheadSize = IntSize(width, height)
                                        } else {
                                            parentApproachSize = IntSize(width, height)
                                        }
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }
                            .approachLayout({ childMeasureApproachComplete }) { m, _ ->
                                m
                                    .measure(Constraints.fixed(500, 500))
                                    .run {
                                        layout(500, 500) {
                                            place(0, 0)
                                        }
                                    }
                            }
                            .layout { measurable, constraints ->
                                if (isLookingAhead) {
                                    childLookaheadConstraints = constraints
                                } else {
                                    childApproachConstraints = constraints
                                }
                                measurable
                                    .measure(constraints)
                                    .run {
                                        if (isLookingAhead) {
                                            childLookaheadSize = IntSize(width, height)
                                        } else {
                                            childApproachSize = IntSize(width, height)
                                        }
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(IntSize(700, 700), parentLookaheadSize)
            assertEquals(IntSize(700, 700), childLookaheadSize)
            assertEquals(Constraints.fixed(700, 700), parentLookaheadConstraints)
            assertEquals(Constraints.fixed(700, 700), childLookaheadConstraints)

            assertEquals(IntSize(500, 500), childApproachSize)
            assertEquals(IntSize(600, 600), parentApproachSize)
            assertEquals(Constraints.fixed(600, 600), parentApproachConstraints)
            assertEquals(Constraints.fixed(500, 500), childApproachConstraints)
        }

        rule.runOnIdle {
            childMeasureApproachComplete = false
            parentMeasureApproachComplete = false
        }

        rule.runOnIdle {
            assertEquals(IntSize(700, 700), parentLookaheadSize)
            assertEquals(IntSize(700, 700), childLookaheadSize)
            assertEquals(Constraints.fixed(700, 700), parentLookaheadConstraints)
            assertEquals(Constraints.fixed(700, 700), childLookaheadConstraints)

            assertEquals(IntSize(500, 500), childApproachSize)
            assertEquals(IntSize(600, 600), parentApproachSize)
            assertEquals(Constraints.fixed(600, 600), parentApproachConstraints)
            assertEquals(Constraints.fixed(500, 500), childApproachConstraints)
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun activeChildNestedApproachNode() {
        var parentMeasureApproachComplete by mutableStateOf(true)
        var childMeasureApproachComplete by mutableStateOf(false)
        var parentLookaheadConstraints: Constraints? = null
        var parentApproachConstraints: Constraints? = null
        var parentLookaheadSize: IntSize? = null
        var parentApproachSize: IntSize? = null

        var childLookaheadConstraints: Constraints? = null
        var childApproachConstraints: Constraints? = null
        var childLookaheadSize: IntSize? = null
        var childApproachSize: IntSize? = null
        val parentApproachNode = object : TestApproachLayoutModifierNode() {
            override fun isMeasurementApproachComplete(lookaheadSize: IntSize): Boolean {
                return parentMeasureApproachComplete
            }

            @ExperimentalComposeUiApi
            override fun ApproachMeasureScope.approachMeasure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                return measurable.measure(Constraints.fixed(600, 600)).run {
                    layout(600, 600) {
                        place(0, 0)
                    }
                }
            }
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    Modifier
                        .fillMaxSize(
                            // This forces a lookahead pass when approach complete is changed,
                            // because in the future we will only permit complete becoming true
                            // after a lookahead pass.
                            if (childMeasureApproachComplete)
                                1f
                            else if (parentMeasureApproachComplete)
                                0.9f
                            else
                                0.95f
                        )
                        .requiredSize(700.dp, 700.dp)
                        .then(
                            TestApproachElement(parentApproachNode)
                        ),
                    propagateMinConstraints = true
                ) {
                    Box(
                        Modifier
                            .layout { measurable, constraints ->
                                if (isLookingAhead) {
                                    parentLookaheadConstraints = constraints
                                } else {
                                    parentApproachConstraints = constraints
                                }
                                measurable
                                    .measure(constraints)
                                    .run {
                                        if (isLookingAhead) {
                                            parentLookaheadSize = IntSize(width, height)
                                        } else {
                                            parentApproachSize = IntSize(width, height)
                                        }
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }
                            .approachLayout({ childMeasureApproachComplete }) { m, _ ->
                                m
                                    .measure(Constraints.fixed(500, 500))
                                    .run {
                                        layout(500, 500) {
                                            place(0, 0)
                                        }
                                    }
                            }
                            .layout { measurable, constraints ->
                                if (isLookingAhead) {
                                    childLookaheadConstraints = constraints
                                } else {
                                    childApproachConstraints = constraints
                                }
                                measurable
                                    .measure(constraints)
                                    .run {
                                        if (isLookingAhead) {
                                            childLookaheadSize = IntSize(width, height)
                                        } else {
                                            childApproachSize = IntSize(width, height)
                                        }
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }) {
                        Box(Modifier.fillMaxSize())
                    }
                }
            }
        }
        rule.runOnIdle {
            // Child approach is active, parent completed
            assertEquals(IntSize(700, 700), parentLookaheadSize)
            assertEquals(IntSize(700, 700), childLookaheadSize)
            assertEquals(Constraints.fixed(700, 700), parentLookaheadConstraints)
            assertEquals(Constraints.fixed(700, 700), childLookaheadConstraints)

            assertEquals(IntSize(500, 500), childApproachSize)
            assertEquals(IntSize(700, 700), parentApproachSize)
            assertEquals(Constraints.fixed(700, 700), parentApproachConstraints)
            assertEquals(Constraints.fixed(500, 500), childApproachConstraints)
        }

        rule.runOnIdle {
            childMeasureApproachComplete = false
            parentMeasureApproachComplete = false
        }

        rule.runOnIdle {
            assertEquals(IntSize(700, 700), parentLookaheadSize)
            assertEquals(IntSize(700, 700), childLookaheadSize)
            assertEquals(Constraints.fixed(700, 700), parentLookaheadConstraints)
            assertEquals(Constraints.fixed(700, 700), childLookaheadConstraints)

            assertEquals(IntSize(500, 500), childApproachSize)
            assertEquals(IntSize(600, 600), parentApproachSize)
            assertEquals(Constraints.fixed(600, 600), parentApproachConstraints)
            assertEquals(Constraints.fixed(500, 500), childApproachConstraints)
        }
    }

    @Test
    fun testDefaultPlacementApproachComplete() {
        var measurementComplete = true
        val node = object : ApproachLayoutModifierNode {
            override fun isMeasurementApproachComplete(lookaheadSize: IntSize): Boolean {
                return measurementComplete
            }

            @ExperimentalComposeUiApi
            override fun ApproachMeasureScope.approachMeasure(
                measurable: Measurable,
                constraints: Constraints
            ): MeasureResult {
                return measurable.measure(constraints).run {
                    layout(width, height) {
                        place(0, 0)
                    }
                }
            }

            override val node: Node = object : Node() {}
        }

        assertEquals(true, node.isMeasurementApproachComplete(IntSize.Zero))
        with(TestPlacementScope()) {
            with(node) {
                isPlacementApproachComplete(LayoutCoordinatesStub())
            }
        }.also {
            assertEquals(true, it)
        }

        measurementComplete = false
        assertEquals(false, node.isMeasurementApproachComplete(IntSize.Zero))
        with(TestPlacementScope()) {
            with(node) {
                isPlacementApproachComplete(LayoutCoordinatesStub())
            }
        }.also {
            assertEquals(true, it)
        }
    }

    private class TestPlacementScope : Placeable.PlacementScope() {
        override val parentWidth: Int
            get() = TODO("Not yet implemented")
        override val parentLayoutDirection: LayoutDirection
            get() = TODO("Not yet implemented")
    }

    private data class TestApproachElement(
        var approachNode: TestApproachLayoutModifierNode
    ) : ModifierNodeElement<TestApproachLayoutModifierNode>() {
        override fun create(): TestApproachLayoutModifierNode {
            return approachNode
        }

        override fun update(node: TestApproachLayoutModifierNode) {
        }
    }

    abstract class TestApproachLayoutModifierNode : Node(), ApproachLayoutModifierNode
}
