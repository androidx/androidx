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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.padding
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.spatial.RectInfo
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.sqrt
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
                                Modifier.onRectChanged(0, 0) { wrap1Position = it.windowRect }
                        )
                    } else {
                        Wrap(
                            minWidth = size,
                            minHeight = size,
                            modifier =
                                Modifier.onRectChanged(0, 0) { wrap2Position = it.windowRect }
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
                        Modifier.onRectChanged(0, 0) { realChildSize = it.rootRect.size.width }
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
                                Modifier.onRectChanged(0, 0) { rect ->
                                    childGlobalPosition = rect.rootRect.offset()
                                    latch.countDown()
                                }
                        )
                    }
                }
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
                        modifier = Modifier.onRectChanged(0, 0) { wrap1OnPositionedCalled = true }
                    )
                    Wrap(
                        minWidth = 10,
                        minHeight = 10,
                        modifier = Modifier.onRectChanged(0, 0) { wrap2OnPositionedCalled = true }
                    ) {
                        Wrap(
                            minWidth = 10,
                            minHeight = 10,
                            modifier = Modifier.onRectChanged(0, 0) { latch.countDown() }
                        )
                    }
                }
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
        val lambda1: (RectInfo) -> Unit = { lambda1Called = true }
        val lambda2: (RectInfo) -> Unit = { lambda2Called = true }

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
                        .onRectChanged(0, 0, if (changeLambda.value) lambda1 else lambda2)
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
            assertFalse(lambda2Called)
            assertFalse(lambda1Called)
            assertFalse(layoutCalled)
            assertFalse(placementCalled)
        }
    }

    @Test
    fun callbacksAreCalledOnlyOnceWhenLambdaChangesAndLayoutChanges() {
        var lambda1Called = false
        val lambda1: (RectInfo) -> Unit = {
            assert(!lambda1Called)
            lambda1Called = true
        }

        var lambda2Called = false
        val lambda2: (RectInfo) -> Unit = {
            assert(!lambda2Called)
            lambda2Called = true
        }

        val changeLambda = mutableStateOf(true)
        val size = mutableStateOf(100.dp)
        rule.setContent {
            Box(
                modifier =
                    Modifier.size(size.value)
                        .onRectChanged(0, 0, if (changeLambda.value) lambda1 else lambda2)
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
        val lambda1: (RectInfo) -> Unit = {
            assert(!lambda1Called)
            lambda1Called = true
        }

        var lambda2Called = false
        val lambda2: (RectInfo) -> Unit = {
            assert(!lambda2Called)
            lambda2Called = true
        }

        val changeLambda = mutableStateOf(true)
        val size = mutableStateOf(10.dp)
        rule.setContent {
            Box(
                modifier =
                    Modifier.padding(10.dp)
                        .onRectChanged(0, 0, if (changeLambda.value) lambda1 else lambda2)
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
                        Modifier.onRectChanged(0, 0) {
                            coordinates = it.windowRect
                            positionedLatch.countDown()
                        }
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
            positionedLatch.await(1, TimeUnit.SECONDS)
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
                        .onRectChanged(0, 0) {
                            coordinates = it.windowRect
                            positionedLatch.countDown()
                        }
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
            positionedLatch.await(1, TimeUnit.SECONDS)
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
                        Modifier.onRectChanged(0, 0) {
                            coordinates = it.windowRect
                            positionedLatch.countDown()
                        }
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
            positionedLatch.await(1, TimeUnit.SECONDS)
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
                            Modifier.requiredSize(size.toDp()).onRectChanged(0, 0) {
                                coordinates1 = it.rootRect
                            }
                        )
                    }
                    Box(Modifier.requiredSize(25.toDp())) {
                        Box(
                            Modifier.requiredSize(size.toDp()).onRectChanged(0, 0) {
                                coordinates2 = it.rootRect
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
                    Modifier.fillMaxSize().onRectChanged(0, 0) {
                        val position = IntArray(2)
                        composeView.getLocationInWindow(position)
                        frameGlobalPosition = IntOffset(position[0], position[1])

                        realGlobalPosition = it.windowRect.offset()

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
    fun testRepositionTriggersCallback() {
        val left = mutableStateOf(30)
        var realLeft: Int? = null

        rule.setContent {
            with(LocalDensity.current) {
                Box {
                    Box(
                        Modifier.fillMaxSize().padding(start = left.value.toDp()).onRectChanged(
                            0,
                            0
                        ) {
                            realLeft = it.rootRect.left
                        }
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
                                    Modifier.onRectChanged(0, 0) {
                                            realLeft = it.rootRect.left
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
        var rect: RectInfo? = null
        var view: View? = null
        var toggle by mutableStateOf(false)
        rule.setContent {
            view = LocalView.current
            if (toggle) {
                FixedSize(30, Modifier.padding(10).onRectChanged(0, 0) { rect = it }) { /* no-op */
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
            assertEquals(IntOffset(10, 10), layoutCoordinates.rootRect.offset())
            assertEquals(IntRect(10, 10, 40, 40), layoutCoordinates.rootRect)

            val boundsInWindow = layoutCoordinates.windowRect
            assertEquals(10f * sqrt(2f), boundsInWindow.top.toFloat(), 1f)
            assertEquals(30f * sqrt(2f) / 2f, boundsInWindow.right.toFloat(), 1f)
            assertEquals(-30f * sqrt(2f) / 2f, boundsInWindow.left.toFloat(), 1f)
            assertEquals(40f * sqrt(2f), boundsInWindow.bottom.toFloat(), 1f)
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
                        Modifier.padding(10).background(Color.Red).onRectChanged(0, 0) {
                            coords = it.windowRect
                        }
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
                    .onRectChanged(0, 0) { coords1 = it.windowRect }
                    .padding(2.dp)
                    .onRectChanged(0, 0) { coords2 = it.windowRect }
                    .padding(3.dp)
                    .onRectChanged(0, 0) { coords3 = it.windowRect }
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
        val lambda: (RectInfo) -> Unit = {}
        assertEquals(Modifier.onRectChanged(0, 0, lambda), Modifier.onRectChanged(0, 0, lambda))
    }

    @Test
    @SmallTest
    fun modifierIsReturningNotEqualObjectForDifferentLambdas() {
        val lambda1: (RectInfo) -> Unit = { print("foo") }
        val lambda2: (RectInfo) -> Unit = { print("bar") }
        Assert.assertNotEquals(
            Modifier.onRectChanged(0, 0, lambda1),
            Modifier.onRectChanged(0, 0, lambda2)
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
                        .onRectChanged(0, 0) {
                            if (offset != IntOffset.Zero) {
                                position = it.rootRect.offset()
                            }
                        }
                )
                Box(
                    Modifier.fillMaxSize()
                        .offset { offset }
                        .onRectChanged(0, 0) {
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
                            .onRectChanged(0, 0) { position = it.rootRect.offset() }
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
                    Modifier.onRectChanged(0, 0, remember { { positionCalled1Count++ } })
                } else {
                    Modifier
                }
            Box(
                Modifier
                    // Remember lambdas to avoid triggering a node update when the lambda changes
                    .onRectChanged(0, 0, remember { { positionCalled2Count++ } })
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
}
