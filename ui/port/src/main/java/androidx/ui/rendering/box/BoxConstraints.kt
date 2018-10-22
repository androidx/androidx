package androidx.ui.rendering.box

import androidx.ui.assert
import androidx.ui.clamp
import androidx.ui.engine.geometry.Size
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.InformationCollector
import androidx.ui.lerpDouble
import androidx.ui.rendering.obj.Constraints
import androidx.ui.toStringAsFixed
import androidx.ui.truncDiv

/** Immutable layout constraints for [RenderBox] layout.
 *
 * A [Size] respects a [BoxConstraints] if, and only if, all of the following
 * relations hold:
 *
 * * [minWidth] <= [Size.width] <= [maxWidth]
 * * [minHeight] <= [Size.height] <= [maxHeight]
 *
 * The constraints themselves must satisfy these relations:
 *
 * * 0.0 <= [minWidth] <= [maxWidth] <= [Double.POSITIVE_INFINITY]
 * * 0.0 <= [minHeight] <= [maxHeight] <= [Double.POSITIVE_INFINITY]
 *
 * [Double.POSITIVE_INFINITY] is a legal value for each constraint.
 *
 * ## The box layout model
 *
 * Render objects in the Flutter framework are laid out by a one-pass layout
 * model which walks down the render tree passing constraints, then walks back
 * up the render tree passing concrete geometry.
 *
 * For boxes, the constraints are [BoxConstraints], which, as described herein,
 * consist of four numbers: a minimum width [minWidth], a maximum width
 * [maxWidth], a minimum height [minHeight], and a maximum height [maxHeight].
 *
 * The geometry for boxes consists of a [Size], which must satisfy the
 * constraints described above.
 *
 * Each [RenderBox] (the objects that provide the layout models for box
 * widgets) receives [BoxConstraints] from its parent, then lays out each of
 * its children, then picks a [Size] that satisfies the [BoxConstraints].
 *
 * Render objects position their children independently of laying them out.
 * Frequently, the parent will use the children's sizes to determine their
 * position. A child does not know its position and will not necessarily be
 * laid out again, or repainted, if its position changes.
 *
 * ## Terminology
 *
 * When the minimum constraints and the maximum constraint in an axis are the
 * same, that axis is _tightly_ constrained. See: [new
 * BoxConstraints.tightFor], [new BoxConstraints.tightForFinite], [tighten],
 * [hasTightWidth], [hasTightHeight], [isTight].
 *
 * An axis with a minimum constraint of 0.0 is _loose_ (regardless of the
 * maximum constraint; if it is also 0.0, then the axis is simultaneously tight
 * and loose!). See: [new BoxConstraints.loose], [loosen].
 *
 * An axis whose maximum constraint is not infinite is _bounded_. See:
 * [hasBoundedWidth], [hasBoundedHeight].
 *
 * An axis whose maximum constraint is infinite is _unbounded_. An axis is
 * _expanding_ if it is tightly infinite (its minimum and maximum constraints
 * are both infinite). See: [new BoxConstraints.expand].
 *
 * A size is _constrained_ when it satisfies a [BoxConstraints] description.
 * See: [constrain], [constrainWidth], [constrainHeight],
 * [constrainDimensions], [constrainSizeAndAttemptToPreserveAspectRatio],
 * [isSatisfiedBy].
 */

