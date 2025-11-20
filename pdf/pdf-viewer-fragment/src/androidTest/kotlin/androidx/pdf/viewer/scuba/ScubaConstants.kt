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

package androidx.pdf.viewer.scuba

internal object ScubaConstants {
    internal const val SCREENSHOT_GOLDEN_DIRECTORY = "pdf/pdf-viewer-fragment"

    /**
     * Scuba screenshot file name constants.
     *
     * File names should be unique for each screenshot, to avoid conflict.
     *
     * Refer go/scuba-best-practices#use-good-naming for naming file names for scuba screenshots.
     */
    internal const val FILE_FAST_SCROLLER_HIDDEN_ON_LOAD = "fastScroller_hidden_onLoad"
    internal const val FILE_FAST_SCROLLER_SHOWN_IN_IMMERSIVE_MODE =
        "fastScroller_shown_inImmersiveMode"
    internal const val FILE_FAST_SCROLLER_AND_FAB_SHOWN_ON_SCROLL_TO_TOP =
        "fastScroller_and_fab_shown_onScrollToTop"
    internal const val FILE_FAST_SCROLLER_WITH_STYLE_IN_PORTRAIT =
        "fastScroller_withStyle_inPortrait"
    internal const val FILE_FAST_SCROLLER_WITH_STYLE_IN_LANDSCAPE =
        "fastScroller_withStyle_inLandscape"
    internal const val FILE_PASSWORD_DIALOG_VISIBLE_PORTRAIT = "password_dialog_portrait"
    internal const val FILE_PASSWORD_DIALOG_VISIBLE_LANDSCAPE = "password_dialog_landscape"
}
