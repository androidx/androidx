/*
 * Copyright 2020 The Android Open Source Project
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

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PlacedChildTest {

    private val Tag = "tag"

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun remeasureNotPlacedChild() {
        val root = root {
            measurePolicy = UseChildSizeButNotPlace
            add(
                node {
                    wrapChildren = true
                    add(node { size = 10 })
                }
            )
        }

        val delegate = createDelegate(root)

        assertThat(root.height).isEqualTo(10)

        val childWithSize = root.first.first
        childWithSize.size = 20
        childWithSize.requestRemeasure()
        delegate.measureAndLayout()

        assertThat(root.height).isEqualTo(20)
    }

    @Test
    fun addingAndRemovingNotPlacingModifier() {
        var visible by mutableStateOf(false)
        rule.setContent {
            Box(
                Modifier.then(
                        if (visible) Modifier
                        else
                            Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {}
                            }
                    )
                    .size(10.dp)
                    .testTag(Tag)
            )
        }

        rule.runOnIdle { visible = true }

        rule.onNodeWithTag(Tag).assertIsDisplayed()

        rule.runOnIdle { visible = false }

        rule.onNodeWithTag(Tag).assertIsNotDisplayed()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun drawingOrderIsCorrectWhenAddingAndRemovingNotPlacingModifier() {
        var visible by mutableStateOf(false)
        val size = 4
        val halfSize = 4 / 2
        val sizeDp = with(rule.density) { size.toDp() }
        val halfSizeDp = with(rule.density) { halfSize.toDp() }
        rule.setContent {
            Box(Modifier.background(Color.Black).size(sizeDp).testTag(Tag)) {
                Box(
                    Modifier.then(
                            if (visible) Modifier
                            else
                                Modifier.layout { measurable, constraints ->
                                    val placeable = measurable.measure(constraints)
                                    layout(placeable.width, placeable.height) {}
                                }
                        )
                        .fillMaxSize()
                        .background(Color.Red)
                )
                Box(
                    Modifier.offset(y = halfSizeDp)
                        .height(halfSizeDp)
                        .fillMaxWidth()
                        .background(Color.Green)
                )
            }
        }

        rule.runOnIdle { visible = true }

        rule.onNodeWithTag(Tag).captureToImage().assertPixels(expectedSize = IntSize(size, size)) {
            offset ->
            if (offset.y < halfSize) {
                Color.Red
            } else {
                Color.Green
            }
        }

        rule.runOnIdle { visible = false }

        rule.onNodeWithTag(Tag).captureToImage().assertPixels(expectedSize = IntSize(size, size)) {
            offset ->
            if (offset.y < halfSize) {
                Color.Black
            } else {
                Color.Green
            }
        }
    }

    @Test
    fun notPlacedChildIsNotCallingPlacingBlockOnItsModifier() {
        var modifier by mutableStateOf<Modifier>(Modifier)
        rule.setContent {
            Layout(content = { Box(modifier.size(10.dp)) }) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(placeable.width, placeable.height) {}
            }
        }

        var measureCount = 0
        var placementCount = 0
        rule.runOnIdle {
            modifier =
                Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    measureCount++
                    layout(placeable.width, placeable.height) {
                        placementCount++
                        placeable.place(0, 0)
                    }
                }
        }

        rule.runOnIdle {
            assertThat(measureCount).isEqualTo(1)
            assertThat(placementCount).isEqualTo(0)
        }
    }

    @Test
    fun placementIsNotCalledOnChildOfNotPlacedParent() {
        val counterState = mutableStateOf(0)
        val shouldPlaceState = mutableStateOf(true)
        var placementCount = 0
        rule.setContent {
            Layout(
                content = {
                    Layout(
                        content = {
                            Layout { _, _ ->
                                counterState.value
                                layout(50, 50) { placementCount++ }
                            }
                        }
                    ) { measurables, constraints ->
                        // this parent is always placing a child
                        val placeable = measurables.first().measure(constraints)
                        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                val shouldPlace = shouldPlaceState.value
                layout(placeable.width, placeable.height) {
                    // this parent is placing a child conditionally
                    if (shouldPlace) {
                        placeable.place(0, 0)
                    }
                }
            }
        }

        rule.runOnIdle {
            placementCount = 0
            // we trigger remeasurement for the leaf node
            counterState.value++

            // and we also trigger remeasurement for the grand-parent
            // during this remeasurement forceMeasureTheSubtree will reach the leaf node
            // via the forceMeasureTheSubtree, however it shouldn't run its placement block
            // as the leaf node will not be placed in the end
            shouldPlaceState.value = false
        }

        rule.runOnIdle { assertThat(placementCount).isEqualTo(0) }
    }

    @Test
    fun placementIsNotCalledOnChildOfNotPlacedParentForLookahead() {
        val counterState = mutableStateOf(0)
        val shouldPlaceState = mutableStateOf(true)
        var placementCount = 0
        rule.setContent {
            LookaheadScope {
                Layout(
                    content = {
                        Layout(
                            content = {
                                Layout { _, _ ->
                                    counterState.value
                                    layout(50, 50) { placementCount++ }
                                }
                            }
                        ) { measurables, constraints ->
                            // this parent is always placing a child
                            val placeable = measurables.first().measure(constraints)
                            layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    val shouldPlace = shouldPlaceState.value
                    layout(placeable.width, placeable.height) {
                        // this parent is placing a child conditionally
                        if (shouldPlace) {
                            placeable.place(0, 0)
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            placementCount = 0
            // we trigger remeasurement for the leaf node
            counterState.value++

            // and we also trigger remeasurement for the grand-parent
            // during this remeasurement forceMeasureTheSubtree will reach the leaf node
            // via the forceMeasureTheSubtree, however it shouldn't run its placement block
            // as the leaf node will not be placed in the end
            shouldPlaceState.value = false
        }

        rule.runOnIdle { assertThat(placementCount).isEqualTo(0) }
    }

    @Test
    fun forceMeasureTheSubtreeSkipsNodesMeasuringInLayoutBlock() {
        val remeasurings = mutableListOf<Int>()
        val root = root {
            runDuringMeasure(once = false) { remeasurings.add(0) }
            add(
                node {
                    measureInLayoutBlock()
                    runDuringMeasure(once = false) { remeasurings.add(1) }
                    add(
                        node {
                            runDuringMeasure(once = false) { remeasurings.add(2) }
                            size = 10
                        }
                    )
                }
            )
            add(node { runDuringMeasure(once = false) { remeasurings.add(3) } })
        }

        val delegate = createDelegate(root)

        remeasurings.clear()
        root.requestRemeasure() // node with index 0
        root.first.first.requestRemeasure() // node with index 2
        root.second.requestRemeasure() // node with index 3
        delegate.measureAndLayout()

        assertThat(remeasurings).isEqualTo(listOf(0, 3, 2))
    }

    @Test
    fun forceMeasureTheSubtreeDoesntRelayoutWhenParentsSizeChanges() {
        val order = mutableListOf<Int>()
        val root = root {
            runDuringMeasure(once = false) { order.add(0) }
            runDuringLayout(once = false) { order.add(1) }
            add(
                node {
                    runDuringMeasure(once = false) { order.add(2) }
                    runDuringLayout(once = false) { order.add(3) }
                    add(
                        node {
                            runDuringMeasure(once = false) { order.add(6) }
                            runDuringLayout(once = false) { order.add(7) }
                            size = 10
                        }
                    )
                }
            )
            add(
                node {
                    runDuringMeasure(once = false) { order.add(4) }
                    runDuringLayout(once = false) { order.add(5) }
                }
            )
        }

        val delegate = createDelegate(root)

        order.clear()
        root.requestRemeasure() // node with indexes 0 and 1
        root.first.first.size = 20
        root.first.first.requestRemeasure() // node with indexes 6 and 7
        root.second.requestRemeasure() // node with indexes 4 and 5
        delegate.measureAndLayout()

        assertThat(order)
            .isEqualTo(
                listOf(
                    0, // remeasure root
                    6, // force remeasure root.first.first, it will change the size
                    2, // remeasure root.first because the size changed
                    4, // remeasure root.second
                    1, // relayout root
                    3, // relayout root.first
                    7, // relayout root.first.first
                    5, // relayout root.second
                )
            )
    }

    @Test
    fun invalidatePlacementMutableStateChange() {
        var placeChild by mutableStateOf(false)
        var placeCount = 0
        var childPlaceCount = 0
        rule.setContent {
            Box(
                Modifier.layout { measurable, constraints ->
                        val p = measurable.measure(constraints)
                        layout(p.width, p.height) {
                            placeCount++
                            if (placeChild) {
                                childPlaceCount++
                                p.place(0, 0)
                            }
                        }
                    }
                    .size(10.dp)
                    .background(Color.White)
            ) {
                Box(Modifier.size(5.dp).background(Color.Green))
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertThat(placeCount).isEqualTo(1)
            assertThat(childPlaceCount).isEqualTo(0)
            placeChild = true
        }
        rule.runOnIdle {
            assertThat(placeCount).isEqualTo(2)
            assertThat(childPlaceCount).isEqualTo(1)
        }
    }

    @Test
    fun remeasureNotPlacedChildMeasuredInPlacement() {
        lateinit var coordinates: LayoutCoordinates
        var childHeight by mutableIntStateOf(0)
        rule.setContent {
            Layout(
                {
                    Box(
                        Modifier.layout { m, c ->
                            val h = childHeight
                            val p = m.measure(c.copy(minHeight = h, maxHeight = h))
                            layout(p.width, h) { if (h != 0) p.place(0, 0) }
                        }
                    )
                    Box(Modifier.size(100.dp).onPlaced { coordinates = it })
                },
                Modifier.fillMaxSize(),
            ) { measurables, constraints ->
                val layoutWidth = constraints.maxWidth
                val layoutHeight = constraints.maxHeight
                layout(layoutWidth, layoutHeight) {
                    val upperPlaceable = measurables[0].measure(Constraints())
                    val lowerPlaceable = measurables[1].measure(Constraints())
                    upperPlaceable.place(0, 0)
                    lowerPlaceable.place(0, upperPlaceable.height)
                }
            }
        }
        rule.runOnIdle { childHeight = 200 }
        rule.runOnIdle { assertThat(coordinates.positionInParent().y.roundToInt()).isEqualTo(200) }
    }

    @Test
    fun remeasureNotPlacedChildMeasuredInPlacementInLookahead() {
        lateinit var coordinates: LayoutCoordinates
        var childHeight by mutableIntStateOf(0)
        rule.setContent {
            LookaheadScope {
                Layout(
                    {
                        Box(
                            Modifier.layout { m, c ->
                                if (isLookingAhead) {
                                    val h = childHeight
                                    val p = m.measure(c.copy(minHeight = h, maxHeight = h))
                                    layout(p.width, h) { if (h != 0) p.place(0, 0) }
                                } else {
                                    val p = m.measure(c)
                                    layout(p.width, p.height) {}
                                }
                            }
                        )
                        Box(
                            Modifier.size(100.dp).layout { m, c ->
                                val p = m.measure(c)
                                layout(p.width, p.height) {
                                    if (isLookingAhead && this.coordinates != null) {
                                        coordinates = this.coordinates!!
                                    }
                                    p.place(0, 0)
                                }
                            }
                        )
                    },
                    Modifier.fillMaxSize(),
                ) { measurables, constraints ->
                    val layoutWidth = constraints.maxWidth
                    val layoutHeight = constraints.maxHeight
                    layout(layoutWidth, layoutHeight) {
                        val upperPlaceable = measurables[0].measure(Constraints())
                        val lowerPlaceable = measurables[1].measure(Constraints())
                        upperPlaceable.place(0, 0)
                        lowerPlaceable.place(0, upperPlaceable.height)
                    }
                }
            }
        }
        rule.runOnIdle { childHeight = 200 }
        rule.runOnIdle { assertThat(coordinates.positionInParent().y.roundToInt()).isEqualTo(200) }
    }
}

private val UseChildSizeButNotPlace =
    object : LayoutNode.NoIntrinsicsMeasurePolicy("") {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints,
        ): MeasureResult {
            val placeable = measurables.first().measure(constraints)
            return layout(placeable.width, placeable.height) {
                // do not place
            }
        }
    }
