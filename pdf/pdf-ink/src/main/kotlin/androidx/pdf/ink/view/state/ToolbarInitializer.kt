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

import android.content.Context
import androidx.pdf.ink.view.colorpalette.model.getHighlightPaletteItems
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.PEN

/** Responsible for creating the fully-formed initial state for the annotation toolbar */
internal object ToolbarInitializer {

    // The smallest screen width threshold in density-independent pixels (dp) used to differentiate
    // between phone and tablet form factors.
    private const val TABLET_SMALLEST_SCREEN_WIDTH_DP = 600

    /**
     * Creates the default initial `AnnotationToolbarState`.
     *
     * @param context The context used to resolve context-dependent resources like color palettes.
     */
    fun createInitialState(context: Context): AnnotationToolbarState {
        val penPaletteItems = getPenPaletteItems(context)
        val highlightPaletteItems = getHighlightPaletteItems(context)

        // Default indices of selected brush size and color
        val defaultPenColorIndex = 0
        val defaultHighlighterColorIndex = 10

        return AnnotationToolbarState(
            selectedTool = PEN,
            isAnnotationVisible = true,
            canUndo = false,
            canRedo = false,
            showBrushSizeSlider = false,
            isColorPaletteEnabled = true,
            showColorPalette = false,
            penState =
                ToolAttributes(
                    selectedBrushSizeIndex = 1,
                    selectedColorIndex = defaultPenColorIndex,
                    paletteItem = penPaletteItems[defaultPenColorIndex],
                ),
            highlighterState =
                ToolAttributes(
                    selectedBrushSizeIndex = 1,
                    selectedColorIndex = defaultHighlighterColorIndex,
                    paletteItem = highlightPaletteItems[defaultHighlighterColorIndex],
                ),
            dockedState = getDefaultDockState(context),
            isExpanded = true,
        )
    }

    /**
     * Determines the default dock state for the toolbar based on the device's screen size.
     * - On tablets (smallest width >= 600dp), it returns [DOCK_STATE_END] to place the toolbar
     *   vertically on the side.
     * - On phones, it returns [DOCK_STATE_BOTTOM] to place the toolbar horizontally at the bottom.
     *
     * @param context The context used to access screen configuration.
     * @return The calculated default dock state.
     */
    private fun getDefaultDockState(context: Context): Int {
        val screenWidthDp = context.resources.configuration.smallestScreenWidthDp
        return if (screenWidthDp >= TABLET_SMALLEST_SCREEN_WIDTH_DP) DOCK_STATE_END
        else DOCK_STATE_BOTTOM
    }
}
