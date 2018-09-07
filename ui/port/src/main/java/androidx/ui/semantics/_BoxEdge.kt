package androidx.ui.semantics

import kotlin.math.sign

// / An edge of a box, such as top, bottom, left or right, used to compute
// / [SemanticsNode]s that overlap vertically or horizontally.
// /
// / For computing horizontal overlap in an LTR setting we create two [_BoxEdge]
// / objects for each [SemanticsNode]: one representing the left edge (marked
// / with [isLeadingEdge] equal to true) and one for the right edge (with [isLeadingEdge]
// / equal to false). Similarly, for vertical overlap we also create two objects
// / for each [SemanticsNode], one for the top and one for the bottom edge.
internal data class _BoxEdge(
    // / True if the edge comes before the seconds edge along the traversal
    // / direction, and false otherwise.
    // /
    // / This field is never null.
    // /
    // / For example, in LTR traversal the left edge's [isLeadingEdge] is set to true,
    // / the right edge's [isLeadingEdge] is set to false. When considering vertical
    // / ordering of boxes, the top edge is the start edge, and the bottom edge is
    // / the end edge.
    val isLeadingEdge: Boolean,

    // / The offset from the start edge of the parent [SemanticsNode] in the
    // / direction of the traversal.
    val offset: Double,

    // / The node to whom this edge belongs.
    val node: SemanticsNode
) : Comparable<_BoxEdge> {

    init {
        assert(isLeadingEdge != null)
        assert(offset != null)
        assert(node != null)
    }

    override fun compareTo(other: _BoxEdge): Int {
        return (offset - other.offset).sign.toInt()
    }
}