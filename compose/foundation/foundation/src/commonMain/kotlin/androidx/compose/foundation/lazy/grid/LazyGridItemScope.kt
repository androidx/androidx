/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lazy.grid

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset

/** Receiver scope being used by the item content parameter of [LazyVerticalGrid]. */
@Stable
@LazyGridScopeMarker
sealed interface LazyGridItemScope {
    /**
     * This modifier animates the item appearance (fade in), disappearance (fade out) and placement
     * changes (such as an item reordering).
     *
     * You should also provide a key via [LazyGridScope.item]/[LazyGridScope.items] for this
     * modifier to enable animations.
     *
     * @sample androidx.compose.foundation.samples.GridAnimateItemSample
     * @param fadeInSpec an animation specs to use for animating the item appearance. When null is
     *   provided the item will be appearing without animations.
     * @param placementSpec an animation specs that will be used to animate the item placement.
     *   Aside from item reordering all other position changes caused by events like arrangement or
     *   alignment changes will also be animated. When null is provided no animations will happen.
     * @param fadeOutSpec an animation specs to use for animating the item disappearance. When null
     *   is provided the item will be disappearance without animations.
     */
    fun Modifier.animateItem(
        fadeInSpec: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
        placementSpec: FiniteAnimationSpec<IntOffset>? =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = IntOffset.VisibilityThreshold
            ),
        fadeOutSpec: FiniteAnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    ): Modifier
}
