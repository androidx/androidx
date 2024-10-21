/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.foundation.text.input.internal.undo

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setSelectionCoerced
import androidx.compose.foundation.text.timeNowMillis
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange

/**
 * An undo identifier designed for text editors. Defines a single atomic change that can be applied
 * directly or in reverse to modify the contents of a text editor.
 *
 * @param index Start point of [preText] and [postText].
 * @param preText Previously written text that's deleted starting from [index].
 * @param postText New text that's inserted at [index]
 * @param preSelection Previous selection before changes are applied
 * @param postSelection New selection after changes are applied
 * @param timeInMillis When did this change was first committed
 * @param canMerge Whether this change can be merged with the next or previous change in an undo
 *   stack. There are many other rules that affect the merging strategy between two
 *   [TextUndoOperation]s but this flag is a sure way to force a non-mergeable property.
 */
internal class TextUndoOperation(
    val index: Int,
    val preText: String,
    val postText: String,
    val preSelection: TextRange,
    val postSelection: TextRange,
    val timeInMillis: Long = timeNowMillis(),
    val canMerge: Boolean = true
) {

    /**
     * What kind of edit operation is defined by this change. Edit type is decided by forward the
     * behavior of this change in forward direction (pre -> post).
     */
    val textEditType: TextEditType =
        when {
            preText.isEmpty() && postText.isEmpty() ->
                throw IllegalArgumentException("Either pre or post text must not be empty")
            preText.isEmpty() && postText.isNotEmpty() -> TextEditType.Insert
            preText.isNotEmpty() && postText.isEmpty() -> TextEditType.Delete
            else -> TextEditType.Replace
        }

    /** Only required while deciding whether to merge two deletion type undo operations. */
    val deletionType: TextDeleteType
        get() {
            if (textEditType != TextEditType.Delete) return TextDeleteType.NotByUser
            if (!postSelection.collapsed) return TextDeleteType.NotByUser
            if (preSelection.collapsed) {
                return if (preSelection.start > postSelection.start) {
                    TextDeleteType.Start
                } else {
                    TextDeleteType.End
                }
            } else if (preSelection.start == postSelection.start && preSelection.start == index) {
                return TextDeleteType.Inner
            }
            return TextDeleteType.NotByUser
        }

    companion object {

        val Saver =
            object : Saver<TextUndoOperation, Any> {
                override fun SaverScope.save(value: TextUndoOperation): Any =
                    listOf(
                        value.index,
                        value.preText,
                        value.postText,
                        value.preSelection.start,
                        value.preSelection.end,
                        value.postSelection.start,
                        value.postSelection.end,
                        value.timeInMillis
                    )

                override fun restore(value: Any): TextUndoOperation {
                    return with((value as List<*>)) {
                        TextUndoOperation(
                            index = get(0) as Int,
                            preText = get(1) as String,
                            postText = get(2) as String,
                            preSelection = TextRange(get(3) as Int, get(4) as Int),
                            postSelection = TextRange(get(5) as Int, get(6) as Int),
                            timeInMillis = get(7) as Long,
                        )
                    }
                }
            }
    }
}

/** Apply a given [TextUndoOperation] in reverse to undo this [TextFieldState]. */
internal fun TextFieldState.undo(op: TextUndoOperation) {
    editWithNoSideEffects {
        replace(op.index, op.index + op.postText.length, op.preText)
        setSelectionCoerced(op.preSelection.start, op.preSelection.end)
    }
}

/** Apply a given [TextUndoOperation] in forward direction to redo this [TextFieldState]. */
internal fun TextFieldState.redo(op: TextUndoOperation) {
    editWithNoSideEffects {
        replace(op.index, op.index + op.preText.length, op.postText)
        setSelectionCoerced(op.postSelection.start, op.postSelection.end)
    }
}

/**
 * Possible types of a text operation.
 * 1. Insert; if the edited range has 0 length, and the new text is longer than 0 length
 * 2. Delete: if the edited range is longer than 0, and the new text has 0 length
 * 3. Replace: All other changes.
 */
internal enum class TextEditType {
    Insert,
    Delete,
    Replace
}

/**
 * When a delete occurs during text editing, it can happen in various shapes.
 * 1. Start; When a single character is removed to the start (towards 0) of the cursor, backspace
 *    key behavior. "abcd|efg" -> "abc|efg"
 * 2. End; When a single character is removed to the end (towards length) of the cursor, delete key
 *    behavior. "abcd|efg" -> "abcd|fg"
 * 3. Inner; When a selection of characters are removed, directionless. Both backspace and delete
 *    express the same behavior in this case. "ab|cde|fg" -> "ab|fg"
 * 4. NotByUser; A text editing operation that cannot be executed via a hardware or software
 *    keyboard. For example when a portion of text is removed but it's not next to a cursor or
 *    selection, or selection remains after removal. "abcd|efg" -> "bcd|efg" "abc|def|g" -> "a|bc|g"
 */
internal enum class TextDeleteType {
    Start,
    End,
    Inner,
    NotByUser
}

/**
 * There are multiple strategies while deciding how to add certain edit operations to undo stack.
 * - Normally, merge is decided by UndoOperation's own merge logic, comparing itself to the latest
 *   operation in the Undo stack.
 * - Programmatic updates should clear the history completely.
 * - Some atomic actions like cut, and paste shouldn't merge to previous or next actions.
 */
internal enum class TextFieldEditUndoBehavior {
    MergeIfPossible,
    ClearHistory,
    NeverMerge
}
