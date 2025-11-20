/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.demos.text

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun EllipsizeRedrawDemo() {
    val transition = rememberInfiniteTransition("padding")
    val padding = transition.animateFloat(0f, 50f, infiniteRepeatable(tween(10000)))
    Box(
        modifier =
            Modifier.padding(top = padding.value.dp)
                .fillMaxSize()
                .border(1.dp, Color.Blue)
                .padding(24.dp)
                .border(1.dp, Color.Cyan),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BasicText("If b/389707025 fixed, the following should display with no animation")

            // reset everything at 49
            key(padding.value.toInt() / 49) {
                BasicText(
                    text = "I will definitely fill the screen!".repeat(10),
                    modifier = Modifier.border(1.dp, Color.Red),
                    style =
                        TextStyle(
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp,
                            lineHeight = 24.sp,
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.Fixed,
                                ),
                        ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = { Color.Black },
                )
                BasicText(
                    text = "I will definitely fill the screen!".repeat(10),
                    modifier = Modifier.border(1.dp, Color.Red),
                    style =
                        TextStyle(
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp,
                            lineHeight = 24.sp,
                            lineHeightStyle =
                                LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.None,
                                    mode = LineHeightStyle.Mode.Fixed,
                                ),
                        ),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 3,
                    color = { Color.Black },
                )
            }
        }
    }
}
