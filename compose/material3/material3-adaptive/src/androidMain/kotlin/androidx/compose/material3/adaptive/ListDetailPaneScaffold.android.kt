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

import androidx.compose.foundation.layout.fillMaxSize
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
 * @param extraPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.Extra].
 * @param detailPane the list pane of the scaffold. See [ListDetailPaneScaffoldRole.Detail].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    listPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    modifier: Modifier = Modifier,
    scaffoldState: ThreePaneScaffoldState = calculateListDetailPaneScaffoldState(),
    extraPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
    detailPane: @Composable ThreePaneScaffoldScope.() -> Unit
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = scaffoldState.scaffoldDirective,
        scaffoldValue = scaffoldState.scaffoldValue,
        paneOrder = ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder,
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
 * @param scaffoldDirective the layout directives that the associated [ListDetailPaneScaffold]
 *        needs to follow. The default value will be the calculation result from
 *        [calculateStandardPaneScaffoldDirective] with the current window configuration, and
 *        will be automatically updated when the window configuration changes.
 * @param adaptStrategies the [ThreePaneScaffoldAdaptStrategies] should be used by scaffold panes.
 * @param currentPaneDestination the current pane destination, which will be guaranteed to have
 *        highest priority when deciding pane visibility.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun calculateListDetailPaneScaffoldState(
    scaffoldDirective: PaneScaffoldDirective =
        calculateStandardPaneScaffoldDirective(currentWindowAdaptiveInfo()),
    adaptStrategies: ThreePaneScaffoldAdaptStrategies =
        ListDetailPaneScaffoldDefaults.adaptStrategies(),
    currentPaneDestination: ThreePaneScaffoldRole = ListDetailPaneScaffoldRole.List
): ThreePaneScaffoldState = ThreePaneScaffoldStateImpl(
    scaffoldDirective,
    calculateThreePaneScaffoldValue(
        scaffoldDirective.maxHorizontalPartitions,
        adaptStrategies,
        currentPaneDestination
    )
)

/**
 * Provides default values of [ListDetailPaneScaffold].
 */
@ExperimentalMaterial3AdaptiveApi
object ListDetailPaneScaffoldDefaults {
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
