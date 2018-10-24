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

package androidx.ui.gestures.scale_test

import androidx.ui.engine.geometry.Offset
import androidx.ui.flutter_test.TestPointer
import androidx.ui.gestures.gesture_tester.ensureGestureBinding
import androidx.ui.gestures.gesture_tester.gestureArena
import androidx.ui.gestures.gesture_tester.pointerRouter
import androidx.ui.gestures.scale.ScaleGestureRecognizer
import androidx.ui.gestures.tap.TapGestureRecognizer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScaleTest {

    @Test
    fun `Should recognize scale gestures`() {
        ensureGestureBinding()
        val scale = ScaleGestureRecognizer()
        val tap = TapGestureRecognizer()

        var didStartScale = false
        var updatedFocalPoint: Offset? = null
        scale.onStart = { details ->
            didStartScale = true
            updatedFocalPoint = details.focalPoint
        }

        var updatedScale: Double? = null
        scale.onUpdate = { details ->
            updatedScale = details.scale
            updatedFocalPoint = details.focalPoint
        }

        var didEndScale = false
        scale.onEnd = { _ ->
            didEndScale = true
        }

        var didTap = false
        tap.onTap = {
            didTap = true
        }

        val pointer1 = TestPointer(1)

        val down = pointer1.down(Offset(0.0, 0.0))
        scale.addPointer(down)
        tap.addPointer(down)

        gestureArena.close(1)
        assertThat(didStartScale, `is`(false))
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(didEndScale, `is`(false))
        assertThat(didTap, `is`(false))

        // One-finger panning
        pointerRouter.route(down)
        assertThat(didStartScale, `is`(false))
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(didEndScale, `is`(false))
        assertThat(didTap, `is`(false))

        pointerRouter.route(pointer1.move(Offset(20.0, 30.0)))
        assertThat(didStartScale, `is`(true))
        didStartScale = false
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(20.0, 30.0))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(1.0))
        updatedScale = null
        assertThat(didEndScale, `is`(false))
        assertThat(didTap, `is`(false))

        // Two-finger scaling
        val pointer2 = TestPointer(2)
        val down2 = pointer2.down(Offset(10.0, 20.0))
        scale.addPointer(down2)
        tap.addPointer(down2)
        gestureArena.close(2)
        pointerRouter.route(down2)

        assertThat(didEndScale, `is`(true))
        didEndScale = false
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(didStartScale, `is`(false))

        // Zoom in
        pointerRouter.route(pointer2.move(Offset(0.0, 10.0)))
        assertThat(didStartScale, `is`(true))
        didStartScale = false
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(10.0, 20.0))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(equalTo(2.0)))
        updatedScale = null
        assertThat(didEndScale, `is`(false))
        assertThat(didTap, `is`(false))

        // Zoom out
        pointerRouter.route(pointer2.move(Offset(15.0, 25.0)))
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(17.5, 27.5))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(equalTo(0.5)))
        updatedScale = null
        assertThat(didTap, `is`(false))

        // Three-finger scaling
        val pointer3 = TestPointer(3)
        val down3 = pointer3.down(Offset(25.0, 35.0))
        scale.addPointer(down3)
        tap.addPointer(down3)
        gestureArena.close(3)
        pointerRouter.route(down3)

        assertThat(didEndScale, `is`(true))
        didEndScale = false
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(didStartScale, `is`(false))

        // Zoom in
        pointerRouter.route(pointer3.move(Offset(55.0, 65.0)))
        assertThat(didStartScale, `is`(true))
        didStartScale = false
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(30.0, 40.0))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(equalTo(5.0)))
        updatedScale = null
        assertThat(didEndScale, `is`(false))
        assertThat(didTap, `is`(false))

        // Return to original positions but with different fingers
        pointerRouter.route(pointer1.move(Offset(25.0, 35.0)))
        pointerRouter.route(pointer2.move(Offset(20.0, 30.0)))
        pointerRouter.route(pointer3.move(Offset(15.0, 25.0)))
        assertThat(didStartScale, `is`(false))
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(20.0, 30.0))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(equalTo(1.0)))
        updatedScale = null
        assertThat(didEndScale, `is`(false))
        assertThat(didTap, `is`(false))

        pointerRouter.route(pointer1.up())
        assertThat(didStartScale, `is`(false))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(didEndScale, `is`(true))
        didEndScale = false
        assertThat(didTap, `is`(false))

        // Continue scaling with two fingers
        pointerRouter.route(pointer3.move(Offset(10.0, 20.0)))
        assertThat(didStartScale, `is`(true))
        didStartScale = false
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(15.0, 25.0))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(equalTo(2.0)))
        updatedScale = null

        pointerRouter.route(pointer2.up())
        assertThat(didStartScale, `is`(false))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(didEndScale, `is`(true))
        didEndScale = false
        assertThat(didTap, `is`(false))

        // Continue panning with one finger
        pointerRouter.route(pointer3.move(Offset(0.0, 0.0)))
        assertThat(didStartScale, `is`(true))
        didStartScale = false
        assertThat(updatedFocalPoint, `is`(equalTo(Offset(0.0, 0.0))))
        updatedFocalPoint = null
        assertThat(updatedScale, `is`(equalTo(1.0)))
        updatedScale = null

        // We are done
        pointerRouter.route(pointer3.up())
        assertThat(didStartScale, `is`(false))
        assertThat(updatedFocalPoint, `is`(nullValue()))
        assertThat(updatedScale, `is`(nullValue()))
        assertThat(didEndScale, `is`(true))
        didEndScale = false
        assertThat(didTap, `is`(false))

        scale.dispose()
        tap.dispose()
    }

    // TODO(Migration/shepshapard): Need HorizontalDragGestureRecognizer.
