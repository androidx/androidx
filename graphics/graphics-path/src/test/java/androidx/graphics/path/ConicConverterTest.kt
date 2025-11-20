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
package androidx.graphics.path

import com.google.common.truth.Truth.assertThat
import kotlin.math.sqrt
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConicConverterTest {

    private val converter = ConicConverter()

    @Test
    fun initialState() {
        assertThat(converter.quadraticCount).isEqualTo(0)
        assertThat(converter.currentQuadratic).isEqualTo(0)
    }

    @Test
    fun convertAndIterate() {
        val conicPoints = floatArrayOf(0f, 0f, 50f, 100f, 100f, 0f)
        val weight = 0.707f
        val tolerance = 0.1f

        converter.convert(conicPoints, weight, tolerance)

        assertThat(converter.quadraticCount).isGreaterThan(0)
        assertThat(converter.currentQuadratic).isEqualTo(0)

        val quadraticPoints = FloatArray(6)
        converter.nextQuadratic(quadraticPoints)
        var quadraticCount = 1
        // We can't easily verify the exact points here without reimplementing the
        // conversion logic, but we can check that the start and end points of the
        // sequence of quadratics match the original conic.
        assertThat(quadraticPoints[0]).isEqualTo(0f)
        assertThat(quadraticPoints[1]).isEqualTo(0f)

        while (converter.nextQuadratic(quadraticPoints)) {
            quadraticCount++
        }

        assertThat(quadraticCount).isEqualTo(converter.quadraticCount)
        // Check the last point of the last quadratic
        assertThat(quadraticPoints[4]).isEqualTo(100f)
        assertThat(quadraticPoints[5]).isEqualTo(0f)

        // No more quadratics
        assertThat(converter.nextQuadratic(quadraticPoints)).isFalse()
    }

    @Test
    fun bufferResizing() {
        val conicPoints = floatArrayOf(0f, 0f, 100f, 200f, 200f, 0f)
        val weight = 0.707f
        // A very small tolerance will force many subdivisions, exceeding the default buffer size
        val tolerance = 0.0001f

        converter.convert(conicPoints, weight, tolerance)

        // The number of subdivisions is capped, so a small tolerance will result in
        // the maximum number of quadratics.
        assertThat(converter.quadraticCount).isEqualTo(32)
        assertThat(converter.currentQuadratic).isEqualTo(0)

        val quadraticPoints = FloatArray(6)
        var quadraticCount = 0
        while (converter.nextQuadratic(quadraticPoints)) {
            quadraticCount++
        }
        assertThat(quadraticCount).isEqualTo(converter.quadraticCount)
    }

    @Test
    fun conicIsQuadratic() {
        val conicPoints = floatArrayOf(0f, 0f, 50f, 100f, 100f, 0f)
        val weight = 0.5f // This makes the conic a quadratic
        // In order to have a smaller tolerance (e.g. 1f), Conic.computeQuadraticCount should be
        // changed to return 0 if weight is approxEquals 0.5f.
        val tolerance = 100.0f

        converter.convert(conicPoints, weight, tolerance)

        assertThat(converter.quadraticCount).isEqualTo(1)
        assertThat(converter.currentQuadratic).isEqualTo(0)

        val quadraticPoints = FloatArray(6)
        assertThat(converter.nextQuadratic(quadraticPoints)).isTrue()

        // The resulting quadratic should be very close to the original conic points
        // (with the control point adjusted for the weight).
        assertThat(quadraticPoints[0]).isWithin(POINT_VALUE_TOLERANCE).of(0f)
        assertThat(quadraticPoints[1]).isWithin(POINT_VALUE_TOLERANCE).of(0f)
        assertThat(quadraticPoints[2]).isWithin(POINT_VALUE_TOLERANCE).of(50f)
        assertThat(quadraticPoints[3]).isWithin(POINT_VALUE_TOLERANCE).of(100f)
        assertThat(quadraticPoints[4]).isWithin(POINT_VALUE_TOLERANCE).of(100f)
        assertThat(quadraticPoints[5]).isWithin(POINT_VALUE_TOLERANCE).of(0f)

        assertThat(converter.nextQuadratic(quadraticPoints)).isFalse()
    }

    @Test
    fun conicIsLine() {
        // A conic where the control point is on the line between the end points
        val conicPoints = floatArrayOf(0f, 0f, 50f, 50f, 100f, 100f)
        val weight = 0.8f
        val tolerance = 0.1f

        converter.convert(conicPoints, weight, tolerance)

        // This should be treated as a single line, which is one quadratic
        assertThat(converter.quadraticCount).isEqualTo(1)

        val quadraticPoints = FloatArray(6)
        assertThat(converter.nextQuadratic(quadraticPoints)).isTrue()

        // The control point of the quadratic should also be on the line
        assertThat(quadraticPoints[2]).isWithin(POINT_VALUE_TOLERANCE).of(50f)
        assertThat(quadraticPoints[3]).isWithin(POINT_VALUE_TOLERANCE).of(50f)

        assertThat(converter.nextQuadratic(quadraticPoints)).isFalse()
    }

    @Test
    fun parabola() {
        val conicPoints = floatArrayOf(0f, 0f, 50f, 100f, 100f, 0f)
        val weight = 1.0f // Parabola
        val tolerance = 0.1f

        converter.convert(conicPoints, weight, tolerance)
        assertThat(converter.quadraticCount).isEqualTo(1)

        val quadraticPoints = FloatArray(6)
        var quadraticCount = 0
        while (converter.nextQuadratic(quadraticPoints)) {
            quadraticCount++
        }
        assertThat(quadraticCount).isEqualTo(converter.quadraticCount)
    }

    @Test
    fun hyperbola() {
        val conicPoints = floatArrayOf(0f, 0f, 50f, 100f, 100f, 0f)
        val weight = 1.5f // Hyperbola
        val tolerance = 0.1f

        converter.convert(conicPoints, weight, tolerance)
        assertThat(converter.quadraticCount).isGreaterThan(1)

        val quadraticPoints = FloatArray(6)
        var quadraticCount = 0
        while (converter.nextQuadratic(quadraticPoints)) {
            quadraticCount++
        }
        assertThat(quadraticCount).isEqualTo(converter.quadraticCount)
    }

    @Test
    fun circleArc() {
        // 90-degree arc of a circle centered at (50, 50) with radius 50
        val conicPoints = floatArrayOf(50f, 0f, 100f, 50f, 50f, 100f)
        val weight = sqrt(2.0f) / 2.0f // Weight for a 90-degree circular arc
        val tolerance = 1.0f

        converter.convert(conicPoints, weight, tolerance)
        assertThat(converter.quadraticCount).isGreaterThan(0)

        val quadraticPoints = FloatArray(6)
        var quadraticCount = 0
        var firstPoint = true
        while (converter.nextQuadratic(quadraticPoints)) {
            if (firstPoint) {
                assertThat(quadraticPoints[0]).isWithin(POINT_VALUE_TOLERANCE).of(50f)
                assertThat(quadraticPoints[1]).isWithin(POINT_VALUE_TOLERANCE).of(0f)
                firstPoint = false
            }
            quadraticCount++
        }
        assertThat(quadraticCount).isEqualTo(converter.quadraticCount)
        // Check the last point of the last quadratic
        assertThat(quadraticPoints[4]).isWithin(POINT_VALUE_TOLERANCE).of(50f)
        assertThat(quadraticPoints[5]).isWithin(POINT_VALUE_TOLERANCE).of(100f)
    }

    @Test
    fun convert_withOffset() {
        val cleanConicPoints = floatArrayOf(0f, 0f, 50f, 100f, 100f, 0f)
        val conicPointsWithJunk = floatArrayOf(-1f, -1f) + cleanConicPoints + floatArrayOf(-1f, -1f)
        val weight = 0.707f
        val tolerance = 0.1f
        val offset = 2

        // First, convert with offset
        converter.convert(conicPointsWithJunk, weight, tolerance, offset)
        val offsetQuadratics = mutableListOf<FloatArray>()
        val tempQuadratic = FloatArray(6)
        while (converter.nextQuadratic(tempQuadratic)) {
            offsetQuadratics.add(tempQuadratic.clone())
        }

        // Second, convert the clean data
        converter.convert(cleanConicPoints, weight, tolerance)
        val cleanQuadratics = mutableListOf<FloatArray>()
        while (converter.nextQuadratic(tempQuadratic)) {
            cleanQuadratics.add(tempQuadratic.clone())
        }

        // Compare the results
        assertThat(offsetQuadratics.size).isEqualTo(cleanQuadratics.size)
        assertThat(offsetQuadratics).isNotEmpty()

        for (i in offsetQuadratics.indices) {
            val offsetResult = offsetQuadratics[i]
            val cleanResult = cleanQuadratics[i]
            for (j in offsetResult.indices) {
                assertThat(offsetResult[j]).isWithin(POINT_VALUE_TOLERANCE).of(cleanResult[j])
            }
        }
    }

    @Test
    fun reuseInstance_stateIsReset() {
        // First conversion: a circle arc that generates multiple quadratics
        val circlePoints = floatArrayOf(50f, 0f, 100f, 50f, 50f, 100f)
        val circleWeight = sqrt(2.0f) / 2.0f
        converter.convert(circlePoints, circleWeight, 1.0f)

        assertThat(converter.quadraticCount).isGreaterThan(1)

        // Consume part of the iterator, leaving the converter in a partially used state
        val tempPoints = FloatArray(6)
        assertThat(converter.nextQuadratic(tempPoints)).isTrue()
        assertThat(converter.currentQuadratic).isEqualTo(1)

        // Second conversion: a simple line that generates one quadratic
        val linePoints = floatArrayOf(0f, 0f, 50f, 50f, 100f, 100f)
        converter.convert(linePoints, 0.8f, 0.1f)

        // Check that the state was reset correctly for the new conversion
        assertThat(converter.quadraticCount).isEqualTo(1)
        assertThat(converter.currentQuadratic).isEqualTo(0)

        // Verify the data is from the second conversion
        val quadraticPoints = FloatArray(6)
        assertThat(converter.nextQuadratic(quadraticPoints)).isTrue()
        assertThat(quadraticPoints[0]).isWithin(POINT_VALUE_TOLERANCE).of(0f)
        assertThat(quadraticPoints[1]).isWithin(POINT_VALUE_TOLERANCE).of(0f)
        assertThat(quadraticPoints[4]).isWithin(POINT_VALUE_TOLERANCE).of(100f)
        assertThat(quadraticPoints[5]).isWithin(POINT_VALUE_TOLERANCE).of(100f)

        // No more quadratics should be available
        assertThat(converter.nextQuadratic(quadraticPoints)).isFalse()
    }

    companion object {
        private const val POINT_VALUE_TOLERANCE = 1e-6f
    }
}
