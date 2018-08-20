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

package androidx.ui.engine.geometry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RRectTest {

    @Test
    fun `RRect_contains()`() {
        val rrect = RRect.fromRectAndCorners(
                Rect.fromLTRB(1.0, 1.0, 2.0, 2.0),
                topLeft = Radius.circular(0.5),
                topRight = Radius.circular(0.25),
                bottomRight = Radius.elliptical(0.25, 0.75),
                bottomLeft = Radius.zero
        )

        assertFalse(rrect.contains(Offset(1.0, 1.0)))
        assertFalse(rrect.contains(Offset(1.1, 1.1)))
        assertTrue(rrect.contains(Offset(1.15, 1.15)))
        assertFalse(rrect.contains(Offset(2.0, 1.0)))
        assertFalse(rrect.contains(Offset(1.93, 1.07)))
        assertFalse(rrect.contains(Offset(1.97, 1.7)))
        assertTrue(rrect.contains(Offset(1.7, 1.97)))
        assertTrue(rrect.contains(Offset(1.0, 1.99)))
    }

    @Test
    fun `RRect_contains() large radii`() {
        val rrect = RRect.fromRectAndCorners(
                Rect.fromLTRB(1.0, 1.0, 2.0, 2.0),
                topLeft = Radius.circular(5000.0),
                topRight = Radius.circular(2500.0),
                bottomRight = Radius.elliptical(2500.0, 7500.0),
                bottomLeft = Radius.zero
        )

        assertFalse(rrect.contains(Offset(1.0, 1.0)))
        assertFalse(rrect.contains(Offset(1.1, 1.1)))
        assertTrue(rrect.contains(Offset(1.15, 1.15)))
        assertFalse(rrect.contains(Offset(2.0, 1.0)))
        assertFalse(rrect.contains(Offset(1.93, 1.07)))
        assertFalse(rrect.contains(Offset(1.97, 1.7)))
        assertTrue(rrect.contains(Offset(1.7, 1.97)))
        assertTrue(rrect.contains(Offset(1.0, 1.99)))
    }
}