package androidx.ui.semantics

// /// The implementation of [Comparable] that implements the ordering of
// /// [SemanticsNode]s in the accessibility traversal.
// ///
// /// [SemanticsNode]s are sorted prior to sending them to the engine side.
// ///
// /// This implementation considers a [node]'s [sortKey] and its position within
// /// the list of its siblings. [sortKey] takes precedence over position.
internal data class _TraversalSortNode(
    /** The node whose position this sort node determines. */
    val node: SemanticsNode,

    /**
     * Determines the position of this node among its siblings.
     *
     * Sort keys take precedence over other attributes, such as
     * [position].
     */
    val sortKey: SemanticsSortKey? = null,

    /**
     * Position within the list of siblings as determined by the default sort
     * order.
     */
    val position: Int
) : Comparable<_TraversalSortNode> {

    override fun compareTo(other: _TraversalSortNode): Int {
        if (sortKey == null || other.sortKey == null) {
            return position - other.position
        }
        return sortKey.compareTo(other.sortKey)
    }

    // TODO(Migration/ryanmentley): Should these compare by values?
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }
}