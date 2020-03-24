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

package androidx.ui.framework.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.gesture.TapGestureDetector
import androidx.ui.foundation.Border
import androidx.ui.foundation.Box
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutSize
import androidx.ui.unit.dp

/**
 * Simple PressReleasedGestureDetector demo.
 */
@Composable
fun PressReleasedGestureDetectorDemo() {
    val color = state { Colors.random() }

    val onTap = {
        color.value = color.value.anotherRandomColor()
    }

    Box(
        LayoutSize.Fill + LayoutAlign.Center + TapGestureDetector(onTap) + LayoutSize(192.dp),
        backgroundColor = color.value,
        border = Border(2.dp, BorderColor)
    )
}
