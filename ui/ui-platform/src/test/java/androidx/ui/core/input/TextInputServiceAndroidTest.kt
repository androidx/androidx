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

package androidx.ui.core.input

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.test.filters.SmallTest
import androidx.ui.input.EditorState
import androidx.ui.input.ImeAction
import androidx.ui.input.KeyboardType
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TextInputServiceAndroidTest {

    private lateinit var textInputService: TextInputServiceAndroid
    private lateinit var imm: InputMethodManager

    @Before
    fun setup() {
        imm = mock()
        val view: View = mock()
        val context: Context = mock()
        whenever(context.getSystemService(eq(Context.INPUT_METHOD_SERVICE))).thenReturn(imm)
        whenever(view.context).thenReturn(context)
        textInputService = TextInputServiceAndroid(view)
    }

    @Test
    fun test_fill_editor_info_text() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Text,
            ImeAction.Unspecified,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED)
        }
    }

    @Test
    fun test_fill_editor_info_ascii() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Unspecified,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED)
        }
    }

    @Test
    fun test_fill_editor_info_number() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Number,
            ImeAction.Unspecified,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED)
        }
    }

    @Test
    fun test_fill_editor_info_phone() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Phone,
            ImeAction.Unspecified,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_PHONE and info.inputType) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED)
        }
    }

    @Test
    fun test_fill_editor_info_uri() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Uri,
            ImeAction.Unspecified,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_URI and info.inputType) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED)
        }
    }

    @Test
    fun test_fill_editor_info_email() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Email,
            ImeAction.Unspecified,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS and info.inputType) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_UNSPECIFIED)
        }
    }

    @Test
    fun test_fill_editor_info_action_none() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.NoAction,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_NONE)
        }
    }

    @Test
    fun test_fill_editor_info_action_go() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Go,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_GO)
        }
    }

    @Test
    fun test_fill_editor_info_action_next() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Next,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_NEXT)
        }
    }

    @Test
    fun test_fill_editor_info_action_previous() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Previous,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_PREVIOUS)
        }
    }

    @Test
    fun test_fill_editor_info_action_search() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Search,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_SEARCH)
        }
    }

    @Test
    fun test_fill_editor_info_action_send() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Send,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_SEND)
        }
    }

    @Test
    fun test_fill_editor_info_action_done() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Ascii,
            ImeAction.Done,
            onEditCommand = {},
            onImeActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
            assertTrue((EditorInfo.IME_MASK_ACTION and info.imeOptions)
                    == EditorInfo.IME_ACTION_DONE)
        }
    }
}