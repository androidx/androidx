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

package androidx.compose.material3.adaptive

import androidx.compose.runtime.Immutable

/**
 * Represents the pane order of [ThreePaneScaffold] from start to end. Note that the values of
 * [firstPane], [secondPane] and [thirdPane] have to be different, otherwise
 * [IllegalArgumentException] will be thrown.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneScaffoldArrangement(
    /** The first pane from the start of the [ThreePaneScaffold]. */
    val firstPane: ThreePaneScaffoldRole,
    /** The second pane from the start of the [ThreePaneScaffold]. */
    val secondPane: ThreePaneScaffoldRole,
    /** The third pane from the start of the [ThreePaneScaffold]. */
    val thirdPane: ThreePaneScaffoldRole
) {
    init {
        require(firstPane != secondPane && secondPane != thirdPane && firstPane != thirdPane) {
            "invalid ThreePaneScaffoldArrangement($firstPane, $secondPane, $thirdPane)" +
                " - panes must be unique"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldArrangement) return false
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
 * The set of the available pane roles of [ThreePaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
enum class ThreePaneScaffoldRole {
    /**
     * The primary pane of [ThreePaneScaffold]. It is supposed to have the highest priority during
     * layout adaptation and usually contains the most important content of the screen, like content
     * details in a list-detail settings.
     */
    Primary,
    /**
     * The secondary pane of [ThreePaneScaffold]. It is supposed to have the second highest priority
     * during layout adaptation and usually contains the supplement content of the screen, like
     * content list in a list-detail settings.
     */
    Secondary,
    /**
     * The tertiary pane of [ThreePaneScaffold]. It is supposed to have the lowest priority during
     * layout adaptation and usually contains the additional info which will only be shown under
     * user interaction.
     */
    Tertiary
}
