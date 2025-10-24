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

package androidx.compose.foundation.textfield

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TextFieldToolbarTest {
    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    val TAG = "TestBasicTextField"

    @OptIn(ExperimentalFoundationApi::class)
    @Before
    fun setUp() {
        assumeTrue(ComposeFoundationFlags.isNewContextMenuEnabled)
    }

    @Test
    fun btf1_decorationBoxNotCallInnerTextField_longPress_doNotCrash() {
        rule.setContent {
            BasicTextField(
                modifier = Modifier.testTag(TAG),
                value = TextFieldValue(),
                onValueChange = {},
                decorationBox = { innerTextField ->
                    Box(modifier = Modifier.size(50.dp, 50.dp).background(Color.Red))
                },
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        // Long click won't crash, no assertion needed.
    }

    @Test
    fun btf2_decorationBoxNotCallInnerTextField_longPress_doNotCrash() {
        rule.setContent {
            val state = rememberTextFieldState()
            BasicTextField(
                modifier = Modifier.testTag(TAG),
                state = state,
                decorator = { innerTextField ->
                    Box(modifier = Modifier.size(50.dp, 50.dp).background(Color.Red))
                },
            )
        }

        rule.onNodeWithTag(TAG).performTouchInput { longClick() }
        // Long click won't crash, no assertion needed.
    }
}
