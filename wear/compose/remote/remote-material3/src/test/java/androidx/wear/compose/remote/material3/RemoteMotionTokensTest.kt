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

import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.remoteSpring
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteMotionTokensTest {

    @Test
    fun duration_tokens_match_wear_motion_tokens() {
        assertEquals(50, RemoteMotionTokens.DurationShort1)
        assertEquals(100, RemoteMotionTokens.DurationShort2)
        assertEquals(150, RemoteMotionTokens.DurationShort3)
        assertEquals(200, RemoteMotionTokens.DurationShort4)
        assertEquals(250, RemoteMotionTokens.DurationMedium1)
        assertEquals(300, RemoteMotionTokens.DurationMedium2)
        assertEquals(350, RemoteMotionTokens.DurationMedium3)
        assertEquals(400, RemoteMotionTokens.DurationMedium4)
        assertEquals(450, RemoteMotionTokens.DurationLong1)
        assertEquals(500, RemoteMotionTokens.DurationLong2)
        assertEquals(550, RemoteMotionTokens.DurationLong3)
        assertEquals(600, RemoteMotionTokens.DurationLong4)
        assertEquals(700, RemoteMotionTokens.DurationExtraLong1)
        assertEquals(800, RemoteMotionTokens.DurationExtraLong2)
        assertEquals(900, RemoteMotionTokens.DurationExtraLong3)
        assertEquals(1000, RemoteMotionTokens.DurationExtraLong4)
    }

    @Test
    fun easing_tokens_match_wear_motion_tokens() {
        assertEquals(
            RemoteEasing.Cubic(0.3f, 0.0f, 0.8f, 0.15f),
            RemoteMotionTokens.EasingEmphasizedAccelerate,
        )
        assertEquals(
            RemoteEasing.Cubic(0.05f, 0.7f, 0.1f, 1.0f),
            RemoteMotionTokens.EasingEmphasizedDecelerate,
        )
        assertEquals(
            RemoteEasing.Cubic(0.4f, 0.0f, 1.0f, 1.0f),
            RemoteMotionTokens.EasingLegacyAccelerate,
        )
        assertEquals(
            RemoteEasing.Cubic(0.0f, 0.0f, 0.2f, 1.0f),
            RemoteMotionTokens.EasingLegacyDecelerate,
        )
        assertEquals(
            RemoteEasing.Cubic(0.4f, 0.0f, 0.2f, 1.0f),
            RemoteMotionTokens.EasingLegacyStandard,
        )
        assertEquals(RemoteEasing.Cubic(0.2f, 0.0f, 0.0f, 1.0f), RemoteMotionTokens.EasingStandard)
        assertEquals(
            RemoteEasing.Cubic(0.3f, 0.0f, 1.0f, 1.0f),
            RemoteMotionTokens.EasingStandardAccelerate,
        )
        assertEquals(
            RemoteEasing.Cubic(0.0f, 0.0f, 0.0f, 1.0f),
            RemoteMotionTokens.EasingStandardDecelerate,
        )
    }

    @Test
    fun effects_spring_specs_match_wear_motion_scheme() {
        assertEquals(
            remoteSpring(stiffness = 500f, dampingRatio = 1.0f),
            RemoteMotionTokens.defaultEffectsSpec(),
        )
        assertEquals(
            remoteSpring(stiffness = 1400f, dampingRatio = 1.0f),
            RemoteMotionTokens.fastEffectsSpec(),
        )
        assertEquals(
            remoteSpring(stiffness = 260f, dampingRatio = 1.0f),
            RemoteMotionTokens.slowEffectsSpec(),
        )
    }

    @Test
    fun standard_spatial_spring_specs_match_wear_motion_scheme() {
        assertEquals(
            remoteSpring(stiffness = 500f, dampingRatio = 1.0f),
            RemoteMotionTokens.defaultSpatialSpec(),
        )
        assertEquals(
            remoteSpring(stiffness = 1400f, dampingRatio = 1.0f),
            RemoteMotionTokens.fastSpatialSpec(),
        )
        assertEquals(
            remoteSpring(stiffness = 260f, dampingRatio = 1.0f),
            RemoteMotionTokens.slowSpatialSpec(),
        )
    }

    @Test
    fun expressive_spatial_spring_specs_match_wear_motion_scheme() {
        assertEquals(
            remoteSpring(stiffness = 350f, dampingRatio = 0.75f),
            RemoteMotionTokens.expressiveDefaultSpatialSpec(),
        )
        assertEquals(
            remoteSpring(stiffness = 800f, dampingRatio = 0.7f),
            RemoteMotionTokens.expressiveFastSpatialSpec(),
        )
        assertEquals(
            remoteSpring(stiffness = 200f, dampingRatio = 0.8f),
            RemoteMotionTokens.expressiveSlowSpatialSpec(),
        )
    }
}
