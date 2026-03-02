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
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveComponentOverrideApi
import androidx.compose.material3.adaptive.layout.DefaultAnimatedPaneOverride.AnimatedPane
import androidx.compose.material3.adaptive.layout.internal.Strings
import androidx.compose.material3.adaptive.layout.internal.delegableSemantics
import androidx.compose.material3.adaptive.layout.internal.getString
import androidx.compose.material3.adaptive.layout.internal.getValue
import androidx.compose.material3.adaptive.layout.internal.rememberRef
import androidx.compose.material3.adaptive.layout.internal.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
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
 * @param dragToResizeHandle The optional handle which will shown when the pane is levitated and
 *   drag-to-resizable; the handle will be draggable and clickable to resize the pane freely or
 *   among collapsed, partially expanded, and expanded states. See [rememberDragToResizeState] for
 *   more details about how to implement the drag-to-resize behavior.
 * @param content The content of the [AnimatedPane]. Also see [AnimatedPaneScope].
 *
 * See usage samples at:
 *
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPane
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog
 * @sample androidx.compose.material3.adaptive.samples.SupportingPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.SupportingPaneScaffoldSampleWithExtraPaneLevitatedAsBottomSheet
 */
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <
    RoleT : PaneScaffoldRole,
    ScaffoldValueT : PaneScaffoldValue<RoleT>,
> ExtendedPaneScaffoldPaneScope<RoleT, ScaffoldValueT>.AnimatedPane(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = motionDataProvider.calculateDefaultEnterTransition(paneRole),
    exitTransition: ExitTransition = motionDataProvider.calculateDefaultExitTransition(paneRole),
    boundsAnimationSpec: FiniteAnimationSpec<IntRect> = PaneMotionDefaults.AnimationSpec,
    dragToResizeHandle: (@Composable (DragToResizeState) -> Unit)? = null,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) {
    with(LocalAnimatedPaneOverride.current) {
        AnimatedPaneOverrideScope(
                scope = this@AnimatedPane,
                modifier = modifier,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                boundsAnimationSpec = boundsAnimationSpec,
                dragToResizeHandle = dragToResizeHandle,
                content = content,
            )
            .AnimatedPane()
    }
}

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
 * @sample androidx.compose.material3.adaptive.samples.ListDetailPaneScaffoldSampleWithExtraPaneLevitatedAsDialog
 * @sample androidx.compose.material3.adaptive.samples.SupportingPaneScaffoldSample
 * @sample androidx.compose.material3.adaptive.samples.SupportingPaneScaffoldSampleWithExtraPaneLevitatedAsBottomSheet
 */
@Deprecated(
    message = "Keep the old function for binary compatibility",
    level = DeprecationLevel.HIDDEN,
)
@OptIn(ExperimentalMaterial3AdaptiveComponentOverrideApi::class)
@ExperimentalMaterial3AdaptiveApi
@Composable
fun <
    RoleT : PaneScaffoldRole,
    ScaffoldValueT : PaneScaffoldValue<RoleT>,
