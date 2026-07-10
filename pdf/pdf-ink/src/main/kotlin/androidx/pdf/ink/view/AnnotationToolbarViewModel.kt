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

package androidx.pdf.ink.view

import androidx.pdf.ink.view.brush.model.BrushSizes
import androidx.pdf.ink.view.colorpalette.model.Color
import androidx.pdf.ink.view.colorpalette.model.Emoji
import androidx.pdf.ink.view.state.AnnotationToolbarState
import androidx.pdf.ink.view.state.ToolbarEffect
import androidx.pdf.ink.view.state.ToolbarIntent
import androidx.pdf.ink.view.tool.Eraser
import androidx.pdf.ink.view.tool.Highlighter
import androidx.pdf.ink.view.tool.Pen
import androidx.pdf.ink.view.tool.model.AnnotationToolsKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The ViewModel for the Annotation Toolbar, following the MVI (Model-View-Intent) pattern.
 *
 * This class is the "brain" of the toolbar. It owns the single source of truth, the
 * [androidx.pdf.ink.view.state.AnnotationToolbarState], and is the only component authorized to
 * modify it. All user interactions and external events are dispatched to it as
 * [androidx.pdf.ink.view.state.ToolbarIntent]s.
 *
 * It processes these intents, updates the state accordingly, and emits
 * [androidx.pdf.ink.view.state.ToolbarEffect]s for one-off events that need to be communicated to
 * external listeners (like `onUndo` or `onToolChanged`).
 *
 * @param initialState The initial state of the toolbar.
 */
internal class AnnotationToolbarViewModel(initialState: AnnotationToolbarState) {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<AnnotationToolbarState> = _state.asStateFlow()

    // A Channel is designed for one-time events. It ensures that each
    // effect is consumed exactly once and is buffered safely across configuration changes.
    private val _effects = Channel<ToolbarEffect>(1)
    val effects = _effects.receiveAsFlow()

    /**
     * Updates the toolbar's state to a state set internally or externally. This would be useful is
     * restoring/reset scenarios.
     *
     * After updating the internal state, it immediately dispatches effects.
     *
     * @param state The non-null [AnnotationToolbarState] to update. If null, this function is a
     *   no-op.
     */
    fun updateState(state: AnnotationToolbarState?) {
        state?.let {
            _state.value = state

            dispatchToolUpdated(state)

            dispatchAnnotationVisibility(state)
        }
    }

    fun onAction(intent: ToolbarIntent) {
        when (intent) {
            is ToolbarIntent.UndoAvailabilityChanged ->
                _state.value = _state.value.copy(canUndo = intent.canUndo)
            is ToolbarIntent.RedoAvailabilityChanged ->
                _state.value = _state.value.copy(canRedo = intent.canRedo)
            is ToolbarIntent.PenToolClicked -> onDrawingToolClicked(AnnotationToolsKey.PEN)
            is ToolbarIntent.HighlighterToolClicked ->
                onDrawingToolClicked(AnnotationToolsKey.HIGHLIGHTER)
            is ToolbarIntent.EraserToolClicked -> onEraserToolClicked()
            is ToolbarIntent.ToggleColorPalette -> onToggleColorPalette()
            is ToolbarIntent.UndoClicked -> _effects.trySend(ToolbarEffect.Undo)
            is ToolbarIntent.RedoClicked -> _effects.trySend(ToolbarEffect.Redo)
            is ToolbarIntent.ToggleAnnotationVisibility -> onToggleAnnotationVisibility()
            is ToolbarIntent.ClearToolSelection -> onToolTrayCleared()
            is ToolbarIntent.BrushSizeChanged -> onBrushSizeChanged(intent)
            is ToolbarIntent.ColorSelected -> onColorSelected(intent)
            is ToolbarIntent.DismissPopups -> hideAnyPopup()
            is ToolbarIntent.DockStateChanged ->
                _state.value = _state.value.copy(dockedState = intent.dockedState)
            is ToolbarIntent.ExpandToolbar -> expandOrCollapseToolbar(isExpanded = true)
            is ToolbarIntent.CollapseToolbar -> expandOrCollapseToolbar(isExpanded = false)
        }
    }

    private fun onDrawingToolClicked(newTool: String) {
        val isAlreadySelected = state.value.selectedTool == newTool
        if (isAlreadySelected) {
            // If the pen/highlighter is already selected,
            // its button acts as a toggle for the brush slider
            _state.value =
                state.value.copy(
                    showBrushSizeSlider = !state.value.showBrushSizeSlider,
                    showColorPalette = false,
                )
        } else {
            // If a different tool was selected (or no tool), switch to the newTool.
            _state.value =
                _state.value.copy(
                    selectedTool = newTool,
                    isColorPaletteEnabled = true,
                    showBrushSizeSlider = false,
                    showColorPalette = false,
                )
            // Only dispatch the tool updated when it's not previously selected
            // to avoid redundant callbacks
            dispatchToolUpdated(state.value)
        }
    }

