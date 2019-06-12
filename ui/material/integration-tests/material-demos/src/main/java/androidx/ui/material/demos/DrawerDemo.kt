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

package androidx.ui.material.demos

import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.state
import androidx.compose.unaryPlus
import androidx.ui.baseui.ColoredRect
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Alignment
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.HeightSpacer
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.material.BottomDrawer
import androidx.ui.material.Button
import androidx.ui.material.DrawerState
import androidx.ui.material.ModalDrawer
import androidx.ui.material.StaticDrawer
import androidx.ui.material.themeTextStyle
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.style.TextAlign

@Composable
fun StaticDrawerDemo() {
    Row {
        StaticDrawer {
            Center {
                Text("Drawer Content", +themeTextStyle { h4 })
            }
        }
        ColoredRect(Color.Black, width = 1.dp)
        Text("Rest of App", +themeTextStyle { h5 })
    }
}

@Composable
fun ModalDrawerDemo() {
    val (state, onStateChange) = +state { DrawerState.Closed }

    Stack {
        aligned(Alignment.Center) {
            val text =
                if (state == DrawerState.Closed) {
                    "Drawer Closed.\n>>>> Pull to open >>>>"
                } else {
                    "Drawer Opened.\n<<<< Swipe to close <<<<"
                }
            Column {
                Text(
                    text = text,
                    paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center),
                    style = +themeTextStyle { h5 })
                HeightSpacer(20.dp)
                Button(text = "Click to open", onClick = { onStateChange(DrawerState.Opened) })
            }
        }
        aligned(Alignment.CenterLeft) {
            ModalDrawer(state, onStateChange) {
                Column {
                    Text("Drawer Content", +themeTextStyle { h4 })
                    Button(
                        text = "Close Drawer",
                        onClick = { onStateChange(DrawerState.Closed) })
                }
            }
        }
    }

}

@Composable
fun BottomDrawerDemo() {
    val (state, onStateChange) = +state { DrawerState.Closed }

    Stack {
        aligned(Alignment.Center) {
            val text =
                if (state == DrawerState.Closed) {
                    "Drawer Closed.\n▲▲▲ Pull to open ▲▲▲"
                } else {
                    "Drawer Opened.\n▼▼▼ Drag down to close ▼▼▼"
                }
            Column {
                Text(
                    text = text,
                    paragraphStyle = ParagraphStyle(textAlign = TextAlign.Center),
                    style = +themeTextStyle { h5 })
                HeightSpacer(20.dp)
                Button(text = "Click to open", onClick = { onStateChange(DrawerState.Opened) })
            }
        }
        aligned(Alignment.BottomCenter) {
            BottomDrawer(state, onStateChange) {
                Column {
                    Text("Drawer Content", +themeTextStyle { h4 })
                    Button(
                        text = "Close Drawer",
                        onClick = { onStateChange(DrawerState.Closed) })
                }
            }
        }
    }
}
