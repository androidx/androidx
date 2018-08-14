package androidx.ui.semantics

// / Used by [debugDumpSemanticsTree] to specify the order in which child nodes
// / are printed.
enum class DebugSemanticsDumpOrder {
    // / Print nodes in inverse hit test order.
    // /
    // / In inverse hit test order, the last child of a [SemanticsNode] will be
    // / asked first if it wants to respond to a user's interaction, followed by
    // / the second last, etc. until a taker is found.
    inverseHitTest,

    // / Print nodes in semantic traversal order.
    // /
    // / This is the order in which a user would navigate the UI using the "next"
    // / and "previous" gestures.
    traversalOrder,
}