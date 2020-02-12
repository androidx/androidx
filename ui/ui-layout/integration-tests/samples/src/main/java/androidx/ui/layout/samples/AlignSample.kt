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
import androidx.ui.graphics.Color
import androidx.ui.layout.Align
import androidx.ui.layout.Center
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutAlign
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.unit.dp

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
    // Here, the blue rectangle prefers to have a 20.dp size, subject to the incoming constraints.
    // Because of the LayoutSize.Min modifier, if LayoutAlign was not present, the blue rectangle
    // would actually be 40.dp x 40.dp to satisfy the min size set by the modifier. However,
    // because we also provide LayoutAlign, the blue rectangle is allowed to be smaller than the min
    // constraints, and it will be aligned in the 40.dp x 40.dp space. Note the example would not
    // work if LayoutAlign was specified before LayoutSize in the modifier chain.
    SizedRectangle(
        modifier = LayoutSize.Min(40.dp, 40.dp) + LayoutAlign.TopCenter,
        color = Color.Blue,
        width = 20.dp,
        height = 20.dp
    )
}

@Sampled
@Composable
fun SimpleVerticallyAlignedModifier() {
    // Here, the blue rectangle prefers to have a 50.dp height, subject to the incoming constraints.
    // Because of the LayoutSize.Fill modifier, if LayoutAlign was not present, the blue rectangle
    // would actually fill the available height to satisfy the min height set by the modifier.
    // However, because we also provide LayoutAlign, the blue rectangle is allowed to be smaller
    // than the min height, and it will be centered vertically in the available height.
    // The width of the rectangle will still fill the available width, because the
    // LayoutAlign.CenterVertically modifier is only concerned with vertical alignment.
    // Note the example would not work if LayoutAlign was specified before LayoutSize
    // in the modifier chain.
    SizedRectangle(
        modifier = LayoutSize.Fill + LayoutAlign.CenterVertically,
        color = Color.Blue,
        height = 50.dp
    )
}

@Sampled
@Composable
fun SimpleGravityInRow() {
    Row(LayoutHeight.Fill) {
        // The child with no gravity modifier is positioned by default so that its top edge is
        // aligned to the top of the vertical axis.
        SizedRectangle(color = Color.Magenta, width = 80.dp, height = 40.dp)
        // Gravity.Top, the child will be positioned so that its top edge is aligned to the top
        // of the vertical axis.
        SizedRectangle(LayoutGravity.Top, color = Color.Red, width = 80.dp, height = 40.dp)
        // Gravity.Center, the child will be positioned so that its center is in the middle of
        // the vertical axis.
        SizedRectangle(LayoutGravity.Center, color = Color.Yellow, width = 80.dp, height = 40.dp)
        // Gravity.Bottom, the child will be positioned so that its bottom edge is aligned to the
        // bottom of the vertical axis.
        SizedRectangle(LayoutGravity.Bottom, color = Color.Green, width = 80.dp, height = 40.dp)
    }
}

@Sampled
@Composable
fun SimpleGravityInColumn() {
    Column(LayoutWidth.Fill) {
        // The child with no gravity modifier is positioned by default so that its start edge
        // aligned with the start edge of the horizontal axis.
        SizedRectangle(color = Color.Magenta, width = 80.dp, height = 40.dp)
        // Gravity.Start, the child will be positioned so that its start edge is aligned with
        // the start edge of the horizontal axis.
        SizedRectangle(LayoutGravity.Start, color = Color.Red, width = 80.dp, height = 40.dp)
        // Gravity.Center, the child will be positioned so that its center is in the middle of
        // the horizontal axis.
        SizedRectangle(LayoutGravity.Center, color = Color.Yellow, width = 80.dp, height = 40.dp)
        // Gravity.End, the child will be positioned so that its end edge aligned to the end of
        // the horizontal axis.
        SizedRectangle(LayoutGravity.End, color = Color.Green, width = 80.dp, height = 40.dp)
    }
}