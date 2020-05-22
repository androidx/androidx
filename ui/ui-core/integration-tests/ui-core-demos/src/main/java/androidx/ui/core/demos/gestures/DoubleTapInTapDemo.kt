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

package androidx.ui.core.demos.gestures

import androidx.compose.Composable
import androidx.compose.state
import androidx.ui.core.Modifier
import androidx.ui.core.gesture.doubleTapGestureFilter
import androidx.ui.core.gesture.tapGestureFilter
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.foundation.drawBackground
import androidx.ui.foundation.drawBorder
import androidx.ui.graphics.RectangleShape
import androidx.ui.layout.Column
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp

@Composable
fun DoubleTapInTapDemo() {
    val defaultColor = Grey

    val innerColor = state { defaultColor }
    val outerColor = state { defaultColor }

    val onTap: (PxPosition) -> Unit = {
        outerColor.value = outerColor.value.next()
    }

    val onDoubleTap: (PxPosition) -> Unit = { _ ->
        innerColor.value = innerColor.value.prev()
    }

    Column {
        Text(
            "Demonstrates interaction between DoubleTapGestureFilter and TapGestureFilter in an " +
                    "edge case that is nevertheless supported (normally regions will be separated" +
                    " by a pressIndicatorGestureFilter, but here they are not)."
        )
        Text(
            "Double tap the inner box to change the inner box color. Tap anywhere in the outer " +
                    "box once (including the inner box) to change the outer box background " +
                    "color. Tap rapidly with one or more fingers anywhere and the colors should" +
                    "change as one would expect."
        )
        Box(
            Modifier

                .tapGestureFilter(onTap)
                .drawBorder(2.dp, BorderColor)
                .drawBackground(outerColor.value, RectangleShape)
                .fillMaxSize()

                .padding(48.dp)
                .doubleTapGestureFilter(onDoubleTap)
                .drawBorder(2.dp, BorderColor)
                .drawBackground(innerColor.value, RectangleShape)
                .fillMaxSize()
        )
    }
}