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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

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
        val paneMeasurables = buildList {
            arrangement.forEach { role ->
                when (role) {
                    ThreePaneScaffoldRole.Primary -> {
                        createPaneMeasurableIfNeeded(
                            primaryMeasurables,
                            ThreePaneScaffoldDefaults.PrimaryPanePriority,
                            ThreePaneScaffoldDefaults.PrimaryPanePreferredWidth.toPx()
                        )
                    }
                    ThreePaneScaffoldRole.Secondary -> {
                        createPaneMeasurableIfNeeded(
                            secondaryMeasurables,
                            ThreePaneScaffoldDefaults.SecondaryPanePriority,
                            ThreePaneScaffoldDefaults.SecondaryPanePreferredWidth.toPx()
                        )
                    }
                    ThreePaneScaffoldRole.Tertiary -> {
                        createPaneMeasurableIfNeeded(
                            tertiaryMeasurables,
                            ThreePaneScaffoldDefaults.TertiaryPanePriority,
                            ThreePaneScaffoldDefaults.TertiaryPanePreferredWidth.toPx()
                        )
                    }
                }
            }
        }

        val outerVerticalGutterSize = layoutDirective.gutterSizes.outerVertical.toPx()
        val innerVerticalGutterSize = layoutDirective.gutterSizes.innerVertical.toPx()
        val outerHorizontalGutterSize = layoutDirective.gutterSizes.outerHorizontal.toPx()

        val availableWidth =
            constraints.maxWidth -
                outerVerticalGutterSize * 2 -
                innerVerticalGutterSize * (paneMeasurables.size - 1)
        val availableHeight =
            constraints.maxHeight -
                outerHorizontalGutterSize * 2

        val totalPreferredWidth = paneMeasurables.map { it.measuredWidth }.sum()
        (if (availableWidth > totalPreferredWidth) {
            paneMeasurables.maxBy { it.priority }
        } else if (availableWidth < totalPreferredWidth) {
            // TODO(conradchen): Confirm the behavior with designers and address negative widths
            paneMeasurables.minBy { it.priority }
        } else {
            null
        })?.apply {
            measuredWidth += availableWidth - totalPreferredWidth
        }
        // TODO(conradchen): Address hinge position
        layout(constraints.maxWidth, constraints.maxHeight) {
            var positionX = outerVerticalGutterSize
            paneMeasurables.forEach {
                it
                    .measure(Constraints.fixed(it.measuredWidth.toInt(), availableHeight.toInt()))
                    .place(positionX.toInt(), outerHorizontalGutterSize.toInt())
                positionX += innerVerticalGutterSize + it.measuredWidth
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
    preferredWidth: Float
) {
    if (measurables.isNotEmpty()) {
        add(PaneMeasurable(measurables[0], priority, preferredWidth))
    }
}

private class PaneMeasurable(
    val measurable: Measurable,
    val priority: Int,
    defaultPreferredWidth: Float
) : Measurable by measurable {
    private val data = ((parentData as? PaneScaffoldParentData) ?: PaneScaffoldParentData())

    val preferredWidth =
        if (data.preferredWidth.isNaN()) defaultPreferredWidth else data.preferredWidth

    var measuredWidth = if (preferredWidth.isFinite() && preferredWidth > 0) preferredWidth else 0f
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
    fun threePaneScaffoldAdaptStrategies(
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
