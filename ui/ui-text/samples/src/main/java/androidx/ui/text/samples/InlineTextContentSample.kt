/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.text.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.graphics.RectangleShape
import androidx.ui.layout.fillMaxSize
import androidx.ui.text.InlineTextContent
import androidx.ui.text.Placeholder
import androidx.ui.text.PlaceholderVerticalAlign
import androidx.ui.text.annotatedString
import androidx.ui.text.appendInlineContent
import androidx.ui.unit.em

@Sampled
@Composable
fun InlineTextContentSample() {
    val text = annotatedString {
        append("Hello")
        appendInlineContent("inlineContent", "[myBox]")
    }

    val inlineContent = mapOf(
        Pair(
            // This tells the [CoreText] to replace the text annotated with "InlineContent" with
            // the composable given in the [InlineTextContent] object.
            "InlineContent",
            InlineTextContent(
                // Placeholder tells text layout the expected size and vertical alignment of
                // children composable.
                Placeholder(
                    width = 0.5.em,
                    height = 0.5.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                )
            ) {
                // This [Box] will fill maximum size, which is specified by the [Placeholder]
                // above. Notice the width and height in [Placeholder] are specified in TextUnit,
                // and are converted into pixel by text layout.
                Box(modifier = Modifier.fillMaxSize().drawBackground(Color.Red, RectangleShape))
            }
        )
    )

    Text(text = text, inlineContent = inlineContent)
}