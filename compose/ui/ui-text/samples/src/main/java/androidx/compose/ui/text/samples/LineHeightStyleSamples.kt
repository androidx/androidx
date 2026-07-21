/*
 * Copyright 2026 The Android Open Source Project
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun LineHeightStylePerLineSample() {
    val text = buildAnnotatedString {
        append("This is a line with small text.\n")
        withStyle(SpanStyle(fontSize = 40.sp)) { append("TALL TEXT HERE\n") }
        append("Another line with small text.")
    }
    Text(
        text = text,
        style =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                lineHeightStyle =
                    LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Proportional,
                        trim = LineHeightStyle.Trim.None,
                        mode = LineHeightStyle.Mode.PerLine,
                    ),
            ),
    )
}
