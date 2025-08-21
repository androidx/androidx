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
 *   the default value will be [ThreePaneScaffoldDefaults.adaptStrategies].
 * @param currentDestination The current destination item, which will be treated as having the
 *   highest priority, can be `null`.
 * @param maxVerticalPartitions The maximum allowed partitions along the vertical axis, by default
 *   it will be 1 and in this case no reflowed panes will be allowed; if the value equals to or
 *   larger than 2, reflowed panes are allowed, besides the expanded pane in the same horizontal
 *   partition.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculateThreePaneScaffoldValue(
    maxHorizontalPartitions: Int,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    currentDestination: ThreePaneScaffoldDestinationItem<*>?,
    maxVerticalPartitions: Int = 1,
): ThreePaneScaffoldValue =
    calculateThreePaneScaffoldValue(
        maxHorizontalPartitions,
        adaptStrategies,
        listOfNotNull(currentDestination),
        maxVerticalPartitions,
    )

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
 *   the default value will be [ThreePaneScaffoldDefaults.adaptStrategies].
 * @param destinationHistory The history of past destination items. The last destination will have
 *   the highest priority, and the second last destination will have the second highest priority,
 *   and so forth until all panes have a priority assigned. Note that the last destination is
 *   supposed to be the last item of the provided list.
 * @param maxVerticalPartitions The maximum allowed partitions along the vertical axis, by default
 *   it will be 1 and in this case no reflowed panes will be allowed; if the value equals to or
 *   larger than 2, reflowed panes are allowed, besides the expanded pane in the same horizontal
 *   partition.
 */
@ExperimentalMaterial3AdaptiveApi
fun calculateThreePaneScaffoldValue(
    maxHorizontalPartitions: Int,
    adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    destinationHistory: List<ThreePaneScaffoldDestinationItem<*>>,
    maxVerticalPartitions: Int = 1,
): ThreePaneScaffoldValue {
    var expandedCount = 0
    var primaryPaneAdaptedValue: PaneAdaptedValue? = null
    var secondaryPaneAdaptedValue: PaneAdaptedValue? = null
    var tertiaryPaneAdaptedValue: PaneAdaptedValue? = null

    fun getAdaptedValue(role: ThreePaneScaffoldRole) =
        when (role) {
            ThreePaneScaffoldRole.Primary -> primaryPaneAdaptedValue
            ThreePaneScaffoldRole.Secondary -> secondaryPaneAdaptedValue
            ThreePaneScaffoldRole.Tertiary -> tertiaryPaneAdaptedValue
        }

    fun setAdaptedValue(role: ThreePaneScaffoldRole, value: PaneAdaptedValue) {
        when (role) {
            ThreePaneScaffoldRole.Primary -> primaryPaneAdaptedValue = value
            ThreePaneScaffoldRole.Secondary -> secondaryPaneAdaptedValue = value
            ThreePaneScaffoldRole.Tertiary -> tertiaryPaneAdaptedValue = value
        }
    }

    var checkReflowedPane =
        maxHorizontalPartitions == 1 &&
            maxVerticalPartitions > 1 &&
            (adaptStrategies[ThreePaneScaffoldRole.Primary] is AdaptStrategy.Reflow ||
                adaptStrategies[ThreePaneScaffoldRole.Secondary] is AdaptStrategy.Reflow ||
                adaptStrategies[ThreePaneScaffoldRole.Tertiary] is AdaptStrategy.Reflow)

    // Only levitate a pane when it is the current destination
    destinationHistory.lastOrNull()?.apply {
        (adaptStrategies[pane] as? AdaptStrategy.Levitate)?.apply {
            setAdaptedValue(pane, PaneAdaptedValue.Levitated(alignment, scrim))
        }
    }

    run {
        forEachPaneByPriority(destinationHistory) { pane ->
            val hasAvailablePartition = expandedCount < maxHorizontalPartitions
            if (!hasAvailablePartition && !checkReflowedPane) {
                return@run // No need to check more panes, break;
            }
            if (getAdaptedValue(pane) != null) {
                return@forEachPaneByPriority // Pane already adapted, continue;
            }
            var reflowedPane: ThreePaneScaffoldRole? = null
            var anchorPane: ThreePaneScaffoldRole = pane
            var anchorPaneValue: PaneAdaptedValue? = null
            if (checkReflowedPane) {
                (adaptStrategies[pane] as? AdaptStrategy.Reflow)?.apply {
                    (this.reflowUnder as? ThreePaneScaffoldRole)?.apply {
                        reflowedPane = pane
                        anchorPane = this
                        anchorPaneValue = getAdaptedValue(anchorPane)
                    }
                }
            }
            when (anchorPaneValue) {
                null ->
                    if (adaptStrategies[anchorPane] is AdaptStrategy.Levitate) {
                        // The anchor pane can only be levitated, continue;
                        return@forEachPaneByPriority
                    } else if (hasAvailablePartition) {
                        // Expand the anchor pane to reflow the pane
                        setAdaptedValue(anchorPane, PaneAdaptedValue.Expanded)
                        expandedCount++
                    } else {
                        // Cannot expand the anchor pane, continue;
                        return@forEachPaneByPriority
                    }
                PaneAdaptedValue.Expanded -> {
                    // Anchor pane is expanded, do nothing
                }
                else -> return@forEachPaneByPriority // Anchor pane is not expanded, continue;
            }
            reflowedPane?.apply {
                setAdaptedValue(this, PaneAdaptedValue.Reflowed(anchorPane))
                checkReflowedPane = false
            }
        }
    }
    return ThreePaneScaffoldValue(
        primary = primaryPaneAdaptedValue ?: PaneAdaptedValue.Hidden,
        secondary = secondaryPaneAdaptedValue ?: PaneAdaptedValue.Hidden,
        tertiary = tertiaryPaneAdaptedValue ?: PaneAdaptedValue.Hidden,
    )
}

