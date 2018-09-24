package androidx.ui.engine.text

import androidx.ui.engine.geometry.Rect
import androidx.ui.toStringAsFixed

/**
 * A rectangle enclosing a run of text.
 *
 * This is similar to [Rect] but includes an inherent [TextDirection].
 */
data class TextBox(
    /**
     * The left edge of the text box, irrespective of direction.
     * To get the leading edge (which may depend on the [direction]), consider [start].
     */
    val left: Double,
    /** The top edge of the text box. */
    val top: Double,
    /**
     * The right edge of the text box, irrespective of direction.
     * To get the trailing edge (which may depend on the [direction]), consider [end].
     */
    val right: Double,
    /** The bottom edge of the text box. */
    val bottom: Double,
    /** The direction in which text inside this box flows. */
    val direction: TextDirection
) {

    /** Returns a rect of the same size as this box. */
    fun toRect(): Rect {
        return Rect.fromLTRB(left, top, right, bottom)
    }

    /**
     * The [left] edge of the box for left-to-right text; the [right] edge of the box for right-to-left text.
     * See also:
     *  * [direction], which specifies the text direction.
     */
    fun start(): Double {
        return if ((direction == TextDirection.LTR)) left else right
    }

    /**
     * The [right] edge of the box for left-to-right text; the [left] edge of the box for right-to-left text.
     * See also:
     *  * [direction], which specifies the text direction.
     */
    fun end(): Double {
        return if ((direction == TextDirection.LTR)) right else left
    }

    override fun toString(): String {
        return "TextBox.fromLTRBD(${left.toStringAsFixed(1)}, ${top.toStringAsFixed(1)}, " +
            "${right.toStringAsFixed(1)}, ${bottom.toStringAsFixed(1)}, $direction)"
    }

    companion object {

        fun fromLTRBD(
            left: Double,
            top: Double,
            right: Double,
            bottom: Double,
            direction: TextDirection
        ): TextBox {
            return TextBox(left, top, right, bottom, direction)
        }
    }
}

// TODO(Migration/siyamed): removed the following since converted into data class
//    @override
//    bool operator ==(dynamic other) {
//        if (identical(this, other))
//            return true;
//        if (other.runtimeType != runtimeType)
//            return false;
//        final TextBox typedOther = other;
//        return typedOther.left == left
//                && typedOther.top == top
//                && typedOther.right == right
//                && typedOther.bottom == bottom
//                && typedOther.direction == direction;
//    }
//
//    @override
//    int get hashCode => hashValues(left, top, right, bottom, direction);
//
//    open override fun ==(other : Any) : Boolean {
//        if (identical(this, other)) {
//            return true
//        }
//        if (!(other.runtimeType == runtimeType())) {
//            return false
//        }
//        var typedOther : TextBox = other as TextBox
//        return (((((typedOther.left == left) && (typedOther.top == top)) && (typedOther.right == right)) && (typedOther.bottom == bottom)) && (typedOther.direction == direction))
//    }
//    open override fun hashCode() : Int {
//        return hashValues(left, top, right, bottom, direction)
//    }

// TODO(Migration/siyamed): I do not know what this is
//    @pragma('vm:entry-point')
//    TextBox._(
//    this.left,
//    this.top,
//    this.right,
//    this.bottom,
//    int directionIndex,
//    ) : direction = TextDirection.values[directionIndex];