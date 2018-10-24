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

package androidx.ui.gestures.double_tap_test

import androidx.ui.engine.geometry.Offset
import androidx.ui.gestures.arena.GestureArenaMember
import androidx.ui.gestures.arena.GestureDisposition
import androidx.ui.gestures.events.PointerDownEvent
import androidx.ui.gestures.events.PointerMoveEvent
import androidx.ui.gestures.events.PointerUpEvent
import androidx.ui.gestures.gesture_tester.ensureGestureBinding
import androidx.ui.gestures.gesture_tester.gestureArena
import androidx.ui.gestures.gesture_tester.pointerRouter
import androidx.ui.gestures.multitap.DoubleTapGestureRecognizer
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Test

class DoubleTapTest {

    @Test
    fun `Should recognize double tap`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down2)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up2)
        assertThat(doubleTapRecognized, `is`(true))
        gestureArena.sweep(2)
        assertThat(doubleTapRecognized, `is`(true))

        tap.dispose()
    }

    @Test
    fun `Inter-tap distance cancels double tap`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down3)
        gestureArena.close(3)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down3)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up3)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(3)
        assertThat(doubleTapRecognized, `is`(false))

        tap.dispose()
    }

    @Test
    fun `Intra-tap distance cancels double tap`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down4)
        gestureArena.close(4)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down4)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(move4)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(up4)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(4)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down2)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.dispose()
    }

    // TODO(Migration/shepshapard): Need a way to fake time.
//    fun `Inter-tap delay cancels double tap`()
//    {
//        val tap = DoubleTapGestureRecognizer()
//
//        var doubleTapRecognized = false
//        tap.onDoubleTap = {
//            doubleTapRecognized = true
//        }
//
//        tap.addPointer(down1)
//        gestureArena.close(1)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down1)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        pointerRouter.route(up1)
//        assertThat(doubleTapRecognized, `is`(false))
//        gestureArena.sweep(1)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        tester.async.elapse(const Duration (milliseconds: 5000))
//        tap.addPointer(down2)
//        gestureArena.close(2)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down2)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        pointerRouter.route(up2)
//        assertThat(doubleTapRecognized, `is`(false))
//        gestureArena.sweep(2)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        tap.dispose()
//    }

    // TODO(Migration/shepshapard): Need a way to fake time.
//    fun `Inter-tap delay resets double tap, allowing third tap to be a double-tap`()
//    {
//        val tap = DoubleTapGestureRecognizer()
//
//        var doubleTapRecognized = false
//        tap.onDoubleTap = {
//            doubleTapRecognized = true
//        }
//
//        tap.addPointer(down1)
//        gestureArena.close(1)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down1)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        pointerRouter.route(up1)
//        assertThat(doubleTapRecognized, `is`(false))
//        gestureArena.sweep(1)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        tester.async.elapse(const Duration (milliseconds: 5000))
//        tap.addPointer(down2)
//        gestureArena.close(2)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down2)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        pointerRouter.route(up2)
//        assertThat(doubleTapRecognized, `is`(false))
//        gestureArena.sweep(2)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        tester.async.elapse(const Duration (milliseconds: 100))
//        tap.addPointer(down5)
//        gestureArena.close(5)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down5)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        pointerRouter.route(up5)
//        assertThat(doubleTapRecognized, `is`(true))
//        gestureArena.sweep(5)
//        assertThat(doubleTapRecognized, `is`(true))
//
//        tap.dispose()
//    }

    // TODO(Migration/shepshapard): Need a way to fake time.
