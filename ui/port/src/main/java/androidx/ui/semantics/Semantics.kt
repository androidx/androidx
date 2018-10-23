package androidx.ui.semantics

import androidx.ui.engine.text.TextDirection
import androidx.ui.services.text_editing.TextSelection

/**
 * Signature for a function that is called for each [SemanticsNode].
 *
 * Return false to stop visiting nodes.
 *
 * Used by [SemanticsNode.visitChildren].
 */
typealias SemanticsNodeVisitor = (node: SemanticsNode) -> Boolean

/**
 * Signature for [SemanticsAction]s that move the cursor.
 *
 * If `extendSelection` is set to true the cursor movement should extend the
 * current selection or (if nothing is currently selected) start a selection.
 */
typealias MoveCursorHandler = (extendSelection: Boolean) -> Unit

/**
 * Signature for the [SemanticsAction.setSelection] handlers to change the
 * text selection (or re-position the cursor) to `selection`.
 */
typealias SetSelectionHandler = (selection: TextSelection) -> Unit

typealias _SemanticsActionHandler = (args: Any?) -> Unit

fun _concatStrings(
    thisString: String,
    otherString: String,
    thisTextDirection: TextDirection?,
    otherTextDirection: TextDirection?
): String {
    if (otherString.isEmpty())
        return thisString
    var nestedLabel = otherString
    if (thisTextDirection != otherTextDirection && otherTextDirection != null) {
        nestedLabel = when (otherTextDirection) {
            TextDirection.RTL -> "${Unicode.RLE}$nestedLabel${Unicode.PDF}"
            TextDirection.LTR -> "${Unicode.LRE}$nestedLabel${Unicode.PDF}"
        }
    }
    if (thisString.isEmpty())
        return nestedLabel
    return "$thisString\n$nestedLabel"
}
