/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.wear.compose.material3.MotionScheme

/**
 * Shared motion tokens and [RemoteAnimationSpec]s for Wear Remote Material 3 components, matching
 * Wear Compose Material 3 `MotionTokens` and [MotionScheme].
 */
internal object RemoteMotionTokens {
    // Durations (in milliseconds), matching androidx.wear.compose.material3.tokens.MotionTokens
    const val DurationExtraLong1: Int = 700
    const val DurationExtraLong2: Int = 800
    const val DurationExtraLong3: Int = 900
    const val DurationExtraLong4: Int = 1000
    const val DurationLong1: Int = 450
    const val DurationLong2: Int = 500
    const val DurationLong3: Int = 550
    const val DurationLong4: Int = 600
    const val DurationMedium1: Int = 250
    const val DurationMedium2: Int = 300
    const val DurationMedium3: Int = 350
    const val DurationMedium4: Int = 400
    const val DurationShort1: Int = 50
    const val DurationShort2: Int = 100
    const val DurationShort3: Int = 150
    const val DurationShort4: Int = 200

    // Cubic Bezier easings, matching androidx.wear.compose.material3.tokens.MotionTokens
    val EasingEmphasizedAccelerate: RemoteEasing = RemoteEasing.Cubic(0.3f, 0.0f, 0.8f, 0.15f)
    val EasingEmphasizedDecelerate: RemoteEasing = RemoteEasing.Cubic(0.05f, 0.7f, 0.1f, 1.0f)
    val EasingLegacyAccelerate: RemoteEasing = RemoteEasing.Cubic(0.4f, 0.0f, 1.0f, 1.0f)
    val EasingLegacyDecelerate: RemoteEasing = RemoteEasing.Cubic(0.0f, 0.0f, 0.2f, 1.0f)
    val EasingLegacyStandard: RemoteEasing = RemoteEasing.Cubic(0.4f, 0.0f, 0.2f, 1.0f)
    val EasingStandard: RemoteEasing = RemoteEasing.Cubic(0.2f, 0.0f, 0.0f, 1.0f)
    val EasingStandardAccelerate: RemoteEasing = RemoteEasing.Cubic(0.3f, 0.0f, 1.0f, 1.0f)
    val EasingStandardDecelerate: RemoteEasing = RemoteEasing.Cubic(0.0f, 0.0f, 0.0f, 1.0f)

    // Spring damping ratios and stiffnesses, matching androidx.wear.compose.material3.MotionScheme
    const val StandardSpatialDampingRatio: Float = 1.0f
    const val EffectsDampingRatio: Float = 1.0f

    const val EffectsDefaultStiffness: Float = 500f
    const val EffectsFastStiffness: Float = 1400f
    const val EffectsSlowStiffness: Float = 260f

    const val StandardDefaultStiffness: Float = 500f
    const val StandardFastStiffness: Float = 1400f
    const val StandardSlowStiffness: Float = 260f

    const val ExpressiveDefaultStiffness: Float = 350f
    const val ExpressiveFastStiffness: Float = 800f
    const val ExpressiveSlowStiffness: Float = 200f
    const val ExpressiveDefaultDamping: Float = 0.75f
    const val ExpressiveFastDamping: Float = 0.7f
    const val ExpressiveSlowDamping: Float = 0.8f

    // Effects spring specs (shared across Standard and Expressive schemes)
    val DefaultEffectsSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = EffectsDefaultStiffness, dampingRatio = EffectsDampingRatio)
    val FastEffectsSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = EffectsFastStiffness, dampingRatio = EffectsDampingRatio)
    val SlowEffectsSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = EffectsSlowStiffness, dampingRatio = EffectsDampingRatio)

    // Standard spatial spring specs, matching MotionScheme.standard()
    val StandardDefaultSpatialSpec: RemoteAnimationSpec =
        remoteSpring(
            stiffness = StandardDefaultStiffness,
            dampingRatio = StandardSpatialDampingRatio,
        )
    val StandardFastSpatialSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = StandardFastStiffness, dampingRatio = StandardSpatialDampingRatio)
    val StandardSlowSpatialSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = StandardSlowStiffness, dampingRatio = StandardSpatialDampingRatio)

    // Expressive spatial spring specs, matching MotionScheme.expressive()
    val ExpressiveDefaultSpatialSpec: RemoteAnimationSpec =
        remoteSpring(
            stiffness = ExpressiveDefaultStiffness,
            dampingRatio = ExpressiveDefaultDamping,
        )
    val ExpressiveFastSpatialSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = ExpressiveFastStiffness, dampingRatio = ExpressiveFastDamping)
    val ExpressiveSlowSpatialSpec: RemoteAnimationSpec =
        remoteSpring(stiffness = ExpressiveSlowStiffness, dampingRatio = ExpressiveSlowDamping)

    /** Returns the default effects [RemoteAnimationSpec], matching `defaultEffectsSpec()`. */
    fun defaultEffectsSpec(): RemoteAnimationSpec = DefaultEffectsSpec

    /** Returns the fast effects [RemoteAnimationSpec], matching `fastEffectsSpec()`. */
    fun fastEffectsSpec(): RemoteAnimationSpec = FastEffectsSpec

    /** Returns the slow effects [RemoteAnimationSpec], matching `slowEffectsSpec()`. */
    fun slowEffectsSpec(): RemoteAnimationSpec = SlowEffectsSpec

    /**
     * Returns the default spatial [RemoteAnimationSpec], matching
     * `MotionScheme.standard().defaultSpatialSpec()`.
     */
    fun defaultSpatialSpec(): RemoteAnimationSpec = StandardDefaultSpatialSpec

    /**
     * Returns the fast spatial [RemoteAnimationSpec], matching
     * `MotionScheme.standard().fastSpatialSpec()`.
     */
    fun fastSpatialSpec(): RemoteAnimationSpec = StandardFastSpatialSpec

    /**
     * Returns the slow spatial [RemoteAnimationSpec], matching
     * `MotionScheme.standard().slowSpatialSpec()`.
     */
    fun slowSpatialSpec(): RemoteAnimationSpec = StandardSlowSpatialSpec

    /**
     * Returns the expressive default spatial [RemoteAnimationSpec], matching
     * `MotionScheme.expressive().defaultSpatialSpec()`.
     */
    fun expressiveDefaultSpatialSpec(): RemoteAnimationSpec = ExpressiveDefaultSpatialSpec

    /**
     * Returns the expressive fast spatial [RemoteAnimationSpec], matching
     * `MotionScheme.expressive().fastSpatialSpec()`.
     */
    fun expressiveFastSpatialSpec(): RemoteAnimationSpec = ExpressiveFastSpatialSpec

    /**
     * Returns the expressive slow spatial [RemoteAnimationSpec], matching
     * `MotionScheme.expressive().slowSpatialSpec()`.
     */
    fun expressiveSlowSpatialSpec(): RemoteAnimationSpec = ExpressiveSlowSpatialSpec
}
