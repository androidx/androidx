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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertContainsColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.filters.SdkSuppress
import org.junit.Rule
import org.junit.Test

class OpenOnPhoneDialogTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun openOnPhone_supports_testtag() {
        rule.setContentWithTheme {
            OpenOnPhoneDialog(
                show = true,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertExists()
    }

    @Test
    fun openOnPhone_supports_swipeToDismiss() {
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            var showDialog by remember { mutableStateOf(true) }
            OpenOnPhoneDialog(
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = { showDialog = false },
                show = showDialog
            )
        }
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis / 2)
        rule.onNodeWithTag(TEST_TAG).performTouchInput({ swipeRight() })
        // Advancing time so that the dialog is dismissed
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun hides_openOnPhone_when_show_false() {
        rule.setContentWithTheme {
            OpenOnPhoneDialog(
                show = false,
                modifier = Modifier.testTag(TEST_TAG),
                onDismissRequest = {},
            )
        }
        rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun openOnPhone_displays_icon() {
        rule.setContentWithTheme {
            OpenOnPhoneDialog(onDismissRequest = {}, show = true) { TestImage(IconTestTag) }
        }
        rule.onNodeWithTag(IconTestTag).assertExists()
    }

    @Test
    fun openOnPhone_dismissed_after_timeout() {
        var dismissed = false
        rule.mainClock.autoAdvance = false
        rule.setContentWithTheme {
            OpenOnPhoneDialog(onDismissRequest = { dismissed = true }, show = true) {}
        }
        // Timeout longer than default confirmation duration
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis + 1000)
        assert(dismissed)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun openOnPhone_correct_colors() {
        rule.mainClock.autoAdvance = false
        var expectedIconColor: Color = Color.Unspecified
        var expectedIconContainerColor: Color = Color.Unspecified
        var expectedProgressIndicatorColor: Color = Color.Unspecified
        var expectedProgressTrackColor: Color = Color.Unspecified
        rule.setContentWithTheme {
            OpenOnPhoneDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                show = true
            )
            expectedIconColor = MaterialTheme.colorScheme.primary
            expectedIconContainerColor = MaterialTheme.colorScheme.primaryContainer
            expectedProgressIndicatorColor = MaterialTheme.colorScheme.primary
            expectedProgressTrackColor = MaterialTheme.colorScheme.onPrimary
        }
        // Advance time by half of the default confirmation duration, so that the track and
        // indicator are shown
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis / 2)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(expectedIconColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedIconContainerColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedProgressIndicatorColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(expectedProgressTrackColor)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    @Test
    fun openOnPhone_custom_colors() {
        rule.mainClock.autoAdvance = false
        val customIconColor: Color = Color.Red
        val customIconContainerColor: Color = Color.Green
        val customProgressIndicatorColor: Color = Color.Blue
        val customProgressTrackColor: Color = Color.Magenta
        rule.setContentWithTheme {
            OpenOnPhoneDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag(TEST_TAG),
                colors =
                    OpenOnPhoneDialogDefaults.colors(
                        iconColor = customIconColor,
                        iconContainerColor = customIconContainerColor,
                        progressIndicatorColor = customProgressIndicatorColor,
                        progressTrackColor = customProgressTrackColor
                    ),
                show = true
            )
        }
        // Advance time by half of the default confirmation duration, so that the track and
        // indicator are shown
        rule.mainClock.advanceTimeBy(OpenOnPhoneDialogDefaults.DurationMillis / 2)

        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customIconContainerColor)
        rule
            .onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertContainsColor(customProgressIndicatorColor)
        rule.onNodeWithTag(TEST_TAG).captureToImage().assertContainsColor(customProgressTrackColor)
    }
}

private const val IconTestTag = "icon"
