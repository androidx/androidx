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
import androidx.ui.core.FirstBaseline
import androidx.ui.core.Text
import androidx.ui.foundation.Box
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.LayoutHeight
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.LayoutWidth
import androidx.ui.layout.Row
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Sampled
@Composable
fun SimpleRow() {
    Row {
        // The child with no weight will have the specified size.
        Box(LayoutSize(40.dp, 80.dp), backgroundColor = Color.Magenta)
        // Has weight, the child will occupy half of the remaining width.
        Box(LayoutHeight(40.dp) + LayoutWeight(1f), backgroundColor = Color.Yellow)
        // Has weight and does not fill, the child will occupy at most half of the remaining width.
        Box(
            LayoutHeight(80.dp) + LayoutWeight(1f, fill = false),
            backgroundColor = Color.Green
        )
    }
}

@Sampled
@Composable
fun SimpleColumn() {
    Column {
        // The child with no weight will have the specified size.
        Box(LayoutSize(40.dp, 80.dp), backgroundColor = Color.Magenta)
        // Has weight, the child will occupy half of the remaining height.
        Box(LayoutWidth(40.dp) + LayoutWeight(1f), backgroundColor = Color.Yellow)
        // Has weight and does not fill, the child will occupy at most half of the remaining height.
        Box(
            LayoutHeight(80.dp) + LayoutWeight(1f, fill = false),
            backgroundColor = Color.Green
        )
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblings() {
    Column {
        // Center of the first rectangle is aligned to the right edge of the second rectangle and
        // left edge of the third one.
        Box(
            LayoutSize(80.dp, 40.dp) + LayoutGravity.RelativeToSiblings { it.width * 0.5 },
            backgroundColor = Color.Blue
        )
        Box(
            LayoutSize(80.dp, 40.dp) + LayoutGravity.RelativeToSiblings { it.width },
            backgroundColor = Color.Magenta
        )
        Box(
            LayoutSize(80.dp, 40.dp) + LayoutGravity.RelativeToSiblings { 0.ipx },
            backgroundColor = Color.Red
        )
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblingsInRow() {
    Row(LayoutHeight.Fill) {
        // Center of the colored rectangle is aligned to first baseline of the text.
        Box(
            backgroundColor = Color.Red,
            modifier = LayoutSize(80.dp, 40.dp) +
                    LayoutGravity.RelativeToSiblings { it.height * 0.5 }
        )
        Box(LayoutWidth(80.dp) + LayoutGravity.RelativeToSiblings(FirstBaseline)) {
            Text(text = "Text.", style = TextStyle(background = Color.Cyan))
        }
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblingsInColumn() {
    Column {
        // Center of the first rectangle is aligned to the right edge of the second rectangle and
        // left edge of the third one.
        Box(
            LayoutSize(80.dp, 40.dp) + LayoutGravity.RelativeToSiblings { it.width * 0.5 },
            backgroundColor = Color.Blue
        )
        SizedRectangleWithLines(
            LayoutGravity.RelativeToSiblings(End),
            color = Color.Magenta,
            width = 80.dp,
            height = 40.dp
        )
        SizedRectangleWithLines(
            LayoutGravity.RelativeToSiblings(Start),
            color = Color.Red,
            width = 80.dp,
            height = 40.dp
        )
    }
}