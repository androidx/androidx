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

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReusableContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AtLeastSize
import androidx.compose.ui.FixedSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.SimpleRow
import androidx.compose.ui.Wrap
import androidx.compose.ui.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.padding
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.spatial.RelativeLayoutBounds
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class OnGlobalRectChangedTest {

    @get:Rule val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun correctPositionInRootWhenMovingBothGrandParentAndNodeItself() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            rule.setContent {
                Layout(
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(offset, offset)
                                    }
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(offset, offset)
                    }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset.Zero) }

            rule.runOnIdle { offset = 10 }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 20)) }
        }
    }

    @Test
    fun correctPositionInRootWhenMovingGrandParentWithLayerAndNode() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            rule.setContent {
                Layout(
                    modifier =
                        Modifier.graphicsLayer {
                            translationX = offset.toFloat()
                            translationY = offset.toFloat()
                        },
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(offset, offset)
                                    }
                                }
                            }
                        }
                    },
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset.Zero) }

            rule.runOnIdle { offset = 10 }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 20)) }
        }
    }

    @Test
    fun correctPositionInRootWhenMovingBothGrandParentWithLayoutModifierAndNode() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            rule.setContent {
                Layout(
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(offset, offset)
                            }
                        },
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(offset, offset)
                                    }
                                }
                            }
                        }
                    },
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset.Zero) }

            rule.runOnIdle { offset = 10 }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 20)) }
        }
    }

    @Test
    fun correctPositionInRootWhenUnplacingThenPlacingGrandparentWithDifferentOffsetAndNode() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            var shouldPlace by mutableStateOf(true)
            rule.setContent {
                Layout(
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(offset, offset)
                                    }
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        if (shouldPlace) {
                            placeable.place(offset, offset)
                        }
                    }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset.Zero) }

            rule.runOnIdle {
                shouldPlace = false
                offset = 10
                actualPosition = IntOffset.Max
            }

            rule.runOnIdle {
                assertThat(actualPosition).isEqualTo(IntOffset.Max)
                shouldPlace = true
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 20)) }
        }
    }

    @Test
    fun handlesChildrenNodeMoveCorrectly() {
        val size = 50
        var index by mutableStateOf(0)
        var wrap1Position = IntRect.Zero
        var wrap2Position = IntRect.Zero
        rule.setContent {
            SimpleRow {
                for (i in 0 until 2) {
                    if (index == i) {
                        Wrap(
                            minWidth = size,
                            minHeight = size,
                            modifier =
                                Modifier.onLayoutRectChanged(0, 0) {
                                    wrap1Position = it.boundsInWindow
                                },
                        )
                    } else {
                        Wrap(
                            minWidth = size,
                            minHeight = size,
                            modifier =
                                Modifier.onLayoutRectChanged(0, 0) {
                                    wrap2Position = it.boundsInWindow
                                },
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(0, wrap1Position.left)
            assertEquals(size, wrap2Position.left)
            index = 1
        }

        rule.runOnIdle {
            assertEquals(size, wrap1Position.left)
            assertEquals(0, wrap2Position.left)
        }
    }

    @Test
    fun callbacksAreCalledWhenChildResized() {
        var size by mutableStateOf(10)
        var realChildSize = 0
        rule.setContent {
            AtLeastSize(size = 20) {
                Wrap(
                    minWidth = size,
                    minHeight = size,
                    modifier =
                        Modifier.onLayoutRectChanged(0, 0) {
                            realChildSize = it.boundsInRoot.size.width
                        },
                )
            }
        }

        rule.runOnIdle {
            assertEquals(10, realChildSize)
            size = 15
        }

        rule.runOnIdle { assertEquals(15, realChildSize) }
    }

    fun IntRect.offset() = IntOffset(left, top)

    @Test
    fun callbackCalledForChildWhenParentMoved() {
        var position by mutableStateOf(0)
        var childGlobalPosition = IntOffset(0, 0)
        var latch = CountDownLatch(1)
        rule.setContent {
            Layout(
                measurePolicy = { measurables, constraints ->
                    layout(10, 10) { measurables[0].measure(constraints).place(position, 0) }
                },
                content = {
                    Wrap(minWidth = 10, minHeight = 10) {
                        Wrap(
                            minWidth = 10,
                            minHeight = 10,
                            modifier =
                                Modifier.onLayoutRectChanged(0, 0) { rect ->
                                    childGlobalPosition = rect.boundsInRoot.offset()
                                    latch.countDown()
                                },
                        )
                    }
                },
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))

        latch = CountDownLatch(1)
        rule.runOnUiThread { position = 10 }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntOffset(10, 0), childGlobalPosition)
    }

    @Test
    fun callbacksAreCalledOnlyForPositionedChildren() {
        val latch = CountDownLatch(1)
        var wrap1OnPositionedCalled = false
        var wrap2OnPositionedCalled = false
        rule.setContent {
            Layout(
                measurePolicy = { measurables, constraints ->
                    layout(10, 10) { measurables[1].measure(constraints).place(0, 0) }
                },
                content = {
                    Wrap(
                        minWidth = 10,
                        minHeight = 10,
                        modifier =
                            Modifier.onLayoutRectChanged(0, 0) { wrap1OnPositionedCalled = true },
                    )
                    Wrap(
                        minWidth = 10,
                        minHeight = 10,
                        modifier =
                            Modifier.onLayoutRectChanged(0, 0) { wrap2OnPositionedCalled = true },
                    ) {
                        Wrap(
                            minWidth = 10,
                            minHeight = 10,
                            modifier = Modifier.onLayoutRectChanged(0, 0) { latch.countDown() },
                        )
                    }
                },
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertFalse(wrap1OnPositionedCalled)
        assertTrue(wrap2OnPositionedCalled)
    }

    @Test
    fun globalPositionedModifierUpdateDoesNotInvalidateLayout() {
        var lambda1Called = false
        var lambda2Called = false
        var layoutCalled = false
        var placementCalled = false
        val lambda1: (RelativeLayoutBounds) -> Unit = { lambda1Called = true }
        val lambda2: (RelativeLayoutBounds) -> Unit = { lambda2Called = true }

        val changeLambda = mutableStateOf(true)

        val layoutModifier =
            Modifier.layout { measurable, constraints ->
                layoutCalled = true
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placementCalled = true
                    placeable.place(0, 0)
                }
            }

        rule.setContent {
            Box(
                modifier =
                    Modifier.then(layoutModifier)
                        .size(10.dp)
                        .onLayoutRectChanged(0, 0, if (changeLambda.value) lambda1 else lambda2)
            )
        }

        rule.runOnIdle {
            assertTrue(lambda1Called)
            assertTrue(layoutCalled)
            assertTrue(placementCalled)
            assertFalse(lambda2Called)
        }

        lambda1Called = false
        lambda2Called = false
        layoutCalled = false
        placementCalled = false
        changeLambda.value = false

        rule.runOnIdle {
            assertFalse(lambda1Called)
            assertFalse(layoutCalled)
            assertFalse(placementCalled)
            // we execute the new lambda when the lambda is updated
            assertTrue(lambda2Called)
        }
    }

    @Test
    fun columnCenteringHasCorrectPosition() {
        var padding by mutableStateOf(0.dp)
        var lastOffsetFromRectChanged: IntOffset? = null
        var lastOffsetFromGloballyPositioned: IntOffset? = null

        rule.setContent {
            Column(
                modifier = Modifier.size(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier =
                        Modifier.padding(top = padding)
                            .onLayoutRectChanged(0, 0) {
                                lastOffsetFromRectChanged = it.positionInRoot
                            }
                            .onGloballyPositioned {
                                lastOffsetFromGloballyPositioned = it.positionInRoot().round()
                            }
                            .size(100.dp)
                )
            }
        }

        rule.runOnIdle {
            assertNotNull(lastOffsetFromGloballyPositioned)
            assertNotNull(lastOffsetFromRectChanged)
            assertEquals(lastOffsetFromGloballyPositioned, lastOffsetFromRectChanged)
        }
    }

    @Test
    fun callbackIsReExecutedOnReuse() {
        with(rule.density) {
            val size = 10
            var actualSize: Int = Int.MAX_VALUE
            var reuseKey by mutableStateOf(0)
            rule.setContent {
                ReusableContent(reuseKey) {
                    Box(
                        Modifier.requiredSize(size.toDp(), size.toDp()).onLayoutRectChanged(0, 0) {
                            actualSize = it.width
                        }
                    )
                }
            }

            rule.runOnIdle {
                actualSize = Int.MAX_VALUE
                reuseKey++
            }

            rule.runOnIdle { assertThat(actualSize).isEqualTo(size) }
        }
    }

    @Test
    fun callbackIsCalledWhenAddedLater() {
        with(rule.density) {
            val size = 10
            var actualSize: Int = Int.MAX_VALUE
            var dynamicModifier by mutableStateOf<Modifier>(Modifier)
            rule.setContent {
                Box(Modifier.requiredSize(size.toDp(), size.toDp()).then(dynamicModifier))
            }

            rule.runOnIdle {
                dynamicModifier = Modifier.onLayoutRectChanged(0, 0) { actualSize = it.width }
            }

            rule.runOnIdle { assertThat(actualSize).isEqualTo(size) }
        }
    }

    @Test
    fun callbacksAreCalledOnlyOnceWhenLambdaChangesAndLayoutChanges() {
        var lambda1Called = false
        val lambda1: (RelativeLayoutBounds) -> Unit = {
            assert(!lambda1Called)
            lambda1Called = true
        }

        var lambda2Called = false
        val lambda2: (RelativeLayoutBounds) -> Unit = {
            assert(!lambda2Called)
            lambda2Called = true
        }

        val changeLambda = mutableStateOf(true)
        val size = mutableStateOf(100.dp)
        rule.setContent {
            Box(
                modifier =
                    Modifier.size(size.value)
                        .onLayoutRectChanged(0, 0, if (changeLambda.value) lambda1 else lambda2)
            )
        }

        rule.runOnIdle {
            assertTrue(lambda1Called)
            assertFalse(lambda2Called)
        }

        lambda1Called = false
        lambda2Called = false
        size.value = 120.dp
        changeLambda.value = false

        rule.runOnIdle {
            assertTrue(lambda2Called)
            assertFalse(lambda1Called)
        }
    }

    // change layout below callback, callback only gets called ones
    @Test
    fun callbacksAreCalledOnlyOnceWhenLayoutBelowItAndLambdaChanged() {
        var lambda1Called = false
        val lambda1: (RelativeLayoutBounds) -> Unit = {
            assert(!lambda1Called)
            lambda1Called = true
        }

        var lambda2Called = false
        val lambda2: (RelativeLayoutBounds) -> Unit = {
            assert(!lambda2Called)
            lambda2Called = true
        }

        val changeLambda = mutableStateOf(true)
        val size = mutableStateOf(10.dp)
        rule.setContent {
            Box(
                modifier =
                    Modifier.padding(10.dp)
                        .onLayoutRectChanged(0, 0, if (changeLambda.value) lambda1 else lambda2)
                        .padding(size.value)
                        .size(10.dp)
            )
        }

        rule.runOnIdle {
            assertTrue(lambda1Called)
            assertFalse(lambda2Called)
        }

        lambda1Called = false
        lambda2Called = false
        size.value = 20.dp
        changeLambda.value = false

        rule.runOnIdle {
            assertTrue(lambda2Called)
            assertFalse(lambda1Called)
        }
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerIsScrolled() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: IntRect? = null
        var scrollView: ScrollView? = null
        lateinit var view: ComposeView

        rule.runOnUiThread {
            scrollView = ScrollView(rule.activity)
            rule.activity.setContentView(scrollView, ViewGroup.LayoutParams(100, 100))
            view = ComposeView(rule.activity)
            scrollView!!.addView(view)
            view.setContent {
                Layout(
                    {},
                    modifier =
                        Modifier.onLayoutRectChanged(0, 0) {
                            coordinates = it.boundsInWindow
                            positionedLatch.countDown()
                        },
                ) { _, _ ->
                    layout(100, 200) {}
                }
            }
        }

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        positionedLatch = CountDownLatch(1)

        rule.runOnIdle {
            coordinates = null
            scrollView!!.scrollBy(0, 50)
        }

        assertTrue(
            "OnPositioned is not called when the container scrolled",
            positionedLatch.await(1, TimeUnit.SECONDS),
        )

        rule.runOnIdle { assertThat(abs(view.getYInWindow().toInt() - coordinates!!.top) <= 1) }
    }

    @Test
    fun onPositionedCalledWhenLayerChanged() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: IntRect? = null
        var offsetX by mutableStateOf(0f)

        rule.setContent {
            Layout(
                {},
                modifier =
                    Modifier.graphicsLayer { translationX = offsetX }
                        .onLayoutRectChanged(0, 0) {
                            coordinates = it.boundsInWindow
                            positionedLatch.countDown()
                        },
            ) { _, _ ->
                layout(100, 200) {}
            }
        }

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        positionedLatch = CountDownLatch(1)

        rule.runOnIdle {
            coordinates = null
            offsetX = 5f
        }

        assertTrue(
            "OnPositioned is not called when the container scrolled",
            positionedLatch.await(1, TimeUnit.SECONDS),
        )

        rule.runOnIdle { assertEquals(5, coordinates!!.left) }
    }

    private fun View.getYInWindow(): Float {
        var offset = 0f
        val parentView = parent
        if (parentView is View) {
            offset += parentView.getYInWindow()
            offset -= scrollY.toFloat()
            offset += top.toFloat()
        }
        return offset
    }

    @Test
    fun onPositionedIsCalledWhenComposeContainerPositionChanged() {
        var positionedLatch = CountDownLatch(1)
        var coordinates: IntRect? = null
        var topView: View? = null

        rule.runOnUiThread {
            val linearLayout = LinearLayout(rule.activity)
            linearLayout.orientation = LinearLayout.VERTICAL
            rule.activity.setContentView(linearLayout, ViewGroup.LayoutParams(100, 200))
            topView = View(rule.activity)
            linearLayout.addView(topView!!, ViewGroup.LayoutParams(100, 100))
            val view = ComposeView(rule.activity)
            linearLayout.addView(view, ViewGroup.LayoutParams(100, 100))
            view.setContent {
                Layout(
                    {},
                    modifier =
                        Modifier.onLayoutRectChanged(0, 0) {
                            coordinates = it.boundsInWindow
                            positionedLatch.countDown()
                        },
                ) { _, constraints ->
                    layout(constraints.maxWidth, constraints.maxHeight) {}
                }
            }
        }

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        val startY = coordinates!!.top
        positionedLatch = CountDownLatch(1)

        rule.runOnIdle { topView!!.visibility = View.GONE }

        assertTrue(
            "OnPositioned is not called when the container moved",
            positionedLatch.await(1, TimeUnit.SECONDS),
        )

        rule.runOnIdle { assertEquals(startY - 100, coordinates!!.top) }
    }

    @Test
    fun onPositionedCalledInDifferentPartsOfHierarchy() {
        var coordinates1: IntRect? = null
        var coordinates2: IntRect? = null
        var size by mutableStateOf(10f)

        rule.setContent {
            with(LocalDensity.current) {
                DelayedMeasure(50) {
                    Box(Modifier.requiredSize(25.toDp())) {
                        Box(
                            Modifier.requiredSize(size.toDp()).onLayoutRectChanged(0, 0) {
                                coordinates1 = it.boundsInRoot
                            }
                        )
                    }
                    Box(Modifier.requiredSize(25.toDp())) {
                        Box(
                            Modifier.requiredSize(size.toDp()).onLayoutRectChanged(0, 0) {
                                coordinates2 = it.boundsInRoot
                            }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(coordinates1)
            assertNotNull(coordinates2)
            coordinates1 = null
            coordinates2 = null
            size = 15f
        }

        rule.runOnIdle {
            assertNotNull(coordinates1)
            assertNotNull(coordinates2)
        }
    }

    @Test
    fun globalCoordinatesAreInActivityCoordinates() {
        val padding = 30
        val framePadding = IntOffset(padding, padding)
        var realGlobalPosition: IntOffset? = null
        var frameGlobalPosition: IntOffset? = null

        val positionedLatch = CountDownLatch(1)
        rule.runOnUiThread {
            val composeView = ComposeView(rule.activity)
            composeView.setPadding(padding, padding, padding, padding)
            rule.activity.setContentView(composeView)

            composeView.setContent {
                Box(
                    Modifier.fillMaxSize().onLayoutRectChanged(0, 0) {
                        val position = IntArray(2)
                        composeView.getLocationInWindow(position)
                        frameGlobalPosition = IntOffset(position[0], position[1])

                        realGlobalPosition = it.boundsInWindow.offset()

                        positionedLatch.countDown()
                    }
                )
            }
        }

        rule.waitForIdle()

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        rule.runOnIdle {
            assertThat(realGlobalPosition).isEqualTo(frameGlobalPosition!! + framePadding)
        }
    }

    @Test
    fun testPositionInRootFromRelativeLayoutBoundsForItemsInLazyColumn() {
        val itemCount = 20
        val positionInRootFromRelativeLayoutBounds = Array(itemCount) { IntOffset.Max }
        val positionInRootExpected = Array(itemCount) { IntOffset.Max }
        val lazystate = LazyListState()
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                LazyColumn(Modifier.size(200.dp, 400.dp), lazystate) {
                    // This Lazy Column fits exactly 4 items in the viewport
                    repeat(itemCount) { id ->
                        item {
                            Box(
                                Modifier.size(100.dp)
                                    .onLayoutRectChanged(0, 0) {
                                        positionInRootFromRelativeLayoutBounds[id] =
                                            it.positionInRoot
                                    }
                                    .onGloballyPositioned {
                                        positionInRootExpected[id] = it.positionInRoot().round()
                                    }
                                    .padding(10.dp)
                                    .background(Color.Gray)
                            )
                        }
                    }
                }
            }
        }
        rule.runOnIdle {
            repeat(4) {
                assertEquals(positionInRootExpected[it], positionInRootFromRelativeLayoutBounds[it])
            }
        }
        // Scroll by 1/2 an item, to verify the offset change of the existing items is tracked.
        rule.runOnIdle { runBlocking { lazystate.scrollBy(50f) } }
        rule.runOnIdle {
            assertEquals(0, lazystate.firstVisibleItemIndex)
            repeat(5) {
                val id = it
                assertEquals(positionInRootExpected[id], positionInRootFromRelativeLayoutBounds[id])
            }
        }

        // Scroll to item #10 to bring in a new set of items, and verify they are getting the right
        // position from RelativeLayoutBounds
        rule.runOnIdle { runBlocking { lazystate.scrollToItem(10, 20) } }
        rule.runOnIdle {
            assertEquals(10, lazystate.firstVisibleItemIndex)
            repeat(4) {
                val id = 10 + it
                assertEquals(positionInRootExpected[id], positionInRootFromRelativeLayoutBounds[id])
            }
        }
    }

    @Test
    fun testRepositionTriggersCallback() {
        val left = mutableStateOf(30)
        var realLeft: Int? = null

        rule.setContent {
            with(LocalDensity.current) {
                Box {
                    Box(
                        Modifier.fillMaxSize()
                            .padding(start = left.value.toDp())
                            .onLayoutRectChanged(0, 0) { realLeft = it.boundsInRoot.left }
                    )
                }
            }
        }

        rule.runOnIdle { left.value = 40 }

        rule.runOnIdle { assertThat(realLeft).isEqualTo(40) }
    }

    @Test
    fun testGrandParentRepositionTriggersChildrenCallback() {
        // when we reposition any parent layout is causes the change in global
        // position of all the children down the tree(for example during the scrolling).
        // children should be able to react on this change.
        val left = mutableStateOf(20)
        var realLeft: Int? = null
        var positionedLatch = CountDownLatch(1)
        rule.setContent {
            with(LocalDensity.current) {
                Box {
                    Offset(left) {
                        Box(Modifier.requiredSize(10.toDp())) {
                            Box(Modifier.requiredSize(10.toDp())) {
                                Box(
                                    Modifier.onLayoutRectChanged(0, 0) {
                                            realLeft = it.boundsInRoot.left
                                            positionedLatch.countDown()
                                        }
                                        .requiredSize(10.toDp())
                                )
                            }
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        rule.runOnUiThread { left.value = 40 }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        assertThat(realLeft).isEqualTo(40)
    }

    @Test
    fun testLayerBoundsPositionInRotatedView() {
        var rect: RelativeLayoutBounds? = null
        var view: View? = null
        var toggle by mutableStateOf(false)
        rule.setContent {
            view = LocalView.current
            if (toggle) {
                FixedSize(
                    30,
                    Modifier.padding(10).onLayoutRectChanged(0, 0) { rect = it },
                ) { /* no-op */
                }
            }
        }

        val composeView = view as AndroidComposeView
        rule.runOnUiThread {
            // rotate the view so that it no longer aligns squarely
            composeView.rotation = 45f
            composeView.pivotX = 0f
            composeView.pivotY = 0f
            toggle = !toggle
        }

        rule.runOnIdle {
            val layoutCoordinates = rect!!
            assertEquals(IntOffset(10, 10), layoutCoordinates.boundsInRoot.offset())
            assertEquals(IntRect(10, 10, 40, 40), layoutCoordinates.boundsInRoot)

            val boundsInWindow = layoutCoordinates.boundsInWindow
            assertEquals(10f * sqrt(2f), boundsInWindow.top.toFloat(), 1f)
            assertEquals(30f * sqrt(2f) / 2f, boundsInWindow.right.toFloat(), 1f)
            assertEquals(-30f * sqrt(2f) / 2f, boundsInWindow.left.toFloat(), 1f)
            assertEquals(40f * sqrt(2f), boundsInWindow.bottom.toFloat(), 1f)
        }
    }

    @Test
    fun testDrawOnlyUpdates() {
        var rect: RelativeLayoutBounds? = null
        var offset by mutableStateOf(IntOffset(0, 0))
        rule.setContent {
            FixedSize(
                30,
                Modifier.offset { offset }.onLayoutRectChanged(1, 2000) { rect = it },
            ) { /* no-op */
            }
        }

        // Even though debounce is set to 2s, the callback should get called right away because
        // throttle is non-zero
        rule.runOnIdle {
            val rect = rect!!
            assertEquals(IntRect(0, 0, 30, 30), rect.boundsInRoot)
            offset = IntOffset(10, 10)
        }

        // This ensures that, even though only draw got affected (through layer offset change), the
        // rect should get updated immediately because throttle is set to 1
        rule.runOnIdle {
            val rect = rect!!
            assertEquals(IntRect(10, 10, 40, 40), rect.boundsInRoot)
        }
    }

    @Test
    fun testLayerBoundsPositionInMovedWindow() {
        var coords: IntRect? = null
        var alignment by mutableStateOf(Alignment.Center)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Popup(alignment = alignment) {
                    FixedSize(
                        30,
                        Modifier.padding(10).background(Color.Red).onLayoutRectChanged(0, 0) {
                            coords = it.boundsInWindow
                        },
                    ) { /* no-op */
                    }
                }
            }
        }

        rule.runOnIdle {
            val inWindow = coords!!.offset()
            assertEquals(10, inWindow.x)
            assertEquals(10, inWindow.y)
            alignment = Alignment.BottomEnd
        }

        rule.runOnIdle {
            val inWindow = coords!!.offset()
            assertEquals(10, inWindow.x)
            assertEquals(10, inWindow.y)
        }
    }

    @Test
    fun coordinatesOfTheModifierAreReported() {
        var coords1: IntRect? = null
        var coords2: IntRect? = null
        var coords3: IntRect? = null
        rule.setContent {
            Box(
                Modifier.fillMaxSize()
                    .onLayoutRectChanged(0, 0) { coords1 = it.boundsInWindow }
                    .padding(2.dp)
                    .onLayoutRectChanged(0, 0) { coords2 = it.boundsInWindow }
                    .padding(3.dp)
                    .onLayoutRectChanged(0, 0) { coords3 = it.boundsInWindow }
            )
        }

        rule.runOnIdle {
            assertEquals(0, coords1!!.offset().x)
            val padding1 = with(rule.density) { 2.dp.roundToPx() }
            assertEquals(padding1, coords2!!.offset().x)
            val padding2 = padding1 + with(rule.density) { 3.dp.roundToPx() }
            assertEquals(padding2, coords3!!.offset().x)
        }
    }

    @Test
    @SmallTest
    fun modifierIsReturningEqualObjectForTheSameLambda() {
        val lambda: (RelativeLayoutBounds) -> Unit = {}
        assertEquals(
            Modifier.onLayoutRectChanged(0, 0, lambda),
            Modifier.onLayoutRectChanged(0, 0, lambda),
        )
    }

    @Test
    @SmallTest
    fun modifierIsReturningNotEqualObjectForDifferentLambdas() {
        val lambda1: (RelativeLayoutBounds) -> Unit = { print("foo") }
        val lambda2: (RelativeLayoutBounds) -> Unit = { print("bar") }
        Assert.assertNotEquals(
            Modifier.onLayoutRectChanged(0, 0, lambda1),
            Modifier.onLayoutRectChanged(0, 0, lambda2),
        )
    }

    // In some special circumstances, the onGloballyPositioned callbacks can be called recursively
    // and they shouldn't crash when that happens. This tests a pointer event causing an
    // onGloballyPositioned callback while processing the onGloballyPositioned.
    @Test
    fun recurseGloballyPositionedCallback() {
        val view = rule.activity.findViewById<View>(android.R.id.content)
        var offset by mutableStateOf(IntOffset.Zero)
        var position = IntOffset.Max
        var hasSent = false
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxSize()
                        .offset { offset }
                        .onLayoutRectChanged(0, 0) {
                            if (offset != IntOffset.Zero) {
                                position = it.boundsInRoot.offset()
                            }
                        }
                )
                Box(
                    Modifier.fillMaxSize()
                        .offset { offset }
                        .onLayoutRectChanged(0, 0) {
                            if (offset != IntOffset.Zero && !hasSent) {
                                hasSent = true
                                val now = SystemClock.uptimeMillis()
                                val event =
                                    MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
                                view.dispatchTouchEvent(event)
                            }
                        }
                )
            }
        }
        rule.runOnIdle { offset = IntOffset(1, 1) }
        rule.runOnIdle { assertThat(position).isEqualTo(IntOffset(1, 1)) }
    }

    @Test
    fun lotsOfNotifications() {
        // have more than 16 OnGloballyPositioned liseteners to test listener cache
        var offset by mutableStateOf(IntOffset.Zero)
        var position = IntOffset.Max
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                repeat(30) {
                    Box(
                        Modifier.fillMaxSize()
                            .offset { offset }
                            .onLayoutRectChanged(0, 0) { position = it.boundsInRoot.offset() }
                    )
                }
            }
        }
        rule.runOnIdle { offset = IntOffset(1, 1) }
        rule.waitForIdle()
        rule.waitForIdle()
        rule.runOnIdle { assertThat(position).isEqualTo(IntOffset(1, 1)) }
    }

    @Test
    fun removingOnPositionedCallbackDoesNotTriggerOtherCallbacks() {
        val callbackPresent = mutableStateOf(true)

        var positionCalled1Count = 0
        var positionCalled2Count = 0
        rule.setContent {
            val modifier =
                if (callbackPresent.value) {
                    // Remember lambdas to avoid triggering a node update when the lambda changes
                    Modifier.onLayoutRectChanged(0, 0, remember { { positionCalled1Count++ } })
                } else {
                    Modifier
                }
            Box(
                Modifier
                    // Remember lambdas to avoid triggering a node update when the lambda changes
                    .onLayoutRectChanged(0, 0, remember { { positionCalled2Count++ } })
                    .then(modifier)
                    .fillMaxSize()
            )
        }

        rule.runOnIdle {
            // Both callbacks should be called
            assertThat(positionCalled1Count).isEqualTo(1)
            assertThat(positionCalled2Count).isEqualTo(1)
        }

        // Remove the first node
        rule.runOnIdle { callbackPresent.value = false }

        rule.runOnIdle {
            // Removing the node should not trigger any new callbacks
            assertThat(positionCalled1Count).isEqualTo(1)
            assertThat(positionCalled2Count).isEqualTo(1)
        }
    }

    @Test
    fun occlusionCalculationOnRectChangedCallbacks() {
        var box2Fraction by mutableStateOf(1f)

        var box0Bounds: RelativeLayoutBounds? = null

        var box0Occlusions = emptyList<IntRect>()
        var box1Occlusions = emptyList<IntRect>()
        var box2Occlusions = emptyList<IntRect>()

        var box0CallbackCount = 0
        var box1CallbackCount = 0
        var box2CallbackCount = 0

        val box0Callback = { rectInfo: RelativeLayoutBounds ->
            box0Bounds = rectInfo
            box0CallbackCount++
            box0Occlusions = rectInfo.calculateOcclusions()
        }
        val box1Callback = { rectInfo: RelativeLayoutBounds ->
            box1CallbackCount++
            box1Occlusions = rectInfo.calculateOcclusions()
        }
        val box2Callback = { rectInfo: RelativeLayoutBounds ->
            box2CallbackCount++
            box2Occlusions = rectInfo.calculateOcclusions()
        }
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(0.5f)
                        .align(Alignment.TopStart)
                        .onLayoutRectChanged(0, 0, box0Callback)
                )
                Box(
                    // Should initially occlude first box
                    Modifier.fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .align(Alignment.BottomStart)
                        .onLayoutRectChanged(0, 0, box1Callback)
                )
                Box(
                    // Should initially occlude both boxes
                    Modifier.fillMaxSize(box2Fraction)
                        .align(Alignment.BottomStart)
                        .onLayoutRectChanged(0, 0, box2Callback)
                )
            }
        }
        rule.waitForIdle()

        // Occlusion calculation should return expected result from each layout
        assertThat(box0Occlusions.size).isEqualTo(2)
        assertThat(box1Occlusions.size).isEqualTo(1)
        assertThat(box2Occlusions.size).isEqualTo(0)

        assertThat(box0CallbackCount).isEqualTo(1)
        assertThat(box1CallbackCount).isEqualTo(1)
        assertThat(box2CallbackCount).isEqualTo(1)

        // We change box2 so that it no longer occludes box0
        box2Fraction = 0.3f
        rule.waitForIdle()

        // Only box2 should receive a new callback, so most of the pre-existing information should
        // remain the same, except for box2CallbackCount
        assertThat(box0Occlusions.size).isEqualTo(2)
        assertThat(box1Occlusions.size).isEqualTo(1)
        assertThat(box2Occlusions.size).isEqualTo(0)

        assertThat(box0CallbackCount).isEqualTo(1)
        assertThat(box1CallbackCount).isEqualTo(1)
        assertThat(box2CallbackCount).isEqualTo(2)

        // Currently, it's possible to capture rectInfo and re-calculate occlusions
        // The new calculation should reflect one less occluding box
        assertThat(box0Bounds!!.calculateOcclusions().size).isEqualTo(1)
    }

    @Test
    fun rectChangedBoundsInModifierChain() {
        val boxSizePx = 100
        val paddingPx = 10
        val offsetPx = 130
        val childBoxSizePx = 60
        val siblingBoxSizePx = 55

        var bounds0 = IntRect.Zero
        var bounds1 = IntRect.Zero
        var bounds2 = IntRect.Zero
        var bounds3 = IntRect.Zero // child
        var bounds4 = IntRect.Zero // sibling
        rule.setContent {
            with(rule.density) {
                Column {
                    Box(
                        Modifier.size(boxSizePx.toDp())
                            .onLayoutRectChanged(0, 0) { bounds0 = it.boundsInRoot }
                            .padding(all = paddingPx.toDp())
                            .onLayoutRectChanged(0, 0) { bounds1 = it.boundsInRoot }
                            .offset(x = offsetPx.toDp(), y = offsetPx.toDp())
                            .onLayoutRectChanged(0, 0) { bounds2 = it.boundsInRoot }
                    ) {
                        Box(
                            Modifier.size(childBoxSizePx.toDp()).onLayoutRectChanged(0, 0) {
                                bounds3 = it.boundsInRoot
                            }
                        )
                    }
                    Box(
                        Modifier.size(siblingBoxSizePx.toDp()).onLayoutRectChanged(0, 0) {
                            bounds4 = it.boundsInRoot
                        }
                    )
                }
            }
        }
        assertEquals(IntOffset.Zero, bounds0.topLeft)
        assertEquals(IntSize(boxSizePx, boxSizePx), bounds0.size)

        // After padding
        val sizeAfterPadding = boxSizePx - (paddingPx * 2)
        assertEquals(IntOffset(paddingPx, paddingPx), bounds1.topLeft)
        assertEquals(IntSize(sizeAfterPadding, sizeAfterPadding), bounds1.size)

        // After offset
        assertEquals(IntOffset(paddingPx + offsetPx, paddingPx + offsetPx), bounds2.topLeft)
        assertEquals(IntSize(sizeAfterPadding, sizeAfterPadding), bounds2.size)

        // Child Box
        assertEquals(IntOffset(paddingPx + offsetPx, paddingPx + offsetPx), bounds3.topLeft)
        assertEquals(IntSize(childBoxSizePx, childBoxSizePx), bounds3.size)

        // Sibling Box
        assertEquals(IntOffset(0, boxSizePx), bounds4.topLeft)
        assertEquals(IntSize(siblingBoxSizePx, siblingBoxSizePx), bounds4.size)
    }

    @Test
    fun correctPositionInRootWhenOffsetIsProvidedByLayoutCooperation() {
        with(rule.density) {
            val containerSize = 100
            val width = 50
            val height = 40
            var actualPosition: IntOffset = IntOffset.Max
            rule.setContent {
                Layout(
                    content = {
                        Box(
                            Modifier.requiredSize(width.toDp(), height.toDp()).onLayoutRectChanged(
                                0,
                                0,
                            ) {
                                actualPosition = it.positionInRoot
                            }
                        )
                    }
                ) { measurables, _ ->
                    val placeable =
                        measurables.first().measure(Constraints.fixed(containerSize, containerSize))
                    layout(containerSize, containerSize) { placeable.place(0, 0) }
                }
            }

            rule.runOnIdle {
                val expectedLeft = ((containerSize - width) / 2)
                val expectedTop = ((containerSize - height) / 2)
                assertThat(actualPosition).isEqualTo(IntOffset(expectedLeft, expectedTop))
            }
        }
    }

    @Test
    fun updatingOffsetInParentsLayoutModifier() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(IntOffset(0), neverEqualPolicy())
            rule.setContent {
                Box(
                    Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val resolvedInMeasureOffset = offset
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(resolvedInMeasureOffset)
                        }
                    }
                ) {
                    Box(
                        modifier =
                            Modifier.size(10.dp).onLayoutRectChanged(0, 0) {
                                actualPosition = it.positionInRoot
                            }
                    )
                }
            }

            rule.runOnIdle { offset = IntOffset(20, 20) }

            rule.runOnIdle {
                // during the first rerun the parent might clear the cached offset for it
                // so we rerun again in order to update the cache and re-calculate the child
                // position based on it again
                offset = IntOffset(20, 20)
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 20)) }
        }
    }

    @Test
    fun testLayoutModifierPlacingWithOffsetAndScale() {
        var actualPosition: IntOffset = IntOffset.Max
        var actualPositionChild: IntOffset = IntOffset.Max
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box {
                    Box(
                        Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.placeWithLayer(10, 10) {
                                        scaleX = 2f
                                        scaleY = 2f
                                    }
                                }
                            }
                            .onLayoutRectChanged(0, 0) { actualPosition = it.positionInRoot }
                    ) {
                        Box(
                            Modifier.onLayoutRectChanged(0, 0) {
                                    actualPositionChild = it.positionInRoot
                                }
                                .size(10.dp)
                        )
                    }
                }
            }
        }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(5, 5)) }
        rule.runOnIdle { assertThat(actualPositionChild).isEqualTo(IntOffset(5, 5)) }
    }

    @Test
    fun removingLayoutModifierShouldInvalidateOffsetCacheForSubtree() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            var modifier by mutableStateOf(Modifier.offset { IntOffset(10, 0) })
            rule.setContent {
                Layout(
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(0, offset)
                                    }
                                }
                            }
                        }
                    },
                    modifier = modifier.then(Modifier.offset { IntOffset(10, 0) }),
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 0)) }

            rule.runOnIdle {
                offset = 5
                modifier = Modifier
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(10, 5)) }
        }
    }

    @Test
    fun removingLayerModifierShouldInvalidateOffsetCacheForSubtree() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            var modifier by mutableStateOf(Modifier.graphicsLayer { translationX = 10f })
            rule.setContent {
                Layout(
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(0, offset)
                                    }
                                }
                            }
                        }
                    },
                    modifier = modifier.graphicsLayer { translationX = 10f },
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(20, 0)) }

            rule.runOnIdle {
                offset = 5
                modifier = Modifier
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(10, 5)) }
        }
    }

    @Test
    fun stoppingPlacingWithLayerShouldInvalidateOffsetCacheForSubtree() {
        with(rule.density) {
            var actualPosition: IntOffset = IntOffset.Max
            var offset by mutableStateOf(0)
            var needLayer by mutableStateOf(true)
            rule.setContent {
                Layout(
                    content = {
                        Box(Modifier.size(10.dp)) {
                            Box {
                                Layout(
                                    content = {
                                        Box(
                                            Modifier.onLayoutRectChanged(0, 0) {
                                                actualPosition = it.positionInRoot
                                            }
                                        )
                                    }
                                ) { measurables, constraints ->
                                    val placeable = measurables.first().measure(constraints)
                                    layout(constraints.maxWidth, constraints.maxHeight) {
                                        placeable.place(0, offset)
                                    }
                                }
                            }
                        }
                    },
                    modifier =
                        Modifier.layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                if (needLayer) {
                                    placeable.placeWithLayer(0, 0) { translationX = 10f }
                                } else {
                                    placeable.place(0, 0)
                                }
                            }
                        },
                ) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
                }
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(10, 0)) }

            rule.runOnIdle {
                offset = 5
                needLayer = false
            }

            rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(0, 5)) }
        }
    }

    @Test
    fun updatingLayerBlockShouldInvalidateOffsetCacheForSubtree() {
        var actualPosition: IntOffset = IntOffset.Max
        var offset by mutableStateOf(0)
        rule.setContent {
            Layout(
                content = {
                    Box(Modifier.size(10.dp)) {
                        Box {
                            Layout(
                                content = {
                                    Box(
                                        Modifier.onLayoutRectChanged(0, 0) {
                                            actualPosition = it.positionInRoot
                                        }
                                    )
                                }
                            ) { measurables, constraints ->
                                val placeable = measurables.first().measure(constraints)
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.place(0, offset)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.graphicsLayer(translationX = offset.toFloat()),
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
            }
        }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(0, 0)) }

        rule.runOnIdle { offset = 5 }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(5, 5)) }
    }

    @Test
    fun updatingLayerBlockShouldCallCallbackOnTheSameNode() {
        var actualPosition: IntOffset = IntOffset.Max
        var offset by mutableStateOf(0)
        rule.setContent {
            Layout(
                modifier =
                    Modifier.graphicsLayer(translationX = offset.toFloat()).onLayoutRectChanged(
                        0,
                        0,
                    ) {
                        actualPosition = it.positionInRoot
                    }
            ) { _, constraints ->
                layout(constraints.maxWidth, constraints.maxHeight) {}
            }
        }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(0, 0)) }

        rule.runOnIdle { offset = 5 }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(5, 0)) }
    }

    @Test
    fun updatingLayerBlockOnAChildShouldCallCallbackOnTheSameNode() {
        var actualPosition: IntOffset = IntOffset.Max
        var layerBlock by mutableStateOf<GraphicsLayerScope.() -> Unit>({})
        rule.setContent {
            Layout(
                content = {
                    Box(
                        Modifier.onLayoutRectChanged(0, 0) { actualPosition = it.positionInRoot }
                            .size(10.dp)
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
                }
            }
        }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(0, 0)) }

        rule.runOnIdle { layerBlock = { translationX = 5f } }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(5, 0)) }
    }

    @Test
    fun updatingLayerParameterOnAChildShouldCallCallbackOnTheSameNode() {
        var actualPosition: IntOffset = IntOffset.Max
        var offset by mutableStateOf(0f)
        rule.setContent {
            Layout(
                content = {
                    Box(
                        Modifier.onLayoutRectChanged(0, 0) { actualPosition = it.positionInRoot }
                            .size(10.dp)
                    )
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    placeable.placeWithLayer(0, 0) { translationX = offset }
                }
            }
        }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(0, 0)) }

        rule.runOnIdle { offset = 5f }

        rule.runOnIdle { assertThat(actualPosition).isEqualTo(IntOffset(5, 0)) }
    }

    @Test
    fun correctPositionInRootWhenOffsetIsProvidedByLayoutCooperation_afterConstraintsChange() {
        with(rule.density) {
            var containerSize by mutableStateOf(100)
            val width = 50
            val height = 40
            var actualPosition: IntOffset = IntOffset.Max
            rule.setContent {
                Layout(
                    content = {
                        Box(Modifier.offset().requiredSize(width.toDp(), height.toDp())) {
                            Box(
                                Modifier.onLayoutRectChanged(0, 0) {
                                    actualPosition = it.positionInRoot
                                }
                            )
                        }
                    }
                ) { measurables, _ ->
                    val placeable =
                        measurables.first().measure(Constraints.fixed(containerSize, containerSize))
                    layout(containerSize, containerSize) { placeable.place(0, 0) }
                }
            }

            fun verifyCoordinates() {
                val expectedLeft = ((containerSize - width) / 2)
                val expectedTop = ((containerSize - height) / 2)
                assertThat(actualPosition).isEqualTo(IntOffset(expectedLeft, expectedTop))
            }

            rule.runOnIdle {
                verifyCoordinates()

                containerSize = 110
            }

            rule.runOnIdle { verifyCoordinates() }
        }
    }
}
