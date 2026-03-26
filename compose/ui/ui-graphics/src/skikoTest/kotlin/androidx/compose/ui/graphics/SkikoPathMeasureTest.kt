/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.util.lerp
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SkikoPathMeasureTest {

    @Test
    fun getSegment_reusedDestinationUpdatesRetainedSkiaPath() {
        val startX = 300f
        val startY = 450f
        val endX = 800f
        val endY = 900f
        val source = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val measure = PathMeasure().apply { setPath(source, false) }
        val destination = Path()
        // Simulate that render/native code retaining the underlying SkPath while the
        // Compose Path object itself is reused across frames.
        val retainedDestination = destination.asSkiaPath()
        val startFraction = 0.5f
        val startDistance = measure.length * startFraction

        for (stopFraction in floatArrayOf(0.6f, 0.75f)) {
            destination.reset()

            assertTrue(
                measure.getSegment(
                    startDistance = startDistance,
                    stopDistance = measure.length * stopFraction,
                    destination = destination,
                    startWithMoveTo = true
                )
            )

            val expected = Path().apply {
                moveTo(
                    lerp(startX, endX, startFraction),
                    lerp(startY, endY, startFraction),
                )
                lineTo(
                    lerp(startX, endX, stopFraction),
                    lerp(startY, endY, stopFraction),
                )
            }

            // Verify Compose state
            assertEquals(expected.toSvg(), destination.toSvg())

            // Verify Skia state
            assertContentEquals(expected.asSkiaPath().serializeToBytes(), retainedDestination.serializeToBytes())
        }
    }
}
