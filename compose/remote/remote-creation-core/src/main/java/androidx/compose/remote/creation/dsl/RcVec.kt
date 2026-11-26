/*
 * Copyright 2026 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.utilities.VectorOpCodes
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.dsl.minus
import androidx.compose.remote.creation.dsl.plus
import androidx.compose.remote.creation.dsl.times

/**
 * A value a particle update equation can produce: a scalar [RcFloat], which fills one particle
 * variable slot, or a vector [RcVec], which fills one slot per component. See
 * [RcScope.particlesLoopVec] and [RcScope.particlesComparisonVec].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public sealed interface RcParticleValue

/**
 * A vec2/3/4-valued expression — the vector analog of [RcFloat]. It carries the vector RPN that
 * produces it (evaluated by `VectorRpn`) and its [dim]. Build one from scalar components with
 * [vec], combine with vector math ([plus]/[minus]/[times], [dot], [cross], [length], [normalize]),
 * and use it as a vector update equation in a particle loop (see `particlesLoopVec`). When built
 * from components, [x]/[y]/[z]/[w] expose them as [RcFloat] for bridging back to scalar APIs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RcVec
internal constructor(
    /** The number of components: 2, 3 or 4. */
    @IntRange(from = 2, to = 4) public val dim: Int,
    internal val rpn: FloatArray,
    internal val writer: RemoteComposeWriter?,
    internal val comps: Array<RcFloat>?,
) : RcParticleValue {
    /** The vector RPN program producing this value. */
    internal fun toArray(): FloatArray = rpn

    public val x: RcFloat
        get() = comp(0)

    public val y: RcFloat
        get() = comp(1)

    public val z: RcFloat
        get() = comp(2)

    public val w: RcFloat
        get() = comp(3)

    public operator fun get(i: Int): RcFloat = comp(i)

    private fun comp(i: Int): RcFloat =
        comps?.getOrNull(i)
            ?: throw IllegalStateException(
                "component access is only valid on a vec built from components"
            )

    public operator fun plus(o: RcVec): RcVec = bin(o, VectorOpCodes.ADD)

    public operator fun minus(o: RcVec): RcVec = bin(o, VectorOpCodes.SUB)

    /** Componentwise multiply by another vector. */
    public operator fun times(o: RcVec): RcVec = bin(o, VectorOpCodes.MUL)

    /** Scale by a scalar (broadcast across components). */
    public operator fun times(s: RcFloat): RcVec =
        RcVec(dim, rpn + s.toArray() + floatArrayOf(VectorOpCodes.MUL), writer ?: s.writer, null)

    public operator fun times(s: Float): RcVec =
        RcVec(dim, rpn + floatArrayOf(s, VectorOpCodes.MUL), writer, null)

    private fun bin(o: RcVec, op: Float): RcVec =
        RcVec(dim, rpn + o.rpn + floatArrayOf(op), writer ?: o.writer, null)

    /**
     * Commit this vector to the document as a
     * [VectorExpression][androidx.compose.remote.core.operations.VectorExpression] and return an
     * [RcVec] backed by the resulting component variables (so [x]/[y]/[z] and [components] work
     * even for a *computed* vector like `normalize(a)`). A vec already built from components is
     * returned unchanged.
     */
    public fun flush(): RcVec {
        if (comps != null) return this
        val w = writer ?: throw IllegalStateException("RcVec.flush() requires a writer")
        val base = w.vectorExpressionInt(dim, 0, rpn)
        val cs = Array(dim) { i -> RcFloat(w, w.asFloatId((base + i).toLong())) }
        return when (dim) {
            2 -> vec(cs[0], cs[1])
            3 -> vec(cs[0], cs[1], cs[2])
            4 -> vec(cs[0], cs[1], cs[2], cs[3])
            else -> throw IllegalStateException("unsupported vec dim $dim")
        }
    }

    /**
     * The [dim] component [RcFloat]s. For a vec built from components, these are them directly; for
     * a computed vec, this [flush]es first (emitting a `VectorExpression`).
     */
    internal fun components(): Array<RcFloat> =
        comps ?: (flush().comps ?: error("flush produced no components"))

    /** Applies [f] to each component; this vec must be built from components (see [vec]). */
    internal fun mapComponents(f: (RcFloat) -> RcFloat): RcVec {
        val c =
            comps
                ?: throw IllegalStateException(
                    "per-particle selection requires a vec built from components"
                )
        return when (c.size) {
            2 -> vec(f(c[0]), f(c[1]))
            3 -> vec(f(c[0]), f(c[1]), f(c[2]))
            4 -> vec(f(c[0]), f(c[1]), f(c[2]), f(c[3]))
            else -> throw IllegalStateException("unsupported vec dim ${c.size}")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcVec) return false
        return dim == other.dim && rpn.contentEquals(other.rpn)
    }

    override fun hashCode(): Int = 31 * dim + rpn.contentHashCode()

    override fun toString(): String = "RcVec(dim=$dim, rpn=${rpn.contentToString()})"

    /**
     * The [dim] components as constant floats. For baked geometry (primitive centers/sizes) that
     * can't be reactive. Throws if any component isn't a constant.
     */
    internal fun constArray(): FloatArray {
        val c = comps ?: error("a constant vec must be built from components")
        return FloatArray(c.size) { i ->
            val a = c[i].toArray()
            require(a.size == 1 && !a[0].isNaN()) {
                "this vec parameter must be constant (component $i is reactive)"
            }
            a[0]
        }
    }
}

/** Build a vec2 from two scalar expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun vec(x: RcFloat, y: RcFloat): RcVec =
    RcVec(
        2,
        x.toArray() + y.toArray() + floatArrayOf(VectorOpCodes.VBUILD2),
        x.writer ?: y.writer,
        arrayOf(x, y),
    )

/** Build a vec3 from three scalar expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun vec(x: RcFloat, y: RcFloat, z: RcFloat): RcVec =
    RcVec(
        3,
        x.toArray() + y.toArray() + z.toArray() + floatArrayOf(VectorOpCodes.VBUILD3),
        x.writer ?: y.writer ?: z.writer,
        arrayOf(x, y, z),
    )

/** Build a vec4 from four scalar expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun vec(x: RcFloat, y: RcFloat, z: RcFloat, w: RcFloat): RcVec =
    RcVec(
        4,
        x.toArray() + y.toArray() + z.toArray() + w.toArray() + floatArrayOf(VectorOpCodes.VBUILD4),
        x.writer ?: y.writer ?: z.writer ?: w.writer,
        arrayOf(x, y, z, w),
    )

/** Dot product `a · b` (a scalar). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun dot(a: RcVec, b: RcVec): RcFloat =
    RcFloat(a.writer ?: b.writer, a.rpn + b.rpn + floatArrayOf(VectorOpCodes.VDOT))

/** Cross product `a × b` (vec3). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun cross(a: RcVec, b: RcVec): RcVec =
    RcVec(3, a.rpn + b.rpn + floatArrayOf(VectorOpCodes.VCROSS), a.writer ?: b.writer, null)

/** Length `|a|` (a scalar). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun length(a: RcVec): RcFloat = RcFloat(a.writer, a.rpn + floatArrayOf(VectorOpCodes.VLEN))

/** Normalized `a / |a|` (softened denominator). */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun normalize(a: RcVec): RcVec =
    RcVec(a.dim, a.rpn + floatArrayOf(VectorOpCodes.VNORM), a.writer, null)
