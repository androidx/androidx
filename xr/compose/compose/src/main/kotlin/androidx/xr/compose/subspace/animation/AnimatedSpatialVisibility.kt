/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.subspace.animation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier

/**
 * [AnimatedSpatialVisibility] composable animates the appearance and disappearance of its subspace
 * content, as [visible] value changes. Different [SpatialEnterTransition]s and
 * [SpatialExitTransition]s can be defined in [enter] and [exit] for the appearance and
 * disappearance animation. There are 4 types of [SpatialEnterTransition] and
 * [SpatialExitTransition]: Fade, Expand/Shrink, Scale and Slide. The enter transitions can be
 * combined using `+`. Same for exit transitions. The order of the combination does not matter, as
 * the transition animations will start simultaneously. See [SpatialEnterTransition] and
 * [SpatialExitTransition] for details on the three types of transition.
 *
 * @param visible defines whether the content should be visible
 * @param modifier modifier for the [SubspaceLayout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation
 * @param exit ExitTransition(s) used for the disappearing animation
 * @param label A label to differentiate from other animations in Android Studio animation preview.
 * @param content Content to appear or disappear based on the value of [visible]
 * @see androidx.compose.animation.AnimatedVisibility
 * @see SpatialEnterTransition
 * @see SpatialExitTransition
 * @see AnimatedSpatialVisibilityScope
 */
@Composable
@SubspaceComposable
public fun AnimatedSpatialVisibility(
    visible: Boolean,
    modifier: SubspaceModifier = SubspaceModifier,
    enter: SpatialEnterTransition = SpatialTransitionDefaults.DefaultEnter,
    exit: SpatialExitTransition = SpatialTransitionDefaults.DefaultExit,
    label: String = "AnimatedSpatialVisibility",
    content: @Composable @SubspaceComposable AnimatedSpatialVisibilityScope.() -> Unit,
) {
    val transition: Transition<Boolean> = updateTransition(targetState = visible, label = label)
    AnimatedSpatialVisibilityImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * [AnimatedSpatialVisibility] composable animates the appearance and disappearance of its content,
 * as [visibleState]'s [targetState][MutableTransitionState.targetState] changes. The [visibleState]
 * can also be used to observe the state of [AnimatedSpatialVisibility]. For example:
 * `visibleState.isIdle` indicates whether all the animations have finished in
 * [AnimatedSpatialVisibility], and `visibleState.currentState` returns the initial state of the
 * current animations.
 *
 * @param visibleState defines whether the content should be visible
 * @param modifier modifier for the [SubspaceLayout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation
 * @param exit ExitTransition(s) used for the disappearing animation
 * @param label A label to differentiate from other animations in Android Studio animation preview.
 * @param content Content to appear or disappear based on the value of [visibleState]
 * @see androidx.compose.animation.AnimatedVisibility
 * @see SpatialEnterTransition
 * @see SpatialExitTransition
 * @see AnimatedSpatialVisibilityScope
 */
@Composable
@SubspaceComposable
public fun AnimatedSpatialVisibility(
    visibleState: MutableTransitionState<Boolean>,
    modifier: SubspaceModifier = SubspaceModifier,
    enter: SpatialEnterTransition = SpatialTransitionDefaults.DefaultEnter,
    exit: SpatialExitTransition = SpatialTransitionDefaults.DefaultExit,
    label: String = "AnimatedSpatialVisibility",
    content: @Composable @SubspaceComposable AnimatedSpatialVisibilityScope.() -> Unit,
) {
    val transition = rememberTransition(visibleState, label)
    AnimatedSpatialVisibilityImpl(transition, { it }, modifier, enter, exit, content)
}

/**
 * This extension function creates an [AnimatedSpatialVisibility] composable as a child Transition
 * of the given Transition. This means: 1) the enter/exit transition is now triggered by the
 * provided [Transition]'s [targetState][Transition.targetState] change. When the targetState
 * changes, the visibility will be derived using the [visible] lambda and
 * [Transition.targetState]. 2) The enter/exit transitions, as well as any custom enter/exit
 * animations defined in [AnimatedSpatialVisibility] are now hoisted to the parent Transition. The
 * parent Transition will wait for all of them to finish before it considers itself finished (i.e.
 * [Transition.currentState] = [Transition.targetState]), and subsequently removes the content in
 * the exit case.
 *
 * @param visible defines whether the content should be visible based on transition state T
 * @param modifier modifier for the [SubspaceLayout] created to contain the [content]
 * @param enter EnterTransition(s) used for the appearing animation
 * @param exit ExitTransition(s) used for the disappearing animation
 * @param content Content to appear or disappear based on the visibility derived from the
 *   [Transition.targetState] and the provided [visible] lambda
 * @see androidx.compose.animation.AnimatedVisibility
 * @see SpatialEnterTransition
 * @see SpatialExitTransition
 * @see AnimatedSpatialVisibilityScope
 */
@Composable
@SubspaceComposable
public fun <T> Transition<T>.AnimatedSpatialVisibility(
    visible: (T) -> Boolean,
    modifier: SubspaceModifier = SubspaceModifier,
    enter: SpatialEnterTransition = SpatialTransitionDefaults.DefaultEnter,
    exit: SpatialExitTransition = SpatialTransitionDefaults.DefaultExit,
    content: @Composable @SubspaceComposable AnimatedSpatialVisibilityScope.() -> Unit,
) {
    AnimatedSpatialVisibilityImpl(this, visible, modifier, enter, exit, content)
}

/**
 * This is the scope for the content of [AnimatedSpatialVisibility]. In this scope, direct and
 * indirect children of [AnimatedSpatialVisibility] will be able to define their own enter/exit
 * transitions using the built-in options via [SubspaceModifier.animateEnterExit]. They will also be
 * able define custom enter/exit animations using the [transition] object.
 * [AnimatedSpatialVisibility] will ensure both custom and built-in enter/exit animations finish
 * before it considers itself idle, and subsequently removes its content in the case of exit.
 *
 * __Note:__ Custom enter/exit animations that are created *independent* of the
 * [AnimatedSpatialVisibilityScope.transition] will have no guarantee to finish when exiting, as
 * [AnimatedSpatialVisibility] would have no visibility of such animations.
 *
 * @see AnimatedVisibilityScope
 */
public class AnimatedSpatialVisibilityScope
internal constructor(override val transition: Transition<EnterExitState>) :
    AnimatedVisibilityScope {
    @Composable
    public fun SubspaceModifier.animateEnterExit(
        enter: SpatialEnterTransition = SpatialTransitionDefaults.DefaultEnter,
        exit: SpatialExitTransition = SpatialTransitionDefaults.DefaultExit,
    ): SubspaceModifier = this.then(transition.createModifier(enter, exit))
}
