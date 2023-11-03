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

package androidx.compose.foundation.text2.input.internal.selection

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val TargetTag = "TargetLayout"

/**
 * These tests are mostly copied from TapGestureDetectorTest.
 */
@RunWith(JUnit4::class)
class TapAndDoubleTapTest {

    @get:Rule
    val rule = createComposeRule()

    private var tapped = false
    private var doubleTapped = false

    /** The time before a long press gesture attempts to win. */
    private val LongPressTimeoutMillis: Long = 500L

    /**
     * The maximum time from the start of the first tap to the start of the second
     * tap in a double-tap gesture.
     */
    private val DoubleTapTimeoutMillis: Long = 300L

    private val util = layoutWithGestureDetector {
        detectTapAndDoubleTap(
            onTap = {
                tapped = true
            }
        )
    }

    private val utilWithDoubleTap = layoutWithGestureDetector {
        detectTapAndDoubleTap(
            onTap = {
                tapped = true
            },
            onDoubleTap = {
                doubleTapped = true
            }
        )
    }

    private val nothingHandler: PointerInputChange.() -> Unit = {}

    private var initialPass: PointerInputChange.() -> Unit = nothingHandler
    private var finalPass: PointerInputChange.() -> Unit = nothingHandler

    @Before
    fun setup() {
        tapped = false
        doubleTapped = false
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
                        .size(10.toDp())
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

    @Test
    fun normalTap() {
        rule.setContent(util)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        assertFalse(tapped)

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            up(0)
        }

        assertTrue(tapped)
    }

    @Test
    fun normalTapWithDoubleTapDefined_butNotExecuted() {
        rule.setContent(utilWithDoubleTap)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            up(0)
        }

        // we don't wait for double tap, tap should be called immediately.
        assertTrue(tapped)
        assertFalse(doubleTapped)

        rule.mainClock.advanceTimeBy(DoubleTapTimeoutMillis + 10)

        assertTrue(tapped)
        assertFalse(doubleTapped)
    }

    @Test
    fun normalDoubleTap() {
        rule.setContent(utilWithDoubleTap)

        performTouch { down(0, Offset(5f, 5f)) }
        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertTrue(tapped)
        assertFalse(doubleTapped)

        rule.mainClock.advanceTimeBy(50)

        performTouch { down(0, Offset(5f, 5f)) }
        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertTrue(tapped)
        assertTrue(doubleTapped)
    }

    @Test
    fun normalLongPress() {
        rule.setContent(utilWithDoubleTap)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
        }

        assertFalse(tapped)
        assertFalse(doubleTapped)

        rule.mainClock.advanceTimeBy(LongPressTimeoutMillis + 10)

        assertFalse(tapped)
        assertFalse(doubleTapped)

        rule.mainClock.advanceTimeBy(500)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            up(0)
        }

        assertFalse(tapped)
        assertFalse(doubleTapped)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in
     * the callback not being invoked
     */
    @Test
    fun tapMiss() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(0)
        }

        assertFalse(tapped)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in
     * the callback not being invoked
     */
    @Test
    fun longPressMiss() {
        rule.setContent(utilWithDoubleTap)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
        }

        rule.mainClock.advanceTimeBy(LongPressTimeoutMillis + 10)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(0)
        }

        assertFalse(tapped)
        assertFalse(doubleTapped)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in
     * the callback not being invoked for double-tap
     */
    @Test
    fun doubleTapMiss() {
        rule.setContent(utilWithDoubleTap)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(tapped)

        rule.mainClock.advanceTimeBy(50)

        performTouch {
            down(1, Offset(5f, 5f))
            moveTo(1, Offset(15f, 15f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(1)
        }

        assertTrue(tapped)
        assertFalse(doubleTapped)
    }

    /**
     * After a first tap, a second tap should also be detected.
     */
    @Test
    fun secondTap() {
        rule.setContent(util)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(tapped)

        tapped = false

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(1, Offset(4f, 4f))
            up(1)
        }

        assertTrue(tapped)
    }

    /**
     * Clicking in the region with the up already consumed should result in the callback not
     * being invoked.
     */
    @Test
    fun consumedUpTap() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
        }

        assertFalse(tapped)

        performTouch(initialPass = { if (pressed != previousPressed) consume() }) {
            up(0)
        }

        assertFalse(tapped)
    }

    /**
     * Clicking in the region with the motion consumed should result in the callback not
     * being invoked.
     */
    @Test
    fun consumedMotionTap() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
        }

        performTouch(initialPass = { consume() }) {
            moveTo(0, Offset(6f, 2f))
        }

        rule.mainClock.advanceTimeBy(50)

        performTouch {
            up(0)
        }

        assertFalse(tapped)
    }

    /**
     * Ensure that two-finger taps work.
     */
    @Test
    fun twoFingerTap() {
        rule.setContent(util)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(1f, 1f))
        }

        assertFalse(tapped)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            down(1, Offset(9f, 5f))
        }

        assertFalse(tapped)

        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(0)
        }

        assertFalse(tapped)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            up(1)
        }

        assertTrue(tapped)
    }

    /**
     * A position change consumption on any finger should cause tap to cancel.
     */
    @Test
    fun twoFingerTapCancel() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(1f, 1f))
        }
        assertFalse(tapped)

        performTouch {
            down(1, Offset(9f, 5f))
        }

        performTouch(initialPass = { consume() }) {
            moveTo(0, Offset(5f, 5f))
        }
        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(0)
        }

        assertFalse(tapped)

        rule.mainClock.advanceTimeBy(50)
        performTouch(finalPass = { assertFalse(isConsumed) }) {
            up(1)
        }

        assertFalse(tapped)
    }
}
