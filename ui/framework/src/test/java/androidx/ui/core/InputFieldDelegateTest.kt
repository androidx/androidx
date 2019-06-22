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

package androidx.ui.core

import androidx.ui.graphics.Color
import androidx.ui.input.EditorState
import androidx.ui.painting.Canvas
import androidx.ui.painting.TextPainter
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InputFieldDelegateTest {
    @Test
    fun draw_selection_test() {
        val painter = mock<TextPainter>()
        val canvas = mock<Canvas>()
        val delegate = InputFieldDelegate(painter)

        val selection = TextRange(0, 1)
        val selectionColor = Color.Blue

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = selection),
            editorStyle = EditorStyle(selectionColor = selectionColor))

        verify(painter, times(1)).paintBackground(
            eq(selection.start), eq(selection.end), eq(selectionColor), eq(canvas), any())
        verify(painter, times(1)).paint(eq(canvas), any())

        verify(painter, never()).paintCursor(any(), any())
    }

    @Test
    fun draw_cursor_test() {
        val painter = mock<TextPainter>()
        val canvas = mock<Canvas>()
        val delegate = InputFieldDelegate(painter)

        val cursor = TextRange(1, 1)

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = cursor),
            editorStyle = EditorStyle())

        verify(painter, times(1)).paintCursor(eq(cursor.start), eq(canvas))
        verify(painter, times(1)).paint(eq(canvas), any())
        verify(painter, never()).paintBackground(any(), any(), any(), any(), any())
    }

    @Test
    fun draw_composition_test() {
        val painter = mock<TextPainter>()
        val canvas = mock<Canvas>()
        val delegate = InputFieldDelegate(painter)

        val composition = TextRange(0, 1)
        val compositionColor = Color.Red

        val cursor = TextRange(1, 1)

        delegate.draw(
            canvas = canvas,
            value = EditorState(text = "Hello, World", selection = cursor,
                composition = composition),
            editorStyle = EditorStyle(compositionColor = compositionColor))

        verify(painter, times(1)).paintBackground(
            eq(composition.start), eq(composition.end), eq(compositionColor), eq(canvas), any())
        verify(painter, times(1)).paint(eq(canvas), any())
        verify(painter, times(1)).paintCursor(eq(cursor.start), any())
    }
}