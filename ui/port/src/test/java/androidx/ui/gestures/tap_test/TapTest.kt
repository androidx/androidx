/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.ui.gestures.tap_test

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureArenaMember
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.gesture_tester.ensureGestureBinding
import androidx.ui.gestures.gesture_tester.gestureArena
import androidx.ui.gestures.gesture_tester.pointerRouter
import androidx.ui.gestures.tap.TapGestureRecognizer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test

class TapTest {

    internal inner class TestGestureArenaMember : GestureArenaMember {
        override fun acceptGesture(pointer: Int) {}
        override fun rejectGesture(pointer: Int) {}
    }

    @Before
    fun setup() {
        ensureGestureBinding()
    }

    @Test
    fun `Should recognize tap`() {
        val tap = TapGestureRecognizer()

        var tapRecognized = false
        tap.onTap = {
            tapRecognized = true
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(tapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(tapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(tapRecognized, `is`(true))

        // TODO(Migration/shepshapard): The below test isn't useful because there is no way for
        // tapRecognized to become false once it has become true.
        gestureArena.sweep(1)
        assertThat(tapRecognized, `is`(true))

        tap.dispose()
    }

    @Test
    fun `No duplicate tap events`() {
        val tap = TapGestureRecognizer()

        var tapsRecognized = 0
        tap.onTap = {
            tapsRecognized++
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(tapsRecognized, `is`(0))
        pointerRouter.route(down1)
        assertThat(tapsRecognized, `is`(0))

        pointerRouter.route(up1)
        assertThat(tapsRecognized, `is`(1))
        gestureArena.sweep(1)
        assertThat(tapsRecognized, `is`(1))

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(tapsRecognized, `is`(1))
        pointerRouter.route(down1)
        assertThat(tapsRecognized, `is`(1))

        pointerRouter.route(up1)
        assertThat(tapsRecognized, `is`(2))
        gestureArena.sweep(1)
        assertThat(tapsRecognized, `is`(2))

        tap.dispose()
    }

    @Test
    fun `Should not recognize two overlapping taps`() {
        val tap = TapGestureRecognizer()

        var tapsRecognized = 0
        tap.onTap = {
            tapsRecognized++
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(tapsRecognized, `is`(0))
        pointerRouter.route(down1)
        assertThat(tapsRecognized, `is`(0))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(tapsRecognized, `is`(0))
        pointerRouter.route(down1)
        assertThat(tapsRecognized, `is`(0))

        pointerRouter.route(up1)
        assertThat(tapsRecognized, `is`(1))
        gestureArena.sweep(1)
        assertThat(tapsRecognized, `is`(1))

        pointerRouter.route(up2)
        assertThat(tapsRecognized, `is`(1))
        gestureArena.sweep(2)
        assertThat(tapsRecognized, `is`(1))

        tap.dispose()
    }

    @Test
    fun `Distance cancels tap`() {
        val tap = TapGestureRecognizer()

        var tapRecognized = false
        tap.onTap = {
            tapRecognized = true
        }
        var tapCanceled = false
        tap.onTapCancel = {
            tapCanceled = true
        }

        tap.addPointer(down3)
        gestureArena.close(3)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(false))
        pointerRouter.route(down3)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(false))

        pointerRouter.route(move3)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(true))
        pointerRouter.route(up3)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(true))
        gestureArena.sweep(3)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(true))

        tap.dispose()
    }

