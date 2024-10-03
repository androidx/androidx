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

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A motion scheme provides all the [FiniteAnimationSpec]s for a [MaterialTheme].
 *
 * Motion schemes are designed to create a harmonious motion for components in the app.
 *
 * There are two built-in schemes, a [standard] and an [expressive], that can be used as-is or
 * customized.
 */
@Immutable
interface MotionScheme {
    /**
     * A default spatial motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that may change the shape or bounds
     * of the component. For color or alpha animations use the `effects` equivalent which ensures a
     * "non-spatial" motion.
     *
     * [T] is the generic data type that will be animated by the system, as long as the appropriate
     * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
     */
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T>

    /**
     * A fast spatial motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that may change the shape or bounds
     * of the component. For color or alpha animations use the `effects` equivalent which ensures a
     * "non-spatial" motion.
     *
     * [T] is the generic data type that will be animated by the system, as long as the appropriate
     * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
     */
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T>

    /**
     * A slow spatial motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that may change the shape or bounds
     * of the component. For color or alpha animations use the `effects` equivalent which ensures a
     * "non-spatial" motion.
     *
     * [T] is the generic data type that will be animated by the system, as long as the appropriate
     * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
     */
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T>

    /**
     * A default effects motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that do not change the shape or
     * bounds of the component. For example, color animation.
     *
     * [T] is the generic data type that will be animated by the system, as long as the appropriate
     * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
     */
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T>

    /**
     * A fast effects motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that do not change the shape or
     * bounds of the component. For example, color animation.
     *
     * [T] is the generic data type that will be animated by the system, as long as the appropriate
     * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
     */
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T>

    /**
     * A slow effects motion [FiniteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that do not change the shape or
     * bounds of the component. For example, color animation.
     *
     * [T] is the generic data type that will be animated by the system, as long as the appropriate
     * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
     */
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T>

    companion object {
        /**
         * Returns a standard Material motion scheme.
         *
         * The standard scheme is Material's basic motion scheme for utilitarian UI elements and
         * recurring interactions. It provides a linear motion feel.
         */
        @Suppress("UNCHECKED_CAST")
        fun standard(): MotionScheme =
            object : MotionScheme {
                private val defaultSpatialSpec =
                    spring<Any>(
                        dampingRatio = StandardSpatialDampingRatio,
                        stiffness = StandardDefaultStiffness
                    )

                private val fastSpatialSpec =
                    spring<Any>(
                        dampingRatio = StandardSpatialDampingRatio,
                        stiffness = StandardFastStiffness
                    )

                private val slowSpatialSpec =
                    spring<Any>(
                        dampingRatio = StandardSpatialDampingRatio,
                        stiffness = StandardSlowStiffness
                    )

                private val defaultEffectsSpec =
                    spring<Any>(
                        dampingRatio = EffectsDampingRatio,
                        stiffness = EffectsDefaultStiffness
                    )

                private val fastEffectsSpec =
                    spring<Any>(
                        dampingRatio = EffectsDampingRatio,
                        stiffness = EffectsFastStiffness
                    )

                private val slowEffectsSpec =
                    spring<Any>(
                        dampingRatio = EffectsDampingRatio,
                        stiffness = EffectsSlowStiffness
                    )

                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> {
                    return defaultSpatialSpec as FiniteAnimationSpec<T>
                }

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> {
                    return fastSpatialSpec as FiniteAnimationSpec<T>
                }

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> {
                    return slowSpatialSpec as FiniteAnimationSpec<T>
                }

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> {
                    return defaultEffectsSpec as FiniteAnimationSpec<T>
                }

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> {
                    return fastEffectsSpec as FiniteAnimationSpec<T>
                }

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> {
                    return slowEffectsSpec as FiniteAnimationSpec<T>
                }
            }

        /**
         * Returns an expressive Material motion scheme.
         *
         * The expressive scheme is Material's recommended motion scheme for prominent UI elements
         * and hero interactions. It provides a visually engaging motion feel.
         */
        @Suppress("UNCHECKED_CAST")
        fun expressive(): MotionScheme =
            object : MotionScheme {
                private val defaultSpatialSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveDefaultDamping,
                        stiffness = ExpressiveDefaultStiffness
                    )

                private val fastSpatialSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveFastDamping,
                        stiffness = ExpressiveFastStiffness
                    )

                private val slowSpatialSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveSlowDamping,
                        stiffness = ExpressiveSlowStiffness
                    )

                private val defaultEffectsSpec =
                    spring<Any>(
                        dampingRatio = EffectsDampingRatio,
                        stiffness = EffectsDefaultStiffness
                    )

                private val fastEffectsSpec =
                    spring<Any>(
                        dampingRatio = EffectsDampingRatio,
                        stiffness = EffectsFastStiffness
                    )

                private val slowEffectsSpec =
                    spring<Any>(
                        dampingRatio = EffectsDampingRatio,
                        stiffness = EffectsSlowStiffness
                    )

                override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> {
                    return defaultSpatialSpec as FiniteAnimationSpec<T>
                }

                override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> {
                    return fastSpatialSpec as FiniteAnimationSpec<T>
                }

                override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> {
                    return slowSpatialSpec as FiniteAnimationSpec<T>
                }

                override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> {
                    return defaultEffectsSpec as FiniteAnimationSpec<T>
                }

                override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> {
                    return fastEffectsSpec as FiniteAnimationSpec<T>
                }

                override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> {
                    return slowEffectsSpec as FiniteAnimationSpec<T>
                }
            }
    }
}

/**
 * CompositionLocal used to pass [MotionScheme] down the tree.
 *
 * Setting the value here is typically done as part of [MaterialTheme]. To retrieve the current
 * value of this CompositionLocal, use [MaterialTheme.motionScheme].
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
internal val LocalMotionScheme = staticCompositionLocalOf { MotionScheme.standard() }

// TODO - These values should come from Tokens.
private const val StandardSpatialDampingRatio = 0.9f
private const val EffectsDampingRatio = Spring.DampingRatioNoBouncy

internal const val EffectsDefaultStiffness = 500f
internal const val EffectsFastStiffness = 1400f
internal const val EffectsSlowStiffness = 260f

internal const val StandardDefaultStiffness = 500f
internal const val StandardFastStiffness = 1400f
internal const val StandardSlowStiffness = 260f

internal const val ExpressiveDefaultStiffness = 350f
internal const val ExpressiveFastStiffness = 800f
internal const val ExpressiveSlowStiffness = 200f
internal const val ExpressiveDefaultDamping = 0.75f
internal const val ExpressiveFastDamping = 0.7f
internal const val ExpressiveSlowDamping = 0.8f
