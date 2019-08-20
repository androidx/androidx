/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.test

import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.test.filters.SmallTest
import androidx.ui.core.FocusManagerAmbient
import androidx.ui.core.TestTag
import androidx.ui.core.TextField
import androidx.ui.core.input.FocusManager
import androidx.ui.input.EditorModel
import androidx.ui.input.EditorStyle
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TextFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun textField_focusInSemantics() {
        val focusManager = mock<FocusManager>()
        composeTestRule.setContent {
            val state = +state { EditorModel() }
            FocusManagerAmbient.Provider(value = focusManager) {
                TestTag(tag = "textField") {
                    TextField(
                        value = state.value,
                        onValueChange = { state.value = it },
                        editorStyle = EditorStyle()
                    )
                }
            }
        }

        findByTag("textField")
            .doClick()

        verify(focusManager, times(1)).requestFocus(any())
    }
}