    private fun onEraserToolClicked() {
        val isAlreadySelected = state.value.selectedTool == AnnotationToolsKey.ERASER
        // Only update the state if the eraser is not already the active tool.
        // This prevents redundant state updates and callbacks if the user taps it multiple times.
        if (!isAlreadySelected) {
            _state.value =
                _state.value.copy(
                    selectedTool = AnnotationToolsKey.ERASER,
                    // The eraser has no configurable attributes, so its popups are always
                    // hidden/disabled.
                    showColorPalette = false,
                    showBrushSizeSlider = false,
                    isColorPaletteEnabled = false,
                )
            dispatchToolUpdated(state.value)
        }
    }

    private fun onBrushSizeChanged(intent: ToolbarIntent.BrushSizeChanged) {
        val currentSelectedTool = state.value.selectedTool
        when (currentSelectedTool) {
            AnnotationToolsKey.PEN -> {
                _state.value =
                    _state.value.copy(
                        penState =
                            _state.value.penState.copy(
                                selectedBrushSizeIndex = intent.selectedBrushIndex
                            )
                    )
            }
            AnnotationToolsKey.HIGHLIGHTER -> {
                _state.value =
                    _state.value.copy(
                        highlighterState =
                            _state.value.highlighterState.copy(
                                selectedBrushSizeIndex = intent.selectedBrushIndex
                            )
                    )
            }
        }
        // After updating the internal state for the brush size, immediately dispatch an
        // updated ToolInfo. This allows the parent view to get live updates as the
        // user scrubs the slider.
        dispatchToolUpdated(state.value)
    }

    private fun onColorSelected(intent: ToolbarIntent.ColorSelected) {
        val currentSelectedTool = state.value.selectedTool
        when (currentSelectedTool) {
            AnnotationToolsKey.PEN -> {
                _state.value =
                    _state.value.copy(
                        penState =
                            _state.value.penState.copy(
                                selectedColorIndex = intent.selectedColorIndex,
                                paletteItem = intent.paletteItem,
                            )
                    )
            }
            AnnotationToolsKey.HIGHLIGHTER -> {
                _state.value =
                    _state.value.copy(
                        highlighterState =
                            _state.value.highlighterState.copy(
                                selectedColorIndex = intent.selectedColorIndex,
                                paletteItem = intent.paletteItem,
                            )
                    )
            }
        }
        // Dispatch tool update on every color value changed
        dispatchToolUpdated(state.value)
    }

    private fun onToolTrayCleared() {
        _state.value =
            _state.value.copy(
                selectedTool = null,
                showColorPalette = false,
                isColorPaletteEnabled = false,
                showBrushSizeSlider = false,
            )
    }

    private fun onToggleAnnotationVisibility() {
        _state.value =
            _state.value.copy(
                isAnnotationVisible = !state.value.isAnnotationVisible,
                showColorPalette = false,
                showBrushSizeSlider = false,
            )
        dispatchAnnotationVisibility(state.value)
    }

    private fun onToggleColorPalette() {
        requireNotNull(state.value.selectedTool) {
            "Color palette cannot be toggled if there's no tool selected"
        }

        _state.value =
            _state.value.copy(
                showColorPalette = !state.value.showColorPalette,
                showBrushSizeSlider = false,
            )
    }

    private fun dispatchToolUpdated(state: AnnotationToolbarState) {
        val toolUpdatedEffect =
            when (state.selectedTool) {
                AnnotationToolsKey.PEN -> {
                    val brushSize = BrushSizes.penBrushSizes[state.penState.selectedBrushSizeIndex]
                    val color = (state.penState.paletteItem as? Color)?.color ?: 0x000000
                    ToolbarEffect.ToolUpdated(Pen(brushSize.toFloat(), color))
                }
                AnnotationToolsKey.HIGHLIGHTER -> {
                    val brushSize =
                        BrushSizes.highlightBrushSizes[
                                state.highlighterState.selectedBrushSizeIndex]
                    val color = (state.highlighterState.paletteItem as? Color)?.color
                    val emoji = (state.highlighterState.paletteItem as? Emoji)?.emoji
                    ToolbarEffect.ToolUpdated(Highlighter(brushSize.toFloat(), color, emoji))
                }
                AnnotationToolsKey.ERASER -> {
                    ToolbarEffect.ToolUpdated(Eraser)
                }
                else -> null
            }

        if (toolUpdatedEffect != null) _effects.trySend(toolUpdatedEffect)
    }

    private fun hideAnyPopup() {
        _state.value = _state.value.copy(showColorPalette = false, showBrushSizeSlider = false)
    }

    private fun expandOrCollapseToolbar(isExpanded: Boolean) {
        _state.value =
            _state.value.copy(
                isExpanded = isExpanded,
                // Hide any popup shown while expanding or collapsing
                showBrushSizeSlider = false,
                showColorPalette = false,
            )
    }

    private fun dispatchAnnotationVisibility(state: AnnotationToolbarState) {
        _effects.trySend(ToolbarEffect.AnnotationVisibilityChanged(state.isAnnotationVisible))
    }
}
