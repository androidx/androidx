package androidx.ui.semantics

import androidx.ui.runtimeType

// / A tag for a [SemanticsNode].
// /
// / Tags can be interpreted by the parent of a [SemanticsNode]
// / and depending on the presence of a tag the parent can for example decide
// / how to add the tagged node as a child. Tags are not sent to the engine.
// /
// / As an example, the [RenderSemanticsGestureHandler] uses tags to determine
// / if a child node should be excluded from the scrollable area for semantic
// / purposes.
// /
// / The provided [name] is only used for debugging. Two tags created with the
// / same [name] and the `new` operator are not considered identical. However,
// / two tags created with the same [name] and the `const` operator are always
// / identical.
class SemanticsTag(
    // / A human-readable name for this tag used for debugging.
    // /
    // / This string is not used to determine if two tags are identical.

    val name: String
) {
    override fun toString(): String {
        // TODO(ryanmentley): Is using runtime types like this a good idea with ProGuard?
        return runtimeType().toString() + "($name)"
    }
}