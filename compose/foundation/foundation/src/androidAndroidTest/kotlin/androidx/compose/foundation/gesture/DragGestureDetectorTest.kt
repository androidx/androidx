/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalDragOrCancellation
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
class DragGestureDetectorTest(dragType: GestureType) {

    @get:Rule
    val rule = createComposeRule()

    enum class GestureType {
        VerticalDrag,
        HorizontalDrag,
        AwaitVerticalDragOrCancel,
        AwaitHorizontalDragOrCancel,
        AwaitDragOrCancel,
        DragWithVertical,
        DragWithHorizontal,
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun parameters() = GestureType.values()
    }

    private var dragDistance = 0f
    private var dragged = false
    private var gestureEnded = false
    private var gestureStarted = false
    private var gestureCanceled = false
    private var consumePositiveOnly = false
    private var sloppyDetector = false
    private var startOrder = -1
    private var endOrder = -1
    private var cancelOrder = -1
    private var dragOrder = -1

    private val DragTouchSlopUtil = layoutWithGestureDetector {
        var count = 0
        detectDragGestures(
            onDragStart = {
                gestureStarted = true
                startOrder = count++
            },
            onDragEnd = {
                gestureEnded = true
                endOrder = count++
            },
            onDragCancel = {
                gestureCanceled = true
                cancelOrder = count++
            }
        ) { change, dragAmount ->
            val positionChange = change.positionChange()
            dragOrder = count++
            if (positionChange.x > 0f || positionChange.y > 0f || !consumePositiveOnly) {
                change.consume()
                dragged = true
                dragDistance += dragAmount.getDistance()
            }
        }
    }

    private val VerticalTouchSlopUtil = layoutWithGestureDetector {
        var count = 0
        detectVerticalDragGestures(
            onDragStart = {
                gestureStarted = true
                startOrder = count++
            },
            onDragEnd = {
                gestureEnded = true
                endOrder = count++
            },
            onDragCancel = {
                gestureCanceled = true
                cancelOrder = count++
            }
        ) { change, dragAmount ->
            dragOrder = count++
            if (change.positionChange().y > 0f || !consumePositiveOnly) {
                dragged = true
                dragDistance += dragAmount
            }
        }
    }

    private val HorizontalTouchSlopUtil = layoutWithGestureDetector {
        var count = 0
        detectHorizontalDragGestures(
            onDragStart = {
                gestureStarted = true
                startOrder = count++
            },
            onDragEnd = {
                gestureEnded = true
                endOrder = count++
            },
            onDragCancel = {
                gestureCanceled = true
                cancelOrder = count++
            }
        ) { change, dragAmount ->
            dragOrder = count++
            if (change.positionChange().x > 0f || !consumePositiveOnly) {
                dragged = true
                dragDistance += dragAmount
            }
        }
    }

