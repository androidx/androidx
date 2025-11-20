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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.xr.compose.unit.IntVolumeOffset
import androidx.xr.compose.unit.IntVolumeSize

/**
 * [SpatialEnterTransition] defines how an [AnimatedSpatialVisibility] Composable appears on screen
 * as it becomes visible. The categories of EnterTransitions available are:
 * 1. fade: [SpatialTransitions.fadeIn]
 * 2. slide: [SpatialTransitions.slideIn], [SpatialTransitions.slideInHorizontally],
 *    [SpatialTransitions.slideInVertically], [SpatialTransitions.slideInDepth]
 *
 * [SpatialEnterTransition.None] can be used when no enter transition is desired. Different
 * [SpatialEnterTransition]s can be combined using [SpatialEnterTransition.plus].
 *
 * __Note__: [SpatialTransitions.fadeIn] and [SpatialTransitions.slideIn] do not affect the size of
 * the [AnimatedVisibility] composable.
 *
 * @see EnterTransition
 */
public sealed class SpatialEnterTransition {
    internal abstract val data: SpatialTransitionData

    /**
     * Combines different enter transitions. The order of the [SpatialEnterTransition]s being
     * combined does not matter, as these [SpatialEnterTransition]s will start simultaneously. The
     * order of applying transforms from these enter transitions (if defined) is: alpha and scale
     * first, shrink or expand, then slide.
     *
     * @param enter another [SpatialEnterTransition] to be combined
     */
    @Stable
    public operator fun plus(enter: SpatialEnterTransition): SpatialEnterTransition {
        return Impl(data.merge(enter.data))
    }

    @Immutable
    internal class Impl(override val data: SpatialTransitionData) : SpatialEnterTransition()

    public companion object {
        /**
         * This can be used when no enter transition is desired.
         *
         * @see [SpatialExitTransition.None]
         */
        public val None: SpatialEnterTransition = Impl(SpatialTransitionData())
    }
}

/**
 * [SpatialExitTransition] defines how an [AnimatedSpatialVisibility] Composable disappears on
 * screen as it becomes not visible. The categories of [SpatialExitTransition] available are:
 * 1. fade: [SpatialTransitions.fadeOut]
 * 2. slide: [SpatialTransitions.slideOut], [SpatialTransitions.slideOutHorizontally],
 *    [SpatialTransitions.slideOutVertically], [SpatialTransitions.slideOutDepth]
 *
 * [SpatialExitTransition.None] can be used when no exit transition is desired. Different
 * [SpatialExitTransition]s can be combined using [SpatialExitTransition.plus].
 *
 * __Note__: [SpatialTransitions.fadeOut] and [SpatialTransitions.slideOut] do not affect the size
 * of the [AnimatedSpatialVisibility] composable.
 *
 * @see ExitTransition
 */
public sealed class SpatialExitTransition {
    internal abstract val data: SpatialTransitionData

    /**
     * Combines different exit transitions. The order of the [SpatialExitTransition]s being combined
     * does not matter, as these [SpatialExitTransition]s will start simultaneously. The order of
     * applying transforms from these exit transitions (if defined) is: alpha and scale first,
     * shrink or expand, then slide.
     *
     * @param exit another [SpatialExitTransition] to be combined
     */
    @Stable
    public operator fun plus(exit: SpatialExitTransition): SpatialExitTransition {
        return Impl(data.merge(exit.data))
    }

    @Immutable
    internal class Impl(override val data: SpatialTransitionData) : SpatialExitTransition()

    public companion object {
        /**
         * This can be used when no enter transition is desired.
         *
         * @see [SpatialEnterTransition.None]
         */
        public val None: SpatialExitTransition = Impl(SpatialTransitionData())
    }
}

/** ********************* Below are internal classes and methods ***************** */
@Immutable
internal data class Fade(val alpha: Float, val animationSpec: FiniteAnimationSpec<Float>)

@Immutable
internal data class Slide(
    val slideOffset: Density.(fullSize: IntVolumeSize) -> IntVolumeOffset,
    val animationSpec: FiniteAnimationSpec<IntVolumeOffset>,
)

@Immutable
internal data class SpatialTransitionData(val fade: Fade? = null, val slide: Slide? = null)

private fun SpatialTransitionData.merge(other: SpatialTransitionData): SpatialTransitionData {
    return SpatialTransitionData(fade = other.fade ?: fade, slide = other.slide ?: slide)
}
