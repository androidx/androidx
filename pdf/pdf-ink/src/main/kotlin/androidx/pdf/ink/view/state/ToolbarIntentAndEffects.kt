/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.pdf.ink.view.state

import androidx.pdf.ink.view.AnnotationToolbar
import androidx.pdf.ink.view.colorpalette.model.PaletteItem
import androidx.pdf.ink.view.tool.AnnotationToolInfo

/**
 * Represents all possible user actions or system events that can occur on the AnnotationToolbar.
 * These intents are dispatched to the state holder to be processed.
 */
internal sealed interface ToolbarIntent {

    // --- Tool Selection Intents ---
    /** User clicked the Pen tool button. */
    object PenToolClicked : ToolbarIntent

    /** User clicked the Highlighter tool button. */
    object HighlighterToolClicked : ToolbarIntent

    /** User clicked the Eraser tool button. */
    object EraserToolClicked : ToolbarIntent

    /** Triggered when [AnnotationToolbar.clearToolSelection] is called. */
    object ClearToolSelection : ToolbarIntent

    // --- Pop-up UI Toggles ---
    /** User clicked the color palette icon to toggle its visibility. */
    object ToggleColorPalette : ToolbarIntent

    // --- Attribute Change Intents ---
    /** User selected a new brush size from the slider. */
    data class BrushSizeChanged(val selectedBrushIndex: Int) : ToolbarIntent

    /** User selected a new color or item from the palette. */
    data class ColorSelected(val selectedColorIndex: Int, val paletteItem: PaletteItem) :
        ToolbarIntent

    // --- Action Intents ---
    /** User clicked the Undo button. */
    object UndoClicked : ToolbarIntent

    /** User clicked the Redo button. */
    object RedoClicked : ToolbarIntent

    /** User clicked the button to toggle annotation visibility. */
    object ToggleAnnotationVisibility : ToolbarIntent

    // --- External State Update Intents ---
    /** The availability of the undo action has changed externally. */
    data class UndoAvailabilityChanged(val canUndo: Boolean) : ToolbarIntent

    /** The availability of the redo action has changed externally. */
    data class RedoAvailabilityChanged(val canRedo: Boolean) : ToolbarIntent
}

/**
 * Represents side effects that the AnnotationToolbar needs to communicate to external listeners.
 * These are "fire-and-forget" events that do not directly alter the toolbar's own state.
 */
internal sealed interface ToolbarEffect {
    /** Announce that the selected tool or its attributes have been updated. */
    data class ToolUpdated(val toolInfo: AnnotationToolInfo) : ToolbarEffect

    /** Announce that the user has triggered an undo action. */
    object Undo : ToolbarEffect

    /** Announce that the user has triggered a redo action. */
    object Redo : ToolbarEffect

    /** Announce that the visibility of annotations has been changed. */
    data class AnnotationVisibilityChanged(val isVisible: Boolean) : ToolbarEffect
}
