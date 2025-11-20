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
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.PEN

/** Responsible for creating the fully-formed initial state for the annotation toolbar */
internal object ToolbarInitializer {

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
            isBrushSizeSliderVisible = false,
            isColorPaletteEnabled = true,
            isColorPaletteVisible = false,
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
        )
    }
}