    @Test
    fun `Short distance does not cancel tap`() {
        val tap = TapGestureRecognizer()

        var tapRecognized = false
        tap.onTap = {
            tapRecognized = true
        }
        var tapCanceled = false
        tap.onTapCancel = {
            tapCanceled = true
        }

        tap.addPointer(down4)
        gestureArena.close(4)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(false))
        pointerRouter.route(down4)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(false))

        pointerRouter.route(move4)
        assertThat(tapRecognized, `is`(false))
        assertThat(tapCanceled, `is`(false))
        pointerRouter.route(up4)
        assertThat(tapRecognized, `is`(true))
        assertThat(tapCanceled, `is`(false))
        gestureArena.sweep(4)
        assertThat(tapRecognized, `is`(true))
        assertThat(tapCanceled, `is`(false))

        tap.dispose()
    }

    // TODO(Migration/shepshapard): Need to be able to fake the timer in the TapGestureRecognizer in
    // order to test this.  However, this is a test that is confirming something does not happen,
    // which is a pretty unreasonable test when it comes down to it.
    /*
      fun `Timeout does not cancel tap`() {
        val tap = TapGestureRecognizer()

        var tapRecognized = false
        tap.onTap = {
          tapRecognized = true
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(tapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(tapRecognized, `is`(false))

        tester.async.elapse(const Duration(milliseconds: 500))
        assertThat(tapRecognized, `is`(false))
        pointerRouter.route(up1)
        assertThat(tapRecognized, `is`(true))
        gestureArena.sweep(1)
        assertThat(tapRecognized, `is`(true))

        tap.dispose()
      })

    */

    @Test
    fun `Should yield to other arena members`() {
        val tap = TapGestureRecognizer()

        var tapRecognized = false
        tap.onTap = {
            tapRecognized = true
        }

        tap.addPointer(down1)
        val member = TestGestureArenaMember()
        val entry = gestureArena.add(1, member)
        gestureArena.hold(1)
        gestureArena.close(1)
        assertThat(tapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(tapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(tapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(tapRecognized, `is`(false))

        entry.resolve(GestureDisposition.accepted)
        assertThat(tapRecognized, `is`(false))

        tap.dispose()
    }

    @Test
    fun `Should trigger on release of held arena`() {
        val tap = TapGestureRecognizer()

        var tapRecognized = false
        tap.onTap = {
            tapRecognized = true
        }

        tap.addPointer(down1)
        val entry = gestureArena.add(1, TestGestureArenaMember())
        gestureArena.hold(1)
        gestureArena.close(1)
        assertThat(tapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(tapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(tapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(tapRecognized, `is`(false))

        entry.resolve(GestureDisposition.rejected)
        assertThat(tapRecognized, `is`(true))

        tap.dispose()
    }

    @Test
    fun `Verify correct order of events`() {
        val tapA = TapGestureRecognizer()
        val tapB = TapGestureRecognizer()

        val log: MutableList<String> = mutableListOf()
        tapA.onTapDown = { log.add("tapA onTapDown") }
        tapA.onTapUp = { log.add("tapA onTapUp") }
        tapA.onTap = { log.add("tapA onTap") }
        tapA.onTapCancel = { log.add("tapA onTapCancel") }
        tapB.onTapDown = { log.add("tapB onTapDown") }
        tapB.onTapUp = { log.add("tapB onTapUp") }
        tapB.onTap = { log.add("tapB onTap") }
        tapB.onTapCancel = { log.add("tapB onTapCancel") }

        log.add("start")
        tapA.addPointer(down1)
        log.add("added 1 to A")
        tapB.addPointer(down1)
        log.add("added 1 to B")
        gestureArena.close(1)
        log.add("closed 1")
        pointerRouter.route(down1)
        log.add("routed 1 down")
        pointerRouter.route(up1)
        log.add("routed 1 up")
        gestureArena.sweep(1)
        log.add("swept 1")
        tapA.addPointer(down2)
        log.add("down 2 to A")
        tapB.addPointer(down2)
        log.add("down 2 to B")
        gestureArena.close(2)
        log.add("closed 2")
        pointerRouter.route(down2)
        log.add("routed 2 down")
        pointerRouter.route(up2)
        log.add("routed 2 up")
        gestureArena.sweep(2)
        log.add("swept 2")
        tapA.dispose()
        log.add("disposed A")
        tapB.dispose()
        log.add("disposed B")

        assertThat(
            log, `is`(
                equalTo(
                    mutableListOf(
                        "start",
                        "added 1 to A",
                        "added 1 to B",
                        "closed 1",
                        "routed 1 down",
                        "routed 1 up",
                        "tapA onTapDown",
                        "tapA onTapUp",
                        "tapA onTap",
                        "tapB onTapCancel",
                        "swept 1",
                        "down 2 to A",
                        "down 2 to B",
                        "closed 2",
                        "routed 2 down",
                        "routed 2 up",
                        "tapA onTapDown",
                        "tapA onTapUp",
                        "tapA onTap",
                        "tapB onTapCancel",
                        "swept 2",
                        "disposed A",
                        "disposed B"
                    )
                )
            )
        )
    }

    companion object {
        // Down/up pair 1: normal tap sequence
        val down1 = PointerDownEvent(
            pointer = 1,
            position = Offset(10.0, 10.0)
        )

        val up1 = PointerUpEvent(
            pointer = 1,
            position = Offset(11.0, 9.0)
        )

        // Down/up pair 2: normal tap sequence far away from pair 1
        val down2 = PointerDownEvent(
            pointer = 2,
            position = Offset(30.0, 30.0)
        )

        val up2 = PointerUpEvent(
            pointer = 2,
            position = Offset(31.0, 29.0)
        )

        // Down/move/up sequence 3: intervening motion, more than kTouchSlop. (~21px)
        val down3 = PointerDownEvent(
            pointer = 3,
            position = Offset(10.0, 10.0)
        )

        val move3 = PointerMoveEvent(
            pointer = 3,
            position = Offset(25.0, 25.0)
        )

        val up3 = PointerUpEvent(
            pointer = 3,
            position = Offset(25.0, 25.0)
        )

        // Down/move/up sequence 4: intervening motion, less than kTouchSlop. (~17px)
        val down4 = PointerDownEvent(
            pointer = 4,
            position = Offset(10.0, 10.0)
        )

        val move4 = PointerMoveEvent(
            pointer = 4,
            position = Offset(22.0, 22.0)
        )

        val up4 = PointerUpEvent(
            pointer = 4,
            position = Offset(22.0, 22.0)
        )
    }
}