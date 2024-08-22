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
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.LookaheadScope

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
    motionScope: PaneScaffoldMotionScope,
    transitionScope: PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue>,
    lookaheadScope: LookaheadScope
) :
    ThreePaneScaffoldScope,
    PaneScaffoldMotionScope by motionScope,
    PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue> by transitionScope,
    LookaheadScope by lookaheadScope,
    PaneScaffoldScopeImpl()

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class ThreePaneScaffoldPaneScopeImpl(
    override val paneRole: ThreePaneScaffoldRole,
    scaffoldScope: ThreePaneScaffoldScope,
) : ThreePaneScaffoldPaneScope, ThreePaneScaffoldScope by scaffoldScope {
    override var paneMotion: PaneMotion by mutableStateOf(DefaultPaneMotion.ExitToLeft)
        private set

    fun updatePaneMotion(
        paneMotions: List<PaneMotion>,
        paneOrder: ThreePaneScaffoldHorizontalOrder
    ) {
        paneMotion = paneMotions[paneOrder.indexOf(paneRole)]
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal class ThreePaneScaffoldTransitionScopeImpl :
    PaneScaffoldTransitionScope<ThreePaneScaffoldRole, ThreePaneScaffoldValue> {
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
