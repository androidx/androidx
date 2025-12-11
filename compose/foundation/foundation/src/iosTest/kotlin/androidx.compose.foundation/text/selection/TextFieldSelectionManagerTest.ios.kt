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

package androidx.compose.foundation.text.selection

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.experimental.ExperimentalNativeApi
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalNativeApi::class)
class TextFieldSelectionManagerTest {
    @Test
    fun isSelectionHandleIsVisible_ltr_text() {
        // Text = "I am TextField"
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(94.58f, 50.0f),
            50.0f,
            Rect(Offset(0f, 0f), Offset(744.0f, 50.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(272.38f, 50.0f),
            50.0f,
            Rect(Offset(0f, 0f), Offset(744.0f, 50.0f))
        )
        assertTrue(isStartVisible)
        assertTrue(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_rtl_text() {
        // Text = "أنا حقل النص"
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(153.84f, 56.0f),
            54.0f,
            Rect(Offset(0f, 0f), Offset(744.0f, 54.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(92.93f, 56.0f),
            54.0f,
            Rect(Offset(0f, 0f), Offset(744.0f, 54.0f))
        )
        assertTrue(isStartVisible)
        assertTrue(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_bidi_text() {
        // Text = "I am TextField أنا حقل النص"
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(365.31f, 56.0f),
            54.0f,
            Rect(Offset(0f, 0f), Offset(744.0f, 54.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(284.2f, 56.0f),
            54.0f,
            Rect(Offset(0f, 0f), Offset(744.0f, 54.0f))
        )
        assertTrue(isStartVisible)
        assertTrue(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_ltr_text_not_visible() {
        // Text = "I am TextField"
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(94.58f, 50.0f),
            50.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(272.38f, 50.0f),
            50.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        assertFalse(isStartVisible)
        assertFalse(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_rtl_text_not_visible() {
        // Text = "أنا حقل النص"
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(153.84f, 56.0f),
            54.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(92.93f, 56.0f),
            54.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        assertFalse(isStartVisible)
        assertFalse(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_bidi_text_not_visible() {
        // Text = "I am TextField أنا حقل النص"
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(365.31f, 56.0f),
            54.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(284.2f, 56.0f),
            54.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        assertFalse(isStartVisible)
        assertFalse(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_start_is_invisible_end_is_visible() {
        /* Selected text in multiline scrollable textfield, start handle is located above then visible bounds */
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(94.79f, 250.0f),
            50.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(164.29f, 250.0f),
            50.0f,
            Rect(Offset(0f, 217.0f), Offset(1194.0f, 2230.0f))
        )
        assertFalse(isStartVisible)
        assertTrue(isEndVisible)
    }

    @Test
    fun isSelectionHandleIsVisible_start_is_visible_end_is_invisible() {
        /* Selected text in multiline scrollable textfield, end handle is located beneath then visible bounds */
        val isStartVisible = isSelectionHandleIsVisible(
            true,
            Offset(0.0f, 2250.0f),
            50.0f,
            Rect(Offset(0f, 211.0f), Offset(1194.0f, 2224.0f))
        )
        val isEndVisible = isSelectionHandleIsVisible(
            false,
            Offset(82.97f, 2250.0f),
            54.0f,
            Rect(Offset(0f, 211.0f), Offset(744.0f, 54.0f))
        )
        assertTrue(isStartVisible)
        assertFalse(isEndVisible)
    }
}