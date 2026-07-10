/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ModalBottomSheetDialogTest {

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>(StandardTestDispatcher())

    @Test
    fun dialog_showsContent() {
        rule.setContent {
            ModalBottomSheetDialog(
                onDismissRequest = {},
                content = { Box(Modifier.fillMaxSize()) { Text("BottomSheet Content") } },
            )
        }

        rule.onNodeWithText("BottomSheet Content").assertIsDisplayed()
    }

    @Test
    @Ignore("Dismiss callback should be reconsidered if API is made public")
    fun dialog_dismissOnBackPress_callsDismissRequest() {
        var dismissCount = 0
        rule.setContent {
            ModalBottomSheetDialog(
                onDismissRequest = { dismissCount++ },
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true),
                content = { Box(Modifier.size(100.dp)) },
            )
        }

        // Wait for dialog to show
        rule.waitForIdle()

        // Trigger system back press
        Espresso.pressBack()

        // Verify callback was invoked
        rule.runOnIdle { assertThat(dismissCount).isEqualTo(1) }
    }

    @Test
    fun dialog_doesNotDismissOnBackPress_whenPropertyFalse() {
        var dismissCount = 0
        rule.setContent {
            ModalBottomSheetDialog(
                onDismissRequest = { dismissCount++ },
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
                content = { Box(Modifier.size(100.dp)) },
            )
        }

        rule.waitForIdle()
        Espresso.pressBackUnconditionally()

        rule.runOnIdle { assertThat(dismissCount).isEqualTo(0) }
    }

    @SdkSuppress(
        minSdkVersion = Build.VERSION_CODES.R
    ) // Flag check relies on modern window APIs usually
    @Test
    fun dialog_securePolicy_setsWindowFlag() {
        var isSecure = false

        rule.setContent {
            ModalBottomSheetDialog(
                onDismissRequest = {},
                properties = ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.SecureOn),
                content = {
                    // Capture the window flags from inside the dialog's composition
                    val view = LocalView.current
                    SideEffect {
                        val windowParams = view.rootView.layoutParams as? WindowManager.LayoutParams
                        isSecure =
                            (windowParams?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE !=
                                0
                    }
                    Box(Modifier.size(100.dp))
                },
            )
        }

        rule.runOnIdle { assertThat(isSecure).isTrue() }
    }

    @Test
    @Ignore("Dismiss callback should be reconsidered if API is made public")
    fun dialog_updatesParameters_whenRecomposed() {
        val shouldDismissOnBack = mutableStateOf(false)
        var dismissCount = 0

        rule.setContent {
            val properties =
                remember(shouldDismissOnBack.value) {
                    ModalBottomSheetProperties(shouldDismissOnBackPress = shouldDismissOnBack.value)
                }
            ModalBottomSheetDialog(
                onDismissRequest = { dismissCount++ },
                properties = properties,
                content = { Box(Modifier.size(100.dp)) },
            )
        }

        // 1. Verify initial state (Back press ignored)
        rule.waitForIdle()
        Espresso.pressBackUnconditionally()
        rule.runOnIdle { assertThat(dismissCount).isEqualTo(0) }

        // 2. Update state to allow back press
        shouldDismissOnBack.value = true
        rule.waitForIdle()

        // 3. Verify update took effect
        Espresso.pressBack()
        rule.runOnIdle { assertThat(dismissCount).isEqualTo(1) }
    }
}
