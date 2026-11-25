/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.markuptext.parser.block

/**
 * Ordered or unordered list container holding [ListItemParseBlock] children.
 *
 * A list has no syntax of its own: a list item outside a list of its type wraps itself in a new
 * list (see [ListItemParseBlock.prepareForAttach]), and each line is matched by the items. A list
 * holds only list items of its own type ([canAcceptChild]); any other block closes it and moves up
 * to the list's ancestors.
 */
internal class ListParseBlock(
    id: Long = -1L,
    /**
     * The marker of this list's first item, fixed when the list opened. The list reads only its
     * type ([ListMarker.isSameListType]) and [ListMarker.startNumber].
     */
    val marker: ListMarker,
) : BaseContainerParseBlock(id) {

    override val supportsLazyContinuation: Boolean = true

    override fun matchLineStart(context: ParseContext): Boolean = true

    override fun canAcceptChild(child: ParseBlock): Boolean =
        child is ListItemParseBlock && marker.isSameListType(child.marker)

    // A list has no line prefix, so the blank lines at the end of its last item are also blank for
    // the list's parent (for example, they separate a nested list from the outer item's next
    // block).
    override fun countTrailingEmptyLines(): Int =
        children.lastOrNull()?.countTrailingEmptyLines() ?: 0

    override val consumesTrailingEmptyLines: Boolean
        get() = children.lastOrNull()?.consumesTrailingEmptyLines ?: false

    /**
     * Computes whether this list is tight: no blank lines separate any two of its items, or two
     * blocks directly inside one of its items.
     *
     * Blank lines stay in the block that consumed them, so a separator is found from the trailing
     * blank lines of every block that has a next sibling ([separatingEmptyLineCount]). Blank lines
     * that are part of a block's content ([ParseBlock.consumesTrailingEmptyLines]) never count.
     *
     * Walks every item and its direct children on each call, so callers should compute it once per
     * list, not once per item.
     */
    fun computeIsTight(): Boolean {
        val lastIndex = children.lastIndex
        for (index in 0..lastIndex) {
            val item = children[index]
            // Blank lines at the end of an item separate it from the next item.
            if (index < lastIndex && item.separatingEmptyLineCount() > 0) {
                return false
            }
            if (item is ListItemParseBlock && item.hasBlankLineBetweenChildren()) {
                return false
            }
        }
        return true
    }
}

/**
 * Number of blank lines at the end of this block that separate it from the next block: its
 * [ParseBlock.countTrailingEmptyLines], or `0` if they are its content
 * ([ParseBlock.consumesTrailingEmptyLines]).
 */
internal fun ParseBlock.separatingEmptyLineCount(): Int =
    if (consumesTrailingEmptyLines) 0 else countTrailingEmptyLines()
