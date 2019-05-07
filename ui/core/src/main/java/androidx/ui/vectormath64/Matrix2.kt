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

data class Matrix2(
    var x: Vector2 = Vector2(x = 1.0f),
    var y: Vector2 = Vector2(y = 1.0f)
) {
    constructor(m: Matrix2) : this(m.x.copy(), m.y.copy())

    companion object {
        fun of(vararg a: Float): Matrix2 {
            require(a.size >= 4)
            return Matrix2(
                    Vector2(a[0], a[2]),
                    Vector2(a[1], a[3])
            )
        }

        fun identity() = Matrix2()
    }

    inline val m2storage: List<Float>
        get() = x.v2storage + y.v2storage

    operator fun get(column: Int) = when (column) {
        0 -> x
        1 -> y
        else -> throw IllegalArgumentException("column must be in 0..1")
    }
    operator fun get(column: Int, row: Int) = get(column)[row]

    operator fun get(column: MatrixColumn) = when (column) {
        MatrixColumn.X -> x
        MatrixColumn.Y -> y
        else -> throw IllegalArgumentException("column must be X or Y")
    }
    operator fun get(column: MatrixColumn, row: Int) = get(column)[row]

    operator fun set(column: Int, v: Vector2) {
        this[column].xy = v
    }
    operator fun set(column: Int, row: Int, v: Float) {
        this[column][row] = v
    }

    operator fun unaryMinus() = Matrix2(-x, -y)
    operator fun inc() = Matrix2(this).apply {
        ++x
        ++y
    }
    operator fun dec() = Matrix2(this).apply {
        --x
        --y
    }

    operator fun plus(v: Float) = Matrix2(x + v, y + v)
    operator fun minus(v: Float) = Matrix2(x - v, y - v)
    operator fun times(v: Float) = Matrix2(x * v, y * v)
    operator fun div(v: Float) = Matrix2(x / v, y / v)

    operator fun times(m: Matrix2): Matrix2 {
        val t = transpose(this)
        return Matrix2(
                Vector2(dot(t.x, m.x), dot(t.y, m.x)),
                Vector2(dot(t.x, m.y), dot(t.y, m.y))
        )
    }

    operator fun times(v: Vector2): Vector2 {
        val t = transpose(this)
        return Vector2(dot(t.x, v), dot(t.y, v))
    }

    fun toFloatArray() = floatArrayOf(
            x.x, y.x,
            x.y, y.y
    )

    override fun toString(): String {
        return """
            |${x.x} ${y.x}|
            |${x.y} ${y.y}|
            """.trimIndent()
    }
}
