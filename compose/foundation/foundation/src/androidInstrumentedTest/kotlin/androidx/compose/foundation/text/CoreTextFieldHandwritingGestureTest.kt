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

package androidx.compose.foundation.text

import android.graphics.RectF
import android.view.inputmethod.DeleteGesture
import android.view.inputmethod.DeleteRangeGesture
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.SelectGesture
import android.view.inputmethod.SelectRangeGesture
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.setFocusableContent
import androidx.compose.foundation.text.input.InputMethodInterceptor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(34)
@SdkSuppress(minSdkVersion = 34)
class CoreTextFieldHandwritingGestureTest {
    @get:Rule
    val rule = createComposeRule()
    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "CoreTextField"

    @Test
    fun textField_selectGesture_wordLevel() {
        val text = "abc def ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("b")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(text.rangeOf("abc"))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun textField_selectGesture_characterLevel() {
        val text = "abcdef"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("bc")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(text.rangeOf("bc"))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun textField_selectGesture_characterLevel_noSelection_insertFallbackText() {
        val text = "abcdef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { _ ->
                SelectGesture.Builder()
                    .setSelectionArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_selectGesture_characterLevel_noSelection_fail() {
        val text = "abcdef"
        testHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                SelectGesture.Builder()
                    .setSelectionArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(text.length))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_selectGesture_wordLevel_noSelection_insertFallbackText() {
        val text = "abc def ghi"
        val fallback = "fallback"
        val initialCursor = 5
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("a")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)

            assertThat(textFieldValue.text).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_selectGesture_wordLevel_noSelectionNoFallbackText_fail() {
        val text = "abc def ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("a")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(text.length))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_removeSpaceBeforeDeletion() {
        val text = "abc def ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space after "def" is removed.
            val expectedText = "abc def"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(expectedText.length))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_onlyRemoveSpaceBeforeDeletion() {
        val text = "abc\n def ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("e")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space before "def" is removed the space after "def" is not removed.
            // Cursor is placed after "\n"
            val expectedText = "abc\n ghi"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(4))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_removeSpaceAfterDeletion() {
        val text = "abc def ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("b")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space before "def" is also removed
            val expectedText = "def ghi"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(0))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_endWithPunctuation_removeSpaceBeforeDeletion() {
        val text = "abc def ghi!"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space before "!" is removed
            val expectedText = "abc def!"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(expectedText.length - 1))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_characterLevel() {
        val text = "abcdefghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("def")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // "def" is removed and cursor is placed before 'g'
            val expectedText = "abcghi"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(expectedText.indexOf('g')))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_characterLevel_notRemoveSpaces() {
        val text = "abcdef ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("def")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // "def" is removed and cursor is placed before ' ', when the delete is character level
            // it won't remove spaces before or after the deleted range.
            val expectedText = "abc ghi"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(expectedText.indexOf(' ')))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_noDeletion_insertFallbackText() {
        val text = "abc def ghi"
        val fallback = "fallback"
        val initialCursor = 5
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { _ ->
                DeleteGesture.Builder()
                    .setDeletionArea(RectF(-1f, -1f, 0f, 0f))
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)

            assertThat(textFieldValue.text).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)

            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteGesture_noDeletionNoFallbackText_fail() {
        val text = "abc def ghi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                DeleteGesture.Builder()
                    .setDeletionArea(RectF(-1f, -1f, 0f, 0f))
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(text.length))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    fun textField_selectRangeGesture_characterLevel() {
        val text = "abc\ndef"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea = textLayoutResult.boundingBoxOf("c").let {
                    localToScreen(it).toAndroidRectF()
                }

                val endArea = textLayoutResult.boundingBoxOf("d").let {
                    localToScreen(it).toAndroidRectF()
                }

                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(text.rangeOf("c\nd"))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun textField_selectRangeGesture_wordLevel() {
        val text = "abc\ndef jhi"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea = textLayoutResult.boundingBoxOf("b").let {
                    localToScreen(it).toAndroidRectF()
                }

                val endArea = textLayoutResult.boundingBoxOf("e").let {
                    localToScreen(it).toAndroidRectF()
                }

                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(text.rangeOf("abc\ndef"))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Shown)
        }
    }

    @Test
    fun textField_selectRangeGesture_nothingSelectedInStartArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val endArea = textLayoutResult.boundingBoxOf("d").let {
                    localToScreen(it).toAndroidRectF()
                }
                // The startArea selects nothing, but the endArea contains one character, it
                // should still fallback.
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_selectRangeGesture_nothingSelectedInEndArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val startArea = textLayoutResult.boundingBoxOf("c").let {
                    localToScreen(it).toAndroidRectF()
                }
                // The endArea selects nothing, but the start contains one character, it
                // should still fallback.
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_selectRangeGesture_noSelection_fail() {
        val text = "abcdef"
        testHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setSelectionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(text.length))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteRangeGesture_characterLevel() {
        val text = "abc\ndef"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea = textLayoutResult.boundingBoxOf("c").let {
                    localToScreen(it).toAndroidRectF()
                }
                val endArea = textLayoutResult.boundingBoxOf("d").let {
                    localToScreen(it).toAndroidRectF()
                }

                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abef"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            // Cursor is placed before 'e'
            assertThat(textFieldValue.selection).isEqualTo(TextRange(expectedText.indexOf('e')))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteRangeGesture_wordLevel() {
        val text = "abc def\n jhi lmn"
        testHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea = textLayoutResult.boundingBoxOf("e").let {
                    localToScreen(it).toAndroidRectF()
                }
                val endArea = textLayoutResult.boundingBoxOf("h").let {
                    localToScreen(it).toAndroidRectF()
                }

                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abc lmn"
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(expectedText.indexOf(' ')))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteRangeGesture_nothingDeletedInStartArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val endArea = textLayoutResult.boundingBoxOf("d").let {
                    localToScreen(it).toAndroidRectF()
                }
                // The startArea selects nothing, but the endArea contains one character, it
                // should still fallback.
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteRangeGesture_nothingDeletedInEndArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val startArea = textLayoutResult.boundingBoxOf("c").let {
                    localToScreen(it).toAndroidRectF()
                }
                // The endArea selects nothing, but the start contains one character, it
                // should still fallback.
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldValue.text).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldValue.selection).isEqualTo(expectedSelection)
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    @Test
    fun textField_deleteRangeGesture_noDeletion_fail() {
        val text = "abcdef"
        testHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setDeletionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldValue, resultCode, textToolbar ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldValue.text).isEqualTo(text)
            assertThat(textFieldValue.selection).isEqualTo(TextRange(text.length))
            assertThat(textToolbar.status).isEqualTo(TextToolbarStatus.Hidden)
        }
    }

    private fun testHandwritingGesture(
        text: String,
        initialSelection: TextRange = TextRange(text.length),
        gestureFactory: LayoutCoordinates.(TextLayoutResult) -> HandwritingGesture,
        assertion: (TextFieldValue, resultCode: Int, TextToolbar) -> Unit
    ) {
        var textFieldValue by mutableStateOf(TextFieldValue(text, initialSelection))
        var textLayoutResult: TextLayoutResult? = null
        var layoutCoordinates: LayoutCoordinates? = null
        val textToolbar = FakeTextToolbar()

        setContent {
            CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                CoreTextField(
                    value = textFieldValue,
                    onValueChange = { textFieldValue = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(Tag)
                        .onGloballyPositioned { layoutCoordinates = it },
                    onTextLayout = {
                        textLayoutResult = it
                    }
                )
            }
        }
        rule.onNodeWithTag(Tag).requestFocus()
        rule.waitForIdle()

        val gesture = gestureFactory.invoke(layoutCoordinates!!, textLayoutResult!!)
        var resultCode = InputConnection.HANDWRITING_GESTURE_RESULT_UNKNOWN

        inputMethodInterceptor.withInputConnection {
            performHandwritingGesture(gesture, /* executor= */null) { resultCode = it }
        }

        rule.runOnIdle {
            assertion.invoke(textFieldValue, resultCode, textToolbar)
        }
    }

    private fun setContent(
        extraItemForInitialFocus: Boolean = true,
        content: @Composable () -> Unit
    ) {
        rule.setFocusableContent(extraItemForInitialFocus) {
            inputMethodInterceptor.Content {
                content()
            }
        }
    }

    private fun LayoutCoordinates.localToScreen(rect: Rect): Rect {
        val localOriginInScreen = localToScreen(Offset.Zero)
        return rect.translate(localOriginInScreen)
    }

    private fun FakeTextToolbar(): TextToolbar {
        return object : TextToolbar {
            private var _status: TextToolbarStatus = TextToolbarStatus.Hidden

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?
            ) {
                _status = TextToolbarStatus.Shown
            }

            override fun hide() {
                _status = TextToolbarStatus.Hidden
            }

            override val status: TextToolbarStatus
                get() = _status
        }
    }
}
