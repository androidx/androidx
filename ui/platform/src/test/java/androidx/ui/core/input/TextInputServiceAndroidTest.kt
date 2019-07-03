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
            onEditCommand = {},
            onEditorActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
        }
    }

    @Test
    fun test_fill_editor_info_ascii() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.ASCII,
            onEditCommand = {},
            onEditorActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((EditorInfo.IME_FLAG_FORCE_ASCII and info.imeOptions) != 0)
        }
    }

    @Test
    fun test_fill_editor_info_number() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Number,
            onEditCommand = {},
            onEditorActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_NUMBER and info.inputType) != 0)
        }
    }

    @Test
    fun test_fill_editor_info_phone() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Phone,
            onEditCommand = {},
            onEditorActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_PHONE and info.inputType) != 0)
        }
    }

    @Test
    fun test_fill_editor_info_uri() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.URI,
            onEditCommand = {},
            onEditorActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_URI and info.inputType) != 0)
        }
    }

    @Test
    fun test_fill_editor_info_email() {
        textInputService.startInput(
            EditorState(""),
            KeyboardType.Email,
            onEditCommand = {},
            onEditorActionPerformed = {})

        EditorInfo().let { info ->
            textInputService.createInputConnection(info)
            assertTrue((InputType.TYPE_CLASS_TEXT and info.inputType) != 0)
            assertTrue((InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS and info.inputType) != 0)
        }
    }
}