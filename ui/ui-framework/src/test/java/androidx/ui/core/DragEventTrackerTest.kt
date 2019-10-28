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

package androidx.ui.core

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DragEventTrackerTest {
    @Test
    fun test_not_moving() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10.px, 20.px))
        assertEquals(PxPosition(10.px, 20.px), tracker.getPosition())
    }

    @Test
    fun test_drag_one_distance() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10.px, 20.px))
        tracker.onDrag(PxPosition(30.px, 40.px))
        assertEquals(PxPosition(40.px, 60.px), tracker.getPosition())
    }

    @Test
    fun test_drag_two_distance() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10.px, 20.px))
        tracker.onDrag(PxPosition(30.px, 40.px))
        tracker.onDrag(PxPosition(50.px, 60.px))
        assertEquals(PxPosition(60.px, 80.px), tracker.getPosition())
    }

    @Test
    fun test_drag_twice() {
        val tracker = DragEventTracker()

        tracker.init(PxPosition(10.px, 20.px))
        tracker.onDrag(PxPosition(30.px, 40.px))

        tracker.init(PxPosition(50.px, 60.px))
        tracker.onDrag(PxPosition(70.px, 80.px))
        assertEquals(PxPosition(120.px, 140.px), tracker.getPosition())
    }
}