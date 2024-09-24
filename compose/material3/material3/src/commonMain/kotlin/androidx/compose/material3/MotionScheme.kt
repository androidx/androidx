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

package androidx.compose.material3

import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.spring
import androidx.compose.material3.MotionScheme.Companion.expressiveMotionScheme
import androidx.compose.material3.MotionScheme.Companion.standardMotionScheme
import androidx.compose.material3.tokens.ExpressiveMotionTokens
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.tokens.StandardMotionTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A motion scheme provides all the [FiniteAnimationSpec]s for a [MaterialTheme].
 *
 * Motion schemes are designed to create a harmonious motion for components in the app.
 *
 * There are two built-in schemes, a [standardMotionScheme] and an [expressiveMotionScheme], that
 * can be used as-is or customized.
 *
 * You can customize the motion scheme for all components in the [MaterialTheme].
 */
@ExperimentalMaterial3ExpressiveApi
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

        /** Returns a standard Material motion scheme. */
        @Suppress("UNCHECKED_CAST")
        @ExperimentalMaterial3ExpressiveApi
        fun standardMotionScheme(): MotionScheme =
            object : MotionScheme {
                private val defaultSpatialSpec =
                    spring<Any>(
                        dampingRatio = StandardMotionTokens.SpringDefaultSpatialDamping,
                        stiffness = StandardMotionTokens.SpringDefaultSpatialStiffness
                    )

                private val fastSpatialSpec =
                    spring<Any>(
                        dampingRatio = StandardMotionTokens.SpringFastSpatialDamping,
                        stiffness = StandardMotionTokens.SpringFastSpatialStiffness
                    )

                private val slowSpatialSpec =
                    spring<Any>(
                        dampingRatio = StandardMotionTokens.SpringSlowSpatialDamping,
                        stiffness = StandardMotionTokens.SpringSlowSpatialStiffness
                    )

                private val defaultEffectsSpec =
                    spring<Any>(
                        dampingRatio = StandardMotionTokens.SpringDefaultEffectsDamping,
                        stiffness = StandardMotionTokens.SpringDefaultEffectsStiffness
                    )

                private val fastEffectsSpec =
                    spring<Any>(
                        dampingRatio = StandardMotionTokens.SpringFastEffectsDamping,
                        stiffness = StandardMotionTokens.SpringFastEffectsStiffness
                    )

                private val slowEffectsSpec =
                    spring<Any>(
                        dampingRatio = StandardMotionTokens.SpringSlowEffectsDamping,
                        stiffness = StandardMotionTokens.SpringSlowEffectsStiffness
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

        /** Returns an expressive Material motion scheme. */
        @Suppress("UNCHECKED_CAST")
        @ExperimentalMaterial3ExpressiveApi
        fun expressiveMotionScheme(): MotionScheme =
            object : MotionScheme {

                private val defaultSpatialSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveMotionTokens.SpringDefaultSpatialDamping,
                        stiffness = ExpressiveMotionTokens.SpringDefaultSpatialStiffness
                    )

                private val fastSpatialSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveMotionTokens.SpringFastSpatialDamping,
                        stiffness = ExpressiveMotionTokens.SpringFastSpatialStiffness
                    )

                private val slowSpatialSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveMotionTokens.SpringSlowSpatialDamping,
                        stiffness = ExpressiveMotionTokens.SpringSlowSpatialStiffness
                    )

                private val defaultEffectsSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveMotionTokens.SpringDefaultEffectsDamping,
                        stiffness = ExpressiveMotionTokens.SpringDefaultEffectsStiffness
                    )

                private val fastEffectsSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveMotionTokens.SpringFastEffectsDamping,
                        stiffness = ExpressiveMotionTokens.SpringFastEffectsStiffness
                    )

                private val slowEffectsSpec =
                    spring<Any>(
                        dampingRatio = ExpressiveMotionTokens.SpringSlowEffectsDamping,
                        stiffness = ExpressiveMotionTokens.SpringSlowEffectsStiffness
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
@get:ExperimentalMaterial3ExpressiveApi
@ExperimentalMaterial3ExpressiveApi
internal val LocalMotionScheme = staticCompositionLocalOf { MotionScheme.standardMotionScheme() }

/**
 * Helper function for component motion tokens.
 *
 * Here is an example on how to use component motion tokens:
 * ``MaterialTheme.motionScheme.fromToken(ExtendedFabBranded.ExpandMotion)``
 *
 * The returned [FiniteAnimationSpec] is remembered across compositions.
 *
 * @param value the token's value
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Stable
internal fun <T> MotionScheme.fromToken(value: MotionSchemeKeyTokens): FiniteAnimationSpec<T> {
    return when (value) {
        MotionSchemeKeyTokens.DefaultSpatial -> defaultSpatialSpec()
        MotionSchemeKeyTokens.FastSpatial -> fastSpatialSpec()
        MotionSchemeKeyTokens.SlowSpatial -> slowSpatialSpec()
        MotionSchemeKeyTokens.DefaultEffects -> defaultEffectsSpec()
        MotionSchemeKeyTokens.FastEffects -> fastEffectsSpec()
        MotionSchemeKeyTokens.SlowEffects -> slowEffectsSpec()
    }
}

/**
 * Converts a [MotionSchemeKeyTokens] key to the [FiniteAnimationSpec] provided by the
 * [MotionScheme].
 */
@Composable
@ReadOnlyComposable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun <T> MotionSchemeKeyTokens.value(): FiniteAnimationSpec<T> =
    MaterialTheme.motionScheme.fromToken(this)
