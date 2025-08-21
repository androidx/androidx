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

package androidx.compose.material3.adaptive.navigation3

import androidx.activity.compose.PredictiveBackHandler
import androidx.collection.IntList
import androidx.collection.buildIntSet
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldAdaptStrategies
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.Scene
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal sealed interface ThreePaneScaffoldType {
    class ListDetail(val detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit) :
        ThreePaneScaffoldType

    object SupportingPane : ThreePaneScaffoldType
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class ThreePaneScaffoldScene<T : Any>(
    override val key: Any,
    val onBack: (Int) -> Unit,
    val backNavBehavior: BackNavigationBehavior,
    val directive: PaneScaffoldDirective,
    val adaptStrategies: ThreePaneScaffoldAdaptStrategies,
    /** All backstack entries, including those not relevant to the three pane scaffold scene. */
    val allEntries: List<NavEntry<T>>,
    /** The entries in the backstack that are handled by this three pane scaffold scene. */
    val scaffoldEntries: List<NavEntry<T>>,
    /** The indices of [allEntries] that result in [scaffoldEntries]. */
    val scaffoldEntryIndices: IntList,
    /** [scaffoldEntries], but each entry is converted to [ThreePaneScaffoldDestinationItem]. */
    val entriesAsNavItems: List<ThreePaneScaffoldDestinationItem<Any>>,
    val getPaneRole: (NavEntry<T>) -> ThreePaneScaffoldRole?,
    val scaffoldType: ThreePaneScaffoldType,
) : Scene<T> {
    override val entries: List<NavEntry<T>>
        get() = scaffoldEntries

    override val previousEntries: List<NavEntry<T>>
        get() = onBackResult.previousEntries

    val currentScaffoldValue: ThreePaneScaffoldValue
        get() = calculateScaffoldValue(destinationHistory = entriesAsNavItems)

    class OnBackResult<T : Any>(
        /**
         * The previous scaffold value of the three pane scaffold once a back event is handled.
         *
         * If this value is null, it means that either:
         * - there is no previous NavEntry in the backstack, or
         * - the back event leaves the three pane scaffold scene and is therefore not handled
         *   internally.
         */
        val previousScaffoldValue: ThreePaneScaffoldValue?,

        /** The resulting backstack after the back event is handled. */
        val previousEntries: List<NavEntry<T>>,
    )

    val onBackResult: OnBackResult<T> = calculateOnBackResult()

    private fun calculateOnBackResult(): OnBackResult<T> {
        // index relative to `scaffoldEntries`
        val prevDestRelativeIndex = getPreviousDestinationIndex()

        // index relative to `allEntries`
        val prevDestAbsoluteIndex =
            if (prevDestRelativeIndex < 0) {
                scaffoldEntryIndices.first() - 1
            } else {
                scaffoldEntryIndices[prevDestRelativeIndex]
            }

        val scaffoldEntryIndicesSet = buildIntSet { scaffoldEntryIndices.forEach { add(it) } }

        for (index in allEntries.lastIndex downTo 0) {
            if (index !in scaffoldEntryIndicesSet) {
                // Back event leaves the scaffold
                return OnBackResult(
                    previousScaffoldValue = null,
                    previousEntries = ArrayList(allEntries.subList(0, index + 1)),
                )
            }
            if (index == prevDestAbsoluteIndex) {
                // Back event stays within the scaffold -- handled internally
                val previousScaffoldValue =
                    calculateScaffoldValue(
                        destinationHistory = entriesAsNavItems.subList(0, prevDestRelativeIndex + 1)
                    )
                return OnBackResult(
                    previousScaffoldValue = previousScaffoldValue,
                    previousEntries = ArrayList(allEntries.subList(0, index + 1)),
                )
            }
        }

        // No previous entry in backstack
        return OnBackResult(previousScaffoldValue = null, previousEntries = emptyList())
    }

    private fun getPreviousDestinationIndex(): Int {
        if (entriesAsNavItems.size <= 1) {
            // No previous destination
            return -1
        }
        val currentDestination = entriesAsNavItems.last()
        val currentScaffoldValue = this.currentScaffoldValue

        when (backNavBehavior) {
            BackNavigationBehavior.PopLatest -> return entriesAsNavItems.lastIndex - 1
            BackNavigationBehavior.PopUntilScaffoldValueChange ->
                for (previousDestinationIndex in entriesAsNavItems.lastIndex - 1 downTo 0) {
                    val previousValue =
                        calculateScaffoldValue(
                            destinationHistory =
                                entriesAsNavItems.subList(0, previousDestinationIndex + 1)
                        )
                    if (previousValue != currentScaffoldValue) {
                        return previousDestinationIndex
                    }
                }
            BackNavigationBehavior.PopUntilCurrentDestinationChange ->
                for (previousDestinationIndex in entriesAsNavItems.lastIndex - 1 downTo 0) {
                    val destination = entriesAsNavItems[previousDestinationIndex].pane
                    if (destination != currentDestination.pane) {
                        return previousDestinationIndex
                    }
                }
            BackNavigationBehavior.PopUntilContentChange ->
                for (previousDestinationIndex in entriesAsNavItems.lastIndex - 1 downTo 0) {
                    val contentKey = entriesAsNavItems[previousDestinationIndex].contentKey
                    if (contentKey != currentDestination.contentKey) {
                        return previousDestinationIndex
                    }
                    // A scaffold value change also counts as a content change.
                    val previousValue =
                        calculateScaffoldValue(
                            destinationHistory =
                                entriesAsNavItems.subList(0, previousDestinationIndex + 1)
                        )
                    if (previousValue != currentScaffoldValue) {
                        return previousDestinationIndex
                    }
                }
        }

        return -1
    }

    private fun calculateScaffoldValue(
        destinationHistory: List<ThreePaneScaffoldDestinationItem<*>>
    ): ThreePaneScaffoldValue =
        calculateThreePaneScaffoldValue(
            maxHorizontalPartitions = directive.maxHorizontalPartitions,
            maxVerticalPartitions = directive.maxVerticalPartitions,
            adaptStrategies = adaptStrategies,
            destinationHistory = destinationHistory,
        )

    override val content: @Composable () -> Unit = {
        val scaffoldValue = currentScaffoldValue
        val scaffoldState = remember { MutableThreePaneScaffoldState(scaffoldValue) }
        LaunchedEffect(scaffoldValue) { scaffoldState.animateTo(scaffoldValue) }

        val previousScaffoldValue = onBackResult.previousScaffoldValue
        PredictiveBackHandler(enabled = previousScaffoldValue != null) { progress ->
            try {
                progress.collect { backEvent ->
                    scaffoldState.seekTo(
                        fraction =
                            backProgressToStateProgress(
                                progress = backEvent.progress,
                                scaffoldValue = scaffoldValue,
                            ),
                        targetState = previousScaffoldValue!!,
                    )
                }
                onBack(allEntries.size - onBackResult.previousEntries.size)
            } catch (_: CancellationException) {
                scaffoldState.animateTo(targetState = scaffoldValue)
            }
        }

        if (scaffoldType is ThreePaneScaffoldType.ListDetail) {
            ListDetailContent(scaffoldState, scaffoldType.detailPlaceholder)
        } else { // Supporting pane
            SupportingPaneContent(scaffoldState)
        }
    }

    @Suppress("ComposableLambdaParameterNaming")
    @Composable
    private fun ListDetailContent(
        scaffoldState: ThreePaneScaffoldState,
        detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit,
    ) {
        val lastList = entries.findLast { getPaneRole(it) == ListDetailPaneScaffoldRole.List }
        val lastDetail = entries.findLast { getPaneRole(it) == ListDetailPaneScaffoldRole.Detail }
        val lastExtra = entries.findLast { getPaneRole(it) == ListDetailPaneScaffoldRole.Extra }

        ListDetailPaneScaffold(
            directive = directive,
            scaffoldState = scaffoldState,
            listPane = lastList?.let { { AnimatedPane { it.Content() } } } ?: {},
            detailPane = lastDetail?.let { { AnimatedPane { it.Content() } } } ?: detailPlaceholder,
            extraPane = lastExtra?.let { { AnimatedPane { it.Content() } } },
        )
    }

    @Composable()
    private fun SupportingPaneContent(scaffoldState: ThreePaneScaffoldState) {
        val lastMain = entries.findLast { getPaneRole(it) == SupportingPaneScaffoldRole.Main }
        val lastSupporting =
            entries.findLast { getPaneRole(it) == SupportingPaneScaffoldRole.Supporting }
        val lastExtra = entries.findLast { getPaneRole(it) == SupportingPaneScaffoldRole.Extra }

        SupportingPaneScaffold(
            directive = directive,
            scaffoldState = scaffoldState,
            mainPane = lastMain?.let { { AnimatedPane { it.Content() } } } ?: {},
            supportingPane = lastSupporting?.let { { AnimatedPane { it.Content() } } } ?: {},
            extraPane = lastExtra?.let { { AnimatedPane { it.Content() } } },
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun backProgressToStateProgress(
    progress: Float,
    scaffoldValue: ThreePaneScaffoldValue,
): Float =
    ThreePaneScaffoldPredictiveBackEasing.transform(progress) *
        when (scaffoldValue.expandedCount) {
            1 -> SinglePaneProgressRatio
            2 -> DualPaneProgressRatio
            else -> TriplePaneProgressRatio
        }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ThreePaneScaffoldValue.expandedCount: Int
    get() {
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
        return count
    }

private val ThreePaneScaffoldPredictiveBackEasing: Easing = CubicBezierEasing(0.1f, 0.1f, 0f, 1f)
private const val SinglePaneProgressRatio: Float = 0.1f
private const val DualPaneProgressRatio: Float = 0.15f
private const val TriplePaneProgressRatio: Float = 0.2f
