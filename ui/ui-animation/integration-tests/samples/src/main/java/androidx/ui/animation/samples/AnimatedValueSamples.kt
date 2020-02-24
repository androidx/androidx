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

package androidx.ui.animation.samples

import androidx.animation.AnimationVector2D
import androidx.animation.TwoWayConverter
import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.animation.animate
import androidx.ui.core.Opacity
import androidx.ui.core.Text
import androidx.ui.core.currentTextStyle
import androidx.ui.foundation.ColoredRect
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

enum class VisibilityState {
    Visible,
    Invisible
}

@Sampled
@Composable
fun VisibilityTransition(visibility: VisibilityState) {
    Container(expanded = true) {
        val opacity = animate(if (visibility == VisibilityState.Invisible) 0f else 1f)
        Opacity(opacity = opacity) {
            Text("Visibility Transition")
        }
    }
}

@Sampled
@Composable
fun ColorTransition(enabled: Boolean) {
    Container(expanded = true) {
        val textColor = animate(if (enabled) Color.Black else Color.Gray)
        Text("Visibility Transition", style = currentTextStyle().copy(color = textColor))
    }
}

data class MySize(val width: Dp, val height: Dp)

@Sampled
@Composable
fun ArbitraryValueTypeTransition(enabled: Boolean) {
    val mySize = remember(enabled) {
        if (enabled) {
            MySize(500.dp, 500.dp)
        } else {
            MySize(100.dp, 100.dp)
        }
    }
    val animSize = animate<MySize, AnimationVector2D>(mySize, TwoWayConverter(
        convertToVector = { AnimationVector2D(it.width.value, it.height.value) },
        convertFromVector = { MySize(it.v1.dp, it.v2.dp) }
    ))
    ColoredRect(Color.Red, width = animSize.width, height = animSize.height)
}
