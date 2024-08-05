/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.platform

import android.graphics.Matrix as AndroidMatrix
import androidx.compose.ui.geometry.MutableRect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.isIdentity
import androidx.compose.ui.graphics.setFrom

/**
 * Helper class to cache a [Matrix] and inverse [Matrix], allowing the instance to be reused until
 * the Layer's properties have changed, causing it to call [invalidate].
 *
 * This allows us to avoid repeated calls to [AndroidMatrix.getValues], which calls an expensive
 * native method (nGetValues). If we know the matrix hasn't changed, we can just re-use it without
 * needing to read and update values.
 */
internal class LayerMatrixCache<T>(
    private val getMatrix: (target: T, matrix: AndroidMatrix) -> Unit
) {
    private var androidMatrixCache: AndroidMatrix? = null
    private var previousAndroidMatrix: AndroidMatrix? = null
    private var matrixCache: Matrix = Matrix()
    private var inverseMatrixCache: Matrix = Matrix()

    private var isDirty = false
    private var isInverseDirty = false
    private var isInverseValid = true
    private var isIdentity = true

    /** Reset the cache to the identity matrix. */
    fun reset() {
        isDirty = false
        isInverseDirty = false
        isIdentity = true
        isInverseValid = true
        matrixCache.reset()
        inverseMatrixCache.reset()
    }

    /**
     * Ensures that the internal matrix will be updated next time [calculateMatrix] or
     * [calculateInverseMatrix] is called - this should be called when something that will change
     * the matrix calculation has happened.
     */
    fun invalidate() {
        isDirty = true
        isInverseDirty = true
    }

    /**
     * Returns the cached [Matrix], updating it if required (if [invalidate] was previously called).
     */
    fun calculateMatrix(target: T): Matrix {
        val matrix = matrixCache
        if (!isDirty) {
            return matrix
        }

        val cachedMatrix = androidMatrixCache ?: AndroidMatrix().also { androidMatrixCache = it }

        getMatrix(target, cachedMatrix)

        val prevMatrix = previousAndroidMatrix
        if (prevMatrix == null || cachedMatrix != prevMatrix) {
            matrix.setFrom(cachedMatrix)
            androidMatrixCache = prevMatrix
            previousAndroidMatrix = cachedMatrix
        }

        isDirty = false
        isIdentity = matrix.isIdentity()
        return matrix
    }

    /**
     * Returns the cached inverse [Matrix], updating it if required (if [invalidate] was previously
     * called). This returns `null` if the inverse matrix isn't valid. This can happen, for example,
     * when scaling is 0.
     */
    fun calculateInverseMatrix(target: T): Matrix? {
        val matrix = inverseMatrixCache
        if (isInverseDirty) {
            val normalMatrix = calculateMatrix(target)
            isInverseValid = normalMatrix.invertTo(matrix)
            isInverseDirty = false
        }
        return if (isInverseValid) matrix else null
    }

    fun map(target: T, rect: MutableRect) {
        val matrix = calculateMatrix(target)
        if (!isIdentity) {
            matrix.map(rect)
        }
    }

    fun mapInverse(target: T, rect: MutableRect) {
        val matrix = calculateInverseMatrix(target)
        if (matrix == null) {
            rect.set(0f, 0f, 0f, 0f)
        } else if (!isIdentity) {
            matrix.map(rect)
        }
    }

    fun map(target: T, offset: Offset): Offset {
        val matrix = calculateMatrix(target)
        return if (!isIdentity) {
            matrix.map(offset)
        } else {
            offset
        }
    }

    fun mapInverse(target: T, offset: Offset): Offset {
        val matrix = calculateInverseMatrix(target)
        return if (matrix == null) {
            Offset.Infinite
        } else if (!isIdentity) {
            matrix.map(offset)
        } else {
            offset
        }
    }
}
