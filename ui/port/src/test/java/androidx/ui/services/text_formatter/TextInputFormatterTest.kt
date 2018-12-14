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

package androidx.ui.services.text_formatter

import androidx.ui.services.text_input.TextEditingValue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(JUnit4::class)
class TextInputFormatterTest {

    open class FunctionMockHelper : TextInputFormatFunction {
        override fun invoke(old: TextEditingValue, new: TextEditingValue): TextEditingValue {
            return new
        }
    }

    @Test
    fun `given function is called`() {
        val func = mock(FunctionMockHelper::class.java)
        val formatter = TextInputFormatter.withFunction(func)
        val oldValue = TextEditingValue(text = "foo")
        val newValue = TextEditingValue(text = "bar")
        formatter.formatEditUpdate(oldValue, newValue)
        verify(func, times(1)).invoke(oldValue, newValue)
    }
}