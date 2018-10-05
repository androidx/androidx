package androidx.ui.engine.text

// / Layout constraints for [Paragraph] objects.
// /
// / Instances of this class are typically used with [Paragraph.layout].
// /
// / The only constraint that can be specified is the [width]. See the discussion
// / at [width] for more details.
// /
// / Creates constraints for laying out a paragraph.
// /
// / The [width] argument must not be null.
// /
// / The width the paragraph should use whey computing the positions of glyphs.
// /
// / If possible, the paragraph will select a soft line break prior to reaching
// / this width. If no soft line break is available, the paragraph will select
// / a hard line break prior to reaching this width. If that would force a line
// / break without any characters having been placed (i.e. if the next
// / character to be laid out does not fit within the given width constraint)
// / then the next character is allowed to overflow the width constraint and a
// / forced line break is placed after it (even if an explicit line break
// / follows).
// /
// / The width influences how ellipses are applied. See the discussion at [new
// / ParagraphStyle] for more details.
// /
// / This width is also used to position glyphs according to the [TextAlign]
// / alignment described in the [ParagraphStyle] used when building the
// / [Paragraph] with a [ParagraphBuilder].
data class ParagraphConstraints(val width: Double) {
    override fun toString(): String {
        return "ParagraphConstraints(width: $width)"
    }
}

// TODO(Migration/siyamed): removed since changed it to a data class.
// @override
// bool operator ==(dynamic other) {
//     if (other.runtimeType != runtimeType)
//         return false;
//     final ParagraphConstraints typedOther = other;
//     return typedOther.width == width;
// }
//
// @override
// int get hashCode => width.hashCode;