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

package androidx.compose.material.textfield

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.OutlinedSecureTextField
import androidx.compose.material.SecureTextField
import androidx.compose.material.setMaterialContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SecureTextFieldTest {
    private val TextFieldTag = "TextField"

    @get:Rule val rule = createComposeRule()

    @Test
    fun testSecureTextField_filled_textContentIsNotObfuscated() {
        rule.setMaterialContent {
            SecureTextField(
                state = rememberTextFieldState("password"),
                modifier = Modifier.testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).assertTextEquals("password")
    }

    @Test
    fun testSecureTextField_outlined_textContentIsNotObfuscated() {
        rule.setMaterialContent {
            OutlinedSecureTextField(
                state = rememberTextFieldState("password"),
                modifier = Modifier.testTag(TextFieldTag)
            )
        }

        rule.onNodeWithTag(TextFieldTag).assertTextEquals("password")
    }
}
