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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * An opinionated implementation of [ThreePaneScaffold] following Material guidelines that displays
 * the provided three panes in a canonical [supporting pane layout](
 * https://m3.material.io/foundations/layout/canonical-layouts/supporting-pane).
 *
 * This overload takes a [ThreePaneScaffoldValue] describing the adapted value of each pane within
 * the scaffold.
 *
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param value The current adapted value of the scaffold, which indicates how each pane of the
 *   scaffold is adapted.
 * @param mainPane the main pane of the scaffold, which is supposed to hold the major content of an
 *   app, for example, the editing screen of a doc app. See [SupportingPaneScaffoldRole.Main].
 * @param supportingPane the supporting pane of the scaffold, which is supposed to hold the support
 *   content of an app, for example, the comment list of a doc app. See
 *   [SupportingPaneScaffoldRole.Supporting].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any additional content
 *   besides the main and the supporting panes, for example, a styling panel in a doc app. See
 *   [SupportingPaneScaffoldRole.Extra].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun SupportingPaneScaffold(
    directive: PaneScaffoldDirective,
    value: ThreePaneScaffoldValue,
    mainPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    supportingPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldValue = value,
        paneOrder = SupportingPaneScaffoldDefaults.PaneOrder,
        secondaryPane = supportingPane,
        tertiaryPane = extraPane,
        primaryPane = mainPane
    )
}

/**
 * An opinionated implementation of [ThreePaneScaffold] following Material guidelines that displays
 * the provided three panes in a canonical [supporting pane layout](
 * https://m3.material.io/foundations/layout/canonical-layouts/supporting-pane).
 *
 * This overload takes a [ThreePaneScaffoldState] describing the current [ThreePaneScaffoldValue]
 * and any pane transitions or animations in progress.
 *
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param scaffoldState The current state of the scaffold, containing information about the adapted
 *   value of each pane of the scaffold and the transitions/animations in progress.
 * @param mainPane the main pane of the scaffold, which is supposed to hold the major content of an
 *   app, for example, the editing screen of a doc app. See [SupportingPaneScaffoldRole.Main].
 * @param supportingPane the supporting pane of the scaffold, which is supposed to hold the support
 *   content of an app, for example, the comment list of a doc app. See
 *   [SupportingPaneScaffoldRole.Supporting].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any additional content
 *   besides the main and the supporting panes, for example, a styling panel in a doc app. See
 *   [SupportingPaneScaffoldRole.Extra].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun SupportingPaneScaffold(
    directive: PaneScaffoldDirective,
    scaffoldState: ThreePaneScaffoldState,
    mainPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    supportingPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldState = scaffoldState,
        paneOrder = SupportingPaneScaffoldDefaults.PaneOrder,
        secondaryPane = supportingPane,
        tertiaryPane = extraPane,
        primaryPane = mainPane
    )
}

/** Provides default values of [SupportingPaneScaffold]. */
@ExperimentalMaterial3AdaptiveApi
object SupportingPaneScaffoldDefaults {
    /**
     * Creates a default [ThreePaneScaffoldAdaptStrategies] for [SupportingPaneScaffold].
     *
     * @param mainPaneAdaptStrategy the adapt strategy of the main pane
     * @param supportingPaneAdaptStrategy the adapt strategy of the supporting pane
     * @param extraPaneAdaptStrategy the adapt strategy of the extra pane
     */
    fun adaptStrategies(
        mainPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        supportingPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
        extraPaneAdaptStrategy: AdaptStrategy = AdaptStrategy.Hide,
    ): ThreePaneScaffoldAdaptStrategies =
        ThreePaneScaffoldAdaptStrategies(
            mainPaneAdaptStrategy,
            supportingPaneAdaptStrategy,
            extraPaneAdaptStrategy
        )

    /**
     * Denotes [ThreePaneScaffold] to use the supporting-pane pane-order to arrange its panes
     * horizontally, which allocates panes in the order of primary, secondary, and tertiary from
     * start to end.
     */
    internal val PaneOrder =
        ThreePaneScaffoldHorizontalOrder(
            ThreePaneScaffoldRole.Primary,
            ThreePaneScaffoldRole.Secondary,
            ThreePaneScaffoldRole.Tertiary
        )
}

/**
 * The set of the available pane roles of [SupportingPaneScaffold]. Those roles map to their
 * corresponding [ThreePaneScaffoldRole], which is a generic role definition across all types of
 * three pane scaffolds. We suggest you to use the values defined here instead of the raw
 * [ThreePaneScaffoldRole] under the context of [SupportingPaneScaffold] for better code clarity.
 */
@ExperimentalMaterial3AdaptiveApi
object SupportingPaneScaffoldRole {
    /**
     * The main pane of [SupportingPaneScaffold], which is supposed to hold the major content of an
     * app, for example, the editing screen of a doc app. It maps to
     * [ThreePaneScaffoldRole.Primary].
     */
    val Main = ThreePaneScaffoldRole.Primary

    /**
     * The supporting pane of [SupportingPaneScaffold], which is supposed to hold the support
     * content of an app, for example, the comment list of a doc app. It maps to
     * [ThreePaneScaffoldRole.Secondary].
     */
    val Supporting = ThreePaneScaffoldRole.Secondary

    /**
     * The extra pane of [SupportingPaneScaffold], which is supposed to hold any additional content
     * besides the main and the supporting panes, for example, a styling panel in a doc app. It maps
     * to [ThreePaneScaffoldRole.Tertiary].
     */
    val Extra = ThreePaneScaffoldRole.Tertiary
}
