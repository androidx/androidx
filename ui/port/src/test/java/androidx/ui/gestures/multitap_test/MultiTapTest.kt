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

package androidx.ui.gestures.multitap_test

import androidx.ui.engine.geometry.Offset
import androidx.ui.flutter_test.TestPointer
import androidx.ui.gestures.gesture_tester.ensureGestureBinding
import androidx.ui.gestures.gesture_tester.gestureArena
import androidx.ui.gestures.gesture_tester.pointerRouter
import androidx.ui.gestures.kLongPressTimeout
import androidx.ui.gestures.multitap.MultiTapGestureRecognizer
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MultiTapTest {

    @Test
    fun `Should recognize pan`() {
        ensureGestureBinding()
        val tap = MultiTapGestureRecognizer(longTapDelay = kLongPressTimeout)

        val log: MutableList<String> = mutableListOf()

        tap.onTapDown = { pointer, _ -> log.add("tap-down $pointer") }
        tap.onTapUp = { pointer, _ -> log.add("tap-up $pointer") }
        tap.onTap = { pointer -> log.add("tap $pointer") }
        tap.onLongTapDown = { pointer, _ -> log.add("long-tap-down $pointer") }
        tap.onTapCancel = { pointer -> log.add("tap-cancel $pointer") }

        val pointer5 = TestPointer(5)
        val down5 = pointer5.down(Offset(10.0, 10.0))
        tap.addPointer(down5)
        gestureArena.close(5)
        assertThat(log, `is`(equalTo(mutableListOf("tap-down 5"))))
        log.clear()
        pointerRouter.route(down5)
        assertThat(log, `is`(equalTo(mutableListOf())))

        val pointer6 = TestPointer(6)
        val down6 = pointer6.down(Offset(15.0, 15.0))
        tap.addPointer(down6)
        gestureArena.close(6)
        assertThat(log, `is`(equalTo(mutableListOf("tap-down 6"))))
        log.clear()
        pointerRouter.route(down6)
        assertThat(log, `is`(equalTo(mutableListOf())))

        pointerRouter.route(pointer5.move(Offset(11.0, 12.0)))
        assertThat(log, `is`(equalTo(mutableListOf())))

        pointerRouter.route(pointer6.move(Offset(14.0, 13.0)))
        assertThat(log, `is`(equalTo(mutableListOf())))

        pointerRouter.route(pointer5.up())
        assertThat(log, `is`(equalTo(mutableListOf("tap-up 5", "tap 5"))))
        log.clear()

        // TODO(Migration:shepshapard): How do we fake elapsed time.
        /*tester.async.elapse(kLongPressTimeout + kPressTimeout)
        assertThat(log, `is`(equalTo(mutableListOf("long-tap-down 6"))))
        log.clear()*/

        // move more than kTouchSlop from 15.0,15.0
        pointerRouter.route(pointer6.move(Offset(40.0, 30.0)))
        assertThat(log, `is`(equalTo(mutableListOf("tap-cancel 6"))))
        log.clear()

        pointerRouter.route(pointer6.up())
        assertThat(log, `is`(equalTo(mutableListOf())))

        tap.dispose()
    }
}