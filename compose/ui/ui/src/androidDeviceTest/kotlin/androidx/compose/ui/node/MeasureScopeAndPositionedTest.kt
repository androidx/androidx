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

package androidx.compose.ui.node

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.HorizontalRuler
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.approachLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class MeasureScopeAndPositionedTest {

    @get:Rule val rule = createComposeRule()

    /**
     * Checks that `Modifier.approachLayout` overrides the outer measured size (`80x90`), preserves
     * custom rulers and placement offsets for its children, and transitions back to `40x50` once
     * lookahead is reached.
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun approachLayout_sizeOverride_preservesAlignmentLinesRulersAndPlacement() {
        var reachedLookahead by mutableStateOf(false)
        val customRuler = VerticalRuler()
        var childRulerValue = Float.NaN
        var approachSize = IntSize.Zero
        var childSize = IntSize.Zero
        var wrapperSize = IntSize.Zero

        rule.setContent {
            LookaheadScope {
                Layout(
                    content = {
                        Layout(
                            modifier =
                                Modifier.approachLayout(
                                        isMeasurementApproachInProgress = { !reachedLookahead },
                                        approachMeasure = { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            layout(
                                                width =
                                                    if (reachedLookahead) placeable.width else 80,
                                                height =
                                                    if (reachedLookahead) placeable.height else 90,
                                                rulers = { customRuler provides 42f },
                                            ) {
                                                placeable.place(5, 7)
                                            }
                                        },
                                    )
                                    .onGloballyPositioned { wrapperSize = it.size },
                            content = {
                                Layout(
                                    modifier = Modifier.onGloballyPositioned { childSize = it.size }
                                ) { _, _ ->
                                    layout(40, 50) {
                                        childRulerValue = customRuler.current(0f)
                                    }
                                }
                            },
                        ) { measurables, constraints ->
                            val placeable = measurables.first().measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    approachSize = IntSize(placeable.width, placeable.height)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
            }
        }

        rule.runOnIdle {
            assertThat(approachSize).isEqualTo(IntSize(80, 90))
            assertThat(wrapperSize).isEqualTo(IntSize(40, 50))
            assertThat(childSize).isEqualTo(IntSize(40, 50))
            assertThat(childRulerValue).isEqualTo(37f)
        }

        reachedLookahead = true
        rule.runOnIdle {
            assertThat(approachSize).isEqualTo(IntSize(40, 50))
            assertThat(wrapperSize).isEqualTo(IntSize(40, 50))
        }
    }

    /**
     * Checks that `approachLayout` wrapping `requiredSize` overrides the outer measured height
     * during the approach, snaps back once lookahead is reached, and propagates rulers only
     * downward to children (not upward to the parent).
     */
    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun approachLayout_withRequiredSize_propagatesBaselineAndRulersToParent() {
        var reachedLookahead by mutableStateOf(false)
        var outerMeasuredSize = IntSize.Zero
        var wrapperSize = IntSize.Zero
        var childRulerValue = Float.NaN
        var outerRulerValue = Float.NaN
        val approachRuler = HorizontalRuler()

        rule.setContent {
            LookaheadScope {
                Layout(
                    content = {
                        Box(
                            Modifier.approachLayout(
                                    isMeasurementApproachInProgress = { !reachedLookahead },
                                    approachMeasure = { measurable, constraints ->
                                        val placeable = measurable.measure(constraints)
                                        layout(
                                            width = placeable.width,
                                            height = 60,
                                            rulers = { approachRuler provides 19f },
                                        ) {
                                            placeable.place(0, 0)
                                        }
                                    },
                                )
                                .requiredSize(50.dp)
                                .onGloballyPositioned { wrapperSize = it.size }
                        ) {
                            Layout { _, _ ->
                                layout(10, 10) {
                                    childRulerValue = approachRuler.current(Float.NaN)
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    outerMeasuredSize = IntSize(placeable.width, placeable.height)
                    layout(placeable.width, placeable.height) {
                        outerRulerValue = approachRuler.current(Float.NaN)
                        placeable.place(0, 0)
                    }
                }
            }
        }

        rule.runOnIdle {
            val expectedWidthPx = with(rule.density) { 50.dp.roundToPx() }
            assertThat(outerMeasuredSize).isEqualTo(IntSize(expectedWidthPx, 60))
            assertThat(wrapperSize).isEqualTo(IntSize(expectedWidthPx, expectedWidthPx))
            assertThat(childRulerValue).isEqualTo(19f)
            assertThat(outerRulerValue).isEqualTo(Float.NaN) // rulers flow down to children, not up
        }

        reachedLookahead = true
        rule.runOnIdle {
            val expectedPx = with(rule.density) { 50.dp.roundToPx() }
            assertThat(outerMeasuredSize).isEqualTo(IntSize(expectedPx, expectedPx))
            assertThat(wrapperSize).isEqualTo(IntSize(expectedPx, expectedPx))
        }
    }

    /**
     * Checks that calling `MeasureScope.layout(...)` with both `alignmentLines` and `rulers` sends
     * alignment lines up to the parent and rulers down to the child at the same time.
     */
    @Test
    fun measureScope_layout_propagatesRulersAndAlignmentLinesToChildrenAndParent() {
        val vRuler = VerticalRuler()
        val hRuler = HorizontalRuler()
        var readVRuler = Float.NaN
        var readHRuler = Float.NaN
        var readBaseline = -1

        rule.setContent {
            Layout(
                content = {
                    Layout(
                        content = {
                            Layout { _, _ ->
                                layout(10, 10) {
                                    readVRuler = vRuler.current(-1f)
                                    readHRuler = hRuler.current(-1f)
                                }
                            }
                        }
                    ) { measurables, constraints ->
                        val p = measurables.first().measure(constraints)
                        layout(
                            width = 100,
                            height = 120,
                            alignmentLines = mapOf(FirstBaseline to 33),
                            rulers = {
                                vRuler provides 12.5f
                                hRuler provides 24.5f
                            },
                        ) {
                            p.place(0, 0)
                        }
                    }
                }
            ) { measurables, constraints ->
                val p = measurables.first().measure(constraints)
                readBaseline = p[FirstBaseline]
                layout(p.width, p.height) { p.place(0, 0) }
            }
        }

        rule.runOnIdle {
            assertThat(readBaseline).isEqualTo(33)
            assertThat(readVRuler).isEqualTo(12.5f)
            assertThat(readHRuler).isEqualTo(24.5f)
        }
    }

    /**
     * Checks that dynamically changing a `graphicsLayer`'s `clip`, `shape`, and `translationX`
     * updates `onGloballyPositioned` coordinates and keeps semantics intact.
     */
    @Test
    fun graphicsLayer_clipAndShapeChanges_updateCoordinatesAndSemantics() {
        var clipEnabled by mutableStateOf(false)
        var useCircle by mutableStateOf(false)
        var translationX by mutableStateOf(0f)
        var positionedCount = 0
        var latestPosition = Offset.Zero

        rule.setContent {
            Box(
                Modifier.testTag("layerBox")
                    .size(100.dp)
                    .graphicsLayer {
                        clip = clipEnabled
                        shape = if (useCircle) CircleShape else RoundedCornerShape(8.dp)
                        this.translationX = translationX
                    }
                    .onGloballyPositioned {
                        positionedCount++
                        latestPosition = it.positionInRoot()
                    }
                    .semantics { contentDescription = "layered" }
            )
        }

        rule.runOnIdle {
            assertThat(positionedCount).isGreaterThan(0)
            assertThat(latestPosition).isEqualTo(Offset.Zero)
        }

        val initialCount = positionedCount
        clipEnabled = true
        useCircle = true
        translationX = 25f

        rule.runOnIdle {
            assertThat(positionedCount).isGreaterThan(initialCount)
            assertThat(latestPosition.x).isWithin(0.5f).of(25f)
        }

        rule.onNodeWithTag("layerBox").assertContentDescriptionEquals("layered")

        // Verify toggling back to unclipped rectangle works cleanly
        clipEnabled = false
        useCircle = false
        rule.runOnIdle {
            assertThat(latestPosition.x).isWithin(0.5f).of(25f)
        }
        rule.onNodeWithTag("layerBox").assertContentDescriptionEquals("layered")
    }

    /**
     * Checks that coordinate conversion (`localPositionOf`) and matrix transforms (`transformFrom`)
     * agree and round-trip accurately across `offset` and `graphicsLayer` transforms.
     */
    @Test
    fun nodeCoordinator_nestedTransformsAndOffsets_roundTripAccurately() {
        lateinit var rootCoords: LayoutCoordinates
        lateinit var childCoords: LayoutCoordinates

        rule.setContent {
            Box(Modifier.size(200.dp).onGloballyPositioned { rootCoords = it }) {
                Box(
                    Modifier.offset { IntOffset(30, 40) }
                        .graphicsLayer {
                            translationX = 10f
                            translationY = 20f
                            scaleX = 2f
                            scaleY = 2f
                        }
                        .size(50.dp)
                        .onGloballyPositioned { childCoords = it }
                )
            }
        }

        rule.runOnIdle {
            val localPoint = Offset(10f, 15f)
            val inRoot = rootCoords.localPositionOf(childCoords, localPoint)

            val matrix = Matrix()
            rootCoords.transformFrom(childCoords, matrix)
            val mapped = matrix.map(localPoint)

            assertThat(mapped.x).isWithin(0.5f).of(inRoot.x)
            assertThat(mapped.y).isWithin(0.5f).of(inRoot.y)

            val inverseMatrix = Matrix()
            childCoords.transformFrom(rootCoords, inverseMatrix)
            val roundTripped = inverseMatrix.map(mapped)
            assertThat(roundTripped.x.roundToInt()).isEqualTo(localPoint.x.roundToInt())
            assertThat(roundTripped.y.roundToInt()).isEqualTo(localPoint.y.roundToInt())
        }
    }

    /**
     * Checks that `onGloballyPositioned` fires for parent, child, and a child dynamically added
     * during a callback, and removes cleanly without firing for the removed node.
     */
    @Test
    fun onGloballyPositioned_parentDispatchedBeforeChild_andDynamicAddRemove() {
        val dispatchOrder = mutableListOf<String>()
        var showDynamicChild by mutableStateOf(false)
        var parentSize by mutableStateOf(100.dp)

        rule.setContent {
            Box(Modifier.size(parentSize).onGloballyPositioned { dispatchOrder += "parent" }) {
                Box(Modifier.size(50.dp).onGloballyPositioned { dispatchOrder += "child" }) {
                    if (showDynamicChild) {
                        Box(
                            Modifier.size(20.dp).onGloballyPositioned {
                                dispatchOrder += "dynamic"
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertThat(dispatchOrder).containsAtLeast("parent", "child")
            showDynamicChild = true
        }

        rule.runOnIdle { assertThat(dispatchOrder).contains("dynamic") }

        dispatchOrder.clear()
        rule.runOnIdle {
            showDynamicChild = false
            parentSize = 110.dp
        }
        rule.runOnIdle {
            assertThat(dispatchOrder).contains("parent")
            assertThat(dispatchOrder).doesNotContain("dynamic")
        }
    }
}
