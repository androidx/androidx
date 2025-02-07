/*
 * Copyright 2025 The Android Open Source Project
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

import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.test.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/** Regression test for [b/391378120](https://issuetracker.google.com/391378120). */
@MediumTest
class BasicTextUnexpectedWrappingRegressionTest {
    @get:Rule val rule = createComposeRule()

    private val fontResource = R.font.overshoot_test
    private val regularExtentChar = "a" // width of 1, and a matching extent of 1
    private val largerExtentChar = "b" // width of 1, but a larger extent of 1.5
    private val lineHeight = 20.sp // lineHeight will force the text to use StaticLayout

    // Give the last character an extent that wraps it to the next line in the repro case.
    private val text =
        listOf(regularExtentChar, regularExtentChar, regularExtentChar, largerExtentChar)
            .joinToString(separator = "")

    private val style =
        TextStyle(lineHeight = lineHeight, fontFamily = FontFamily(Font(fontResource)))

    @Test
    @SdkSuppress(minSdkVersion = 35)
    fun testNoRegression() {
        primeRegressionCondition()

        lateinit var textLayout: TextLayoutResult
        var reTriggerLayoutStyleState by mutableStateOf(style, policy = neverEqualPolicy())
        rule.setContent {
            Column(Modifier.fillMaxSize().wrapContentSize()) {
                BasicText(
                    text = text,
                    style = reTriggerLayoutStyleState,
                    onTextLayout = { textLayout = it },
                )
            }
        }

        // force recomposition, necessary to reproduce the regression.
        // Doesn't actually change the style.
        reTriggerLayoutStyleState = style
        rule.waitForIdle()

        assertThat(textLayout.lineCount).isEqualTo(1)
    }

    /**
     * The bug fix requires [StaticLayout] to set `mUseBoundsForWidth` and then be re-used. This
     * sets and recycles a [StaticLayout] like so.
     */
    @RequiresApi(35)
    private fun primeRegressionCondition() {
        StaticLayout.Builder.obtain("a", 0, 1, TextPaint(), 1024)
            .setUseBoundsForWidth(true)
            .build() // build method recycles the builder.
    }
}
