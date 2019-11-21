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

import android.app.Activity
import android.os.Bundle
import androidx.compose.memo
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.core.Alignment
import androidx.ui.core.Popup
import androidx.ui.core.PxPosition
import androidx.ui.core.Text
import androidx.ui.core.disposeActivityComposition
import androidx.ui.core.dp
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.core.round
import androidx.ui.core.setContent
import androidx.ui.foundation.shape.DrawShape
import androidx.ui.foundation.shape.corner.CircleShape
import androidx.ui.graphics.Color
import androidx.ui.layout.Container
import androidx.ui.layout.Wrap
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.style.TextAlign

class PopupDragActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            val offset = +state {
                PxPosition.Origin
            }

            val observer = +memo {
                object : DragObserver {
                    override fun onDrag(dragDistance: PxPosition): PxPosition {
                        offset.value += dragDistance
                        return dragDistance
                    }
                }
            }

            Popup(alignment = Alignment.TopLeft, offset = offset.value.round()) {
                Wrap {
                    DrawShape(CircleShape, Color.Green)
                    RawDragGestureDetector(observer) {
                        Container(width = 70.dp, height = 70.dp) {
                            Text(
                                text = "This is a popup!",
                                paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center)
                            )
                        }
                    }
                }
            }
        }
    }

    // TODO(b/140396932): Replace with Activity.disposeComposition() when it will be working
    //  properly
    override fun onDestroy() {
        disposeActivityComposition(this)
        super.onDestroy()
    }
}