// Creates box constraints with the given constraints.
data class BoxConstraints(
        // The minimum width that satisfies the constraints.
    val minWidth: Double = 0.0,
        // The maximum width that satisfies the constraints.
        // Might be [Double.POSITIVE_INFINITY].
    val maxWidth: Double = Double.POSITIVE_INFINITY,
        // The minimum height that satisfies the constraints.
    val minHeight: Double = 0.0,
        // The maximum height that satisfies the constraints.
        // Might be [Double.POSITIVE_INFINITY].
    val maxHeight: Double = Double.POSITIVE_INFINITY
) : Constraints() {

    companion object {
        // Creates box constraints that is respected only by the given size.
        fun tight(size: Size): BoxConstraints {
            return BoxConstraints(size.width, size.width, size.height, size.height)
        }

        /**
         * Creates box constraints that require the given width or height.
         *
         * See also:
         *
         *  * [new BoxConstraints.tightForFinite], which is similar but instead of
         *    being tight if the value is non-null, is tight if the value is not
         *    infinite.
         */
        fun tightFor(width: Double? = null, height: Double? = null): BoxConstraints {
            return BoxConstraints(minWidth = width ?: 0.0,
                    maxWidth = width ?: Double.POSITIVE_INFINITY,
                    minHeight = height ?: 0.0,
                    maxHeight = height ?: Double.POSITIVE_INFINITY
            )
        }

        /**
         * Creates box constraints that require the given width or height, except if
         * they are infinite.
         *
         * See also:
         *
         *  * [new BoxConstraints.tightFor], which is similar but instead of being
         *    tight if the value is not infinite, is tight if the value is non-null.
         */
        fun tightForFinite(
            width: Double = Double.POSITIVE_INFINITY,
            height: Double = Double.POSITIVE_INFINITY
        ): BoxConstraints {
            return BoxConstraints(minWidth = if (width != Double.POSITIVE_INFINITY) width else 0.0,
                    maxWidth = if (width != Double.POSITIVE_INFINITY) width
                    else Double.POSITIVE_INFINITY,
                    minHeight = if (height != Double.POSITIVE_INFINITY) height
                    else 0.0,
                    maxHeight = if (height != Double.POSITIVE_INFINITY) height
                    else Double.POSITIVE_INFINITY
            )
        }

        /**
         * Creates box constraints that forbid sizes larger than the given size.
         */
        fun loose(size: Size): BoxConstraints {
            return BoxConstraints(minWidth = 0.0,
                    maxWidth = size.width,
                    minHeight = 0.0,
                    maxHeight = size.height)
        }

        /**
         * Creates box constraints that expand to fill another box constraints.
         *
         * If width or height is given, the constraints will require exactly the
         * given value in the given dimension.
         */
        fun expand(width: Double? = null, height: Double? = null): BoxConstraints {
            return BoxConstraints(minWidth = width ?: Double.POSITIVE_INFINITY,
                    maxWidth = width ?: Double.POSITIVE_INFINITY,
                    minHeight = height ?: Double.POSITIVE_INFINITY,
                    maxHeight = height ?: Double.POSITIVE_INFINITY
            )
        }

        /**
         * Linearly interpolate between two BoxConstraints.
         *
         * If either is null, this function interpolates from a [BoxConstraints]
         * object whose fields are all set to 0.0.
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
         * Values for `t` are usually obtained from an [Animation<double>], such as
         * an [AnimationController].
         */
        fun lerp(a: BoxConstraints?, b: BoxConstraints?, t: Double): BoxConstraints? {
            if (a == null && b == null)
                return null
            if (a == null)
                return b!! * t
            if (b == null)
                return a * (1.0 - t)
            assert(a.debugAssertIsValid())
            assert(b.debugAssertIsValid())
            assert((a.minWidth.isFinite() && b.minWidth.isFinite()) ||
                    (a.minWidth == Double.POSITIVE_INFINITY &&
                            b.minWidth == Double.POSITIVE_INFINITY)
            ) { "Cannot interpolate between finite constraints and unbounded constraints." }
            assert((a.maxWidth.isFinite() && b.maxWidth.isFinite()) ||
                    (a.maxWidth == Double.POSITIVE_INFINITY &&
                            b.maxWidth == Double.POSITIVE_INFINITY)
            ) { "Cannot interpolate between finite constraints and unbounded constraints." }
            assert((a.minHeight.isFinite() && b.minHeight.isFinite()) ||
                    (a.minHeight == Double.POSITIVE_INFINITY &&
                            b.minHeight == Double.POSITIVE_INFINITY)
            ) { "Cannot interpolate between finite constraints and unbounded constraints." }
            assert((a.maxHeight.isFinite() && b.maxHeight.isFinite()) ||
                    (a.maxHeight == Double.POSITIVE_INFINITY &&
                            b.maxHeight == Double.POSITIVE_INFINITY)
            ) { "Cannot interpolate between finite constraints and unbounded constraints." }
            return BoxConstraints(
                    minWidth = if (a.minWidth.isFinite()) lerpDouble(a.minWidth, b.minWidth,
                            t) else Double.POSITIVE_INFINITY,
                    maxWidth = if (a.maxWidth.isFinite()) lerpDouble(a.maxWidth, b.maxWidth,
                            t) else Double.POSITIVE_INFINITY,
                    minHeight = if (a.minHeight.isFinite()) lerpDouble(a.minHeight, b.minHeight,
                            t) else Double.POSITIVE_INFINITY,
                    maxHeight = if (a.maxHeight.isFinite()) lerpDouble(a.maxHeight, b.maxHeight,
                            t) else Double.POSITIVE_INFINITY
            )
        }
    }

    /**
     * Creates a copy of this box constraints but with the given fields replaced with the new values.
     */
    fun copyWith(
        minWidth: Double? = null,
        maxWidth: Double? = null,
        minHeight: Double? = null,
        maxHeight: Double? = null
    ): BoxConstraints {
        return BoxConstraints(
                minWidth = minWidth ?: this.minWidth,
                maxWidth = maxWidth ?: this.maxWidth,
                minHeight = minHeight ?: this.minHeight,
                maxHeight = maxHeight ?: this.maxHeight
        )
    }

    // TODO(Migration/Andrey): needs EdgeInsets
//     * Returns new box constraints that are smaller by the given edge dimensions.
//    BoxConstraints deflate(EdgeInsets edges) {
//        assert(edges != null);
//        assert(debugAssertIsValid());
//        final double horizontal = edges.horizontal;
//        final double vertical = edges.vertical;
//        final double deflatedMinWidth = math.max(0.0, minWidth - horizontal);
//        final double deflatedMinHeight = math.max(0.0, minHeight - vertical);
//        return new BoxConstraints(
//                minWidth: deflatedMinWidth,
//        maxWidth: math.max(deflatedMinWidth, maxWidth - horizontal),
//        minHeight: deflatedMinHeight,
//        maxHeight: math.max(deflatedMinHeight, maxHeight - vertical)
//        );
//    }

    /**
     * Returns new box constraints that remove the minimum width and height requirements.
     */
    fun loosen(): BoxConstraints {
        assert(debugAssertIsValid())
        return BoxConstraints(
                minWidth = 0.0,
                maxWidth = maxWidth,
                minHeight = 0.0,
                maxHeight = maxHeight
        )
    }

    /**
     * Returns new box constraints that respect the given constraints while being
     * as close as possible to the original constraints.
     */
    fun enforce(constraints: BoxConstraints): BoxConstraints {
        return BoxConstraints(
                minWidth = minWidth.clamp(constraints.minWidth, constraints.maxWidth),
                maxWidth = maxWidth.clamp(constraints.minWidth, constraints.maxWidth),
                minHeight = minHeight.clamp(constraints.minHeight, constraints.maxHeight),
                maxHeight = maxHeight.clamp(constraints.minHeight, constraints.maxHeight)
        )
    }

    /**
     * Returns new box constraints with a tight width and/or height as close to
     * the given width and height as possible while still respecting the original
     * box constraints.
     */
    fun tighten(width: Double? = null, height: Double? = null): BoxConstraints {
        return BoxConstraints(minWidth = width?.clamp(minWidth, maxWidth) ?: minWidth,
                maxWidth = width?.clamp(minWidth, maxWidth) ?: maxWidth,
                minHeight = height?.clamp(minHeight, maxHeight) ?: minHeight,
                maxHeight = height?.clamp(minHeight, maxHeight) ?: maxHeight
        )
    }

    /**
     * A box constraints with the width and height constraints flipped.
     */
    fun flipped(): BoxConstraints {
        return BoxConstraints(
                minWidth = minHeight,
                maxWidth = maxHeight,
                minHeight = minWidth,
                maxHeight = maxWidth
        )
    }

    /**
     * Returns box constraints with the same width constraints but with
     * unconstrained height.
     */
    fun widthConstraints(): BoxConstraints {
        return BoxConstraints(minWidth = minWidth, maxWidth = maxWidth)
    }

    /**
     * Returns box constraints with the same height constraints but with
     * unconstrained width
     */
    fun heightConstraints(): BoxConstraints {
        return BoxConstraints(minHeight = minHeight, maxHeight = maxHeight)
    }

    /**
     * Returns the width that both satisfies the constraints and is as close as
     * possible to the given width.
     */
    fun constrainWidth(width: Double = Double.POSITIVE_INFINITY): Double {
        assert(debugAssertIsValid())
        return width.clamp(minWidth, maxWidth)
    }

    /**
     * Returns the height that both satisfies the constraints and is as close as
     * possible to the given height.
     */
    fun constrainHeight(height: Double = Double.POSITIVE_INFINITY): Double {
        assert(debugAssertIsValid())
        return height.clamp(minHeight, maxHeight)
    }

    private fun _debugPropagateDebugSize(size: Size, result: Size): Size {
        var finalResult = result
        assert {
            if (size is _DebugSize)
                finalResult = _DebugSize(result, size._owner, size._canBeUsedByParent)
            true
        }
        return finalResult
    }

    /**
     * Returns the size that both satisfies the constraints and is as close as
     * possible to the given size.
     *
     * See also [constrainDimensions], which applies the same algorithm to
     * separately provided widths and heights.
     */
    fun constrain(size: Size): Size {
        var result = Size(constrainWidth(size.width), constrainHeight(size.height))
        assert {
            result = _debugPropagateDebugSize(size, result)
            true
        }
        return result
    }

    /**
     * Returns the size that both satisfies the constraints and is as close as
     * possible to the given width and height.
     *
     * When you already have a [Size], prefer [constrain], which applies the same
     * algorithm to a [Size] directly.
     */
    fun constrainDimensions(width: Double, height: Double): Size {
        return Size(constrainWidth(width), constrainHeight(height))
    }

    /**
     * Returns a size that attempts to meet the following conditions, in order:
     *
     *  * The size must satisfy these constraints.
     *  * The aspect ratio of the returned size matches the aspect ratio of the
     *    given size.
     *  * The returned size as big as possible while still being equal to or
     *    smaller than the given size.
     */
    fun constrainSizeAndAttemptToPreserveAspectRatio(size: Size): Size {
        if (isTight) {
            var result = smallest
            assert { result = _debugPropagateDebugSize(size, result); true; }
            return result
        }

        var width = size.width
        var height = size.height
        assert(width > 0.0)
        assert(height > 0.0)
        val aspectRatio = width / height

        if (width > maxWidth) {
            width = maxWidth
            height = width / aspectRatio
        }

        if (height > maxHeight) {
            height = maxHeight
            width = height * aspectRatio
        }

        if (width < minWidth) {
            width = minWidth
            height = width / aspectRatio
        }

        if (height < minHeight) {
            height = minHeight
            width = height * aspectRatio
        }

        var result = Size(constrainWidth(width), constrainHeight(height))
        assert { result = _debugPropagateDebugSize(size, result); true; }
        return result
    }

    /**
     * The biggest size that satisfies the constraints.
     */
    val biggest get() = Size(constrainWidth(), constrainHeight())

    /**
     * The smallest size that satisfies the constraints.
     */
    val smallest get() = Size(constrainWidth(0.0), constrainHeight(0.0))

    /**
     * Whether there is exactly one width value that satisfies the constraints.
     */
    val hasTightWidth get() = minWidth >= maxWidth

    /**
     * Whether there is exactly one height value that satisfies the constraints.
     */
    val hasTightHeight get() = minHeight >= maxHeight

    /**
     * Whether there is exactly one size that satisfies the constraints.
     */
    override val isTight: Boolean
        get() = hasTightWidth && hasTightHeight

    /**
     * Whether there is an upper bound on the maximum width.
     */
    val hasBoundedWidth get() = maxWidth < Double.POSITIVE_INFINITY

    /**
     * Whether there is an upper bound on the maximum height.
     */
    val hasBoundedHeight get() = maxHeight < Double.POSITIVE_INFINITY

    /**
     * Whether the given size satisfies the constraints.
     */
    fun isSatisfiedBy(size: Size): Boolean {
        assert(debugAssertIsValid())
        return (minWidth <= size.width) && (size.width <= maxWidth) &&
                (minHeight <= size.height) && (size.height <= maxHeight)
    }

    /**
     * Scales each constraint parameter by the given factor.
     */
    operator fun times(factor: Double) = BoxConstraints(
            minWidth = minWidth * factor,
            maxWidth = maxWidth * factor,
            minHeight = minHeight * factor,
            maxHeight = maxHeight * factor
    )

    /**
     * Scales each constraint parameter by the inverse of the given factor.
     */
    operator fun div(factor: Double) = BoxConstraints(
            minWidth = minWidth / factor,
            maxWidth = maxWidth / factor,
            minHeight = minHeight / factor,
            maxHeight = maxHeight / factor
    )

    /**
     * Scales each constraint parameter by the inverse of the given factor, rounded to the nearest integer.
     */
    // TODO(Migration/Andrey): Original operator ~/ could not be overriden in Kotlin
    fun truncDiv(factor: Double) = BoxConstraints(
            minWidth = minWidth.truncDiv(factor).toDouble(),
            maxWidth = maxWidth.truncDiv(factor).toDouble(),
            minHeight = minHeight.truncDiv(factor).toDouble(),
            maxHeight = maxHeight.truncDiv(factor).toDouble()
    )

    /**
     * Computes the remainder of each constraint parameter by the given value.
     */
    operator fun rem(value: Double) = BoxConstraints(
            minWidth = minWidth % value,
            maxWidth = maxWidth % value,
            minHeight = minHeight % value,
            maxHeight = maxHeight % value
    )

    /**
     * Returns whether the object's constraints are normalized.
     * Constraints are normalized if the minimums are less than or
     * equal to the corresponding maximums.
     *
     * For example, a BoxConstraints object with a minWidth of 100.0
     * and a maxWidth of 90.0 is not normalized.
     *
     * Most of the APIs on BoxConstraints expect the constraints to be
     * normalized and have undefined behavior when they are not. In
     * checked mode, many of these APIs will assert if the constraints
     * are not normalized.
     */
    override val isNormalized: Boolean
        get() = minWidth >= 0.0 &&
                minWidth <= maxWidth &&
                minHeight >= 0.0 &&
                minHeight <= maxHeight

    override fun debugAssertIsValid(
        isAppliedConstraint: Boolean,
        informationCollector: InformationCollector?
    ): Boolean {
        assert {
            val throwError: (String) -> Unit = { message ->
                val information = StringBuffer()
                if (informationCollector != null)
                    informationCollector(information)
                throw FlutterError(
                        "$message\n${information}The offending constraints were:\n  $this")
            }
            if (minWidth.isNaN() || maxWidth.isNaN() || minHeight.isNaN() || maxHeight.isNaN()) {
                val affectedFieldsList = mutableListOf<String>()
                if (minWidth.isNaN())
                    affectedFieldsList.add("minWidth")
                if (maxWidth.isNaN())
                    affectedFieldsList.add("maxWidth")
                if (minHeight.isNaN())
                    affectedFieldsList.add("minHeight")
                if (maxHeight.isNaN())
                    affectedFieldsList.add("maxHeight")
                assert(affectedFieldsList.isNotEmpty())
                if (affectedFieldsList.size > 1)
                    affectedFieldsList.add(
                            "and ${affectedFieldsList.removeAt(affectedFieldsList.lastIndex)}")
                val whichFields: String
                if (affectedFieldsList.size > 2) {
                    whichFields = affectedFieldsList.joinToString(", ")
                } else if (affectedFieldsList.size == 2) {
                    whichFields = affectedFieldsList.joinToString(" ")
                } else {
                    whichFields = affectedFieldsList.single()
                }
                throwError(
                        "BoxConstraints has ${(if (affectedFieldsList.size == 1) " a NaN value "
                        else " NaN values")} in $whichFields.")
            }
            if (minWidth < 0.0 && minHeight < 0.0)
                throwError("BoxConstraints has both a negative minimum width " +
                        "and a negative minimum height.")
            if (minWidth < 0.0)
                throwError("BoxConstraints has a negative minimum width.")
            if (minHeight < 0.0)
                throwError("BoxConstraints has a negative minimum height.")
            if (maxWidth < minWidth && maxHeight < minHeight)
                throwError("BoxConstraints has both width and height constraints non-normalized.")
            if (maxWidth < minWidth)
                throwError("BoxConstraints has non-normalized width constraints.")
            if (maxHeight < minHeight)
                throwError("BoxConstraints has non-normalized height constraints.")
            if (isAppliedConstraint) {
                if (minWidth.isInfinite() && minHeight.isInfinite())
                    throwError("BoxConstraints forces an infinite width and infinite height.")
                if (minWidth.isInfinite())
                    throwError("BoxConstraints forces an infinite width.")
                if (minHeight.isInfinite())
                    throwError("BoxConstraints forces an infinite height.")
            }
            assert(isNormalized)
            true
        }
        return isNormalized
    }

    /**
     * Returns a box constraints that [isNormalized].
     *
     * The returned [maxWidth] is at least as large as the [minWidth]. Similarly,
     * the returned [maxHeight] is at least as large as the [minHeight].
     */
    fun normalize(): BoxConstraints {
        if (isNormalized)
            return this
        val minWidth = if (this.minWidth >= 0.0) this.minWidth else 0.0
        val minHeight = if (this.minHeight >= 0.0) this.minHeight else 0.0
        return BoxConstraints(
                minWidth = minWidth,
                maxWidth = if (minWidth > maxWidth) minWidth else maxWidth,
                minHeight = minHeight,
                maxHeight = if (minHeight > maxHeight) minHeight else maxHeight
        )
    }

    // (Migration:Andrey) It's data class now, no need for equals and hashcode
