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

package androidx.ui.layout.demos

import android.app.Activity
import android.os.Bundle
import androidx.ui.core.Dp
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.Spacer
import androidx.ui.layout.Row
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Wrap
import androidx.ui.graphics.Color
import androidx.ui.text.TextStyle
import androidx.compose.Composable
import androidx.ui.core.Modifier
import androidx.ui.core.setContent
import androidx.ui.core.sp
import androidx.ui.layout.Arrangement
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.samples.DrawRectangle

class LayoutActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LayoutDemo()
        }
    }
}

@Composable
fun ContainerWithBackground(
    width: Dp? = null,
    height: Dp? = null,
    color: Color,
    children: @Composable() () -> Unit
) {
    Wrap {
        DrawRectangle(color = color)
        Container(width = width, height = height, children = children)
    }
}

@Composable
fun LayoutDemo() {
    val lightGrey = Color(0xFFCFD8DC)
    Column {
        Text(text = "Row", style = TextStyle(fontSize = 48.sp))
        ContainerWithBackground(width = ExampleSize, color = lightGrey) {
            Row(LayoutWidth.Fill) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(LayoutHeight(24.dp))
        ContainerWithBackground(width = ExampleSize, color = lightGrey) {
            Row(LayoutWidth.Fill, arrangement = Arrangement.Center) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(LayoutHeight(24.dp))
        ContainerWithBackground(width = ExampleSize, color = lightGrey) {
            Row(LayoutWidth.Fill, arrangement = Arrangement.End) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(LayoutHeight(24.dp))
        ContainerWithBackground(width = ExampleSize, color = lightGrey) {
            Row(LayoutWidth.Fill) {
                PurpleSquare()
                CyanSquare()
            }
        }
        Spacer(LayoutHeight(24.dp))
        ContainerWithBackground(width = ExampleSize, color = lightGrey) {
            Row(LayoutWidth.Fill) {
                PurpleSquare(LayoutGravity.Bottom)
                CyanSquare(LayoutGravity.Bottom)
            }
        }
        Spacer(LayoutHeight(24.dp))
        Text(text = "Column", style = TextStyle(fontSize = 48.sp))
        Row(LayoutWidth.Fill) {
            ContainerWithBackground(height = ExampleSize, color = lightGrey) {
                Column(LayoutHeight.Fill) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(LayoutWidth(24.dp))
            ContainerWithBackground(height = ExampleSize, color = lightGrey) {
                Column(LayoutHeight.Fill, arrangement = Arrangement.Center) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(LayoutWidth(24.dp))
            ContainerWithBackground(height = ExampleSize, color = lightGrey) {
                Column(LayoutHeight.Fill, arrangement = Arrangement.End) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(LayoutWidth(24.dp))
            ContainerWithBackground(height = ExampleSize, color = lightGrey) {
                Column(LayoutHeight.Fill) {
                    PurpleSquare()
                    CyanSquare()
                }
            }
            Spacer(LayoutWidth(24.dp))
            ContainerWithBackground(height = ExampleSize, color = lightGrey) {
                Column(LayoutHeight.Fill) {
                    PurpleSquare(LayoutGravity.End)
                    CyanSquare(LayoutGravity.End)
                }
            }
        }
    }
}

@Composable
fun PurpleSquare(modifier: Modifier = Modifier.None) {
    Container(width = BigSize, height = BigSize, modifier = modifier) {
        DrawRectangle(color = Color(0xFF6200EE))
    }
}

@Composable
fun CyanSquare(modifier: Modifier = Modifier.None) {
    Container(width = SmallSize, height = SmallSize, modifier = modifier) {
        DrawRectangle(color = Color(0xFF03DAC6))
    }
}

private val SmallSize = 24.dp
private val BigSize = 48.dp
private val ExampleSize = 140.dp