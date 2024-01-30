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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.SNAPSHOTS_INTERVAL_MILLIS
import androidx.compose.foundation.text2.input.internal.undo.TextDeleteType
import androidx.compose.foundation.text2.input.internal.undo.TextEditType
import androidx.compose.foundation.text2.input.internal.undo.TextUndoOperation
import androidx.compose.foundation.text2.input.internal.undo.UndoManager
import androidx.compose.foundation.text2.input.internal.undo.redo
import androidx.compose.foundation.text2.input.internal.undo.undo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.text.substring

/**
 * An undo manager designed specifically for text editing. This class is mostly responsible for
 * delegating the incoming undo/redo/record/clear requests to its own [undoManager]. Its most
 * important task is to keep a separate staging area for incoming text edit operations to possibly
 * merge them before committing as a single undoable action.
 */
@OptIn(ExperimentalFoundationApi::class)
internal class TextUndoManager(
    initialStagingUndo: TextUndoOperation? = null,
    private val undoManager: UndoManager<TextUndoOperation> = UndoManager(
        capacity = TEXT_UNDO_CAPACITY
    )
) {
    private var stagingUndo by mutableStateOf<TextUndoOperation?>(initialStagingUndo)

    val canUndo: Boolean
        get() = undoManager.canUndo || stagingUndo != null

    val canRedo: Boolean
        get() = undoManager.canRedo && stagingUndo == null

    fun undo(state: TextFieldState) {
        if (!canUndo) {
            return
        }

        flush()
        state.undo(undoManager.undo())
    }

    fun redo(state: TextFieldState) {
        if (!canRedo) {
            return
        }

        state.redo(undoManager.redo())
    }

    fun record(op: TextUndoOperation) {
        val unobservedStagingUndo = Snapshot.withoutReadObservation { stagingUndo }
        if (unobservedStagingUndo == null) {
            stagingUndo = op
            return
        }

        val mergedOp = unobservedStagingUndo.merge(op)

        if (mergedOp != null) {
            // merged operation should replace the top op
            stagingUndo = mergedOp
            return
        }

        // no merge, flush the staging area and put the new operation in
        flush()
        stagingUndo = op
    }

    fun clearHistory() {
        stagingUndo = null
        undoManager.clearHistory()
    }

    private fun flush() {
        val unobservedStagingUndo = Snapshot.withoutReadObservation { stagingUndo }
        unobservedStagingUndo?.let { undoManager.record(it) }
        stagingUndo = null
    }

    companion object {
        object Saver : androidx.compose.runtime.saveable.Saver<TextUndoManager, Any> {
            private val undoManagerSaver = UndoManager.createSaver(TextUndoOperation.Saver)

            override fun SaverScope.save(value: TextUndoManager): Any {
                return listOf(
                    value.stagingUndo?.let {
                        with(TextUndoOperation.Saver) {
                            save(it)
                        }
                    },
                    with(undoManagerSaver) {
                        save(value.undoManager)
                    }
                )
            }

            override fun restore(value: Any): TextUndoManager? {
                val (savedStagingUndo, savedUndoManager) = value as List<*>
                return TextUndoManager(
                    savedStagingUndo?.let {
                        with(TextUndoOperation.Saver) {
                            restore(it)
                        }
                    },
                    with(undoManagerSaver) {
                        restore(savedUndoManager!!)
                    }!!
                )
            }
        }
    }
}

/**
 * Try to merge this [TextUndoOperation] with the [next]. Chronologically the [next] op must
 * come after this one. If merge is not possible, this function returns null.
 *
 * There are many rules that govern the grouping logic of successive undo operations. Here we try
 * to cover the most basic requirements but this is certainly not an exhaustive list.
 *
 * 1. Each action defines whether they can be merged at all. For example, text edits that are
 * caused by cut or paste define themselves as unmergeable no matter what comes before or next.
 * 2. If certain amount of time has passed since the latest grouping has begun.
 * 3. Enter key (hard line break) is unmergeable.
 * 4. Only same type of text edits can be merged. An insertion must be grouped by other insertions,
 * a deletion by other deletions. Replace type of edit is never mergeable.
 *   4.a. Two insertions can only be merged if the chronologically next one is a suffix of the
 *   previous insertion. In other words, cursor should always be moving forwards.
 *   4.b. Deletions have directionality. Cursor can only insert in place and move forwards but
 *   deletion can be requested either forwards (delete) or backwards (backspace). Only deletions
 *   that have the same direction can be merged. They also have to share a boundary.
 */
