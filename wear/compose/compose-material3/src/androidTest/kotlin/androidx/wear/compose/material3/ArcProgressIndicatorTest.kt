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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

class ArcProgressIndicatorTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun indeterminate_arc_supports_testtag() {
        setContentWithTheme { ArcProgressIndicator(modifier = Modifier.testTag(TEST_TAG)) }

        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminate_arc_contains_default_indicator_color() {
        rule.mainClock.autoAdvance = false
        var expectedColor = Color.Unspecified

        setContentWithTheme {
            ArcProgressIndicator(
                modifier = Modifier.size(COMPONENT_SIZE).testTag(TEST_TAG),
            )
            expectedColor = MaterialTheme.colorScheme.primary
        }

        rule.mainClock.advanceTimeBy(250)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminate_arc_contains_custom_indicator_color() {
        rule.mainClock.autoAdvance = false
        val customColor = Color.Yellow

        setContentWithTheme {
            ArcProgressIndicator(
                modifier = Modifier.size(COMPONENT_SIZE).testTag(TEST_TAG),
                colors = ProgressIndicatorDefaults.colors(indicatorColor = customColor)
            )
        }

        rule.mainClock.advanceTimeBy(250)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminate_arc_contains_default_track_color() {
        rule.mainClock.autoAdvance = false
        var expectedColor = Color.Unspecified

        setContentWithTheme {
            ArcProgressIndicator(
                modifier = Modifier.size(COMPONENT_SIZE).testTag(TEST_TAG),
            )
            expectedColor = MaterialTheme.colorScheme.surfaceContainer
        }

        rule.mainClock.advanceTimeBy(250)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun indeterminate_arc_contains_custom_track_color() {
        rule.mainClock.autoAdvance = false
        val customColor = Color.Yellow

        setContentWithTheme {
            ArcProgressIndicator(
                modifier = Modifier.size(COMPONENT_SIZE).testTag(TEST_TAG),
                colors = ProgressIndicatorDefaults.colors(trackColor = customColor)
            )
        }

        rule.mainClock.advanceTimeBy(250)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customColor)
    }

    private fun setContentWithTheme(composable: @Composable BoxScope.() -> Unit) {
        // Use constant size modifier to limit relative color percentage ranges.
        rule.setContentWithTheme(modifier = Modifier.size(COMPONENT_SIZE)) {
            ScreenConfiguration(SCREEN_SIZE_LARGE) { composable() }
        }
    }
}

private val COMPONENT_SIZE = 204.dp
