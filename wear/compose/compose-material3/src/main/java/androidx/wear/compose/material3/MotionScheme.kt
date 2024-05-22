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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A motion scheme provides all the [FiniteAnimationSpec]s for a [MaterialTheme].
 *
 * Motion schemes are designed to create a harmonious motion for components in the app.
 *
 * There are two built-in schemes, a [standardMotionScheme] and an [expressiveMotionScheme], that
 * can be used as-is or customized.
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
     *
     * When called from a Composable, use [rememberDefaultSpatialSpec] extension to ensure that the
     * returned animation spec is remembered across compositions.
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
     *
     * When called from a Composable, use [rememberFastSpatialSpec] extension to ensure that the
     * returned animation spec is remembered across compositions.
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
     *
     * When called from a Composable, use [rememberSlowSpatialSpec] extension to ensure that the
     * returned animation spec is remembered across compositions.
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
     *
     * When called from a Composable, use [rememberDefaultEffectsSpec] extension to ensure that the
     * returned animation spec is remembered across compositions.
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
     *
     * When called from a Composable, use [rememberFastEffectsSpec] extension to ensure that the
     * returned animation spec is remembered across compositions.
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
     *
     * When called from a Composable, use [rememberSlowEffectsSpec] extension to ensure that the
     * returned animation spec is remembered across compositions.
     */
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T>
}

/**
 * A default spatial motion [FiniteAnimationSpec] that is remembered across compositions.
 *
 * [T] is the generic data type that will be animated by the system, as long as the appropriate
 * [TwoWayConverter] for converting the data to and from an [AnimationVector] is supplied.
 *
 * @see [MotionScheme.defaultSpatialSpec]
 */
@Composable
inline fun <reified T> MotionScheme.rememberDefaultSpatialSpec() =
    remember(this, T::class) {
        val spec: FiniteAnimationSpec<T> = defaultSpatialSpec()
        spec
    }

/**
 * A fast spatial motion [FiniteAnimationSpec] that is remembered across compositions.
 *
 * [T] is the generic data type that will be animated by the system.
 *
 * @see [MotionScheme.fastSpatialSpec]
 */
@Composable
inline fun <reified T> MotionScheme.rememberFastSpatialSpec() =
    remember(this, T::class) {
        val spec: FiniteAnimationSpec<T> = fastSpatialSpec()
        spec
    }

/**
 * A slow spatial motion [FiniteAnimationSpec] that is remembered across compositions.
 *
 * [T] is the generic data type that will be animated by the system.
 *
 * @see [MotionScheme.slowSpatialSpec]
 */
@Composable
inline fun <reified T> MotionScheme.rememberSlowSpatialSpec() =
    remember(this, T::class) {
        val spec: FiniteAnimationSpec<T> = slowSpatialSpec()
        spec
    }

/**
 * A default effects motion [FiniteAnimationSpec] that is remembered across compositions.
 *
 * [T] is the generic data type that will be animated by the system.
 *
 * @see [MotionScheme.defaultEffectsSpec]
 */
@Composable
inline fun <reified T> MotionScheme.rememberDefaultEffectsSpec() =
    remember(this, T::class) {
        val spec: FiniteAnimationSpec<T> = defaultEffectsSpec()
        spec
    }

/**
 * A fast effects motion [FiniteAnimationSpec] that is remembered across compositions.
 *
 * [T] is the generic data type that will be animated by the system.
 *
 * @see [MotionScheme.fastEffectsSpec]
 */
@Composable
inline fun <reified T> MotionScheme.rememberFastEffectsSpec() =
    remember(this, T::class) {
        val spec: FiniteAnimationSpec<T> = fastEffectsSpec()
        spec
    }

/**
 * A slow effects motion [FiniteAnimationSpec] that is remembered across compositions.
 *
 * [T] is the generic data type that will be animated by the system.
 *
 * @see [MotionScheme.slowEffectsSpec]
 */
@Composable
inline fun <reified T> MotionScheme.rememberSlowEffectsSpec() =
    remember(this, T::class) {
        val spec: FiniteAnimationSpec<T> = slowEffectsSpec()
        spec
    }

/** Returns a standard Material motion scheme. */
fun standardMotionScheme(): MotionScheme =
    object : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> {
            return spring(
                dampingRatio = StandardSpatialDampingRatio,
                stiffness = StandardDefaultStiffness
            )
        }

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> {
            return spring(
                dampingRatio = StandardSpatialDampingRatio,
                stiffness = StandardFastStiffness
            )
        }

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> {
            return spring(
                dampingRatio = StandardSpatialDampingRatio,
                stiffness = StandardSlowStiffness
            )
        }

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = EffectsDampingRatio, stiffness = EffectsDefaultStiffness)
        }

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = EffectsDampingRatio, stiffness = EffectsFastStiffness)
        }

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = EffectsDampingRatio, stiffness = EffectsSlowStiffness)
        }
    }

/** Returns an expressive Material motion scheme. */
fun expressiveMotionScheme(): MotionScheme =
    object : MotionScheme {
        override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> {
            return spring(
                dampingRatio = ExpressiveDefaultDamping,
                stiffness = ExpressiveDefaultStiffness
            )
        }

        override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = ExpressiveFastDamping, stiffness = ExpressiveFastStiffness)
        }

        override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = ExpressiveSlowDamping, stiffness = ExpressiveSlowStiffness)
        }

        override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = EffectsDampingRatio, stiffness = EffectsDefaultStiffness)
        }

        override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = EffectsDampingRatio, stiffness = EffectsFastStiffness)
        }

        override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> {
            return spring(dampingRatio = EffectsDampingRatio, stiffness = EffectsSlowStiffness)
        }
    }

/**
 * CompositionLocal used to pass [MotionScheme] down the tree.
 *
 * Setting the value here is typically done as part of [MaterialTheme]. To retrieve the current
 * value of this CompositionLocal, use [MaterialTheme.motionScheme].
 */
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
internal val LocalMotionScheme = staticCompositionLocalOf { standardMotionScheme() }

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
