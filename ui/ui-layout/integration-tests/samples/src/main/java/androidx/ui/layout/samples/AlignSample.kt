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
import androidx.ui.layout.Aligned
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.ExpandedHeight
import androidx.ui.layout.ExpandedWidth
import androidx.ui.layout.Gravity
import androidx.ui.layout.Row

@Sampled
@Composable
fun SimpleAlign() {
    Align(Alignment.BottomRight) {
        SizedRectangle(color = Color.Blue, width = 20.dp, height = 20.dp)
    }
}

@Sampled
@Composable
fun SimpleCenter() {
    Center {
        SizedRectangle(color = Color.Blue, width = 20.dp, height = 20.dp)
    }
}

@Sampled
@Composable
fun SimpleAlignedModifier() {
    SizedRectangle(modifier = Aligned.TopCenter, color = Color.Blue, width = 20.dp, height = 20.dp)
}

@Sampled
@Composable
fun SimpleVerticallyAlignedModifier() {
    SizedRectangle(modifier = Aligned.CenterVertically, color = Color.Blue, height = 50.dp)
}

@Sampled
@Composable
fun SimpleGravityInRow() {
    Row(ExpandedHeight) {
        // The child with no gravity modifier is positioned by default so that its top edge is
        // aligned to the top of the vertical axis.
        SizedRectangle(color = Color.Magenta, width = 80.dp, height = 40.dp)
        // Gravity.Top, the child will be positioned so that its top edge is aligned to the top
        // of the vertical axis.
        SizedRectangle(Gravity.Top, color = Color.Red, width = 80.dp, height = 40.dp)
        // Gravity.Center, the child will be positioned so that its center is in the middle of
        // the vertical axis.
        SizedRectangle(Gravity.Center, color = Color.Yellow, width = 80.dp, height = 40.dp)
        // Gravity.Bottom, the child will be positioned so that its bottom edge is aligned to the
        // bottom of the vertical axis.
        SizedRectangle(Gravity.Bottom, color = Color.Green, width = 80.dp, height = 40.dp)
    }
}

@Sampled
@Composable
fun SimpleGravityInColumn() {
    Column(ExpandedWidth) {
        // The child with no gravity modifier is positioned by default so that its start edge
        // aligned with the start edge of the horizontal axis.
        SizedRectangle(color = Color.Magenta, width = 80.dp, height = 40.dp)
        // Gravity.Start, the child will be positioned so that its start edge is aligned with
        // the start edge of the horizontal axis.
        SizedRectangle(Gravity.Start, color = Color.Red, width = 80.dp, height = 40.dp)
        // Gravity.Center, the child will be positioned so that its center is in the middle of
        // the horizontal axis.
        SizedRectangle(Gravity.Center, color = Color.Yellow, width = 80.dp, height = 40.dp)
        // Gravity.End, the child will be positioned so that its end edge aligned to the end of
        // the horizontal axis.
        SizedRectangle(Gravity.End, color = Color.Green, width = 80.dp, height = 40.dp)
    }
}