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

package androidx.ui.text

import androidx.ui.unit.PxPosition
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DragEventTrackerTest {
    @Test
    fun test_not_moving() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10f, 20f))
        assertEquals(PxPosition(10f, 20f), tracker.getPosition())
    }

    @Test
    fun test_drag_one_distance() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10f, 20f))
        tracker.onDrag(PxPosition(30f, 40f))
        assertEquals(PxPosition(40f, 60f), tracker.getPosition())
    }

    @Test
    fun test_drag_two_distance() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10f, 20f))
        tracker.onDrag(PxPosition(30f, 40f))
        tracker.onDrag(PxPosition(50f, 60f))
        assertEquals(PxPosition(60f, 80f), tracker.getPosition())
    }

    @Test
    fun test_drag_twice() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10f, 20f))
        tracker.onDrag(PxPosition(30f, 40f))

        tracker.init(PxPosition(50f, 60f))
        tracker.onDrag(PxPosition(70f, 80f))
        assertEquals(PxPosition(120f, 140f), tracker.getPosition())
    }
}