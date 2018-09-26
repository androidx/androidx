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

package androidx.ui.gestures.lsq_solver

import kotlin.math.sqrt

// TODO(abarth): Consider using vector_math.
internal class _Vector internal constructor(
    val _length: Int,
    val _offset: Int = 0,
    val _elements: Array<Double> = Array(_length) { 0.0 }
) {

    operator fun get(i: Int) = _elements.get((i + _offset))

    operator fun set(i: Int, value: Double) {
        _elements[(i + _offset)] = value
    }

    operator fun times(a: _Vector): Double {
        var result = 0.0
        for (i in 0 until _length) {
            result += this[i] * a[i]
        }
        return result
    }

    fun norm(): Double = sqrt(this * this)

    companion object {
        internal fun fromVOL(values: Array<Double>, offset: Int, length: Int) =
            _Vector(length, offset, values)
    }
}