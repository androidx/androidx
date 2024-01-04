/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.foundation.text2

import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.google.common.truth.Truth.assertWithMessage

class TestSoftwareKeyboardController(
    private val rule: ComposeTestRule
) : SoftwareKeyboardController {
    private var shown = false

    override fun show() {
        shown = true
    }

    override fun hide() {
        shown = false
    }

    fun assertShown() {
        rule.runOnIdle {
            assertWithMessage("Expected last call on SoftwareKeyboardController to be show")
                .that(shown).isTrue()
        }
    }

    fun assertHidden() {
        rule.runOnIdle {
            assertWithMessage("Expected last call on SoftwareKeyboardController to be hide")
                .that(shown).isFalse()
        }
    }
}
