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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import kotlin.math.max
import kotlin.math.min

/**
 * A pane scaffold composable that can display up to three panes according to the instructions
 * provided by [ThreePaneScaffoldValue] in the order that [ThreePaneScaffoldArrangement] specifies,
 * and allocate margins and spacers according to [AdaptiveLayoutDirective].
 *
 * [ThreePaneScaffold] is the base composable functions of adaptive programming. Developers can
 * freely pipeline the relevant adaptive signals and use them as input of the scaffold function
 * to render the final adaptive layout.
 *
 * It's recommended to use [ThreePaneScaffold] with [calculateStandardAdaptiveLayoutDirective],
 * [calculateThreePaneScaffoldValue] to follow the Material design guidelines on adaptive
 * programming.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param layoutDirective The top-level directives about how the scaffold should arrange its panes.
 * @param scaffoldValue The current adapted value of the scaffold.
 * @param arrangement The arrangement of the panes in the scaffold.
 * @param secondaryPane The content of the secondary pane that has a priority lower then the primary
 *                      pane but higher than the tertiary pane.
 * @param tertiaryPane The content of the tertiary pane that has the lowest priority.
 * @param primaryPane The content of the primary pane that has the highest priority.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ThreePaneScaffold(
    modifier: Modifier,
    layoutDirective: AdaptiveLayoutDirective,
    scaffoldValue: ThreePaneScaffoldValue,
    arrangement: ThreePaneScaffoldArrangement,
    secondaryPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit,
    tertiaryPane: (@Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit)? = null,
    primaryPane: @Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit
) {
    val contents = listOf<@Composable () -> Unit>(
        { PaneWrapper(scaffoldValue[ThreePaneScaffoldRole.Primary], primaryPane) },
        { PaneWrapper(scaffoldValue[ThreePaneScaffoldRole.Secondary], secondaryPane) },
        { PaneWrapper(scaffoldValue[ThreePaneScaffoldRole.Tertiary], tertiaryPane) },
    )

    Layout(
        contents = contents,
        modifier = modifier,
    ) { (primaryMeasurables, secondaryMeasurables, tertiaryMeasurables), constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (coordinates == null) {
                return@layout
            }
            val paneMeasurables = buildList {
                arrangement.forEach { role ->
                    when (role) {
                        ThreePaneScaffoldRole.Primary -> {
                            createPaneMeasurableIfNeeded(
                                primaryMeasurables,
                                ThreePaneScaffoldDefaults.PrimaryPanePriority,
                                ThreePaneScaffoldDefaults.PrimaryPanePreferredWidth.roundToPx()
                            )
                        }
                        ThreePaneScaffoldRole.Secondary -> {
                            createPaneMeasurableIfNeeded(
                                secondaryMeasurables,
                                ThreePaneScaffoldDefaults.SecondaryPanePriority,
                                ThreePaneScaffoldDefaults.SecondaryPanePreferredWidth.roundToPx()
                            )
                        }
                        ThreePaneScaffoldRole.Tertiary -> {
                            createPaneMeasurableIfNeeded(
                                tertiaryMeasurables,
                                ThreePaneScaffoldDefaults.TertiaryPanePriority,
                                ThreePaneScaffoldDefaults.TertiaryPanePreferredWidth.roundToPx()
                            )
                        }
                    }
                }
            }

            val outerVerticalGutterSize = layoutDirective.gutterSizes.outerVertical.roundToPx()
            val innerVerticalGutterSize = layoutDirective.gutterSizes.innerVertical.roundToPx()
            val outerHorizontalGutterSize = layoutDirective.gutterSizes.outerHorizontal.roundToPx()

            if (layoutDirective.excludedBounds.isNotEmpty()) {
                val layoutBounds = coordinates!!.boundsInWindow()
                val layoutPhysicalPartitions = mutableListOf<Rect>()
                var actualLeft = layoutBounds.left + outerVerticalGutterSize
                var actualRight = layoutBounds.right - outerVerticalGutterSize
                val actualTop = layoutBounds.top + outerHorizontalGutterSize
                val actualBottom = layoutBounds.bottom - outerHorizontalGutterSize
                // Assume hinge bounds are sorted from left to right, non-overlapped.
                layoutDirective.excludedBounds.fastForEach { hingeBound ->
                    if (hingeBound.left <= actualLeft) {
                        // The hinge is at the left of the layout, adjust the left edge of
                        // the current partition to the actual displayable bounds.
                        actualLeft = max(actualLeft, hingeBound.right)
                    } else if (hingeBound.right >= actualRight) {
                        // The hinge is right at the right of the layout and there's no more room
                        // for more partitions, adjust the right edge of the current partition to
                        // the actual displayable bounds.
                        actualRight = min(hingeBound.left, actualRight)
                        return@fastForEach
                    } else {
                        // The hinge is inside the layout, add the current partition to the list and
                        // move the left edge of the next partition to the right of the hinge.
                        layoutPhysicalPartitions.add(
                            Rect(actualLeft, actualTop, hingeBound.left, actualBottom)
                        )
                        actualLeft +=
                            max(hingeBound.right, hingeBound.left + innerVerticalGutterSize)
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
                        innerVerticalGutterSize,
                        paneMeasurables
                    )
                } else if (layoutPhysicalPartitions.size < paneMeasurables.size) {
                    // Note that the only possible situation is we have only two physical partitions
                    // but three expanded panes to show. In this case fit two panes in the larger
                    // partition.
                    if (layoutPhysicalPartitions[0].width > layoutPhysicalPartitions[1].width) {
                        measureAndPlacePanes(
                            layoutPhysicalPartitions[0],
                            innerVerticalGutterSize,
                            paneMeasurables.subList(0, 2)
                        )
                        measureAndPlacePane(layoutPhysicalPartitions[1], paneMeasurables[2])
                    } else {
                        measureAndPlacePane(layoutPhysicalPartitions[0], paneMeasurables[0])
                        measureAndPlacePanes(
                            layoutPhysicalPartitions[1],
                            innerVerticalGutterSize,
                            paneMeasurables.subList(1, 3)
                        )
                    }
                } else {
                    // Layout each pane in a physical partition
                    paneMeasurables.fastForEachIndexed { index, paneMeasurable ->
                        measureAndPlacePane(layoutPhysicalPartitions[index], paneMeasurable)
                    }
                }
            } else {
                measureAndPlacePanesWithLocalBounds(
                    IntRect(
                        outerVerticalGutterSize,
                        outerHorizontalGutterSize,
                        constraints.maxWidth - outerVerticalGutterSize,
                        constraints.maxHeight - outerHorizontalGutterSize),
                    innerVerticalGutterSize,
                    paneMeasurables
                )
            }
        }
    }
}

@ExperimentalMaterial3AdaptiveApi
@Composable
private fun PaneWrapper(
    adaptedValue: PaneAdaptedValue,
    pane: (@Composable ThreePaneScaffoldScope.(PaneAdaptedValue) -> Unit)?
) {
    if (adaptedValue != PaneAdaptedValue.Hidden) {
        pane?.invoke(ThreePaneScaffoldScopeImpl, adaptedValue)
    }
}

private fun MutableList<PaneMeasurable>.createPaneMeasurableIfNeeded(
    measurables: List<Measurable>,
    priority: Int,
    defaultPreferredWidth: Int
) {
    if (measurables.isNotEmpty()) {
        add(PaneMeasurable(measurables[0], priority, defaultPreferredWidth))
    }
}
private fun Placeable.PlacementScope.measureAndPlacePane(
    partitionBounds: Rect,
    measurable: PaneMeasurable
) {
    val localBounds = getLocalBounds(partitionBounds)
    measurable.measuredWidth = localBounds.width
    measurable.apply {
        measure(Constraints.fixed(measuredWidth, localBounds.height))
            .place(localBounds.left, localBounds.top)
    }
}

private fun Placeable.PlacementScope.measureAndPlacePanes(
    partitionBounds: Rect,
    spacerSize: Int,
    measurables: List<PaneMeasurable>
) {
    measureAndPlacePanesWithLocalBounds(
        getLocalBounds(partitionBounds),
        spacerSize,
        measurables
    )
}

private fun Placeable.PlacementScope.measureAndPlacePanesWithLocalBounds(
    partitionBounds: IntRect,
    spacerSize: Int,
    measurables: List<PaneMeasurable>
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
        positionX += it.measuredWidth + spacerSize
    }
}

private fun Placeable.PlacementScope.getLocalBounds(bounds: Rect): IntRect {
    return bounds.translate(coordinates!!.windowToLocal(Offset.Zero)).roundToIntRect()
}

private class PaneMeasurable(
    val measurable: Measurable,
    val priority: Int,
    defaultPreferredWidth: Int
) : Measurable by measurable {
    private val data = ((parentData as? PaneScaffoldParentData) ?: PaneScaffoldParentData())

    // TODO(conradchen): Handle the case of a low priority pane with no preferred with
    var measuredWidth = when (data.preferredWidth) {
        null -> defaultPreferredWidth
        Float.NaN -> 0
        else -> data.preferredWidth!!.toInt()
    }
}

/**
 * Scope for the panes of [ThreePaneScaffold].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
interface ThreePaneScaffoldScope : PaneScaffoldScope

private object ThreePaneScaffoldScopeImpl : ThreePaneScaffoldScope, PaneScaffoldScopeImpl()

/**
 * Provides default values of [ThreePaneScaffold] and the calculation functions of
 * [ThreePaneScaffoldValue].
 */
@ExperimentalMaterial3AdaptiveApi
object ThreePaneScaffoldDefaults {
    /**
     * Denotes [ThreePaneScaffold] to use the list-detail arrangement to arrange its panes, which
     * allocates panes in the order of secondary, primary, and tertiary form start to end.
     */
    val ListDetailLayoutArrangement = ThreePaneScaffoldArrangement(
        ThreePaneScaffoldRole.Secondary,
        ThreePaneScaffoldRole.Primary,
        ThreePaneScaffoldRole.Tertiary
    )

    // TODO(conradchen): confirm with designers before we make these values public
    internal val PrimaryPanePreferredWidth = 360.dp
    internal val SecondaryPanePreferredWidth = 360.dp
    internal val TertiaryPanePreferredWidth = 360.dp

    // TODO(conradchen): consider declaring a value class for priority
    internal const val PrimaryPanePriority = 10
    internal const val SecondaryPanePriority = 5
    internal const val TertiaryPanePriority = 1

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
}
