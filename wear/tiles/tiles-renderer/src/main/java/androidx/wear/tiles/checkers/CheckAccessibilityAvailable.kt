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
    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types.
    override fun check(entry: androidx.wear.tiles.TimelineBuilders.TimelineEntry) {
        // Do a descent through the tile, checking that at least one element has an a11y tag.
        if (entry.layout?.root?.let(this::checkElement) == false) {
            throw CheckerException(
                "Tile layout does not have any nodes with an accessibility " +
                    "description. You should add a Semantics Modifier to at least one of your " +
                    "LayoutElements."
            )
        }
    }

    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types.
    private fun checkElement(
        element: androidx.wear.tiles.LayoutElementBuilders.LayoutElement
    ): Boolean {
        val modifiers =
            when (element) {
                is androidx.wear.tiles.LayoutElementBuilders.Row -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Column -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Box -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Arc -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Spacer -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Image -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Text -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.Spannable -> element.modifiers
                else -> null
            }

        if (modifiers?.semantics != null) {
            return true
        }

        // Descend...
        // Note that individual Spannable elements cannot have semantics; the parent should have
        // these.
        return when (element) {
            is androidx.wear.tiles.LayoutElementBuilders.Row ->
                element.contents.any(this::checkElement)
            is androidx.wear.tiles.LayoutElementBuilders.Column ->
                element.contents.any(this::checkElement)
            is androidx.wear.tiles.LayoutElementBuilders.Box ->
                element.contents.any(this::checkElement)
            is androidx.wear.tiles.LayoutElementBuilders.Arc ->
                element.contents.any(this::checkArcLayoutElement)
            else -> false
        }
    }

    @Suppress("deprecation") // TODO(b/276343540): Use protolayout types.
    private fun checkArcLayoutElement(
        element: androidx.wear.tiles.LayoutElementBuilders.ArcLayoutElement
    ): Boolean {
        val modifiers =
            when (element) {
                // Note that ArcAdapter should be handled by taking the modifiers from the inner
                // element instead.
                is androidx.wear.tiles.LayoutElementBuilders.ArcText -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.ArcLine -> element.modifiers
                is androidx.wear.tiles.LayoutElementBuilders.ArcSpacer -> element.modifiers
                else -> null
            }

        if (modifiers?.semantics != null) {
            return true
        }

        return if (element is androidx.wear.tiles.LayoutElementBuilders.ArcAdapter) {
            element.content?.let(this::checkElement) ?: false
        } else {
            false
        }
    }
}