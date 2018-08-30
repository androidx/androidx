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

data class Vector3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {
    constructor(v: Vector2, z: Double = 0.0) : this(v.x, v.y, z)
    constructor(v: Vector3) : this(v.x, v.y, v.z)

    inline val v3storage: List<Double>
        get() = listOf(x, y, z)

    inline var r: Double
        get() = x
        set(value) {
            x = value
        }
    inline var g: Double
        get() = y
        set(value) {
            y = value
        }
    inline var b: Double
        get() = z
        set(value) {
            z = value
        }

    inline var s: Double
        get() = x
        set(value) {
            x = value
        }
    inline var t: Double
        get() = y
        set(value) {
            y = value
        }
    inline var p: Double
        get() = z
        set(value) {
            z = value
        }

    inline var xy: Vector2
        get() = Vector2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }
    inline var rg: Vector2
        get() = Vector2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }
    inline var st: Vector2
        get() = Vector2(x, y)
        set(value) {
            x = value.x
            y = value.y
        }

    inline var rgb: Vector3
        get() = Vector3(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }
    inline var xyz: Vector3
        get() = Vector3(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }
    inline var stp: Vector3
        get() = Vector3(x, y, z)
        set(value) {
            x = value.x
            y = value.y
            z = value.z
        }

    operator fun get(index: VectorComponent) = when (index) {
        VectorComponent.X, VectorComponent.R, VectorComponent.S -> x
        VectorComponent.Y, VectorComponent.G, VectorComponent.T -> y
        VectorComponent.Z, VectorComponent.B, VectorComponent.P -> z
        else -> throw IllegalArgumentException("index must be X, Y, Z, R, G, B, S, T or P")
    }

    operator fun get(index1: VectorComponent, index2: VectorComponent): Vector2 {
        return Vector2(get(index1), get(index2))
    }
    operator fun get(
        index1: VectorComponent,
        index2: VectorComponent,
        index3: VectorComponent
    ): Vector3 {
        return Vector3(get(index1), get(index2), get(index3))
    }

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException("index must be in 0..2")
    }

    operator fun get(index1: Int, index2: Int) = Vector2(get(index1), get(index2))
    operator fun get(index1: Int, index2: Int, index3: Int): Vector3 {
        return Vector3(get(index1), get(index2), get(index3))
    }

    operator fun set(index: Int, v: Double) = when (index) {
        0 -> x = v
        1 -> y = v
        2 -> z = v
        else -> throw IllegalArgumentException("index must be in 0..2")
    }

    operator fun set(index1: Int, index2: Int, v: Double) {
        set(index1, v)
        set(index2, v)
    }

    operator fun set(index1: Int, index2: Int, index3: Int, v: Double) {
        set(index1, v)
        set(index2, v)
        set(index3, v)
    }

    operator fun set(index: VectorComponent, v: Double) = when (index) {
        VectorComponent.X, VectorComponent.R, VectorComponent.S -> x = v
        VectorComponent.Y, VectorComponent.G, VectorComponent.T -> y = v
        VectorComponent.Z, VectorComponent.B, VectorComponent.P -> z = v
        else -> throw IllegalArgumentException("index must be X, Y, Z, R, G, B, S, T or P")
    }

    operator fun set(index1: VectorComponent, index2: VectorComponent, v: Double) {
        set(index1, v)
        set(index2, v)
    }

    operator fun set(
        index1: VectorComponent,
        index2: VectorComponent,
        index3: VectorComponent,
        v: Double
    ) {
        set(index1, v)
        set(index2, v)
        set(index3, v)
    }

    operator fun unaryMinus() = Vector3(-x, -y, -z)
    operator fun inc() = Vector3(this).apply {
        ++x
        ++y
        ++z
    }
    operator fun dec() = Vector3(this).apply {
        --x
        --y
        --z
    }

    inline operator fun plus(v: Double) = Vector3(x + v, y + v, z + v)
    inline operator fun minus(v: Double) = Vector3(x - v, y - v, z - v)
    inline operator fun times(v: Double) = Vector3(x * v, y * v, z * v)
    inline operator fun div(v: Double) = Vector3(x / v, y / v, z / v)

    inline operator fun plus(v: Vector2) = Vector3(x + v.x, y + v.y, z)
    inline operator fun minus(v: Vector2) = Vector3(x - v.x, y - v.y, z)
    inline operator fun times(v: Vector2) = Vector3(x * v.x, y * v.y, z)
    inline operator fun div(v: Vector2) = Vector3(x / v.x, y / v.y, z)

    inline operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)
    inline operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)
    inline operator fun times(v: Vector3) = Vector3(x * v.x, y * v.y, z * v.z)
    inline operator fun div(v: Vector3) = Vector3(x / v.x, y / v.y, z / v.z)

    inline fun transform(block: (Double) -> Double): Vector3 {
        x = block(x)
        y = block(y)
        z = block(z)
        return this
    }
}