internal fun TextUndoOperation.merge(next: TextUndoOperation): TextUndoOperation? {
    if (!canMerge || !next.canMerge) return null
    // Do not merge if [other] came before this op, or if certain amount of time has passed
    // between these ops
    if (
        next.timeInMillis < timeInMillis ||
        next.timeInMillis - timeInMillis >= SNAPSHOTS_INTERVAL_MILLIS
    ) return null
    // Do not merge undo history when one of the ops is a new line insertion
    if (isNewLineInsert || next.isNewLineInsert) return null
    // Only same type of ops can be merged together
    if (textEditType != next.textEditType) return null

    // only merge insertions if the chronologically next one continuous from the end of
    // this previous insertion
    if (textEditType == TextEditType.Insert && index + postText.length == next.index) {
        return TextUndoOperation(
            index = index,
            preText = "",
            postText = postText + next.postText,
            preSelection = this.preSelection,
            postSelection = next.postSelection,
            timeInMillis = timeInMillis
        )
    } else if (textEditType == TextEditType.Delete) {
        // only merge consecutive deletions if both have the same directionality
        if (
            deletionType == next.deletionType &&
            (deletionType == TextDeleteType.Start || deletionType == TextDeleteType.End)
        ) {
            // This op deletes
            if (index == next.index + next.preText.length) {
                return TextUndoOperation(
                    index = next.index,
                    preText = next.preText + preText,
                    postText = "",
                    preSelection = preSelection,
                    postSelection = next.postSelection,
                    timeInMillis = timeInMillis
                )
            } else if (index == next.index) {
                return TextUndoOperation(
                    index = index,
                    preText = preText + next.preText,
                    postText = "",
                    preSelection = preSelection,
                    postSelection = next.postSelection,
                    timeInMillis = timeInMillis
                )
            }
        }
    }
    return null
}

/**
 * Adds the [changes] to this [UndoManager] by converting from [TextFieldBuffer.ChangeList] space
 * to [TextUndoOperation] space.
 *
 * @param pre State of the [TextFieldBuffer] before any changes are applied
 * @param post State of the [TextFieldBuffer] after all the changes are applied
 * @param changes List of changes that are applied on [pre] that transforms it to [post].
 * @param allowMerge Whether to allow merging the calculated operation with the last operation
 * in the stack.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun TextUndoManager.recordChanges(
    pre: TextFieldCharSequence,
    post: TextFieldCharSequence,
    changes: TextFieldBuffer.ChangeList,
    allowMerge: Boolean = true
) {
    // if there are unmerged changes coming from a single edit, force merge all of them to
    // create a single replace undo operation
    if (changes.changeCount > 1) {
        record(
            TextUndoOperation(
                index = 0,
                preText = pre.toString(),
                postText = post.toString(),
                preSelection = pre.selectionInChars,
                postSelection = post.selectionInChars,
                canMerge = false
            )
        )
    } else if (changes.changeCount == 1) {
        val preRange = changes.getOriginalRange(0)
        val postRange = changes.getRange(0)
        if (!preRange.collapsed || !postRange.collapsed) {
            record(
                TextUndoOperation(
                    index = preRange.min,
                    preText = pre.substring(preRange),
                    postText = post.substring(postRange),
                    preSelection = pre.selectionInChars,
                    postSelection = post.selectionInChars,
                    canMerge = allowMerge
                )
            )
        }
    }
}

/**
 * Determines whether this operation is adding a new hard line break. This type of change produces
 * unmergable [TextUndoOperation].
 */
private val TextUndoOperation.isNewLineInsert
    get() = this.postText == "\n" || this.postText == "\r\n"

private const val TEXT_UNDO_CAPACITY = 100
