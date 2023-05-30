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

@ExperimentalMaterial3AdaptiveApi
private inline fun buildThreePaneScaffoldValue(
    buildAction: (ThreePaneScaffoldRole) -> PaneAdaptedValue
): ThreePaneScaffoldValue {
    return ThreePaneScaffoldValue(
        buildAction(ThreePaneScaffoldRole.Primary),
        buildAction(ThreePaneScaffoldRole.Secondary),
        buildAction(ThreePaneScaffoldRole.Tertiary)
    )
}

/**
 * Calculates the current adapted value of [ThreePaneScaffold] according to the given
 * [maxHorizontalPartitions], [adaptStrategies] and [currentFocus]. The returned value can be used
 * as a unique representation of the current layout structure.
 *
 * The function will treat the current focus as the highest priority and then adapt the rest
 * panes according to the order of [ThreePaneScaffoldRole.Primary],
 * [ThreePaneScaffoldRole.Secondary] and [ThreePaneScaffoldRole.Tertiary]. If there are still
 * remaining partitions to put the pane, the pane will be set as [PaneAdaptedValue.Expanded],
 * otherwise it will be adapted according to its associated [AdaptStrategy].
 *
 * @param maxHorizontalPartitions The maximum allowed partitions along the horizontal axis, i.e.
 *                                how many expanded panes can be shown at the same time.
 * @param adaptStrategies The adapt strategies of each pane role that [ThreePaneScaffold] supports.
 * @param currentFocus The current focused pane, which will be treated as the highest priority, can
 *                     be `null`.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculateThreePaneScaffoldValue(
    maxHorizontalPartitions: Int,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    currentFocus: ThreePaneScaffoldRole? = null,
): ThreePaneScaffoldValue {
    var expandedCount = if (currentFocus != null) 1 else 0
    return buildThreePaneScaffoldValue { role ->
        when {
            role == currentFocus -> PaneAdaptedValue.Expanded
            expandedCount < maxHorizontalPartitions -> {
                expandedCount++
                PaneAdaptedValue.Expanded
            }
            else -> adaptStrategies[role].adapt()
        }
    }
}

/**
 * The adapted value of [ThreePaneScaffold]. It contains each pane's adapted value.
 * [ThreePaneScaffold] will use the adapted values to decide which panes should be displayed
 * and how they should be displayed. With other input parameters of [ThreePaneScaffold] fixed,
 * each possible instance of this class should represent a unique state of [ThreePaneScaffold]
 * and developers can compare two [ThreePaneScaffoldValue] to decide if there is a layout structure
 * change.
 *
 * For a Material-opinionated layout, it's suggested to use [calculateThreePaneScaffoldValue] to
 * calculate the current scaffold value.
 *
 * @param primary [PaneAdaptedValue] of the primary pane of [ThreePaneScaffold]
 * @param secondary [PaneAdaptedValue] of the secondary pane of [ThreePaneScaffold]
 * @param tertiary [PaneAdaptedValue] of the tertiary pane of [ThreePaneScaffold]
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneScaffoldValue(
    val primary: PaneAdaptedValue,
    val secondary: PaneAdaptedValue,
    val tertiary: PaneAdaptedValue
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldValue) return false
        if (primary != other.primary) return false
        if (secondary != other.secondary) return false
        if (tertiary != other.tertiary) return false
        return true
    }

    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + secondary.hashCode()
        result = 31 * result + tertiary.hashCode()
        return result
    }

    operator fun get(role: ThreePaneScaffoldRole): PaneAdaptedValue =
        when (role) {
            ThreePaneScaffoldRole.Primary -> primary
            ThreePaneScaffoldRole.Secondary -> secondary
            ThreePaneScaffoldRole.Tertiary -> tertiary
        }
}