//    fun `Intra-tap delay does not cancel double tap`()
//    {
//        val tap = DoubleTapGestureRecognizer()
//
//        var doubleTapRecognized = false
//        tap.onDoubleTap = {
//            doubleTapRecognized = true
//        }
//
//        tap.addPointer(down1)
//        gestureArena.close(1)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down1)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        tester.async.elapse(const Duration (milliseconds: 1000))
//        pointerRouter.route(up1)
//        assertThat(doubleTapRecognized, `is`(false))
//        gestureArena.sweep(1)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        tap.addPointer(down2)
//        gestureArena.close(2)
//        assertThat(doubleTapRecognized, `is`(false))
//        pointerRouter.route(down2)
//        assertThat(doubleTapRecognized, `is`(false))
//
//        pointerRouter.route(up2)
//        assertThat(doubleTapRecognized, `is`(true))
//        gestureArena.sweep(2)
//        assertThat(doubleTapRecognized, `is`(true))
//
//        tap.dispose()
//    }

    @Test
    fun `Should not recognize two overlapping taps`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up2)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(2)
        assertThat(doubleTapRecognized, `is`(false))

        tap.dispose()
    }

    @Test
    fun `Should recognize one tap of group followed by second tap`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up2)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(2)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down1)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(true))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(true))

        tap.dispose()
    }

    @Test
    fun `Should cancel on arena reject during first tap`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        val member = TestGestureArenaMember()
        val entry = gestureArena.add(1, member)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        entry.resolve(GestureDisposition.accepted)
        assertThat(member.accepted, `is`(true))
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down2)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up2)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(2)
        assertThat(doubleTapRecognized, `is`(false))

        tap.dispose()
    }

    @Test
    fun `Should cancel on arena reject between taps`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        val member = TestGestureArenaMember()
        val entry = gestureArena.add(1, member)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        entry.resolve(GestureDisposition.accepted)
        assertThat(member.accepted, `is`(true))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down2)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up2)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(2)
        assertThat(doubleTapRecognized, `is`(false))

        tap.dispose()
    }

    @Test
    fun `Should cancel on arena reject during last tap`() {
        ensureGestureBinding()

        val tap = DoubleTapGestureRecognizer()

        var doubleTapRecognized = false
        tap.onDoubleTap = {
            doubleTapRecognized = true
        }

        tap.addPointer(down1)
        val member = TestGestureArenaMember()
        val entry = gestureArena.add(1, member)
        gestureArena.close(1)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down1)
        assertThat(doubleTapRecognized, `is`(false))

        pointerRouter.route(up1)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(1)
        assertThat(doubleTapRecognized, `is`(false))

        tap.addPointer(down2)
        gestureArena.close(2)
        assertThat(doubleTapRecognized, `is`(false))
        pointerRouter.route(down2)
        assertThat(doubleTapRecognized, `is`(false))

        entry.resolve(GestureDisposition.accepted)
        assertThat(member.accepted, `is`(true))

        pointerRouter.route(up2)
        assertThat(doubleTapRecognized, `is`(false))
        gestureArena.sweep(2)
        assertThat(doubleTapRecognized, `is`(false))

        tap.dispose()
    }

    // TODO(Migration/shepshapard): Need a way to fake time.
//    fun `Passive gesture should trigger on double tap cancel`()
//    {
//        val tap = DoubleTapGestureRecognizer()
//
//        var doubleTapRecognized = false
//        tap.onDoubleTap = {
//            doubleTapRecognized = true
//        }
//
//        new FakeAsync ().run((FakeAsync async) {
//            tap.addPointer(down1)
//            final TestGestureArenaMember member = new TestGestureArenaMember ()
//            GestureBinding.instance.gestureArena.add(1, member)
//            gestureArena.close(1)
//            assertThat(doubleTapRecognized, `is`(false))
//            pointerRouter.route(down1)
//            assertThat(doubleTapRecognized, `is`(false))
//
//            pointerRouter.route(up1)
//            assertThat(doubleTapRecognized, `is`(false))
//            gestureArena.sweep(1)
//            assertThat(doubleTapRecognized, `is`(false))
//
//            expect(member.accepted, isFalse)
//
//            async.elapse(const Duration (milliseconds: 5000))
//
//            expect(member.accepted, isTrue)
//        })
//
//        tap.dispose()
//    }

    private class TestGestureArenaMember : GestureArenaMember {
        var accepted = false
        var rejected = false

        override fun acceptGesture(pointer: Int) {
            accepted = true
        }

        override fun rejectGesture(pointer: Int) {
            rejected = true
        }
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

        // Down/up pair 2: normal tap sequence close to pair 1
        val down2 = PointerDownEvent(
            pointer = 2,
            position = Offset(12.0, 12.0)
        )

        val up2 = PointerUpEvent(
            pointer = 2,
            position = Offset(13.0, 11.0)
        )

        // Down/up pair 3: normal tap sequence far away from pair 1
        val down3 = PointerDownEvent(
            pointer = 3,
            position = Offset(130.0, 130.0)
        )

        val up3 = PointerUpEvent(
            pointer = 3,
            position = Offset(131.0, 129.0)
        )

        // Down/move/up sequence 4: intervening motion
        val down4 = PointerDownEvent(
            pointer = 4,
            position = Offset(10.0, 10.0)
        )

        val move4 = PointerMoveEvent(
            pointer = 4,
            position = Offset(25.0, 25.0)
        )

        val up4 = PointerUpEvent(
            pointer = 4,
            position = Offset(25.0, 25.0)
        )

        // Down/up pair 5: normal tap sequence identical to pair 1 with different pointer
        val down5 = PointerDownEvent(
            pointer = 5,
            position = Offset(10.0, 10.0)
        )

        val up5 = PointerUpEvent(
            pointer = 5,
            position = Offset(11.0, 9.0)
        )
    }
}
