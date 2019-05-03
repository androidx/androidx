/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material.borders

import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.text.TextDirection
import androidx.ui.toStringAsFixed

/**
 * Base class for [BorderRadius] that allows for text-direction aware resolution.
 *
 * A property or argument of this type accepts classes created either with [new
 * BorderRadius.only] and its variants, or [new BorderRadiusDirectional.only]
 * and its variants.
 *
 * To convert a [BorderRadiusGeometry] object of indeterminate type into a
 * [BorderRadius] object, call the [resolve] method.
 */
abstract class BorderRadiusGeometry {

    abstract val topLeft: Radius
    abstract val topRight: Radius
    abstract val bottomLeft: Radius
    abstract val bottomRight: Radius
    abstract val topStart: Radius
    abstract val topEnd: Radius
    abstract val bottomStart: Radius
    abstract val bottomEnd: Radius

    /**
     * Returns the difference between two [BorderRadiusGeometry] objects.
     *
     * If you know you are applying this to two [BorderRadius] or two
     * [BorderRadiusDirectional] objects, consider using the binary infix `-`
     * operator instead, which always returns an object of the same type as the
     * operands, and is typed accordingly.
     *
     * If [subtract] is applied to two objects of the same type ([BorderRadius] or
     * [BorderRadiusDirectional]), an object of that type will be returned (though
     * this is not reflected in the type system). Otherwise, an object
     * representing a combination of both is returned. That object can be turned
     * into a concrete [BorderRadius] using [resolve].
     *
     * This method returns the same result as [add] applied to the result of
     * negating the argument (using the prefix unary `-` operator or multiplying
     * the argument by -1.0 using the `*` operator).
     */
    open fun subtract(other: BorderRadiusGeometry): BorderRadiusGeometry {
        return MixedBorderRadius(
            topLeft - other.topLeft,
            topRight - other.topRight,
            bottomLeft - other.bottomLeft,
            bottomRight - other.bottomRight,
            topStart - other.topStart,
            topEnd - other.topEnd,
            bottomStart - other.bottomStart,
            bottomEnd - other.bottomEnd
        )
    }

    /**
     * Returns the sum of two [BorderRadiusGeometry] objects.
     *
     * If you know you are adding two [BorderRadius] or two [BorderRadiusDirectional]
     * objects, consider using the `+` operator instead, which always returns an
     * object of the same type as the operands, and is typed accordingly.
     *
     * If [add] is applied to two objects of the same type ([BorderRadius] or
     * [BorderRadiusDirectional]), an object of that type will be returned (though
     * this is not reflected in the type system). Otherwise, an object
     * representing a combination of both is returned. That object can be turned
     * into a concrete [BorderRadius] using [resolve].
     */
    open fun add(other: BorderRadiusGeometry): BorderRadiusGeometry {
        return MixedBorderRadius(
            topLeft + other.topLeft,
            topRight + other.topRight,
            bottomLeft + other.bottomLeft,
            bottomRight + other.bottomRight,
            topStart + other.topStart,
            topEnd + other.topEnd,
            bottomStart + other.bottomStart,
            bottomEnd + other.bottomEnd
        )
    }

    /**
     * Returns the [BorderRadiusGeometry] object with each corner radius negated.
     *
     * This is the same as multiplying the object by -1.0.
     *
     * This operator returns an object of the same type as the operand.
     */
    abstract operator fun unaryMinus(): BorderRadiusGeometry

    /**
     * Scales the [BorderRadiusGeometry] object's corners by the given factor.
     *
     * This operator returns an object of the same type as the operand.
     */
    abstract operator fun times(other: Float): BorderRadiusGeometry

    /**
     * Divides the [BorderRadiusGeometry] object's corners by the given factor.
     *
     * This operator returns an object of the same type as the operand.
     */
    abstract operator fun div(other: Float): BorderRadiusGeometry

    /**
     * Integer divides the [BorderRadiusGeometry] object's corners by the given factor.
     *
     * This operator returns an object of the same type as the operand.
     *
     * This operator may have unexpected results when applied to a mixture of
     * [BorderRadius] and [BorderRadiusDirectional] objects.
     */
    abstract fun truncDiv(other: Float): BorderRadiusGeometry

    /**
     * Computes the remainder of each corner by the given factor.
     *
     * This operator returns an object of the same type as the operand.
     *
     * This operator may have unexpected results when applied to a mixture of
     * [BorderRadius] and [BorderRadiusDirectional] objects.
     */
    abstract operator fun rem(other: Float): BorderRadiusGeometry

