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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class MotionSchemeTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun tokenValue() {
        lateinit var motionScheme: MotionScheme
        lateinit var defaultSpatialSpec: FiniteAnimationSpec<Float>
        lateinit var fastSpatialSpec: FiniteAnimationSpec<Float>
        lateinit var slowSpatialSpec: FiniteAnimationSpec<Float>
        lateinit var defaultEffectsSpec: FiniteAnimationSpec<Float>
        lateinit var fastEffectsSpec: FiniteAnimationSpec<Float>
        lateinit var slowEffectsSpec: FiniteAnimationSpec<Float>
        rule.setMaterialContent(lightColorScheme()) {
            motionScheme = LocalMotionScheme.current
            defaultSpatialSpec = MotionSchemeKeyTokens.DefaultSpatial.value()
            fastSpatialSpec = MotionSchemeKeyTokens.FastSpatial.value()
            slowSpatialSpec = MotionSchemeKeyTokens.SlowSpatial.value()
            defaultEffectsSpec = MotionSchemeKeyTokens.DefaultEffects.value()
            fastEffectsSpec = MotionSchemeKeyTokens.FastEffects.value()
            slowEffectsSpec = MotionSchemeKeyTokens.SlowEffects.value()
        }

        rule.runOnIdle {
            assertThat(motionScheme.defaultSpatialSpec<Float>()).isEqualTo(defaultSpatialSpec)
            assertThat(motionScheme.fastSpatialSpec<Float>()).isEqualTo(fastSpatialSpec)
            assertThat(motionScheme.slowSpatialSpec<Float>()).isEqualTo(slowSpatialSpec)
            assertThat(motionScheme.defaultEffectsSpec<Float>()).isEqualTo(defaultEffectsSpec)
            assertThat(motionScheme.fastEffectsSpec<Float>()).isEqualTo(fastEffectsSpec)
            assertThat(motionScheme.slowEffectsSpec<Float>()).isEqualTo(slowEffectsSpec)
        }
    }
}
