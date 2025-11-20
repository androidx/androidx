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

package androidx.compose.material3.adaptive.layout

import androidx.compose.animation.core.Transition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.Dp

/** Scope for the panes of [ThreePaneScaffold]. */
@ExperimentalMaterial3AdaptiveApi
sealed interface ThreePaneScaffoldScope :
    ExtendedPaneScaffoldScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue>

/** Scope for the panes of [ThreePaneScaffold]. */
@ExperimentalMaterial3AdaptiveApi
sealed interface ThreePaneScaffoldPaneScope :
    ThreePaneScaffoldScope,
    ExtendedPaneScaffoldPaneScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue>

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class ThreePaneScaffoldScopeImpl(
    transitionScope: PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue>,
    lookaheadScope: LookaheadScope,
    saveableStateHolder: SaveableStateHolder,
    override val focusRequesters: Map<ThreePaneScaffoldRole, FocusRequester>,
) :
    ThreePaneScaffoldScope,
    PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue> by transitionScope,
    LookaheadScope by lookaheadScope,
    PaneScaffoldScopeImpl(saveableStateHolder) {

    override fun Modifier.paneExpansionDraggable(
        state: PaneExpansionState,
        minTouchTargetSize: Dp,
        interactionSource: MutableInteractionSource,
        semanticsProperties: (SemanticsPropertyReceiver.() -> Unit)?,
    ): Modifier =
        this.paneExpansionDraggable(
            state = state,
            minTouchTargetSize = minTouchTargetSize,
            interactionSource = interactionSource,
            semanticsProperties = semanticsProperties,
            animateFraction = { motionProgress },
            lookaheadScope = this@ThreePaneScaffoldScopeImpl,
        )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun rememberThreePaneScaffoldPaneScope(
    paneRole: ThreePaneScaffoldRole,
    scaffoldScope: ThreePaneScaffoldScope,
    paneMotion: PaneMotion,
    isInteractable: Boolean,
): ThreePaneScaffoldPaneScope =
    remember(scaffoldScope) { ThreePaneScaffoldPaneScopeImpl(paneRole, scaffoldScope) }
        .apply {
            this.paneMotion = paneMotion
            this.isInteractable = isInteractable
        }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Stable
internal class ThreePaneScaffoldPaneScopeImpl(
    override val paneRole: ThreePaneScaffoldRole,
    scaffoldScope: ThreePaneScaffoldScope,
) : ThreePaneScaffoldPaneScope, ThreePaneScaffoldScope by scaffoldScope {
    override var paneMotion: PaneMotion by mutableStateOf(PaneMotion.ExitToLeft)

    override var isInteractable: Boolean by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class ThreePaneScaffoldTransitionScopeImpl(
    override val motionDataProvider: PaneScaffoldMotionDataProvider<ThreePaneScaffoldRole>
) : PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue> {
    override val motionProgress: Float
        get() =
            if (transitionState.currentState == transitionState.targetState) {
                1f
            } else {
                transitionState.progressFraction
            }

    override lateinit var scaffoldStateTransition: Transition<ThreePaneScaffoldValue>
    lateinit var transitionState: ThreePaneScaffoldState
}
