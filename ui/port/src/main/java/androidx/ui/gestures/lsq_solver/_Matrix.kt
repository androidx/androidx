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

// TODO(abarth): Consider using vector_math.
internal class _Matrix(rows: Int, cols: Int) {
    private val _columns: Int = cols
    private val _elements: Array<Double> = Array(rows * cols) { 0.0 }

    fun get(row: Int, col: Int): Double {
        return _elements[(row * _columns) + col]
    }

    fun set(row: Int, col: Int, value: Double) {
        _elements[(row * _columns) + col] = value
    }

    fun getRow(row: Int): _Vector {
        return _Vector.fromVOL(_elements, (row * _columns), _columns)
    }
}