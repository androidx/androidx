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

package androidx.pdf.ink.view.layout

import android.content.Context
import androidx.constraintlayout.widget.ConstraintSet
import androidx.pdf.ink.R
import androidx.pdf.ink.view.draganddrop.ToolbarDockState
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_BOTTOM
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_END
import androidx.pdf.ink.view.draganddrop.ToolbarDockState.Companion.DOCK_STATE_START

internal class AnnotationToolbarConstraintSet(context: Context) {

    private val margin16dp = context.resources.getDimensionPixelSize(R.dimen.margin_16dp)
    private val colorPaletteMaxWidth =
        context.resources.getDimensionPixelSize(R.dimen.color_palette_max_width)
    private val colorPaletteMaxHeight =
        context.resources.getDimensionPixelSize(R.dimen.color_palette_max_height)

    val dockStateStart: ConstraintSet = createConstraintSetFor(DOCK_STATE_START)
    val dockStateEnd: ConstraintSet = createConstraintSetFor(DOCK_STATE_END)
    val dockStateBottom: ConstraintSet = createConstraintSetFor(DOCK_STATE_BOTTOM)

    /**
     * Creates a new [ConstraintSet] configured for the given [ToolbarDockState.DockState]. This is
     * the main factory method that delegates to helper functions.
     */
    private fun createConstraintSetFor(@ToolbarDockState.DockState dockState: Int): ConstraintSet {
        return ConstraintSet().apply {
            applyToolTrayConstraints(dockState)
            applyColorPaletteConstraints(dockState)
            applyBrushSliderConstraints(dockState)
        }
    }

    /** Applies the constraints for the main `scrollable_tool_tray_container`. */
    private fun ConstraintSet.applyToolTrayConstraints(@ToolbarDockState.DockState dockState: Int) {
        clear(R.id.scrollable_tool_tray_container, ConstraintSet.TOP)
        clear(R.id.scrollable_tool_tray_container, ConstraintSet.BOTTOM)
        clear(R.id.scrollable_tool_tray_container, ConstraintSet.START)
        clear(R.id.scrollable_tool_tray_container, ConstraintSet.END)

        constrainWidth(R.id.scrollable_tool_tray_container, ConstraintSet.WRAP_CONTENT)
        constrainHeight(R.id.scrollable_tool_tray_container, ConstraintSet.WRAP_CONTENT)

        when (dockState) {
            DOCK_STATE_START -> {
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                )
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                )
            }
            DOCK_STATE_END -> {
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                )
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                )
            }
            DOCK_STATE_BOTTOM -> {
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                )
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                )
                connect(
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                )
            }
        }
    }

    /** Applies the constraints for the `color_palette` view. */
    private fun ConstraintSet.applyColorPaletteConstraints(
        @ToolbarDockState.DockState dockState: Int
    ) {
        clear(R.id.color_palette, ConstraintSet.TOP)
        clear(R.id.color_palette, ConstraintSet.BOTTOM)
        clear(R.id.color_palette, ConstraintSet.START)
        clear(R.id.color_palette, ConstraintSet.END)

        when (dockState) {
            DOCK_STATE_START -> {
                constrainWidth(R.id.color_palette, ConstraintSet.WRAP_CONTENT)
                constrainMaxWidth(R.id.color_palette, colorPaletteMaxWidth)
                constrainedWidth(R.id.color_palette, true)
                constrainHeight(R.id.color_palette, ConstraintSet.MATCH_CONSTRAINT)
                connect(
                    R.id.color_palette,
                    ConstraintSet.TOP,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.BOTTOM,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.START,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.END,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                )

                setMargin(R.id.color_palette, ConstraintSet.START, margin16dp)
            }
            DOCK_STATE_END -> {
                constrainWidth(R.id.color_palette, ConstraintSet.WRAP_CONTENT)
                constrainMaxWidth(R.id.color_palette, colorPaletteMaxWidth)
                constrainedWidth(R.id.color_palette, true)
                constrainHeight(R.id.color_palette, ConstraintSet.MATCH_CONSTRAINT)

                connect(
                    R.id.color_palette,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.TOP,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.BOTTOM,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.END,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.START,
                )
                setMargin(R.id.color_palette, ConstraintSet.END, margin16dp)
            }
            DOCK_STATE_BOTTOM -> {
                constrainWidth(R.id.color_palette, ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(R.id.color_palette, ConstraintSet.WRAP_CONTENT)
                constrainedHeight(R.id.color_palette, true)
                constrainMaxHeight(R.id.color_palette, colorPaletteMaxHeight)

                connect(
                    R.id.color_palette,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.START,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.START,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.END,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.END,
                )
                connect(
                    R.id.color_palette,
                    ConstraintSet.BOTTOM,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                )
                setMargin(R.id.color_palette, ConstraintSet.BOTTOM, margin16dp)
            }
        }
    }

    /** Applies the constraints for the `brush_size_selector` view. */
    private fun ConstraintSet.applyBrushSliderConstraints(
        @ToolbarDockState.DockState dockState: Int
    ) {
        clear(R.id.brush_size_selector, ConstraintSet.TOP)
        clear(R.id.brush_size_selector, ConstraintSet.BOTTOM)
        clear(R.id.brush_size_selector, ConstraintSet.START)
        clear(R.id.brush_size_selector, ConstraintSet.END)

        when (dockState) {
            DOCK_STATE_START -> {
                constrainWidth(R.id.brush_size_selector, ConstraintSet.WRAP_CONTENT)
                constrainedWidth(R.id.brush_size_selector, true)
                constrainHeight(R.id.brush_size_selector, ConstraintSet.MATCH_CONSTRAINT)

                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.TOP,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.BOTTOM,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.START,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.END,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                )
                setMargin(R.id.brush_size_selector, ConstraintSet.START, margin16dp)
            }
            DOCK_STATE_END -> {
                constrainWidth(R.id.brush_size_selector, ConstraintSet.WRAP_CONTENT)
                constrainedWidth(R.id.brush_size_selector, true)
                constrainHeight(R.id.brush_size_selector, ConstraintSet.MATCH_CONSTRAINT)

                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.TOP,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.BOTTOM,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.BOTTOM,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.END,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.START,
                )
                setMargin(R.id.brush_size_selector, ConstraintSet.END, margin16dp)
            }
            DOCK_STATE_BOTTOM -> {
                constrainWidth(R.id.brush_size_selector, ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(R.id.brush_size_selector, ConstraintSet.WRAP_CONTENT)
                constrainedHeight(R.id.brush_size_selector, true)

                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.START,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.START,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.END,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.END,
                )
                connect(
                    R.id.brush_size_selector,
                    ConstraintSet.BOTTOM,
                    R.id.scrollable_tool_tray_container,
                    ConstraintSet.TOP,
                )
                setMargin(R.id.brush_size_selector, ConstraintSet.BOTTOM, margin16dp)
            }
        }
    }
}
