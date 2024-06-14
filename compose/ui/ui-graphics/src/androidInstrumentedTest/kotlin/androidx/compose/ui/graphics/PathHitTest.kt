/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class PathHitTest {
    @Test
    fun linesHit() {
        val path = createSvgPath(SvgShape.Lines)
        val hitTester = PathHitTester(path)
        assertTrue(Offset(400f, 370f) in hitTester)
        assertFalse(Offset(180f, 160f) in hitTester)
    }

    @Test
    fun cubicsHit() {
        val path = createSvgPath(SvgShape.Cubics)
        val hitTester = PathHitTester(path)
        assertTrue(Offset(100f, 275f) in hitTester) // cubics and lines
        assertTrue(Offset(370f, 40f) in hitTester) // on a scanline with only cubics
        assertFalse(Offset(370f, 280f) in hitTester) // cutout
        assertFalse(Offset(380f, 600f) in hitTester) // outside
    }

    @Test
    fun quadsHit() {
        val path = createSvgPath(SvgShape.Quads)
        val hitTester = PathHitTester(path)
        assertTrue(Offset(50f, 180f) in hitTester) // quads and lines
        assertTrue(Offset(225f, 15f) in hitTester) // on a scanline with only quads
        assertFalse(Offset(270f, 175f) in hitTester) // cutout
        assertFalse(Offset(275f, 425f) in hitTester) // outside
    }

    @Test
    fun fillTypesHit() {
        val nonZero = createSvgPath(SvgShape.FillTypes).apply { fillType = PathFillType.NonZero }
        val evenOdd = createSvgPath(SvgShape.FillTypes)

        val nonZeroHitTester = PathHitTester(nonZero)
        val evenOddHitTester = PathHitTester(evenOdd)
        assertTrue(Offset(350f, 350f) in nonZeroHitTester)
        assertFalse(Offset(350f, 350f) in evenOddHitTester)
    }
}
