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

package androidx.compose.ui.test

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.expectError
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.test.filters.SdkSuppress
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityViewCheckException
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@OptIn(ExperimentalTestApi::class)
class AccessibilityChecksTest {

    @Test
    fun performAccessibilityChecks_findsNoErrors() = runComposeUiTest {
        setContent { BoxWithoutProblems() }
        enableAccessibilityChecks()

        // There are no errors, this should not throw
        onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun performAccessibilityChecks_findsOneError() = runComposeUiTest {
        setContent { BoxWithMissingContentDescription() }
        enableAccessibilityChecks()

        expectError<AccessibilityViewCheckException>(
            expectedMessage = "There was 1 accessibility result:.*"
        ) {
            // There is an error, this should throw
            onRoot().tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun performAccessibilityChecks_usesCustomValidator() =
        runAndroidComposeUiTest<ComponentActivity> {
            setContent { BoxWithoutProblems() }
            var listenerInvocations = 0
            // addCheckListener resolves to the overload that takes an AccessibilityCheckListener
            accessibilityValidator =
                AccessibilityValidator().addCheckListener { _, _ -> listenerInvocations++ }

            // There are no errors, this should not throw
            onRoot().tryPerformAccessibilityChecks()
            // But our validator must have run checks once
            assertThat(listenerInvocations).isEqualTo(1)
        }

    // Dialogs live in a sibling root view. Test if we catch problems in dialogs
    @Test
    fun performAccessibilityChecks_checksDialogs() = runComposeUiTest {
        setContent {
            BoxWithoutProblems()
            Dialog({}) { BoxWithMissingContentDescription() }
        }

        enableAccessibilityChecks()
        expectError<AccessibilityViewCheckException>(
            expectedMessage = "There was 1 accessibility result:.*"
        ) {
            // There are no errors in the main screen, but there is one in the dialog, so this
            // should throw
            onRoot().tryPerformAccessibilityChecks()
        }
    }

    // Checks that tryPerformAccessibilityChecks does not throw if the validator is thus configured
    @Test
    fun performAccessibilityChecks_allowsErrorCollection() =
        runAndroidComposeUiTest<ComponentActivity> {
            setContent { BoxWithMissingContentDescription() }
            accessibilityValidator = AccessibilityValidator().setThrowExceptionFor(null)

            // Despite the a11y error, this should not throw
            onRoot().tryPerformAccessibilityChecks()
        }

    @Composable
    private fun BoxWithoutProblems() {
        Box(Modifier.size(20.dp))
    }

    @Composable
    private fun BoxWithMissingContentDescription() {
        Box(
            Modifier.size(48.dp).semantics {
                // The SemanticsModifier will make this node importantForAccessibility
                // Having no content description is now a violation
                this.contentDescription = ""
            }
        )
    }
}
