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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.util.fastForEachReversed

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
 * [maxHorizontalPartitions], [adaptStrategies] and [currentDestination]. The returned value can be
 * used as a unique representation of the current layout structure.
 *
 * The function will treat the current destination as the highest priority and then adapt the rest
 * panes according to the order of [ThreePaneScaffoldRole.Primary],
 * [ThreePaneScaffoldRole.Secondary] and [ThreePaneScaffoldRole.Tertiary]. If there are still
 * remaining partitions to put the pane, the pane will be set as [PaneAdaptedValue.Expanded],
 * otherwise it will be adapted according to its associated [AdaptStrategy].
 *
 * @param maxHorizontalPartitions The maximum allowed partitions along the horizontal axis, i.e.,
 *   how many expanded panes can be shown at the same time.
 * @param adaptStrategies The adapt strategies of each pane role that [ThreePaneScaffold] supports,
 *   the default value will be [ThreePaneScaffoldDefaults.threePaneScaffoldAdaptStrategies].
 * @param currentDestination The current destination item, which will be treated as having the
 *   highest priority, can be `null`.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculateThreePaneScaffoldValue(
    maxHorizontalPartitions: Int,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    currentDestination: ThreePaneScaffoldDestinationItem<*>?,
): ThreePaneScaffoldValue {
    var expandedCount = if (currentDestination != null) 1 else 0
    return buildThreePaneScaffoldValue { role ->
        when {
            role == currentDestination?.pane -> PaneAdaptedValue.Expanded
            expandedCount < maxHorizontalPartitions -> {
                expandedCount++
                PaneAdaptedValue.Expanded
            }
            else -> adaptStrategies[role].adapt()
        }
    }
}

