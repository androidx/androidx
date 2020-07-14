/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

import org.jetbrains.skija.Matrix33

@Suppress("unused")
class Matrix() {
    companion object {
        val identityArray = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
    }

    internal var skija = Matrix33(*identityArray)
        private set

    fun isIdentity() = skija.mat!!.contentEquals(identityArray)

    fun getValues(values: FloatArray) {
        skija.mat.copyInto(values)
    }

    fun setValues(values: FloatArray) {
        skija = Matrix33(*values)
    }

    fun reset() {
        skija = Matrix33(*identityArray)
    }

    fun setTranslate(dx: Float, dy: Float) {
        skija = skija.makeConcat(Matrix33.makeTranslate(dx, dy))
    }
}