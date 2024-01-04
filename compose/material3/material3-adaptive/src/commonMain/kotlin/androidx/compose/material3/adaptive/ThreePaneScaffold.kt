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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
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
 * freely pipeline the relevant adaptive signals and use them as input of the scaffold function
 * to render the final adaptive layout.
 *
 * It's recommended to use [ThreePaneScaffold] with [calculateStandardPaneScaffoldDirective],
 * [calculateThreePaneScaffoldValue] to follow the Material design guidelines on adaptive
 * programming.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param scaffoldDirective The top-level directives about how the scaffold should arrange its panes.
 * @param scaffoldValue The current adapted value of the scaffold.
 * @param paneOrder The horizontal order of the panes from start to end in the scaffold.
 * @param secondaryPane The content of the secondary pane that has a priority lower then the primary
 *                      pane but higher than the tertiary pane.
 * @param tertiaryPane The content of the tertiary pane that has the lowest priority.
 * @param primaryPane The content of the primary pane that has the highest priority.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
internal fun ThreePaneScaffold(
    modifier: Modifier,
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    windowInsets: WindowInsets,
    secondaryPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    tertiaryPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
    primaryPane: @Composable ThreePaneScaffoldScope.() -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val ltrPaneOrder = remember(paneOrder, layoutDirection) {
        paneOrder.toLtrOrder(layoutDirection)
    }
    val previousScaffoldValue = remember { ThreePaneScaffoldValueHolder(scaffoldValue) }
    val paneMotion = calculateThreePaneMotion(
        previousScaffoldValue = previousScaffoldValue.value,
        currentScaffoldValue = scaffoldValue,
        paneOrder = ltrPaneOrder
    )
    previousScaffoldValue.value = scaffoldValue

    // Create PaneWrappers for each of the panes and map the transitions according to each pane
    // role and order.
    val contents = listOf<@Composable () -> Unit>(
        {
            remember { ThreePaneScaffoldScopeImpl() }.apply {
                paneAdaptedValue = scaffoldValue[ThreePaneScaffoldRole.Primary]
                positionAnimationSpec = paneMotion.animationSpec
                enterTransition = paneMotion.enterTransition(
                    ThreePaneScaffoldRole.Primary,
                    ltrPaneOrder
                )
                exitTransition = paneMotion.exitTransition(
                    ThreePaneScaffoldRole.Primary,
                    ltrPaneOrder
                )
                animationToolingLabel = "Primary"
            }.primaryPane()
        },
        {
            remember { ThreePaneScaffoldScopeImpl() }.apply {
                paneAdaptedValue = scaffoldValue[ThreePaneScaffoldRole.Secondary]
                positionAnimationSpec = paneMotion.animationSpec
                enterTransition = paneMotion.enterTransition(
                    ThreePaneScaffoldRole.Secondary,
                    ltrPaneOrder
                )
                exitTransition = paneMotion.exitTransition(
                    ThreePaneScaffoldRole.Secondary,
                    ltrPaneOrder
                )
                animationToolingLabel = "Secondary"
            }.secondaryPane()
        },
        {
            if (tertiaryPane != null) {
                remember { ThreePaneScaffoldScopeImpl() }.apply {
                    paneAdaptedValue = scaffoldValue[ThreePaneScaffoldRole.Tertiary]
                    positionAnimationSpec = paneMotion.animationSpec
                    enterTransition = paneMotion.enterTransition(
                        ThreePaneScaffoldRole.Tertiary,
                        ltrPaneOrder
                    )
                    exitTransition = paneMotion.exitTransition(
                        ThreePaneScaffoldRole.Tertiary,
                        ltrPaneOrder
                    )
                    animationToolingLabel = "Tertiary"
                }.tertiaryPane()
            }
        },
    )

    val measurePolicy = remember {
        ThreePaneContentMeasurePolicy(scaffoldDirective, scaffoldValue, ltrPaneOrder, windowInsets)
    }.apply {
        this.scaffoldDirective = scaffoldDirective
        this.scaffoldValue = scaffoldValue
        this.paneOrder = ltrPaneOrder
        this.windowInsets = windowInsets
    }

    LookaheadScope {
        Layout(
            contents = contents,
            modifier = modifier,
            measurePolicy = measurePolicy
        )
    }
}