> ExtendedPaneScaffoldPaneScope<RoleT, ScaffoldValueT>.AnimatedPane(
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = motionDataProvider.calculateDefaultEnterTransition(paneRole),
    exitTransition: ExitTransition = motionDataProvider.calculateDefaultExitTransition(paneRole),
    boundsAnimationSpec: FiniteAnimationSpec<IntRect> = PaneMotionDefaults.AnimationSpec,
    content: (@Composable AnimatedPaneScope.() -> Unit),
) = AnimatedPane(modifier, enterTransition, exitTransition, boundsAnimationSpec, null, content)

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
            val (paneModifier, contentModifier) = modifier.splitPaneAndContentModifiers()
            scaffoldStateTransition.AnimatedVisibility(
                visible = { value: ScaffoldValue -> value[paneRole] != PaneAdaptedValue.Hidden },
                modifier =
                    Modifier.animatedPane()
                        .animateBounds(
                            animateFraction = motionProgress,
                            animationSpec = boundsAnimationSpec,
                            scaleConversion = scaleConversion,
                            lookaheadScope = this,
                            enabled = animatingBounds,
                        )
                        .focusRequester(focusRequesters[paneRole]!!)
                        .focusableInWholeTree(isInteractable, paneRole)
                        // This is a workaround to b/375496210 - shadows cannot be faded so we have
                        // to apply shadows on AnimatedVisibility instead of the content.
                        .levitatedProperties(paneValue, dragToResizeHandle != null)
                        .then(if (animatingBounds) Modifier else Modifier.clipToBounds())
                        // The pane modifiers contains:
                        // 1. Size modifiers that have to be applied at this level so the scaffold
                        //    can read them from the parent data.
                        // 2. The graphics layer modifiers, which have to be applied last so they
                        //    can take effect on modifiers (like, shadows) applied before them.
                        .then(paneModifier),
                enter = enterTransition,
                exit = exitTransition,
            ) {
                scope.saveableStateHolder.SaveableStateProvider(paneRole.toString()) {
                    Column(modifier = contentModifier) {
                        if (
                            paneValue is PaneAdaptedValue.Levitated &&
                                paneValue.dragToResizeState != null &&
                                dragToResizeHandle != null
                        ) {
                            Box(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .wrapContentHeight()
                                        .dragToResize(
                                            state = paneValue.dragToResizeState,
                                            showIndication = true,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                dragToResizeHandle(paneValue.dragToResizeState)
                            }
                        }
                        AnimatedPaneScope.create(this@AnimatedVisibility).content()
                    }
                }
            }

            var scrim by rememberRef<(@Composable () -> Unit)?>(null)
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
                    this@apply.invoke()
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
 * @param dragToResizeHandle The optional handle which will shown when the pane is levitated and
 *   drag-to-resizable; the handle will be draggable and clickable to resize the pane freely or
 *   among collapsed, partially expanded, and expanded states. See [rememberDragToResizeState] for
 *   more details about how to implement the drag-to-resize behavior.
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
    val dragToResizeHandle: (@Composable (DragToResizeState) -> Unit)?,
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

private fun Modifier.focusableInWholeTree(focusable: Boolean, role: PaneScaffoldRole): Modifier =
    this
        // Workaround(b/342653995): Make the whole pane a focus group but cancel any focusing
        //   attempts so the whole subtree won't be focusable.
        .focusGroup()
        .focusProperties {
            if (!focusable) {
                canFocus = false
                onEnter = { cancelFocusChange() }
            }
        }
        .then(
            if (focusable) {
                Modifier.delegableSemantics {
                    isTraversalGroup = true
                    role.paneTitle()?.apply { paneTitle = getString(this) }
                }
            } else {
                // Workaround(b/343950986): clear all semantics under the tree so no children can
                //   get the a11y focus.
                Modifier.clearAndSetSemantics {}
            }
        )

private fun PaneScaffoldRole.paneTitle() =
    (this as? ThreePaneScaffoldRole).run {
        when (this) {
            ThreePaneScaffoldRole.Primary -> Strings.defaultPaneTitlePrimary
            ThreePaneScaffoldRole.Secondary -> Strings.defaultPaneTitleSecondary
            ThreePaneScaffoldRole.Tertiary -> Strings.defaultPaneTitleTertiary
            else -> null
        }
    }

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun Modifier.levitatedProperties(
    paneValue: PaneAdaptedValue,
    hasDragHandle: Boolean,
): Modifier =
    if (paneValue !is PaneAdaptedValue.Levitated) {
        this
    } else {
        this.shadow(AnimatedPaneDefaults.ShadowElevation)
            .then(
                if (paneValue.dragToResizeState != null && !hasDragHandle) {
                    // Make the whole pane draggable if no drag handle is provided.
                    Modifier.dragToResize(paneValue.dragToResizeState, showIndication = false)
                } else {
                    Modifier
                }
            )
    }
