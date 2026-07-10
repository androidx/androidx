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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateElementColorAsState

object Clock {
    object Elements {
        val Clock = ElementKey("Clock")
    }

    object Values {
        val TextColor = ValueKey("ClockTextColor")
    }
}

@Composable
fun ContentScope.Clock(color: Color, modifier: Modifier = Modifier) {
    ElementWithValues(Clock.Elements.Clock, modifier) {
        val color by animateElementColorAsState(color, Clock.Values.TextColor)

        content {
            BasicText(
                "03:25",
                color = { color },
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
            )
        }
    }
}
