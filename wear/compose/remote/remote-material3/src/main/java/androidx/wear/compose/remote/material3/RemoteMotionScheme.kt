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
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.runtime.Immutable

/**
 * A motion scheme provides all the [RemoteAnimationSpec]s for a [RemoteMaterialTheme].
 *
 * Motion schemes are designed to create a harmonious motion for components in the app. This is the
 * Remote Compose counterpart of [androidx.wear.compose.material3.MotionScheme].
 *
 * There are two built-in schemes, a [standard] and an [expressive], that can be used as-is or
 * customized.
 *
 * The animation specs are written into the remote document when it is created, so the motion scheme
 * is chosen at creation time and doesn't change on the player.
 */
@Immutable
public interface RemoteMotionScheme {
    /**
     * A default spatial motion [RemoteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that can overshoot their targets,
     * such as shape or bounds animations. For color, alpha or other animations which have strict
     * limits use the `effects` equivalent.
     */
    public fun defaultSpatialSpec(): RemoteAnimationSpec

    /**
     * A fast spatial motion [RemoteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that can overshoot their targets,
     * such as shape or bounds animations. For color, alpha or other animations which have strict
     * limits use the `effects` equivalent.
     */
    public fun fastSpatialSpec(): RemoteAnimationSpec

    /**
     * A slow spatial motion [RemoteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that can overshoot their targets,
     * such as shape or bounds animations. For color, alpha or other animations which have strict
     * limits use the `effects` equivalent.
     */
    public fun slowSpatialSpec(): RemoteAnimationSpec

    /**
     * A default effects motion [RemoteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that have strict limits and which
     * shouldn't overshoot their targets, such as color or alpha animations. For shape, bounds or
     * other animations which don't have strict limits use the `spatial` equivalent.
     */
    public fun defaultEffectsSpec(): RemoteAnimationSpec

    /**
     * A fast effects motion [RemoteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that have strict limits and which
     * shouldn't overshoot their targets, such as color or alpha animations. For shape, bounds or
     * other animations which don't have strict limits use the `spatial` equivalent.
     */
    public fun fastEffectsSpec(): RemoteAnimationSpec

    /**
     * A slow effects motion [RemoteAnimationSpec].
     *
     * This motion spec is designed to be applied to animations that have strict limits and which
     * shouldn't overshoot their targets, such as color or alpha animations. For shape, bounds or
     * other animations which don't have strict limits use the `spatial` equivalent.
     */
    public fun slowEffectsSpec(): RemoteAnimationSpec

    public companion object {
        /**
         * Returns a standard Material motion scheme.
         *
         * The standard scheme is Material's basic motion scheme for utilitarian UI elements and
         * recurring interactions. It provides a linear motion feel. This is the default motion
         * scheme of [RemoteMaterialTheme], as in Wear Compose Material 3.
         */
        public fun standard(): RemoteMotionScheme = StandardRemoteMotionScheme

        /**
         * Returns an expressive Material motion scheme.
         *
         * The expressive scheme is Material's recommended motion scheme for prominent UI elements
         * and hero interactions. It provides a visually engaging motion feel.
         */
        public fun expressive(): RemoteMotionScheme = ExpressiveRemoteMotionScheme
    }
}

// The effects specs are the same in the standard and expressive schemes.
private val DefaultEffectsSpec: RemoteAnimationSpec =
    remoteSpring(
        stiffness = RemoteMotionTokens.EffectsDefaultStiffness,
        dampingRatio = RemoteMotionTokens.EffectsDampingRatio,
    )

private val FastEffectsSpec: RemoteAnimationSpec =
    remoteSpring(
        stiffness = RemoteMotionTokens.EffectsFastStiffness,
        dampingRatio = RemoteMotionTokens.EffectsDampingRatio,
    )

private val SlowEffectsSpec: RemoteAnimationSpec =
    remoteSpring(
        stiffness = RemoteMotionTokens.EffectsSlowStiffness,
        dampingRatio = RemoteMotionTokens.EffectsDampingRatio,
    )

private object StandardRemoteMotionScheme : RemoteMotionScheme {
    private val defaultSpatialSpec =
        remoteSpring(
            stiffness = RemoteMotionTokens.StandardDefaultStiffness,
            dampingRatio = RemoteMotionTokens.StandardSpatialDampingRatio,
        )

    private val fastSpatialSpec =
        remoteSpring(
            stiffness = RemoteMotionTokens.StandardFastStiffness,
            dampingRatio = RemoteMotionTokens.StandardSpatialDampingRatio,
        )

    private val slowSpatialSpec =
        remoteSpring(
            stiffness = RemoteMotionTokens.StandardSlowStiffness,
            dampingRatio = RemoteMotionTokens.StandardSpatialDampingRatio,
        )

    override fun defaultSpatialSpec(): RemoteAnimationSpec = defaultSpatialSpec

    override fun fastSpatialSpec(): RemoteAnimationSpec = fastSpatialSpec

    override fun slowSpatialSpec(): RemoteAnimationSpec = slowSpatialSpec

    override fun defaultEffectsSpec(): RemoteAnimationSpec = DefaultEffectsSpec

    override fun fastEffectsSpec(): RemoteAnimationSpec = FastEffectsSpec

    override fun slowEffectsSpec(): RemoteAnimationSpec = SlowEffectsSpec

    override fun toString(): String = "RemoteMotionScheme.standard()"
}

private object ExpressiveRemoteMotionScheme : RemoteMotionScheme {
    private val defaultSpatialSpec =
        remoteSpring(
            stiffness = RemoteMotionTokens.ExpressiveDefaultStiffness,
            dampingRatio = RemoteMotionTokens.ExpressiveDefaultDamping,
        )

    private val fastSpatialSpec =
        remoteSpring(
            stiffness = RemoteMotionTokens.ExpressiveFastStiffness,
            dampingRatio = RemoteMotionTokens.ExpressiveFastDamping,
        )

    private val slowSpatialSpec =
        remoteSpring(
            stiffness = RemoteMotionTokens.ExpressiveSlowStiffness,
            dampingRatio = RemoteMotionTokens.ExpressiveSlowDamping,
        )

    override fun defaultSpatialSpec(): RemoteAnimationSpec = defaultSpatialSpec

    override fun fastSpatialSpec(): RemoteAnimationSpec = fastSpatialSpec

    override fun slowSpatialSpec(): RemoteAnimationSpec = slowSpatialSpec

    override fun defaultEffectsSpec(): RemoteAnimationSpec = DefaultEffectsSpec

    override fun fastEffectsSpec(): RemoteAnimationSpec = FastEffectsSpec

    override fun slowEffectsSpec(): RemoteAnimationSpec = SlowEffectsSpec

    override fun toString(): String = "RemoteMotionScheme.expressive()"
}
