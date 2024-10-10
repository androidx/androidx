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

import androidx.compose.animation.core.Animatable
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneExpansionDragHandle
import androidx.compose.material3.adaptive.layout.PaneExpansionState
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffold
import androidx.compose.material3.adaptive.layout.SupportingPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneMotion
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldPaneScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldScope
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.layout.calculateListDetailPaneScaffoldMotion
import androidx.compose.material3.adaptive.layout.calculateSupportingPaneScaffoldMotion
import androidx.compose.material3.adaptive.layout.rememberPaneExpansionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * A version of [ListDetailPaneScaffold] that supports navigation and predictive back handling out
 * of the box, controlled by [ThreePaneScaffoldNavigator].
 *
 * @param navigator The navigator instance to navigate through the scaffold.
 * @param listPane the list pane of the scaffold, which is supposed to hold a list of item summaries
 *   that can be selected from, for example, the inbox mail list of a mail app. See
 *   [ListDetailPaneScaffoldRole.List].
 * @param detailPane the detail pane of the scaffold, which is supposed to hold the detailed info of
 *   a selected item, for example, the mail content currently being viewed. See
 *   [ListDetailPaneScaffoldRole.Detail].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any supplementary info
 *   besides the list and the detail panes, for example, a task list or a mini-calendar view of a
 *   mail app. See [ListDetailPaneScaffoldRole.Extra].
 * @param defaultBackBehavior the default back navigation behavior when the system back event
 *   happens. See [BackNavigationBehavior] for the use cases of each behavior.
 * @param paneMotions The specified motion of the panes. By default the value will be calculated by
 *   [calculateListDetailPaneScaffoldMotion] according to the target [ThreePaneScaffoldValue].
 * @param paneExpansionDragHandle the pane expansion drag handle to let users be able to drag to
 *   change pane expansion state. Note that by default this argument will be `null`, and there won't
 *   be a drag handle rendered and users won't be able to drag to change the pane split. You can
 *   provide a [PaneExpansionDragHandle] here as our sample suggests. On the other hand, even if
 *   there's no drag handle, you can still modify [paneExpansionState] directly to apply pane
 *   expansion.
 * @param paneExpansionState the state object of pane expansion.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <T> NavigableListDetailPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<T>,
    listPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    detailPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    defaultBackBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
    paneMotions: ThreePaneMotion = navigator.scaffoldState.calculateListDetailPaneScaffoldMotion(),
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState = rememberPaneExpansionState(navigator.scaffoldValue),
) {
    val predictiveBackScale = remember { Animatable(initialValue = 1f) }

    ThreePaneScaffoldPredictiveBackHandler(
        navigator = navigator,
        backBehavior = defaultBackBehavior,
        scale = predictiveBackScale,
    )

    ListDetailPaneScaffold(
        modifier = modifier.predictiveBackTransform(predictiveBackScale::value),
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        detailPane = detailPane,
        listPane = listPane,
        extraPane = extraPane,
        paneMotions = paneMotions,
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
 *   app, for example, the editing screen of a doc app. See [SupportingPaneScaffoldRole.Main].
 * @param supportingPane the supporting pane of the scaffold, which is supposed to hold the support
 *   content of an app, for example, the comment list of a doc app. See
 *   [SupportingPaneScaffoldRole.Supporting].
 * @param modifier [Modifier] of the scaffold layout.
 * @param extraPane the extra pane of the scaffold, which is supposed to hold any additional content
 *   besides the main and the supporting panes, for example, a styling panel in a doc app. See
 *   [SupportingPaneScaffoldRole.Extra].
 * @param defaultBackBehavior the default back navigation behavior when the system back event
 *   happens. See [BackNavigationBehavior] for the use cases of each behavior.
 * @param paneMotions The specified motion of the panes. By default the value will be calculated by
 *   [calculateSupportingPaneScaffoldMotion] according to the target [ThreePaneScaffoldValue].
 * @param paneExpansionDragHandle the pane expansion drag handle to let users be able to drag to
 *   change pane expansion state. Note that by default this argument will be `null`, and there won't
 *   be a drag handle rendered and users won't be able to drag to change the pane split. You can
 *   provide a [PaneExpansionDragHandle] here as our sample suggests. On the other hand, even if
 *   there's no drag handle, you can still modify [paneExpansionState] directly to apply pane
 *   expansion.
 * @param paneExpansionState the state object of pane expansion.
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <T> NavigableSupportingPaneScaffold(
    navigator: ThreePaneScaffoldNavigator<T>,
    mainPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    supportingPane: @Composable ThreePaneScaffoldPaneScope.() -> Unit,
    modifier: Modifier = Modifier,
    extraPane: (@Composable ThreePaneScaffoldPaneScope.() -> Unit)? = null,
    defaultBackBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilContentChange,
    paneMotions: ThreePaneMotion = navigator.scaffoldState.calculateSupportingPaneScaffoldMotion(),
    paneExpansionDragHandle: (@Composable ThreePaneScaffoldScope.(PaneExpansionState) -> Unit)? =
        null,
    paneExpansionState: PaneExpansionState = rememberPaneExpansionState(navigator.scaffoldValue),
) {
    val predictiveBackScale = remember { Animatable(initialValue = 1f) }

    ThreePaneScaffoldPredictiveBackHandler(
        navigator = navigator,
        backBehavior = defaultBackBehavior,
        scale = predictiveBackScale,
    )

    SupportingPaneScaffold(
        modifier = modifier.predictiveBackTransform(predictiveBackScale::value),
        directive = navigator.scaffoldDirective,
        scaffoldState = navigator.scaffoldState,
        mainPane = mainPane,
        supportingPane = supportingPane,
        extraPane = extraPane,
        paneMotions = paneMotions,
        paneExpansionDragHandle = paneExpansionDragHandle,
        paneExpansionState = paneExpansionState,
    )
}

private fun Modifier.predictiveBackTransform(scale: () -> Float): Modifier = graphicsLayer {
    val scaleValue = scale()
    scaleX = scaleValue
    scaleY = scaleValue
    transformOrigin = TransformOriginTopCenter
}

private val TransformOriginTopCenter = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 0f)
