/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.xr.glimmer.internal.color.HctUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ContentColorTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultContentColor() {
        var color: Color = Color.Unspecified
        rule.setContent {
            Box(
                Modifier.then(
                    DelegatableNodeProviderElement {
                        color = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            )
        }

        rule.runOnIdle { assertThat(color).isEqualTo(Color.White) }
    }

    @Test
    fun contentColorProvider() {
        var color: Color = Color.Unspecified
        rule.setGlimmerThemeContent {
            Box(
                Modifier.contentColorProvider(Color.White)
                    .then(
                        DelegatableNodeProviderElement {
                            color = it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
            )
        }

        rule.runOnIdle { assertThat(color).isEqualTo(Color.White) }
    }

    @Test
    fun contentColorProvider_updates() {
        var expectedColor by mutableStateOf(Color.White)
        var node: DelegatableNode? = null
        rule.setGlimmerThemeContent {
            Box(
                Modifier.contentColorProvider(contentColor = expectedColor)
                    .then(DelegatableNodeProviderElement { node = it })
            )
        }

        rule.runOnIdle {
            assertThat(node!!.currentContentColor()).isEqualTo(Color.White)
            expectedColor = Color.Blue
        }

        rule.runOnIdle { assertThat(node!!.currentContentColor()).isEqualTo(Color.Blue) }
    }

    @Test
    fun calculateContentColorReturnsExpectedValues() {
        assertThat(calculateContentColor(Color.Black)).isEqualTo(Color.White)
        assertThat(calculateContentColor(Color.White)).isEqualTo(Color.Black)
        assertThat(calculateContentColor(Color.Red)).isEqualTo(Color.Black)
        assertThat(calculateContentColor(Color.Green)).isEqualTo(Color.Black)
        assertThat(calculateContentColor(Color.Blue)).isEqualTo(Color.White)
    }

    @Test
    fun calculateLowEmphasisContentColorReturnsExpectedValues() {
        val light = calculateLowEmphasisContentColor(Color.White)
        val dark = calculateLowEmphasisContentColor(Color.Black)
        assertThat(light).isEqualTo(Color(0xFFC9C9C9))
        assertThat(dark).isEqualTo(Color(0xFF2E2E2E))
        // White and black have tones of 100 and 0, so these are 19 tones closer to the middle. The
        // tones are approximate because each color channel is rounded to 8 bits.
        assertThat(HctUtils.argbToTone(light.toArgb())).isWithin(0.5).of(81.0)
        assertThat(HctUtils.argbToTone(dark.toArgb())).isWithin(0.5).of(19.0)
    }
}
