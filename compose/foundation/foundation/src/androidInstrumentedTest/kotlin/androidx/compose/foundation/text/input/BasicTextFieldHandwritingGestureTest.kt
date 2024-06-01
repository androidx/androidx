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

import android.graphics.RectF
import android.os.CancellationSignal
import android.view.inputmethod.DeleteGesture
import android.view.inputmethod.DeleteRangeGesture
import android.view.inputmethod.HandwritingGesture
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InsertGesture
import android.view.inputmethod.JoinOrSplitGesture
import android.view.inputmethod.PreviewableHandwritingGesture
import android.view.inputmethod.RemoveSpaceGesture
import android.view.inputmethod.SelectGesture
import android.view.inputmethod.SelectRangeGesture
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TEST_FONT_FAMILY
import androidx.compose.foundation.text.boundingBoxOf
import androidx.compose.foundation.text.insert
import androidx.compose.foundation.text.rangeOf
import androidx.compose.foundation.text.toPointF
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@RequiresApi(34)
@SdkSuppress(minSdkVersion = 34)
internal class BasicTextFieldHandwritingGestureTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val immRule = ComposeInputMethodManagerTestRule()

    private val inputMethodInterceptor = InputMethodInterceptor(rule)

    private val Tag = "BasicTextField2"

    private val lineMargin = 16f

    private val cancellationSignal = CancellationSignal()

    @Test
    fun textField_selectGesture_wordLevel() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("b")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(text.rangeOf("abc"))
        }
    }

    @Test
    fun textField_selectGesture_preview_wordLevel() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("b")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingSelectPreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("abc"))

            cancellationSignal.cancel()
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_selectGesture_characterLevel() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("bc")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(text.rangeOf("bc"))
        }
    }

    @Test
    fun textField_selectGesture_preview_characterLevel() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("bc")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingSelectPreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("bc"))
        }
    }

    @Test
    fun textField_selectGesture_characterLevel_noSelection_insertFallbackText() {
        val text = "abcdef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { _ ->
                SelectGesture.Builder()
                    .setSelectionArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_selectGesture_characterLevel_noSelection_fail() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                SelectGesture.Builder()
                    .setSelectionArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_selectGesture_preview_characterLevel_noSelection() {
        val text = "abcdef"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { _ ->
                SelectGesture.Builder()
                    .setSelectionArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(initialCursor))
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_selectGesture_wordLevel_noSelection_insertFallbackText() {
        val text = "abc def ghi"
        val fallback = "fallback"
        val initialCursor = 5
        testTextFieldHandwritingGesture(
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
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)

            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_selectGesture_wordLevel_noSelectionNoFallbackText_fail() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("a")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_selectGesture_preview_wordLevel_noSelection() {
        val text = "abc def ghi"
        val initialCursor = 5
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("a")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(initialCursor))
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_selectGesture_withOutputTransformation() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            outputTransformation = {
                // The visual text is "abcxxxxxx def ghi"
                replace(3, 3, "xxxxxx")
            },
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            // "ghi" is selected.
            assertThat(textFieldState.selection).isEqualTo(text.rangeOf("ghi"))
        }
    }

    @Test
    fun textField_selectGesture_preview_withOutputTransformation() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            outputTransformation = {
                // The visual text is "abcxxxxxx def ghi"
                replace(3, 3, "xxxxxx")
            },
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                SelectGesture.Builder()
                    .setSelectionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingSelectPreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("ghi"))
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_removeSpaceBeforeDeletion() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space after "def" is removed.
            val expectedText = "abc def"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.length))
        }
    }

    @Test
    fun textField_deleteGesture_preview_wordLevel() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingDeletePreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("ghi"))

            cancellationSignal.cancel()
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_onlyRemoveSpaceBeforeDeletion() {
        val text = "abc\n def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("e")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space before "def" is removed the space after "def" is not removed.
            // Cursor is placed after "\n"
            val expectedText = "abc\n ghi"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(4))
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_removeSpaceAfterDeletion() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("b")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space before "def" is also removed
            val expectedText = "def ghi"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(0))
        }
    }

    @Test
    fun textField_deleteGesture_wordLevel_endWithPunctuation_removeSpaceBeforeDeletion() {
        val text = "abc def ghi!"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // The space before "!" is removed
            val expectedText = "abc def!"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.length - 1))
        }
    }

    @Test
    fun textField_deleteGesture_characterLevel() {
        val text = "abcdefghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("def")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // "def" is removed and cursor is placed before 'g'
            val expectedText = "abcghi"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf('g')))
        }
    }

    @Test
    fun textField_deleteGesture_preview_characterLevel() {
        val text = "abcdefghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("def")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingDeletePreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("def"))
        }
    }

    @Test
    fun textField_deleteGesture_characterLevel_notRemoveSpaces() {
        val text = "abcdef ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("def")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // "def" is removed and cursor is placed before ' ', when the delete is character level
            // it won't remove spaces before or after the deleted range.
            val expectedText = "abc ghi"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf(' ')))
        }
    }

    @Test
    fun textField_deleteGesture_noDeletion_insertFallbackText() {
        val text = "abc def ghi"
        val fallback = "fallback"
        val initialCursor = 5
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { _ ->
                DeleteGesture.Builder()
                    .setDeletionArea(RectF(-1f, -1f, 0f, 0f))
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)

            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_deleteGesture_noDeletionNoFallbackText_fail() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                DeleteGesture.Builder()
                    .setDeletionArea(RectF(-1f, -1f, 0f, 0f))
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_deleteGesture_preview_noDeletion() {
        val text = "abc def ghi"
        val initialCursor = 5
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { _ ->
                DeleteGesture.Builder()
                    .setDeletionArea(RectF(-1f, -1f, 0f, 0f))
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(initialCursor))
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_deleteGesture_withOutputTransformation() {
        val text = "abc def ghi"
        testTextFieldHandwritingGesture(
            text = text,
            outputTransformation = {
                // The visual text is "abcxxxxxx def ghi"
                replace(3, 3, "xxxxxx")
            },
            gestureFactory = { textLayoutResult ->
                val localBoundingBox = textLayoutResult.boundingBoxOf("h")
                val screenBoundingBox = localToScreen(localBoundingBox).toAndroidRectF()
                DeleteGesture.Builder()
                    .setDeletionArea(screenBoundingBox)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            // "ghi" is deleted
            val expectedText = "abc def"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.length))
        }
    }

    fun textField_selectRangeGesture_characterLevel() {
        val text = "abc\ndef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("c").let { localToScreen(it).toAndroidRectF() }

                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }

                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(text.rangeOf("c\nd"))
        }
    }

    fun textField_selectRangeGesture_preview_characterLevel() {
        val text = "abc\ndef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("c").let { localToScreen(it).toAndroidRectF() }

                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }

                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingSelectPreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("c\nd"))
        }
    }

    @Test
    fun textField_selectRangeGesture_wordLevel() {
        val text = "abc\ndef jhi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("b").let { localToScreen(it).toAndroidRectF() }

                val endArea =
                    textLayoutResult.boundingBoxOf("e").let { localToScreen(it).toAndroidRectF() }

                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(text.rangeOf("abc\ndef"))
        }
    }

    @Test
    fun textField_selectRangeGesture_preview_wordLevel() {
        val text = "abc\ndef jhi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("b").let { localToScreen(it).toAndroidRectF() }

                val endArea =
                    textLayoutResult.boundingBoxOf("e").let { localToScreen(it).toAndroidRectF() }

                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingSelectPreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("abc\ndef"))
        }
    }

    @Test
    fun textField_selectRangeGesture_nothingSelectedInStartArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }
                // The startArea selects nothing, but the endArea contains one character, it
                // should still fallback.
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_selectRangeGesture_nothingSelectedInEndArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("c").let { localToScreen(it).toAndroidRectF() }
                // The endArea selects nothing, but the start contains one character, it
                // should still fallback.
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(startArea)
                    .setSelectionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_selectRangeGesture_preview_nothingSelectedInStartArea() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }
                // The startArea selects nothing, but the endArea contains one character, it
                // should still fallback.
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setSelectionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(initialCursor))
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_selectRangeGesture_noSelection_fail() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                SelectRangeGesture.Builder()
                    .setSelectionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setSelectionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_deleteRangeGesture_characterLevel() {
        val text = "abc\ndef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("c").let { localToScreen(it).toAndroidRectF() }
                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }

                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            // Cursor is placed before 'e'
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf('e')))
        }
    }

    @Test
    fun textField_deleteRangeGesture_preview_characterLevel() {
        val text = "abc\ndef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("c").let { localToScreen(it).toAndroidRectF() }
                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }

                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingDeletePreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("c\nd"))
        }
    }

    @Test
    fun textField_deleteRangeGesture_wordLevel() {
        val text = "abc def\n jhi lmn"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("e").let { localToScreen(it).toAndroidRectF() }
                val endArea =
                    textLayoutResult.boundingBoxOf("h").let { localToScreen(it).toAndroidRectF() }

                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abc lmn"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf(' ')))
        }
    }

    @Test
    fun textField_deleteRangeGesture_preview_wordLevel() {
        val text = "abc def\n jhi lmn"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("e").let { localToScreen(it).toAndroidRectF() }
                val endArea =
                    textLayoutResult.boundingBoxOf("h").let { localToScreen(it).toAndroidRectF() }

                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_WORD)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
            assertThat(textFieldState.value.highlight?.first)
                .isEqualTo(TextHighlightType.HandwritingDeletePreview)
            assertThat(textFieldState.value.highlight?.second).isEqualTo(text.rangeOf("def\n jhi"))
        }
    }

    @Test
    fun textField_deleteRangeGesture_nothingDeletedInStartArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }
                // The startArea selects nothing, but the endArea contains one character, it
                // should still fallback.
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_deleteRangeGesture_nothingDeletedInEndArea_insertFallbackText() {
        val text = "abc\ndef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val startArea =
                    textLayoutResult.boundingBoxOf("c").let { localToScreen(it).toAndroidRectF() }
                // The endArea selects nothing, but the start contains one character, it
                // should still fallback.
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(startArea)
                    .setDeletionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_deleteRangeGesture_preview_nothingDeletedInStartArea() {
        val text = "abc\ndef"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                val endArea =
                    textLayoutResult.boundingBoxOf("d").let { localToScreen(it).toAndroidRectF() }
                // The startArea selects nothing, but the endArea contains one character, it
                // should still fallback.
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setDeletionEndArea(endArea)
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            },
            preview = true
        ) { textFieldState, _ ->
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(initialCursor))
            assertThat(textFieldState.value.highlight).isNull()
        }
    }

    @Test
    fun textField_deleteRangeGesture_noDeletion_fail() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { _ ->
                DeleteRangeGesture.Builder()
                    .setDeletionStartArea(RectF(0f, 0f, 1f, 1f))
                    .setDeletionEndArea(RectF(0f, 0f, 1f, 1f))
                    .setGranularity(HandwritingGesture.GRANULARITY_CHARACTER)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_insertSpace() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture before 'd'.
                val point = textLayoutResult.boundingBoxOf("d").centerLeft
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abc def"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf("d")))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_bidiBoundary_insertFallbackText() {
        val text = "abc\u05D0\u05D1\u05D2"
        val fallback = "fallback"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                // Perform the gesture after 'c'.
                val point = textLayoutResult.boundingBoxOf("c").centerRight
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder()
                    .setJoinOrSplitPoint(screenPoint)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)

            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_joinOrSplitGesture_bidiBoundary_fail() {
        val text = "abc\u05D0\u05D1\u05D2"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture after 'c'.
                val point = textLayoutResult.boundingBoxOf("c").centerRight
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_removeSpaceBeforeGesture() {
        val text = "abc def"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture before 'd'.
                val point = textLayoutResult.boundingBoxOf("d").centerLeft
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcdef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf("d")))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_removeSpaceAfterGesture() {
        val text = "abc def"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture after 'd'.
                val point = textLayoutResult.boundingBoxOf("c").centerRight
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcdef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf("d")))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_removeMultipleSpacesBeforeGesture() {
        val text = "abc   def"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture before 'd'.
                val point = textLayoutResult.boundingBoxOf("d").centerLeft
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcdef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf("d")))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_removeMultipleSpacesAfterGesture() {
        val text = "abc   def"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture before 'd'.
                val point = textLayoutResult.boundingBoxOf("c").centerRight
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcdef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf("d")))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_removeSurroundingSpaces() {
        val text = "abc   def"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture in the middle of the spaces.
                val point = textLayoutResult.boundingBoxOf("   ").center
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcdef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf("d")))
        }
    }

    @Test
    fun textField_joinOrSplitGesture_outOfLineMargin_insertFallbackText() {
        val text = "abcdef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                // Perform the gesture 20 pixels above the line.
                val point =
                    textLayoutResult.boundingBoxOf("d").topLeft.let {
                        Offset(it.x, it.y - lineMargin - 1)
                    }
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder()
                    .setJoinOrSplitPoint(screenPoint)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)

            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_joinOrSplitGesture_outOfLineMargin_fail() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture 20 pixels above the line.
                val point =
                    textLayoutResult.boundingBoxOf("d").topLeft.let {
                        Offset(it.x, it.y - lineMargin - 1)
                    }
                val screenPoint = localToScreen(point).toPointF()
                JoinOrSplitGesture.Builder().setJoinOrSplitPoint(screenPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_insertGesture() {
        val text = "abcdef"
        val textToInsert = "xxx"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Text is inserted before character 'b'
                val point = textLayoutResult.boundingBoxOf("b").centerLeft
                val screenPoint = localToScreen(point).toPointF()
                InsertGesture.Builder()
                    .setInsertionPoint(screenPoint)
                    .setTextToInsert(textToInsert)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "axxxbcdef"
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            // Cursor is placed before 'b'
            assertThat(textFieldState.selection).isEqualTo(TextRange(expectedText.indexOf('b')))
        }
    }

    @Test
    fun textField_insertGesture_outOfLineMargin_insertFallbackText() {
        val text = "abcdef"
        val fallback = "fallbackText"
        val initialCursor = 3
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                // Perform the gesture 20 pixels above the line.
                val point =
                    textLayoutResult.boundingBoxOf("d").topLeft.let {
                        Offset(it.x, it.y - lineMargin - 1)
                    }
                val screenPoint = localToScreen(point).toPointF()
                InsertGesture.Builder()
                    .setInsertionPoint(screenPoint)
                    .setTextToInsert("")
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)

            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text.toString()).isEqualTo(expectedText)
            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_insertGesture_outOfLineMargin_fail() {
        val text = "abcdef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // Perform the gesture 20 pixels above the line.
                val point =
                    textLayoutResult.boundingBoxOf("d").topLeft.let {
                        Offset(it.x, it.y - lineMargin - 1)
                    }
                val screenPoint = localToScreen(point).toPointF()
                InsertGesture.Builder().setInsertionPoint(screenPoint).setTextToInsert("").build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text.toString()).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_removeSpaceGesture() {
        val text = "ab cd ef gh"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // The given gestures should remove spaces within the range of "cd ef".
                val startPoint =
                    textLayoutResult.boundingBoxOf("c").centerLeft.let {
                        localToScreen(it).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("f").centerRight.let {
                        localToScreen(it).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "ab cdef gh"
            assertThat(textFieldState.text).isEqualTo(expectedText)
            // The cursor should be placed before 'e', the offset where the space is removed.
            val expectedSelection = TextRange(expectedText.indexOf('e'))
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_selectFirstLine() {
        val text = "ab cd ef\ngh ij kl"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // This gestures only works for single line. When the given points touches multiple
                // lines, it should only remove spaces at the first line. In this case, it'll be
                // remove spaces within the range of "b cd e" ('e' is at the top of 'k').
                val startPoint =
                    textLayoutResult.boundingBoxOf("b").centerLeft.let {
                        localToScreen(it).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("k").centerRight.let {
                        localToScreen(it).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcdef\ngh ij kl"
            assertThat(textFieldState.text).isEqualTo(expectedText)
            // The cursor should be placed before 'e', the offset where the last space is removed.
            val expectedSelection = TextRange(expectedText.indexOf('e'))
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_spaceOnly() {
        val text = "ab    cd ef"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // The gesture covers spaces only, between 'b' and 'c'.
                val startPoint =
                    textLayoutResult.boundingBoxOf("b").centerRight.let {
                        localToScreen(it).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("c").centerLeft.let {
                        localToScreen(it).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "abcd ef"
            assertThat(textFieldState.text).isEqualTo(expectedText)
            // The cursor should be placed before 'c'.
            val expectedSelection = TextRange(expectedText.indexOf('c'))
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_noSpaceRemoved_fallback() {
        val text = "ab cdef gi"
        val initialCursor = 3
        val fallback = "fallback"
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                // The gesture covers "cdef" which contains no spaces.
                val startPoint =
                    textLayoutResult.boundingBoxOf("c").centerLeft.let {
                        localToScreen(it).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("f").centerRight.let {
                        localToScreen(it).toPointF()
                    }

                RemoveSpaceGesture.Builder()
                    .setPoints(startPoint, endPoint)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_noSpaceRemoved_fail() {
        val text = "ab cdef gi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // The gesture covers "cdef" which contains no spaces.
                val startPoint =
                    textLayoutResult.boundingBoxOf("c").centerLeft.let {
                        localToScreen(it).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("f").centerRight.let {
                        localToScreen(it).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text).isEqualTo(text)
            // Selection didn't move.
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    @Test
    fun textField_removeSpaceGesture_startPointOutOfLineMargin() {
        val text = "ab cd ef\ngh ij kl"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // The start point is out of line margin. and endPoint is now used to select
                // the target line (the 2nd line).
                // It'll remove the spaces between "h ij k".('h' is under 'b')
                val startPoint =
                    textLayoutResult.boundingBoxOf("b").topLeft.let {
                        val offset = it.copy(y = it.y - lineMargin - 1)
                        localToScreen(offset).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("k").centerRight.let {
                        localToScreen(it).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "ab cd ef\nghijkl"
            assertThat(textFieldState.text).isEqualTo(expectedText)
            // The cursor should be placed before 'k', the offset where the last space is removed.
            val expectedSelection = TextRange(expectedText.indexOf('k'))
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_endPointOutOfLineMargin() {
        val text = "ab cd ef\ngh ij kl"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // The end point is out of line margin. and startPoint is now used to select
                // the target line (the 2nd line).
                // It'll remove the spaces between "h ij k". ('k' is under 'e')
                val startPoint =
                    textLayoutResult.boundingBoxOf("h").centerLeft.let {
                        localToScreen(it).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("e").topRight.let {
                        val offset = it.copy(y = it.y - lineMargin - 1)
                        localToScreen(offset).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_SUCCESS)
            val expectedText = "ab cd ef\nghijkl"
            assertThat(textFieldState.text).isEqualTo(expectedText)
            // The cursor should be placed before 'k', the offset where the last space is removed.
            val expectedSelection = TextRange(expectedText.indexOf('k'))
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_bothStartAndEndPointOutOfLineMargin_fallback() {
        val text = "ab cdef gi"
        val initialCursor = 3
        val fallback = "fallback"
        testTextFieldHandwritingGesture(
            text = text,
            initialSelection = TextRange(initialCursor),
            gestureFactory = { textLayoutResult ->
                // The gesture covers "cdef" which contains no spaces.
                val startPoint =
                    textLayoutResult.boundingBoxOf("c").topLeft.let {
                        val offset = it.copy(y = it.y - lineMargin - 1f)
                        localToScreen(offset).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("f").bottomRight.let {
                        val offset = it.copy(y = it.y + lineMargin + 1f)
                        localToScreen(offset).toPointF()
                    }

                RemoveSpaceGesture.Builder()
                    .setPoints(startPoint, endPoint)
                    .setFallbackText(fallback)
                    .build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FALLBACK)
            val expectedText = text.insert(initialCursor, fallback)
            assertThat(textFieldState.text).isEqualTo(expectedText)

            val expectedSelection = TextRange(initialCursor + fallback.length)
            assertThat(textFieldState.selection).isEqualTo(expectedSelection)
        }
    }

    @Test
    fun textField_removeSpaceGesture_bothStartAndEndPointOutOfLineMargin_fail() {
        val text = "ab cd ef gi"
        testTextFieldHandwritingGesture(
            text = text,
            gestureFactory = { textLayoutResult ->
                // The gesture covers "cdef" which contains no spaces.
                val startPoint =
                    textLayoutResult.boundingBoxOf("c").topLeft.let {
                        val offset = it.copy(y = it.y - lineMargin - 1f)
                        localToScreen(offset).toPointF()
                    }
                val endPoint =
                    textLayoutResult.boundingBoxOf("f").bottomRight.let {
                        val offset = it.copy(y = it.y + lineMargin + 1f)
                        localToScreen(offset).toPointF()
                    }

                RemoveSpaceGesture.Builder().setPoints(startPoint, endPoint).build()
            }
        ) { textFieldState, resultCode ->
            assertThat(resultCode).isEqualTo(InputConnection.HANDWRITING_GESTURE_RESULT_FAILED)
            assertThat(textFieldState.text).isEqualTo(text)
            assertThat(textFieldState.selection).isEqualTo(TextRange(text.length))
        }
    }

    private fun testTextFieldHandwritingGesture(
        text: String,
        initialSelection: TextRange = TextRange(text.length),
        outputTransformation: OutputTransformation? = null,
        gestureFactory: LayoutCoordinates.(TextLayoutResult) -> HandwritingGesture,
        preview: Boolean = false,
        assertion: (TextFieldState, resultCode: Int) -> Unit
    ) {
        val textFieldState = TextFieldState(initialText = text, initialSelection = initialSelection)
        var getTextLayoutResult: (() -> TextLayoutResult?)? = null
        var layoutCoordinates: LayoutCoordinates? = null

        inputMethodInterceptor.setTextFieldTestContent {
            val viewConfiguration =
                object : ViewConfiguration by LocalViewConfiguration.current {
                    override val handwritingGestureLineMargin: Float = lineMargin
                }
            CompositionLocalProvider(value = LocalViewConfiguration provides viewConfiguration) {
                BasicTextField(
                    state = textFieldState,
                    modifier =
                        Modifier.fillMaxSize().testTag(Tag).onGloballyPositioned {
                            layoutCoordinates = it
                        },
                    textStyle = TextStyle(fontSize = 10.sp, fontFamily = TEST_FONT_FAMILY),
                    outputTransformation = outputTransformation,
                    onTextLayout = { getTextLayoutResult = it }
                )
            }
        }

        rule.onNodeWithTag(Tag).requestFocus()
        rule.waitForIdle()
        val textLayoutResult = getTextLayoutResult?.invoke() ?: fail("Can't get TextLayoutResult")
        val gesture = gestureFactory.invoke(layoutCoordinates!!, textLayoutResult)
        var resultCode = InputConnection.HANDWRITING_GESTURE_RESULT_UNKNOWN

        inputMethodInterceptor.withInputConnection {
            if (preview) {
                previewHandwritingGesture(
                    gesture as PreviewableHandwritingGesture,
                    cancellationSignal
                )
            } else {
                performHandwritingGesture(gesture, /* executor= */ null) { resultCode = it }
            }
        }

        rule.runOnIdle { assertion.invoke(textFieldState, resultCode) }
    }

    private fun LayoutCoordinates.localToScreen(rect: Rect): Rect {
        val localOriginInScreen = localToScreen(Offset.Zero)
        return rect.translate(localOriginInScreen)
    }
}
