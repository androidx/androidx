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

import androidx.ui.core.Density
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.TextDirection
import androidx.ui.painting.Canvas
import androidx.ui.painting.Path

/**
 * Base class for shape outlines.
 *
 * This class handles how to add multiple borders together.
 */
abstract class ShapeBorder {

    // TODO("Migration|Andrey: Needs EdgeInsetsGeometry")
//    /// The widths of the sides of this border represented as an [EdgeInsets].
//    ///
//    /// Specifically, this is the amount by which a rectangle should be inset so
//    /// as to avoid painting over any important part of the border. It is the
//    /// amount by which additional borders will be inset before they are drawn.
//    ///
//    /// This can be used, for example, with a [Padding] widget to inset a box by
//    /// the size of these borders.
//    ///
//    /// Shapes that have a fixed ratio regardless of the area on which they are
//    /// painted, or that change their rendering based on the size they are given
//    /// when painting (for instance [CircleBorder]), will not return valid
//    /// [dimensions] information because they cannot know their eventual size when
//    /// computing their [dimensions].
//    EdgeInsetsGeometry get dimensions;

    /**
     * Attempts to create a new object that represents the amalgamation of `this`
     * border and the `other` border.
     *
     * If the type of the other border isn't known, or the given instance cannot
     * be reasonably added to this instance, then this should return null.
     *
     * This method is used by the [operator +] implementation.
     *
     * The `reversed` argument is true if this object was the right operand of
     * the `+` operator, and false if it was the left operand.
     */
    protected open fun add(other: ShapeBorder, reversed: Boolean = false): ShapeBorder? = null

    /**
     * Creates a new border consisting of the two borders on either side of the
     * operator.
     *
     * If the borders belong to classes that know how to add themselves, then
     * this results in a new border that represents the intelligent addition of
     * those two borders (see [add]). Otherwise, an object is returned that
     * merely paints the two borders sequentially, with the left hand operand on
     * the inside and the right hand operand on the outside.
     */
    operator fun plus(other: ShapeBorder): ShapeBorder {
        return add(other) ?: other.add(this, reversed = true) ?: TODO(
            "Migration|Andrey: Needs _CompoundBorder")
//                _CompoundBorder(ShapeBorder {[other, this])
    }

    /**
     * Creates a copy of this border, scaled by the factor `t`.
     *
     * Typically this means scaling the width of the border's side, but it can
     * also include scaling other artifacts of the border, e.g. the border radius
     * of a [RoundedRectangleBorder].
     *
     * The `t` argument represents the multiplicand, or the position on the
     * timeline for an interpolation from nothing to `this`, with 0.0 meaning
     * that the object returned should be the nil variant of this object, 1.0
     * meaning that no change should be applied, returning `this` (or something
     * equivalent to `this`), and other values meaning that the object should be
     * multiplied by `t`. Negative values are allowed but may be meaningless
     * (they correspond to extrapolating the interpolation from this object to
     * nothing, and going beyond nothing)
     *
     * Values for `t` are usually obtained from an [Animation<Float>], such as
     * an [AnimationController].
     *
     * See also:
     *
     *  * [BorderSide.scale], which most [ShapeBorder] subclasses defer to for
     *    the actual computation.
     */
    abstract fun scale(t: Float): ShapeBorder

    /**
     * Linearly interpolates from another [ShapeBorder] (possibly of another
     * class) to `this`.
     *
     * When implementing this method in subclasses, return null if this class
     * cannot interpolate from `a`. In that case, [lerp] will try `a`'s [lerpTo]
     * method instead. If `a` is null, this must not return null.
     *
     * The base class implementation handles the case of `a` being null by
     * deferring to [scale].
     *
     * The `t` argument represents position on the timeline, with 0.0 meaning
     * that the interpolation has not started, returning `a` (or something
     * equivalent to `a`), 1.0 meaning that the interpolation has finished,
     * returning `this` (or something equivalent to `this`), and values in
     * between meaning that the interpolation is at the relevant point on the
     * timeline between `a` and `this`. The interpolation can be extrapolated
     * beyond 0.0 and 1.0, so negative values and values greater than 1.0 are
     * valid (and can easily be generated by curves such as
     * [Curves.elasticInOut]).
     *
     * Values for `t` are usually obtained from an [Animation<Float>], such as
     * an [AnimationController].
     *
     * Instead of calling this directly, use [ShapeBorder.lerp].
     */
    open fun lerpFrom(a: ShapeBorder?, t: Float): ShapeBorder? {
        if (a == null)
            return scale(t)
        return null
    }

