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

package androidx.compose.foundation.text

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Regression test for [b/346918500](https://issuetracker.google.com/346918500).
 *
 * This specific reproduction case would cause a visual overflow or line wrapping for start indices
 * 5..7. We expect instead that there is no visual overflow nor line wrapping for any of these
 * cases.
 */
@MediumTest
@RunWith(Parameterized::class)
class BasicTextIntrinsicWidthWrappingRegressionTest(spanStartIndex: Int) {
    companion object {
        private const val TEXT = "\u8AAD\u307F\u8FBC\u307F\u4E2D..."

        @JvmStatic
        @Parameterized.Parameters(name = "spanStartIndex={0}")
        fun data(): Array<Array<Any?>> {
            return Array(TEXT.length + 1) { arrayOf(it) }
        }
    }

    @get:Rule val rule = createComposeRule()

    // These values are exact for the reproduction case (along with TEXT above).
    private val densityScale = 2.625f
    private val fontScale = 1f
    private val fontSize = 16.sp
    private val lineHeight = 20.sp

    // This value is not exact for the reproduction case.
    private val textColor = Color.Black

    // These values are all derived from other values.
    private val density = Density(densityScale, fontScale)
    private val spanTextColor = textColor.copy(alpha = 0.3f)
    private val spanStyle = SpanStyle(color = spanTextColor)
    private val spanStyles = listOf(AnnotatedString.Range(spanStyle, spanStartIndex, TEXT.length))
    private val annotatedString = AnnotatedString(TEXT, spanStyles)
    private val style = TextStyle(color = textColor, fontSize = fontSize, lineHeight = lineHeight)

    private fun runTest(softWrap: Boolean = true, maxLines: Int = Int.MAX_VALUE) {
        lateinit var textLayout: TextLayoutResult
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides density) {
                BasicText(
                    text = annotatedString,
                    style = style,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    onTextLayout = { textLayout = it }
                )
            }
        }
        assertThat(textLayout.hasVisualOverflow).isFalse()
        assertThat(textLayout.lineCount).isEqualTo(1)
    }

    @Test fun whenSoftWrapFalse_lineCountIsOne() = runTest(softWrap = false)

    @Test fun whenMaxLinesOne_lineCountIsOne() = runTest(maxLines = 1)
}
