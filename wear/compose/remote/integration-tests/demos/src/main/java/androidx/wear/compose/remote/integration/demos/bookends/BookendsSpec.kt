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

package androidx.wear.compose.remote.integration.demos.bookends

import androidx.compose.ui.text.style.TextAlign

/**
 * The wire contract shared by both bookends: the creation-side API in
 * [androidx.wear.compose.remote.integration.demos.bookends.material3] emits `Custom` components
 * with these names and property ids, and the player-side plugins in
 * [androidx.wear.compose.remote.integration.demos.bookends.player] render them with the Wear
 * Compose Material3 components.
 */
internal object BookendsSpec {
    // Custom component names.
    const val SLOT = "wear.m3:slot"
    const val APP_SCAFFOLD = "wear.m3:app-scaffold"
    const val SCREEN_SCAFFOLD = "wear.m3:screen-scaffold"
    const val TIME_TEXT = "wear.m3:time-text"
    const val SCROLL_INDICATOR = "wear.m3:scroll-indicator"
    const val TRANSFORMING_LAZY_COLUMN = "wear.m3:transforming-lazy-column"
    const val BUTTON = "wear.m3:button"
    const val ICON = "wear.m3:icon"
    const val TEXT = "wear.m3:text"

    // SLOT properties.
    const val PROP_SLOT_ID = 1

    // Slot ids. A slot groups the remote children for one composable lambda parameter.
    const val SLOT_CONTENT = 0
    const val SLOT_TIME_TEXT = 1
    const val SLOT_SCROLL_INDICATOR = 2
    const val SLOT_LABEL = 3
    const val SLOT_SECONDARY_LABEL = 4
    const val SLOT_ICON = 5

    // BUTTON properties.
    const val PROP_ENABLED = 1

    // ICON properties.
    const val PROP_BITMAP = 1
    const val PROP_CONTENT_DESCRIPTION = 2
    const val PROP_TINT = 3

    /** Size of the icon of a button, matching `ButtonDefaults.IconSize`. */
    const val ICON_SIZE_DP = 26

    // TEXT properties.
    const val PROP_TEXT = 1
    const val PROP_COLOR = 2
    const val PROP_TEXT_ALIGN = 3
    const val PROP_MAX_LINES = 4

    /** [TextAlign] has no public stable int value, so map it explicitly for the wire. */
    fun TextAlign.toWire(): Int =
        when (this) {
            TextAlign.Left -> 1
            TextAlign.Right -> 2
            TextAlign.Center -> 3
            TextAlign.Justify -> 4
            TextAlign.Start -> 5
            TextAlign.End -> 6
            else -> 0
        }

    fun textAlignFromWire(value: Int): TextAlign? =
        when (value) {
            1 -> TextAlign.Left
            2 -> TextAlign.Right
            3 -> TextAlign.Center
            4 -> TextAlign.Justify
            5 -> TextAlign.Start
            6 -> TextAlign.End
            else -> null
        }
}
