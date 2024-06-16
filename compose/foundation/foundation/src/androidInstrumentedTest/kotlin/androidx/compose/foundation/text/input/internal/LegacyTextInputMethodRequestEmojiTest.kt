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
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.emoji2.text.EmojiCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class LegacyTextInputMethodRequestEmojiTest {
    @After
    fun cleanup() {
        EmojiCompat.reset(null)
    }

    @Test
    fun whenEmojiCompat_addsEditorInfo() = runTest {
        val emojiCompat = mock<EmojiCompat>()
        EmojiCompat.reset(emojiCompat)
        val textInputService =
            LegacyTextInputMethodRequest(
                view = View(getInstrumentation().targetContext),
                localToScreen = {},
                inputMethodManager = mock<InputMethodManager>()
            )
        textInputService.startInput(TextFieldValue(""), null, ImeOptions.Default, {}, {})

        val info = EditorInfo()
        textInputService.createInputConnection(info)

        verify(emojiCompat).updateEditorInfo(eq(info))
    }
}
