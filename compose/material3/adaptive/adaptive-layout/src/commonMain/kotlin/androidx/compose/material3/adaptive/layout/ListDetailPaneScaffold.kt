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
 * A three pane layout that follows the Material guidelines, displaying the provided panes in a
 * canonical
 * [list-detail layout](https://m3.material.io/foundations/layout/canonical-layouts/list-detail).
 *
 * This overload takes a [ThreePaneScaffoldValue] describing the adapted value of each pane within
 * the scaffold.
 *
 * Here's a basic usage sample, which demonstrates how a layout can change from single pane to dual
 * pane under different window configurations:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 *
 * For a more sophisticated sample that supports an extra pane and pane expansion functionality that
 * allows users to drag to change layout split, see:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 *
 * By default there isn't a drag handle rendered so users aren't able to drag to change the pane
 * split. Providing a drag handle like the above sample shows will enable the functionality. We
 * suggest developers to use the vertical drag handle implementation provided by the Material3
 * component library here to have default theming/styling support. You can integrate the component
 * as the following sample shows:
 *
 * @sample androidx.compose.material3.adaptive.samples.PaneExpansionDragHandleSample
 *
 * Note that if there's no drag handle, you can still modify [paneExpansionState] directly to apply
 * pane expansion.
 *
 * The following code gives a sample of how to integrate with the Compose Navigation library:
 *
 * @sample androidx.compose.material3.adaptive.samples.NavigableListDetailPaneScaffoldSample
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param value The current adapted value of the scaffold, which indicates how each pane of the
 *   scaffold is adapted.
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
 * @param paneExpansionDragHandle the pane expansion drag handle to allow users to drag to change
 *   pane expansion state, `null` by default.
 * @param paneExpansionState the state object of pane expansion; when no value is provided but
 *   [paneExpansionDragHandle] is not `null`, a default implementation will be created for the drag
 *   handle to use.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    directive: PaneScaffoldDirective,
    value: ThreePaneScaffoldValue,
    listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState? = null,
) {
    val expansionState =
        paneExpansionState
            ?: rememberDefaultPaneExpansionState(
                keyProvider = { value },
                mutable = paneExpansionDragHandle != null
            )
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldValue = value,
        paneOrder = ListDetailPaneScaffoldDefaults.PaneOrder,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = expansionState,
        primaryPane = detailPane
    )
}

/**
 * A three pane layout that follows the Material guidelines, displaying the provided panes in a
 * canonical
 * [list-detail layout](https://m3.material.io/foundations/layout/canonical-layouts/list-detail).
 *
 * This overload takes a [ThreePaneScaffoldState] describing the current [ThreePaneScaffoldValue]
 * and any pane transitions or animations in progress.
 *
 * Here's a basic usage sample, which demonstrates how a layout can change from single pane to dual
 * pane under different window configurations:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 *
 * For a more sophisticated sample that supports an extra pane and pane expansion functionality that
 * allows users to drag to change layout split, see:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 *
 * By default there isn't a drag handle rendered so users aren't able to drag to change the pane
 * split. Providing a drag handle like the above sample shows will enable the functionality. We
 * suggest developers to use the vertical drag handle implementation provided by the Material3
 * component library here to have default theming/styling support. You can integrate the component
 * as the following sample shows:
 *
 * @sample androidx.compose.material3.adaptive.samples.PaneExpansionDragHandleSample
 *
 * Note that if there's no drag handle, you can still modify [paneExpansionState] directly to apply
 * pane expansion.
 *
 * The following code gives a sample of how to integrate with the Compose Navigation library:
 *
 * @sample androidx.compose.material3.adaptive.samples.NavigableListDetailPaneScaffoldSample
 * @param directive The top-level directives about how the scaffold should arrange its panes.
 * @param scaffoldState The current state of the scaffold, containing information about the adapted
 *   value of each pane of the scaffold and the transitions/animations in progress.
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
 * @param paneExpansionDragHandle the pane expansion drag handle to allow users to drag to change
 *   pane expansion state, `null` by default.
 * @param paneExpansionState the state object of pane expansion; when no value is provided but
 *   [paneExpansionDragHandle] is not `null`, a default implementation will be created for the drag
 *   handle to use.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun ListDetailPaneScaffold(
    directive: PaneScaffoldDirective,
    scaffoldState: ThreePaneScaffoldState,
    listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState? = null,
) {
    val expansionState =
        paneExpansionState
            ?: rememberDefaultPaneExpansionState(
                keyProvider = { scaffoldState.targetState },
                mutable = paneExpansionDragHandle != null
            )
    ThreePaneScaffold(
        modifier = modifier.fillMaxSize(),
        scaffoldDirective = directive,
        scaffoldState = scaffoldState,
        paneOrder = ListDetailPaneScaffoldDefaults.PaneOrder,
        secondaryPane = listPane,
        tertiaryPane = extraPane,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = expansionState,
        primaryPane = detailPane
    )
}

/** Provides default values of [ListDetailPaneScaffold]. */
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

    /**
     * Denotes [ThreePaneScaffold] to use the list-detail pane-order to arrange its panes
     * horizontally, which allocates panes in the order of secondary, primary, and tertiary from
     * start to end.
     */
    internal val PaneOrder =
        ThreePaneScaffoldHorizontalOrder(
            ThreePaneScaffoldRole.Secondary,
            ThreePaneScaffoldRole.Primary,
            ThreePaneScaffoldRole.Tertiary
        )
}

/**
 * The set of the available pane roles of [ListDetailPaneScaffold]. Those roles map to their
 * corresponding [ThreePaneScaffoldRole], which is a generic role definition across all types of
 * three pane scaffolds. We suggest you to use the values defined here instead of the raw
 * [ThreePaneScaffoldRole] under the context of [ListDetailPaneScaffold] for better code clarity.
 */
object ListDetailPaneScaffoldRole {
    /**
     * The list pane of [ListDetailPaneScaffold], which is supposed to hold a list of item summaries
     * that can be selected from, for example, the inbox mail list of a mail app. It maps to
     * [ThreePaneScaffoldRole.Secondary].
     */
    val List = ThreePaneScaffoldRole.Secondary

    /**
     * The detail pane of [ListDetailPaneScaffold], which is supposed to hold the detailed info of a
     * selected item, for example, the mail content currently being viewed. It maps to
     * [ThreePaneScaffoldRole.Primary].
     */
    val Detail = ThreePaneScaffoldRole.Primary

    /**
     * The extra pane of [ListDetailPaneScaffold], which is supposed to hold any supplementary info
     * besides the list and the detail panes, for example, a task list or a mini-calendar view of a
     * mail app. It maps to [ThreePaneScaffoldRole.Tertiary].
     */
    val Extra = ThreePaneScaffoldRole.Tertiary
}
