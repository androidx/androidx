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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class SecureTextFieldScreenshotTest {
    private val TextFieldTag = "TextField"

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun secureTextField_filled_noObfuscation() {
        rule.setMaterialContent(lightColorScheme()) {
            SecureTextField(
                state = rememberTextFieldState("password"),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag),
                textObfuscationMode = TextObfuscationMode.Visible,
            )
        }

        assertAgainstGolden("secureTextField_filled_noObfuscation")
    }

    @Test
    fun secureTextField_outlined_noObfuscation() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedSecureTextField(
                state = rememberTextFieldState("password"),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag),
                textObfuscationMode = TextObfuscationMode.Visible,
            )
        }

        assertAgainstGolden("secureTextField_outlined_noObfuscation")
    }

    @Test
    fun secureTextField_filled_customObfuscationCharacter() {
        rule.setMaterialContent(lightColorScheme()) {
            SecureTextField(
                state = rememberTextFieldState("12345"),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag),
                textObfuscationMode = TextObfuscationMode.Hidden,
                textObfuscationCharacter = '?',
            )
        }

        assertAgainstGolden("secureTextField_filled_customObfuscationCharacter")
    }

    @Test
    fun secureTextField_outlined_customObfuscationCharacter() {
        rule.setMaterialContent(lightColorScheme()) {
            OutlinedSecureTextField(
                state = rememberTextFieldState("12345"),
                label = { Text("Label") },
                modifier = Modifier.testTag(TextFieldTag),
                textObfuscationMode = TextObfuscationMode.Hidden,
                textObfuscationCharacter = '?',
            )
        }

        assertAgainstGolden("secureTextField_outlined_customObfuscationCharacter")
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule
            .onNodeWithTag(TextFieldTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}