//    @Test
//    fun `Scale gesture competes with drag`() {
//        val scale = ScaleGestureRecognizer()
//        val drag = HorizontalDragGestureRecognizer()
//
//        val log: MutableList<String> = mutableListOf()
//
//        scale.onStart = { _ -> log.add("scale-start") }
//        scale.onUpdate = { _ -> log.add("scale-update") }
//        scale.onEnd = { _ -> log.add("scale-end") }
//
//        drag.onStart = { _ -> log.add("drag-start") }
//        drag.onEnd = { _ -> log.add("drag-end") }
//
//        val pointer1 = TestPointer(1)
//
//        val down = pointer1.down(Offset(10.0, 10.0))
//        scale.addPointer(down)
//        drag.addPointer(down)
//
//        gestureArena.close(1)
//        assertThat(log, `is`(equalTo(mutableListOf())))
//
//        // Vertical moves are scales.
//        pointerRouter.route(down)
//        assertThat(log, `is`(equalTo(mutableListOf())))
//
//        // scale will win if focal point delta exceeds 18.0*2
//
//        pointerRouter.route(pointer1.move(Offset(10.0, 50.0))) // delta of 40.0 exceeds 18.0*2
//        assertThat(log, `is`(equalTo(mutableListOf("scale-start", "scale-update"))))
//        log.clear()
//
//        val pointer2 = TestPointer (2)
//        val down2 = pointer2.down(Offset(10.0, 20.0))
//        scale.addPointer(down2)
//        drag.addPointer(down2)
//
//        gestureArena.close(2)
//        assertThat(log, `is`(equalTo(mutableListOf())))
//
//        // Second pointer joins scale even though it moves horizontally.
//        pointerRouter.route(down2)
//        assertThat(log, `is`(equalTo(mutableListOf("scale-end"))))
//        log.clear()
//
//        pointerRouter.route(pointer2.move(Offset(30.0, 20.0)))
//        assertThat(log, `is`(equalTo(mutableListOf("scale-start", "scale-update"))))
//        log.clear()
//
//        pointerRouter.route(pointer1.up())
//        assertThat(log, `is`(equalTo(mutableListOf("scale-end"))))
//        log.clear()
//
//        pointerRouter.route(pointer2.up())
//        assertThat(log, `is`(equalTo(mutableListOf())))
//        log.clear()
//
//        // Horizontal moves are either drags or scales, depending on which wins first.
//        // TODO(ianh): https://github.com/flutter/flutter/issues/11384
//        // In this case, we move fast, so that the scale wins. If we moved slowly,
//        // the horizontal drag would win, since it was added first.
//        val pointer3 = TestPointer (3)
//        val down3 = pointer3.down(Offset(30.0, 30.0))
//        scale.addPointer(down3)
//        drag.addPointer(down3)
//        gestureArena.close(3)
//        pointerRouter.route(down3)
//
//        assertThat(log, `is`(equalTo(mutableListOf())))
//
//        pointerRouter.route(pointer3.move(Offset(100.0, 30.0)))
//        assertThat(log, `is`(equalTo(mutableListOf("scale-start", "scale-update"))))
//        log.clear()
//
//        pointerRouter.route(pointer3.up())
//        assertThat(log, `is`(equalTo(mutableListOf("scale-end"))))
//        log.clear()
//
//        scale.dispose()
//        drag.dispose()
//    }
}