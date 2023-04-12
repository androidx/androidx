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

package androidx.compose.foundation.text2.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class TextFieldStateSaverTest {

    @Test
    fun savesAndRestoresTextAndSelection() {
        val state = TextFieldState("hello, world", initialSelectionInChars = TextRange(0, 5))

        val saved = with(TextFieldState.Saver) { TestSaverScope.save(state) }
        assertNotNull(saved)
        val restoredState = TextFieldState.Saver.restore(saved)

        assertNotNull(restoredState)
        assertThat(restoredState.text.toString()).isEqualTo("hello, world")
        assertThat(restoredState.text.selectionInChars).isEqualTo(TextRange(0, 5))
    }

    private object TestSaverScope : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
}