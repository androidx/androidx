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

import androidx.collection.IntList
import androidx.collection.buildIntSet
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.PaneMotionDefaults
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldAdaptStrategies
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculateDefaultEnterTransition
import androidx.compose.material3.adaptive.layout.calculateDefaultExitTransition
import androidx.compose.material3.adaptive.layout.calculateThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal sealed interface ThreePaneScaffoldType {
    class ListDetail(val detailPlaceholder: @Composable ThreePaneScaffoldScope.() -> Unit) :
        ThreePaneScaffoldType

    object SupportingPane : ThreePaneScaffoldType
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class ThreePaneScaffoldScene<T : Any>(
    override val key: Any,
    val onBack: () -> Unit,
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
    val paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)?,
    val paneExpansionState: PaneExpansionState?,
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

        val gestureInfo = remember(key, entries) { ThreePaneScaffoldSceneInfo(key, entries) }
        val gestureState = rememberNavigationEventState(currentInfo = gestureInfo)
        NavigationBackHandler(
            state = gestureState,
            isBackEnabled = previousScaffoldValue != null,
            onBackCompleted = {
                repeat(allEntries.size - onBackResult.previousEntries.size) { onBack() }
            },
        )

        val transitionState = gestureState.transitionState
        LaunchedEffect(transitionState) {
            // Update the scaffold based on the gesture's state:
            if (transitionState is NavigationEventTransitionState.InProgress) {
                // InProgress: Scrub the scaffold's position in real-time.
                scaffoldState.seekTo(
                    fraction =
                        backProgressToStateProgress(
                            progress = transitionState.latestEvent.progress,
                            scaffoldValue = scaffoldValue,
                        ),
                    targetState = previousScaffoldValue!!,
                )
            } else {
                // Completed/Cancelled: Animate back to the stable state.
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
            listPane =
                lastList?.let {
                    {
                        val sceneScope = ListDetailSceneScopeImpl(this)
                        CompositionLocalProvider(LocalListDetailSceneScope provides sceneScope) {
                            AnimatedPane(
                                modifier = preferredSizeModifier(lastList),
                                enterTransition =
                                    lastList.enterTransition()
                                        ?: motionDataProvider.calculateDefaultEnterTransition(
                                            paneRole
                                        ),
                                exitTransition =
                                    lastList.exitTransition()
                                        ?: motionDataProvider.calculateDefaultExitTransition(
                                            paneRole
                                        ),
                                boundsAnimationSpec =
                                    lastList.boundsAnimationSpec()
                                        ?: PaneMotionDefaults.AnimationSpec,
                            ) {
                                it.Content()
                            }
                        }
                    }
                } ?: {},
            detailPane = {
                val sceneScope = ListDetailSceneScopeImpl(this)
                CompositionLocalProvider(LocalListDetailSceneScope provides sceneScope) {
                    AnimatedPane(
                        modifier = preferredSizeModifier(lastDetail),
                        enterTransition =
                            lastDetail.enterTransition()
                                ?: motionDataProvider.calculateDefaultEnterTransition(paneRole),
                        exitTransition =
                            lastDetail.exitTransition()
                                ?: motionDataProvider.calculateDefaultExitTransition(paneRole),
                        boundsAnimationSpec =
                            lastList.boundsAnimationSpec() ?: PaneMotionDefaults.AnimationSpec,
                    ) {
                        lastDetail?.Content() ?: detailPlaceholder()
                    }
                }
            },
            extraPane =
                lastExtra?.let {
                    {
                        val sceneScope = ListDetailSceneScopeImpl(this)
                        CompositionLocalProvider(LocalListDetailSceneScope provides sceneScope) {
                            AnimatedPane(
                                modifier = preferredSizeModifier(lastExtra),
                                enterTransition =
                                    lastExtra.enterTransition()
                                        ?: motionDataProvider.calculateDefaultEnterTransition(
                                            paneRole
                                        ),
                                exitTransition =
                                    lastExtra.exitTransition()
                                        ?: motionDataProvider.calculateDefaultExitTransition(
                                            paneRole
                                        ),
                                boundsAnimationSpec =
                                    lastExtra.boundsAnimationSpec()
                                        ?: PaneMotionDefaults.AnimationSpec,
                            ) {
                                it.Content()
                            }
                        }
                    }
                },
            paneExpansionDragHandle = paneExpansionDragHandle,
            paneExpansionState = paneExpansionState,
        )
    }

    @Composable
    private fun SupportingPaneContent(scaffoldState: ThreePaneScaffoldState) {
        val lastMain = entries.findLast { getPaneRole(it) == SupportingPaneScaffoldRole.Main }
        val lastSupporting =
            entries.findLast { getPaneRole(it) == SupportingPaneScaffoldRole.Supporting }
        val lastExtra = entries.findLast { getPaneRole(it) == SupportingPaneScaffoldRole.Extra }

        SupportingPaneScaffold(
            directive = directive,
            scaffoldState = scaffoldState,
            mainPane =
                lastMain?.let {
                    {
                        val sceneScope = SupportingPaneSceneScopeImpl(this)
                        CompositionLocalProvider(
                            LocalSupportingPaneSceneScope provides sceneScope
                        ) {
                            AnimatedPane(
                                modifier = preferredSizeModifier(lastMain),
                                enterTransition =
                                    lastMain.enterTransition()
                                        ?: motionDataProvider.calculateDefaultEnterTransition(
                                            paneRole
                                        ),
                                exitTransition =
                                    lastMain.exitTransition()
                                        ?: motionDataProvider.calculateDefaultExitTransition(
                                            paneRole
                                        ),
                                boundsAnimationSpec =
                                    lastMain.boundsAnimationSpec()
                                        ?: PaneMotionDefaults.AnimationSpec,
                            ) {
                                it.Content()
                            }
                        }
                    }
                } ?: {},
            supportingPane =
                lastSupporting?.let {
                    {
                        val sceneScope = SupportingPaneSceneScopeImpl(this)
                        CompositionLocalProvider(
                            LocalSupportingPaneSceneScope provides sceneScope
                        ) {
                            AnimatedPane(
                                modifier = preferredSizeModifier(lastSupporting),
                                enterTransition =
                                    lastSupporting.enterTransition()
                                        ?: motionDataProvider.calculateDefaultEnterTransition(
                                            paneRole
                                        ),
                                exitTransition =
                                    lastSupporting.exitTransition()
                                        ?: motionDataProvider.calculateDefaultExitTransition(
                                            paneRole
                                        ),
                                boundsAnimationSpec =
                                    lastSupporting.boundsAnimationSpec()
                                        ?: PaneMotionDefaults.AnimationSpec,
                            ) {
                                it.Content()
                            }
                        }
                    }
                } ?: {},
            extraPane =
                lastExtra?.let {
                    {
                        val sceneScope = SupportingPaneSceneScopeImpl(this)
                        CompositionLocalProvider(
                            LocalSupportingPaneSceneScope provides sceneScope
                        ) {
                            AnimatedPane(
                                modifier = preferredSizeModifier(lastExtra),
                                enterTransition =
                                    lastExtra.enterTransition()
                                        ?: motionDataProvider.calculateDefaultEnterTransition(
                                            paneRole
                                        ),
                                exitTransition =
                                    lastExtra.exitTransition()
                                        ?: motionDataProvider.calculateDefaultExitTransition(
                                            paneRole
                                        ),
                                boundsAnimationSpec =
                                    lastExtra.boundsAnimationSpec()
                                        ?: PaneMotionDefaults.AnimationSpec,
                            ) {
                                it.Content()
                            }
                        }
                    }
                },
            paneExpansionDragHandle = paneExpansionDragHandle,
            paneExpansionState = paneExpansionState,
        )
    }

    @Suppress("ModifierFactoryExtensionFunction")
    private fun ThreePaneScaffoldScope.preferredSizeModifier(entry: NavEntry<T>?): Modifier {
        if (entry == null) return Modifier

        val widthModifier =
            when (val width = entry.metadata[MetadataPreferredWidthKey]) {
                is Float -> Modifier.preferredWidth(width)
                is Dp -> Modifier.preferredWidth(width)
                else -> Modifier
            }

        val heightModifier =
            when (val height = entry.metadata[MetadataPreferredHeightKey]) {
                is Float -> Modifier.preferredHeight(height)
                is Dp -> Modifier.preferredHeight(height)
                else -> Modifier
            }
        return widthModifier.then(heightModifier)
    }

    private fun NavEntry<T>?.enterTransition(): EnterTransition? =
        this?.metadata[MetadataEnterTransitionKey] as? EnterTransition

    private fun NavEntry<T>?.exitTransition(): ExitTransition? =
        this?.metadata[MetadataExitTransitionKey] as? ExitTransition

    @Suppress("UNCHECKED_CAST")
    private fun NavEntry<T>?.boundsAnimationSpec(): FiniteAnimationSpec<IntRect>? =
        this?.metadata[MetadataBoundsAnimationSpecKey] as? FiniteAnimationSpec<IntRect>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ThreePaneScaffoldScene<*>

        return key == other.key &&
            backNavBehavior == other.backNavBehavior &&
            directive == other.directive &&
            adaptStrategies == other.adaptStrategies &&
            allEntries == other.allEntries &&
            previousEntries == other.previousEntries &&
            scaffoldEntries == other.scaffoldEntries &&
            scaffoldEntryIndices == scaffoldEntryIndices &&
            entriesAsNavItems == other.entriesAsNavItems
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
            backNavBehavior.hashCode() * 31 +
            directive.hashCode() * 31 +
            adaptStrategies.hashCode() * 31 +
            allEntries.hashCode() * 31 +
            previousEntries.hashCode() * 31 +
            scaffoldEntries.hashCode() * 31 +
            scaffoldEntryIndices.hashCode() * 31 +
            entriesAsNavItems.hashCode() * 31
    }

    override fun toString(): String {
        return "ThreePaneScaffoldScene(key=$key, backNavBehavior=$backNavBehavior" +
            ", directive=$directive, adaptStrategies=$adaptStrategies, allEntries=$allEntries, " +
            "previousEntries=$previousEntries, scaffoldEntries=$scaffoldEntries, " +
            "scaffoldEntryIndices=$scaffoldEntryIndices, entriesAsNavItems=$entriesAsNavItems)"
    }
}

internal const val MetadataEnterTransitionKey: String =
    "androidx.compose.material3.adaptive.layout.PaneMotion.enterTransition"

internal const val MetadataExitTransitionKey: String =
    "androidx.compose.material3.adaptive.layout.PaneMotion.exitTransition"

internal const val MetadataBoundsAnimationSpecKey: String =
    "androidx.compose.material3.adaptive.layout.PaneMotion.boundsAnimationSpec"

internal const val MetadataPreferredWidthKey: String =
    "androidx.compose.material3.adaptive.layout.preferredWidth"

internal const val MetadataPreferredHeightKey: String =
    "androidx.compose.material3.adaptive.layout.preferredHeight"

private data class ThreePaneScaffoldSceneInfo(val key: Any, val entries: List<NavEntry<*>>) :
    NavigationEventInfo()

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
