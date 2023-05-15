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

package androidx.compose.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp

/**
 * Sample showing how to use TextMeasurer in a non-composable function with a plain String for text.
 */
@Sampled
fun measureTextStringWithConstraints(textMeasurer: TextMeasurer) {
    textMeasurer.measure(
        text = "Hello, World",
        style = TextStyle(
            color = Color.Red,
            fontSize = 16.sp,
            fontFamily = FontFamily.Cursive
        ),
        constraints = Constraints(
            minWidth = 400,
            maxWidth = 400,
            minHeight = 200,
            maxHeight = 400
        )
    )
}

/**
 * Sample showing how to use TextMeasurer in a non-composable function with an AnnotatedString
 * for text.
 */
@Sampled
fun measureTextAnnotatedString(textMeasurer: TextMeasurer) {
    textMeasurer.measure(
        text = buildAnnotatedString {
            append("Hello, ")
            withStyle(SpanStyle(color = Color.Blue)) {
                append("World!")
            }
        },
        style = TextStyle(
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace
        )
    )
}
