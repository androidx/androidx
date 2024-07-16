/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.MultiParagraph
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Constraints

/**
 * Provides [Selection] information for a composable to SelectionContainer. Composables who can
 * be selected should subscribe to [SelectionRegistrar] using this interface.
 */

internal interface Selectable {
    /**
     * An ID used by [SelectionRegistrar] to identify this [Selectable]. This value should not be
     * [SelectionRegistrar.InvalidSelectableId].
     * When a [Selectable] is created, it can request an ID from [SelectionRegistrar] by
     * calling [SelectionRegistrar.nextSelectableId].
     * @see SelectionRegistrar.nextSelectableId
     */
    val selectableId: Long

    /**
     * A function which adds [SelectableInfo] representing this [Selectable]
     * to the [SelectionLayoutBuilder].
     */
    fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder)

    /**
     * Returns selectAll [Selection] information for a selectable composable. If no selection can be
     * provided null should be returned.
     *
     * @return selectAll [Selection] information for a selectable composable. If no selection can be
     * provided null should be returned.
     */
    fun getSelectAllSelection(): Selection?

    /**
     * Return the [Offset] of a [SelectionHandle].
     *
     * @param selection [Selection] contains the [SelectionHandle]
     * @param isStartHandle true if it's the start handle, false if it's the end handle.
     *
     * @return [Offset] of this handle, based on which the [SelectionHandle] will be drawn.
     */
    fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset

    /**
     * Return the [LayoutCoordinates] of the [Selectable].
     *
     * @return [LayoutCoordinates] of the [Selectable]. This could be null if called before
     * composing.
     */
    fun getLayoutCoordinates(): LayoutCoordinates?

    /**
     * Return the [TextLayoutResult] of the selectable.
     *
     * @return [TextLayoutResult] of the [Selectable]. This could be null if called before
     *   composing.
     */
    fun textLayoutResult(): TextLayoutResult?

    /**
     * Return the [AnnotatedString] of the [Selectable].
     *
     * @return text content as [AnnotatedString] of the [Selectable].
     */
    fun getText(): AnnotatedString

    /**
     * Return the bounding box of the character for given character offset. This is currently for
     * text.
     * In future when we implemented other selectable Composables, we can return the bounding box of
     * the wanted rectangle. For example, for an image selectable, this should return the
     * bounding box of the image.
     *
     * @param offset a character offset
     * @return the bounding box for the character in [Rect], or [Rect.Zero] if the selectable is
     * empty.
     */
    fun getBoundingBox(offset: Int): Rect

    /**
     * Returns the left x coordinate of the line for the given offset.
     *
     * @param offset a character offset
     * @return the line left x coordinate for the given offset
     */
    fun getLineLeft(offset: Int): Float

    /**
     * Returns the right x coordinate of the line for the given offset.
     *
     * @param offset a character offset
     * @return the line right x coordinate for the given offset
     */
    fun getLineRight(offset: Int): Float

    /**
     * Returns the center y coordinate of the line on which the specified text offset appears.
     *
     * If you ask for a position before 0, you get the center of the first line;
     * if you ask for a position beyond the end of the text, you get the center of the last line.
     *
     * @param offset a character offset
     * @return the line center y coordinate of the line containing [offset]
     */
    fun getCenterYForOffset(offset: Int): Float

    /**
     * Return the offsets of the start and end of the line containing [offset], or [TextRange.Zero]
     * if the selectable is empty. These offsets are in the same "coordinate space" as
     * [getBoundingBox], and despite being returned in a [TextRange], may not refer to offsets in
     * actual text if the selectable contains other types of content. This function returns
     * the last visible line's boundaries if offset is larger than text length or the character at
     * given offset would fall on a line which is hidden by maxLines or Constraints.
     */
    fun getRangeOfLineContaining(offset: Int): TextRange

    /**
     * Returns the last visible character's offset. Some lines can be hidden due to either
     * [TextLayoutInput.maxLines] or [Constraints.maxHeight] being smaller than
     * [MultiParagraph.height]. If overflow is set to clip and a line is partially visible, it
     * counts as the last visible line.
     */
    fun getLastVisibleOffset(): Int

    fun getLineHeight(offset: Int): Float = 0f
}
