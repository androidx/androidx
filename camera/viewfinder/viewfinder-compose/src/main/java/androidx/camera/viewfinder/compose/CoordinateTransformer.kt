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

package androidx.camera.viewfinder.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix

/**
 * Coordinate transformer that's used to convert coordinates from one space to another.
 *
 * [transformMatrix] must be set by whoever manipulates the coordinate space, otherwise an identity
 * will be used for the coordinate transformations. When used in a [Viewfinder], the viewfinder will
 * set this transform matrix.
 */
public interface CoordinateTransformer {
    /** Matrix that's used for coordinate transformations. */
    public val transformMatrix: Matrix

    /** Returns the [Offset] in the transformed space. */
    public fun Offset.transform(): Offset = transformMatrix.map(this)
}

/** CoordinateTransformer where the transformMatrix is mutable. */
public interface MutableCoordinateTransformer : CoordinateTransformer {
    override var transformMatrix: Matrix
}

/** Creates a [MutableCoordinateTransformer] with the given matrix as the transformMatrix. */
public fun MutableCoordinateTransformer(matrix: Matrix = Matrix()): MutableCoordinateTransformer =
    MutableCoordinateTransformerImpl(matrix)

private class MutableCoordinateTransformerImpl(initialMatrix: Matrix) :
    MutableCoordinateTransformer {
    override var transformMatrix: Matrix by mutableStateOf(initialMatrix)
}

/** [CoordinateTransformer] where the transformMatrix is the identity matrix. */
public object IdentityCoordinateTransformer : CoordinateTransformer {
    override val transformMatrix: Matrix = Matrix()

    override fun Offset.transform(): Offset = this
}
