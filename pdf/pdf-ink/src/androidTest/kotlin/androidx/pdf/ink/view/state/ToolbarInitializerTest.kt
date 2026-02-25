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
import androidx.pdf.ink.view.draganddrop.ToolbarDockState
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ToolbarInitializerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun createInitialState_returnsCorrectDefaultState() {
        val initialState = ToolbarInitializer.createInitialState(context)

        // Verify top-level state defaults
        assertThat(initialState.selectedTool).isEqualTo(AnnotationToolsKey.PEN)
        assertThat(initialState.isAnnotationVisible).isTrue()
        assertThat(initialState.canUndo).isFalse()
        assertThat(initialState.canRedo).isFalse()
        assertThat(initialState.showBrushSizeSlider).isFalse()
        assertThat(initialState.isColorPaletteEnabled).isTrue()
        assertThat(initialState.showColorPalette).isFalse()

        // Verify default pen attributes
        val penPaletteItems = getPenPaletteItems(context)
        val defaultPenColorIndex = 0
        assertThat(initialState.penState.selectedBrushSizeIndex).isEqualTo(1)
        assertThat(initialState.penState.selectedColorIndex).isEqualTo(defaultPenColorIndex)
        assertThat(initialState.penState.paletteItem)
            .isEqualTo(penPaletteItems[defaultPenColorIndex])

        // Verify default highlighter attributes
        val highlightPaletteItems = getHighlightPaletteItems(context)
        val defaultHighlighterColorIndex = 10
        assertThat(initialState.highlighterState.selectedBrushSizeIndex).isEqualTo(1)
        assertThat(initialState.highlighterState.selectedColorIndex)
            .isEqualTo(defaultHighlighterColorIndex)
        assertThat(initialState.highlighterState.paletteItem)
            .isEqualTo(highlightPaletteItems[defaultHighlighterColorIndex])
        assertThat(initialState.dockedState).isEqualTo(ToolbarDockState.DOCK_STATE_BOTTOM)
        assertThat(initialState.isExpanded).isTrue()
    }
}
