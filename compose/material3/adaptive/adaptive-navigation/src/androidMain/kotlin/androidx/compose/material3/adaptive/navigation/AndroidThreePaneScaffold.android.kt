/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.navigation

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A version of [ListDetailPaneScaffold] that supports navigation and predictive back handling out
 * of the box, controlled by [ThreePaneScaffoldNavigator].
 *
 * Example usage, including integration with the Compose Navigation library:
 *
 * @sample androidx.compose.material3.adaptive.samples.NavigableListDetailPaneScaffoldSample
 * @param navigator The navigator instance to navigate through the scaffold.
 * @param listPane the list pane of the scaffold, which is supposed to hold a list of item summaries
 *   that can be selected from, for example, the inbox mail list of a mail app. See
 *   [ListDetailPaneScaffoldRole.List]. Note that we suggest you to use [AnimatedPane] as the root
 *   layout of panes, which supports default pane behaviors like enter/exit transitions.
 * @param detailPane the detail pane of the scaffold, which is supposed to hold the detailed info of
 *   a selected item, for example, the mail content currently being viewed. See
 *   [ListDetailPaneScaffoldRole.Detail]. Note that we suggest you to use [AnimatedPane] as the root
 *   layout of panes, which supports default pane behaviors like enter/exit transitions.
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any supplementary info
 *   besides the list and the detail panes, for example, a task list or a mini-calendar view of a
 *   mail app. See [ListDetailPaneScaffoldRole.Extra]. Note that we suggest you to use
 *   [AnimatedPane] as the root layout of panes, which supports default pane behaviors like
 *   enter/exit transitions.
 * @param defaultBackBehavior the default back navigation behavior when the system back event
 *   happens. See [BackNavigationBehavior] for the use cases of each behavior.
 * @param paneExpansionDragHandle the pane expansion drag handle to allow users to drag to change
 *   pane expansion state, `null` by default.
 * @param paneExpansionState the state object of pane expansion; when no value is provided but
 *   [paneExpansionDragHandle] is not `null`, a default implementation will be created for the drag
 *   handle to use.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <T> NavigableListDetailPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<T>,
    listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    defaultBackBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilScaffoldValueChange,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState? = null,
) {
    ThreePaneScaffoldPredictiveBackHandler(
        navigator = navigator,
        backBehavior = defaultBackBehavior,
    )

    ListDetailPaneScaffold(
        modifier = modifier,
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        detailPane = detailPane,
        listPane = listPane,
        extraPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = paneExpansionState,
    )
}

/**
 * A version of [SupportingPaneScaffold] that supports navigation and predictive back handling out
 * of the box, controlled by [ThreePaneScaffoldNavigator].
 *
 * @param navigator The navigator instance to navigate through the scaffold.
 * @param mainPane the main pane of the scaffold, which is supposed to hold the major content of an
 *   app, for example, the editing screen of a doc app. See [SupportingPaneScaffoldRole.Main]. Note
 *   that we suggest you to use [AnimatedPane] as the root layout of panes, which supports default
 *   pane behaviors like enter/exit transitions.
 * @param supportingPane the supporting pane of the scaffold, which is supposed to hold the support
 *   content of an app, for example, the comment list of a doc app. See
 *   [SupportingPaneScaffoldRole.Supporting]. Note that we suggest you to use [AnimatedPane] as the
 *   root layout of panes, which supports default pane behaviors like enter/exit transitions.
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any additional content
 *   besides the main and the supporting panes, for example, a styling panel in a doc app. See
 *   [SupportingPaneScaffoldRole.Extra]. Note that we suggest you to use [AnimatedPane] as the root
 *   layout of panes, which supports default pane behaviors like enter/exit transitions.
 * @param defaultBackBehavior the default back navigation behavior when the system back event
 *   happens. See [BackNavigationBehavior] for the use cases of each behavior.
 * @param paneExpansionDragHandle the pane expansion drag handle to allow users to drag to change
 *   pane expansion state, `null` by default.
 * @param paneExpansionState the state object of pane expansion; when no value is provided but
 *   [paneExpansionDragHandle] is not `null`, a default implementation will be created for the drag
 *   handle to use.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <T> NavigableSupportingPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<T>,
    mainPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    supportingPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    defaultBackBehavior: BackNavigationBehavior =
        BackNavigationBehavior.PopUntilScaffoldValueChange,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState? = null,
) {
    ThreePaneScaffoldPredictiveBackHandler(
        navigator = navigator,
        backBehavior = defaultBackBehavior,
    )

    SupportingPaneScaffold(
        modifier = modifier,
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        mainPane = mainPane,
        supportingPane = supportingPane,
        extraPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = paneExpansionState,
    )
}
