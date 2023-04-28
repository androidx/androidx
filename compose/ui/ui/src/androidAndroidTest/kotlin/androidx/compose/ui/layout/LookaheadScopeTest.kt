/*
 * Copyright 2022 The Android Open Source Project
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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.SpaceAround
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import java.lang.Integer.max
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.launch
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val Debug = false

@MediumTest
@RunWith(AndroidJUnit4::class)
class LookaheadScopeTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    @Test
    fun randomLookaheadPlacementOrder() {
        val nodeList = List(10) { node() }
        val placementOrder = (0..9).toMutableList()
        fun generateRandomPlaceOrder() {
            repeat(9) {
                val swapId = Random.nextInt(it, 10)
                val tmp = placementOrder[it]
                placementOrder[it] = placementOrder[swapId]
                placementOrder[swapId] = tmp
            }
        }

        val root = node {
            add(LayoutNode(isVirtual = true).apply {
                isVirtualLookaheadRoot = true
                add(node {
                    generateRandomPlaceOrder()
                    add(nodeList[0])
                    add(LayoutNode(isVirtual = true).apply {
                        repeat(4) {
                            add(nodeList[it + 1])
                        }
                    })
                    add(LayoutNode(isVirtual = true).apply {
                        repeat(5) {
                            add(nodeList[5 + it])
                        }
                    })
                    measurePolicy = MeasurePolicy { measurables, constraints ->
                        assertEquals(10, measurables.size)
                        val placeables = measurables.fastMap { it.measure(constraints) }
                        assertEquals(10, placeables.size)
                        layout(100, 100) {
                            placementOrder.fastForEach { id ->
                                placeables[id].place(0, 0)
                            }
                        }
                    }
                })
            }
            )
        }
        val delegate = createDelegate(root)
        repeat(5) {
            placementOrder.fastForEachIndexed { placeOrder, nodeId ->
                assertEquals(placeOrder, nodeList[nodeId].lookaheadPassDelegate!!.placeOrder)
                assertEquals(placeOrder, nodeList[nodeId].measurePassDelegate.placeOrder)
            }
            generateRandomPlaceOrder()
            root.children[0].requestLookaheadRemeasure()
            delegate.measureAndLayout()
        }
    }

    @Test
    fun defaultIntermediateMeasurePolicyInSubcomposeLayout() {
        val expectedSizes = listOf(
            IntSize(200, 100),
            IntSize(400, 300),
            IntSize(100, 500),
            IntSize(20, 5),
            IntSize(90, 120)
        )
        val targetSize = IntSize(260, 350)
        var actualSize by mutableStateOf(IntSize.Zero)
        var actualTargetSize by mutableStateOf(IntSize.Zero)
        var iteration by mutableStateOf(0)

        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                SubcomposeLayout(
                    Modifier
                        .requiredSize(targetSize.width.dp, targetSize.height.dp)
                        .intermediateLayout { measurable, _ ->
                            val intermediateConstraints = Constraints.fixed(
                                expectedSizes[iteration].width,
                                expectedSizes[iteration].height
                            )
                            measurable
                                .measure(intermediateConstraints)
                                .run {
                                    layout(width, height) { place(0, 0) }
                                }
                        }) { constraints ->
                    val placeable = subcompose(0) {
                        Box(Modifier.fillMaxSize())
                    }[0].measure(constraints)
                    val size = placeable.run { IntSize(width, height) }
                    if (this is SubcomposeIntermediateMeasureScope) {
                        actualSize = size
                    } else {
                        actualTargetSize = size
                    }
                    layout(size.width, size.height) {
                        placeable.place(0, 0)
                    }
                }
            }
        }

        repeat(5) {
            rule.runOnIdle {
                assertEquals(targetSize, actualTargetSize)
                assertEquals(expectedSizes[iteration], actualSize)
                if (iteration < 4) {
                    iteration++
                }
            }
        }
    }

    @Test
    fun lookaheadLayoutAnimation() {
        var isLarge by mutableStateOf(true)
        var size1 = IntSize.Zero
        var size2 = IntSize.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Row(if (isLarge) Modifier.size(200.dp) else Modifier.size(50.dp, 100.dp)) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(2f)
                                .onSizeChanged {
                                    size1 = it
                                }
                                .animateSize())
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(3f)
                                .onSizeChanged {
                                    size2 = it
                                }
                                .animateSize())
                    }
                }
            }
        }
        // Check that:
        // 1) size changes happen when parent constraints change,
        // 2) animations finish and actual measurements get updated by animation,
        // 3) during the animation the tree is consistent.
        rule.runOnIdle {
            assertEquals(IntSize(80, 200), size1)
            assertEquals(IntSize(120, 200), size2)
            isLarge = false
        }
        rule.runOnIdle {
            assertEquals(IntSize(20, 100), size1)
            assertEquals(IntSize(30, 100), size2)
            isLarge = true
        }
        rule.runOnIdle {
            assertEquals(IntSize(80, 200), size1)
            assertEquals(IntSize(120, 200), size2)
        }
    }

    private fun Modifier.animateSize(): Modifier = composed {
        var anim: Animatable<IntSize, AnimationVector2D>? by remember { mutableStateOf(null) }
        this.intermediateLayout { measurable, _ ->
            anim = anim?.apply {
                launch {
                    if (lookaheadSize != targetValue) {
                        animateTo(lookaheadSize, tween(200))
                    }
                }
            } ?: Animatable(lookaheadSize, IntSize.VectorConverter)
            val (width, height) = anim!!.value
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        }
    }

    @Test
    fun nestedLookaheadLayoutTest() {
        var parentLookaheadMeasure = 0
        var childLookaheadMeasure = 0
        var parentLookaheadPlace = 0
        var childLookaheadPlace = 0
        var parentMeasure = 0
        var childMeasure = 0
        var parentPlace = 0
        var childPlace = 0

        var rootPreMeasure = 0
        var rootPrePlace = 0
        var rootPostMeasure = 0
        var rootPostPlace = 0

        var counter = 0

        rule.setContent {
            // The right sequence for this nested lookahead layout setup:
            // parentLookaheadMeasure -> childLookaheadMeasure -> parentMeasure -> childMeasure
            // -> parentLookaheadPlace -> childLookaheadPlace -> -> parentPlace -> childPlace
            // Each event should happen exactly once in the end.
            Box(Modifier.layout(
                measureWithLambdas(
                    preMeasure = { rootPreMeasure = ++counter },
                    postMeasure = { rootPostMeasure = ++counter },
                    prePlacement = { rootPrePlace = ++counter },
                    postPlacement = { rootPostPlace = ++counter }
                )
            )) {
                MyLookaheadLayout {
                    Box(
                        Modifier
                            .padding(top = 100.dp)
                            .fillMaxSize()
                            .intermediateLayout { measurable, constraints ->
                                measureWithLambdas(
                                    preMeasure = { parentMeasure = ++counter },
                                    prePlacement = { parentPlace = ++counter }
                                ).invoke(this, measurable, constraints)
                            }
                            .layout(
                                measureWithLambdas(
                                    preMeasure = {
                                        if (parentLookaheadMeasure == 0) {
                                            // Only the first invocation is for lookahead
                                            parentLookaheadMeasure = ++counter
                                        }
                                    },
                                    prePlacement = {
                                        if (parentLookaheadPlace == 0) {
                                            // Only the first invocation is for lookahead
                                            parentLookaheadPlace = ++counter
                                        }
                                    }
                                )
                            )
                    ) {
                        MyLookaheadLayout {
                            Column {
                                Box(
                                    Modifier
                                        .size(100.dp)
                                        .background(Color.Red)
                                        .intermediateLayout { measurable, constraints ->
                                            measureWithLambdas(
                                                preMeasure = { childMeasure = ++counter },
                                                prePlacement = { childPlace = ++counter }
                                            ).invoke(this, measurable, constraints)
                                        }
                                        .layout(
                                            measure = measureWithLambdas(
                                                preMeasure = {
                                                    if (childLookaheadMeasure == 0) {
                                                        childLookaheadMeasure = ++counter
                                                    }
                                                },
                                                prePlacement = {
                                                    if (childLookaheadPlace == 0) {
                                                        childLookaheadPlace = ++counter
                                                    }
                                                }
                                            )
                                        )
                                )
                                Box(
                                    Modifier
                                        .size(100.dp)
                                        .background(Color.Green)
                                )
                            }
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(1, rootPreMeasure)
            assertEquals(2, parentLookaheadMeasure)
            assertEquals(3, childLookaheadMeasure)
            assertEquals(4, parentMeasure)
            assertEquals(5, childMeasure)
            assertEquals(6, rootPostMeasure)

            // Measure finished. Then placement.
            assertEquals(7, rootPrePlace)
            assertEquals(8, parentLookaheadPlace)
            assertEquals(9, childLookaheadPlace)
            assertEquals(10, parentPlace)
            assertEquals(11, childPlace)
            assertEquals(12, rootPostPlace)
        }
    }

    @Test
    fun parentObserveActualMeasurementTest() {
        val width = 200
        val height = 120
        var scaleFactor by mutableStateOf(0.1f)
        var parentSize = IntSize.Zero
        var grandParentSize = IntSize.Zero
        var greatGrandParentSize = IntSize.Zero
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Column(
                    Modifier.layout(measureWithLambdas(postMeasure = { greatGrandParentSize = it }))
                ) {
                    Row(
                        Modifier.layout(measureWithLambdas(postMeasure = { grandParentSize = it }))
                    ) {
                        Box(
                            Modifier.layout(measureWithLambdas(postMeasure = { parentSize = it }))
                        ) {
                            MyLookaheadLayout {
                                Box(modifier = Modifier
                                    .intermediateLayout { measurable, constraints ->
                                        assertEquals(width, lookaheadSize.width)
                                        assertEquals(height, lookaheadSize.height)
                                        val placeable = measurable.measure(constraints)
                                        layout(
                                            (scaleFactor * width).roundToInt(),
                                            (scaleFactor * height).roundToInt()
                                        ) {
                                            placeable.place(0, 0)
                                        }
                                    }
                                    .size(width.dp, height.dp))
                            }
                        }
                        Spacer(modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.size(50.dp))
                }
            }
        }

        val size = IntSize(width, height)
        repeat(20) {
            rule.runOnIdle {
                assertEquals(size * scaleFactor, parentSize)
                assertEquals((size * scaleFactor).width + 20, grandParentSize.width)
                assertEquals(max((size * scaleFactor).height, 20), grandParentSize.height)
                assertEquals(max(grandParentSize.width, 50), greatGrandParentSize.width)
                assertEquals(grandParentSize.height + 50, greatGrandParentSize.height)
                scaleFactor += 0.1f
            }
        }
    }

    private operator fun IntSize.times(multiplier: Float): IntSize =
        IntSize((width * multiplier).roundToInt(), (height * multiplier).roundToInt())

    @Test
    fun noExtraLookaheadTest() {
        var parentMeasure = 0
        var parentPlace = 0
        var measurePlusLookahead = 0
        var placePlusLookahead = 0
        var measure = 0
        var place = 0

        var isSmall by mutableStateOf(true)
        var controlGroupEnabled by mutableStateOf(true)

        var controlGroupParentMeasure = 0
        var controlGroupParentPlace = 0
        var controlGroupMeasure = 0
        var controlGroupPlace = 0

        rule.setContent {
            if (controlGroupEnabled) {
                Box(
                    Modifier.layout(
                        measureWithLambdas(
                            postMeasure = { controlGroupParentMeasure++ },
                            postPlacement = { controlGroupParentPlace++ }
                        )
                    )
                ) {
                    Layout(measurePolicy = defaultMeasurePolicy, content = {
                        Box(
                            Modifier
                                .size(if (isSmall) 100.dp else 200.dp)
                                .layout(
                                    measureWithLambdas(
                                        postMeasure = { controlGroupMeasure++ },
                                        postPlacement = { controlGroupPlace++ },
                                    )
                                )
                        )
                    })
                }
            } else {
                Box(
                    Modifier.layout(
                        measureWithLambdas(
                            postMeasure = { parentMeasure++ },
                            postPlacement = { parentPlace++ }
                        )
                    )
                ) {
                    MyLookaheadLayout {
                        Box(
                            Modifier
                                .size(if (isSmall) 100.dp else 200.dp)
                                .animateSize()
                                .layout(
                                    measureWithLambdas(
                                        postMeasure = { measurePlusLookahead++ },
                                        postPlacement = { placePlusLookahead++ },
                                    )
                                )
                                .intermediateLayout { measurable, constraints ->
                                    measureWithLambdas(
                                        postMeasure = { measure++ },
                                        postPlacement = { place++ }
                                    ).invoke(this, measurable, constraints)
                                }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(1, controlGroupParentMeasure)
            assertEquals(1, controlGroupParentPlace)
            assertEquals(1, controlGroupMeasure)
            assertEquals(1, controlGroupPlace)
            isSmall = !isSmall
        }

        rule.runOnIdle {
            // Check the starting condition before switching over from control group
            assertEquals(0, parentMeasure)
            assertEquals(0, parentPlace)
            assertEquals(0, measurePlusLookahead)
            assertEquals(0, placePlusLookahead)
            assertEquals(0, measure)
            assertEquals(0, place)

            // Switch to LookaheadLayout
            controlGroupEnabled = !controlGroupEnabled
        }

        rule.runOnIdle {
            // Expects 1
            assertEquals(1, parentMeasure)
            assertEquals(1, parentPlace)
            val lookaheadMeasure = measurePlusLookahead - measure
            val lookaheadPlace = placePlusLookahead - place
            assertEquals(1, lookaheadMeasure)
            assertEquals(1, lookaheadPlace)
        }

        // Pump frames so that animation triggered measurements are not completely dependent on
        // system timing.
        rule.mainClock.autoAdvance = false
        rule.runOnIdle {
            isSmall = !isSmall
        }
        repeat(10) {
            rule.mainClock.advanceTimeByFrame()
            rule.waitForIdle()
        }
        rule.mainClock.autoAdvance = true
        rule.runOnIdle {
            // Compare number of lookahead measurements & placements with control group.
            assertEquals(controlGroupParentMeasure, parentMeasure)
            assertEquals(controlGroupParentPlace, parentPlace)
            val lookaheadMeasure = measurePlusLookahead - measure
            val lookaheadPlace = placePlusLookahead - place
            assertEquals(controlGroupMeasure, lookaheadMeasure)
            assertEquals(controlGroupPlace, lookaheadPlace)
            assertTrue(lookaheadMeasure < measure)
            assertTrue(lookaheadPlace < place)
        }
    }

    @Test
    fun defaultMeasurePolicyInSubcomposeLayout() {
        var actualLookaheadSize by mutableStateOf(IntSize.Zero)
        var defaultIntermediateMeasureSize by mutableStateOf(IntSize.Zero)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    SubcomposeLayout(
                        Modifier
                            .fillMaxSize()
                            .requiredSize(200.dp),
                        intermediateMeasurePolicy = { constraints ->
                            measurablesForSlot(Unit)[0].measure(constraints)
                            actualLookaheadSize = this.lookaheadSize
                            layout(0, 0) {}
                        }
                    ) { constraints ->
                        val placeable = subcompose(Unit) {
                            Box(Modifier.requiredSize(400.dp, 600.dp))
                        }[0].measure(constraints)
                        layout(500, 300) {
                            placeable.place(0, 0)
                        }
                    }
                    SubcomposeLayout(
                        Modifier
                            .size(150.dp)
                            .intermediateLayout { measurable, _ ->
                                measurable
                                    .measure(Constraints(0, 2000, 0, 2000))
                                    .run {
                                        defaultIntermediateMeasureSize = IntSize(width, height)
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }
                    ) { constraints ->
                        val placeable = subcompose(Unit) {
                            Box(Modifier.requiredSize(400.dp, 600.dp))
                        }[0].measure(constraints)
                        layout(500, 300) {
                            placeable.place(0, 0)
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(IntSize(500, 300), actualLookaheadSize)
            assertEquals(IntSize(500, 300), defaultIntermediateMeasureSize)
        }
    }

    @Test
    fun lookaheadStaysTheSameDuringAnimationTest() {
        var isLarge by mutableStateOf(true)
        var parentLookaheadSize = IntSize.Zero
        var child1LookaheadSize = IntSize.Zero
        var child2LookaheadSize = IntSize.Zero
        rule.setContent {
            LookaheadScope {
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Row(
                        (if (isLarge) Modifier.size(200.dp) else Modifier.size(50.dp, 100.dp))
                            .intermediateLayout { measurable, constraints ->
                                parentLookaheadSize = lookaheadSize
                                measureWithLambdas().invoke(this, measurable, constraints)
                            }
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(2f)
                                .intermediateLayout { measurable, constraints ->
                                    child1LookaheadSize = lookaheadSize
                                    measureWithLambdas().invoke(this, measurable, constraints)
                                }
                                .animateSize()
                        )
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .weight(3f)
                                .intermediateLayout { measurable, constraints ->
                                    child2LookaheadSize = lookaheadSize
                                    measureWithLambdas().invoke(this, measurable, constraints)
                                }
                                .animateSize()
                        )
                    }
                }
            }
        }
        rule.waitForIdle()
        rule.runOnIdle {
            assertEquals(IntSize(200, 200), parentLookaheadSize)
            assertEquals(IntSize(80, 200), child1LookaheadSize)
            assertEquals(IntSize(120, 200), child2LookaheadSize)
            rule.mainClock.autoAdvance = false
            isLarge = false
        }

        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        repeat(10) {
            rule.runOnIdle {
                assertEquals(IntSize(50, 100), parentLookaheadSize)
                assertEquals(IntSize(20, 100), child1LookaheadSize)
                assertEquals(IntSize(30, 100), child2LookaheadSize)
            }
            rule.mainClock.advanceTimeByFrame()
        }
        rule.runOnIdle {
            isLarge = true
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()

        repeat(10) {
            rule.runOnIdle {
                assertEquals(IntSize(200, 200), parentLookaheadSize)
                assertEquals(IntSize(80, 200), child1LookaheadSize)
                assertEquals(IntSize(120, 200), child2LookaheadSize)
            }
            rule.mainClock.advanceTimeByFrame()
        }
    }

    @Test
    fun skipPlacementOnlyPostLookahead() {
        var child1TotalPlacement = 0
        var child1Placement = 0
        var child2TotalPlacement = 0
        var child2Placement = 0

        rule.setContent {
            MyLookaheadLayout {
                Row(Modifier.widthIn(100.dp, 200.dp)) {
                    Box(
                        modifier = Modifier
                            .intermediateLayout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) {
                                    // skip placement in the post-lookahead placement pass
                                }
                            }
                            .weight(1f)
                            .layout { measurable, constraints ->
                                measureWithLambdas(
                                    prePlacement = { child1TotalPlacement++ }
                                ).invoke(this, measurable, constraints)
                            }
                            .intermediateLayout { measurable, constraints ->
                                measureWithLambdas(prePlacement = { child1Placement++ })
                                    .invoke(this, measurable, constraints)
                            }
                    )
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                measureWithLambdas(
                                    prePlacement = { child2TotalPlacement++ }
                                ).invoke(this, measurable, constraints)
                            }
                            .intermediateLayout { measurable, constraints ->
                                measureWithLambdas(prePlacement = { child2Placement++ })
                                    .invoke(this, measurable, constraints)
                            }
                            .weight(3f)
                    )
                    Box(modifier = Modifier.sizeIn(50.dp))
                }
            }
        }

        rule.runOnIdle {
            // Child1 skips post-lookahead placement
            assertEquals(0, child1Placement)
            // Child2 is placed in post-lookahead placement
            assertEquals(1, child2Placement)
            val child1LookaheadPlacement = child1TotalPlacement - child1Placement
            val child2LookaheadPlacement = child2TotalPlacement - child2Placement
            // Both child1 & child2 should be placed in lookahead, since the skipping only
            // applies to regular placement pass, as per API contract in `intermediateLayout`
            assertEquals(1, child1LookaheadPlacement)
            assertEquals(1, child2LookaheadPlacement)
        }
    }

    @Composable
    private fun MyLookaheadLayout(
        modifier: Modifier = Modifier,
        postMeasure: () -> Unit = {},
        postPlacement: () -> Unit = {},
        content: @Composable LookaheadScope.() -> Unit
    ) {
        Box(modifier.layout { measurable, constraints ->
            measurable.measure(constraints).run {
                postMeasure()
                // Position the children.
                layout(width, height) {
                    place(0, 0)
                }.apply {
                    postPlacement()
                }
            }
        }) {
            LookaheadScope(content)
        }
    }

    @Test
    fun alterPlacementTest() {
        var placementCount = 0
        var totalPlacementCount = 0
        var shouldPlace by mutableStateOf(false)
        rule.setContent {
            MyLookaheadLayout {
                Layout(
                    content = {
                        Box(Modifier
                            .intermediateLayout { measurable, constraints ->
                                measureWithLambdas(prePlacement = {
                                    placementCount++
                                }).invoke(this, measurable, constraints)
                            }
                            .layout { measurable, constraints ->
                                measureWithLambdas(prePlacement = {
                                    totalPlacementCount++
                                }).invoke(this, measurable, constraints)
                            })
                    }
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val maxWidth: Int = placeables.maxOf { it.width }
                    val maxHeight = placeables.maxOf { it.height }
                    // Position the children.
                    layout(maxWidth, maxHeight) {
                        if (shouldPlace) {
                            placeables.forEach {
                                it.place(0, 0)
                            }
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(0, totalPlacementCount)
            assertEquals(0, placementCount)
            shouldPlace = true
        }
        rule.runOnIdle {
            val lookaheadPlacementCount = totalPlacementCount - placementCount
            assertEquals(1, lookaheadPlacementCount)
            assertEquals(1, placementCount)
        }
    }

    @Test
    fun localLookaheadPositionOfFromDisjointedLookaheadLayoutsTest() {
        var firstCoordinates: LayoutCoordinates? = null
        var secondCoordinates: LayoutCoordinates? = null
        fun LookaheadScope.assertEqualOffset() {
            val offset = secondCoordinates!!.localPositionOf(firstCoordinates!!, Offset.Zero)
            val lookaheadOffset = secondCoordinates!!.localLookaheadPositionOf(firstCoordinates!!)
            assertEquals(offset, lookaheadOffset)
        }
        rule.setContent {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LookaheadScope {
                    Box(
                        Modifier
                            .size(200.dp)
                            .onPlaced { coords ->
                                firstCoordinates = coords
                            })
                }
                Box(
                    Modifier
                        .padding(top = 30.dp, start = 70.dp)
                        .offset(40.dp, 60.dp)
                ) {
                    LookaheadScope {
                        Box(
                            Modifier
                                .size(100.dp, 50.dp)
                                .onPlaced { coords ->
                                    secondCoordinates = coords
                                    assertEqualOffset()
                                })
                    }
                }
            }
        }
    }

    @Test
    fun localLookaheadPositionOfFromNestedLookaheadLayoutsTest() {
        var firstCoordinates: LayoutCoordinates? = null
        var secondCoordinates: LayoutCoordinates? = null
        fun LookaheadScope.assertEqualOffset() {
            val offset = secondCoordinates!!.localPositionOf(firstCoordinates!!, Offset.Zero)
            val lookaheadOffset = secondCoordinates!!.localLookaheadPositionOf(firstCoordinates!!)
            assertEquals(offset, lookaheadOffset)
        }
        rule.setContent {
            MyLookaheadLayout {
                Row(
                    Modifier
                        .fillMaxSize()
                        .onPlaced { it -> firstCoordinates = it },
                    horizontalArrangement = SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(200.dp))
                    Box(
                        Modifier
                            .padding(top = 30.dp, start = 70.dp)
                            .offset(40.dp, 60.dp)
                    ) {
                        MyLookaheadLayout {
                            Box(
                                Modifier
                                    .size(100.dp, 50.dp)
                                    .onPlaced { it ->
                                        secondCoordinates = it
                                    })
                        }
                    }
                }
            }
        }
    }

    @Test
    fun lookaheadMaxHeightIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box(modifier) {
                Row(modifier.height(IntrinsicSize.Max)) {
                    Box(
                        modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .aspectRatio(2f)
                            .background(Color.Gray)
                    )
                    Box(
                        modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .width(1.dp)
                            .background(Color.Black)
                    )
                    Box(
                        modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(Color.Blue)
                    )
                }
            }
        }
    }

    @Test
    fun lookaheadMinHeightIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box {
                Row(modifier.height(IntrinsicSize.Min)) {
                    Text(
                        text = "This is a really short text",
                        modifier = modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                    Box(
                        modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.Black)
                    )
                    Text(
                        text = "This is a much much much much much much much much much much" +
                            " much much much much much much longer text",
                        modifier = modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    @Test
    fun lookaheadMinWidthIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Column(
                modifier
                    .width(IntrinsicSize.Min)
                    .wrapContentHeight()
            ) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .size(20.dp, 10.dp)
                        .background(Color.Gray)
                )
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .size(30.dp, 10.dp)
                        .background(Color.Blue)
                )
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .size(10.dp, 10.dp)
                        .background(Color.Magenta)
                )
            }
        }
    }

    @Test
    fun lookaheadMaxWidthIntrinsicsTest() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box {
                Column(
                    modifier
                        .width(IntrinsicSize.Max)
                        .wrapContentHeight()
                ) {
                    Box(
                        modifier
                            .fillMaxWidth()
                            .background(Color.Gray)
                    ) {
                        Text("Short text")
                    }
                    Box(
                        modifier
                            .fillMaxWidth()
                            .background(Color.Blue)
                    ) {
                        Text("Extremely long text giving the width of its siblings")
                    }
                    Box(
                        modifier
                            .fillMaxWidth()
                            .background(Color.Magenta)
                    ) {
                        Text("Medium length text")
                    }
                }
            }
        }
    }

    @Test
    fun intermediateLayoutMaxHeightIntrinsicsTest() {
        var rowHeight: Int by mutableStateOf(0)
        var fraction: Float by mutableStateOf(0f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Row(
                        Modifier
                            .height(IntrinsicSize.Max)
                            .onGloballyPositioned {
                                rowHeight = it.size.height
                            }
                            .fillMaxWidth()
                    ) {
                        Box(
                            Modifier
                                .intermediateLayout { measurable, constraints ->
                                    measurable
                                        .measure(constraints)
                                        .run {
                                            layout(
                                                lookaheadSize.width,
                                                (lookaheadSize.height * fraction).roundToInt()
                                            ) {
                                                place(0, 0)
                                            }
                                        }
                                }
                                .width(5.dp)
                                .requiredHeight(300.dp))
                        Box(
                            Modifier
                                .requiredHeight(20.dp)
                                .weight(1f)
                        )
                    }
                }
            }
        }

        repeat(11) {
            fraction = 0.1f * it
            rule.runOnIdle {
                val expectedHeight = max((300 * fraction).toInt(), 20)
                assertEquals(expectedHeight, rowHeight)
            }
        }
    }

    @Test
    fun intermediateLayoutMinHeightIntrinsicsTest() {
        var rowHeight: Int by mutableStateOf(0)
        var fraction: Float by mutableStateOf(0f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Row(
                        Modifier
                            .height(IntrinsicSize.Min)
                            .onGloballyPositioned {
                                rowHeight = it.size.height
                            }
                            .fillMaxWidth()
                    ) {
                        Box(
                            Modifier
                                .intermediateLayout { measurable, constraints ->
                                    measurable
                                        .measure(constraints)
                                        .run {
                                            layout(
                                                lookaheadSize.width,
                                                (lookaheadSize.height * fraction).roundToInt()
                                            ) {
                                                place(0, 0)
                                            }
                                        }
                                }
                                .width(5.dp)
                                .requiredHeight(300.dp))
                        Box(
                            Modifier
                                .requiredHeight(20.dp)
                                .weight(1f)
                        )
                    }
                }
            }
        }

        repeat(11) {
            fraction = 0.1f * it
            rule.runOnIdle {
                val expectedHeight = max((300 * fraction).toInt(), 20)
                assertEquals(expectedHeight, rowHeight)
            }
        }
    }

    @Test
    fun intermediateLayoutMinWidthIntrinsicsTest() {
        var rowWidth: Int by mutableStateOf(0)
        var fraction: Float by mutableStateOf(0f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Column(
                        Modifier
                            .width(IntrinsicSize.Min)
                            .onGloballyPositioned {
                                rowWidth = it.size.width
                            }
                            .height(50.dp)
                    ) {
                        Box(
                            Modifier
                                .intermediateLayout { measurable, constraints ->
                                    measurable
                                        .measure(constraints)
                                        .run {
                                            layout(
                                                (lookaheadSize.width * fraction).roundToInt(),
                                                lookaheadSize.height
                                            ) {
                                                place(0, 0)
                                            }
                                        }
                                }
                                .requiredWidth(300.dp))
                        Box(
                            Modifier
                                .requiredWidth(120.dp)
                                .height(10.dp)
                        )
                        Text(
                            text =
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec" +
                                " non felis euismod nunc commodo pharetra a nec eros. Sed varius," +
                                " metus sed facilisis condimentum, orci orci aliquet arcu",
                            Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        repeat(11) {
            fraction = 0.1f * it
            rule.runOnIdle {
                val expectedWidth = max((300 * fraction).toInt(), 120)
                assertEquals(expectedWidth, rowWidth)
            }
        }
    }

    @Test
    fun intermediateLayoutMaxWidthIntrinsicsTest() {
        var boxSize: IntSize by mutableStateOf(IntSize.Zero)
        var childBoxSize: IntSize by mutableStateOf(IntSize.Zero)
        var fraction: Float by mutableStateOf(0f)
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Box(
                        Modifier
                            .height(50.dp)
                            .width(IntrinsicSize.Max)
                            .onGloballyPositioned {
                                boxSize = it.size
                            }) {
                        Box(
                            Modifier
                                .intermediateLayout { measurable, constraints ->
                                    measurable
                                        .measure(constraints)
                                        .run {
                                            layout(
                                                (lookaheadSize.width * fraction).roundToInt(),
                                                lookaheadSize.height
                                            ) {
                                                place(0, 0)
                                            }
                                        }
                                }
                                .requiredWidth(100.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .onGloballyPositioned {
                                    childBoxSize = it.size
                                })
                    }
                }
            }
        }

        repeat(11) {
            fraction = 0.1f * it
            rule.runOnIdle {
                assertEquals(IntSize((100 * fraction).toInt(), 50), boxSize)
                assertEquals(IntSize((100 * fraction).toInt(), 10), childBoxSize)
            }
        }
    }

    @Test
    fun firstBaselineAlignmentInLookaheadLayout() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Box(modifier.fillMaxWidth()) {
                Row {
                    Text("Short", modifier.alignByBaseline())
                    Text("3\nline\n\text", modifier.alignByBaseline())
                    Text(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do" +
                            " eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim" +
                            " ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut" +
                            " aliquip ex ea commodo consequat. Duis aute irure dolor in" +
                            " reprehenderit in voluptate velit esse cillum dolore eu fugiat" +
                            " nulla pariatur. Excepteur sint occaecat cupidatat non proident," +
                            " sunt in culpa qui officia deserunt mollit anim id est laborum.",
                        modifier.alignByBaseline()
                    )
                }
            }
        }
    }

    @Test
    fun grandparentQueryBaseline() {
        assertSameLayoutWithAndWithoutLookahead { modifier ->
            Layout(modifier = modifier, content = {
                Row(
                    modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color(0xffb4c8ea)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "First",
                        fontSize = 80.sp,
                        color = Color.White,
                        modifier = modifier
                            .alignByBaseline()
                            .background(color = Color(0xfff3722c), RoundedCornerShape(10))
                    )
                    Spacer(modifier.size(10.dp))
                    Text(
                        text = "Second",
                        color = Color.White,
                        fontSize = 30.sp,
                        modifier = modifier
                            .alignByBaseline()
                            .background(color = Color(0xff90be6d), RoundedCornerShape(10))
                    )
                    Spacer(modifier.size(10.dp))
                    Text(
                        text = "Text",
                        fontSize = 50.sp,
                        color = Color.White,
                        modifier = modifier
                            .alignByBaseline()
                            .background(color = Color(0xffffb900), RoundedCornerShape(10))
                    )
                }
                Spacer(
                    modifier
                        .fillMaxWidth()
                        .requiredHeight(1.dp)
                        .background(Color.Black)
                )
            }) { measurables, constraints ->
                val placeables = measurables.map {
                    it.measure(constraints)
                }
                val row = placeables.first()
                val position = row[FirstBaseline]
                layout(row.width, row.height) {
                    row.place(0, 0)
                    placeables[1].place(0, position)
                }
            }
        }
    }

    @Test
    fun lookaheadLayoutTransformFrom() {
        val matrix = Matrix()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LookaheadScope {
                    Layout(
                        measurePolicy = { measurables, constraints ->
                            val placeable = measurables[0].measure(constraints)
                            // Position the children.
                            layout(placeable.width + 10, placeable.height + 10) {
                                placeable.place(10, 10)
                            }
                        },
                        content = {
                            Box(
                                Modifier
                                    .intermediateLayout { measurable, constraints ->
                                        measurable
                                            .measure(constraints)
                                            .run {
                                                layout(width, height) {
                                                    coordinates!!.transformFrom(
                                                        lookaheadScopeCoordinates,
                                                        matrix
                                                    )
                                                }
                                            }
                                    }
                                    .size(10.dp))
                        }
                    )
                }
            }
        }
        rule.waitForIdle()
        val posInChild = matrix.map(Offset(10f, 10f))
        assertEquals(Offset.Zero, posInChild)
    }

    @Test
    fun moveLookaheadScope() {
        var scopePositionInRoot by mutableStateOf(IntOffset(Int.MAX_VALUE, Int.MAX_VALUE))
        var firstBox by mutableStateOf(true)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box {
                    val movableContent = remember {
                        movableContentOf {
                            LookaheadScope {
                                Box(
                                    Modifier
                                        .layout { measurable, constraints ->
                                            measurable
                                                .measure(constraints)
                                                .run {
                                                    layout(width, height) {
                                                        scopePositionInRoot =
                                                            lookaheadScopeCoordinates
                                                                .localToRoot(Offset.Zero)
                                                                .round()
                                                        place(0, 0)
                                                    }
                                                }
                                        }
                                        .size(200.dp))
                            }
                        }
                    }

                    Box(Modifier.offset(100.dp, 5.dp)) {
                        if (firstBox) {
                            movableContent()
                        }
                    }
                    Box(Modifier.offset(40.dp, 200.dp)) {
                        if (!firstBox) {
                            movableContent()
                        }
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(IntOffset(100, 5), scopePositionInRoot)
        firstBox = false
        rule.waitForIdle()
        assertEquals(IntOffset(40, 200), scopePositionInRoot)
    }

    @Test
    fun moveIntermediateLayout() {
        var positionInScope by mutableStateOf(IntOffset(Int.MAX_VALUE, Int.MAX_VALUE))
        var boxId by mutableStateOf(1)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                val movableContent = remember {
                    movableContentOf {
                        Box(
                            Modifier
                                .intermediateLayout { measurable, constraints ->
                                    measurable
                                        .measure(constraints)
                                        .run {
                                            layout(width, height) {
                                                coordinates?.let {
                                                    positionInScope =
                                                        lookaheadScopeCoordinates
                                                            .localLookaheadPositionOf(
                                                                it
                                                            )
                                                            .round()
                                                }
                                            }
                                        }
                                }
                                .size(200.dp))
                    }
                }
                Box {
                    LookaheadScope {
                        Box(Modifier.offset(100.dp, 5.dp)) {
                            if (boxId == 1) {
                                movableContent()
                            }
                        }
                    }
                }
                Box(Modifier.offset(40.dp, 200.dp)) {
                    if (boxId == 2) {
                        movableContent()
                    }
                }
                Box(Modifier
                    .offset(50.dp, 50.dp)
                    .intermediateLayout { measurable, constraints ->
                        measurable
                            .measure(constraints)
                            .run {
                                layout(width, height) {
                                    place(0, 0)
                                }
                            }
                    }
                    .offset(60.dp, 60.dp)) {
                    if (boxId == 3) {
                        movableContent()
                    }
                }
            }
        }
        rule.waitForIdle()
        assertEquals(IntOffset(100, 5), positionInScope)
        boxId++
        rule.waitForIdle()
        // Expect no offset when moving intermediateLayout out of LookaheadScope, as the implicitly
        // created lookahead scope will have the same coordinates as intermediateLayout
        assertEquals(IntOffset(0, 0), positionInScope)
        boxId++
        rule.waitForIdle()
        // Expect the lookaheadScope to be created by the ancestor intermediateLayoutModifier
        assertEquals(IntOffset(60, 60), positionInScope)
    }

    @Test
    fun nestedVirtualNodes() {
        val root = node().also {
            createDelegate(it)
        }
        val virtualGrandParent = LayoutNode(isVirtual = true)
        val virtualParent = LayoutNode(isVirtual = true)
        val child = node()
        root.add(virtualGrandParent)
        virtualGrandParent.add(virtualParent)
        virtualParent.add(child)
        val newChild = node()
        rule.runOnIdle {
            assertEquals(1, root.children.size)
            assertEquals(1, virtualGrandParent.children.size)
            assertEquals(1, virtualParent.children.size)
            assertEquals(child, root.children[0])
            assertEquals(child, virtualGrandParent.children[0])
            assertEquals(child, virtualParent.children[0])
            // Add another child to virtual parent.
            virtualParent.add(newChild)
        }
        rule.runOnIdle {
            assertEquals(2, root.children.size)
            assertEquals(2, virtualGrandParent.children.size)
            assertEquals(2, virtualParent.children.size)
            assertEquals(child, root.children[0])
            assertEquals(child, virtualGrandParent.children[0])
            assertEquals(child, virtualParent.children[0])
            assertEquals(newChild, root.children[1])
            assertEquals(newChild, virtualGrandParent.children[1])
            assertEquals(newChild, virtualParent.children[1])
        }
    }

    @Test
    fun nestedLookaheadScope() {
        rule.setContent {
            Box(Modifier.offset(20.dp, 30.dp)) {
                LookaheadScope {
                    Box(
                        Modifier
                            .offset(50.dp, 25.dp)
                            .intermediateLayout { measurable, constraints ->
                                measureWithLambdas(prePlacement = {
                                    val outerLookaheadScopeCoords = with(this@LookaheadScope) {
                                        lookaheadScopeCoordinates
                                    }
                                    assertEquals(
                                        outerLookaheadScopeCoords,
                                        lookaheadScopeCoordinates
                                    )
                                })(measurable, constraints)
                            }
                            .offset(15.dp, 20.dp)
                    ) {
                        LookaheadScope {
                            val innerLookaheadScope = this
                            Box(
                                Modifier
                                    .intermediateLayout { measurable, constraints ->
                                        measureWithLambdas(prePlacement = {
                                            val innerLookaheadCoords = with(innerLookaheadScope) {
                                                lookaheadScopeCoordinates
                                            }
                                            assertEquals(
                                                innerLookaheadCoords,
                                                lookaheadScopeCoordinates
                                            )
                                        })(measurable, constraints)
                                    }
                                    .size(50.dp)
                                    .intermediateLayout { measurable, constraints ->
                                        measureWithLambdas(prePlacement = {
                                            val innerLookaheadCoords = with(innerLookaheadScope) {
                                                lookaheadScopeCoordinates
                                            }
                                            assertEquals(
                                                innerLookaheadCoords,
                                                lookaheadScopeCoordinates
                                            )
                                        })(measurable, constraints)
                                    }
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun lookaheadScopeInImplicitScope() {
        rule.setContent {
            Box(Modifier.offset(20.dp, 30.dp)) {
                Box(
                    Modifier
                        .offset(50.dp, 25.dp)
                        .intermediateLayout { measurable, constraints ->
                            measureWithLambdas(prePlacement = {
                                assertEquals(
                                    coordinates!!,
                                    lookaheadScopeCoordinates
                                )
                            })(measurable, constraints)
                        }
                        .offset(15.dp, 20.dp)
                ) {
                    LookaheadScope {
                        val explicitLookaheadScope = this
                        Box(
                            Modifier
                                .intermediateLayout { measurable, constraints ->
                                    measureWithLambdas(prePlacement = {
                                        val innerLookaheadCoords =
                                            with(explicitLookaheadScope) {
                                                lookaheadScopeCoordinates
                                            }
                                        assertEquals(
                                            innerLookaheadCoords,
                                            lookaheadScopeCoordinates
                                        )
                                    })(measurable, constraints)
                                }
                                .size(50.dp)
                                .intermediateLayout { measurable, constraints ->
                                    measureWithLambdas(prePlacement = {
                                        val innerLookaheadCoords =
                                            with(explicitLookaheadScope) {
                                                lookaheadScopeCoordinates
                                            }
                                        assertEquals(
                                            innerLookaheadCoords,
                                            lookaheadScopeCoordinates
                                        )
                                    })(measurable, constraints)
                                }
                        )
                    }
                }
            }
        }
    }

    @Test
    fun nestedVirtualNodeFromLookaheadScope() {
        var small by mutableStateOf(true)
        fun size1(): Int = if (small) 20 else 50
        fun size2(): Int = if (small) 60 else 120
        fun offset1(): IntOffset = if (small) IntOffset.Zero else IntOffset(80, 30)
        fun offset2(): IntOffset = if (small) IntOffset(50, 100) else IntOffset(60, 170)
        var actualSize1: IntSize = IntSize.Zero
        var actualSize2: IntSize = IntSize.Zero
        var actualOffset1: IntOffset = IntOffset.Zero
        var actualOffset2: IntOffset = IntOffset.Zero

        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box {
                    LookaheadScope {
                        LookaheadScope {
                            Box(
                                Modifier
                                    .offset(offset1().x.dp, offset1().y.dp)
                                    .onGloballyPositioned {
                                        actualSize1 = it.size
                                        actualOffset1 = it
                                            .localToRoot(Offset.Zero)
                                            .round()
                                    }
                                    .size(size1().dp)
                            )
                        }
                        Box(
                            Modifier
                                .offset(offset2().x.dp, offset2().y.dp)
                                .onGloballyPositioned {
                                    actualSize2 = it.size
                                    actualOffset2 = it
                                        .localToRoot(Offset.Zero)
                                        .round()
                                }
                                .size(size2().dp)
                        )
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(IntSize(size1(), size1()), actualSize1)
            assertEquals(IntSize(size2(), size2()), actualSize2)
            assertEquals(offset1(), actualOffset1)
            assertEquals(offset2(), actualOffset2)
            small = !small
        }
        // Change the size & position of LayoutNode in the nested virtual node
        rule.runOnIdle {
            assertEquals(IntSize(size1(), size1()), actualSize1)
            assertEquals(IntSize(size2(), size2()), actualSize2)
            assertEquals(offset1(), actualOffset1)
            assertEquals(offset2(), actualOffset2)
        }
    }

    @Test
    fun subcomposeLayoutSkipToLookaheadConstraintsPlacementBehavior() {
        val actualPlacementOrder = mutableStateListOf<Int>()
        val expectedPlacementOrder1 = listOf(1, 3, 5, 2, 4, 0)
        val expectedPlacementOrder2 = listOf(2, 0, 3, 1, 5, 4)
        val expectedPlacementOrder3 = listOf(5, 2, 4, 0, 3, 1)
        val expectedPlacementOrder =
            mutableStateListOf<Int>().apply { addAll(expectedPlacementOrder1) }

        var iteration by mutableStateOf(0)
        // Expect the default placement to be the same as lookahead
        rule.setContent {
            LookaheadScope {
                SubcomposeLayout(
                    intermediateMeasurePolicy = { lookaheadMeasurePolicy(lookaheadConstraints) }
                ) { constraints ->
                    val placeables = mutableListOf<Placeable>()
                    repeat(3) { id ->
                        subcompose(id) {
                            Box(Modifier.trackMainPassPlacement {
                                actualPlacementOrder.add(id)
                            })
                        }.fastMap { it.measure(constraints) }.let { placeables.addAll(it) }
                    }
                    layout(100, 100) {
                        val allPlaceables = mutableListOf<Placeable>().apply { addAll(placeables) }
                        repeat(3) { index ->
                            val id = index + 3
                            subcompose(id) {
                                Box(Modifier.trackMainPassPlacement {
                                    actualPlacementOrder.add(id)
                                })
                            }.fastMap { it.measure(constraints) }.let { allPlaceables.addAll(it) }
                        }
                        // Start lookahead placement
                        assertEquals(6, allPlaceables.size)
                        expectedPlacementOrder.fastForEach {
                            allPlaceables[it].place(0, 0)
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(expectedPlacementOrder.toList(), actualPlacementOrder.toList())

            expectedPlacementOrder.clear()
            expectedPlacementOrder.addAll(expectedPlacementOrder2)

            iteration++
            actualPlacementOrder.clear()
        }
        rule.runOnIdle {
            assertEquals(expectedPlacementOrder.toList(), actualPlacementOrder.toList())

            expectedPlacementOrder.clear()
            expectedPlacementOrder.addAll(expectedPlacementOrder3)

            iteration++
            actualPlacementOrder.clear()
        }
        rule.runOnIdle {
            assertEquals(expectedPlacementOrder.toList(), actualPlacementOrder.toList())
        }
    }

    private fun Modifier.trackMainPassPlacement(block: () -> Unit) =
        Modifier.intermediateLayout { measurable, constraints ->
            measurable.measure(constraints).run {
                layout(width, height) {
                    block()
                    place(0, 0)
                }
            }
        }

    @Ignore("b/276805422")
    @Test
    fun subcomposeLayoutInLookahead() {
        val expectedConstraints = mutableStateListOf(
            Constraints.fixed(0, 0),
            Constraints.fixed(0, 0),
            Constraints.fixed(0, 0)
        )
        val expectedPlacements = mutableStateListOf(
            IntOffset.Zero,
            IntOffset.Zero,
            IntOffset.Zero
        )

        fun generateRandomConstraintsAndPlacements() {
            repeat(3) {
                expectedConstraints[it] = Constraints.fixed(
                    Random.nextInt(100, 1000),
                    Random.nextInt(100, 1000)
                )
                expectedPlacements[it] = IntOffset(
                    Random.nextInt(-200, 1200),
                    Random.nextInt(-200, 1200)
                )
            }
        }

        val actualConstraints = arrayOfNulls<Constraints?>(3)
        val actualPlacements = arrayOfNulls<IntOffset?>(3)
        generateRandomConstraintsAndPlacements()
        rule.setContent {
            LookaheadScope {
                SubcomposeLayout {
                    val placeables = mutableVectorOf<Placeable>()
                    repeat(3) {
                        subcompose(it) {
                            Box(
                                Modifier
                                    .intermediateLayout { measurable, constraints ->
                                        actualConstraints[it] = constraints
                                        val placeable = measurable.measure(constraints)
                                        layout(placeable.width, placeable.height) {
                                            actualPlacements[it] =
                                                lookaheadScopeCoordinates
                                                    .localLookaheadPositionOf(
                                                        coordinates!!.toLookaheadCoordinates()
                                                    )
                                                    .round()
                                            placeable.place(0, 0)
                                        }
                                    }
                                    .fillMaxSize())
                            // This is intentionally left not placed, to check for crash.
                            Box(Modifier.size(200.dp))
                        }[0].measure(expectedConstraints[it]).let {
                            placeables.add(it)
                        }
                    }
                    layout(100, 100) {
                        repeat(3) {
                            placeables[it].place(expectedPlacements[it])
                        }
                    }
                }
            }
        }

        repeat(5) {
            rule.runOnIdle {
                repeat(expectedPlacements.size) {
                    assertEquals(expectedConstraints[it], actualConstraints[it])
                }
                repeat(expectedPlacements.size) {
                    assertEquals(expectedPlacements[it], actualPlacements[it])
                }
            }
            generateRandomConstraintsAndPlacements()
        }
    }

    @Test
    fun forceMeasureSubtreeWithoutAffectingLookahead() {
        var iterations by mutableStateOf(0)
        var lookaheadPosition: Offset? = null
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                LookaheadScope {
                    // Fill max size will cause the remeasure requests to go down the
                    // forceMeasureSubtree code path.
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Column(Modifier.fillMaxSize()) {
                            // This box will get a remeasure request when `iterations` changes.
                            // Subsequently this Box's size change will trigger a measurement pass
                            // from Column.
                            Box(Modifier.intermediateLayout { measurable, _ ->
                                // Force a state-read (similar to animation but more reliable)
                                measurable.measure(Constraints.fixed(200 + 100 * iterations, 200))
                                    .run {
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }) {
                                Box(Modifier.size(100.dp))
                            }
                            Box { // forceMeasureSubtree starts here
                                Box {
                                    // Lookahead measure will be marked dirty in the containing box.
                                    // This test ensures it doesn't get ignored when there's a
                                    // forceMeasureSubtree (without lookahead) triggered from
                                    // ancestor while it's lookaheadMeasurePending.
                                    if (iterations % 2 == 0) {
                                        Box(Modifier.size(100.dp))
                                    } else {
                                        Box(Modifier.size(200.dp))
                                    }
                                }
                            }
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .intermediateLayout { measurable, constraints ->
                                        measurable
                                            .measure(constraints)
                                            .run {
                                                layout(width, height) {
                                                    lookaheadPosition = lookaheadScopeCoordinates
                                                        .localLookaheadPositionOf(coordinates!!)
                                                    place(0, 0)
                                                }
                                            }
                                    })
                        }
                    }
                }
            }
        }

        repeat(4) {
            rule.runOnIdle {
                assertEquals(Offset(0f, 200f + 100 * (iterations % 2)), lookaheadPosition)
                iterations++
            }
        }
    }

    @Test
    fun forceMeasureSubtreeWhileLookaheadMeasureRequestedFromSubtree() {
        var iterations by mutableStateOf(0)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                LookaheadScope {
                    // Fill max size will cause the remeasure requests to go down the
                    // forceMeasureSubtree code path.
                    CompositionLocalProvider(LocalDensity provides Density(1f)) {
                        Column(Modifier.fillMaxSize()) {
                            // This box will get a remeasure request when `iterations` changes.
                            // Subsequently this Box's size change will trigger a measurement pass
                            // from Column.
                            Box(Modifier.intermediateLayout { measurable, _ ->
                                // Force a state-read, so that this node is the node where
                                // remeasurement starts.
                                @Suppress("UNUSED_EXPRESSION")
                                iterations
                                measurable.measure(Constraints.fixed(200, 200))
                                    .run {
                                        layout(width, height) {
                                            place(0, 0)
                                        }
                                    }
                            }) {
                                // Swap modifiers. If lookahead re-measurement from this node isn't
                                // handled before parent's non-lookahead remeasurement, this would
                                // lead to a crash.
                                Box(
                                    if (iterations % 2 == 0)
                                        Modifier.size(100.dp)
                                    else
                                        Modifier.intermediateLayout { measurable, constraints ->
                                            measurable.measure(constraints).run {
                                                layout(width, height) {
                                                    place(5, 5)
                                                }
                                            }
                                        }.padding(5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        repeat(4) {
            rule.runOnIdle {
                iterations++
            }
        }
    }

    @Test
    fun multiMeasureLayoutInLookahead() {
        var horizontal by mutableStateOf(true)
        rule.setContent {
            LookaheadScope {
                @Suppress("DEPRECATION")
                MultiMeasureLayout(
                    content = {
                        if (horizontal) {
                            Row {
                                repeat(3) {
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .background(Color.Red)
                                    )
                                }
                            }
                        } else {
                            Column {
                                repeat(3) {
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .background(Color.Red)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(0.6f),
                    measurePolicy = MeasurePolicy { measurables, constraints ->
                        // Intentionally measure twice here to ensure multi-measure is supported.
                        measurables.map { it.measure(Constraints.fixed(200, 300)) }
                        val placeables = measurables.map { it.measure(constraints) }
                        val maxWidth: Int = placeables.maxOf { it.width }
                        val maxHeight = placeables.maxOf { it.height }
                        // Position the children.
                        layout(maxWidth, maxHeight) {
                            placeables.forEach {
                                it.place(0, 0)
                            }
                        }
                    })
            }
        }
        rule.runOnIdle { horizontal = !horizontal }
        rule.runOnIdle { horizontal = !horizontal }
        rule.waitForIdle()
    }

    private fun assertSameLayoutWithAndWithoutLookahead(
        content: @Composable (
            modifier: Modifier
        ) -> Unit
    ) {
        val controlGroupSizes = mutableVectorOf<IntSize>()
        val controlGroupPositions = mutableVectorOf<Offset>()
        val sizes = mutableVectorOf<IntSize>()
        val positions = mutableVectorOf<Offset>()
        var enableControlGroup by mutableStateOf(true)
        rule.setContent {
            if (enableControlGroup) {
                Layout(measurePolicy = defaultMeasurePolicy, content = {
                    content(
                        modifier = Modifier.trackSizeAndPosition(
                            controlGroupSizes,
                            controlGroupPositions,
                        )
                    )
                })
            } else {
                Layout(measurePolicy = defaultMeasurePolicy, content = {
                    LookaheadScope {
                        content(
                            modifier = Modifier
                                .trackSizeAndPosition(sizes, positions)
                                .assertSameSizeAndPosition(this)
                        )
                    }
                })
            }
        }
        rule.runOnIdle {
            enableControlGroup = !enableControlGroup
        }
        rule.runOnIdle {
            if (Debug) {
                controlGroupPositions.debugPrint("Lookahead")
                controlGroupSizes.debugPrint("Lookahead")
                positions.debugPrint("Lookahead")
                sizes.debugPrint("Lookahead")
            }
            assertEquals(controlGroupPositions.size, positions.size)
            controlGroupPositions.forEachIndexed { i, position ->
                assertEquals(position, positions[i])
            }
            assertEquals(controlGroupSizes.size, sizes.size)
            controlGroupSizes.forEachIndexed { i, size ->
                assertEquals(size, sizes[i])
            }
        }
    }

    private fun Modifier.assertSameSizeAndPosition(scope: LookaheadScope) = composed {
        var lookaheadSize by remember {
            mutableStateOf(IntSize.Zero)
        }
        var lookaheadLayoutCoordinates: LayoutCoordinates? by remember {
            mutableStateOf(
                null
            )
        }
        var onPlacedCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }
        with(scope) {
            this@composed
                .intermediateLayout { measurable, constraints ->
                    lookaheadSize = this.lookaheadSize
                    measureWithLambdas(
                        prePlacement = {
                            lookaheadLayoutCoordinates = lookaheadScopeCoordinates
                        }
                    ).invoke(this, measurable, constraints)
                }
                .onPlaced { it ->
                    onPlacedCoordinates = it
                }
                .onGloballyPositioned {
                    assertEquals(lookaheadSize, it.size)
                    assertEquals(
                        lookaheadLayoutCoordinates!!.localLookaheadPositionOf(
                            onPlacedCoordinates!!
                        ),
                        lookaheadLayoutCoordinates!!.localPositionOf(
                            onPlacedCoordinates!!,
                            Offset.Zero
                        )
                    )
                    // Also check that localPositionOf with non-zero offset works
                    // correctly for lookahead coordinates and LayoutCoordinates.
                    val randomOffset = Offset(
                        Random
                            .nextInt(0, 1000)
                            .toFloat(),
                        Random
                            .nextInt(0, 1000)
                            .toFloat()
                    )
                    assertEquals(
                        lookaheadLayoutCoordinates!!
                            .toLookaheadCoordinates()
                            .localPositionOf(
                                onPlacedCoordinates!!.toLookaheadCoordinates(),
                                randomOffset
                            ),
                        lookaheadLayoutCoordinates!!.localPositionOf(
                            onPlacedCoordinates!!,
                            randomOffset
                        )
                    )
                }
        }
    }

    // This is needed because Offset comparison would fail when comparing -0.0f to 0.0f in one
    // or both of its dimensions.
    private fun assertEquals(expected: Offset, actual: Offset?) {
        assertEquals(expected.x, actual!!.x, 0f)
        assertEquals(expected.y, actual.y, 0f)
    }

    private fun Modifier.trackSizeAndPosition(
        sizes: MutableVector<IntSize>,
        positions: MutableVector<Offset>
    ) = this
        .onGloballyPositioned {
            positions.add(it.positionInRoot())
            sizes.add(it.size)
        }

    private fun <T> MutableVector<T>.debugPrint(tag: String) {
        print("$tag: [")
        forEach { print("$it, ") }
        print("]")
        println()
    }

    private val defaultMeasurePolicy: MeasurePolicy =
        MeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val maxWidth: Int = placeables.maxOf { it.width }
            val maxHeight = placeables.maxOf { it.height }
            // Position the children.
            layout(maxWidth, maxHeight) {
                placeables.forEach {
                    it.place(0, 0)
                }
            }
        }

    private fun measureWithLambdas(
        preMeasure: () -> Unit = {},
        postMeasure: (IntSize) -> Unit = {},
        prePlacement: Placeable.PlacementScope.() -> Unit = {},
        postPlacement: () -> Unit = {}
    ): MeasureScope.(Measurable, Constraints) -> MeasureResult = { measurable, constraints ->
        preMeasure()
        val placeable = measurable.measure(constraints)
        postMeasure(IntSize(placeable.width, placeable.height))
        layout(placeable.width, placeable.height) {
            prePlacement()
            placeable.place(0, 0)
            postPlacement()
        }
    }
}