    private val AwaitVerticalDragUtil = layoutWithGestureDetector {
        awaitEachGesture {
            val down = awaitFirstDown()
            val slopChange = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
                if (change.positionChange().y > 0f || !consumePositiveOnly) {
                    dragged = true
                    dragDistance = overSlop
                    change.consume()
                }
            }
            if (slopChange != null || sloppyDetector) {
                gestureStarted = true
                var pointer = if (sloppyDetector) down.id else slopChange!!.id
                do {
                    val change = awaitVerticalDragOrCancellation(pointer)
                    if (change == null) {
                        gestureCanceled = true
                    } else {
                        dragDistance += change.positionChange().y
                        change.consume()
                        if (change.changedToUpIgnoreConsumed()) {
                            gestureEnded = true
                        }
                        pointer = change.id
                    }
                } while (!gestureEnded && !gestureCanceled)
            }
        }
    }

    private val AwaitHorizontalDragUtil = layoutWithGestureDetector {
        awaitEachGesture {
            val down = awaitFirstDown()
            val slopChange =
                awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                    if (change.positionChange().x > 0f || !consumePositiveOnly) {
                        dragged = true
                        dragDistance = overSlop
                        change.consume()
                    }
                }
            if (slopChange != null || sloppyDetector) {
                gestureStarted = true
                var pointer = if (sloppyDetector) down.id else slopChange!!.id
                do {
                    val change = awaitHorizontalDragOrCancellation(pointer)
                    if (change == null) {
                        gestureCanceled = true
                    } else {
                        dragDistance += change.positionChange().x
                        change.consume()
                        if (change.changedToUpIgnoreConsumed()) {
                            gestureEnded = true
                        }
                        pointer = change.id
                    }
                } while (!gestureEnded && !gestureCanceled)
            }
        }
    }

    private val AwaitDragUtil = layoutWithGestureDetector {
        awaitEachGesture {
            val down = awaitFirstDown()
            val slopChange = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                val positionChange = change.positionChange()
                if (positionChange.x > 0f || positionChange.y > 0f || !consumePositiveOnly) {
                    dragged = true
                    dragDistance = overSlop.getDistance()
                    change.consume()
                }
            }
            if (slopChange != null || sloppyDetector) {
                gestureStarted = true
                var pointer = if (sloppyDetector) down.id else slopChange!!.id
                do {
                    val change = awaitDragOrCancellation(pointer)
                    if (change == null) {
                        gestureCanceled = true
                    } else {
                        dragDistance += change.positionChange().getDistance()
                        change.consume()
                        if (change.changedToUpIgnoreConsumed()) {
                            gestureEnded = true
                        }
                        pointer = change.id
                    }
                } while (!gestureEnded && !gestureCanceled)
            }
        }
    }

    private val content = when (dragType) {
        GestureType.VerticalDrag -> VerticalTouchSlopUtil
        GestureType.HorizontalDrag -> HorizontalTouchSlopUtil
        GestureType.AwaitVerticalDragOrCancel -> AwaitVerticalDragUtil
        GestureType.AwaitHorizontalDragOrCancel -> AwaitHorizontalDragUtil
        GestureType.AwaitDragOrCancel -> AwaitDragUtil
        GestureType.DragWithVertical -> DragTouchSlopUtil
        GestureType.DragWithHorizontal -> DragTouchSlopUtil
    }

    private val dragMotion = when (dragType) {
        GestureType.VerticalDrag,
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.DragWithVertical -> Offset(0f, 18f)
        else -> Offset(18f, 0f)
    }

    private val crossDragMotion = when (dragType) {
        GestureType.VerticalDrag,
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.DragWithVertical -> Offset(18f, 0f)
        else -> Offset(0f, 18f)
    }

    private val twoAxisDrag = when (dragType) {
        GestureType.DragWithVertical,
        GestureType.DragWithHorizontal,
        GestureType.AwaitDragOrCancel -> true
        else -> false
    }

    private val supportsSloppyGesture = when (dragType) {
        GestureType.AwaitVerticalDragOrCancel,
        GestureType.AwaitHorizontalDragOrCancel,
        GestureType.AwaitDragOrCancel -> true
        else -> false
    }

    private val nothingHandler: PointerInputChange.() -> Unit = {}

    private var initialPass: PointerInputChange.() -> Unit = nothingHandler
    private var finalPass: PointerInputChange.() -> Unit = nothingHandler

    @Before
    fun setup() {
        dragDistance = 0f
        dragged = false
        gestureEnded = false

        rule.setContent(content)
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
                        .pointerInput(Unit) {
                            // Some tests execute a lambda before the initial and final passes
                            // so they are called here, higher up the chain, so that the
                            // calls happen prior to the gestureDetector below. The lambdas
                            // do things like consume events on the initial pass or validate
                            // consumption on the final pass.
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
                        .size(100.toDp())
                        .pointerInput(key1 = gestureDetector, block = gestureDetector)
                        .testTag(TargetTag)
                )
            }
        }
    }

    /**
     * Executes [block] on the [TargetTag] layout. The optional [initialPass] is executed
     * prior to the [PointerEventPass.Initial] and [finalPass] is executed before
     * [PointerEventPass.Final] of the gesture detector.
     */
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
     * A normal drag, just to ensure that the drag worked.
     */
    @Test
    fun normalDrag() {
        performTouch {
            down(Offset.Zero)
            moveBy(dragMotion)
        }
        assertTrue(gestureStarted)
        assertTrue(dragged)
        assertEquals(0f, dragDistance)
        performTouch {
            moveBy(dragMotion)
        }
        assertEquals(18f, dragDistance)
        assertFalse(gestureEnded)
        performTouch { up() }
        assertTrue(gestureEnded)
    }

    /**
     * A drag in the opposite direction doesn't cause a drag event.
     */
    @Test
    fun crossDrag() {
        if (!twoAxisDrag) {
            performTouch {
                down(Offset.Zero)
                moveBy(crossDragMotion)
                up()
            }
            assertFalse(gestureStarted)
            assertFalse(dragged)
            assertFalse(gestureEnded)

            // now try a normal drag to ensure that it is still working.
            performTouch {
                down(Offset.Zero)
                moveBy(dragMotion)
                up()
            }
            assertTrue(gestureStarted)
            assertTrue(dragged)
            assertEquals(0f, dragDistance)
            assertTrue(gestureEnded)
        }
    }

    /**
     * Use two fingers and lift the finger before the touch slop is reached.
     */
    @Test
    fun twoFingerDrag_upBeforeSlop() {
        performTouch {
            down(0, Offset.Zero)
            down(1, Offset.Zero)
        }

        // second finger shouldn't cause a drag. It should follow finger1
        performTouch {
            moveBy(1, dragMotion)
        }

        assertFalse(gestureStarted)
        assertFalse(dragged)

        // now it should move to finger 2
        performTouch {
            up(0)
        }

        performTouch {
            moveBy(1, dragMotion)
            moveBy(1, dragMotion)
            up(1)
        }

        assertTrue(dragged)
        assertEquals(18f, dragDistance)
        assertTrue(gestureEnded)
    }

    /**
     * Use two fingers and lift the finger after the touch slop is reached.
     */
    @Test
    fun twoFingerDrag_upAfterSlop() {
        performTouch {
            down(0, Offset.Zero)
            down(1, Offset.Zero)
            moveBy(0, dragMotion)
            up(0)
        }

        assertTrue(gestureStarted)
        assertTrue(dragged)
        assertEquals(0f, dragDistance)
        assertFalse(gestureEnded)

        performTouch {
            moveBy(1, dragMotion)
            up(1)
        }

        assertEquals(18f, dragDistance)
        assertTrue(gestureEnded)
    }

    /**
     * Cancel drag during touch slop
     */
    @Test
    fun cancelDragDuringSlop() {
        performTouch {
            down(Offset.Zero)
        }
        performTouch(initialPass = { consume() }) {
            moveBy(dragMotion)
        }
        performTouch {
            up()
        }
        assertFalse(gestureStarted)
        assertFalse(dragged)
        assertFalse(gestureEnded)
        assertFalse(gestureCanceled) // only canceled if the touch slop was crossed first
    }

    /**
     * Cancel drag after touch slop
     */
    @Test
    fun cancelDragAfterSlop() {
        performTouch {
            down(Offset.Zero)
            moveBy(dragMotion)
        }
        performTouch(initialPass = { consume() }) {
            moveBy(dragMotion)
        }
        performTouch {
            up()
        }
        assertTrue(gestureStarted)
        assertTrue(dragged)
        assertFalse(gestureEnded)
        assertTrue(gestureCanceled)
        assertEquals(0f, dragDistance)
    }

    /**
     * When this drag direction is more than the other drag direction, it should have priority
     * in locking the orientation.
     */
    @Test
    fun dragLockedWithPriority() {
        if (!twoAxisDrag) {
            performTouch {
                down(Offset.Zero)
            }
            // This should have priority because it has moved more than the other direction.
            performTouch(finalPass = { assertTrue(isConsumed) }) {
                moveBy((dragMotion * 2f) + crossDragMotion)
            }
            performTouch {
                up()
            }
            assertTrue(gestureStarted)
            assertTrue(dragged)
            assertTrue(gestureEnded)
            assertFalse(gestureCanceled)
            assertEquals(18f, dragDistance)
        }
    }

    /**
     * When a drag is not consumed, it should lead to the touch slop being reset. This is
     * important when you drag your finger to
     */
    @Test
    fun dragBackAndForth() {
        if (supportsSloppyGesture) {
            try {
                consumePositiveOnly = true

                performTouch {
                    down(Offset.Zero)
                    moveBy(-dragMotion)
                }

                assertFalse(gestureStarted)
                assertFalse(dragged)
                performTouch {
                    moveBy(dragMotion)
                    up()
                }

                assertTrue(gestureStarted)
                assertTrue(dragged)
            } finally {
                consumePositiveOnly = false
            }
        }
    }

    /**
     * When gesture detectors use the wrong pointer for the drag, it should just not
     * detect the touch.
     */
    @Test
    fun pointerUpTooQuickly() {
        if (supportsSloppyGesture) {
            try {
                sloppyDetector = true

                performTouch {
                    down(0, Offset.Zero)
                    down(1, Offset.Zero)
                    up(0)
                    moveBy(1, dragMotion)
                    up(1)
                }

                // The sloppy detector doesn't know to look at finger2
                assertTrue(gestureCanceled)
            } finally {
                sloppyDetector = false
            }
        }
    }

    @Test
    fun dragGestureCallbackOrder_normalFinish() {
        if (!supportsSloppyGesture) {
            performTouch {
                down(Offset.Zero)
                moveBy(Offset(50f, 50f))
            }
            assertTrue(startOrder < dragOrder)
            performTouch {
                up()
            }
            assertTrue(startOrder < dragOrder)
            assertTrue(dragOrder < endOrder)
            assertTrue(cancelOrder == -1)
        }
    }

    @Test
    fun dragGestureCallbackOrder_cancel() {
        if (!supportsSloppyGesture) {
            performTouch {
                down(Offset.Zero)
                moveBy(dragMotion)
            }
            performTouch(initialPass = { consume() }) {
                moveBy(dragMotion)
            }
            assertTrue(startOrder < dragOrder)
            assertTrue(dragOrder < cancelOrder)
            assertTrue(endOrder == -1)
        }
    }

    // An error in the pointer input stream should stop the gesture without error.
    @Test
    fun interruptedBeforeDrag() {
        performTouch {
            down(Offset.Zero)
            cancel()
        }
        // The next stream doesn't have the existing pointer, so we lose the dragging
        performTouch {
            down(Offset.Zero)
            up()
        }
        assertFalse(gestureStarted)
    }

    // An error in the pointer input stream should stop the gesture without error.
    @Test
    fun interruptedBeforeTouchSlop() {
        performTouch {
            down(Offset.Zero)
            moveBy(dragMotion / 2f)
            cancel()
        }
        // The next stream doesn't have the existing pointer, so we lose the dragging
        performTouch {
            down(Offset.Zero)
            up()
        }
        assertFalse(gestureStarted)
    }

    // An error in the pointer input stream should end in a drag cancellation.
    @Test
    fun interruptedAfterTouchSlop() {
        performTouch {
            down(Offset.Zero)
            moveBy(dragMotion * 2f)
            cancel()
        }
        // The next stream doesn't have the existing pointer, so we lose the dragging
        performTouch {
            down(Offset.Zero)
            up()
        }
        assertTrue(gestureCanceled)
    }
}