    /**
     * Linearly interpolates from `this` to another [ShapeBorder] (possibly of
     * another class).
     *
     * This is called if `b`'s [lerpTo] did not know how to handle this class.
     *
     * When implementing this method in subclasses, return null if this class
     * cannot interpolate from `b`. In that case, [lerp] will apply a default
     * behavior instead. If `b` is null, this must not return null.
     *
     * The base class implementation handles the case of `b` being null by
     * deferring to [scale].
     *
     * The `t` argument represents position on the timeline, with 0.0 meaning
     * that the interpolation has not started, returning `this` (or something
     * equivalent to `this`), 1.0 meaning that the interpolation has finished,
     * returning `b` (or something equivalent to `b`), and values in between
     * meaning that the interpolation is at the relevant point on the timeline
     * between `this` and `b`. The interpolation can be extrapolated beyond 0.0
     * and 1.0, so negative values and values greater than 1.0 are valid (and can
     * easily be generated by curves such as [Curves.elasticInOut]).
     *
     * Values for `t` are usually obtained from an [Animation<Float>], such as
     * an [AnimationController].
     *
     * Instead of calling this directly, use [ShapeBorder.lerp].
     */
    open fun lerpTo(b: ShapeBorder?, t: Float): ShapeBorder? {
        if (b == null)
            return scale(1.0f - t)
        return null
    }

    /**
     * Create a [Path] that describes the outer edge of the border.
     *
     * This path must not cross the path given by [getInnerPath] for the same
     * [Rect].
     *
     * To obtain a [Path] that describes the area of the border itself, set the
     * [Path.fillType] of the returned object to [PathFillType.evenOdd], and add
     * to this object the path returned from [getInnerPath] (using
     * [Path.addPath]).
     *
     * The `textDirection` argument must be provided non-null if the border
     * has a text direction dependency (for example if it is expressed in terms
     * of "start" and "end" instead of "left" and "right"). It may be null if
     * the border will not need the text direction to paint itself.
     *
     * See also:
     *
     *  * [getInnerPath], which creates the path for the inner edge.
     *  * [Path.contains], which can tell if an [Offset] is within a [Path].
     */
    abstract fun getOuterPath(
        rect: Rect,
        density: Density,
        textDirection: TextDirection? = null
    ): Path

    /**
     * Create a [Path] that describes the inner edge of the border.
     *
     * This path must not cross the path given by [getOuterPath] for the same
     * [Rect].
     *
     * To obtain a [Path] that describes the area of the border itself, set the
     * [Path.fillType] of the returned object to [PathFillType.evenOdd], and add
     * to this object the path returned from [getOuterPath] (using
     * [Path.addPath]).
     *
     * The `textDirection` argument must be provided and non-null if the border
     * has a text direction dependency (for example if it is expressed in terms
     * of "start" and "end" instead of "left" and "right"). It may be null if
     * the border will not need the text direction to paint itself.
     *
     * See also:
     *
     *  * [getOuterPath], which creates the path for the outer edge.
     *  * [Path.contains], which can tell if an [Offset] is within a [Path].
     */
    abstract fun getInnerPath(
        rect: Rect,
        density: Density,
        textDirection: TextDirection? = null
    ): Path

    /**
     * Paints the border within the given [Rect] on the given [Canvas].
     *
     * The `textDirection` argument must be provided and non-null if the border
     * has a text direction dependency (for example if it is expressed in terms
     * of "start" and "end" instead of "left" and "right"). It may be null if
     * the border will not need the text direction to paint itself.
     */
    abstract fun paint(
        canvas: Canvas,
        density: Density,
        rect: Rect,
        textDirection: TextDirection? = null
    )

    // TODO(Andrey) Investigate how to make it better. b/129278276
    abstract val borderStyle: BorderStyle
}

/**
 * Linearly interpolates between two [ShapeBorder]s.
 *
 * This defers to `b`'s [lerpTo] function if `b` is not null. If `b` is
 * null or if its [lerpTo] returns null, it uses `a`'s [lerpFrom]
 * function instead. If both return null, it returns `a` before `t=0.5`
 * and `b` after `t=0.5`.
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
fun lerp(a: ShapeBorder?, b: ShapeBorder?, t: Float): ShapeBorder? {
    var result: ShapeBorder? = null
    if (b != null)
        result = b.lerpFrom(a, t)
    if (result == null && a != null)
        result = a.lerpTo(b, t)
    return result ?: (if (t < 0.5) a else b)
}