/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.ui.core.LayoutCoordinates
import androidx.ui.unit.PxPosition

/**
 * Provides [Selection] information for a composable to SelectionContainer. Composables who can
 * be selected should subscribe to [SelectionRegistrar] using this interface.
 */
interface Selectable {
    /**
     * Returns [Selection] information for a selectable composable. If no selection can be provided
     * null should be returned.
     *
     * @param startPosition graphical position of the start of the selection
     * @param endPosition graphical position of the end of the selection
     * @param containerLayoutCoordinates [LayoutCoordinates] of the widget
     * @param longPress true represents that selection is either initiated via a long press or
     *  being dragged after long press
     * @param previousSelection previous selection result
     * @param isStartHandle true if the start handle is being dragged
     *
     *  @return null if no selection will be applied for this composable, or [Selection] instance
     *    if selection is applied to this composable.
     */
    fun getSelection(
        startPosition: PxPosition,
        endPosition: PxPosition,
        containerLayoutCoordinates: LayoutCoordinates,
        longPress: Boolean,
        previousSelection: Selection? = null,
        isStartHandle: Boolean = true
    ): Selection?

    /**
     * Return the [PxPosition] of a [SelectionHandle].
     *
     * @param selection [Selection] contains the [SelectionHandle]
     * @param isStartHandle true if it's the start handle, false if it's the end handle.
     *
     * @return [PxPosition] of this handle, based on which the [SelectionHandle] will be drawn.
     */
    fun getHandlePosition(selection: Selection, isStartHandle: Boolean): PxPosition

    /**
     * Return the [LayoutCoordinates] of the [Selectable].
     *
     * @return [LayoutCoordinates] of the [Selectable]. This could be null if called before
     * composing.
     */
    fun getLayoutCoordinates(): LayoutCoordinates?
}
