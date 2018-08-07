package androidx.ui.rendering.box

import androidx.ui.clamp
import androidx.ui.engine.geometry.Size
import androidx.ui.rendering.obj.Constraints

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
 * * 0.0 <= [minWidth] <= [maxWidth] <= [double.infinity]
 * * 0.0 <= [minHeight] <= [maxHeight] <= [double.infinity]
 *
 * [double.infinity] is a legal value for each constraint.
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
// TODO(Migration/xbhatnag): const constructor
data class BoxConstraints(
    // The minimum width that satisfies the constraints.
    val minWidth: Double = 0.0,
    // The maximum width that satisfies the constraints.
    // Might be [double.infinity].
    val maxWidth: Double = Double.POSITIVE_INFINITY,
    // The minimum height that satisfies the constraints.
    val minHeight: Double = 0.0,
    // The maximum height that satisfies the constraints.
    // Might be [double.infinity].
    val maxHeight: Double = Double.POSITIVE_INFINITY
) : Constraints() {

//    /// Creates box constraints that is respected only by the given size.
//    BoxConstraints.tight(Size size)
//    : minWidth = size.width,
//    maxWidth = size.width,
//    minHeight = size.height,
//    maxHeight = size.height;
//
//    /// Creates box constraints that require the given width or height.
//    ///
//    /// See also:
//    ///
//    ///  * [new BoxConstraints.tightForFinite], which is similar but instead of
//    ///    being tight if the value is non-null, is tight if the value is not
//    ///    infinite.
//    const BoxConstraints.tightFor({
//        double width,
//        double height
//    }): minWidth = width != null ? width : 0.0,
//    maxWidth = width != null ? width : double.infinity,
//    minHeight = height != null ? height : 0.0,
//    maxHeight = height != null ? height : double.infinity;
//
//    /// Creates box constraints that require the given width or height, except if
//    /// they are infinite.
//    ///
//    /// See also:
//    ///
//    ///  * [new BoxConstraints.tightFor], which is similar but instead of being
//    ///    tight if the value is not infinite, is tight if the value is non-null.
//    const BoxConstraints.tightForFinite({
//        double width: double.infinity,
//        double height: double.infinity
//    }): minWidth = width != double.infinity ? width : 0.0,
//    maxWidth = width != double.infinity ? width : double.infinity,
//    minHeight = height != double.infinity ? height : 0.0,
//    maxHeight = height != double.infinity ? height : double.infinity;
//
//    /// Creates box constraints that forbid sizes larger than the given size.
//    BoxConstraints.loose(Size size)
//    : minWidth = 0.0,
//    maxWidth = size.width,
//    minHeight = 0.0,
//    maxHeight = size.height;
//
//    /// Creates box constraints that expand to fill another box constraints.
//    ///
//    /// If width or height is given, the constraints will require exactly the
//    /// given value in the given dimension.
//    const BoxConstraints.expand({
//        double width,
//        double height
//    }): minWidth = width != null ? width : double.infinity,
//    maxWidth = width != null ? width : double.infinity,
//    minHeight = height != null ? height : double.infinity,
//    maxHeight = height != null ? height : double.infinity;
//
//    /// Creates a copy of this box constraints but with the given fields replaced with the new values.
//    BoxConstraints copyWith({
//        double minWidth,
//        double maxWidth,
//        double minHeight,
//        double maxHeight
//    }) {
//        return new BoxConstraints(
//                minWidth: minWidth ?? this.minWidth,
//        maxWidth: maxWidth ?? this.maxWidth,
//        minHeight: minHeight ?? this.minHeight,
//        maxHeight: maxHeight ?? this.maxHeight
//        );
//    }
//
//    /// Returns new box constraints that are smaller by the given edge dimensions.
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
//
//    /// Returns new box constraints that remove the minimum width and height requirements.
//    BoxConstraints loosen() {
//        assert(debugAssertIsValid());
//        return new BoxConstraints(
//                minWidth: 0.0,
//        maxWidth: maxWidth,
//        minHeight: 0.0,
//        maxHeight: maxHeight
//        );
//    }
//
//    /// Returns new box constraints that respect the given constraints while being
//    /// as close as possible to the original constraints.
//    BoxConstraints enforce(BoxConstraints constraints) {
//        return new BoxConstraints(
//                minWidth: minWidth.clamp(constraints.minWidth, constraints.maxWidth),
//        maxWidth: maxWidth.clamp(constraints.minWidth, constraints.maxWidth),
//        minHeight: minHeight.clamp(constraints.minHeight, constraints.maxHeight),
//        maxHeight: maxHeight.clamp(constraints.minHeight, constraints.maxHeight)
//        );
//    }
//
//    /// Returns new box constraints with a tight width and/or height as close to
//    /// the given width and height as possible while still respecting the original
//    /// box constraints.
//    BoxConstraints tighten({ double width, double height }) {
//        return new BoxConstraints(minWidth: width == null ? minWidth : width.clamp(minWidth, maxWidth),
//        maxWidth: width == null ? maxWidth : width.clamp(minWidth, maxWidth),
//        minHeight: height == null ? minHeight : height.clamp(minHeight, maxHeight),
//        maxHeight: height == null ? maxHeight : height.clamp(minHeight, maxHeight));
//    }
//
//    /// A box constraints with the width and height constraints flipped.
//    BoxConstraints get flipped {
//        return new BoxConstraints(
//                minWidth: minHeight,
//        maxWidth: maxHeight,
//        minHeight: minWidth,
//        maxHeight: maxWidth
//        );
//    }
//
//    /// Returns box constraints with the same width constraints but with
//    /// unconstrained height.
//    BoxConstraints widthConstraints() => new BoxConstraints(minWidth: minWidth, maxWidth: maxWidth);
//
//    /// Returns box constraints with the same height constraints but with
//    /// unconstrained width
//    BoxConstraints heightConstraints() => new BoxConstraints(minHeight: minHeight, maxHeight: maxHeight);
//
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
//
//    Size _debugPropagateDebugSize(Size size, Size result) {
//        assert(() {
//            if (size is _DebugSize)
//                result = new _DebugSize(result, size._owner, size._canBeUsedByParent);
//            return true;
//        }());
//        return result;
//    }
//
//    /// Returns the size that both satisfies the constraints and is as close as
//    /// possible to the given size.
//    ///
//    /// See also [constrainDimensions], which applies the same algorithm to
//    /// separately provided widths and heights.
//    Size constrain(Size size) {
//        Size result = new Size(constrainWidth(size.width), constrainHeight(size.height));
//        assert(() { result = _debugPropagateDebugSize(size, result); return true; }());
//        return result;
//    }
//
//    /// Returns the size that both satisfies the constraints and is as close as
//    /// possible to the given width and height.
//    ///
//    /// When you already have a [Size], prefer [constrain], which applies the same
//    /// algorithm to a [Size] directly.
//    Size constrainDimensions(double width, double height) {
//        return new Size(constrainWidth(width), constrainHeight(height));
//    }
//
//    /// Returns a size that attempts to meet the following conditions, in order:
//    ///
//    ///  * The size must satisfy these constraints.
//    ///  * The aspect ratio of the returned size matches the aspect ratio of the
//    ///    given size.
//    ///  * The returned size as big as possible while still being equal to or
//    ///    smaller than the given size.
//    Size constrainSizeAndAttemptToPreserveAspectRatio(Size size) {
//        if (isTight) {
//            Size result = smallest;
//            assert(() { result = _debugPropagateDebugSize(size, result); return true; }());
//            return result;
//        }
//
//        double width = size.width;
//        double height = size.height;
//        assert(width > 0.0);
//        assert(height > 0.0);
//        final double aspectRatio = width / height;
//
//        if (width > maxWidth) {
//            width = maxWidth;
//            height = width / aspectRatio;
//        }
//
//        if (height > maxHeight) {
//            height = maxHeight;
//            width = height * aspectRatio;
//        }
//
//        if (width < minWidth) {
//            width = minWidth;
//            height = width / aspectRatio;
//        }
//
//        if (height < minHeight) {
//            height = minHeight;
//            width = height * aspectRatio;
//        }
//
//        Size result = new Size(constrainWidth(width), constrainHeight(height));
//        assert(() { result = _debugPropagateDebugSize(size, result); return true; }());
//        return result;
//    }
//
//    /// The biggest size that satisfies the constraints.
//    Size get biggest => new Size(constrainWidth(), constrainHeight());
//
    // The smallest size that satisfies the constraints.
    val smallest get() = Size(constrainWidth(0.0), constrainHeight(0.0))

    // Whether there is exactly one width value that satisfies the constraints.
    val hasTightWidth get() = minWidth >= maxWidth

    // Whether there is exactly one height value that satisfies the constraints.
    val hasTightHeight get() = minHeight >= maxHeight

    // Whether there is exactly one size that satisfies the constraints.
    override val isTight: Boolean
        get() = hasTightWidth && hasTightHeight

    // Whether there is an upper bound on the maximum width.
    val hasBoundedWidth get() = maxWidth < Double.POSITIVE_INFINITY

    // Whether there is an upper bound on the maximum height.
    val hasBoundedHeight get() = maxHeight < Double.POSITIVE_INFINITY

    // Whether the given size satisfies the constraints.
    fun isSatisfiedBy(size: Size): Boolean {
        assert(debugAssertIsValid())
        return (minWidth <= size.width) && (size.width <= maxWidth) &&
                (minHeight <= size.height) && (size.height <= maxHeight)
    }
//
//    /// Scales each constraint parameter by the given factor.
//    BoxConstraints operator*(double factor) {
//        return new BoxConstraints(
//                minWidth: minWidth * factor,
//        maxWidth: maxWidth * factor,
//        minHeight: minHeight * factor,
//        maxHeight: maxHeight * factor
//        );
//    }
//
//    /// Scales each constraint parameter by the inverse of the given factor.
//    BoxConstraints operator/(double factor) {
//        return new BoxConstraints(
//                minWidth: minWidth / factor,
//        maxWidth: maxWidth / factor,
//        minHeight: minHeight / factor,
//        maxHeight: maxHeight / factor
//        );
//    }
//
//    /// Scales each constraint parameter by the inverse of the given factor, rounded to the nearest integer.
//    BoxConstraints operator~/(double factor) {
//        return new BoxConstraints(
//                minWidth: (minWidth ~/ factor).toDouble(),
//        maxWidth: (maxWidth ~/ factor).toDouble(),
//        minHeight: (minHeight ~/ factor).toDouble(),
//        maxHeight: (maxHeight ~/ factor).toDouble()
//        );
//    }
//
//    /// Computes the remainder of each constraint parameter by the given value.
//    BoxConstraints operator%(double value) {
//        return new BoxConstraints(
//                minWidth: minWidth % value,
//        maxWidth: maxWidth % value,
//        minHeight: minHeight % value,
//        maxHeight: maxHeight % value
//        );
//    }
//
//    /// Linearly interpolate between two BoxConstraints.
//    ///
//    /// If either is null, this function interpolates from a [BoxConstraints]
//    /// object whose fields are all set to 0.0.
//    ///
//    /// The `t` argument represents position on the timeline, with 0.0 meaning
//    /// that the interpolation has not started, returning `a` (or something
//    /// equivalent to `a`), 1.0 meaning that the interpolation has finished,
//    /// returning `b` (or something equivalent to `b`), and values in between
//    /// meaning that the interpolation is at the relevant point on the timeline
//    /// between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
//    /// 1.0, so negative values and values greater than 1.0 are valid (and can
//    /// easily be generated by curves such as [Curves.elasticInOut]).
//    ///
//    /// Values for `t` are usually obtained from an [Animation<double>], such as
//    /// an [AnimationController].
//    static BoxConstraints lerp(BoxConstraints a, BoxConstraints b, double t) {
//        assert(t != null);
//        if (a == null && b == null)
//            return null;
//        if (a == null)
//            return b * t;
//        if (b == null)
//            return a * (1.0 - t);
//        assert(a.debugAssertIsValid());
//        assert(b.debugAssertIsValid());
//        assert((a.minWidth.isFinite && b.minWidth.isFinite) || (a.minWidth == double.infinity && b.minWidth == double.infinity), 'Cannot interpolate between finite constraints and unbounded constraints.');
//        assert((a.maxWidth.isFinite && b.maxWidth.isFinite) || (a.maxWidth == double.infinity && b.maxWidth == double.infinity), 'Cannot interpolate between finite constraints and unbounded constraints.');
//        assert((a.minHeight.isFinite && b.minHeight.isFinite) || (a.minHeight == double.infinity && b.minHeight == double.infinity), 'Cannot interpolate between finite constraints and unbounded constraints.');
//        assert((a.maxHeight.isFinite && b.maxHeight.isFinite) || (a.maxHeight == double.infinity && b.maxHeight == double.infinity), 'Cannot interpolate between finite constraints and unbounded constraints.');
//        return new BoxConstraints(
//                minWidth: a.minWidth.isFinite ? ui.lerpDouble(a.minWidth, b.minWidth, t) : double.infinity,
//        maxWidth: a.maxWidth.isFinite ? ui.lerpDouble(a.maxWidth, b.maxWidth, t) : double.infinity,
//        minHeight: a.minHeight.isFinite ? ui.lerpDouble(a.minHeight, b.minHeight, t) : double.infinity,
//        maxHeight: a.maxHeight.isFinite ? ui.lerpDouble(a.maxHeight, b.maxHeight, t) : double.infinity,
//        );
//    }

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

//    @override
//    bool debugAssertIsValid({
//        bool isAppliedConstraint: false,
//        InformationCollector informationCollector,
//    }) {
//        assert(() {
//            void throwError(String message) {
//                final StringBuffer information = new StringBuffer();
//                if (informationCollector != null)
//                    informationCollector(information);
//                throw new FlutterError('$message\n${information}The offending constraints were:\n  $this');
//            }
//            if (minWidth.isNaN || maxWidth.isNaN || minHeight.isNaN || maxHeight.isNaN) {
//                final List<String> affectedFieldsList = <String>[];
//                if (minWidth.isNaN)
//                    affectedFieldsList.add('minWidth');
//                if (maxWidth.isNaN)
//                    affectedFieldsList.add('maxWidth');
//                if (minHeight.isNaN)
//                    affectedFieldsList.add('minHeight');
//                if (maxHeight.isNaN)
//                    affectedFieldsList.add('maxHeight');
//                assert(affectedFieldsList.isNotEmpty);
//                if (affectedFieldsList.length > 1)
//                    affectedFieldsList.add('and ${affectedFieldsList.removeLast()}');
//                String whichFields = '';
//                if (affectedFieldsList.length > 2) {
//                    whichFields = affectedFieldsList.join(', ');
//                } else if (affectedFieldsList.length == 2) {
//                    whichFields = affectedFieldsList.join(' ');
//                } else {
//                    whichFields = affectedFieldsList.single;
//                }
//                throwError('BoxConstraints has ${affectedFieldsList.length == 1 ? 'a NaN value' : 'NaN values' } in $whichFields.');
//            }
//            if (minWidth < 0.0 && minHeight < 0.0)
//                throwError('BoxConstraints has both a negative minimum width and a negative minimum height.');
//            if (minWidth < 0.0)
//                throwError('BoxConstraints has a negative minimum width.');
//            if (minHeight < 0.0)
//                throwError('BoxConstraints has a negative minimum height.');
//            if (maxWidth < minWidth && maxHeight < minHeight)
//                throwError('BoxConstraints has both width and height constraints non-normalized.');
//            if (maxWidth < minWidth)
//                throwError('BoxConstraints has non-normalized width constraints.');
//            if (maxHeight < minHeight)
//                throwError('BoxConstraints has non-normalized height constraints.');
//            if (isAppliedConstraint) {
//                if (minWidth.isInfinite && minHeight.isInfinite)
//                    throwError('BoxConstraints forces an infinite width and infinite height.');
//                if (minWidth.isInfinite)
//                    throwError('BoxConstraints forces an infinite width.');
//                if (minHeight.isInfinite)
//                    throwError('BoxConstraints forces an infinite height.');
//            }
//            assert(isNormalized);
//            return true;
//        }());
//        return isNormalized;
//    }
//
//    /// Returns a box constraints that [isNormalized].
//    ///
//    /// The returned [maxWidth] is at least as large as the [minWidth]. Similarly,
//    /// the returned [maxHeight] is at least as large as the [minHeight].
//    BoxConstraints normalize() {
//        if (isNormalized)
//            return this;
//        final double minWidth = this.minWidth >= 0.0 ? this.minWidth : 0.0;
//        final double minHeight = this.minHeight >= 0.0 ? this.minHeight : 0.0;
//        return new BoxConstraints(
//                minWidth: minWidth,
//        maxWidth: minWidth > maxWidth ? minWidth : maxWidth,
//        minHeight: minHeight,
//        maxHeight: minHeight > maxHeight ? minHeight : maxHeight
//        );
//    }
//
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
//
//    @override
//    String toString() {
//        final String annotation = isNormalized ? '' : '; NOT NORMALIZED';
//        if (minWidth == double.infinity && minHeight == double.infinity)
//            return 'BoxConstraints(biggest$annotation)';
//        if (minWidth == 0 && maxWidth == double.infinity &&
//                minHeight == 0 && maxHeight == double.infinity)
//            return 'BoxConstraints(unconstrained$annotation)';
//        String describe(double min, double max, String dim) {
//        if (min == max)
//            return '$dim=${min.toStringAsFixed(1)}';
//        return '${min.toStringAsFixed(1)}<=$dim<=${max.toStringAsFixed(1)}';
//    }
//        final String width = describe(minWidth, maxWidth, 'w');
//        final String height = describe(minHeight, maxHeight, 'h');
//        return 'BoxConstraints($width, $height$annotation)';
//    }
}