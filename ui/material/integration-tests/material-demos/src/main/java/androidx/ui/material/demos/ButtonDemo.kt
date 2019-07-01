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

import android.util.Log
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.baseui.shape.border.Border
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.MainAxisAlignment
import androidx.ui.layout.Padding
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.material.TransparentButton
import androidx.ui.material.themeColor
import androidx.ui.material.themeTextStyle

@Composable
fun ButtonDemo() {
    val onClick: () -> Unit = { Log.e("ButtonDemo", "onClick") }
    CraneWrapper {
        MaterialTheme {
            Center {
                Column(mainAxisAlignment = MainAxisAlignment.SpaceEvenly) {
                    Button(onClick = onClick, text = "LONG TEXT")
                    Button(onClick = onClick, text = "SH")
                    TransparentButton(onClick = onClick, text = "NO BACKGROUND")
                    Button(
                        onClick = onClick,
                        color = +themeColor { secondary },
                        text = "SECONDARY COLOR")

                    TransparentButton(
                        onClick = onClick,
                        border = Border(Color(0xFF888888.toInt()), 1.dp),
                        text = "OUTLINED"
                    )

                    val customColor = Color(0xFFFFFF00.toInt())
                    Button(
                        onClick = onClick,
                        text = "CUSTOM STYLE",
                        textStyle = +themeTextStyle { body2.copy(color = customColor) })
                    Button(onClick = onClick) {
                        Padding(padding = 16.dp) {
                            Text(text = "CUSTOM BUTTON!")
                        }
                    }

                    // TODO(Andrey): Disabled button has wrong bg and text color for now.
                    // Need to figure out where will we store their styling. Not a part of
                    // MaterialColors right now and specs are not clear about this.
                    Button(text = "DISABLED. TODO")
                }
            }
        }
    }
}