@ExperimentalMaterial3AdaptiveApi
private inline fun forEachPaneByPriority(
    destinationHistory: List<ThreePaneScaffoldDestinationItem<*>>,
    action: (ThreePaneScaffoldRole) -> Unit,
) {
    destinationHistory.fastForEachReversed { action(it.pane) }
    action(ThreePaneScaffoldRole.Primary)
    action(ThreePaneScaffoldRole.Secondary)
    action(ThreePaneScaffoldRole.Tertiary)
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
    val tertiary: PaneAdaptedValue,
) : PaneScaffoldValue<ThreePaneScaffoldRole>, PaneExpansionStateKeyProvider {
    internal val expandedCount by lazy {
        var count = 0
        forEach { _, value ->
            if (value == PaneAdaptedValue.Expanded) {
                count++
            }
        }
        count
    }

    override val paneExpansionStateKey by lazy {
        if (expandedCount != 2) {
            PaneExpansionStateKey.Default
        } else {
            val expandedPanes = Array<ThreePaneScaffoldRole?>(2) { null }
            var count = 0
            forEach { role, value ->
                if (value == PaneAdaptedValue.Expanded) {
                    expandedPanes[count++] = role
                }
            }
            TwoPaneExpansionStateKeyImpl(expandedPanes[0]!!, expandedPanes[1]!!)
        }
    }

    internal inline fun forEach(action: (ThreePaneScaffoldRole, PaneAdaptedValue) -> Unit) {
        action(ThreePaneScaffoldRole.Primary, primary)
        action(ThreePaneScaffoldRole.Secondary, secondary)
        action(ThreePaneScaffoldRole.Tertiary, tertiary)
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
    val secondExpandedPane: ThreePaneScaffoldRole,
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
                        secondExpandedPane = it[1],
                    )
                },
            )
    }
}
