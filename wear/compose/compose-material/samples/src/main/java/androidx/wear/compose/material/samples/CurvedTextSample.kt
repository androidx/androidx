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
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.material.curvedText
import androidx.wear.compose.material.LocalContentAlpha
import androidx.wear.compose.material.LocalContentColor
import androidx.wear.compose.material.ProvideTextStyle

@Sampled
@Composable
fun CurvedTextDemo() {
    CurvedLayout() {
        curvedText("Default")
        curvedText("Red", color = Color.Red)
        curvedText("White On Green", color = Color.White, background = Color.Green)
        curvedText("Big", fontSize = 24.sp)
    }
    ProvideTextStyle(value = TextStyle(color = Color.Green, background = Color.White)) {
        CurvedLayout(anchor = 45f) {
            curvedText("Green On White")
        }
    }
    CompositionLocalProvider(
        LocalContentColor provides Color.Cyan,
        LocalContentAlpha provides 0.5f
    ) {
        CurvedLayout(anchor = 135f) {
            curvedText("Cyan 50%")
        }
    }
}