    /**
     * Convert this instance into a [BorderRadius], so that the radii are
     * expressed for specific physical corners (top-left, top-right, etc) rather
     * than in a direction-dependent manner.
     *
     * See also:
     *
     *  * [BorderRadius], for which this is a no-op (returns itself).
     *  * [BorderRadiusDirectional], which flips the horizontal direction
     *    based on the `direction` argument.
     */
    abstract fun resolve(direction: TextDirection?): BorderRadius

    override fun toString(): String {
        var visual: String? = null
        var logical: String? = null
        if (topLeft == topRight &&
            topRight == bottomLeft &&
            bottomLeft == bottomRight
        ) {
            if (topLeft != Radius.zero) {
                if (topLeft.x == topLeft.y) {
                    visual = "BorderRadius.circular(${topLeft.x.toStringAsFixed(1)})"
                } else {
                    visual = "BorderRadius.all($topLeft)"
                }
            }
        } else {
            // visuals aren"t the same and at least one isn't zero
            val result = StringBuffer()
            result.append("BorderRadius.only(")
            var comma = false
            if (topLeft != Radius.zero) {
                result.append("topLeft: $topLeft")
                comma = true
            }
            if (topRight != Radius.zero) {
                if (comma)
                    result.append(", ")
                result.append("topRight: $topRight")
                comma = true
            }
            if (bottomLeft != Radius.zero) {
                if (comma)
                    result.append(", ")
                result.append("bottomLeft: $bottomLeft")
                comma = true
            }
            if (bottomRight != Radius.zero) {
                if (comma)
                    result.append(", ")
                result.append("bottomRight: $bottomRight")
            }
            result.append(")")
            visual = result.toString()
        }
        if (topStart == topEnd &&
            topEnd == bottomEnd &&
            bottomEnd == bottomStart
        ) {
            if (topStart != Radius.zero) {
                if (topStart.x == topStart.y) {
                    logical = "BorderRadiusDirectional.circular(${topStart.x.toStringAsFixed(1)})"
                } else {
                    logical = "BorderRadiusDirectional.all($topStart)"
                }
            }
        } else {
            // logicals aren't the same and at least one isn't zero
            val result = StringBuffer()
            result.append("BorderRadiusDirectional.only(")
            var comma = false
            if (topStart != Radius.zero) {
                result.append("topStart: $topStart")
                comma = true
            }
            if (topEnd != Radius.zero) {
                if (comma)
                    result.append(", ")
                result.append("topEnd: $topEnd")
                comma = true
            }
            if (bottomStart != Radius.zero) {
                if (comma)
                    result.append(", ")
                result.append("bottomStart: $bottomStart")
                comma = true
            }
            if (bottomEnd != Radius.zero) {
                if (comma)
                    result.append(", ")
                result.append("bottomEnd: $bottomEnd")
            }
            result.append(")")
            logical = result.toString()
        }
        if (visual != null && logical != null)
            return "$visual + $logical"
        if (visual != null)
            return visual
        if (logical != null)
            return logical
        return "BorderRadius.Zero"
    }

    // TODO("Migration|Andrey: Autogenerated equals/hashCode. can't use data class")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BorderRadiusGeometry

        if (topLeft != other.topLeft) return false
        if (topRight != other.topRight) return false
        if (bottomLeft != other.bottomLeft) return false
        if (bottomRight != other.bottomRight) return false
        if (topStart != other.topStart) return false
        if (topEnd != other.topEnd) return false
        if (bottomStart != other.bottomStart) return false
        if (bottomEnd != other.bottomEnd) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + topRight.hashCode()
        result = 31 * result + bottomLeft.hashCode()
        result = 31 * result + bottomRight.hashCode()
        result = 31 * result + topStart.hashCode()
        result = 31 * result + topEnd.hashCode()
        result = 31 * result + bottomStart.hashCode()
        result = 31 * result + bottomEnd.hashCode()
        return result
    }
}

/**
 * Linearly interpolate between two [BorderRadiusGeometry] objects.
 *
 * If either is null, this function interpolates from [BorderRadius.Zero],
 * and the result is an object of the same type as the non-null argument. (If
 * both are null, this returns null.)
 *
 * If [lerp] is applied to two objects of the same type ([BorderRadius] or
 * [BorderRadiusDirectional]), an object of that type will be returned (though
 * this is not reflected in the type system). Otherwise, an object
 * representing a combination of both is returned. That object can be turned
 * into a concrete [BorderRadius] using [resolve].
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves such as [Curves.elasticInOut]).
 *
 * Values for `t` are usually obtained from an [Animation<Float>], such as
 * an [AnimationController].
 */
fun lerp(a: BorderRadiusGeometry?, b: BorderRadiusGeometry?, t: Float): BorderRadiusGeometry? {
    if (a == null && b == null)
        return null
    val newA = a ?: BorderRadius.Zero
    val newB = b ?: BorderRadius.Zero
    return newA.add((newB.subtract(newA)) * t)
}