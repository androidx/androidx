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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.DefaultAnimatedPaneOverride.AnimatedPane
import androidx.compose.material3.adaptive.layout.internal.getValue
import androidx.compose.material3.adaptive.layout.internal.rememberRef
import androidx.compose.material3.adaptive.layout.internal.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp

/**
 * The root composable of pane contents in a [ThreePaneScaffold] that supports default motions
 * during pane switching. It's recommended to use this composable to wrap your own contents when
 * passing them into pane parameters of the scaffold functions, therefore your panes can have a nice
 * default animation for free.
 *
 * @param modifier The modifier applied to the [AnimatedPane].
 * @param enterTransition The [EnterTransition] used to animate the pane in.
 * @param exitTransition The [ExitTransition] used to animate the pane out.
 * @param boundsAnimationSpec The [FiniteAnimationSpec] used to animate the bounds of the pane when
 *   the pane is keeping showing but changing its size and/or position.
 * @param content The content of the [AnimatedPane]. Also see [AnimatedPaneScope].
 *
 * See usage samples at:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 */
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <
    Role : PaneScaffoldRole,
    ScaffoldValue : PaneScaffoldValue<Role>,
> ExtendedPaneScaffoldPaneScope<Role, ScaffoldValue>.AnimatedPane(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = motionDataProvider.calculateDefaultEnterTransition(paneRole),
    exitTransition: ExitTransition = motionDataProvider.calculateDefaultExitTransition(paneRole),
    boundsAnimationSpec: FiniteAnimationSpec<IntRect> = PaneMotionDefaults.AnimationSpec,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) {
    with(LocalAnimatedPaneOverride.current) {
        AnimatedPaneOverrideScope(
                scope = this@AnimatedPane,
                modifier = modifier,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                boundsAnimationSpec = boundsAnimationSpec,
                content = content,
            )
            .AnimatedPane()
    }
}

/**
 * This override provides the default behavior of the [AnimatedPane] component.
 *
 * [AnimatedPaneOverride] used when no override is specified.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalMaterial3AdaptiveComponentOverrideApi
private object DefaultAnimatedPaneOverride : AnimatedPaneOverride {
    @Composable
    override fun <
        Role : PaneScaffoldRole,
        ScaffoldValue : PaneScaffoldValue<Role>,
    > AnimatedPaneOverrideScope<Role, ScaffoldValue>.AnimatedPane() {
        with(scope) {
            val scaleConversion = { offset: IntOffset ->
                (motionDataProvider as? ThreePaneScaffoldMotionDataProvider)?.run {
                    predictiveBackScaleState.convert(offset)
                } ?: offset
            }
            val animatingBounds = paneMotion == PaneMotion.AnimateBounds
            val motionProgress = { motionProgress }
            val paneValue = scaffoldStateTransition.targetState[paneRole]
            scaffoldStateTransition.AnimatedVisibility(
                visible = { value: ScaffoldValue -> value[paneRole] != PaneAdaptedValue.Hidden },
                modifier =
                    modifier
                        .animatedPane()
                        .animateBounds(
                            animateFraction = motionProgress,
                            animationSpec = boundsAnimationSpec,
                            scaleConversion = scaleConversion,
                            lookaheadScope = this,
                            enabled = animatingBounds,
                        )
                        .semantics { isTraversalGroup = true }
                        .then(
                            if (paneValue is PaneAdaptedValue.Levitated) {
                                Modifier.shadow(AnimatedPaneDefaults.ShadowElevation)
                            } else {
                                Modifier
                            }
                        )
                        .then(if (animatingBounds) Modifier else Modifier.clipToBounds()),
                enter = enterTransition,
                exit = exitTransition,
            ) {
                scope.saveableStateHolder.SaveableStateProvider(paneRole.toString()) {
                    AnimatedPaneScope.create(this).content()
                }
            }

            var scrim by rememberRef<Scrim?>(null)
            (paneValue as? PaneAdaptedValue.Levitated)?.apply { scrim = this.scrim }
            scrim?.apply {
                // Display a scrim when the pane gets levitated
                scaffoldStateTransition.AnimatedVisibility(
                    visible = { value: ScaffoldValue ->
                        value[paneRole] != PaneAdaptedValue.Hidden
                    },
                    enter = enterTransition,
                    exit = exitTransition,
                ) {
                    Content(
                        defaultColor = ThreePaneScaffoldDefaults.ScrimColor,
                        enabled = paneValue is PaneAdaptedValue.Levitated,
                    )
                }
            }
        }
    }
}

/**
 * Scope for the content of [AnimatedPane]. It extends from the necessary animation scopes so
 * developers can use the info carried by the scopes to do certain customizations.
 */
sealed interface AnimatedPaneScope : AnimatedVisibilityScope {
    companion object {
        /** Create an instance of [AnimatedPaneScope] for the given [AnimatedVisibilityScope]. */
        @ExperimentalMaterial3AdaptiveApi
        fun create(animatedVisibilityScope: AnimatedVisibilityScope): AnimatedPaneScope =
            Impl(animatedVisibilityScope)
    }

    private class Impl(animatedVisibilityScope: AnimatedVisibilityScope) :
        AnimatedPaneScope, AnimatedVisibilityScope by animatedVisibilityScope
}

/**
 * Interface that allows libraries to override the behavior of [AnimatedPane].
 *
 * To override this component, implement the member function of this interface, then provide the
 * implementation to [AnimatedPaneOverride] in the Compose hierarchy.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalMaterial3AdaptiveComponentOverrideApi
interface AnimatedPaneOverride {
    /** Behavior function that is called by the [AnimatedPane] composable. */
    @Composable
    fun <
        Role : PaneScaffoldRole,
        ScaffoldValue : PaneScaffoldValue<Role>,
    > AnimatedPaneOverrideScope<Role, ScaffoldValue>.AnimatedPane()
}

/**
 * Parameters available to [AnimatedPane].
 *
 * @param modifier The modifier applied to the [AnimatedPane].
 * @param enterTransition The [EnterTransition] used to animate the pane in.
 * @param exitTransition The [ExitTransition] used to animate the pane out.
 * @param boundsAnimationSpec The [FiniteAnimationSpec] used to animate the bounds of the pane when
 *   the pane is keeping showing but changing its size and/or position.
 * @param content The content of the [AnimatedPane]. Also see [AnimatedPaneScope].
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@ExperimentalMaterial3AdaptiveComponentOverrideApi
@Immutable
class AnimatedPaneOverrideScope<Role : PaneScaffoldRole, ScaffoldValue : PaneScaffoldValue<Role>>
internal constructor(
    val scope: ExtendedPaneScaffoldPaneScope<Role, ScaffoldValue>,
    val modifier: Modifier,
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val boundsAnimationSpec: FiniteAnimationSpec<IntRect>,
    val content: (@Composable AnimatedPaneScope.() -> Unit),
)

/** CompositionLocal containing the currently-selected [AnimatedPaneOverride]. */
@ExperimentalMaterial3AdaptiveComponentOverrideApi
val LocalAnimatedPaneOverride: ProvidableCompositionLocal<AnimatedPaneOverride> =
    compositionLocalOf {
        DefaultAnimatedPaneOverride
    }

internal object AnimatedPaneDefaults {
    val ShadowElevation = 15.dp
}