/**
 * Calculates the current adapted value of [ThreePaneScaffold] according to the given
 * [maxHorizontalPartitions], [adaptStrategies] and [destinationHistory]. The returned value can be
 * used as a unique representation of the current layout structure.
 *
 * The function will treat the current focus as the highest priority and then adapt the rest panes
 * according to the order of [ThreePaneScaffoldRole.Primary], [ThreePaneScaffoldRole.Secondary] and
 * [ThreePaneScaffoldRole.Tertiary]. If there are still remaining partitions to put the pane, the
 * pane will be set as [PaneAdaptedValue.Expanded], otherwise it will be adapted according to its
 * associated [AdaptStrategy].
 *
 * @param maxHorizontalPartitions The maximum allowed partitions along the horizontal axis, i.e.,
 *   how many expanded panes can be shown at the same time.
 * @param adaptStrategies The adapt strategies of each pane role that [ThreePaneScaffold] supports,
 *   the default value will be [ThreePaneScaffoldDefaults.threePaneScaffoldAdaptStrategies].
 * @param destinationHistory The history of past destination items. The last destination will have
 *   the highest priority, and the second last destination will have the second highest priority,
 *   and so forth until all panes have a priority assigned. Note that the last destination is
 *   supposed to be the last item of the provided list.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculateThreePaneScaffoldValue(
    maxHorizontalPartitions: Int,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    destinationHistory: List<ThreePaneScaffoldDestinationItem<*>>,
): ThreePaneScaffoldValue {
    var expandedCount = 0
    var primaryPaneAdaptedValue: PaneAdaptedValue? = null
    var secondaryPaneAdaptedValue: PaneAdaptedValue? = null
    var tertiaryPaneAdaptedValue: PaneAdaptedValue? = null
    destinationHistory.fastForEachReversed {
        if (expandedCount >= maxHorizontalPartitions) {
            return@fastForEachReversed
        }
        when (it.pane) {
            ThreePaneScaffoldRole.Primary -> {
                if (primaryPaneAdaptedValue == null) {
                    primaryPaneAdaptedValue = PaneAdaptedValue.Expanded
                    expandedCount++
                }
            }
            ThreePaneScaffoldRole.Secondary -> {
                if (secondaryPaneAdaptedValue == null) {
                    secondaryPaneAdaptedValue = PaneAdaptedValue.Expanded
                    expandedCount++
                }
            }
            ThreePaneScaffoldRole.Tertiary -> {
                if (tertiaryPaneAdaptedValue == null) {
                    tertiaryPaneAdaptedValue = PaneAdaptedValue.Expanded
                    expandedCount++
                }
            }
        }
    }
    return ThreePaneScaffoldValue(
        primary =
            primaryPaneAdaptedValue
                ?: if (expandedCount < maxHorizontalPartitions) {
                    expandedCount++
                    PaneAdaptedValue.Expanded
                } else {
                    adaptStrategies[ThreePaneScaffoldRole.Primary].adapt()
                },
        secondary =
            secondaryPaneAdaptedValue
                ?: if (expandedCount < maxHorizontalPartitions) {
                    expandedCount++
                    PaneAdaptedValue.Expanded
                } else {
                    adaptStrategies[ThreePaneScaffoldRole.Secondary].adapt()
                },
        tertiary =
            tertiaryPaneAdaptedValue
                ?: if (expandedCount < maxHorizontalPartitions) {
                    expandedCount++
                    PaneAdaptedValue.Expanded
                } else {
                    adaptStrategies[ThreePaneScaffoldRole.Tertiary].adapt()
                }
    )
}

/**
 * The adapted value of [ThreePaneScaffold]. It contains each pane's adapted value.
 * [ThreePaneScaffold] will use the adapted values to decide which panes should be displayed and how
 * they should be displayed. With other input parameters of [ThreePaneScaffold] fixed, each possible
 * instance of this class should represent a unique state of [ThreePaneScaffold] and developers can
 * compare two [ThreePaneScaffoldValue] to decide if there is a layout structure change.
 *
 * For a Material-opinionated layout, it's suggested to use [calculateThreePaneScaffoldValue] to
 * calculate the current scaffold value.
 *
 * @param primary [PaneAdaptedValue] of the primary pane of [ThreePaneScaffold]
 * @param secondary [PaneAdaptedValue] of the secondary pane of [ThreePaneScaffold]
 * @param tertiary [PaneAdaptedValue] of the tertiary pane of [ThreePaneScaffold]
 * @constructor create an instance of [ThreePaneScaffoldValue]
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
class ThreePaneScaffoldValue(
    val primary: PaneAdaptedValue,
    val secondary: PaneAdaptedValue,
    val tertiary: PaneAdaptedValue
) : PaneScaffoldValue<ThreePaneScaffoldRole>, PaneExpansionStateKeyProvider {
    internal val expandedCount by lazy {
        var count = 0
        if (primary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (secondary == PaneAdaptedValue.Expanded) {
            count++
        }
        if (tertiary == PaneAdaptedValue.Expanded) {
            count++
        }
        count
    }

    override val paneExpansionStateKey by lazy {
        if (expandedCount != 2) {
            PaneExpansionStateKey.Default
        } else {
            val expandedPanes = Array<ThreePaneScaffoldRole?>(2) { null }
            var count = 0
            if (primary == PaneAdaptedValue.Expanded) {
                expandedPanes[count++] = ThreePaneScaffoldRole.Primary
            }
            if (secondary == PaneAdaptedValue.Expanded) {
                expandedPanes[count++] = ThreePaneScaffoldRole.Secondary
            }
            if (tertiary == PaneAdaptedValue.Expanded) {
                expandedPanes[count] = ThreePaneScaffoldRole.Tertiary
            }
            TwoPaneExpansionStateKeyImpl(expandedPanes[0]!!, expandedPanes[1]!!)
        }
    }

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

    override fun toString(): String {
        return "ThreePaneScaffoldValue(primary=$primary, " +
            "secondary=$secondary, " +
            "tertiary=$tertiary)"
    }

    override operator fun get(role: ThreePaneScaffoldRole): PaneAdaptedValue =
        when (role) {
            ThreePaneScaffoldRole.Primary -> primary
            ThreePaneScaffoldRole.Secondary -> secondary
            ThreePaneScaffoldRole.Tertiary -> tertiary
        }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class TwoPaneExpansionStateKeyImpl(
    val firstExpandedPane: ThreePaneScaffoldRole,
    val secondExpandedPane: ThreePaneScaffoldRole
) : PaneExpansionStateKey {
    override fun hashCode(): Int {
        return firstExpandedPane.hashCode() * 31 + secondExpandedPane.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKey = other as? TwoPaneExpansionStateKeyImpl ?: return false
        return firstExpandedPane == otherKey.firstExpandedPane &&
            secondExpandedPane == otherKey.secondExpandedPane
    }

    companion object {
        fun saver(): Saver<TwoPaneExpansionStateKeyImpl, Any> =
            listSaver(
                save = { listOf(it.firstExpandedPane, it.secondExpandedPane) },
                restore = {
                    TwoPaneExpansionStateKeyImpl(
                        firstExpandedPane = it[0],
                        secondExpandedPane = it[1]
                    )
                }
            )
    }
}
