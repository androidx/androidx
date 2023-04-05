/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.tiles.checkers

import androidx.wear.tiles.LayoutElementBuilders.Arc
import androidx.wear.tiles.LayoutElementBuilders.ArcAdapter
import androidx.wear.tiles.LayoutElementBuilders.ArcLayoutElement
import androidx.wear.tiles.LayoutElementBuilders.ArcLine
import androidx.wear.tiles.LayoutElementBuilders.ArcSpacer
import androidx.wear.tiles.LayoutElementBuilders.ArcText
import androidx.wear.tiles.LayoutElementBuilders.Box
import androidx.wear.tiles.LayoutElementBuilders.Column
import androidx.wear.tiles.LayoutElementBuilders.Image
import androidx.wear.tiles.LayoutElementBuilders.LayoutElement
import androidx.wear.tiles.LayoutElementBuilders.Row
import androidx.wear.tiles.LayoutElementBuilders.Spacer
import androidx.wear.tiles.LayoutElementBuilders.Spannable
import androidx.wear.tiles.LayoutElementBuilders.Text
import androidx.wear.tiles.TimelineBuilders
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import kotlin.jvm.Throws

/**
 * Checks a [TimelineBuilders.TimelineEntry] to ensure that at least one element within it has an
 * accessibility description registered.
 *
 * At least one element on each tile should have a machine-readable content description
 * associated with it, which can be read out using screen readers.
 */
internal class CheckAccessibilityAvailable : TimelineEntryChecker {
    override val name: String
        get() = "CheckAccessibilityAvailable"

    @Throws(CheckerException::class)
    override fun check(entry: TimelineEntry) {
        // Do a descent through the tile, checking that at least one element has an a11y tag.
        if (entry.layout?.root?.let(this::checkElement) == false) {
            throw CheckerException(
                "Tile layout does not have any nodes with an accessibility " +
                    "description. You should add a Semantics Modifier to at least one of your " +
                    "LayoutElements."
            )
        }
    }

    private fun checkElement(element: LayoutElement): Boolean {
        val modifiers = when (element) {
            is Row -> element.modifiers
            is Column -> element.modifiers
            is Box -> element.modifiers
            is Arc -> element.modifiers
            is Spacer -> element.modifiers
            is Image -> element.modifiers
            is Text -> element.modifiers
            is Spannable -> element.modifiers
            else -> null
        }

        if (modifiers?.semantics != null) {
            return true
        }

        // Descend...
        // Note that individual Spannable elements cannot have semantics; the parent should have
        // these.
        return when (element) {
            is Row -> element.contents.any(this::checkElement)
            is Column -> element.contents.any(this::checkElement)
            is Box -> element.contents.any(this::checkElement)
            is Arc -> element.contents.any(this::checkArcLayoutElement)
            else -> false
        }
    }

    private fun checkArcLayoutElement(element: ArcLayoutElement): Boolean {
        val modifiers = when (element) {
            // Note that ArcAdapter should be handled by taking the modifiers from the inner element
            // instead.
            is ArcText -> element.modifiers
            is ArcLine -> element.modifiers
            is ArcSpacer -> element.modifiers
            else -> null
        }

        if (modifiers?.semantics != null) {
            return true
        }

        return if (element is ArcAdapter) {
            element.content?.let(this::checkElement) ?: false
        } else {
            false
        }
    }
}