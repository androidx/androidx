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

package androidx.graphics.shapes

import kotlin.test.Test
import kotlin.test.assertTrue

open class MorphTest {

    val RADIUS = 50
    val SCALE = RADIUS.toFloat()

    val poly1 = RoundedPolygon(3, centerX = .5f, centerY = .5f)
    val poly2 = RoundedPolygon(4, centerX = .5f, centerY = .5f)
    val morph11 = Morph(poly1, poly1)
    val morph12 = Morph(poly1, poly2)

    /**
     * Simple test to verify that a Morph with the same start and end shape has curves equivalent to
     * those in that shape.
     */
    @Test
    fun cubicsTest() {
        val p1Cubics = poly1.cubics
        val cubics11 = morph11.asCubics(0f)
        assertTrue(cubics11.size > 0)
        // The structure of a morph and its component shapes may not match exactly, because morph
        // calculations may optimize some of the zero-length curves out. But in general, every
        // curve in the morph *should* exist somewhere in the shape it is based on, so we
        // do an exhaustive search for such existence. Note that this assertion only works because
        // we constructed the Morph from/to the same shape. A Morph between different shapes
        // may not have the curves replicated exactly.
        for (morphCubic in cubics11) {
            var matched = false
            for (p1Cubic in p1Cubics) {
                if (cubicsEqualish(morphCubic, p1Cubic)) {
                    matched = true
                    continue
                }
            }
            assertTrue(matched)
        }
    }
}
