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

package androidx.compose.ui.test.accessibility

import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.ui.test.ComposeAccessibilityValidator
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator

/**
 * Enables accessibility checks using an [accessibilityValidator] that will be run before every
 * action that is expected to change the UI.
 *
 * This requires API 34+ (Android U), and currently does not work on Robolectric.
 *
 * @sample androidx.compose.ui.test.accessibility.samples.accessibilityChecks_withComposeUiTest_sample
 * @sample androidx.compose.ui.test.accessibility.samples.accessibilityChecks_withAndroidComposeUiTest_sample
 *
 * If you have a hybrid application with both Compose and Views, and you use both Compose Test and
 * Espresso, then you should set up accessibility checks in both frameworks and share the
 * configuration in the following way:
 *
 * @sample androidx.compose.ui.test.accessibility.samples.accessibilityChecks_interopWithEspresso_withTestFunction
 * @see disableAccessibilityChecks
 */
@ExperimentalTestApi
@RequiresApi(34)
public fun ComposeUiTest.enableAccessibilityChecks(
    accessibilityValidator: AccessibilityValidator =
        AccessibilityValidator().setRunChecksFromRootView(true)
) {
    if (HasRobolectricFingerprint) {
        // TODO(b/332778271): Remove this warning when said bug is fixed
        Log.w("ComposeUiTest", "Accessibility checks are currently not supported by Robolectric")
        return
    }
    setComposeAccessibilityValidator(
        object : ComposeAccessibilityValidator {
            override fun check(view: View) {
                accessibilityValidator.check(view)
            }
        }
    )
}

/**
 * Disables accessibility checks.
 *
 * @sample androidx.compose.ui.test.accessibility.samples.accessibilityChecks_withComposeUiTest_sample
 * @see enableAccessibilityChecks
 */
@ExperimentalTestApi
@RequiresApi(34)
public fun ComposeUiTest.disableAccessibilityChecks() {
    setComposeAccessibilityValidator(null)
}

/** Whether or not this test is running on Robolectric. */
private val HasRobolectricFingerprint
    get() = Build.FINGERPRINT.lowercase() == "robolectric"
