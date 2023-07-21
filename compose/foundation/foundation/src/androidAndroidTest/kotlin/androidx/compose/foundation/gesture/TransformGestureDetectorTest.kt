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

package androidx.compose.foundation.gesture

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.testutils.TestViewConfiguration
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

private const val TargetTag = "TargetLayout"

@RunWith(Parameterized::class)
class TransformGestureDetectorTest(val panZoomLock: Boolean) {

    @get:Rule
    val rule = createComposeRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = arrayOf(false, true)
    }

    private var centroid = Offset.Zero
    private var panned = false
    private var panAmount = Offset.Zero
    private var rotated = false
    private var rotateAmount = 0f
    private var zoomed = false
    private var zoomAmount = 1f

    private val util = layoutWithGestureDetector {
        detectTransformGestures(
            panZoomLock = panZoomLock
        ) { c, pan, gestureZoom, gestureAngle ->
            centroid = c
            if (gestureAngle != 0f) {
                rotated = true
                rotateAmount += gestureAngle
            }
            if (gestureZoom != 1f) {
                zoomed = true
                zoomAmount *= gestureZoom
            }
            if (pan != Offset.Zero) {
                panned = true
                panAmount += pan
            }
        }
    }

    private val nothingHandler: PointerInputChange.() -> Unit = {}

    private var initialPass: PointerInputChange.() -> Unit = nothingHandler
    private var finalPass: PointerInputChange.() -> Unit = nothingHandler

    @Before
    fun setup() {
        panned = false
        panAmount = Offset.Zero
        rotated = false
        rotateAmount = 0f
        zoomed = false
        zoomAmount = 1f
    }

    private fun layoutWithGestureDetector(
        gestureDetector: suspend PointerInputScope.() -> Unit,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalDensity provides Density(1f),
            LocalViewConfiguration provides TestViewConfiguration(
                minimumTouchTargetSize = DpSize.Zero
            )
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // Some tests execute a lambda before the initial and final passes
                        // so they are called here, higher up the chain, so that the
                        // calls happen prior to the gestureDetector below. The lambdas
                        // do things like consume events on the initial pass or validate
                        // consumption on the final pass.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach {
                                        initialPass(it)
                                    }
                                    awaitPointerEvent(PointerEventPass.Final)
                                    event.changes.forEach {
                                        finalPass(it)
                                    }
                                }
                            }
                        }
                        .wrapContentSize(AbsoluteAlignment.TopLeft)
                        .size(1600.toDp())
                        .pointerInput(gestureDetector, gestureDetector)
                        .testTag(TargetTag)
                )
            }
        }
    }

    private fun performTouch(
        initialPass: PointerInputChange.() -> Unit = nothingHandler,
        finalPass: PointerInputChange.() -> Unit = nothingHandler,
        block: TouchInjectionScope.() -> Unit
    ) {
        this.initialPass = initialPass
        this.finalPass = finalPass
        rule.onNodeWithTag(TargetTag).performTouchInput(block)
        rule.waitForIdle()
        this.initialPass = nothingHandler
        this.finalPass = nothingHandler
    }

    /**
     * Single finger pan.
     */
    @Test
    fun singleFingerPan() {
        rule.setContent(util)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        assertFalse(panned)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            moveBy(0, Offset(12.7f, 12.7f))
        }

        assertFalse(panned)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            moveBy(0, Offset(0.1f, 0.1f))
        }

        assertEquals(17.7f, centroid.x, 0.1f)
        assertEquals(17.7f, centroid.y, 0.1f)
        assertTrue(panned)
        assertFalse(zoomed)
        assertFalse(rotated)

        assertTrue(panAmount.getDistance() < 1f)

        panAmount = Offset.Zero

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            moveBy(0, Offset(1f, 0f))
        }

        assertEquals(Offset(1f, 0f), panAmount)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(0)
        }

        assertFalse(rotated)
        assertFalse(zoomed)
    }

    /**
     * Multi-finger pan
     */
    @Test
    fun multiFingerPanZoom() {
        rule.setContent(util)

        // [PointerId] needed later to assert whether or not a particular pointer id was consumed.
        var pointerId0: PointerId? = null
        var pointerId1: PointerId? = null

        performTouch(
            finalPass = {
                pointerId0 = id
                assertFalse(isConsumed)
            }
        ) {
            down(0, Offset(5f, 5f))
        }

        performTouch(
            finalPass = {
                if (id != pointerId0) {
                    pointerId1 = id
                }
                assertFalse(isConsumed)
            }
        ) {
            down(1, Offset(25f, 25f))
        }

        assertFalse(panned)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            moveBy(0, Offset(13f, 13f))
        }

        // With the move below, we've now averaged enough movement (touchSlop is around 18.0)
        performTouch(
            finalPass = {
                if (id == pointerId1) {
                    assertTrue(isConsumed)
                }
            }
        ) {
            moveBy(1, Offset(13f, 13f))
        }

        assertEquals((5f + 25f + 13f) / 2f, centroid.x, 0.1f)
        assertEquals((5f + 25f + 13f) / 2f, centroid.y, 0.1f)
        assertTrue(panned)
        assertTrue(zoomed)
        assertFalse(rotated)

        assertEquals(6.4f, panAmount.x, 0.1f)
        assertEquals(6.4f, panAmount.y, 0.1f)

        performTouch {
            up(0)
            up(1)
        }
    }

    /**
     * 2-pointer zoom
     */
    @Test
    fun zoom2Pointer() {
        rule.setContent(util)

        // [PointerId] needed later to assert whether or not a particular pointer id was consumed.
        var pointerId0: PointerId? = null
        var pointerId1: PointerId? = null

        performTouch(
            finalPass = {
                pointerId0 = id
                assertFalse(isConsumed)
            }
        ) {
            down(0, Offset(5f, 5f))
        }

        performTouch(
            finalPass = {
                if (id != pointerId0) {
                    pointerId1 = id
                    assertFalse(isConsumed)
                }
            }
        ) {
            down(1, Offset(25f, 5f))
        }

        performTouch(
            finalPass = {
                if (id == pointerId1) {
                    assertFalse(isConsumed)
                }
            }
        ) {
            moveBy(1, Offset(35.95f, 0f))
        }

        performTouch(
            finalPass = {
                if (id == pointerId1) {
                    assertTrue(isConsumed)
                }
            }
        ) {
            moveBy(1, Offset(0.1f, 0f))
        }

        assertTrue(panned)
        assertTrue(zoomed)
        assertFalse(rotated)

        // both should be small movements
        assertTrue(panAmount.getDistance() < 1f)
        assertTrue(zoomAmount in 1f..1.1f)

        zoomAmount = 1f
        panAmount = Offset.Zero

        performTouch(
            finalPass = {
                if (id == pointerId0) {
                    assertTrue(isConsumed)
                }
            }
        ) {
            moveBy(0, Offset(-1f, 0f))
        }

        performTouch(
            finalPass = {
                if (id == pointerId1) {
                    assertTrue(isConsumed)
                }
            }
        ) {
            moveBy(1, Offset(1f, 0f))
        }

        assertEquals(0f, panAmount.x, 0.01f)
        assertEquals(0f, panAmount.y, 0.01f)

        assertEquals(48f / 46f, zoomAmount, 0.01f)

        performTouch {
            up(0)
            up(1)
        }
    }

    /**
     * 4-pointer zoom
     */
    @Test
    fun zoom4Pointer() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(0f, 50f))
        }

        // just get past the touch slop
        performTouch {
            moveBy(0, Offset(-1000f, 0f))
            moveBy(0, Offset(1000f, 0f))
        }

        panned = false
        panAmount = Offset.Zero

        performTouch {
            down(1, Offset(100f, 50f))
            down(2, Offset(50f, 0f))
            down(3, Offset(50f, 100f))
        }

        performTouch {
            moveBy(0, Offset(-50f, 0f))
            moveBy(1, Offset(50f, 0f))
        }

        assertTrue(zoomed)
        assertTrue(panned)

        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(1.5f, zoomAmount, 0.1f)

        performTouch {
            moveBy(2, Offset(0f, -50f))
            moveBy(3, Offset(0f, 50f))
        }

        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(2f, zoomAmount, 0.1f)

        performTouch {
            up(0)
            up(1)
            up(2)
            up(3)
        }
    }

    /**
     * 2 pointer rotation.
     */
    @Test
    fun rotation2Pointer() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(0f, 50f))
            down(1, Offset(100f, 50f))

            // Move
            moveBy(0, Offset(50f, -50f))
            moveBy(1, Offset(-50f, 50f))
        }

        // assume some of the above was touch slop
        assertTrue(rotated)
        rotateAmount = 0f
        rotated = false
        zoomAmount = 1f
        panAmount = Offset.Zero

        // now do the real rotation:
        performTouch {
            moveBy(0, Offset(-50f, 50f))
            moveBy(1, Offset(50f, -50f))
        }

        performTouch {
            up(0)
            up(1)
        }

        assertTrue(rotated)
        assertEquals(-90f, rotateAmount, 0.01f)
        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(1f, zoomAmount, 0.1f)
    }

    /**
     * 2 pointer rotation, with early panning.
     */
    @Test
    fun rotation2PointerLock() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(0f, 50f))
        }

        // just get past the touch slop with panning
        performTouch {
            moveBy(0, Offset(-1000f, 0f))
            moveBy(0, Offset(1000f, 0f))
        }

        performTouch {
            down(1, Offset(100f, 50f))
        }

        // now do the rotation:
        performTouch {
            moveBy(0, Offset(50f, -50f))
            moveBy(1, Offset(-50f, 50f))
        }

        performTouch {
            up(0)
            up(1)
        }

        if (panZoomLock) {
            assertFalse(rotated)
        } else {
            assertTrue(rotated)
            assertEquals(90f, rotateAmount, 0.01f)
        }
        assertEquals(0f, panAmount.x, 0.1f)
        assertEquals(0f, panAmount.y, 0.1f)
        assertEquals(1f, zoomAmount, 0.1f)
    }

    /**
     * Adding or removing a pointer won't change the current values
     */
    @Test
    fun noChangeOnPointerDownUp() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(0f, 50f))
            down(1, Offset(100f, 50f))

            moveBy(0, Offset(50f, -50f))
            moveBy(1, Offset(-50f, 50f))
        }

        // now we've gotten past the touch slop
        rotated = false
        panned = false
        zoomed = false

        performTouch {
            down(2, Offset(0f, 50f))
        }

        assertFalse(rotated)
        assertFalse(panned)
        assertFalse(zoomed)

        performTouch {
            down(3, Offset(100f, 50f))
        }

        assertFalse(rotated)
        assertFalse(panned)
        assertFalse(zoomed)

        performTouch {
            up(0)
            up(1)
            up(2)
            up(3)
        }

        assertFalse(rotated)
        assertFalse(panned)
        assertFalse(zoomed)
    }

    /**
     * Consuming position during touch slop will cancel the current gesture.
     */
    @Test
    fun touchSlopCancel() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
        }

        performTouch(initialPass = { consume() }) {
            moveBy(0, Offset(50f, 0f))
        }

        performTouch {
            up(0)
        }

        assertFalse(panned)
        assertFalse(zoomed)
        assertFalse(rotated)
    }

    /**
     * Consuming position after touch slop will cancel the current gesture.
     */
    @Test
    fun afterTouchSlopCancel() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
        }

        performTouch {
            moveBy(0, Offset(50f, 0f))
        }

        performTouch(initialPass = { consume() }) {
            moveBy(0, Offset(50f, 0f))
        }

        performTouch {
            up(0)
        }

        assertTrue(panned)
        assertFalse(zoomed)
        assertFalse(rotated)
        assertEquals(50f, panAmount.x, 0.1f)
    }
}