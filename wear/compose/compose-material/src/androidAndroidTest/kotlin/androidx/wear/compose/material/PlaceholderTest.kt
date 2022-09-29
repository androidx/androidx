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

package androidx.wear.compose.material

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class PlaceholderTest {
    @get:Rule
    val rule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_is_correct_color() {
        var expectedPlaceholderColor = Color.Transparent
        // var expectedBackgroundColor = Color.Transparent
        var contentReady = false
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            expectedPlaceholderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                .compositeOver(MaterialTheme.colors.surface)
            // expectedBackgroundColor = MaterialTheme.colors.primary
            Chip(
                modifier = Modifier
                    .testTag("test-item")
                    .placeholder(placeholderState = placeholderState),
                content = {},
                onClick = {},
                colors = ChipDefaults.primaryChipColors(),
                border = ChipDefaults.chipBorder()
            )
            LaunchedEffect(placeholderState) {
                placeholderState.startPlaceholderAnimation()
            }
        }

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedPlaceholderColor
            )

//        contentReady = true
//        // Ideally advance the clock in order to let the wipe off take effect.
//        // However this doesn't work with withInfiniteAnimationFrameMillis as it is not delivered
//        // any frame timings so we will need to look for a different way to test
//        rule.mainClock.advanceTimeBy(3500)
//        rule.onNodeWithTag("test-item")
//            .captureToImage()
//            .assertContainsColor(
//                expectedBackgroundColor
//            )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalWearMaterialApi::class)
    @Test
    fun placeholder_background_is_correct_color() {
        var expectedPlaceholderBackgroundColor = Color.Transparent
        // var expectedBackgroundColor = Color.Transparent
        var contentReady = false
        lateinit var placeholderState: PlaceholderState
        rule.setContentWithTheme {
            placeholderState = rememberPlaceholderState {
                contentReady
            }
            expectedPlaceholderBackgroundColor = MaterialTheme.colors.surface
            // expectedBackgroundColor = MaterialTheme.colors.primary
            Chip(
                modifier = Modifier
                    .testTag("test-item"),
                content = {},
                onClick = {},
                colors = PlaceholderDefaults.placeholderChipColors(
                    originalChipColors = ChipDefaults.primaryChipColors(),
                    placeholderState = placeholderState,
                ),
                border = ChipDefaults.chipBorder()
            )
            LaunchedEffect(placeholderState) {
                placeholderState.startPlaceholderAnimation()
            }
        }

        rule.onNodeWithTag("test-item")
            .captureToImage()
            .assertContainsColor(
                expectedPlaceholderBackgroundColor
            )

//        contentReady = true
//        // Ideally advance the clock in order to let the wipe off take effect.
//        // However this doesn't work with withInfiniteAnimationFrameMillis as it is not delivered
//        // any frame timings so we will need to look for a different way to test
//        rule.mainClock.advanceTimeBy(3500)
//        rule.onNodeWithTag("test-item")
//            .captureToImage()
//            .assertContainsColor(
//                expectedBackgroundColor
//            )
    }
}