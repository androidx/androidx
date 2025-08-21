/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.LayoutDirection

/** Represents the horizontal order of panes in a [ThreePaneScaffold] from start to end. */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneScaffoldHorizontalOrder
internal constructor(
    internal val firstPane: ThreePaneScaffoldRole,
    internal val secondPane: ThreePaneScaffoldRole,
    internal val thirdPane: ThreePaneScaffoldRole,
) : PaneScaffoldHorizontalOrder<ThreePaneScaffoldRole> {
    init {
        require(firstPane != secondPane && secondPane != thirdPane && firstPane != thirdPane) {
            "invalid ThreePaneScaffoldHorizontalOrder($firstPane, $secondPane, $thirdPane)" +
                " - panes must be unique"
        }
    }

    override val size = 3

    operator fun get(index: Int) =
        when (index) {
            0 -> firstPane
            1 -> secondPane
            2 -> thirdPane
            else -> throw IndexOutOfBoundsException("Invalid pane index $index")
        }

    override fun indexOf(role: ThreePaneScaffoldRole) =
        when (role) {
            firstPane -> 0
            secondPane -> 1
            thirdPane -> 2
            else -> -1
        }

    override fun forEach(action: (ThreePaneScaffoldRole) -> Unit) {
        action(firstPane)
        action(secondPane)
        action(thirdPane)
    }

    override fun forEachIndexed(action: (Int, ThreePaneScaffoldRole) -> Unit) {
        action(0, firstPane)
        action(1, secondPane)
        action(2, thirdPane)
    }

    override fun forEachIndexedReversed(action: (Int, ThreePaneScaffoldRole) -> Unit) {
        action(2, thirdPane)
        action(1, secondPane)
        action(0, firstPane)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldHorizontalOrder) return false
        if (firstPane != other.firstPane) return false
        if (secondPane != other.secondPane) return false
        if (thirdPane != other.thirdPane) return false
        return true
    }

    override fun hashCode(): Int {
        var result = firstPane.hashCode()
        result = 31 * result + secondPane.hashCode()
        result = 31 * result + thirdPane.hashCode()
        return result
    }
}

/**
 * Converts a bidirectional [ThreePaneScaffoldHorizontalOrder] to a left-to-right order.
 *
 * @param layoutDirection the current [LayoutDirection]
 */
@ExperimentalMaterial3AdaptiveApi
internal fun ThreePaneScaffoldHorizontalOrder.toLtrOrder(
    layoutDirection: LayoutDirection
): ThreePaneScaffoldHorizontalOrder {
    return if (layoutDirection == LayoutDirection.Rtl) {
        ThreePaneScaffoldHorizontalOrder(thirdPane, secondPane, firstPane)
    } else {
        this
    }
}
