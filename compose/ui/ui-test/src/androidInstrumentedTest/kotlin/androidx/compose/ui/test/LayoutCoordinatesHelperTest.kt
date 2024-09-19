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

package androidx.compose.ui.test

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.OnPlacedModifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.toOffset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutCoordinatesHelperTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun positionInParent_noOffset() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setContent {
            Column(
                Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                    parentCoordinates = coordinates
                    latch.countDown()
                }
            ) {
                Box(
                    Modifier.size(10.dp).align(Alignment.Start).onGloballyPositioned { coordinates
                        ->
                        childCoordinates = coordinates
                        latch.countDown()
                    }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            Offset.Zero,
            parentCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero)
        )
    }

    @Test
    fun positionInParent_centered() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.width(40.toDp()), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.width(20.toDp()).onGloballyPositioned {
                            coordinates: LayoutCoordinates ->
                            parentCoordinates = coordinates
                            latch.countDown()
                        }
                    ) {
                        Box(
                            Modifier.size(10.toDp())
                                .align(Alignment.CenterHorizontally)
                                .onGloballyPositioned { coordinates ->
                                    childCoordinates = coordinates
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            Offset(5f, 0f),
            parentCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero)
        )
    }

    @Test
    fun onPlaced_alignmentChange() {
        var offset by mutableStateOf(IntOffset(0, 0))
        var alignment by mutableStateOf(Alignment.Center)
        var positionInRoot by mutableStateOf(Offset.Zero)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box(Modifier.offset(200.dp, 100.dp).size(300.dp)) {
                    Box(
                        Modifier.onPlaced {
                                offset = it.placementInParent()
                                positionInRoot = it.positionInRoot()
                            }
                            .align(alignment = alignment)
                            .size(100.dp)
                    )
                }
            }
        }
        val parentOffset = Offset(200f, 100f)
        rule.runOnIdle {
            assertEquals(IntOffset(100, 100), offset)
            assertEquals(parentOffset + Offset(100f, 100f), positionInRoot)
            alignment = Alignment.TopStart
        }
        rule.runOnIdle {
            assertEquals(IntOffset(0, 0), offset)
            assertEquals(parentOffset + Offset(0f, 0f), positionInRoot)
            alignment = Alignment.BottomEnd
        }
        rule.runOnIdle {
            assertEquals(IntOffset(200, 200), offset)
            assertEquals(parentOffset + Offset(200f, 200f), positionInRoot)
        }
    }

    @Ignore("b/225198728")
    @Test
    fun onPlaced_invocation() {
        var additionalOffset by mutableStateOf(IntOffset.Zero)
        var alignment by mutableStateOf(Alignment.Center)
        val invocations = mutableListOf(0, 0, 0)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box(Modifier.offset(200.dp, 100.dp).size(300.dp)) {
                    Box(
                        Modifier.align(alignment = alignment)
                            .offset { additionalOffset }
                            .onPlaced {
                                assertEquals(additionalOffset, it.placementInParent())
                                invocations[0] = invocations[0] + 1
                            }
                            .clickable {}
                            .onPlaced {
                                assertEquals(additionalOffset, it.placementInParent())
                                invocations[1] = invocations[1] + 1
                            }
                            .testTag("Test")
                            .onPlaced {
                                assertEquals(additionalOffset, it.placementInParent())
                                invocations[2] = invocations[2] + 1
                            }
                            .size(100.dp)
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(invocations).containsExactlyElementsIn(listOf(1, 1, 1))
            alignment = Alignment.TopStart
        }
        rule.runOnIdle {
            assertThat(invocations).containsExactlyElementsIn(listOf(2, 2, 2))
            additionalOffset = IntOffset(0, 10)
        }
        rule.runOnIdle { assertThat(invocations).containsExactlyElementsIn(listOf(3, 3, 3)) }
    }

    @Test
    @Ignore("b/242125166")
    fun onPlacedUpToDateWhenModifierChainChanges() {
        var alignment by mutableStateOf(Alignment.TopStart)
        val targetOffset = mutableStateOf(Offset.Zero)
        rule.setContent {
            CompositionLocalProvider(LocalDensity.provides(Density(1f))) {
                Box(Modifier.size(200.dp)) {
                    Box(
                        modifier =
                            Modifier.animatePlacement(targetOffset) { alignment }
                                .align(alignment)
                                .size(20.dp)
                                .background(Color.Red)
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(calculateExpectedIntOffset(alignment), targetOffset.value)
            alignment = Alignment.Center
        }
        rule.runOnIdle {
            assertEquals(calculateExpectedIntOffset(alignment), targetOffset.value)
            alignment = Alignment.BottomEnd
        }
        rule.runOnIdle {
            assertEquals(calculateExpectedIntOffset(alignment), targetOffset.value)
            alignment = Alignment.TopCenter
        }
        rule.runOnIdle {
            assertEquals(calculateExpectedIntOffset(alignment), targetOffset.value)
            alignment = Alignment.TopEnd
        }
        rule.runOnIdle { assertEquals(calculateExpectedIntOffset(alignment), targetOffset.value) }
    }

    @Test
    fun onPlacedCalledOnReuseInsideLazyColumn() {
        lateinit var density: Density
        val items = 200
        val visibleItems = 2
        val itemSize = 50.dp
        val invocations = arrayOf(0, 0)

        // It's important to share lambda across all iterations
        val placedCallback0: (LayoutCoordinates) -> Unit = { invocations[0] = invocations[0] + 1 }
        val placedCallback1: (LayoutCoordinates) -> Unit = { invocations[1] = invocations[1] + 1 }
        val scrollState = LazyListState()
        rule.setContent {
            density = LocalDensity.current
            LazyColumn(Modifier.size(itemSize, itemSize * visibleItems), scrollState) {
                items(items) {
                    Box(Modifier.size(itemSize).onPlaced(placedCallback0)) {
                        Box(Modifier.size(itemSize).onPlaced(placedCallback1))
                    }
                }
            }
        }

        var expectedInvocations = visibleItems
        val delta = with(density) { (itemSize * visibleItems).toPx() }
        repeat(items / visibleItems) {
            rule.runOnIdle {
                assertThat(invocations[0]).isAtLeast(expectedInvocations)
                assertThat(invocations[1]).isAtLeast(expectedInvocations)

                scrollState.dispatchRawDelta(delta)
                expectedInvocations += visibleItems
            }
        }
    }

    private fun Modifier.animatePlacement(
        targetOffset: MutableState<Offset>,
        alignment: () -> Alignment
    ): Modifier = composed {
        val scope = rememberCoroutineScope()
        var animatable by remember { mutableStateOf<Animatable<Offset, AnimationVector2D>?>(null) }
        this.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            .onPlaced { coordinates ->
                targetOffset.value = coordinates.positionInParent()
                assertEquals(calculateExpectedIntOffset(alignment()), targetOffset.value)
                // Animate to the new target offset when alignment changes.
                val anim =
                    animatable
                        ?: Animatable(targetOffset.value, Offset.VectorConverter).also {
                            animatable = it
                        }
                if (anim.targetValue != targetOffset.value) {
                    scope.launch {
                        anim.animateTo(
                            targetOffset.value,
                            spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        animatable?.let { (it.value - targetOffset.value).round() }
                            ?: IntOffset.Zero
                    )
                }
            }
    }

    private fun calculateExpectedIntOffset(alignment: Alignment) =
        alignment.align(IntSize(20, 20), IntSize(200, 200), LayoutDirection.Ltr).toOffset()

    @Test
    fun onPlacedModifierWithLayoutModifier() {
        lateinit var coords: LayoutCoordinates

        val modifier =
            object : OnPlacedModifier, LayoutModifier {
                override fun MeasureScope.measure(
                    measurable: Measurable,
                    constraints: Constraints
                ): MeasureResult {
                    val p = measurable.measure(Constraints.fixed(50, 50))
                    return layout(50, 50) {
                        // coords should already be set by the time we are running this.
                        assertThat(coords.size).isEqualTo(IntSize(50, 50))
                        p.place(0, 0)
                    }
                }

                override fun onPlaced(coordinates: LayoutCoordinates) {
                    coords = coordinates
                }
            }

        rule.setContent { Box(Modifier.fillMaxSize().then(modifier)) }

        rule.runOnIdle { assertThat(coords.size).isEqualTo(IntSize(50, 50)) }
    }

    @Test
    fun onBoxPlaced_failing() {
        var coordinates: LayoutCoordinates? = null
        rule.setContent { Box(Modifier.onPlaced { coordinates = it }) }
        rule.runOnIdle { assertThat(coordinates).isNotNull() }
    }

    @Test(expected = UnsupportedOperationException::class)
    fun defaultTransformFromThrows() {
        val layoutCoordinates =
            object : LayoutCoordinates {
                override val size: IntSize
                    get() = TODO("Not yet implemented")

                override val providedAlignmentLines: Set<AlignmentLine>
                    get() = TODO("Not yet implemented")

                override val parentLayoutCoordinates: LayoutCoordinates?
                    get() = TODO("Not yet implemented")

                override val parentCoordinates: LayoutCoordinates?
                    get() = TODO("Not yet implemented")

                override val isAttached: Boolean
                    get() = TODO("Not yet implemented")

                override fun windowToLocal(relativeToWindow: Offset): Offset {
                    TODO("Not yet implemented")
                }

                override fun localToWindow(relativeToLocal: Offset): Offset {
                    TODO("Not yet implemented")
                }

                override fun localToRoot(relativeToLocal: Offset): Offset {
                    TODO("Not yet implemented")
                }

                override fun localPositionOf(
                    sourceCoordinates: LayoutCoordinates,
                    relativeToSource: Offset
                ): Offset {
                    TODO("Not yet implemented")
                }

                override fun localBoundingBoxOf(
                    sourceCoordinates: LayoutCoordinates,
                    clipBounds: Boolean
                ): Rect {
                    TODO("Not yet implemented")
                }

                override fun get(alignmentLine: AlignmentLine): Int {
                    TODO("Not yet implemented")
                }
            }
        val matrix = Matrix()
        // This should throw UnsupoportedOperationException
        layoutCoordinates.transformFrom(layoutCoordinates, matrix)
    }

    private fun LayoutCoordinates.placementInParent() =
        parentCoordinates!!.localPositionOf(this, Offset.Zero).round()
}
