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
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteMotionTokensTest {

    @Test
    fun duration_tokens_match_wear_motion_tokens() {
        assertThat(RemoteMotionTokens.DurationShort1).isEqualTo(50)
        assertThat(RemoteMotionTokens.DurationShort2).isEqualTo(100)
        assertThat(RemoteMotionTokens.DurationShort3).isEqualTo(150)
        assertThat(RemoteMotionTokens.DurationShort4).isEqualTo(200)
        assertThat(RemoteMotionTokens.DurationMedium1).isEqualTo(250)
        assertThat(RemoteMotionTokens.DurationMedium2).isEqualTo(300)
        assertThat(RemoteMotionTokens.DurationMedium3).isEqualTo(350)
        assertThat(RemoteMotionTokens.DurationMedium4).isEqualTo(400)
        assertThat(RemoteMotionTokens.DurationLong1).isEqualTo(450)
        assertThat(RemoteMotionTokens.DurationLong2).isEqualTo(500)
        assertThat(RemoteMotionTokens.DurationLong3).isEqualTo(550)
        assertThat(RemoteMotionTokens.DurationLong4).isEqualTo(600)
        assertThat(RemoteMotionTokens.DurationExtraLong1).isEqualTo(700)
        assertThat(RemoteMotionTokens.DurationExtraLong2).isEqualTo(800)
        assertThat(RemoteMotionTokens.DurationExtraLong3).isEqualTo(900)
        assertThat(RemoteMotionTokens.DurationExtraLong4).isEqualTo(1000)
    }

    @Test
    fun easing_tokens_match_wear_motion_tokens() {
        assertThat(RemoteMotionTokens.EasingEmphasizedAccelerate)
            .isEqualTo(RemoteEasing.cubic(0.3f, 0.0f, 0.8f, 0.15f))
        assertThat(RemoteMotionTokens.EasingEmphasizedDecelerate)
            .isEqualTo(RemoteEasing.cubic(0.05f, 0.7f, 0.1f, 1.0f))
        assertThat(RemoteMotionTokens.EasingLegacyAccelerate)
            .isEqualTo(RemoteEasing.cubic(0.4f, 0.0f, 1.0f, 1.0f))
        assertThat(RemoteMotionTokens.EasingLegacyDecelerate)
            .isEqualTo(RemoteEasing.cubic(0.0f, 0.0f, 0.2f, 1.0f))
        assertThat(RemoteMotionTokens.EasingLegacyStandard)
            .isEqualTo(RemoteEasing.cubic(0.4f, 0.0f, 0.2f, 1.0f))
        assertThat(RemoteMotionTokens.EasingStandard)
            .isEqualTo(RemoteEasing.cubic(0.2f, 0.0f, 0.0f, 1.0f))
        assertThat(RemoteMotionTokens.EasingStandardAccelerate)
            .isEqualTo(RemoteEasing.cubic(0.3f, 0.0f, 1.0f, 1.0f))
        assertThat(RemoteMotionTokens.EasingStandardDecelerate)
            .isEqualTo(RemoteEasing.cubic(0.0f, 0.0f, 0.0f, 1.0f))
    }
}
