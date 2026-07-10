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

internal const val SCREENSHOT_GOLDEN_DIRECTORY = "pdf/pdf-ink"

/**
 * Scuba screenshot file name constants.
 *
 * File names should be unique for each screenshot, to avoid conflict.
 *
 * Refer go/scuba-best-practices#use-good-naming for naming file names for scuba screenshots.
 */
internal const val ANNOTATION_TOOLBAR = "annotation_toolbar"
internal const val ANNOTATION_TOOLBAR_WITH_PEN_SELECTED = "annotation_toolbar_with_pen_selected"
internal const val ANNOTATION_TOOLBAR_WITH_SLIDER_VISIBLE = "annotation_toolbar_with_slider_visible"
internal const val ANNOTATION_TOOLBAR_WITH_COLOR_PALETTE_VISIBLE =
    "annotation_toolbar_with_color_palette_visible"

internal const val ANNOTATION_TOOLBAR_IN_DARK_MODE = "annotation_toolbar_in_dark_mode"

internal const val ANNOTATION_TOOLBAR_IN_LIGHT_MODE = "annotation_toolbar_in_light_mode"

internal const val ANNOTATION_TOOLBAR_COLLAPSED = "annotation_toolbar_collapsed"
internal const val ANNOTATION_TOOLBAR_DOCKED_START_WITH_BRUSH_SIZE =
    "annotation_toolbar_docked_start_with_brush_size"
internal const val ANNOTATION_TOOLBAR_DOCKED_START_WITH_COLOR_PALETTE =
    "annotation_toolbar_docked_start_with_color_palette"
internal const val ANNOTATION_TOOLBAR_DOCKED_END_WITH_BRUSH_SIZE =
    "annotation_toolbar_docked_end_with_brush_size"
internal const val ANNOTATION_TOOLBAR_DOCKED_END_WITH_COLOR_PALETTE =
    "annotation_toolbar_docked_end_with_color_palette"
internal const val BRUSH_SIZE_SELECTED_ON_STEP_0 = "brush_size_selector_on_step_0"
internal const val BRUSH_SIZE_SELECTED_ON_STEP_4 = "brush_size_selector_on_step_4"
internal const val BRUSH_SIZE_IN_VERTICAL_ORIENTATION = "brush_size_in_vertical_orientation"
internal const val PALETTE_COLOR_ITEM_UNSELECTED = "palette_color_item_unselected"
internal const val PALETTE_COLOR_ITEM_SELECTED = "palette_color_item_selected"
internal const val PALETTE_COLOR_ITEM_SELECTED_INVERSE_COLOR_TICK =
    "palette_color_item_selected_inverse_color_tick"

internal const val PALETTE_VIEW_WITH_PEN_ITEMS = "palette_view_with_pen_items"

internal const val PALETTE_VIEW_WITH_HIGHLIGHT_ITEMS = "palette_view_highlight_items"
internal const val PALETTE_VIEW_AFTER_CLICK_COLOR = "palette_view_after_click_color"
