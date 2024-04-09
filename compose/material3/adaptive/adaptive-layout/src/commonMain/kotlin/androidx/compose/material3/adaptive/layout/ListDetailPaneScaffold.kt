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
 * A Material opinionated implementation of [ThreePaneScaffold] that will display the provided three
 * panes in a canonical list-detail layout.
 *
 * See usage samples at:
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldWithNavigationSample
 *
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param value The current adapted value of the scaffold, which indicates how each pane of
 *        the scaffold is adapted.
 * @param listPane the list pane of the scaffold, which is supposed to hold a list of item summaries
 *        that can be selected from, for example, the inbox mail list of a mail app. See
 *        [ListDetailPaneScaffoldRole.List].
 * @param detailPane the detail pane of the scaffold, which is supposed to hold the detailed info
 *        of a selected item, for example, the mail content currently being viewed. See
 *        [ListDetailPaneScaffoldRole.Detail].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any supplementary info
 *        besides the list and the detail panes, for example, a task list or a mini-calendar view of
 *        a mail app. See [ListDetailPaneScaffoldRole.Extra].
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    directive: PaneScaffoldDirective,
    value: ThreePaneScaffoldValue,
    listPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldScope.() -> Unit)? = null,
) {
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldValue = value,
        paneOrder = ThreePaneScaffoldDefaults.ListDetailLayoutPaneOrder,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        primaryPane = detailPane
    )
}

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
 * The set of the available pane roles of [ListDetailPaneScaffold]. Those roles map to their
 * corresponding [ThreePaneScaffoldRole], which is a generic role definition across all types
 * of three pane scaffolds. We suggest you to use the values defined here instead of the raw
 * [ThreePaneScaffoldRole] under the context of [ListDetailPaneScaffold] for better code clarity.
 */
@ExperimentalMaterial3AdaptiveApi
object ListDetailPaneScaffoldRole {
    /**
     * The list pane of [ListDetailPaneScaffold], which is supposed to hold a list of item summaries
     * that can be selected from, for example, the inbox mail list of a mail app. It maps to
     * [ThreePaneScaffoldRole.Secondary].
     */
    val List = ThreePaneScaffoldRole.Secondary

    /**
     * The detail pane of [ListDetailPaneScaffold], which is supposed to hold the detailed info
     * of a selected item, for example, the mail content currently being viewed. It maps to
     * [ThreePaneScaffoldRole.Primary].
     */
    val Detail = ThreePaneScaffoldRole.Primary

    /**
     * The extra pane of [ListDetailPaneScaffold], which is supposed to hold any supplementary info
     * besides the list and the detail panes, for example, a task list or a mini-calendar view of
     * a mail app. It maps to [ThreePaneScaffoldRole.Tertiary].
     */
    val Extra = ThreePaneScaffoldRole.Tertiary
}
