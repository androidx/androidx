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

package androidx.ui.core.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.pressIndicatorGestureFilter
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.graphics.compositeOver
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.preferredSize
import androidx.ui.layout.wrapContentSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

/**
 * Simple [pressIndicatorGestureFilter] demo.
 */
@Composable
fun PressIndicatorGestureFilterDemo() {
    val pressed = state { false }

    val onStart: (PxPosition) -> Unit = {
        pressed.value = true
    }

    val onStop = {
        pressed.value = false
    }

    val color =
        if (pressed.value) {
            PressedColor.compositeOver(Grey)
        } else {
            Grey
        }

    Column {
        Text("The box shows press indication when you press it.")
        Box(
            Modifier.fillMaxSize()
                .wrapContentSize(Alignment.Center)
                .pressIndicatorGestureFilter(onStart, onStop, onStop)
                .preferredSize(192.dp),
            backgroundColor = color,
            border = Border(2.dp, BorderColor)
        )
    }
}
