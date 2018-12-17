/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.widgets.editable_text

import androidx.ui.VoidCallback
import androidx.ui.services.text_editing.TextRange
import androidx.ui.services.text_editing.TextSelection
import androidx.ui.services.text_input.TextEditingValue
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class TextEditingControllerTest {

    open class CallbackMockHelper : VoidCallback {
        override fun invoke() {}
    }

    @Test
    fun `constructor with null string`() {
        val c = TextEditingController(null as String?)
        assertThat(c.text, equalTo(""))
        assertThat(c.selection, equalTo(TextSelection.collapsed(-1)))
    }

    @Test
    fun `constructor with empty string`() {
        val c = TextEditingController("")
        assertThat(c.text, equalTo(""))
        assertThat(c.selection, equalTo(TextSelection.collapsed(-1)))
    }

    @Test
    fun `constructor with string`() {
        val c = TextEditingController("foo")
        assertThat(c.text, equalTo("foo"))
        assertThat(c.selection, equalTo(TextSelection.collapsed(-1)))
    }

    @Test
    fun `constructor with null text editing value`() {
        val c = TextEditingController(null as TextEditingValue?)
        assertThat(c.text, equalTo(""))
        assertThat(c.selection, equalTo(TextSelection.collapsed(-1)))
    }

    @Test
    fun `constructor with text editing value`() {
        val v = TextEditingValue(
            text = "foo",
            selection = TextSelection(0, 1),
            composing = TextRange(1, 2)
        )
        val c = TextEditingController(v)
        assertThat(c.text, equalTo("foo"))
        assertThat(c.selection, equalTo(TextSelection(0, 1)))
    }

    @Test
    fun `setText notify event`() {
        val callback = mock(CallbackMockHelper::class.java)
        val c = TextEditingController("foo")
        c.addListener(callback)
        c.text = "bar"
        verify(callback, times(1)).invoke()
        assertThat(c.text, equalTo("bar"))
    }

    @Test
    fun `setSelection notify event`() {
        val callback = mock(CallbackMockHelper::class.java)
        val c = TextEditingController("foo")
        c.addListener(callback)
        c.selection = TextSelection(2, 3)
        verify(callback, times(1)).invoke()
        assertThat(c.selection, equalTo(TextSelection(2, 3)))
    }

    @Test
    fun `clear`() {
        val callback = mock(CallbackMockHelper::class.java)
        val c = TextEditingController(
            TextEditingValue(
                text = "foo",
                selection = TextSelection(0, 1),
                composing = TextRange(1, 2)
            )
        )
        c.addListener(callback)
        c.clear()
        verify(callback, times(1)).invoke()
        assertThat(c.text, equalTo(""))
        assertThat(c.selection, equalTo(TextSelection.collapsed(-1)))
    }

    @Test
    fun `clearComposing`() {
        val callback = mock(CallbackMockHelper::class.java)
        val c = TextEditingController(
            TextEditingValue(
                text = "foo",
                selection = TextSelection(0, 1),
                composing = TextRange(1, 2)
            )
        )
        c.addListener(callback)
        c.clearComposing()
        verify(callback, times(1)).invoke()
        assertThat(c.text, equalTo("foo"))
        assertThat(c.selection, equalTo(TextSelection(0, 1)))
        assertThat(c.value.composing, equalTo(TextRange.empty))
    }
}
