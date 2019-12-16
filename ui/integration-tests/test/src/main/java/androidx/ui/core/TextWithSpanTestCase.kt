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

import androidx.compose.Composable
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.LayoutWrapped
import androidx.ui.test.ComposeTestCase
import androidx.ui.test.RandomTextGenerator
import androidx.ui.text.TextStyle

/**
 * The benchmark test case for [Text], where the input is some [Span]s with [TextStyle]s on it.
 */
class TextWithSpanTestCase(
    textLength: Int,
    randomTextGenerator: RandomTextGenerator
) : ComposeTestCase {

    /**
     * Trick to avoid the text word cache.
     * @see TextBasicTestCase.text
     */
    private val textPieces = randomTextGenerator.nextStyledWordList(
        length = textLength,
        hasMetricAffectingStyle = true
    )

    @Composable
    override fun emitContent() {
        Text(
            style = TextStyle(color = Color.Black, fontSize = 8.sp),
            modifier = LayoutWrapped + LayoutWidth(160.dp)
        ) {
            textPieces.forEach { (text, style) ->
                Span(text = text, style = style)
            }
        }
    }
}