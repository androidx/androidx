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
import androidx.ui.core.PxPosition

/**
 * An interface handling selection. Get selection from a composable by passing in the start and end
 * of selection in a selection container, and the layout coordinates of the selection container,
 * with other information.
 */
interface TextSelectionHandler {
    /**
     * Method to get selection from one composable.
     * For example, a text composable subscribes to SelectionRegistrar with a TextSelectionDelegate
     * which implements TextSelectionHandler.
     *
     * @param startPosition graphical position of the start of the selection
     * @param endPosition graphical position of the end of the selection
     * @param containerLayoutCoordinates [LayoutCoordinates] of the widget
     * @param wordSelectIfCollapsed This flag is ignored if the selection offsets anchors point
     * different location. If the selection anchors point the same location and this is true, the
     * result selection will be adjusted to word boundary. Otherwise, the selection will be adjusted
     * to keep single character selected.
     */
    fun getSelection(
        startPosition: PxPosition,
        endPosition: PxPosition,
        containerLayoutCoordinates: LayoutCoordinates,
        wordSelectIfCollapsed: Boolean
    ): Selection?
}
