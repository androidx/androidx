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

package androidx.compose.ui.test.junit4.accessibility.samples

import androidx.activity.ComponentActivity
import androidx.annotation.Sampled
import androidx.compose.ui.test.junit4.accessibility.disableAccessibilityChecks
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.espresso.accessibility.AccessibilityChecks
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator

private val composeTestRule = createComposeRule()
private val androidComposeTestRule = createAndroidComposeRule<ComponentActivity>()

/** Sample that shows how to enable accessibility checks when using a ComposeTestRule. */
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
 * Sample that shows how to enable accessibility checks when using a custom validator and
 * AndroidComposeTestRule<A : ComponentActivity>.
 */
@Sampled
fun accessibilityChecks_withAndroidComposeTestRule_sample() {
    // Configure your own AccessibilityValidator
    val accessibilityValidator =
        AccessibilityValidator().also {
            it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
        }

    // Enable accessibility checks with your own AccessibilityValidator:
    androidComposeTestRule.enableAccessibilityChecks(accessibilityValidator)
}

/**
 * Sample that shows how to set up accessibility checks in a hybrid environment, when using a
 * ComposeTestRule.
 */
@Sampled
fun accessibilityChecks_interopWithEspresso_withTestRule() {
    // Enable accessibility checks in both Espresso and Compose, and share the configuration
    val accessibilityValidator = AccessibilityChecks.enable()
    androidComposeTestRule.enableAccessibilityChecks(accessibilityValidator)
}
