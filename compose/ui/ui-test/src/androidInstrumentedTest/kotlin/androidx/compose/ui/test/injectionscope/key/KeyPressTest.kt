/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test.injectionscope.key

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.KeyInjectionScope
import androidx.compose.ui.test.injectionscope.key.Common.assertTyped
import androidx.compose.ui.test.injectionscope.key.Common.performKeyInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.util.TestTextField
import androidx.compose.ui.test.util.TestTextField.Tag
import androidx.test.filters.FlakyTest
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tests if [KeyInjectionScope.pressKey] works.
 */
@LargeTest
class KeyPressTest {

    @get:Rule
    val rule = createComposeRule()

    @Before
    fun setUp() {
        // Set content to a simple text field.
        rule.setContent {
            TestTextField()
        }
        // Bring text field into focus by clicking on it.
        rule.onNodeWithTag(Tag).performClick()
    }

    @Test
    fun pressingEnter_typesNewLine() {
        rule.performKeyInput { pressKey(Key.Enter) }
        rule.assertTyped("\n")
    }

    @Test
    fun pressingLetterKeys_typesLetterChars() {
        rule.performKeyInput {
            pressKey(Key.A)
            pressKey(Key.B)
        }
        rule.performKeyInput { pressKey(Key.B) }
        rule.assertTyped("abb")
    }

    @FlakyTest(bugId = 236864049)
    @Test
    fun pressingNumberKeys_typesNumberChars() {
        rule.performKeyInput { pressKey(Key.One) }
        rule.performKeyInput { pressKey(Key.Two) }
        rule.assertTyped("12")
    }

    @Test
    fun pressingBackspace_deletesLastCharacter() {
        rule.performKeyInput {
            pressKey(Key.A)
            pressKey(Key.B)
            pressKey(Key.C)
        }
        rule.assertTyped("abc")
        rule.performKeyInput { pressKey(Key.Backspace) }
        rule.assertTyped("ab")
    }
}
