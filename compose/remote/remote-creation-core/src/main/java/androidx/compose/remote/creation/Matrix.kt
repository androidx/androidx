/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation

/** This class wraps a matrix expression in an easy to use class */
public class Matrix(public val rc: RemoteComposeContext, vararg exp: Float) {
    public var id: Float = rc.matrixExpression(*exp)

    public data class Vector4(val x: Float, val y: Float, val z: Float, val w: Float)

    public data class Vector3(val x: Float, val y: Float, val z: Float)

    public data class Vector2(val x: Float, val y: Float)

    /** Multiply the matrix by a vector */
    public fun mult(x: Float, y: Float): Vector2 {
        val out = FloatArray(2)
        rc.MatrixMultiply(id, floatArrayOf(x, y), out)
        return Vector2(out[0], out[1])
    }

    /** Multiply the matrix by a vector */
    public fun mult(x: Float, y: Float, z: Float, w: Float): Vector4 {
        val out = FloatArray(4)
        rc.MatrixMultiply(id, floatArrayOf(x, y, z, w), out)
        return Vector4(out[0], out[1], out[2], out[3])
    }

    /** Multiply the matrix by a vector */
    public fun mult(x: Float, y: Float, z: Float): Vector3 {
        val out = FloatArray(3)
        rc.MatrixMultiply(id, floatArrayOf(x, y, z), out)
        return Vector3(out[0], out[1], out[2])
    }

    /** projection Multiply the matrix by a vector (dividing by w) */
    public fun projectionMult(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
        val out = FloatArray(3)
        rc.MatrixMultiply(id, 1, floatArrayOf(x, y, z), out)
        return Triple(out[0], out[1], out[2])
    }
}
