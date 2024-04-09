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

package androidx.compose.foundation.text.input.internal

import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class LegacyTextInputMethodRequestOnStateUpdateTest {

    private lateinit var textInputService: LegacyTextInputMethodRequest
    private lateinit var inputMethodManager: InputMethodManager
    private lateinit var inputConnection: RecordingInputConnection
    private val coroutineScope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setup() {
        inputMethodManager = mock<InputMethodManager>()
        textInputService = LegacyTextInputMethodRequest(
            view = View(getInstrumentation().targetContext),
            localToScreen = {},
            inputMethodManager = inputMethodManager
        )
        textInputService.startInput(
            value = TextFieldValue(""),
            textInputNode = null,
            imeOptions = ImeOptions.Default,
            onEditCommand = {},
            onImeActionPerformed = {}
        )
        inputConnection = textInputService.createInputConnection(EditorInfo())
    }

    @After
    fun tearDown() {
        coroutineScope.cancel()
    }

    @Test
    fun onUpdateState_resetInputCalled_whenOnlyTextChanged() {
        val newValue = TextFieldValue("b")
        textInputService.updateState(
            oldValue = TextFieldValue("a"),
            newValue = newValue
        )

        verify(inputMethodManager, times(1)).restartInput()
        verify(inputMethodManager, never()).updateSelection(any(), any(), any(), any())

        assertThat(inputConnection.textFieldValue).isEqualTo(newValue)
        assertThat(textInputService.state).isEqualTo(newValue)
    }

    @Test
    fun onUpdateState_resetInputCalled_whenOnlyCompositionChanged() {
        val newValue = TextFieldValue("a", TextRange.Zero, null)
        textInputService.updateState(
            oldValue = TextFieldValue("a", TextRange.Zero, TextRange.Zero),
            newValue = newValue
        )

        verify(inputMethodManager, times(1)).restartInput()
        verify(inputMethodManager, never()).updateSelection(any(), any(), any(), any())

        assertThat(inputConnection.textFieldValue).isEqualTo(newValue)
        assertThat(textInputService.state).isEqualTo(newValue)
    }

    @Test
    fun onUpdateState_updateSelectionCalled_whenOnlySelectionChanged() {
        val newValue = TextFieldValue("a", TextRange(1), null)
        textInputService.updateState(
            oldValue = TextFieldValue("a", TextRange.Zero, null),
            newValue = newValue
        )

        verify(inputMethodManager, never()).restartInput()
        verify(inputMethodManager, times(1)).updateSelection(any(), any(), any(), any())

        assertThat(inputConnection.textFieldValue).isEqualTo(newValue)
        assertThat(textInputService.state).isEqualTo(newValue)
    }

    @Test
    fun onUpdateState_updateSelectionCalled_whenSelectionIsDifferentFromState() {
        // The textInputService.state has selection = TextRange(0, 0)
        // Here we simulate a situation where a character is inserted and selection is updated to
        // TextRange(1, 1). The EditBuffer is still in sync with the newValue, so the oldValue
        // equals to newValue, but different from the textInputService.state.
        // We still need to call IMM.updateSelection in this case, for more info please check:
        // https://developer.android.com/reference/android/view/inputmethod/InputMethodManager#updateSelection(android.view.View,%20int,%20int,%20int,%20int)
        val value = TextFieldValue("a", TextRange(1), null)
        textInputService.updateState(
            oldValue = value,
            newValue = value
        )

        verify(inputMethodManager, never()).restartInput()
        verify(inputMethodManager, times(1)).updateSelection(any(), any(), any(), any())

        assertThat(inputConnection.textFieldValue).isEqualTo(value)
        assertThat(textInputService.state).isEqualTo(value)
    }

    @Test
    fun onUpdateState_updateSelectionCalled_whenCompositionIsDifferentFromState() {
        // set the initial state, composition active on text, cursor in the middle
        val value1 = TextFieldValue("ab", TextRange(1), TextRange(0, 2))
        textInputService.updateState(
            oldValue = value1,
            newValue = value1
        )

        reset(inputMethodManager)

        // reset the composition, however simulate that editing buffer already applied the
        // change and old and new values are the same. However they will be different than the
        // last stored TextFieldValue in TextInputService which is value1
        val value2 = value1.copy(composition = null)
        textInputService.updateState(
            oldValue = value2,
            newValue = value2
        )

        verify(inputMethodManager, never()).restartInput()
        verify(inputMethodManager, times(1)).updateSelection(
            eq(value2.selection.min), eq(value2.selection.max), eq(-1), eq(-1)
        )
    }

    @Test
    fun onUpdateState_resetInputNotCalled_whenSelectionAndCompositionChanged() {
        val newValue = TextFieldValue("a", TextRange(1), null)
        textInputService.updateState(
            oldValue = TextFieldValue("a", TextRange.Zero, TextRange.Zero),
            newValue = newValue
        )

        verify(inputMethodManager, never()).restartInput()
        verify(inputMethodManager, times(1)).updateSelection(any(), any(), any(), any())

        assertThat(inputConnection.textFieldValue).isEqualTo(newValue)
        assertThat(textInputService.state).isEqualTo(newValue)
    }

    @Test
    fun onUpdateState_resetInputNotCalled_whenValuesAreSame() {
        val value = TextFieldValue("a")
        textInputService.updateState(
            oldValue = value,
            newValue = value
        )

        verify(inputMethodManager, never()).restartInput()
        verify(inputMethodManager, never()).updateSelection(any(), any(), any(), any())

        assertThat(inputConnection.textFieldValue).isEqualTo(value)
        assertThat(textInputService.state).isEqualTo(value)
    }

    @Test
    fun onUpdateState_recreateInputConnection_createsWithCorrectValue() {
        val value = TextFieldValue("a")
        textInputService.updateState(
            oldValue = value,
            newValue = value
        )

        verify(inputMethodManager, never()).restartInput()
        verify(inputMethodManager, never()).updateSelection(any(), any(), any(), any())

        // recreate the connection
        inputConnection = textInputService.createInputConnection(EditorInfo())

        assertThat(inputConnection.textFieldValue).isEqualTo(value)
    }
}
