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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A Material opinionated implementation of [ThreePaneScaffold] that will display the provided three
 * panes in a canonical list-detail layout.
 *
 * @param listPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.List].
 * @param modifier [Modifier] of the scaffold layout.
 * @param scaffoldState the state of the scaffold, which provides the current scaffold directive
 *        and scaffold value.
 * @param windowInsets window insets that the scaffold will respect.
 * @param extraPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.Extra].
 * @param detailPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.Detail].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    listPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: ThreePaneScaffoldState = calculateListDetailPaneScaffoldState(),
    windowInsets: WindowInsets = ListDetailPaneScaffoldDefaults.windowInsets,
    extraPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
    detailPane: @Composable ThreePaneScaffoldScope.() -> Unit
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = scaffoldState.scaffoldDirective,
        scaffoldValue = scaffoldState.scaffoldValue,
        paneOrder = ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder,
        windowInsets = windowInsets,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        primaryPane = detailPane
    )
}

/**
 * This function calculates [ThreePaneScaffoldValue] based on the given [PaneScaffoldDirective],
 * [ThreePaneScaffoldAdaptStrategies], and the current pane destination of a
 * [ListDetailPaneScaffold].
 *
 * @param currentDestination the current destination item, which will be guaranteed to have the
 *        highest priority when deciding pane visibilities.
 * @param scaffoldDirective the layout directives that the associated [ListDetailPaneScaffold]
 *        needs to follow. The default value will be the calculation result from
 *        [calculateStandardPaneScaffoldDirective] with the current window configuration, and
 *        will be automatically updated when the window configuration changes.
 * @param adaptStrategies the [ThreePaneScaffoldAdaptStrategies] should be used by scaffold panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun calculateListDetailPaneScaffoldState(
    currentDestination: ThreePaneScaffoldDestinationItem<*> =
        ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.List, null),
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies()
): ThreePaneScaffoldState = ThreePaneScaffoldStateImpl(
    scaffoldDirective,
    calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        adaptStrategies,
        currentDestination
    )
)

/**
 * This function calculates [ThreePaneScaffoldValue] based on the given [PaneScaffoldDirective],
 * [ThreePaneScaffoldAdaptStrategies], and the pane destination history of a
 * [ListDetailPaneScaffold].
 *
 * @param destinationHistory The history of past destinations items. The last destination will
 *        have the highest priority, and the second last destination will have the second highest
 *        priority, and so forth until all panes have a priority assigned. Note that the last
 *        destination is supposed to be the last item of the provided list. When the history is
 *        empty or there are panes left unassigned, default priorities will be assigned to those
 *        panes in the order of Detail > List > Extra.
 * @param scaffoldDirective the layout directives that the associated [ListDetailPaneScaffold]
 *        needs to follow. The default value will be the calculation result from
 *        [calculateStandardPaneScaffoldDirective] with the current window configuration, and
 *        will be automatically updated when the window configuration changes.
 * @param adaptStrategies the [ThreePaneScaffoldAdaptStrategies] should be used by scaffold panes.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun calculateListDetailPaneScaffoldState(
    destinationHistory: List<ThreePaneScaffoldDestinationItem<*>>,
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies()
): ThreePaneScaffoldState = ThreePaneScaffoldStateImpl(
    scaffoldDirective,
    calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        adaptStrategies,
        destinationHistory
    )
)

/**
 * Provides default values of [ListDetailPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
object ListDetailPaneScaffoldDefaults {
    /**
     * Default insets that will be used and consumed by [ListDetailPaneScaffold]. By default it will
     * be the union of [WindowInsets.Companion.systemBars] and
     * [WindowInsets.Companion.displayCutout].
     */
    val windowInsets @Composable get() = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies] for [ListDetailPaneScaffold].
     *
     * @param detailPaneAdaptStrategy the adapt strategy of the primary pane
     * @param listPaneAdaptStrategy the adapt strategy of the secondary pane
     * @param extraPaneAdaptStrategy the adapt strategy of the tertiary pane
     */
    fun adaptStrategies(
        detailPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        listPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        extraPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            detailPaneAdaptStrategy,
            listPaneAdaptStrategy,
            extraPaneAdaptStrategy
        )
}

/**
 * The set of the available pane roles of [ListDetailPaneScaffold]. Basically those values are
 * aliases of [ThreePaneScaffoldRole]. We suggest you to use the values defined here instead of
 * the raw [ThreePaneScaffoldRole] under the context of [ListDetailPaneScaffold] for better
 * code clarity.
 */
@ExperimentalMaterial3AdaptiveApi
object ListDetailPaneScaffoldRole {
    /**
     * The list pane of [ListDetailPaneScaffold]. It is an alias of
     * [ThreePaneScaffoldRole.Secondary].
     */
    val List = ThreePaneScaffoldRole.Secondary

    /**
     * The detail pane of [ListDetailPaneScaffold]. It is an alias of
     * [ThreePaneScaffoldRole.Primary].
     */
    val Detail = ThreePaneScaffoldRole.Primary

    /**
     * The extra pane of [ListDetailPaneScaffold]. It is an alias of
     * [ThreePaneScaffoldRole.Tertiary].
     */
    val Extra = ThreePaneScaffoldRole.Tertiary
}
