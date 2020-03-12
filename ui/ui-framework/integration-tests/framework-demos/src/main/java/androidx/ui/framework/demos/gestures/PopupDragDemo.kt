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
import androidx.compose.remember
import androidx.compose.state
import androidx.ui.core.Alignment
import androidx.ui.core.Popup
import androidx.ui.core.Text
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack
import androidx.ui.text.TextStyle
import androidx.ui.text.style.TextAlign
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.round

@Composable
fun PopupDragDemo() {
    val offset = state {
        PxPosition.Origin
    }

    val observer = remember {
        object : DragObserver {
            override fun onDrag(dragDistance: PxPosition): PxPosition {
                offset.value += dragDistance
                return dragDistance
            }
        }
    }

    Popup(alignment = Alignment.TopStart, offset = offset.value.round()) {
        Stack {
            RawDragGestureDetector(observer) {
                Box(
                    LayoutSize(70.dp),
                    shape = CircleShape,
                    backgroundColor = Color.Green,
                    gravity = ContentGravity.Center
                ) {
                    Text(
                        text = "This is a popup!",
                        style = TextStyle(textAlign = TextAlign.Center)
                    )
                }
            }
        }
    }
}
