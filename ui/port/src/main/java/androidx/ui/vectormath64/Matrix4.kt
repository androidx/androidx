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
package androidx.ui.vectormath64

import kotlin.math.asin
import kotlin.math.atan2

data class Matrix4(
    var x: Vector4 = Vector4(x = 1.0f),
    var y: Vector4 = Vector4(y = 1.0f),
    var z: Vector4 = Vector4(z = 1.0f),
    var w: Vector4 = Vector4(w = 1.0f)
) {
    constructor(right: Vector3, up: Vector3, forward: Vector3, position: Vector3 = Vector3()) :
            this(Vector4(right), Vector4(up), Vector4(forward), Vector4(position, 1.0f))
    constructor(m: Matrix4) : this(m.x.copy(), m.y.copy(), m.z.copy(), m.w.copy())

    companion object {
        fun of(vararg a: Float): Matrix4 {
            require(a.size >= 16)
            return Matrix4(
                    Vector4(a[0], a[4], a[8], a[12]),
                    Vector4(a[1], a[5], a[9], a[13]),
                    Vector4(a[2], a[6], a[10], a[14]),
                    Vector4(a[3], a[7], a[11], a[15])
            )
        }

        fun identity() = Matrix4()

        fun diagonal3Values(x: Float, y: Float, z: Float): Matrix4 {
            return Matrix4(
                    Vector4(x, 0f, 0f, 0f),
                    Vector4(0f, y, 0f, 0f),
                    Vector4(0f, 0f, z, 0f),
                    Vector4(0f, 0f, 0f, 1f)
            )
        }
    }

    inline val m4storage: List<Float>
        get() = x.v4storage + y.v4storage + z.v4storage + w.v4storage

    inline var right: Vector3
        get() = x.xyz
        set(value) {
            x.xyz = value
        }
    inline var up: Vector3
        get() = y.xyz
        set(value) {
            y.xyz = value
        }
    inline var forward: Vector3
        get() = z.xyz
        set(value) {
            z.xyz = value
        }
    inline var position: Vector3
        get() = w.xyz
        set(value) {
            w.xyz = value
        }

    inline val scale: Vector3
        get() = Vector3(length(x.xyz), length(y.xyz), length(z.xyz))
    inline val translation: Vector3
        get() = w.xyz
    val rotation: Vector3
        get() {
            val x = normalize(right)
            val y = normalize(up)
            val z = normalize(forward)

            return when {
                z.y <= -1.0f -> Vector3(degrees(-HALF_PI), 0.0f, degrees(atan2(x.z, y.z)))
                z.y >= 1.0f -> Vector3(degrees(HALF_PI), 0.0f, degrees(atan2(-x.z, -y.z)))
                else -> Vector3(
                        degrees(-asin(z.y)), degrees(-atan2(z.x, z.z)), degrees(atan2(x.y, y.y)))
            }
        }

    inline val upperLeft: Matrix3
        get() = Matrix3(x.xyz, y.xyz, z.xyz)

    operator fun get(column: Int) = when (column) {
        0 -> x
        1 -> y
        2 -> z
        3 -> w
        else -> throw IllegalArgumentException("column must be in 0..3")
    }
    operator fun get(column: Int, row: Int) = get(column)[row]

    operator fun get(column: MatrixColumn) = when (column) {
        MatrixColumn.X -> x
        MatrixColumn.Y -> y
        MatrixColumn.Z -> z
        MatrixColumn.W -> w
    }
    operator fun get(column: MatrixColumn, row: Int) = get(column)[row]

    fun getRow(row: Int): Vector4 {
        return Vector4(x[row], y[row], z[row], w[row])
    }

    operator fun invoke(row: Int, column: Int) = get(column - 1)[row - 1]
    operator fun invoke(row: Int, column: Int, v: Float) = set(column - 1, row - 1, v)

    operator fun set(column: Int, v: Vector4) {
        this[column].xyzw = v
    }
    operator fun set(column: Int, row: Int, v: Float) {
        this[column][row] = v
    }

    operator fun unaryMinus() = Matrix4(-x, -y, -z, -w)
    operator fun inc(): Matrix4 {
        x++
        y++
        z++
        w++
        return this
    }
    operator fun dec(): Matrix4 {
        x--
        y--
        z--
        w--
        return this
    }

    operator fun plus(v: Float) = Matrix4(x + v, y + v, z + v, w + v)
    operator fun minus(v: Float) = Matrix4(x - v, y - v, z - v, w - v)
    operator fun times(v: Float) = Matrix4(x * v, y * v, z * v, w * v)
    operator fun div(v: Float) = Matrix4(x / v, y / v, z / v, w / v)

    operator fun times(m: Matrix4): Matrix4 {
        val t = transpose(this)
        return Matrix4(
                Vector4(dot(t.x, m.x), dot(t.y, m.x), dot(t.z, m.x), dot(t.w, m.x)),
                Vector4(dot(t.x, m.y), dot(t.y, m.y), dot(t.z, m.y), dot(t.w, m.y)),
                Vector4(dot(t.x, m.z), dot(t.y, m.z), dot(t.z, m.z), dot(t.w, m.z)),
                Vector4(dot(t.x, m.w), dot(t.y, m.w), dot(t.z, m.w), dot(t.w, m.w))
        )
    }

    operator fun times(v: Vector4): Vector4 {
        val t = transpose(this)
        return Vector4(dot(t.x, v), dot(t.y, v), dot(t.z, v), dot(t.w, v))
    }

    operator fun timesAssign(m: Matrix4) {
        assignColumns(this * m)
    }

    fun assignColumns(other: Matrix4) {
        this.x = other.x
        this.y = other.y
        this.z = other.z
        this.w = other.w
    }

    fun toFloatArray() = floatArrayOf(
            x.x, y.x, z.x, w.x,
            x.y, y.y, z.y, w.y,
            x.z, y.z, z.z, w.z,
            x.w, y.w, z.w, w.w
    )

    override fun toString(): String {
        return """
            |${x.x} ${y.x} ${z.x} ${w.x}|
            |${x.y} ${y.y} ${z.y} ${w.y}|
            |${x.z} ${y.z} ${z.z} ${w.z}|
            |${x.w} ${y.w} ${z.w} ${w.w}|
            """.trimIndent()
    }

    // ***** Required methods from dart's matrix4 *****

    // / Transform [arg] of type [Vector3] using the perspective transformation
    // / defined by [this].
    fun perspectiveTransform(arg: Vector3): Vector3 {
        val argStorage = arg.v3storage
        val x_ = m4storage[0] * argStorage[0] +
                m4storage[4] * argStorage[1] +
                m4storage[8] * argStorage[2] +
                m4storage[12]
        val y_ = m4storage[1] * argStorage[0] +
                m4storage[5] * argStorage[1] +
                m4storage[9] * argStorage[2] +
                m4storage[13]
        val z_ = m4storage[2] * argStorage[0] +
                m4storage[6] * argStorage[1] +
                m4storage[10] * argStorage[2] +
                m4storage[14]
        val w_ = 1.0 / (m4storage[3] * argStorage[0] +
                m4storage[7] * argStorage[1] +
                m4storage[11] * argStorage[2] +
                m4storage[15])
        arg.x = x_ * w_.toFloat()
        arg.y = y_ * w_.toFloat()
        arg.z = z_ * w_.toFloat()
        return arg
    }
}
