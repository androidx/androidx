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

package androidx.compose.ui.test.samples

import androidx.activity.ComponentActivity
import androidx.annotation.Sampled
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import org.junit.Test

/**
 * Sample that shows how to enable accessibility checks from common code, when using a
 * ComposeTestRule.
 */
@Sampled
fun accessibilityChecks_withComposeTestRule_sample() {
    // Enable accessibility checks with default configuration:
    composeTestRule.enableAccessibilityChecks()

    // Accessibility checks are run automatically when performing an action:
    composeTestRule.onNodeWithText("Submit").performClick()

    // You can also manually run accessibility checks:
    composeTestRule.onRoot().tryPerformAccessibilityChecks()

    // When disabling accessibility checks..
    composeTestRule.disableAccessibilityChecks()

    // .. they no longer run when performing an action:
    composeTestRule.onNodeWithTag("list").performScrollToIndex(15)
}

/**
 * Sample that shows how to enable accessibility checks from common code, when using
 * runComposeUiTest {}.
 */
@Sampled
fun accessibilityChecks_withComposeUiTest_sample() {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun testWithAccessibilityChecks() = runComposeUiTest {
        // Enable accessibility checks with default configuration:
        enableAccessibilityChecks()

        // When enabled, accessibility checks run automatically when performing an action:
        onNodeWithText("Submit").performClick()

        // You can also manually run accessibility checks:
        onRoot().tryPerformAccessibilityChecks()

        // When disabling accessibility checks..
        disableAccessibilityChecks()

        // .. they no longer run when performing an action:
        onNodeWithTag("list").performScrollToIndex(15)
    }
}

/**
 * Sample that shows how to enable accessibility checks from android code, when using a
 * AndroidComposeTestRule<A : ComponentActivity>.
 */
@Sampled
fun accessibilityChecks_withAndroidComposeTestRule_sample() {
    // Enable accessibility checks with your own AccessibilityValidator:
    androidComposeTestRule.accessibilityValidator = AccessibilityValidator()
    // By setting a non-null AccessibilityValidator, accessibility checks are enabled

    // Configure the AccessibilityValidator:
    androidComposeTestRule.accessibilityValidator!!.setThrowExceptionFor(
        AccessibilityCheckResultType.ERROR
    )
}

/**
 * Sample that shows how to enable accessibility checks from android code, when using
 * runAndroidComposeUiTest<A : ComponentActivity> {}.
 */
@Sampled
fun accessibilityChecks_withAndroidComposeUiTest_sample() {
    @Test
    @OptIn(ExperimentalTestApi::class)
    fun testWithAccessibilityChecks() =
        runAndroidComposeUiTest<ComponentActivity> {
            // Enable accessibility checks with your own AccessibilityValidator:
            accessibilityValidator = AccessibilityValidator()
            // By setting a non-null AccessibilityValidator, accessibility checks are enabled

            // Configure the AccessibilityValidator:
            @OptIn(ExperimentalTestApi::class)
            accessibilityValidator!!.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
        }
}
