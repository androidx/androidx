/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.tv.material.carousel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import java.time.Duration
import org.junit.Rule
import org.junit.Test

class CarouselItemTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalTvMaterialApi::class)
    @Test
    fun carouselItem_overlayVisibleAfterRenderTime() {
        val overlayEnterTransitionStartDelay = Duration.ofSeconds(2)
        val overlayTag = "overlay"
        val backgroundTag = "background"
        rule.setContent {
            CarouselItem(
                overlayEnterTransitionStartDelay = overlayEnterTransitionStartDelay,
                background = {
                    Box(
                        Modifier
                            .testTag(backgroundTag)
                            .size(200.dp)
                            .background(Color.Blue)) }) {
                Box(
                    Modifier
                        .testTag(overlayTag)
                        .size(50.dp)
                        .background(Color.Red))
            }
        }

        // only background is visible
        rule.onNodeWithTag(backgroundTag).assertExists()
        rule.onNodeWithTag(overlayTag).assertDoesNotExist()

        // advance clock by `overlayEnterTransitionStartDelay`
        rule.mainClock.advanceTimeBy(overlayEnterTransitionStartDelay.toMillis())

        rule.onNodeWithTag(backgroundTag).assertExists()
        rule.onNodeWithTag(overlayTag).assertExists()
    }
}
