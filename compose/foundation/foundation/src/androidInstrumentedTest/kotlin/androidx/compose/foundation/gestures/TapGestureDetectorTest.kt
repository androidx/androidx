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

package androidx.compose.foundation.gestures

import android.os.Build
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.CLASSIFICATION_DEEP_PRESS
import android.view.MotionEvent.CLASSIFICATION_NONE
import android.view.MotionEvent.PointerCoords
import android.view.View
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val TargetTag = "TargetLayout"

@RunWith(JUnit4::class)
class TapGestureDetectorTest {

    @get:Rule val rule = createComposeRule()

    private var pressed = false
    private var released = false
    private var canceled = false
    private var tapped = false
    private var doubleTapped = false
    private var longPressed = false

    /** The time before a long press gesture attempts to win. */
    private val LongPressTimeoutMillis: Long = 500L

    /**
     * The maximum time from the start of the first tap to the start of the second tap in a
     * double-tap gesture.
     */
    // TODO(shepshapard): In Android, this is actually the time from the first's up event
    // to the second's down event, according to the ViewConfiguration docs.
    private val DoubleTapTimeoutMillis: Long = 300L

    private val util = layoutWithGestureDetector {
        detectTapGestures(
            onPress = {
                pressed = true
                if (tryAwaitRelease()) {
                    released = true
                } else {
                    canceled = true
                }
            },
            onTap = { tapped = true }
        )
    }

    private val utilWithShortcut = layoutWithGestureDetector {
        detectTapAndPress(
            onPress = {
                pressed = true
                if (tryAwaitRelease()) {
                    released = true
                } else {
                    canceled = true
                }
            },
            onTap = { tapped = true }
        )
    }

    private val allGestures = layoutWithGestureDetector {
        detectTapGestures(
            onPress = {
                pressed = true
                try {
                    awaitRelease()
                    released = true
                } catch (_: GestureCancellationException) {
                    canceled = true
                }
            },
            onTap = { tapped = true },
            onLongPress = { longPressed = true },
            onDoubleTap = { doubleTapped = true }
        )
    }

    private val nothingHandler: PointerInputChange.() -> Unit = {}

    private var initialPass: PointerInputChange.() -> Unit = nothingHandler
    private var finalPass: PointerInputChange.() -> Unit = nothingHandler

    @Before
    fun setup() {
        pressed = false
        released = false
        canceled = false
        tapped = false
        doubleTapped = false
        longPressed = false
    }

