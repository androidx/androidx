package androidx.ui.engine.geometry

/**
 * Base class for [Size] and [Offset], which are both ways to describe
 * a distance as a two-dimensional axis-aligned vector.
 *
 * Abstract const constructor. This constructor enables subclasses to provide
 * const constructors so that they can be used in const expressions.
 *
 * The first argument sets the horizontal component, and the second the
 * vertical component.
 */
// TODO(Migration/Filip): Made OffsetBase to be an interface so we can have ancestors as data classes.
interface OffsetBase {

    val dx: Double
    val dy: Double

    /**
     * Returns true if either component is [double.infinity], and false if both
     * are finite (or negative infinity, or NaN).
     *
     * This is different than comparing for equality with an instance that has
     * _both_ components set to [double.infinity].
     *
     * See also:
     *
     *  * [isFinite], which is true if both components are finite (and not NaN).
     */
    // TODO(Migration/Filip): Verify that this is valid in java world.
    fun isInfinite() = dx >= Double.POSITIVE_INFINITY || dy >= Double.POSITIVE_INFINITY

    /**
     * Whether both components are finite (neither infinite nor NaN).
     *
     * See also:
     *
     *  * [isInfinite], which returns true if either component is equal to
     *    positive infinity.
     */
    fun isFinite() = dx.isFinite() && dy.isFinite()

    /**
     * Less-than operator. Compares an [Offset] or [Size] to another [Offset] or
     * [Size], and returns true if both the horizontal and vertical values of the
     * left-hand-side operand are smaller than the horizontal and vertical values
     * of the right-hand-side operand respectively. Returns false otherwise.
     *
     * This is a partial ordering. It is possible for two values to be neither
     * less, nor greater than, nor equal to, another.
     */
    fun isLessThan(other: OffsetBase) = dx < other.dx && dy < other.dy

    /**
     * Less-than-or-equal-to operator. Compares an [Offset] or [Size] to another
     * [Offset] or [Size], and returns true if both the horizontal and vertical
     * values of the left-hand-side operand are smaller than or equal to the
     * horizontal and vertical values of the right-hand-side operand
     * respectively. Returns false otherwise.
     *
     * This is a partial ordering. It is possible for two values to be neither
     * less, nor greater than, nor equal to, another.
     */
    fun isLessOrEqThan(other: OffsetBase) = dx <= other.dx && dy <= other.dy

    /**
     * Greater-than operator. Compares an [Offset] or [Size] to another [Offset]
     * or [Size], and returns true if both the horizontal and vertical values of
     * the left-hand-side operand are bigger than the horizontal and vertical
     * values of the right-hand-side operand respectively. Returns false
     * otherwise.
     *
     * This is a partial ordering. It is possible for two values to be neither
     * less, nor greater than, nor equal to, another.
     */
    fun isGreaterThan(other: OffsetBase) = dx > other.dx && dy > other.dy

    /**
     * Greater-than-or-equal-to operator. Compares an [Offset] or [Size] to
     * another [Offset] or [Size], and returns true if both the horizontal and
     * vertical values of the left-hand-side operand are bigger than or equal to
     * the horizontal and vertical values of the right-hand-side operand
     * respectively. Returns false otherwise.
     *
     * This is a partial ordering. It is possible for two values to be neither
     * less, nor greater than, nor equal to, another.
     */
    fun isGreaterOrEqThan(other: OffsetBase) = dx > other.dx && dy >= other.dy

// TODO(Migration/Filip): Since all ancestors are data classes this should not be needed
//    override fun equals(other: Any?): Boolean {
//        if (other !is OffsetBase) {
//            return false
//        }
//        return dx == other.dx && dy == other.dy;
//    }

// @override
// int get hashCode => hashValues(_dx, _dy);

//    override fun toString() = "$runtimeType(${dx?.toStringAsFixed(1)}, ${dy?.toStringAsFixed(1)})";
}