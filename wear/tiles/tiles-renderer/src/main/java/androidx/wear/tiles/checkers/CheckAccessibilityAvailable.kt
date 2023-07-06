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

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.TimelineBuilders
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
    override fun check(entry: TimelineBuilders.TimelineEntry) {
        // Do a descent through the tile, checking that at least one element has an a11y tag.
        if (entry.layout?.root?.let(this::checkElement) == false) {
            throw CheckerException(
                "Tile layout does not have any nodes with an accessibility " +
                    "description. You should add a Semantics Modifier to at least one of your " +
                    "LayoutElements."
            )
        }
    }

    private fun checkElement(
        element: LayoutElementBuilders.LayoutElement
    ): Boolean {
        val modifiers =
            when (element) {
                is LayoutElementBuilders.Row -> element.modifiers
                is LayoutElementBuilders.Column -> element.modifiers
                is LayoutElementBuilders.Box -> element.modifiers
                is LayoutElementBuilders.Arc -> element.modifiers
                is LayoutElementBuilders.Spacer -> element.modifiers
                is LayoutElementBuilders.Image -> element.modifiers
                is LayoutElementBuilders.Text -> element.modifiers
                is LayoutElementBuilders.Spannable -> element.modifiers
                else -> null
            }

        if (modifiers?.semantics != null) {
            return true
        }

        // Descend...
        // Note that individual Spannable elements cannot have semantics; the parent should have
        // these.
        return when (element) {
            is LayoutElementBuilders.Row ->
                element.contents.any(this::checkElement)
            is LayoutElementBuilders.Column ->
                element.contents.any(this::checkElement)
            is LayoutElementBuilders.Box ->
                element.contents.any(this::checkElement)
            is LayoutElementBuilders.Arc ->
                element.contents.any(this::checkArcLayoutElement)
            else -> false
        }
    }

    private fun checkArcLayoutElement(
        element: LayoutElementBuilders.ArcLayoutElement
    ): Boolean {
        val modifiers =
            when (element) {
                // Note that ArcAdapter should be handled by taking the modifiers from the inner
                // element instead.
                is LayoutElementBuilders.ArcText -> element.modifiers
                is LayoutElementBuilders.ArcLine -> element.modifiers
                is LayoutElementBuilders.ArcSpacer -> element.modifiers
                else -> null
            }

        if (modifiers?.semantics != null) {
            return true
        }

        return if (element is LayoutElementBuilders.ArcAdapter) {
            element.content?.let(this::checkElement) ?: false
        } else {
            false
        }
    }
}
