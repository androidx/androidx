/*
 * Copyright 2022 The Android Open Source Project
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

/**
 * This class converts a given Conic object to the equivalent set of Quadratic objects. It stores
 * all quadratics from a conversion in the call to [convert], but returns only one at a time, from
 * nextQuadratic(), storing the rest for later retrieval (since a PathIterator only retrieves one
 * object at a time).
 *
 * This object is stateful, using quadraticCount, currentQuadratic, and quadraticData to send back
 * the next quadratic when requested, in [nextQuadratic].
 */
internal class ConicConverter {
    /** The total number of quadratics currently stored in the converter */
    var quadraticCount: Int = 0
        private set

    /**
     * The index of the current Quadratic; this is the next quadratic to be returned in the call to
     * nextQuadratic().
     */
    var currentQuadratic = 0

    /**
     * Storage for all quadratics for a particular conic. Set to reasonable default size, will need
     * to resize if we ever get a return count larger than the current size. Initial size holds up
     * to 5 quadratic subdivisions: 2^5 quadratics, with 3 points per quadratic, and 2 floats per
     * point, where all quadratics overlap in one point except the ends.
     */
    private var quadraticData = FloatArray(32 * 2 * 2 + 2)

    /**
     * This function stores the next converted quadratic in the given points array, returning true
     * if this happened, false if there was no quadratic to be returned.
     */
    fun nextQuadratic(points: FloatArray, offset: Int = 0): Boolean {
        if (currentQuadratic < quadraticCount) {
            val index = currentQuadratic * 2 * 2 // Quadratics overlap by 1 point
            points[0 + offset] = quadraticData[index]
            points[1 + offset] = quadraticData[index + 1]
            points[2 + offset] = quadraticData[index + 2]
            points[3 + offset] = quadraticData[index + 3]
            points[4 + offset] = quadraticData[index + 4]
            points[5 + offset] = quadraticData[index + 5]
            currentQuadratic++
            return true
        }
        return false
    }

    /** Converts the conic in [points] to a series of quadratics, which will all be stored */
    fun convert(points: FloatArray, weight: Float, tolerance: Float, offset: Int = 0) {
        val isDalvik = "dalvik".equals(System.getProperty("java.vm.name"), ignoreCase = true)
        if (isDalvik) {
            quadraticCount =
                internalConicToQuadratics(points, offset, quadraticData, weight, tolerance)
            // 3 points per quadratic, 2 floats per point, with one point of overlap
            val newDataSize = quadraticCount * 2 * 2 + 2
            if (newDataSize > quadraticData.size) {
                quadraticData = FloatArray(newDataSize)
                quadraticCount =
                    internalConicToQuadratics(points, offset, quadraticData, weight, tolerance)
            }
        } else {
            quadraticData = conicToQuadratics(points, offset, weight, tolerance)
            quadraticCount = (quadraticData.size - 2) / 4
        }
        currentQuadratic = 0
    }

    /**
     * The actual conversion from conic to quadratic data happens in native code, in the library
     * loaded elsewhere. This JNI function wraps that native functionality.
     */
    @Suppress("KotlinJniMissingFunction")
    private external fun internalConicToQuadratics(
        conicPoints: FloatArray,
        offset: Int,
        quadraticPoints: FloatArray,
        weight: Float,
        tolerance: Float,
    ): Int
}
