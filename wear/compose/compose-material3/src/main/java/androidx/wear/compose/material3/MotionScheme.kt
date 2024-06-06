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
package androidx.wear.compose.material3

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable

/**
 * Internal placeholder implementation of the MotionScheme to unblock adding animations to our
 * components while a proper implementation is discussed.
 */
@Immutable
internal object MotionScheme {
    /**
     * A default motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that may change the shape or bounds
     * of the component. For color or alpha animations use the flat equivalent which ensures a
     * "non-bouncy" motion.
     */
    fun <T> bouncyDefaultSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = ExpressiveDefaultDamping, stiffness = ExpressiveDefaultStiffness)

    /**
     * A fast motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that may change the shape or bounds
     * of the component. For color or alpha animations use the flat equivalent which ensures a
     * "non-bouncy" motion.
     */
    fun <T> bouncyFastSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = ExpressiveFastDamping, stiffness = ExpressiveFastStiffness)

    /**
     * A slow motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that may change the shape or bounds
     * of the component. For color or alpha animations use the flat equivalent which ensures a
     * "non-bouncy" motion.
     */
    fun <T> bouncySlowSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = ExpressiveSlowDamping, stiffness = ExpressiveSlowStiffness)

    // Common non-spatial defaults for all motion schemes.
    /**
     * A default flat motion [FiniteAnimationSpec]. Flat motion is designed to be applied to
     * animations that do not change the shape or bounds of the component. For example, color
     * animation.
     */
    fun <T> flatDefaultSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = NonSpatialDefaultStiffness)

    /**
     * A fast flat motion [FiniteAnimationSpec]. Flat motion is designed to be applied to animations
     * that do not change the shape or bounds of the component. For example, color animation.
     */
    fun <T> flatFastSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = NonSpatialFastStiffness)

    /**
     * A slow flat motion [FiniteAnimationSpec]. Flat motion is designed to be applied to animations
     * that do not change the shape or bounds of the component. For example, color animation.
     */
    fun <T> flatSlowSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = NonSpatialSlowStiffness)
}

internal const val NonSpatialDefaultStiffness = 500f
internal const val NonSpatialFastStiffness = 1400f
internal const val NonSpatialSlowStiffness = 260f
// TODO - These values should come from Carbon.
internal const val ExpressiveDefaultStiffness = 350f
internal const val ExpressiveFastStiffness = 800f
internal const val ExpressiveSlowStiffness = 200f
internal const val ExpressiveDefaultDamping = 0.75f
internal const val ExpressiveFastDamping = 0.7f
internal const val ExpressiveSlowDamping = 0.8f
