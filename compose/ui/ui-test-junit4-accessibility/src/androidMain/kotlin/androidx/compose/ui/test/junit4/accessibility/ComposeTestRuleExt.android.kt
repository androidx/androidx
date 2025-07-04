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

package androidx.compose.ui.test.junit4.accessibility

import androidx.annotation.RequiresApi
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator

/**
 * Enables accessibility checks that will be run before every action that is expected to change the
 * UI.
 *
 * This requires API 34+ (Android U), and currently does not work on Robolectric.
 *
 * Enabling accessibility checks is currently only supported for AndroidComposeTestRule. If you have
 * a custom ComposeTestRule implementation that delegates to AndroidComposeTestRule, make sure to
 * call this function on the AndroidComposeTestRule instead.
 *
 * @sample androidx.compose.ui.test.junit4.accessibility.samples.accessibilityChecks_withComposeTestRule_sample
 * @see disableAccessibilityChecks
 */
@RequiresApi(34)
public fun ComposeTestRule.enableAccessibilityChecks(
    accessibilityValidator: AccessibilityValidator =
        AccessibilityValidator().setRunChecksFromRootView(true)
) {
    if (this is AndroidComposeTestRule<*, *>) {
        this.enableAccessibilityChecks(accessibilityValidator)
    } else {
        throw NotImplementedError(
            "Enabling accessibility checks is currently only supported for AndroidComposeTestRule. If you have a custom ComposeTestRule implementation that wraps an AndroidComposeTestRule, directly call enableAccessibilityChecks on the AndroidComposeTestRule instead"
        )
    }
}

/**
 * Disables accessibility checks.
 *
 * @sample androidx.compose.ui.test.junit4.accessibility.samples.accessibilityChecks_withComposeTestRule_sample
 * @see enableAccessibilityChecks
 */
@RequiresApi(34)
public fun ComposeTestRule.disableAccessibilityChecks() {
    if (this is AndroidComposeTestRule<*, *>) {
        this.disableAccessibilityChecks()
    } else {
        throw NotImplementedError(
            "Enabling accessibility checks is currently only supported for AndroidComposeTestRule. If you have a custom ComposeTestRule implementation that wraps an AndroidComposeTestRule, directly call enableAccessibilityChecks on the AndroidComposeTestRule instead"
        )
    }
}
