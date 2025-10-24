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

package androidx.pdf

internal const val SCREENSHOT_GOLDEN_DIRECTORY = "pdf/pdf-viewer"

/**
 * Scuba screenshot file name constants.
 *
 * File names should be unique for each screenshot, to avoid conflict.
 *
 * Refer go/scuba-best-practices#use-good-naming for naming file names for scuba screenshots.
 */
internal const val FAST_SCROLLER_BOTTOM = "fast_scroller_bottom"
internal const val FAST_SCROLLER_TOP = "fast_scroller_top"
internal const val SEARCH_VIEW_IN_LTR_MODE = "search_view_in_ltr_mode"
internal const val SEARCH_VIEW_IN_RTL_MODE = "search_view_in_rtl_mode"
internal const val ANNOTATION_VIEW_SINGLE_SQUARE_NO_TRANSFORM =
    "annotation_view_single_square_no_transform"
internal const val ANNOTATION_VIEW_SQUARE_TRANSLATED = "annotation_view_square_translated"
internal const val ANNOTATION_VIEW_SQUARE_SCALED = "annotation_view_square_scaled"
internal const val ANNOTATION_VIEW_SQUARE_COMBINED_TRANSFORM =
    "annotation_view_square_combined_transform"
internal const val ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_NO_TRANSFORM =
    "annotation_view_multiple_squares_same_page_no_transform"
internal const val ANNOTATION_VIEW_MULTIPLE_SQUARES_SAME_PAGE_SHARED_TRANSFORM =
    "annotation_view_multiple_squares_same_page_shared_transform"
internal const val ANNOTATION_VIEW_MULTI_PAGE_DIFFERENT_TRANSFORMS =
    "annotation_view_multi_page_different_transforms"
