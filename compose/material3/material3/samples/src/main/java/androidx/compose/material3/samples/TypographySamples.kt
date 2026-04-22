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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Sampled
@Composable
fun TypographySample() {
    val typography =
        Typography(
            displayLarge = TextStyle(fontWeight = FontWeight.W100, fontSize = 96.sp),
            labelLarge = TextStyle(fontWeight = FontWeight.W600, fontSize = 14.sp),
        )

    MaterialTheme(typography = typography) {
        Column {
            Text(text = "Display Large", style = MaterialTheme.typography.displayLarge)
            Text(text = "Label Large", style = MaterialTheme.typography.labelLarge)
            Text(text = "Body Large", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Sampled
@Composable
fun TypographyCustomFontFamilySample() {
    val typography = Typography(fontFamily = FontFamily.Cursive)

    MaterialTheme(typography = typography) {
        Column {
            Text(text = "Display Large", style = MaterialTheme.typography.displayLarge)
            Text(text = "Label Large", style = MaterialTheme.typography.labelLarge)
            Text(text = "Body Large", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Sampled
@Composable
fun TypographyCustomFontFamilyOverrideSample() {
    val typography =
        Typography(
            fontFamily = FontFamily.Cursive,
            displayLarge = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 40.sp),
        )

    MaterialTheme(typography = typography) {
        Column {
            Text(text = "Monospace Display Large", style = MaterialTheme.typography.displayLarge)
            Text(text = "Cursive Label Large", style = MaterialTheme.typography.labelLarge)
            Text(text = "Cursive Body Large", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
