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

package androidx.compose.foundation.text.input

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.handwriting.handwritingHandler
import androidx.compose.foundation.text.handwriting.isStylusHandwritingSupported
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
internal class HandwritingHandlerTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    @Before
    fun setup() {
        // Test is only meaningful when stylus handwriting is supported.
        Assume.assumeTrue(isStylusHandwritingSupported)
    }

    @Test
    fun handler_gainFocus_acceptsDelegation() {
        val imm = FakeInputMethodManager()
        immRule.setFactory { imm }

        val tag = "handler"
        InputMethodInterceptor(rule).setTextFieldTestContent {
            val state = remember { TextFieldState() }
            BasicTextField(
                state = state,
                modifier = Modifier.fillMaxSize().handwritingHandler().testTag(tag)
            )
        }

        rule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.RequestFocus)

        rule.runOnIdle {
            imm.expectCall("acceptStylusHandwritingDelegation")
            imm.expectNoMoreCalls()
        }
    }
}
