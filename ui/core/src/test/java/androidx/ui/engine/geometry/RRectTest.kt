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
        val rrect = RRect(
                Rect.fromLTRB(1.0f, 1.0f, 2.0f, 2.0f),
                topLeft = Radius.circular(0.5f),
                topRight = Radius.circular(0.25f),
                bottomRight = Radius.elliptical(0.25f, 0.75f),
                bottomLeft = Radius.zero
        )

        assertFalse(rrect.contains(Offset(1.0f, 1.0f)))
        assertFalse(rrect.contains(Offset(1.1f, 1.1f)))
        assertTrue(rrect.contains(Offset(1.15f, 1.15f)))
        assertFalse(rrect.contains(Offset(2.0f, 1.0f)))
        assertFalse(rrect.contains(Offset(1.93f, 1.07f)))
        assertFalse(rrect.contains(Offset(1.97f, 1.7f)))
        assertTrue(rrect.contains(Offset(1.7f, 1.97f)))
        assertTrue(rrect.contains(Offset(1.0f, 1.99f)))
    }

    @Test
    fun `RRect_contains() large radii`() {
        val rrect = RRect(
                Rect.fromLTRB(1.0f, 1.0f, 2.0f, 2.0f),
                topLeft = Radius.circular(5000.0f),
                topRight = Radius.circular(2500.0f),
                bottomRight = Radius.elliptical(2500.0f, 7500.0f),
                bottomLeft = Radius.zero
        )

        assertFalse(rrect.contains(Offset(1.0f, 1.0f)))
        assertFalse(rrect.contains(Offset(1.1f, 1.1f)))
        assertTrue(rrect.contains(Offset(1.15f, 1.15f)))
        assertFalse(rrect.contains(Offset(2.0f, 1.0f)))
        assertFalse(rrect.contains(Offset(1.93f, 1.07f)))
        assertFalse(rrect.contains(Offset(1.97f, 1.7f)))
        assertTrue(rrect.contains(Offset(1.7f, 1.97f)))
        assertTrue(rrect.contains(Offset(1.0f, 1.99f)))
    }
}
