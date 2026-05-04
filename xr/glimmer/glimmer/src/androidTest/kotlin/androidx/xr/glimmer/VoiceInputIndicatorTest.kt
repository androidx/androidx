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

package androidx.xr.glimmer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.xr.glimmer.testutils.captureToImage
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class VoiceInputIndicatorTest {

    @get:Rule val rule = createComposeRule()

    private val testTag = "voiceIndicator"

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun voiceInputIndicator_drawsDots() {
        val dotColor = Color.Red
        rule.setContent {
            VoiceInputIndicator(
                level = { 0f },
                indicatorColor = dotColor,
                modifier = Modifier.testTag(testTag),
            )
        }

        val size = with(rule.density) { 32.dp.roundToPx() }
        val dotSize = with(rule.density) { 6.dp.roundToPx() }
        val spacing = with(rule.density) { 3.dp.roundToPx() }

        val centerX = size / 2
        val centerY = size / 2

        val pixelMap = rule.onNodeWithTag(testTag).captureToImage().toPixelMap()

        // Center dot
        assertThat(pixelMap[centerX, centerY]).isEqualTo(dotColor)
        // Left dot
        assertThat(pixelMap[centerX - spacing - dotSize, centerY]).isEqualTo(dotColor)
        // Right dot
        assertThat(pixelMap[centerX + spacing + dotSize, centerY]).isEqualTo(dotColor)
    }

    @Test
    fun containedVoiceInputIndicator_drawsContainer() {
        val containerColor = Color.Blue
        rule.setContent {
            ContainedVoiceInputIndicator(
                level = { 0f },
                backgroundColor = containerColor,
                modifier = Modifier.testTag(testTag),
            )
        }

        val size = with(rule.density) { 32.dp.roundToPx() }
        val centerX = size / 2

        // Top check point: 10dp from top
        val checkYTop = with(rule.density) { 10.dp.roundToPx() }
        // Bottom check point: 10dp from bottom
        val checkYBottom = size - checkYTop

        val pixelMap = rule.onNodeWithTag(testTag).captureToImage().toPixelMap()

        assertThat(pixelMap[centerX, checkYTop]).isEqualTo(containerColor)
        assertThat(pixelMap[centerX, checkYBottom]).isEqualTo(containerColor)
    }

    @Test
    fun containedVoiceInputIndicator_dotsAreTransparent() {
        val containerColor = Color.Blue
        rule.setContent {
            Box(modifier = Modifier.background(Color.Green)) {
                ContainedVoiceInputIndicator(
                    level = { 0f },
                    backgroundColor = containerColor,
                    modifier = Modifier.testTag(testTag),
                )
            }
        }

        val size = with(rule.density) { 32.dp.roundToPx() }
        val dotSize = with(rule.density) { 6.dp.roundToPx() }
        val spacing = with(rule.density) { 3.dp.roundToPx() }

        val centerX = size / 2
        val centerY = size / 2

        val pixelMap = rule.onNodeWithTag(testTag).captureToImage().toPixelMap()

        // Center dot
        assertThat(pixelMap[centerX, centerY]).isEqualTo(Color.Green)
        // Left dot
        assertThat(pixelMap[centerX - spacing - dotSize, centerY]).isEqualTo(Color.Green)
        // Right dot
        assertThat(pixelMap[centerX + spacing + dotSize, centerY]).isEqualTo(Color.Green)
    }

    @Test
    fun voiceInputIndicator_animatesInflateAndDeflate() {
        var level by mutableStateOf(0f)
        val dotColor = Color.Red

        rule.setContent {
            VoiceInputIndicator(
                level = { level },
                indicatorColor = dotColor,
                modifier = Modifier.testTag(testTag),
            )
        }

        val size = with(rule.density) { 32.dp.roundToPx() }
        val centerX = size / 2

        // 10dp from top. Inside the inflated bar but outside the deflated dot.
        val testY = with(rule.density) { 10.dp.roundToPx() }

        // Initially at level 0, testY should not be dotColor
        var pixelMap = rule.onNodeWithTag(testTag).captureToImage().toPixelMap()
        assertThat(pixelMap[centerX, testY]).isNotEqualTo(dotColor)

        // Trigger and advance through inflation
        level = 1.0f
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(100)

        pixelMap = rule.onNodeWithTag(testTag).captureToImage().toPixelMap()
        assertThat(pixelMap[centerX, testY]).isEqualTo(dotColor)

        // Wait for deflation
        rule.mainClock.advanceTimeBy(600)

        pixelMap = rule.onNodeWithTag(testTag).captureToImage().toPixelMap()
        assertThat(pixelMap[centerX, testY]).isNotEqualTo(dotColor)
    }
}
