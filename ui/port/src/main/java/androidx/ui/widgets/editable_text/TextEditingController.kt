package androidx.ui.widgets.editable_text

import androidx.ui.foundation.change_notifier.ValueNotifier
import androidx.ui.services.text_editing.TextRange
import androidx.ui.services.text_editing.TextSelection
import androidx.ui.services.text_input.TextEditingValue

/**
 * A controller for an editable text field.
 *
 * Whenever the user modifies a text field with an associated [TextEditingController], the text
 * field updates [value] and the controller notifies its listeners. Listeners can then read the
 * [text] and [selection] properties to learn what the user has typed or how the selection has been
 * updated.
 *
 * Similarly, if you modify the [text] or [selection] properties, the text field will be notified
 * and will update itself appropriately.
 *
 * A [TextEditingController] can also be used to provide an initial value for a text field. If you
 * build a text field with a controller that already has [text], the text field will use that text
 * as its initial value.

 * See also:
 *
 * * [TextField], which is a Material Design text field that can be controlled with a
 *   [TextEditingController].
 * * [EditableText], which is a raw region of editable text that can be controlled with a
 *   [TextEditingController].
 */
class TextEditingController : ValueNotifier<TextEditingValue> {
    /**
     * Creates a controller for an editable text field.
     *
     * This constructor treats a null [text] argument as if it were the empty string.
     */
    constructor(text: String?)
            : super(if (text == null) TextEditingValue.empty else TextEditingValue(text))

    /**
     * Creates a controller for an editable text field from an initial [TextEditingValue].
     *
     * This constructor treats a null [value] argument as if it were [TextEditingValue.empty].
     */
    constructor(value: TextEditingValue?)
            : super(if (value == null) TextEditingValue.empty else value)

    /** The current string the user is editing. */
    var text: String
        get() = value.text
        /**
         * Setting this will notify all the listeners of this [TextEditingController] that they need
         * to update (it calls [notifyListeners]). For this reason, this value should only be set
         * between frames, e.g. in response to user actions, not during the build, layout, or paint
         * phases.
         */
        set(newText: String) {
            value = value.copy(text = newText,
                    selection = TextSelection.collapsed(offset = -1),
                    composing = TextRange.empty)
        }

    /** The currently selected [text]. */
    var selection: TextSelection
        /**
         * If the selection is collapsed, then this property gives the offset of the cursor within
         * the text.
         */
        get() = value.selection
        /**
         * Setting this will notify all the listeners of this [TextEditingController] that they need
         * to update (it calls [notifyListeners]). For this reason, this value should only be set
         * between frames, e.g. in response to user actions, not during the build, layout, or paint
         * phases.
         */
        set(newSelection) {
            assert(newSelection.start <= text.length || newSelection.end <= text.length) {
                "invalid text selection: $newSelection"
            }
            value = value.copy(selection = newSelection, composing = TextRange.empty)
        }

    /**
     * Set the [value] to empty.
     *
     * After calling this function, [text] will be the empty string and the selection will be
     * invalid.
     *
     * Calling this will notify all the listeners of this [TextEditingController] that they need to
     * update (it calls [notifyListeners]). For this reason, this method should only be called
     * between frames, e.g. in response to user actions, not during the build, layout, or paint
     * phases.
     */
    fun clear() {
        value = TextEditingValue.empty
    }

    /**
     * Set the composing region to an empty range.
     *
     * The composing region is the range of text that is still being composed. Calling this function
     * indicates that the user is done composing that region.
     *
     * Calling this will notify all the listeners of this [TextEditingController] that they need to
     * update (it calls [notifyListeners]). For this reason, this method should only be called
     * between frames, e.g. in response to user actions, not during the build, layout, or paint
     * phases.
     */
    fun clearComposing() {
        value = value.copy(composing = TextRange.empty)
    }
}
