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
import androidx.ui.core.dp
import androidx.ui.graphics.Color
import androidx.ui.layout.LayoutExpanded
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Stack

@Sampled
@Composable
fun SimpleStack() {
    Stack {
        SizedRectangle(LayoutExpanded, color = Color.Cyan)
        SizedRectangle(
            LayoutGravity.Stretch + LayoutPadding(top = 20.dp, bottom = 20.dp),
            color = Color.Yellow
        )
        SizedRectangle(LayoutGravity.Stretch + LayoutPadding(40.dp), color = Color.Magenta)
        SizedRectangle(LayoutGravity.Center + LayoutSize(300.dp, 300.dp), color = Color.Green)
        SizedRectangle(LayoutGravity.TopLeft + LayoutSize(150.dp, 150.dp), color = Color.Red)
        SizedRectangle(
            LayoutGravity.BottomRight + LayoutSize(150.dp, 150.dp),
            color = Color.Blue
        )
    }
}