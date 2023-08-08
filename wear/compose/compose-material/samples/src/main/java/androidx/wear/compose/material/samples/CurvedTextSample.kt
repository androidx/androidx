/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedAlignment
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material.LocalContentAlpha
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.ProvideTextStyle
import androidx.wear.compose.material.curvedText

@Sampled
@Composable
fun CurvedTextDemo() {
    CurvedLayout() {
        curvedColumn(angularAlignment = CurvedAlignment.Angular.Center) {
            curvedRow {
                curvedText("Red", color = Color.Red)
                curvedText(
                    "White On Green",
                    color = Color.White,
                    background = Color.Green,
                    modifier = CurvedModifier.padding(angular = 5.dp)
                )
                curvedText("Big", fontSize = 24.sp)
                curvedText(
                    "Extra Bold",
                    fontWeight = FontWeight.ExtraBold,
                    modifier = CurvedModifier.padding(angular = 5.dp)
                )
            }
            curvedRow {
                curvedText("Default")
                curvedText(
                    "Italic",
                    fontStyle = FontStyle.Italic,
                    modifier = CurvedModifier.padding(angular = 5.dp)
                )
                curvedText("Monospaced", fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Sampled
@Composable
fun CurvedTextProviderDemo() {
    CompositionLocalProvider(
        LocalContentColor provides Color.Cyan,
        LocalContentAlpha provides 0.5f,
        LocalTextStyle provides TextStyle(fontFamily = FontFamily.Serif)
    ) {
        val greenStyle = LocalTextStyle.current.copy(color = Color.Green)
        CurvedLayout {
            curvedText("Serif Cyan 50%")
            curvedText("Green", style = CurvedTextStyle(greenStyle))
        }
    }

    ProvideTextStyle(value = TextStyle(
        color = Color.Green,
        background = Color.White,
        fontWeight = FontWeight.Bold
    )) {
        CurvedLayout(anchor = 90f) {
            curvedText("Green On White")
        }
    }
}
