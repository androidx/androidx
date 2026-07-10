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

import androidx.pdf.ink.view.AnnotationToolbarViewModel
import androidx.pdf.ink.view.brush.model.BrushSizes
import androidx.pdf.ink.view.colorpalette.model.Color
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.tool.Eraser
import androidx.pdf.ink.view.tool.Highlighter
import androidx.pdf.ink.view.tool.Pen
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.HIGHLIGHTER
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey.PEN
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnnotationToolbarViewModelTest {

    private fun createInitialState(): AnnotationToolbarState =
        AnnotationToolbarState(
            selectedTool = null,
            isAnnotationVisible = true,
            showBrushSizeSlider = false,
            showColorPalette = false,
            isColorPaletteEnabled = true,
            canUndo = true,
            canRedo = false,
            highlighterState = ToolAttributes(1, 2, Color(123, 4, 5, "blue color")),
            penState = ToolAttributes(2, 3, Color(123, 4, 5, "red color")),
            dockedState = DOCK_STATE_BOTTOM,
            isExpanded = true,
        )

    private fun createViewModel(initialState: AnnotationToolbarState = createInitialState()) =
        AnnotationToolbarViewModel(initialState)

    @Test
    fun onAction_UndoAvailabilityChanged_updatesState() {
        val viewmodel = createViewModel()
        viewmodel.onAction(ToolbarIntent.UndoAvailabilityChanged(true))
        assertThat(viewmodel.state.value.canUndo).isTrue()
    }

    @Test
    fun onAction_RedoAvailabilityChanged_updatesState() {
        val viewmodel = createViewModel()
        viewmodel.onAction(ToolbarIntent.RedoAvailabilityChanged(true))
        assertThat(viewmodel.state.value.canRedo).isTrue()
    }

    @Test
    fun onAction_PenToolClicked_whenNotSelected_selectsPenAndEmitsEffect() = runTest {
        val viewmodel = createViewModel()

        // Use a background coroutine to collect effects to avoid blocking the test.
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob =
            launch(Dispatchers.Unconfined) {
                viewmodel.effects.collect { collectedEffects.add(it) }
            }

        viewmodel.onAction(ToolbarIntent.PenToolClicked)

        val finalState = viewmodel.state.value
        assertThat(finalState.selectedTool).isEqualTo(PEN)
        assertThat(finalState.isColorPaletteEnabled).isTrue()

        // Assert that the effect was received.
        assertThat(collectedEffects).isNotEmpty()
        val effect = collectedEffects.first() as ToolbarEffect.ToolUpdated
        val pen = effect.toolInfo as Pen
        assertThat(pen.brushSize)
            .isEqualTo(
                BrushSizes.penBrushSizes[finalState.penState.selectedBrushSizeIndex].toFloat()
            )

        collectionJob.cancel()
    }

    @Test
    fun onAction_PenToolClicked_whenAlreadySelected_togglesBrushSlider() {
        val initState = createInitialState().copy(selectedTool = PEN)
        val viewmodel = createViewModel(initState)
        // Since pen is already selected, clicking it again should show the brush slider
        viewmodel.onAction(ToolbarIntent.PenToolClicked)
        assertThat(viewmodel.state.value.showBrushSizeSlider).isTrue()
        // Now clicking it again should hide the brush slider
        viewmodel.onAction(ToolbarIntent.PenToolClicked)
        assertThat(viewmodel.state.value.showBrushSizeSlider).isFalse()
    }

    @Test
    fun onAction_HighlighterToolClicked_whenNotSelected_selectsHighlighter() {
        val viewmodel = createViewModel()
        viewmodel.onAction(ToolbarIntent.HighlighterToolClicked)
        val finalState = viewmodel.state.value
        assertThat(finalState.selectedTool).isEqualTo(AnnotationToolsKey.HIGHLIGHTER)
        assertThat(finalState.isColorPaletteEnabled).isTrue()
    }

    @Test
    fun onAction_HighlighterToolClicked_whenAlreadySelected_togglesBrushSlider() {
        val initState = createInitialState().copy(selectedTool = HIGHLIGHTER)
        val viewmodel = createViewModel(initState)

        // Click on highlighter again
        viewmodel.onAction(ToolbarIntent.HighlighterToolClicked)

        assertThat(viewmodel.state.value.showBrushSizeSlider).isTrue()
        // Clicking on highlighter again should hide the slider
        viewmodel.onAction(ToolbarIntent.HighlighterToolClicked)
        assertThat(viewmodel.state.value.showBrushSizeSlider).isFalse()
    }

    @Test
    fun onAction_EraserToolClicked_selectsEraserAndDisablesPopups() = runTest {
        val viewmodel = createViewModel()
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.onAction(ToolbarIntent.EraserToolClicked)

        val finalState = viewmodel.state.value
        assertThat(finalState.selectedTool).isEqualTo(AnnotationToolsKey.ERASER)
        assertThat(finalState.isColorPaletteEnabled).isFalse()
        assertThat(finalState.showBrushSizeSlider).isFalse()

        val effect = collectedEffects.first()
        assertThat(effect).isEqualTo(ToolbarEffect.ToolUpdated(Eraser))

        collectionJob.cancel()
    }

    @Test
    fun onAction_EraserToolClicked_whenAlreadySelected_doesNothing() = runTest {
        val initialState = createInitialState().copy(selectedTool = AnnotationToolsKey.ERASER)
        val viewmodel = createViewModel(initialState)
        val collectedStates = mutableListOf<AnnotationToolbarState>()
        val collectedJob = collectInto(viewmodel.state, collectedStates)

        viewmodel.onAction(ToolbarIntent.EraserToolClicked)

        assertThat(collectedStates).hasSize(1) // Only initial state, no change
        collectedJob.cancel()
    }

    @Test(expected = IllegalArgumentException::class)
    fun onAction_ToggleColorPalette_togglesVisibility_whenInitStateIsNull() {
        val viewmodel = createViewModel()
        viewmodel.onAction(ToolbarIntent.ToggleColorPalette)
    }

    @Test
    fun onAction_ToggleColorPalette_togglesVisibility() {
        val initState = createInitialState().copy(selectedTool = PEN)
        val viewmodel = createViewModel(initialState = initState)
        viewmodel.onAction(ToolbarIntent.ToggleColorPalette)
        assertThat(viewmodel.state.value.showColorPalette).isTrue()

        viewmodel.onAction(ToolbarIntent.ToggleColorPalette)
        assertThat(viewmodel.state.value.showColorPalette).isFalse()
    }

    @Test
    fun onAction_UndoClicked_emitsUndoEffect() = runTest {
        val viewmodel = createViewModel()
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.onAction(ToolbarIntent.UndoClicked)

        assertThat(collectedEffects.first()).isEqualTo(ToolbarEffect.Undo)

        collectionJob.cancel()
    }

    @Test
    fun onAction_UndoClicked_emitsRedoEffect() = runTest {
        val viewmodel = createViewModel()
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.onAction(ToolbarIntent.RedoClicked)

        assertThat(collectedEffects.first()).isEqualTo(ToolbarEffect.Redo)

        collectionJob.cancel()
    }

    @Test
    fun onAction_ToggleAnnotationVisibility_togglesStateAndEmitsEffect() = runTest {
        val viewmodel = createViewModel()
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.onAction(ToolbarIntent.ToggleAnnotationVisibility)
        assertThat(viewmodel.state.value.isAnnotationVisible).isFalse()
        assertThat(collectedEffects.first())
            .isEqualTo(ToolbarEffect.AnnotationVisibilityChanged(false))

        collectionJob.cancel()
    }

    @Test
    fun onAction_BrushSizeChanged_updatesPenStateAndEmitsEffect() = runTest {
        val initState = createInitialState().copy(selectedTool = PEN)
        val viewmodel = createViewModel(initState)
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.onAction(ToolbarIntent.BrushSizeChanged(3))
        assertThat(viewmodel.state.value.penState.selectedBrushSizeIndex).isEqualTo(3)

        val effect = collectedEffects.first() as ToolbarEffect.ToolUpdated
        val pen = effect.toolInfo as Pen
        assertThat(pen.brushSize).isEqualTo(BrushSizes.penBrushSizes[3].toFloat())

        collectionJob.cancel()
    }

    @Test
    fun onAction_BrushSizeChanged_updatesHighlighterStateAndEmitsEffect() = runTest {
        val initState = createInitialState().copy(selectedTool = HIGHLIGHTER)
        val viewmodel = createViewModel(initState)
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.onAction(ToolbarIntent.BrushSizeChanged(3))
        assertThat(viewmodel.state.value.highlighterState.selectedBrushSizeIndex).isEqualTo(3)

        val effect = collectedEffects.first() as ToolbarEffect.ToolUpdated
        val pen = effect.toolInfo as Highlighter
        assertThat(pen.brushSize).isEqualTo(BrushSizes.highlightBrushSizes[3].toFloat())

        collectionJob.cancel()
    }

    @Test
    fun onAction_clearToolTray_clearsSelectedTool() {
        val initState = createInitialState().copy(selectedTool = HIGHLIGHTER)
        val viewmodel = createViewModel(initState)

        viewmodel.onAction(ToolbarIntent.ClearToolSelection)

        assertThat(viewmodel.state.value.selectedTool).isNull()
    }

    @Test
    fun onAction_ColorSelected_updatesPenStateAndEmitsEffect() = runTest {
        val initState = createInitialState().copy(selectedTool = PEN)
        val viewmodel = createViewModel(initState)
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        val newColor = Color(5, 2, 3, "Black")

        viewmodel.onAction(ToolbarIntent.ColorSelected(10, newColor))

        val state = viewmodel.state.value
        assertThat(state.penState.selectedColorIndex).isEqualTo(10)
        assertThat(state.penState.paletteItem).isEqualTo(newColor)

        val effect = collectedEffects.first() as ToolbarEffect.ToolUpdated
        val pen = effect.toolInfo as Pen
        assertThat(pen.color).isEqualTo(newColor.color)

        collectionJob.cancel()
    }

    @Test
    fun onAction_ColorSelected_updatesHighlighterStateAndEmitsEffect() = runTest {
        val initState = createInitialState().copy(selectedTool = HIGHLIGHTER)
        val viewmodel = createViewModel(initState)
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        val newColor = Color(1, 2, 3, "Blue")

        viewmodel.onAction(ToolbarIntent.ColorSelected(5, newColor))

        val state = viewmodel.state.value
        assertThat(state.highlighterState.selectedColorIndex).isEqualTo(5)
        assertThat(state.highlighterState.paletteItem).isEqualTo(newColor)

        val effect = collectedEffects.first() as ToolbarEffect.ToolUpdated
        val highlighter = effect.toolInfo as Highlighter
        assertThat(highlighter.color).isEqualTo(newColor.color)

        collectionJob.cancel()
    }

    @Test
    fun restoreState_updatesStateAndEmitsEffects() = runTest {
        val viewmodel = createViewModel()
        val restoredState =
            AnnotationToolbarState(
                selectedTool = AnnotationToolsKey.PEN,
                isAnnotationVisible = false,
                showBrushSizeSlider = true,
                showColorPalette = false,
                isColorPaletteEnabled = true,
                canUndo = true,
                canRedo = false,
                highlighterState = ToolAttributes(1, 2, Color(123, 4, 5, "blue color")),
                penState = ToolAttributes(2, 3, Color(123, 4, 5, "red color")),
                dockedState = DOCK_STATE_BOTTOM,
                isExpanded = true,
            )
        val collectedEffects = mutableListOf<ToolbarEffect>()
        val collectionJob = collectInto(viewmodel.effects, collectedEffects)

        viewmodel.updateState(restoredState)

        assertThat(viewmodel.state.value).isEqualTo(restoredState)
        assertThat(collectedEffects).hasSize(2)
        assertThat(collectedEffects[0]).isInstanceOf(ToolbarEffect.ToolUpdated::class.java)
        assertThat(collectedEffects[1]).isEqualTo(ToolbarEffect.AnnotationVisibilityChanged(false))

        collectionJob.cancel()
    }

    @Test
    fun onAction_ExpandOrCollapse_whenExpanded_collapsesToolbar() {
        val initState = createInitialState().copy(isExpanded = true)
        val viewmodel = createViewModel(initState)

        viewmodel.onAction(ToolbarIntent.CollapseToolbar)

        assertThat(viewmodel.state.value.isExpanded).isFalse()

        // Assert brush slider and color palette are dismissed
        // while expanding or collapsing
        assertThat(viewmodel.state.value.showColorPalette).isFalse()
        assertThat(viewmodel.state.value.showBrushSizeSlider).isFalse()
    }

    @Test
    fun onAction_ExpandOrCollapse_whenCollapsed_expandsToolbar() {
        val initState = createInitialState().copy(isExpanded = false)
        val viewmodel = createViewModel(initState)

        viewmodel.onAction(ToolbarIntent.ExpandToolbar)

        assertThat(viewmodel.state.value.isExpanded).isTrue()

        // Assert brush slider and color palette are dismissed
        // while expanding or collapsing
        assertThat(viewmodel.state.value.showColorPalette).isFalse()
        assertThat(viewmodel.state.value.showBrushSizeSlider).isFalse()
    }

    @Test
    fun onAction_DockedStateChanged_updatesState() {
        val viewmodel = createViewModel()
        // Assuming the initial state is DOCK_STATE_BOTTOM, change it to DOCK_STATE_END
        val newDockState =
            androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END

        viewmodel.onAction(ToolbarIntent.DockStateChanged(newDockState))

        assertThat(viewmodel.state.value.dockedState).isEqualTo(newDockState)
    }

    private fun <T> CoroutineScope.collectInto(flow: Flow<T>, destination: MutableList<T>): Job {
        // We launch on the receiver CoroutineScope. Dispatchers.Unconfined is good for eager tests.
        return launch(Dispatchers.Unconfined) { flow.collect { destination.add(it) } }
    }
}
