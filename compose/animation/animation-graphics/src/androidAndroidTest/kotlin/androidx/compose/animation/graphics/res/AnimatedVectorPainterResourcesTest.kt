/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.graphics.res

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.test.R
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.VectorConfig
import androidx.compose.ui.graphics.vector.VectorProperty
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalAnimationGraphicsApi::class)
@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatedVectorPainterResourcesTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun vectorConfig() {
        val isAtEnd = mutableStateOf(false)
        var checked = false
        rule.setContent {
            val avd = AnimatedImageVector.animatedVectorResource(R.drawable.avd_complex)
            rememberAnimatedVectorPainter(
                animatedImageVector = avd,
                atEnd = isAtEnd.value
            ) { _, map ->
                if (!checked) {
                    assertThat(map).containsKey("background")
                    val config = map["background"] as VectorConfig
                    val fill = config.getOrDefault(VectorProperty.Fill, null)
                    assertThat(fill).isNotNull()
                    val stroke = config.getOrDefault(VectorProperty.Stroke, null)
                    assertThat(stroke).isNull()
                    checked = true
                }
            }
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(checked).isTrue()
    }

    @Test
    fun targetDuplicated() {
        val isAtEnd = mutableStateOf(false)
        var checked = false
        rule.setContent {
            val avd = AnimatedImageVector.animatedVectorResource(R.drawable.target_duplicated)
            rememberAnimatedVectorPainter(
                animatedImageVector = avd,
                atEnd = isAtEnd.value
            ) { _, map ->
                if (!checked) {
                    assertThat(map).containsKey("line_01")
                    val config = map["line_01"] as VectorConfig
                    val strokeWidth = config.getOrDefault(VectorProperty.StrokeLineWidth, 0f)
                    assertThat(strokeWidth).isNotEqualTo(0f)
                    val stroke = config.getOrDefault(VectorProperty.Stroke, null)
                    assertThat(stroke).isNotNull()
                    checked = true
                }
            }
        }
        rule.runOnIdle { isAtEnd.value = true }
        rule.waitForIdle()
        assertThat(checked).isTrue()
    }
}
