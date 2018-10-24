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

package androidx.ui.gestures.multidrag_test

import androidx.ui.gestures.drag.Drag

// TODO(Migration/shepshapard): Need a way to fake time

class MultiDragTest {

//    @Test
//    fun `MultiDrag, moving before delay rejects`() {
//        ensureGestureBinding()
//        val drag = DelayedMultiDragGestureRecognizer()
//
//        var didStartDrag = false
//        drag.onStart = { _ ->
//            didStartDrag = true
//            TestDrag()
//        }
//
//        val pointer = TestPointer(5)
//        val down = pointer.down(Offset(10.0, 10.0))
//        drag.addPointer(down)
//        gestureArena.close(5)
//        assertThat(didStartDrag, `is`(false))
//        //tester.async.flushMicrotasks()
//        assertThat(didStartDrag, `is`(false))
//        pointerRouter.route(
//            pointer.move(
//                Offset(
//                    20.0,
//                    60.0
//                )
//            )
//        ) // move more than touch slop before delay expires
//        assertThat(didStartDrag, `is`(false))
//        tester.async.elapse(kLongPressTimeout * 2) // expire delay
//        assertThat(didStartDrag, `is`(false))
//        pointerRouter.route(pointer.move(Offset(30.0, 120.0))) // move some more after delay expires
//        assertThat(didStartDrag, `is`(false))
//        drag.dispose()
//    }
//
//    fun `MultiDrag, delay triggers`() {
//        val drag = DelayedMultiDragGestureRecognizer()
//
//        var didStartDrag = false
//        drag.onStart = { _ ->
//            didStartDrag = true
//            TestDrag()
//        }
//
//        val pointer = TestPointer(5)
//        val down = pointer.down(Offset(10.0, 10.0))
//        drag.addPointer(down)
//        gestureArena.close(5)
//        assertThat(didStartDrag, `is`(false))
//        tester.async.flushMicrotasks()
//        assertThat(didStartDrag, `is`(false))
//        pointerRouter.route(
//            pointer.move(
//                Offset(
//                    20.0,
//                    20.0
//                )
//            )
//        ) // move less than touch slop before delay expires
//        assertThat(didStartDrag, `is`(false))
//        tester.async.elapse(kLongPressTimeout * 2) // expire delay
//        assertThat(didStartDrag, `is`(true))
//        pointerRouter.route(
//            pointer.move(
//                Offset(
//                    30.0,
//                    70.0
//                )
//            )
//        ) // move more than touch slop after delay expires
//        assertThat(didStartDrag, `is`(true))
//        drag.dispose()
//    }

    private inner class TestDrag : Drag()
}
