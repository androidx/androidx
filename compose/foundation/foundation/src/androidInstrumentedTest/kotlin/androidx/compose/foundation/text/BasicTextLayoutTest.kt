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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// largest size of a unfocused dimension from Constraints.kt
private val UnfocusedDimensionConstraintMax = 2 shl 13

@MediumTest
@RunWith(AndroidJUnit4::class)
class BasicTextLayoutTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun simple_layoutText_doesNotThrow_when2shl14char() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            BasicText(
                text = "a".repeat(2 shl 14),
                style = TextStyle(fontSize = 48.sp),
                onTextLayout = { textLayoutResult = it }
            )
        }
        rule.waitForIdle()

        assertThat(textLayoutResult?.multiParagraph?.height)
            .isGreaterThan(UnfocusedDimensionConstraintMax)
    }

    @Test
    fun annotatedString_layoutText_doesNotThrow_when2shl14char() {
        var textLayoutResult: TextLayoutResult? = null
        rule.setContent {
            BasicText(
                text = AnnotatedString("a".repeat(2 shl 14)),
                style = TextStyle(fontSize = 48.sp),
                onTextLayout = { textLayoutResult = it }
            )
        }
        rule.waitForIdle()
        assertThat(textLayoutResult?.multiParagraph?.height)
            .isGreaterThan(UnfocusedDimensionConstraintMax)
    }
}