    private fun layoutWithGestureDetector(
        gestureDetector: suspend PointerInputScope.() -> Unit,
    ): @Composable () -> Unit = {
        CompositionLocalProvider(
            LocalDensity provides Density(1f),
            LocalViewConfiguration provides
                TestViewConfiguration(minimumTouchTargetSize = DpSize.Zero)
        ) {
            with(LocalDensity.current) {
                Box(
                    Modifier.fillMaxSize()
                        // Some tests execute a lambda before the initial and final passes
                        // so they are called here, higher up the chain, so that the
                        // calls happen prior to the gestureDetector below. The lambdas
                        // do things like consume events on the initial pass or validate
                        // consumption on the final pass.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { initialPass(it) }
                                    awaitPointerEvent(PointerEventPass.Final)
                                    event.changes.forEach { finalPass(it) }
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

    /** Clicking in the region should result in the callback being invoked. */
    @Test
    fun normalTap() {
        rule.setContent(util)

        performTouch(finalPass = { assertTrue(isConsumed) }) { down(0, Offset(5f, 5f)) }

        assertTrue(pressed)
        assertFalse(tapped)
        assertFalse(released)

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertTrue(tapped)
        assertTrue(released)
        assertFalse(canceled)
    }

    /** Clicking in the region should result in the callback being invoked. */
    @Test
    fun normalTap_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch(finalPass = { assertTrue(isConsumed) }) { down(0, Offset(5f, 5f)) }

        assertTrue(pressed)
        assertFalse(tapped)
        assertFalse(released)

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertTrue(tapped)
        assertTrue(released)
        assertFalse(canceled)
    }

    /** Clicking in the region should result in the callback being invoked. */
    @Test
    fun normalTapWithAllGestures() {
        rule.setContent(allGestures)

        performTouch(finalPass = { assertTrue(isConsumed) }) { down(0, Offset(5f, 5f)) }

        assertTrue(pressed)

        rule.mainClock.advanceTimeBy(50)

        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertTrue(released)

        // we have to wait for the double-tap timeout before we receive an event

        assertFalse(tapped)
        assertFalse(doubleTapped)

        rule.mainClock.advanceTimeBy(DoubleTapTimeoutMillis + 10)

        assertTrue(tapped)
        assertFalse(doubleTapped)
    }

    /** Clicking in the region should result in the callback being invoked. */
    @Test
    fun normalDoubleTap() {
        rule.setContent(allGestures)

        performTouch { down(0, Offset(5f, 5f)) }
        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertTrue(pressed)
        assertTrue(released)
        assertFalse(tapped)
        assertFalse(doubleTapped)

        pressed = false
        released = false

        rule.mainClock.advanceTimeBy(50)

        performTouch { down(0, Offset(5f, 5f)) }
        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertTrue(doubleTapped)
        assertTrue(pressed)
        assertTrue(released)
    }

    /** Long press in the region should result in the callback being invoked. */
    @Test
    fun normalLongPress() {
        rule.setContent(allGestures)

        performTouch(finalPass = { assertTrue(isConsumed) }) { down(0, Offset(5f, 5f)) }

        assertTrue(pressed)

        rule.mainClock.advanceTimeBy(LongPressTimeoutMillis + 10)

        assertTrue(longPressed)

        rule.mainClock.advanceTimeBy(500)

        performTouch(finalPass = { assertTrue(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertFalse(doubleTapped)
        assertTrue(released)
        assertFalse(canceled)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in the callback not being
     * invoked
     */
    @Test
    fun tapMiss() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertTrue(pressed)
        assertTrue(canceled)
        assertFalse(released)
        assertFalse(tapped)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in the callback not being
     * invoked
     */
    @Test
    fun tapMiss_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertTrue(pressed)
        assertTrue(canceled)
        assertFalse(released)
        assertFalse(tapped)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in the callback not being
     * invoked
     */
    @Test
    fun longPressMiss() {
        rule.setContent(allGestures)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
        }

        rule.mainClock.advanceTimeBy(LongPressTimeoutMillis + 10)

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
        assertFalse(tapped)
        assertFalse(longPressed)
        assertFalse(doubleTapped)
    }

    /**
     * Pressing in the region, sliding out and then lifting should result in the callback not being
     * invoked for double-tap
     */
    @Test
    fun doubleTapMiss() {
        rule.setContent(allGestures)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)

        pressed = false
        released = false

        rule.mainClock.advanceTimeBy(50)

        performTouch {
            down(1, Offset(5f, 5f))
            moveTo(1, Offset(15f, 15f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(1) }

        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
        assertTrue(tapped)
        assertFalse(longPressed)
        assertFalse(doubleTapped)
    }

    /**
     * Pressing in the region, sliding out, then back in, then lifting should result the gesture
     * being canceled.
     */
    @Test
    fun tapOutAndIn() {
        rule.setContent(util)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
            moveTo(0, Offset(6f, 6f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
    }

    /**
     * Pressing in the region, sliding out, then back in, then lifting should result the gesture
     * being canceled.
     */
    @Test
    fun tapOutAndIn_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch {
            down(0, Offset(5f, 5f))
            moveTo(0, Offset(15f, 15f))
            moveTo(0, Offset(6f, 6f))
        }

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
    }

    /** After a first tap, a second tap should also be detected. */
    @Test
    fun secondTap() {
        rule.setContent(util)

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)

        tapped = false
        pressed = false
        released = false

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(1, Offset(4f, 4f))
            up(1)
        }

        assertTrue(tapped)
        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)
    }

    /** After a first tap, a second tap should also be detected. */
    @Test
    fun secondTap_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)

        tapped = false
        pressed = false
        released = false

        performTouch(finalPass = { assertTrue(isConsumed) }) {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(tapped)
        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)
    }

    /**
     * Clicking in the region with the up already consumed should result in the callback not being
     * invoked.
     */
    @Test
    fun consumedUpTap() {
        rule.setContent(util)

        performTouch { down(0, Offset(5f, 5f)) }

        assertFalse(tapped)
        assertTrue(pressed)

        performTouch(initialPass = { if (pressed != previousPressed) consume() }) { up(0) }

        assertFalse(tapped)
        assertFalse(released)
        assertTrue(canceled)
    }

    /**
     * Clicking in the region with the up already consumed should result in the callback not being
     * invoked.
     */
    @Test
    fun consumedUpTap_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch { down(0, Offset(5f, 5f)) }

        assertFalse(tapped)
        assertTrue(pressed)

        performTouch(initialPass = { if (pressed != previousPressed) consume() }) { up(0) }

        assertFalse(tapped)
        assertFalse(released)
        assertTrue(canceled)
    }

    /**
     * Clicking in the region with the motion consumed should result in the callback not being
     * invoked.
     */
    @Test
    fun consumedMotionTap() {
        rule.setContent(util)

        performTouch { down(0, Offset(5f, 5f)) }

        performTouch(initialPass = { consume() }) { moveTo(0, Offset(6f, 2f)) }

        rule.mainClock.advanceTimeBy(50)

        performTouch { up(0) }

        assertFalse(tapped)
        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
    }

    /**
     * Clicking in the region with the motion consumed should result in the callback not being
     * invoked.
     */
    @Test
    fun consumedMotionTap_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch { down(0, Offset(5f, 5f)) }

        performTouch(initialPass = { consume() }) { moveTo(0, Offset(6f, 2f)) }

        rule.mainClock.advanceTimeBy(50)

        performTouch { up(0) }

        assertFalse(tapped)
        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
    }

    @Test
    fun consumedChange_MotionTap() {
        rule.setContent(util)

        performTouch { down(0, Offset(5f, 5f)) }

        performTouch(initialPass = { consume() }) { moveTo(0, Offset(6f, 2f)) }

        rule.mainClock.advanceTimeBy(50)

        performTouch { up(0) }

        assertFalse(tapped)
        assertTrue(pressed)
        assertFalse(released)
        assertTrue(canceled)
    }

    /**
     * Clicking in the region with the up already consumed should result in the callback not being
     * invoked.
     */
    @Test
    fun consumedChange_upTap() {
        rule.setContent(util)

        performTouch { down(0, Offset(5f, 5f)) }

        assertFalse(tapped)
        assertTrue(pressed)

        performTouch(initialPass = { consume() }) { up(0) }

        assertFalse(tapped)
        assertFalse(released)
        assertTrue(canceled)
    }

    /** Ensure that two-finger taps work. */
    @Test
    fun twoFingerTap() {
        rule.setContent(util)

        performTouch(finalPass = { assertTrue(isConsumed) }) { down(0, Offset(1f, 1f)) }

        assertTrue(pressed)
        pressed = false

        performTouch(finalPass = { assertFalse(isConsumed) }) { down(1, Offset(9f, 5f)) }

        assertFalse(pressed)

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertFalse(released)

        performTouch(finalPass = { assertTrue(isConsumed) }) { up(1) }

        assertTrue(tapped)
        assertTrue(released)
        assertFalse(canceled)
    }

    /** Ensure that two-finger taps work. */
    @Test
    fun twoFingerTap_withShortcut() {
        rule.setContent(utilWithShortcut)

        performTouch(finalPass = { assertTrue(isConsumed) }) { down(0, Offset(1f, 1f)) }

        assertTrue(pressed)
        pressed = false

        performTouch(finalPass = { assertFalse(isConsumed) }) { down(1, Offset(9f, 5f)) }

        assertFalse(pressed)

        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertFalse(released)

        performTouch(finalPass = { assertTrue(isConsumed) }) { up(1) }

        assertTrue(tapped)
        assertTrue(released)
        assertFalse(canceled)
    }

    /** A position change consumption on any finger should cause tap to cancel. */
    @Test
    fun twoFingerTapCancel() {
        rule.setContent(util)

        performTouch { down(0, Offset(1f, 1f)) }
        assertTrue(pressed)

        performTouch { down(1, Offset(9f, 5f)) }

        performTouch(initialPass = { consume() }) { moveTo(0, Offset(5f, 5f)) }
        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertTrue(canceled)

        rule.mainClock.advanceTimeBy(50)
        performTouch(finalPass = { assertFalse(isConsumed) }) { up(1) }

        assertFalse(tapped)
        assertFalse(released)
    }

    /** A position change consumption on any finger should cause tap to cancel. */
    @Test
    fun twoFingerTapCancel_withShortcut() {
        rule.setContent(utilWithShortcut)
        performTouch { down(0, Offset(1f, 1f)) }

        assertTrue(pressed)

        performTouch { down(1, Offset(9f, 5f)) }

        performTouch(initialPass = { consume() }) { moveTo(0, Offset(5f, 5f)) }
        performTouch(finalPass = { assertFalse(isConsumed) }) { up(0) }

        assertFalse(tapped)
        assertTrue(canceled)

        rule.mainClock.advanceTimeBy(50)
        performTouch(finalPass = { assertFalse(isConsumed) }) { up(1) }

        assertFalse(tapped)
        assertFalse(released)
    }

    /** Detect the second tap as long press. */
    @Test
    fun secondTapLongPress() {
        rule.setContent(allGestures)

        performTouch {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)
        assertFalse(tapped)
        assertFalse(doubleTapped)
        assertFalse(longPressed)

        pressed = false
        released = false

        rule.mainClock.advanceTimeBy(50)
        performTouch { down(1, Offset(5f, 5f)) }

        assertTrue(pressed)

        rule.mainClock.advanceTimeBy(LongPressTimeoutMillis + 10)

        // The first tap was part of a double tap, which then became a long press, so we don't
        // retroactively treat it as a tap to avoid triggering both a tap and a long press at the
        // same time
        assertFalse(tapped)
        assertTrue(longPressed)
        assertFalse(released)
        assertFalse(canceled)
        assertFalse(doubleTapped)

        rule.mainClock.advanceTimeBy(500)
        performTouch { up(1) }
        assertTrue(released)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun longPress_deepPress() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            allGestures()
        }

        rule.waitForIdle()

        val pointerProperties =
            arrayOf(
                MotionEvent.PointerProperties().also {
                    it.id = 0
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            )

        val downEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 0,
                /* action = */ ACTION_DOWN,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    PointerCoords().apply {
                        x = 5f
                        y = 5f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_NONE
            )

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        assertTrue(pressed)
        assertFalse(longPressed)

        val deepPressMoveEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 50,
                /* action = */ ACTION_MOVE,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    PointerCoords().apply {
                        x = 10f
                        y = 10f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_DEEP_PRESS
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        assertTrue(pressed)
        // Even though the timeout didn't pass, the deep press should immediately trigger the long
        // press
        assertTrue(longPressed)
        assertFalse(tapped)
        assertFalse(released)
        assertFalse(canceled)
        assertFalse(doubleTapped)
    }

    /** Detect the second deep press as long press. */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @Test
    fun secondTapLongPress_deepPress() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            allGestures()
        }

        performTouch {
            down(0, Offset(5f, 5f))
            up(0)
        }

        assertTrue(pressed)
        assertTrue(released)
        assertFalse(canceled)
        assertFalse(tapped)
        assertFalse(doubleTapped)
        assertFalse(longPressed)

        pressed = false
        released = false

        rule.mainClock.advanceTimeBy(50)

        val pointerProperties =
            arrayOf(
                MotionEvent.PointerProperties().also {
                    it.id = 0
                    it.toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            )

        val downEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 50,
                /* action = */ ACTION_DOWN,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    PointerCoords().apply {
                        x = 5f
                        y = 5f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_NONE
            )

        view.dispatchTouchEvent(downEvent)
        rule.mainClock.advanceTimeBy(50)

        assertTrue(pressed)
        assertFalse(longPressed)
        assertFalse(tapped)

        val deepPressMoveEvent =
            MotionEvent.obtain(
                /* downTime = */ 0,
                /* eventTime = */ 100,
                /* action = */ ACTION_MOVE,
                /* pointerCount = */ 1,
                /* pointerProperties = */ pointerProperties,
                /* pointerCoords = */ arrayOf(
                    PointerCoords().apply {
                        x = 10f
                        y = 10f
                    }
                ),
                /* metaState = */ 0,
                /* buttonState = */ 0,
                /* xPrecision = */ 0f,
                /* yPrecision = */ 0f,
                /* deviceId = */ 0,
                /* edgeFlags = */ 0,
                /* source = */ InputDevice.SOURCE_TOUCHSCREEN,
                /* displayId = */ 0,
                /* flags = */ 0,
                /* classification = */ CLASSIFICATION_DEEP_PRESS
            )

        view.dispatchTouchEvent(deepPressMoveEvent)
        rule.mainClock.advanceTimeBy(50)

        assertTrue(pressed)
        // Even though the timeout didn't pass, the deep press should immediately trigger the long
        // press
        assertTrue(longPressed)
        assertFalse(tapped)
        assertFalse(released)
        assertFalse(canceled)
        assertFalse(doubleTapped)
    }
}
