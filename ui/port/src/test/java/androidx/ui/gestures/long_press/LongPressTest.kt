/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.gestures.long_press

import androidx.ui.async.Timer
import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.gesture_tester.ensureGestureBinding
import androidx.ui.gestures.gesture_tester.gestureArena
import androidx.ui.gestures.gesture_tester.pointerRouter
import androidx.ui.gestures.tap.TapGestureRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.TestCoroutineContext
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class LongPressTest {

    private val down = PointerDownEvent(
        pointer = 5,
        position = Offset(10.0f, 10.0f)
    )
    private val up = PointerUpEvent(
        pointer = 5,
        position = Offset(11.0f, 9.0f)
    )

    private val testCoroutineContext = TestCoroutineContext()
    private lateinit var job: Job

    @Before
    fun setup() {
        ensureGestureBinding()
        job = Job()
        Timer.scope = CoroutineScope(testCoroutineContext + job)
    }

    @After
    fun teardown() {
        job.cancel()
    }

    @Test
    fun `Should recognize long press`() {
        val longPress = LongPressGestureRecognizer()

        var longPressRecognized = false
        longPress.onLongPress = {
            longPressRecognized = true
        }

        longPress.addPointer(down)
        gestureArena.close(5)
        assertThat(longPressRecognized, `is`(false))
        pointerRouter.route(down)
        assertThat(longPressRecognized, `is`(false))
        testCoroutineContext.advanceTimeBy(300)
        assertThat(longPressRecognized, `is`(false))
        testCoroutineContext.advanceTimeBy(700)
        assertThat(longPressRecognized, `is`(true))

        longPress.dispose()
    }

    @Test
    fun `Up cancels long press`() {
        val longPress = LongPressGestureRecognizer()

        var longPressRecognized = false
        longPress.onLongPress = {
            longPressRecognized = true
        }

        longPress.addPointer(down)
        gestureArena.close(5)
        assertThat(longPressRecognized, `is`(false))
        pointerRouter.route(down)
        assertThat(longPressRecognized, `is`(false))
        testCoroutineContext.advanceTimeBy(300)
        assertThat(longPressRecognized, `is`(false))
        pointerRouter.route(up)
        assertThat(longPressRecognized, `is`(false))
        testCoroutineContext.advanceTimeBy(1, TimeUnit.SECONDS)
        assertThat(longPressRecognized, `is`(false))

        longPress.dispose()
    }

    @Test
    fun `Should recognize both tap down and long press`() {
        val longPress = LongPressGestureRecognizer()
        val tap = TapGestureRecognizer()

        var tapDownRecognized = false
        tap.onTapDown = {
            tapDownRecognized = true
        }

        var longPressRecognized = false
        longPress.onLongPress = {
            longPressRecognized = true
        }

        tap.addPointer(down)
        longPress.addPointer(down)
        gestureArena.close(5)
        assertThat(tapDownRecognized, `is`(false))
        assertThat(longPressRecognized, `is`(false))
        pointerRouter.route(down)
        assertThat(tapDownRecognized, `is`(false))
        assertThat(longPressRecognized, `is`(false))
        testCoroutineContext.advanceTimeBy(300)
        assertThat(tapDownRecognized, `is`(true))
        assertThat(longPressRecognized, `is`(false))
        testCoroutineContext.advanceTimeBy(700)
        assertThat(tapDownRecognized, `is`(true))
        assertThat(longPressRecognized, `is`(true))

        tap.dispose()
        longPress.dispose()
    }

    // TODO(Migration/shepshapard): Not sure if we need to consider "microtasks"
//    @Test
//    fun `Drag start delayed by microtask`() {
//        val longPress = LongPressGestureRecognizer()
//        val drag = HorizontalDragGestureRecognizer()
//
//        var isDangerousStack = false
//
//        var dragStartRecognized = false
//        drag.onStart = { _ ->
//            assertThat(isDangerousStack, `is`(false))
//            dragStartRecognized = true
//        }
//
//        var longPressRecognized = false
//        longPress.onLongPress = {
//            assertThat(isDangerousStack, `is`(false))
//            longPressRecognized = true
//        }
//
//        drag.addPointer(down)
//        longPress.addPointer(down)
//        gestureArena.close(5)
//        assertThat(dragStartRecognized, `is`(false))
//        assertThat(longPressRecognized, `is`(false))
//        pointerRouter.route(down)
//        assertThat(dragStartRecognized, `is`(false))
//        assertThat(longPressRecognized, `is`(false))
//        FakeTime.elapse(Duration.create(milliseconds = 300))
//        assertThat(dragStartRecognized, `is`(false))
//        assertThat(longPressRecognized, `is`(false))
//        isDangerousStack = true
//        longPress.dispose()
//        isDangerousStack = false
//        assertThat(dragStartRecognized, `is`(false))
//        assertThat(longPressRecognized, `is`(false))
//        //tester.async.flushMicrotasks()
//        assertThat(dragStartRecognized, `is`(true))
//        assertThat(longPressRecognized, `is`(false))
//        drag.dispose()
//    }
}
