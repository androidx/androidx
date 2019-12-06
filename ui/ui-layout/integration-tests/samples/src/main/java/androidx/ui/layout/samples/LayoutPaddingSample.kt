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

package androidx.ui.layout.samples

import androidx.annotation.Sampled
import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.Align
import androidx.ui.layout.Container
import androidx.ui.layout.EdgeInsets
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.Padding

@Sampled
@Composable
fun LayoutPaddingModifier() {
    Align(Alignment.TopLeft) {
        Container {
            DrawRectangle(Color.Gray)
            SizedRectangle(
                modifier = LayoutPadding(
                    left = 20.dp, top = 30.dp, right = 20.dp, bottom = 30.dp
                ),
                color = Color.Blue,
                width = 50.dp,
                height = 50.dp
            )
        }
    }
}

@Sampled
@Composable
fun LayoutPaddingAllModifier() {
    Align(Alignment.TopLeft) {
        Container {
            DrawRectangle(Color.Gray)
            SizedRectangle(
                modifier = LayoutPadding(all = 20.dp),
                color = Color.Blue,
                width = 50.dp,
                height = 50.dp
            )
        }
    }
}

@Sampled
@Composable
fun PaddingComposableEdgeInsets() {
    Align(Alignment.TopLeft) {
        Container {
            DrawRectangle(Color.Gray)
            Padding(EdgeInsets(left = 20.dp, top = 30.dp, right = 20.dp, bottom = 30.dp)) {
                SizedRectangle(color = Color.Blue, width = 50.dp, height = 50.dp)
            }
        }
    }
}

@Sampled
@Composable
fun PaddingComposable() {
    Align(Alignment.TopLeft) {
        Container {
            DrawRectangle(Color.Gray)
            Padding(left = 20.dp, top = 30.dp, right = 20.dp, bottom = 30.dp) {
                SizedRectangle(color = Color.Blue, width = 50.dp, height = 50.dp)
            }
        }
    }
}

@Sampled
@Composable
fun PaddingComposableSameInset() {
    Align(Alignment.TopLeft) {
        Container {
            DrawRectangle(Color.Gray)
            Padding(padding = 20.dp) {
                SizedRectangle(color = Color.Blue, width = 50.dp, height = 50.dp)
            }
        }
    }
}
