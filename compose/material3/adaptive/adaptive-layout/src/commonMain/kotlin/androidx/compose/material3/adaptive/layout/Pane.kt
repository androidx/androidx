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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds

/**
 * The root composable of pane contents in a [ThreePaneScaffold] that supports default motions
 * during pane switching. It's recommended to use this composable to wrap your own contents when
 * passing them into pane parameters of the scaffold functions, therefore your panes can have a nice
 * default animation for free.
 *
 * @param modifier The modifier applied to the [AnimatedPane].
 * @param content The content of the [AnimatedPane]. Also see [AnimatedPaneScope].
 *
 * See usage samples at:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 */
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <S, T : PaneScaffoldValue<S>> ExtendedPaneScaffoldPaneScope<S, T>.AnimatedPane(
    modifier: Modifier = Modifier,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) {
    val animatingBounds = paneMotion == PaneMotion.AnimateBounds
    val motionProgress = { motionProgress }
    scaffoldStateTransition.AnimatedVisibility(
        visible = { value: T -> value[paneRole] != PaneAdaptedValue.Hidden },
        modifier =
            modifier
                .animatedPane()
                .animateBounds(
                    motionProgress,
                    sizeAnimationSpec,
                    positionAnimationSpec,
                    this,
                    animatingBounds
                )
                .then(if (animatingBounds) Modifier else Modifier.clipToBounds()),
        enter = with(paneMotion) { enterTransition },
        exit = with(paneMotion) { exitTransition }
    ) {
        AnimatedPaneScopeImpl(this).content()
    }
}

/**
 * Scope for the content of [AnimatedPane]. It extends from the necessary animation scopes so
 * developers can use the info carried by the scopes to do certain customizations.
 */
sealed interface AnimatedPaneScope : AnimatedVisibilityScope

private class AnimatedPaneScopeImpl(animatedVisibilityScope: AnimatedVisibilityScope) :
    AnimatedPaneScope, AnimatedVisibilityScope by animatedVisibilityScope
