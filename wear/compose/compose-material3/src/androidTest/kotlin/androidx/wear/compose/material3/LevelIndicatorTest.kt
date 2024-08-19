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

package androidx.wear.compose.material3

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class LevelIndicatorTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun supports_test_tag() {
        rule.setContentWithTheme { LevelIndicator() }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_indicator_correct_color() {
        var expectedColor: Color = Color.Unspecified
        rule.setContentWithTheme {
            // Show level = 100 so that the indicator color is shown
            LevelIndicator(value = 100f)
            expectedColor = MaterialTheme.colorScheme.secondaryDim
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_track_correct_color() {
        var expectedColor: Color = Color.Unspecified
        rule.setContentWithTheme {
            // Show level = 0 so that the track color is shown
            LevelIndicator(value = 0f)
            expectedColor = MaterialTheme.colorScheme.surfaceContainer
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_indicator_custom_color() {
        val customColor = Color.Red
        rule.setContentWithTheme {
            // Show level = 100 so that the indicator color is shown
            LevelIndicator(
                value = 100f,
                colors = LevelIndicatorDefaults.colors(indicatorColor = customColor)
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customColor)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun gives_track_custom_color() {
        val customColor = Color.Red
        rule.setContentWithTheme {
            // Show level = 0 so that the track color is shown
            LevelIndicator(
                value = 0f,
                colors = LevelIndicatorDefaults.colors(trackColor = customColor)
            )
        }

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customColor)
    }

    @Composable
    private fun LevelIndicator(
        value: Float = 50f,
        colors: LevelIndicatorColors = LevelIndicatorDefaults.colors(),
        enabled: Boolean = true,
    ) {
        val valueRange = 0f..100f

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LevelIndicator(
                value = { value },
                valueRange = valueRange,
                modifier = Modifier.testTag(TEST_TAG).align(Alignment.CenterStart),
                colors = colors,
                enabled = enabled
            )
        }
    }
}