/**
 * Holds the transitions that can be applied to the different panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Immutable
internal class ThreePaneMotion internal constructor(
    internal val animationSpec: FiniteAnimationSpec<IntOffset> = snap(),
    private val firstPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val firstPaneExitTransition: ExitTransition = ExitTransition.None,
    private val secondPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val secondPaneExitTransition: ExitTransition = ExitTransition.None,
    private val thirdPaneEnterTransition: EnterTransition = EnterTransition.None,
    private val thirdPaneExitTransition: ExitTransition = ExitTransition.None
) {

    /**
     * Resolves and returns the [EnterTransition] for the given [ThreePaneScaffoldRole]
     * at the given [ThreePaneScaffoldHorizontalOrder].
     */
    fun enterTransition(
        role: ThreePaneScaffoldRole,
        paneOrder: ThreePaneScaffoldHorizontalOrder
    ): EnterTransition {
        // Quick return in case this instance is the NoMotion one.
        if (this === NoMotion) return EnterTransition.None

        return when (paneOrder.indexOf(role)) {
            0 -> firstPaneEnterTransition
            1 -> secondPaneEnterTransition
            else -> thirdPaneEnterTransition
        }
    }

    /**
     * Resolves and returns the [ExitTransition] for the given [ThreePaneScaffoldRole]
     * at the given [ThreePaneScaffoldHorizontalOrder].
     */
    fun exitTransition(
        role: ThreePaneScaffoldRole,
        paneOrder: ThreePaneScaffoldHorizontalOrder
    ): ExitTransition {
        // Quick return in case this instance is the NoMotion one.
        if (this === NoMotion) return ExitTransition.None

        return when (paneOrder.indexOf(role)) {
            0 -> firstPaneExitTransition
            1 -> secondPaneExitTransition
            else -> thirdPaneExitTransition
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneMotion) return false
        if (this.animationSpec != other.animationSpec) return false
        if (this.firstPaneEnterTransition != other.firstPaneEnterTransition) return false
        if (this.firstPaneExitTransition != other.firstPaneExitTransition) return false
        if (this.secondPaneEnterTransition != other.secondPaneEnterTransition) return false
        if (this.secondPaneExitTransition != other.secondPaneExitTransition) return false
        if (this.thirdPaneEnterTransition != other.thirdPaneEnterTransition) return false
        if (this.thirdPaneExitTransition != other.thirdPaneExitTransition) return false
        return true
    }

    override fun hashCode(): Int {
        var result = animationSpec.hashCode()
        result = 31 * result + firstPaneEnterTransition.hashCode()
        result = 31 * result + firstPaneExitTransition.hashCode()
        result = 31 * result + secondPaneEnterTransition.hashCode()
        result = 31 * result + secondPaneExitTransition.hashCode()
        result = 31 * result + thirdPaneEnterTransition.hashCode()
        result = 31 * result + thirdPaneExitTransition.hashCode()
        return result
    }

    companion object {
        /**
         * A ThreePaneMotion with all transitions set to [EnterTransition.None] and
         * [ExitTransition.None].
         */
        val NoMotion = ThreePaneMotion()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class ThreePaneScaffoldValueHolder(var value: ThreePaneScaffoldValue)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun calculateThreePaneMotion(
    previousScaffoldValue: ThreePaneScaffoldValue,
    currentScaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder
): ThreePaneMotion {
    if (previousScaffoldValue.equals(currentScaffoldValue)) {
        return ThreePaneMotion.NoMotion
    }
    val previousExpandedCount = getExpandedCount(previousScaffoldValue)
    val currentExpandedCount = getExpandedCount(currentScaffoldValue)
    if (previousExpandedCount != currentExpandedCount) {
        // TODO(conradchen): Address this case
        return ThreePaneMotion.NoMotion
    }
    return when (previousExpandedCount) {
        1 -> when (PaneAdaptedValue.Expanded) {
            previousScaffoldValue[paneOrder.firstPane] -> {
                ThreePaneScaffoldDefaults.panesRightMotion
            }

            previousScaffoldValue[paneOrder.thirdPane] -> {
                ThreePaneScaffoldDefaults.panesLeftMotion
            }

            currentScaffoldValue[paneOrder.thirdPane] -> {
                ThreePaneScaffoldDefaults.panesRightMotion
            }

            else -> {
                ThreePaneScaffoldDefaults.panesLeftMotion
            }
        }

        2 -> when {
            previousScaffoldValue[paneOrder.firstPane] != PaneAdaptedValue.Expanded -> {
                ThreePaneScaffoldDefaults.panesLeftMotion
            }

            previousScaffoldValue[paneOrder.thirdPane] != PaneAdaptedValue.Expanded -> {
                ThreePaneScaffoldDefaults.panesRightMotion
            }

            previousScaffoldValue[paneOrder.secondPane] != PaneAdaptedValue.Expanded &&
                currentScaffoldValue[paneOrder.firstPane] != PaneAdaptedValue.Expanded -> {
                ThreePaneScaffoldDefaults.replaceLeftPaneMotion
            }

            else -> {
                ThreePaneScaffoldDefaults.replaceRightPaneMotion
            }
        }

        else -> {
            // Should not happen
            ThreePaneMotion.NoMotion
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun getExpandedCount(scaffoldValue: ThreePaneScaffoldValue): Int {
    var count = 0
    if (scaffoldValue.primary == PaneAdaptedValue.Expanded) {
        count++
    }
    if (scaffoldValue.secondary == PaneAdaptedValue.Expanded) {
        count++
    }
    if (scaffoldValue.tertiary == PaneAdaptedValue.Expanded) {
        count++
    }
    return count
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class ThreePaneContentMeasurePolicy(
    scaffoldDirective: PaneScaffoldDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    paneOrder: ThreePaneScaffoldHorizontalOrder,
    windowInsets: WindowInsets
) : MultiContentMeasurePolicy {
    var scaffoldDirective by mutableStateOf(scaffoldDirective)
    var scaffoldValue by mutableStateOf(scaffoldValue)
    var paneOrder by mutableStateOf(paneOrder)
    var windowInsets by mutableStateOf(windowInsets)

    /**
     * Data class that is used to store the position and width of an expanded pane to be reused when
     * the pane is being hidden.
     */
    private data class PanePlacement(var positionX: Int = 0, var measuredWidth: Int = 0)

    private val placementsCache = mapOf(
        ThreePaneScaffoldRole.Primary to PanePlacement(),
        ThreePaneScaffoldRole.Secondary to PanePlacement(),
        ThreePaneScaffoldRole.Tertiary to PanePlacement()
    )

    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints
    ): MeasureResult {
        val primaryMeasurables = measurables[0]
        val secondaryMeasurables = measurables[1]
        val tertiaryMeasurables = measurables[2]
        return layout(constraints.maxWidth, constraints.maxHeight) {
            if (coordinates == null) {
                return@layout
            }
            val visiblePanes = getPanesMeasurables(
                paneOrder = paneOrder,
                primaryMeasurables = primaryMeasurables,
                scaffoldValue = scaffoldValue,
                secondaryMeasurables = secondaryMeasurables,
                tertiaryMeasurables = tertiaryMeasurables
            ) {
                it != PaneAdaptedValue.Hidden
            }

            val hiddenPanes = getPanesMeasurables(
                paneOrder = paneOrder,
                primaryMeasurables = primaryMeasurables,
                scaffoldValue = scaffoldValue,
                secondaryMeasurables = secondaryMeasurables,
                tertiaryMeasurables = tertiaryMeasurables
            ) {
                it == PaneAdaptedValue.Hidden
            }

            val verticalSpacerSize = scaffoldDirective.horizontalPartitionSpacerSize.roundToPx()
            val leftContentPadding = max(
                scaffoldDirective.contentPadding.calculateLeftPadding(layoutDirection).roundToPx(),
                windowInsets.getLeft(this@measure, layoutDirection)
            )
            val rightContentPadding = max(
                scaffoldDirective.contentPadding.calculateRightPadding(layoutDirection).roundToPx(),
                windowInsets.getRight(this@measure, layoutDirection)
            )
            val topContentPadding = max(
                scaffoldDirective.contentPadding.calculateTopPadding().roundToPx(),
                windowInsets.getTop(this@measure)
            )
            val bottomContentPadding = max(
                scaffoldDirective.contentPadding.calculateBottomPadding().roundToPx(),
                windowInsets.getBottom(this@measure)
            )
            val outerBounds = IntRect(
                leftContentPadding,
                topContentPadding,
                constraints.maxWidth - rightContentPadding,
                constraints.maxHeight - bottomContentPadding
            )

            if (scaffoldDirective.excludedBounds.isNotEmpty()) {
                val layoutBounds = coordinates!!.boundsInWindow()
                val layoutPhysicalPartitions = mutableListOf<Rect>()
                var actualLeft = layoutBounds.left + leftContentPadding
                var actualRight = layoutBounds.right - rightContentPadding
                val actualTop = layoutBounds.top + topContentPadding
                val actualBottom = layoutBounds.bottom - bottomContentPadding
                // Assume hinge bounds are sorted from left to right, non-overlapped.
                @Suppress("ListIterator")
                scaffoldDirective.excludedBounds.forEach { hingeBound ->
                    if (hingeBound.left <= actualLeft) {
                        // The hinge is at the left of the layout, adjust the left edge of
                        // the current partition to the actual displayable bounds.
                        actualLeft = max(actualLeft, hingeBound.right)
                    } else if (hingeBound.right >= actualRight) {
                        // The hinge is right at the right of the layout and there's no more
                        // room for more partitions, adjust the right edge of the current
                        // partition to the actual displayable bounds.
                        actualRight = min(hingeBound.left, actualRight)
                        return@forEach
                    } else {
                        // The hinge is inside the layout, add the current partition to the list
                        // and move the left edge of the next partition to the right of the
                        // hinge.
                        layoutPhysicalPartitions.add(
                            Rect(actualLeft, actualTop, hingeBound.left, actualBottom)
                        )
                        actualLeft +=
                            max(hingeBound.right, hingeBound.left + verticalSpacerSize)
                    }
                }
                if (actualLeft < actualRight) {
                    // The last partition
                    layoutPhysicalPartitions.add(
                        Rect(actualLeft, actualTop, actualRight, actualBottom)
                    )
                }
                if (layoutPhysicalPartitions.size == 0) {
                    // Display nothing
                } else if (layoutPhysicalPartitions.size == 1) {
                    measureAndPlacePanes(
                        layoutPhysicalPartitions[0],
                        verticalSpacerSize,
                        visiblePanes,
                        isLookingAhead
                    )
                } else if (layoutPhysicalPartitions.size < visiblePanes.size) {
                    // Note that the only possible situation is we have only two physical partitions
                    // but three expanded panes to show. In this case fit two panes in the larger
                    // partition.
                    if (layoutPhysicalPartitions[0].width > layoutPhysicalPartitions[1].width) {
                        measureAndPlacePanes(
                            layoutPhysicalPartitions[0],
                            verticalSpacerSize,
                            visiblePanes.subList(0, 2),
                            isLookingAhead
                        )
                        measureAndPlacePane(
                            layoutPhysicalPartitions[1],
                            visiblePanes[2],
                            isLookingAhead
                        )
                    } else {
                        measureAndPlacePane(
                            layoutPhysicalPartitions[0],
                            visiblePanes[0],
                            isLookingAhead
                        )
                        measureAndPlacePanes(
                            layoutPhysicalPartitions[1],
                            verticalSpacerSize,
                            visiblePanes.subList(1, 3),
                            isLookingAhead
                        )
                    }
                } else {
                    // Layout each visible pane in a physical partition
                    visiblePanes.fastForEachIndexed { index, paneMeasurable ->
                        measureAndPlacePane(
                            layoutPhysicalPartitions[index],
                            paneMeasurable,
                            isLookingAhead
                        )
                    }
                }
            } else {
                measureAndPlacePanesWithLocalBounds(
                    outerBounds,
                    verticalSpacerSize,
                    visiblePanes,
                    isLookingAhead
                )
            }

            // Place the hidden panes. Those should only exist when isLookingAhead = true.
            // Placing these type of pane during the lookahead phase ensures a proper motion
            // at the AnimatedVisibility.
            // The placement is done using the outerBounds, as the placementsCache holds
            // absolute position values.
            placeHiddenPanes(
                outerBounds.top,
                outerBounds.height,
                hiddenPanes
            )
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun MeasureScope.getPanesMeasurables(
        paneOrder: ThreePaneScaffoldHorizontalOrder,
        primaryMeasurables: List<Measurable>,
        scaffoldValue: ThreePaneScaffoldValue,
        secondaryMeasurables: List<Measurable>,
        tertiaryMeasurables: List<Measurable>,
        predicate: (PaneAdaptedValue) -> Boolean
    ): List<PaneMeasurable> {
        return buildList {
            paneOrder.forEach { role ->
                if (predicate(scaffoldValue[role])) {
                    when (role) {
                        ThreePaneScaffoldRole.Primary -> {
                            createPaneMeasurableIfNeeded(
                                primaryMeasurables,
                                ThreePaneScaffoldDefaults.PrimaryPanePriority,
                                role,
                                ThreePaneScaffoldDefaults.PrimaryPanePreferredWidth
                                    .roundToPx()
                            )
                        }

                        ThreePaneScaffoldRole.Secondary -> {
                            createPaneMeasurableIfNeeded(
                                secondaryMeasurables,
                                ThreePaneScaffoldDefaults.SecondaryPanePriority,
                                role,
                                ThreePaneScaffoldDefaults.SecondaryPanePreferredWidth
                                    .roundToPx()
                            )
                        }

                        ThreePaneScaffoldRole.Tertiary -> {
                            createPaneMeasurableIfNeeded(
                                tertiaryMeasurables,
                                ThreePaneScaffoldDefaults.TertiaryPanePriority,
                                role,
                                ThreePaneScaffoldDefaults.TertiaryPanePreferredWidth
                                    .roundToPx()
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun MutableList<PaneMeasurable>.createPaneMeasurableIfNeeded(
        measurables: List<Measurable>,
        priority: Int,
        role: ThreePaneScaffoldRole,
        defaultPreferredWidth: Int
    ) {
        if (measurables.isNotEmpty()) {
            add(PaneMeasurable(measurables[0], priority, role, defaultPreferredWidth))
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun Placeable.PlacementScope.measureAndPlacePane(
        partitionBounds: Rect,
        measurable: PaneMeasurable,
        isLookingAhead: Boolean
    ) {
        val localBounds = getLocalBounds(partitionBounds)
        measurable.measuredWidth = localBounds.width
        measurable.apply {
            measure(Constraints.fixed(measuredWidth, localBounds.height))
                .place(localBounds.left, localBounds.top)
        }
        if (isLookingAhead) {
            // Cache the values to be used when this measurable role is being hidden.
            // See placeHiddenPanes.
            val cachedPanePlacement = placementsCache[measurable.role]!!
            cachedPanePlacement.measuredWidth = measurable.measuredWidth
            cachedPanePlacement.positionX = localBounds.left
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun Placeable.PlacementScope.measureAndPlacePanes(
        partitionBounds: Rect,
        spacerSize: Int,
        measurables: List<PaneMeasurable>,
        isLookingAhead: Boolean
    ) {
        measureAndPlacePanesWithLocalBounds(
            getLocalBounds(partitionBounds),
            spacerSize,
            measurables,
            isLookingAhead
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun Placeable.PlacementScope.measureAndPlacePanesWithLocalBounds(
        partitionBounds: IntRect,
        spacerSize: Int,
        measurables: List<PaneMeasurable>,
        isLookingAhead: Boolean
    ) {
        if (measurables.isEmpty()) {
            return
        }
        val allocatableWidth = partitionBounds.width - (measurables.size - 1) * spacerSize
        val totalPreferredWidth = measurables.sumOf { it.measuredWidth }
        if (allocatableWidth > totalPreferredWidth) {
            // Allocate the remaining space to the pane with the highest priority.
            measurables.maxBy {
                it.priority
            }.measuredWidth += allocatableWidth - totalPreferredWidth
        } else if (allocatableWidth < totalPreferredWidth) {
            // Scale down all panes to fit in the available space.
            val scale = allocatableWidth.toFloat() / totalPreferredWidth
            measurables.fastForEach {
                it.measuredWidth = (it.measuredWidth * scale).toInt()
            }
        }
        var positionX = partitionBounds.left
        measurables.fastForEach {
            it.measure(Constraints.fixed(it.measuredWidth, partitionBounds.height))
                .place(positionX, partitionBounds.top)
            if (isLookingAhead) {
                // Cache the values to be used when this measurable's role is being hidden.
                // See placeHiddenPanes.
                val cachedPanePlacement = placementsCache[it.role]!!
                cachedPanePlacement.measuredWidth = it.measuredWidth
                cachedPanePlacement.positionX = positionX
            }
            positionX += it.measuredWidth + spacerSize
        }
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    private fun Placeable.PlacementScope.placeHiddenPanes(
        partitionTop: Int,
        partitionHeight: Int,
        measurables: List<PaneMeasurable>
    ) {
        // When panes are being hidden, apply each pane's width and position from the cache to
        // maintain the those before it's hidden by the AnimatedVisibility.
        measurables.fastForEach {
            if (!it.isAnimatedPane) {
                // When panes are not animated, we don't need to measure and place them.
                return
            }
            val cachedPanePlacement = placementsCache[it.role]!!
            it.measure(
                Constraints.fixed(
                    width = cachedPanePlacement.measuredWidth,
                    height = partitionHeight
                )
            ).place(cachedPanePlacement.positionX, partitionTop)
        }
    }

    private fun Placeable.PlacementScope.getLocalBounds(bounds: Rect): IntRect {
        return bounds.translate(coordinates!!.windowToLocal(Offset.Zero)).roundToIntRect()
    }
}

/**
 * A conditional [Modifier.clipToBounds] that will only clip when the given [adaptedValue] is
 * [PaneAdaptedValue.Hidden].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun Modifier.clipToBounds(adaptedValue: PaneAdaptedValue): Modifier =
    if (adaptedValue == PaneAdaptedValue.Hidden) this.clipToBounds() else this

/**
 * The root composable of pane contents in a [ThreePaneScaffold] that supports default motions
 * during pane switching. It's recommended to use this composable to wrap your own contents when
 * passing them into pane parameters of the scaffold functions, therefore your panes can have a
 * nice default animation for free.
 *
 * See usage samples at:
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.ListDetailExtraPaneScaffoldSample
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ThreePaneScaffoldScope.AnimatedPane(
    modifier: Modifier,
    content: (@Composable ThreePaneScaffoldScope.() -> Unit),
) {
    AnimatedVisibility(
        visible = paneAdaptedValue == PaneAdaptedValue.Expanded,
        modifier = modifier
            .animatedPane()
            .clipToBounds(paneAdaptedValue)
            .then(
                if (paneAdaptedValue == PaneAdaptedValue.Expanded) {
                    Modifier.animateBounds(
                        // TODO Figure out why we need to pass a non-null here to get the bounds
                        //  animation going on the first navigation event that pass in the spec
                        //  later on. To resolve this, we default to the paneSpringSpec().
                        //  Otherwise, the first motion shows a snap instead of a smooth
                        //  transition.
                        positionAnimationSpec = positionAnimationSpec
                            ?: ThreePaneScaffoldDefaults.PaneSpringSpec
                    )
                } else {
                    Modifier
                }
            ),
        enter = enterTransition,
        exit = exitTransition,
        label = "AnimatedVisibility: $animationToolingLabel"
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class PaneMeasurable(
    val measurable: Measurable,
    val priority: Int,
    val role: ThreePaneScaffoldRole,
    defaultPreferredWidth: Int
) : Measurable by measurable {
    private val data = ((parentData as? PaneScaffoldParentData) ?: PaneScaffoldParentData())

    var measuredWidth = if (data.preferredWidth == null || data.preferredWidth!!.isNaN()) {
        defaultPreferredWidth
    } else {
        data.preferredWidth!!.toInt()
    }

    val isAnimatedPane = data.isAnimatedPane
}

/**
 * Scope for the panes of [ThreePaneScaffold].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
interface ThreePaneScaffoldScope : PaneScaffoldScope {
    /**
     * The adapted value of the associated pane to the scope.
     */
    val paneAdaptedValue: PaneAdaptedValue

    /**
     * The position animation spec of the associated pane to the scope. [AnimatedPane] will use this
     * value to perform pane animations during scaffold state changes.
     */
    val positionAnimationSpec: FiniteAnimationSpec<IntOffset>?

    /**
     * The [EnterTransition] of the associated pane. [AnimatedPane] will use this value to perform
     * pane entering animations when it's showing during scaffold state changes.
     */
    val enterTransition: EnterTransition

    /**
     * The [ExitTransition] of the associated pane. [AnimatedPane] will use this value to perform
     * pane exiting animations when it's hiding during scaffold state changes.
     */
    val exitTransition: ExitTransition

    /**
     * The label will be used by [AnimatedPane] to provide tooling labels to the foundation
     * animation APIs like [AnimatedVisibility].
     */
    val animationToolingLabel: String
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private class ThreePaneScaffoldScopeImpl : ThreePaneScaffoldScope, PaneScaffoldScopeImpl() {
    override var paneAdaptedValue by mutableStateOf(PaneAdaptedValue.Hidden)
    override var positionAnimationSpec: FiniteAnimationSpec<IntOffset>? by mutableStateOf(null)
    override var enterTransition by mutableStateOf(EnterTransition.None)
    override var exitTransition by mutableStateOf(ExitTransition.None)
    override var animationToolingLabel by mutableStateOf("")
}

/**
 * Provides default values of [ThreePaneScaffold] and the calculation functions of
 * [ThreePaneScaffoldValue].
 */
@ExperimentalMaterial3AdaptiveApi
internal object ThreePaneScaffoldDefaults {
    /**
     * Denotes [ThreePaneScaffold] to use the list-detail pane-order to arrange its panes
     * horizontally, which allocates panes in the order of secondary, primary, and tertiary from
     * start to end.
     */
    // TODO(conradchen/sgibly): Consider moving this to the ListDetailPaneScaffoldDefaults
    val ListDetailLayoutPaneOrder = ThreePaneScaffoldHorizontalOrder(
        ThreePaneScaffoldRole.Secondary,
        ThreePaneScaffoldRole.Primary,
        ThreePaneScaffoldRole.Tertiary
    )

    /**
     * Denotes [ThreePaneScaffold] to use the supporting-pane pane-order to arrange its panes
     * horizontally, which allocates panes in the order of primary, secondary, and tertiary from
     * start to end.
     */
    // TODO(conradchen/sgibly): Consider moving this to the SupportingPaneScaffoldDefaults
    val SupportingPaneLayoutPaneOrder = ThreePaneScaffoldHorizontalOrder(
        ThreePaneScaffoldRole.Primary,
        ThreePaneScaffoldRole.Secondary,
        ThreePaneScaffoldRole.Tertiary
    )

    /**
     * The default preferred width of [ThreePaneScaffoldRole.Secondary]. See more details in
     * [ThreePaneScaffoldScope.preferredWidth].
     */
    val SecondaryPanePreferredWidth = 412.dp

    /**
     * The default preferred width of [ThreePaneScaffoldRole.Tertiary]. See more details in
     * [ThreePaneScaffoldScope.preferredWidth].
     */
    val TertiaryPanePreferredWidth = 412.dp

    // Make it the same as the secondary and tertiary panes, so we can have a semi-50-50-split on
    // narrower windows by default.
    val PrimaryPanePreferredWidth = 412.dp

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
            tertiaryPaneAdaptStrategy
        )

    /**
     * A default [SpringSpec] for the panes motion.
     */
    // TODO(conradchen): open this to public when we support motion customization
    val PaneSpringSpec: SpringSpec<IntOffset> =
        spring(
            dampingRatio = 0.8f,
            stiffness = 600f,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )

    private val slideInFromLeft = slideInHorizontally(PaneSpringSpec) { -it }
    private val slideInFromRight = slideInHorizontally(PaneSpringSpec) { it }
    private val slideOutToLeft = slideOutHorizontally(PaneSpringSpec) { -it }
    private val slideOutToRight = slideOutHorizontally(PaneSpringSpec) { it }

    val panesLeftMotion = ThreePaneMotion(
        PaneSpringSpec,
        slideInFromLeft,
        slideOutToRight,
        slideInFromLeft,
        slideOutToRight,
        slideInFromLeft,
        slideOutToRight
    )

    val panesRightMotion = ThreePaneMotion(
        PaneSpringSpec,
        slideInFromRight,
        slideOutToLeft,
        slideInFromRight,
        slideOutToLeft,
        slideInFromRight,
        slideOutToLeft
    )

    // TODO(conradchen): figure out how to add delay and zOffset to spring animations
    val replaceLeftPaneMotion = ThreePaneMotion(
        PaneSpringSpec,
        slideInFromLeft,
        slideOutToLeft,
        slideInFromLeft,
        slideOutToLeft,
        EnterTransition.None,
        ExitTransition.None
    )

    // TODO(conradchen): figure out how to add delay and zOffset to spring animations
    val replaceRightPaneMotion = ThreePaneMotion(
        PaneSpringSpec,
        EnterTransition.None,
        ExitTransition.None,
        slideInFromRight,
        slideOutToRight,
        slideInFromRight,
        slideOutToRight
    )
}
