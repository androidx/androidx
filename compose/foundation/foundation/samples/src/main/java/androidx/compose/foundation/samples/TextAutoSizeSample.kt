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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Sample of using [TextAutoSize.StepBased] to auto-size text. */
@Sampled
@Composable
fun TextAutoSizeBasicTextSample() {
    Box(modifier = Modifier.size(200.dp)) {
        // The text will find the biggest available font size that fits in the box.
        BasicText(text = "Hello World", autoSize = TextAutoSize.StepBased())
    }
}

/**
 * Example of a custom [TextAutoSize] implementation that uses a list of [TextUnit] presets to find
 * the fitting font size.
 */
@Sampled
fun CustomTextAutoSizeSample() {
    data class PresetsTextAutoSize(private val presets: Array<TextUnit>) : TextAutoSize {
        override fun TextAutoSizeLayoutScope.getFontSize(
            constraints: Constraints,
            text: AnnotatedString
        ): TextUnit {
            var lastTextSize: TextUnit = TextUnit.Unspecified
            for (presetSize in presets) {
                val layoutResult = performLayout(constraints, text, presetSize)
                if (
                    layoutResult.size.width <= constraints.maxWidth &&
                        layoutResult.size.height <= constraints.maxHeight
                ) {
                    lastTextSize = presetSize
                } else {
                    break
                }
            }
            return lastTextSize
        }
    }

    @Composable
    fun App() {
        val autoSize = remember { PresetsTextAutoSize(arrayOf(10.sp, 14.sp, 21.sp)) }
        BasicText(text = "Hello World", autoSize = autoSize)
    }
}
