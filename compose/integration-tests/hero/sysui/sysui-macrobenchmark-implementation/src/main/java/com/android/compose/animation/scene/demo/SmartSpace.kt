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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateElementColorAsState

object SmartSpace {
    object Elements {
        val SmartSpace = ElementKey("SmartSpace")
    }

    object Values {
        val TextColor = ValueKey("SmartSpaceTextColor")
    }
}

@Composable
fun ContentScope.SmartSpace(textColor: Color, modifier: Modifier = Modifier) {
    ElementWithValues(SmartSpace.Elements.SmartSpace, modifier) {
        val color = animateElementColorAsState(textColor, SmartSpace.Values.TextColor)

        content {
            Column {
                BasicText(
                    "Mon, Mar 20",
                    color = { color.value },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // We have to use valueOrNull here and fallback on the target color given that
                    // there is no way to tint the color at drawing time.
                    // TODO(b/231674463): We should not read color during composition.
                    val color by color.unsafeCompositionState(initialValue = textColor)

                    Icon(
                        Icons.Default.WbSunny,
                        null,
                        Modifier.size(24.dp).padding(end = 4.dp),
                        tint = color,
                    )

                    Text("13°C", color = color, fontSize = 14.sp)
                }
            }
        }
    }
}
