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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max
import kotlin.math.min

/**
 * A pane scaffold composable that can display up to three panes according to the instructions
 * provided by [ThreePaneScaffoldValue] in the order that [ThreePaneScaffoldHorizontalOrder]
 * specifies, and allocate margins and spacers according to [PaneScaffoldDirective].
 *
 * [ThreePaneScaffold] is the base composable functions of adaptive programming. Developers can
 * freely pipeline the relevant adaptive signals and use them as input of the scaffold function to
 * render the final adaptive layout.
 *
 * It's recommended to use [ThreePaneScaffold] with [calculatePaneScaffoldDirective],
 * [calculateThreePaneScaffoldValue] to follow the Material design guidelines on adaptive
 * programming.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param scaffoldDirective The top-level directives about how the scaffold should arrange its
 *   panes.
 * @param scaffoldValue The current adapted value of the scaffold.
 * @param paneOrder The horizontal order of the panes from start to end in the scaffold.
 * @param secondaryPane The content of the secondary pane that has a priority lower then the primary
 *   pane but higher than the tertiary pane.
 * @param tertiaryPane The content of the tertiary pane that has the lowest priority.
 * @param primaryPane The content of the primary pane that has the highest priority.
 */
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun ThreePaneScaffold(
    modifier: Modifier,
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    secondaryPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    tertiaryPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    paneExpansionState: PaneExpansionState? = null,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    primaryPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
) {
    val scaffoldState = remember { MutableThreePaneScaffoldState(scaffoldValue) }
    LaunchedEffect(key1 = scaffoldValue) { scaffoldState.animateTo(scaffoldValue) }
    ThreePaneScaffold(
        modifier = modifier,
        scaffoldDirective = scaffoldDirective,
        scaffoldState = scaffoldState,
        paneOrder = paneOrder,
        secondaryPane = secondaryPane,
        tertiaryPane = tertiaryPane,
        paneExpansionState = paneExpansionState,
        paneExpansionDragHandle = paneExpansionDragHandle,
        primaryPane = primaryPane,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun ThreePaneScaffold(
    modifier: Modifier,
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldState: ThreePaneScaffoldState,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    secondaryPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    tertiaryPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    paneExpansionState: PaneExpansionState? = null,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    primaryPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
) {
    val expansionState =
        paneExpansionState
            ?: rememberDefaultPaneExpansionState(
                keyProvider = { scaffoldState.targetState },
                mutable = paneExpansionDragHandle != null,
            )
    val layoutDirection = LocalLayoutDirection.current
    val ltrPaneOrder =
        remember(paneOrder, layoutDirection) { paneOrder.toLtrOrder(layoutDirection) }
    val paneMotions = scaffoldState.calculateThreePaneMotion(ltrPaneOrder)
    val motionDataProvider =
        remember { ThreePaneScaffoldMotionDataProvider() }
            .apply {
                // TODO(conradchen): Find a better way to provide predictive back state
                this.scaffoldState = scaffoldState
                update(paneMotions, ltrPaneOrder)
            }

    val currentTransition = scaffoldState.rememberTransition()
    val transitionScope =
        remember { ThreePaneScaffoldTransitionScopeImpl(motionDataProvider) }
            .apply {
                transitionState = scaffoldState
                scaffoldStateTransition = currentTransition
            }

    val stateHolder = rememberSaveableStateHolder()

    LookaheadScope {
        val scaffoldScope =
            remember(currentTransition, this) {
                ThreePaneScaffoldScopeImpl(
                    transitionScope,
                    this,
                    stateHolder,
                    mapOf(
                        ThreePaneScaffoldRole.Primary to FocusRequester(),
                        ThreePaneScaffoldRole.Secondary to FocusRequester(),
                        ThreePaneScaffoldRole.Tertiary to FocusRequester(),
                    ),
                )
            }
        val scaffoldValue = scaffoldState.targetState
        with(LocalThreePaneScaffoldOverride.current) {
            ThreePaneScaffoldOverrideScope(
                    modifier = modifier,
                    scaffoldDirective = scaffoldDirective,
                    scaffoldState = scaffoldState,
                    paneOrder = paneOrder,
                    primaryPane = {
                        rememberThreePaneScaffoldPaneScope(
                                ThreePaneScaffoldRole.Primary,
                                scaffoldScope,
                                paneMotions[ThreePaneScaffoldRole.Primary],
                                scaffoldValue.isInteractable(ThreePaneScaffoldRole.Primary),
                            )
                            .primaryPane()
                    },
                    secondaryPane = {
                        rememberThreePaneScaffoldPaneScope(
                                ThreePaneScaffoldRole.Secondary,
                                scaffoldScope,
                                paneMotions[ThreePaneScaffoldRole.Secondary],
                                scaffoldValue.isInteractable(ThreePaneScaffoldRole.Secondary),
                            )
                            .secondaryPane()
                    },
                    tertiaryPane =
                        if (tertiaryPane == null) null
                        else {
                            {
                                rememberThreePaneScaffoldPaneScope(
                                        ThreePaneScaffoldRole.Tertiary,
                                        scaffoldScope,
                                        paneMotions[ThreePaneScaffoldRole.Tertiary],
                                        scaffoldValue.isInteractable(ThreePaneScaffoldRole.Tertiary),
                                    )
                                    .tertiaryPane()
                            }
                        },
                    paneExpansionState = expansionState,
                    paneExpansionDragHandle =
                        if (paneExpansionDragHandle == null) null
                        else {
                            { paneExpansionState ->
                                scaffoldScope.paneExpansionDragHandle(paneExpansionState)
                            }
                        },
                    motionDataProvider = motionDataProvider,
                )
                .ThreePaneScaffold()

            LaunchedEffect(scaffoldValue.currentDestination) {
                scaffoldScope.focusRequesters[scaffoldValue.currentDestination]?.requestFocus()
            }
        }
    }
}

/**
 * This override provides the default behavior of the [ThreePaneScaffold] component.
 *
 * [ThreePaneScaffoldOverride] used when no override is specified.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalMaterial3AdaptiveComponentOverrideApi
private object DefaultThreePaneScaffoldOverride : ThreePaneScaffoldOverride {
    @Composable
    override fun ThreePaneScaffoldOverrideScope.ThreePaneScaffold() {
        val layoutDirection = LocalLayoutDirection.current
        val ltrPaneOrder =
            remember(paneOrder, layoutDirection) { paneOrder.toLtrOrder(layoutDirection) }
        val scaffoldValue = scaffoldState.targetState
        val contents =
            listOf<@Composable () -> Unit>(
                primaryPane,
                secondaryPane,
                tertiaryPane ?: {},
                { paneExpansionDragHandle?.invoke(paneExpansionState) },
                {
                    // A default scrim when no AnimatedPane is being used.
                    scaffoldValue.forEach { _, value ->
                        (value as? PaneAdaptedValue.Levitated)?.scrim?.invoke()
                        return@listOf
                    }
                },
            )

        val measurePolicy =
            remember(paneExpansionState) {
                    ThreePaneContentMeasurePolicy(
                        scaffoldDirective,
                        scaffoldValue,
                        paneExpansionState,
                        ltrPaneOrder,
                        motionDataProvider,
                    )
                }
                .apply {
                    this.scaffoldDirective = this@ThreePaneScaffold.scaffoldDirective
                    this.scaffoldValue = scaffoldValue
                    this.paneOrder = ltrPaneOrder
                }

        scaffoldState.CollectPredictiveBackScale(motionDataProvider.predictiveBackScaleState)

        Layout(
            contents = contents,
            modifier = modifier.predictiveBackScale(motionDataProvider.predictiveBackScaleState),
            measurePolicy = measurePolicy,
        )
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class ThreePaneContentMeasurePolicy(
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    val paneExpansionState: PaneExpansionState,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    val motionDataProvider: ThreePaneScaffoldMotionDataProvider,
) : MultiContentMeasurePolicy {
    var scaffoldDirective by mutableStateOf(scaffoldDirective)
    var scaffoldValue by mutableStateOf(scaffoldValue)
    var paneOrder by mutableStateOf(paneOrder)

    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val primaryMeasurable = measurables[0].firstOrNull()
        val secondaryMeasurable = measurables[1].firstOrNull()
        val tertiaryMeasurable = measurables[2].firstOrNull()
        val dragHandleMeasurable = measurables[3].firstOrNull()
        val scrimMeasurable = getScrimMeasurable(measurables)
        return layout(constraints.maxWidth, constraints.maxHeight) {
            if (coordinates == null) {
                return@layout
            }
            val scaffoldSize = IntSize(constraints.maxWidth, constraints.maxHeight)
            motionDataProvider.scaffoldSize = scaffoldSize
            val outerBounds = IntRect(0, 0, constraints.maxWidth, constraints.maxHeight)
            val expandedPanes =
                getPanesMeasurablesWithValue(
                    paneOrder = paneOrder,
                    primaryMeasurable = primaryMeasurable,
                    scaffoldValue = scaffoldValue,
                    secondaryMeasurable = secondaryMeasurable,
                    tertiaryMeasurable = tertiaryMeasurable,
                    scaffoldSize = scaffoldSize,
                    paneValue = PaneAdaptedValue.Expanded,
                )
            val reflowedPanes =
                getPanesMeasurables(
                    paneOrder = paneOrder,
                    primaryMeasurable = primaryMeasurable,
                    scaffoldValue = scaffoldValue,
                    secondaryMeasurable = secondaryMeasurable,
                    tertiaryMeasurable = tertiaryMeasurable,
                    scaffoldSize = scaffoldSize,
                ) {
                    it is PaneAdaptedValue.Reflowed
                }
            val levitatedPanes =
                getPanesMeasurables(
                    paneOrder = paneOrder,
                    primaryMeasurable = primaryMeasurable,
                    scaffoldValue = scaffoldValue,
                    secondaryMeasurable = secondaryMeasurable,
                    tertiaryMeasurable = tertiaryMeasurable,
                    scaffoldSize = scaffoldSize,
                ) {
                    it is PaneAdaptedValue.Levitated
                }
            val hiddenPanes =
                getPanesMeasurablesWithValue(
                    paneOrder = paneOrder,
                    primaryMeasurable = primaryMeasurable,
                    scaffoldValue = scaffoldValue,
                    secondaryMeasurable = secondaryMeasurable,
                    tertiaryMeasurable = tertiaryMeasurable,
                    scaffoldSize = scaffoldSize,
                    paneValue = PaneAdaptedValue.Hidden,
                )

            val dragHandle = dragHandleMeasurable?.let { DragHandleMeasurable(it, this@measure) }

            val verticalSpacerSize = scaffoldDirective.horizontalPartitionSpacerSize.roundToPx()
            val horizontalSpacerSize = scaffoldDirective.verticalPartitionSpacerSize.roundToPx()
            if (!isLookingAhead) {
                paneExpansionState.onMeasured(outerBounds.width, this@measure)
            }

            if (!paneExpansionState.isUnspecified() && expandedPanes.size == 2) {
                // Pane expansion should override everything
                if (paneExpansionState.currentDraggingOffset != PaneExpansionState.Unspecified) {
                    // Respect the user dragging result if there's any
                    val halfSpacerSize = verticalSpacerSize / 2
                    if (paneExpansionState.currentDraggingOffset <= halfSpacerSize) {
                        val bounds =
                            if (paneExpansionState.isDraggingOrSettling) {
                                outerBounds.copy(
                                    left =
                                        paneExpansionState.currentDraggingOffset * 2 +
                                            outerBounds.left
                                )
                            } else {
                                outerBounds
                            }
                        measureAndPlacePanesInPartition(
                            bounds,
                            horizontalSpacerSize,
                            expandedPanes[1],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    } else if (
                        paneExpansionState.currentDraggingOffset >=
                            outerBounds.width - halfSpacerSize
                    ) {
                        val bounds =
                            if (paneExpansionState.isDraggingOrSettling) {
                                outerBounds.copy(
                                    right =
                                        paneExpansionState.currentDraggingOffset * 2 -
                                            outerBounds.right
                                )
                            } else {
                                outerBounds
                            }
                        measureAndPlacePanesInPartition(
                            bounds,
                            horizontalSpacerSize,
                            expandedPanes[0],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    } else {
                        measureAndPlacePanesInPartition(
                            outerBounds.copy(
                                right = paneExpansionState.currentDraggingOffset - halfSpacerSize
                            ),
                            horizontalSpacerSize,
                            expandedPanes[0],
                            reflowedPanes,
                            isLookingAhead,
                        )
                        measureAndPlacePanesInPartition(
                            outerBounds.copy(
                                left = paneExpansionState.currentDraggingOffset + halfSpacerSize
                            ),
                            horizontalSpacerSize,
                            expandedPanes[1],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    }
                } else { // Pane expansion settings from non-dragging results
                    val availableWidth = constraints.maxWidth
                    if (
                        paneExpansionState.firstPaneWidth == 0 ||
                            paneExpansionState.firstPaneProportion == 0f
                    ) {
                        measureAndPlacePanesInPartition(
                            outerBounds,
                            horizontalSpacerSize,
                            expandedPanes[1],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    } else if (
                        paneExpansionState.firstPaneWidth >= availableWidth - verticalSpacerSize ||
                            paneExpansionState.firstPaneProportion >= 1f
                    ) {
                        measureAndPlacePanesInPartition(
                            outerBounds,
                            horizontalSpacerSize,
                            expandedPanes[0],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    } else {
                        val firstPaneWidth =
                            if (
                                paneExpansionState.firstPaneWidth != PaneExpansionState.Unspecified
                            ) {
                                paneExpansionState.firstPaneWidth
                            } else {
                                (paneExpansionState.firstPaneProportion *
                                        (availableWidth - verticalSpacerSize))
                                    .toInt()
                            }
                        val firstPaneRight = outerBounds.left + firstPaneWidth
                        measureAndPlacePanesInPartition(
                            outerBounds.copy(right = firstPaneRight),
                            horizontalSpacerSize,
                            expandedPanes[0],
                            reflowedPanes,
                            isLookingAhead,
                        )
                        measureAndPlacePanesInPartition(
                            outerBounds.copy(left = firstPaneRight + verticalSpacerSize),
                            horizontalSpacerSize,
                            expandedPanes[1],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    }
                }
            } else if (scaffoldDirective.excludedBounds.isNotEmpty()) {
                val layoutPartitions = mutableListOf<IntRect>()
                var actualLeft = outerBounds.left
                var actualRight = outerBounds.right
                val actualTop = outerBounds.top
                val actualBottom = outerBounds.bottom
                // Assume hinge bounds are sorted from left to right, non-overlapped.
                @Suppress("ListIterator")
                scaffoldDirective.excludedBounds.forEach { it ->
                    val excludedBound = getLocalBounds(it)
                    if (excludedBound.left <= actualLeft) {
                        // The hinge is at the left of the layout, adjust the left edge of
                        // the current partition to the actual displayable bounds.
                        actualLeft = max(actualLeft, excludedBound.right)
                    } else if (excludedBound.right >= actualRight) {
                        // The hinge is right at the right of the layout and there's no more
                        // room for more partitions, adjust the right edge of the current
                        // partition to the actual displayable bounds.
                        actualRight = min(excludedBound.left, actualRight)
                        return@forEach
                    } else {
                        // The hinge is inside the layout, add the current partition to the list
                        // and move the left edge of the next partition to the right of the
                        // hinge.
                        layoutPartitions.add(
                            IntRect(actualLeft, actualTop, excludedBound.left, actualBottom)
                        )
                        actualLeft =
                            max(excludedBound.right, excludedBound.left + verticalSpacerSize)
                    }
                }
                if (actualLeft < actualRight) {
                    // The last partition
                    layoutPartitions.add(IntRect(actualLeft, actualTop, actualRight, actualBottom))
                }
                if (layoutPartitions.isEmpty()) {
                    // Display nothing
                } else if (layoutPartitions.size == 1) {
                    measureAndPlacePartitionsInBounds(
                        layoutPartitions[0],
                        verticalSpacerSize,
                        horizontalSpacerSize,
                        expandedPanes,
                        reflowedPanes,
                        isLookingAhead,
                    )
                } else if (layoutPartitions.size < expandedPanes.size) {
                    // Note that the only possible situation is we have only two physical partitions
                    // but three expanded panes to show. In this case fit two panes in the larger
                    // partition.
                    if (layoutPartitions[0].width > layoutPartitions[1].width) {
                        measureAndPlacePartitionsInBounds(
                            layoutPartitions[0],
                            verticalSpacerSize,
                            horizontalSpacerSize,
                            expandedPanes.subList(0, 2),
                            reflowedPanes,
                            isLookingAhead,
                        )
                        measureAndPlacePanesInPartition(
                            layoutPartitions[1],
                            horizontalSpacerSize,
                            expandedPanes[2],
                            reflowedPanes,
                            isLookingAhead,
                        )
                    } else {
                        measureAndPlacePanesInPartition(
                            layoutPartitions[0],
                            horizontalSpacerSize,
                            expandedPanes[0],
                            reflowedPanes,
                            isLookingAhead,
                        )
                        measureAndPlacePartitionsInBounds(
                            layoutPartitions[1],
                            verticalSpacerSize,
                            horizontalSpacerSize,
                            expandedPanes.subList(1, 3),
                            reflowedPanes,
                            isLookingAhead,
                        )
                    }
                } else {
                    // Layout each visible pane in a physical partition
                    expandedPanes.fastForEachIndexed { index, paneMeasurable ->
                        measureAndPlacePanesInPartition(
                            layoutPartitions[index],
                            horizontalSpacerSize,
                            paneMeasurable,
                            reflowedPanes,
                            isLookingAhead,
                        )
                    }
                }
            } else {
                measureAndPlacePartitionsInBounds(
                    outerBounds,
                    verticalSpacerSize,
                    horizontalSpacerSize,
                    expandedPanes,
                    reflowedPanes,
                    isLookingAhead,
                )
            }

            if (expandedPanes.size == 2 && dragHandle != null) {
                val handleOffsetX =
                    if (
                        !paneExpansionState.isDraggingOrSettling ||
                            paneExpansionState.currentDraggingOffset ==
                                PaneExpansionState.Unspecified
                    ) {
                        val spacerMiddleOffset =
                            getSpacerMiddleOffsetX(expandedPanes[0], expandedPanes[1])
                        if (!isLookingAhead) {
                            paneExpansionState.onExpansionOffsetMeasured(spacerMiddleOffset)
                        }
                        spacerMiddleOffset
                    } else {
                        paneExpansionState.currentDraggingOffset
                    }
                measureAndPlaceDragHandleIfNeeded(
                    dragHandle = dragHandle,
                    contentBounds = outerBounds,
                    minHorizontalMargin = verticalSpacerSize / 2,
                    offsetX = handleOffsetX,
                )
            } else if (!isLookingAhead) {
                paneExpansionState.onExpansionOffsetMeasured(PaneExpansionState.Unspecified)
            }

            placeLevitatedPanes(
                measurables = levitatedPanes,
                scaffoldBounds = outerBounds,
                layoutDirection = layoutDirection,
                density = this@measure,
                isLookingAhead = isLookingAhead,
            )

            // Place the hidden panes to ensure a proper motion at the AnimatedVisibility,
            // otherwise the pane will be gone immediately when it's hidden.
            // The placement is done using the outerBounds, as the placementsCache holds
            // absolute position values.
            placeHiddenPanes(hiddenPanes)

            expandedPanes.fastForEach { with(it) { doMeasureAndPlace() } }
            reflowedPanes.fastForEach { with(it) { doMeasureAndPlace() } }
            dragHandle?.apply { doMeasureAndPlace() }
            scrimMeasurable?.apply {
                measure(Constraints.fixed(outerBounds.width, outerBounds.height)).place(0, 0)
            }
            levitatedPanes.fastForEach { with(it) { doMeasureAndPlace() } }
            hiddenPanes.fastForEach { with(it) { doMeasureAndPlace() } }
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun MeasureScope.getPanesMeasurablesWithValue(
        paneOrder: ThreePaneScaffoldHorizontalOrder,
        primaryMeasurable: Measurable?,
        scaffoldValue: ThreePaneScaffoldValue,
        secondaryMeasurable: Measurable?,
        tertiaryMeasurable: Measurable?,
        scaffoldSize: IntSize,
        paneValue: PaneAdaptedValue,
    ) =
        getPanesMeasurables(
            paneOrder,
            primaryMeasurable,
            scaffoldValue,
            secondaryMeasurable,
            tertiaryMeasurable,
            scaffoldSize,
        ) {
            it == paneValue
        }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun MeasureScope.getPanesMeasurables(
        paneOrder: ThreePaneScaffoldHorizontalOrder,
        primaryMeasurable: Measurable?,
        scaffoldValue: ThreePaneScaffoldValue,
        secondaryMeasurable: Measurable?,
        tertiaryMeasurable: Measurable?,
        scaffoldSize: IntSize,
        predicate: (PaneAdaptedValue) -> Boolean,
    ): List<PaneMeasurable> {
        return buildList {
            paneOrder.forEach { role ->
                val paneValue = scaffoldValue[role]
                if (predicate(paneValue)) {
                    when (role) {
                        ThreePaneScaffoldRole.Primary -> {
                            createPaneMeasurableIfNeeded(
                                primaryMeasurable,
                                ThreePaneScaffoldDefaults.PrimaryPanePriority,
                                role,
                                paneValue,
                                scaffoldDirective.defaultPanePreferredWidth.roundToPx(),
                                scaffoldDirective.defaultPanePreferredHeight.roundToPx(),
                                this@getPanesMeasurables,
                                scaffoldSize,
                            )
                        }
                        ThreePaneScaffoldRole.Secondary -> {
                            createPaneMeasurableIfNeeded(
                                secondaryMeasurable,
                                ThreePaneScaffoldDefaults.SecondaryPanePriority,
                                role,
                                paneValue,
                                scaffoldDirective.defaultPanePreferredWidth.roundToPx(),
                                scaffoldDirective.defaultPanePreferredHeight.roundToPx(),
                                this@getPanesMeasurables,
                                scaffoldSize,
                            )
                        }
                        ThreePaneScaffoldRole.Tertiary -> {
                            createPaneMeasurableIfNeeded(
                                tertiaryMeasurable,
                                ThreePaneScaffoldDefaults.TertiaryPanePriority,
                                role,
                                paneValue,
                                scaffoldDirective.defaultPanePreferredWidth.roundToPx(),
                                scaffoldDirective.defaultPanePreferredHeight.roundToPx(),
                                this@getPanesMeasurables,
                                scaffoldSize,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun MutableList<PaneMeasurable>.createPaneMeasurableIfNeeded(
        measurable: Measurable?,
        priority: Int,
        role: ThreePaneScaffoldRole,
        value: PaneAdaptedValue,
        defaultPreferredWidth: Int,
        defaultPreferredHeight: Int,
        density: Density,
        scaffoldSize: IntSize,
    ) {
        if (measurable != null) {
            add(
                PaneMeasurable(
                    measurable,
                    priority,
                    role,
                    value,
                    defaultPreferredWidth,
                    defaultPreferredHeight,
                    density,
                    scaffoldSize,
                )
            )
        }
    }

    private fun measureAndPlacePartitionsInBounds(
        bounds: IntRect,
        verticalSpacerSize: Int,
        horizontalSpacerSize: Int,
        expandedPanes: List<PaneMeasurable>,
        reflowedPanes: List<PaneMeasurable>,
        isLookingAhead: Boolean,
    ) {
        if (expandedPanes.isEmpty()) {
            return
        }
        val allocatableWidth = bounds.width - (expandedPanes.size - 1) * verticalSpacerSize
        val totalPreferredWidth = expandedPanes.sumOf { it.measuringWidth }
        if (allocatableWidth > totalPreferredWidth) {
            // Allocate the remaining space to the pane with the highest priority.
            expandedPanes.maxBy { it.priority }.measuringWidth +=
                allocatableWidth - totalPreferredWidth
        } else if (allocatableWidth < totalPreferredWidth) {
            // Scale down all panes to fit in the available space.
            val scale = allocatableWidth.toFloat() / totalPreferredWidth
            expandedPanes.fastForEach { it.measuringWidth = (it.measuringWidth * scale).toInt() }
        }
        var positionX = bounds.left
        expandedPanes.fastForEach { expandedPane ->
            val partitionWidth = expandedPane.measuringWidth
            measureAndPlacePanesInPartition(
                IntRect(positionX, bounds.top, positionX + partitionWidth, bounds.bottom),
                horizontalSpacerSize,
                expandedPane,
                reflowedPanes,
                isLookingAhead,
            )
            positionX += partitionWidth + verticalSpacerSize
        }
    }

    private fun measureAndPlacePanesInPartition(
        partitionBounds: IntRect,
        horizontalSpacerSize: Int,
        expandedPane: PaneMeasurable,
        reflowedPanes: List<PaneMeasurable>,
        isLookingAhead: Boolean,
    ) {
        val reflowedPane = if (reflowedPanes.isEmpty()) null else reflowedPanes[0]
        if ((reflowedPane?.value as? PaneAdaptedValue.Reflowed)?.reflowUnder == expandedPane.role) {
            // Measure the reflowed pane and adjust the expanded pane's height
            // TODO(conradchen): Avoid hinges
            val availableHeight = partitionBounds.height - horizontalSpacerSize
            expandedPane.measuringHeight =
                max(availableHeight - reflowedPane.measuringHeight, availableHeight / 2)
            reflowedPane.measuringHeight = availableHeight - expandedPane.measuringHeight
        } else {
            expandedPane.measuringHeight = partitionBounds.height
        }
        reflowedPane?.apply {
            measureAndPlacePane(
                partitionBounds.copy(
                    top = partitionBounds.top + expandedPane.measuringHeight + horizontalSpacerSize
                ),
                this,
                isLookingAhead,
            )
        }
        expandedPane.apply {
            measureAndPlacePane(
                partitionBounds.copy(bottom = partitionBounds.top + measuringHeight),
                this,
                isLookingAhead,
            )
        }
    }

    private fun measureAndPlacePane(
        paneBounds: IntRect,
        measurable: PaneMeasurable,
        isLookingAhead: Boolean,
    ) =
        measurable.apply {
            measuredBounds = paneBounds
            recordMeasureResult(isLookingAhead)
        }

    private fun placeLevitatedPanes(
        measurables: List<PaneMeasurable>,
        scaffoldBounds: IntRect,
        layoutDirection: LayoutDirection,
        density: Density,
        isLookingAhead: Boolean,
    ) {
        measurables.fastForEach {
            val measuringWidth = min(it.measuringWidth, scaffoldBounds.width)
            val measuringHeight = min(it.measuringHeight, scaffoldBounds.height)
            val paneSize =
                IntSize(
                    width =
                        it.dragToResizeState?.getDraggedWidth(
                            measuringWidth = measuringWidth,
                            defaultMinWidth =
                                with(density) {
                                    ThreePaneScaffoldDefaults.MinPaneWidth.roundToPx()
                                },
                            scaffoldWidth = scaffoldBounds.width,
                        ) ?: measuringWidth,
                    height =
                        it.dragToResizeState?.getDraggedHeight(
                            measuringHeight = measuringHeight,
                            defaultMinHeight =
                                with(density) {
                                    ThreePaneScaffoldDefaults.MinPaneHeight.roundToPx()
                                },
                            scaffoldHeight = scaffoldBounds.height,
                        ) ?: measuringHeight,
                )
            val alignment = (it.value as? PaneAdaptedValue.Levitated)?.alignment ?: Alignment.Center
            val offset = alignment.align(paneSize, scaffoldBounds.size, layoutDirection)
            measureAndPlacePane(IntRect(offset, paneSize), it, isLookingAhead)
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun placeHiddenPanes(measurables: List<PaneMeasurable>) {
        // When panes are being hidden, apply each pane's width and position from the cache to
        // maintain the those before it's hidden by the AnimatedVisibility.
        measurables.fastForEach {
            if (!it.isAnimatedPane) {
                // When panes are not animated, we don't need to measure and place them.
                return
            }
            val measuredData = motionDataProvider[it.role]
            it.measuredBounds =
                IntRect(
                    measuredData.targetLeft,
                    measuredData.targetTop,
                    measuredData.targetRight,
                    measuredData.targetBottom,
                )
            it.zIndex = measuredData.zIndex + ThreePaneScaffoldDefaults.HiddenPaneZIndexOffset
        }
    }

    private fun PaneMeasurable.recordMeasureResult(isLookingAhead: Boolean) {
        val paneMotionData = motionDataProvider[role]
        if (!isLookingAhead && value != PaneAdaptedValue.Hidden) {
            // Save zIndex for visible panes, so when it's hidden we can keep them at the same
            // zIndex level.
            paneMotionData.zIndex = zIndex
            return
        }
        measuredBounds?.apply {
            if (!paneMotionData.isOriginSizeAndPositionSet) {
                // During animation remeasuring can happen multiple times, with the measuring result
                // equals to the lookahead measure. We don't want to override the original
                // measurement
                // so we only use the very first measurement
                paneMotionData.originSize =
                    if (paneMotionData.isTargetSizeAndPositionSet) {
                        paneMotionData.targetSize
                    } else {
                        size
                    }
                paneMotionData.originPosition =
                    if (paneMotionData.isTargetSizeAndPositionSet) {
                        paneMotionData.targetPosition
                    } else {
                        topLeft
                    }
                paneMotionData.isOriginSizeAndPositionSet = true
            }
            paneMotionData.targetSize = size
            paneMotionData.targetPosition = topLeft
            paneMotionData.isTargetSizeAndPositionSet = true
        }
    }

    private fun Placeable.PlacementScope.getLocalBounds(bounds: Rect): IntRect {
        return bounds.translate(coordinates!!.windowToLocal(Offset.Zero)).roundToIntRect()
    }

    private fun Placeable.PlacementScope.measureAndPlaceDragHandleIfNeeded(
        dragHandle: DragHandleMeasurable,
        contentBounds: IntRect,
        minHorizontalMargin: Int,
        offsetX: Int,
    ) {
        if (offsetX == PaneExpansionState.Unspecified) {
            return
        }
        val clampedOffsetX =
            offsetX.coerceIn(
                contentBounds.left + minHorizontalMargin,
                contentBounds.right - minHorizontalMargin,
            )
        val appliedHorizontalMargin =
            min(clampedOffsetX - contentBounds.left, contentBounds.right - clampedOffsetX)
        // When drag down to the end, we want to keep a consistent margin from the middle of the
        // drag handle to the edge of the layout. This may incur the requirement to "expand" and
        // "shift" the touch target area as part of the original area may get cut. When the margin
        // to the layout edge is larger than half of the min touch target size, no adjustment is
        // needed. On the other hand, if it's smaller than half of the min touch target size, we
        // need to expand the whole touch target size to 2 * (minTouchTargetSize - marginSize),
        // therefore the actual "touchable" area will be
        // (marginSize + minTouchTargetSize - marginSize) = minTouchTargetSize.
        val minDragHandleWidth =
            if (appliedHorizontalMargin < dragHandle.minTouchTargetSize / 2) {
                2 * (dragHandle.minTouchTargetSize - appliedHorizontalMargin)
            } else {
                dragHandle.minTouchTargetSize
            }
        dragHandle.isVisible = true
        dragHandle.minWidth = minDragHandleWidth
        dragHandle.maxHeight = contentBounds.height
        dragHandle.placedPositionCenter = IntOffset(clampedOffsetX, contentBounds.center.y)
    }

    private fun getSpacerMiddleOffsetX(paneLeft: PaneMeasurable, paneRight: PaneMeasurable): Int {
        return when {
            paneLeft.measuredAndPlaced && paneRight.measuredAndPlaced ->
                (paneLeft.placedPositionX + paneLeft.measuredWidth + paneRight.placedPositionX) / 2
            paneLeft.measuredAndPlaced -> paneLeft.placedPositionX + paneLeft.measuredWidth
            paneRight.measuredAndPlaced -> 0
            else -> PaneExpansionState.Unspecified
        }
    }

    private fun getScrimMeasurable(measurables: List<List<Measurable>>): Measurable? {
        measurables.subList(0, 3).fastForEach {
            if (it.size > 1) {
                return it[1]
            }
        }
        return measurables[4].firstOrNull()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class PaneMeasurable(
    val measurable: Measurable,
    val priority: Int,
    val role: ThreePaneScaffoldRole,
    val value: PaneAdaptedValue,
    defaultPreferredWidth: Int,
    defaultPreferredHeight: Int,
    density: Density,
    scaffoldSize: IntSize,
) {
    private val data =
        ((measurable.parentData as? PaneScaffoldParentData) ?: PaneScaffoldParentDataImpl())

    var measuringWidth =
        if (data.preferredWidth.isSpecified) {
            with(density) { data.preferredWidth.roundToPx() }
        } else if (data.preferredWidthInProportion.isFinite()) {
            (scaffoldSize.width * data.preferredWidthInProportion).toInt()
        } else {
            defaultPreferredWidth
        }

    var measuringHeight =
        if (data.preferredHeight.isSpecified) {
            with(density) { data.preferredHeight.roundToPx() }
        } else if (data.preferredHeightInProportion.isFinite()) {
            (scaffoldSize.height * data.preferredHeightInProportion).toInt()
        } else {
            defaultPreferredHeight
        }

    // TODO(conradchen): uncomment it when we can expose PaneMargins
    // val margins: PaneMargins = data.paneMargins

    val isAnimatedPane = data.isAnimatedPane

    val measuredWidth
        get() = measuredBounds?.width ?: 0

    val measuredHeight
        get() = measuredBounds?.height ?: 0

    val placedPositionX
        get() = measuredBounds?.left ?: 0

    val placedPositionY
        get() = measuredBounds?.top ?: 0

    var zIndex: Float =
        when {
            (value is PaneAdaptedValue.Levitated) -> ThreePaneScaffoldDefaults.LevitatedPaneZIndex
            else -> 0f
        }

    val dragToResizeState: DragToResizeState? = null
    // TODO: uncomment this when publish the feature: "get() = data.dragToResizeState"

    val measuredAndPlaced
        get() = measuredBounds != null

    var measuredBounds: IntRect? = null

    fun Placeable.PlacementScope.doMeasureAndPlace() =
        measurable
            .measure(Constraints.fixed(measuredWidth, measuredHeight))
            .place(placedPositionX, placedPositionY, zIndex)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class DragHandleMeasurable(val measurable: Measurable, density: Density) {
    val minTouchTargetSize = with(density) { measurable.minTouchTargetSize.roundToPx() }

    var isVisible: Boolean = false
    var minWidth: Int = 0
    var maxHeight: Int = Int.MAX_VALUE
    var placedPositionCenter: IntOffset? = null

    val placedPositionCenterX: Int
        get() = placedPositionCenter?.x ?: 0

    val placedPositionCenterY: Int
        get() = placedPositionCenter?.y ?: 0

    fun Placeable.PlacementScope.doMeasureAndPlace() {
        if (!isVisible) {
            return
        }
        measurable.measure(Constraints(minWidth = minWidth, maxHeight = maxHeight)).also {
            it.place(
                x = placedPositionCenterX - it.width / 2,
                y = placedPositionCenterY - it.height / 2,
            )
        }
    }
}

/**
 * Provides default values of [ThreePaneScaffold] and the calculation functions of
 * [ThreePaneScaffoldValue].
 */
@ExperimentalMaterial3AdaptiveApi
internal object ThreePaneScaffoldDefaults {
    // TODO(conradchen): consider declaring a value class for priority
    const val PrimaryPanePriority = 10
    const val SecondaryPanePriority = 5
    const val TertiaryPanePriority = 1

    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies].
     *
     * @param primaryPaneAdaptStrategy the adapt strategy of the primary pane
     * @param secondaryPaneAdaptStrategy the adapt strategy of the secondary pane
     * @param tertiaryPaneAdaptStrategy the adapt strategy of the tertiary pane
     */
    fun adaptStrategies(
        primaryPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        secondaryPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        tertiaryPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            primaryPaneAdaptStrategy,
            secondaryPaneAdaptStrategy,
            tertiaryPaneAdaptStrategy,
        )

    const val LevitatedPaneZIndex = 1f

    /**
     * The z-index offset of hidden panes to make visible panes always show upon the hidden panes at
     * the same z-index level during pane animations.
     */
    const val HiddenPaneZIndexOffset = -0.1f

    val MinPaneWidth = 48.dp

    val MinPaneHeight = 48.dp
}

/**
 * Interface that allows libraries to override the behavior of [ThreePaneScaffold].
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [LocalThreePaneScaffoldOverride] in the Compose hierarchy.
 */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
interface ThreePaneScaffoldOverride {
    /** Behavior function that is called by the [ThreePaneScaffold] composable. */
    @Composable fun ThreePaneScaffoldOverrideScope.ThreePaneScaffold()
}

/**
 * Parameters available to [ThreePaneScaffold].
 *
 * @property modifier The modifier to be applied to the layout.
 * @property scaffoldDirective The top-level directives about how the scaffold should arrange its
 *   panes.
 * @property scaffoldState The current state of the scaffold, containing information about the
 *   adapted value of each pane of the scaffold and the transitions/animations in progress.
 * @property paneOrder The horizontal order of the panes from start to end in the scaffold.
 * @property secondaryPane The content of the secondary pane that has a priority lower then the
 *   primary pane but higher than the tertiary pane.
 * @property tertiaryPane The content of the tertiary pane that has the lowest priority.
 * @property primaryPane The content of the primary pane that has the highest priority.
 * @property paneExpansionDragHandle the pane expansion drag handle to allow users to drag to change
 *   pane expansion state, `null` by default.
 * @property paneExpansionState the state object of pane expansion state.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalMaterial3AdaptiveComponentOverrideApi
class ThreePaneScaffoldOverrideScope
internal constructor(
    val modifier: Modifier,
    val scaffoldDirective: PaneScaffoldDirective,
    val scaffoldState: ThreePaneScaffoldState,
    val paneOrder: ThreePaneScaffoldHorizontalOrder,
    val primaryPane: @Composable () -> Unit,
    val secondaryPane: @Composable () -> Unit,
    val tertiaryPane: (@Composable () -> Unit)?,
    val paneExpansionState: PaneExpansionState,
    val paneExpansionDragHandle: (@Composable (PaneExpansionState) -> Unit)?,
    internal val motionDataProvider: ThreePaneScaffoldMotionDataProvider,
)

/** CompositionLocal containing the currently-selected [ThreePaneScaffoldOverride]. */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
val LocalThreePaneScaffoldOverride: ProvidableCompositionLocal<ThreePaneScaffoldOverride> =
    compositionLocalOf {
        DefaultThreePaneScaffoldOverride
    }
