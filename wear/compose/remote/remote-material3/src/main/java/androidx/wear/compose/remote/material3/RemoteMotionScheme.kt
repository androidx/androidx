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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.remoteSpring
import androidx.compose.runtime.Immutable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public interface RemoteMotionScheme {
    public fun defaultSpatialSpec(): RemoteAnimationSpec

    public fun fastSpatialSpec(): RemoteAnimationSpec

    public fun slowSpatialSpec(): RemoteAnimationSpec

    public fun defaultEffectsSpec(): RemoteAnimationSpec

    public fun fastEffectsSpec(): RemoteAnimationSpec

    public fun slowEffectsSpec(): RemoteAnimationSpec

    public companion object {
        public fun standard(): RemoteMotionScheme = StandardRemoteMotionScheme

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
