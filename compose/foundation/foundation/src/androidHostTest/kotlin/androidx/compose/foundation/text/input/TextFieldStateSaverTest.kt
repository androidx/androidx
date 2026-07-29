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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.internal.commitText
import androidx.compose.foundation.text.input.internal.withImeScope
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import org.junit.Test

@OptIn(ExperimentalFoundationApi::class)
class TextFieldStateSaverTest {

    @Test
    fun savesAndRestoresTextAndSelection() {
        val state = TextFieldState("hello, world", initialSelection = TextRange(0, 5))

        val saved = with(TextFieldState.Saver) { TestSaverScope.save(state) }
        assertNotNull(saved)
        val restoredState = TextFieldState.Saver.restore(saved)

        assertNotNull(restoredState)
        assertThat(restoredState.text.toString()).isEqualTo("hello, world")
        assertThat(restoredState.selection).isEqualTo(TextRange(0, 5))
        assertThat(restoredState.value.textFieldTextStyles).isNull()
    }

    @Test
    fun savesAndRestoresUndo() {
        val state = TextFieldState("hello, world", initialSelection = TextRange(0, 5))

        state.withImeScope { commitText("hi", 1) }

        val saved = with(TextFieldState.Saver) { TestSaverScope.save(state) }
        assertNotNull(saved)
        val restoredState = TextFieldState.Saver.restore(saved)

        assertNotNull(restoredState)
        assertThat(restoredState.text.toString()).isEqualTo("hi, world")
        assertThat(restoredState.undoState.canUndo).isTrue()
        restoredState.undoState.undo()
        assertThat(restoredState.text.toString()).isEqualTo("hello, world")
        assertThat(restoredState.selection).isEqualTo(TextRange(0, 5))
        assertThat(restoredState.value.textFieldTextStyles).isNull()
    }

    @Test
    fun savesAndRestoresStyles() {
        val state = TextFieldState("hello, world")
        state.edit {
            addStyle(SpanStyle(color = Color.Red), TextRange(0, 5), ExpandPolicy.InsideOnly)
            addStyle(SpanStyle(color = Color.Blue), TextRange(7, 12), ExpandPolicy.AtBoth)
        }

        val saved = with(TextFieldState.Saver) { TestSaverScope.save(state) }
        assertNotNull(saved)
        val restoredState = TextFieldState.Saver.restore(saved)

        assertNotNull(restoredState)
        assertThat(restoredState.text.toString()).isEqualTo("hello, world")

        val styles = restoredState.value.textFieldTextStyles
        assertNotNull(styles)

        val spanStyles = styles.getSpanStyles(TextRange(0, 12))
        assertThat(spanStyles).hasSize(2)

        assertThat(spanStyles[0].item.color).isEqualTo(Color.Red)
        assertThat(spanStyles[0].start).isEqualTo(0)
        assertThat(spanStyles[0].end).isEqualTo(5)

        assertThat(spanStyles[1].item.color).isEqualTo(Color.Blue)
        assertThat(spanStyles[1].start).isEqualTo(7)
        assertThat(spanStyles[1].end).isEqualTo(12)

        // Verify ExpandPolicy of restored styles
        restoredState.edit {
            // Insert at 0 (start of Style 1). None -> should not expand.
            replace(0, 0, "x") // "xhello, world"

            // Insert at 5+1 = 6 (end of Style 1). None -> should not expand.
            replace(6, 6, "y") // "xhello,y world"

            // Style 2 original [7, 12] is now at [9, 14]
            // Insert at 9 (start of Style 2). Both -> should expand.
            replace(9, 9, "w") // "xhello,yw world"

            // Insert at 15 (end of Style 2). Both -> should expand.
            replace(15, 15, "d") // "xhello,yw worldd"
        }

        val finalStyles = restoredState.value.textFieldTextStyles!!
        val finalSpanStyles = finalStyles.getSpanStyles(TextRange(0, restoredState.text.length))

        assertThat(finalSpanStyles).hasSize(2)

        // Style 1 should be shifted but not expanded
        assertThat(finalSpanStyles[0].start).isEqualTo(1)
        assertThat(finalSpanStyles[0].end).isEqualTo(6)

        // Style 2 should be expanded
        assertThat(finalSpanStyles[1].start).isEqualTo(9)
        assertThat(finalSpanStyles[1].end).isEqualTo(16)
    }

    private object TestSaverScope : SaverScope {
        override fun canBeSaved(value: Any): Boolean = true
    }
}