//    @override
//    bool operator ==(dynamic other) {
//        assert(debugAssertIsValid());
//        if (identical(this, other))
//            return true;
//        if (other is! BoxConstraints)
//        return false;
//        final BoxConstraints typedOther = other;
//        assert(typedOther.debugAssertIsValid());
//        return minWidth == typedOther.minWidth &&
//                maxWidth == typedOther.maxWidth &&
//                minHeight == typedOther.minHeight &&
//                maxHeight == typedOther.maxHeight;
//    }
//
//    @override
//    int get hashCode {
//        assert(debugAssertIsValid());
//        return hashValues(minWidth, maxWidth, minHeight, maxHeight);
//    }

    override fun toString(): String {
        val annotation = if (isNormalized) "" else "; NOT NORMALIZED"
        if (minWidth == Double.POSITIVE_INFINITY && minHeight == Double.POSITIVE_INFINITY)
            return "BoxConstraints(biggest$annotation)"
        if (minWidth == 0.0 && maxWidth == Double.POSITIVE_INFINITY &&
                minHeight == 0.0 && maxHeight == Double.POSITIVE_INFINITY)
            return "BoxConstraints(unconstrained$annotation)"
        val describe: (Double, Double, String) -> String = { min, max, dim ->
            if (min == max)
                "$dim=${min.toStringAsFixed(1)}"
            else
                "${min.toStringAsFixed(1)}<=$dim<=${max.toStringAsFixed(1)}"
        }
        val width = describe(minWidth, maxWidth, "w")
        val height = describe(minHeight, maxHeight, "h")
        return "BoxConstraints($width, $height$annotation)"
    }
}