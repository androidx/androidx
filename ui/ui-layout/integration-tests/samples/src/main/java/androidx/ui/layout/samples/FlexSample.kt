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
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutExpandedHeight
import androidx.ui.layout.LayoutGravity
import androidx.ui.layout.Row
import androidx.ui.text.TextStyle

@Sampled
@Composable
fun SimpleRow() {
    Row {
        // The child with no flexibility modifier is inflexible by default, will have the specified
        // size.
        SizedRectangle(color = Color.Magenta, width = 40.dp, height = 80.dp)
        // Inflexible, the child will have the specified size.
        SizedRectangle(LayoutInflexible, color = Color.Red, width = 80.dp, height = 40.dp)
        // Flexible, the child will occupy have of the remaining width.
        SizedRectangle(LayoutFlexible(1f), color = Color.Yellow, height = 40.dp)
        // Flexible not tight, the child will occupy at most half of the remaining width.
        SizedRectangle(LayoutFlexible(1f, tight = false), color = Color.Green, height = 80.dp)
    }
}

@Sampled
@Composable
fun SimpleColumn() {
    Column {
        // The child with no flexibility modifier is inflexible by default, will have the specified
        // size.
        SizedRectangle(color = Color.Magenta, width = 40.dp, height = 80.dp)
        // Inflexible, the child will have the specified size.
        SizedRectangle(LayoutInflexible, color = Color.Red, width = 80.dp, height = 40.dp)
        // Flexible, the child will occupy have of the remaining height.
        SizedRectangle(LayoutFlexible(1f), color = Color.Yellow, width = 40.dp)
        // Flexible not tight, the child will occupy at most half of the remaining height.
        SizedRectangle(LayoutFlexible(1f, tight = false), color = Color.Green, width = 80.dp)
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblings() {
    Column {
        // Center of the first rectangle is aligned to the right edge of the second rectangle and
        // left edge of the third one.
        SizedRectangle(
            LayoutGravity.RelativeToSiblings { it.width * 0.5 },
            color = Color.Blue,
            width = 80.dp,
            height = 40.dp
        )
        SizedRectangle(
            LayoutGravity.RelativeToSiblings { it.width },
            color = Color.Magenta,
            width = 80.dp,
            height = 40.dp
        )
        SizedRectangle(
            LayoutGravity.RelativeToSiblings { 0.ipx },
            color = Color.Red,
            width = 80.dp,
            height = 40.dp
        )
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblingsInRow() {
    Row(LayoutExpandedHeight) {
        // Center of the colored rectangle is aligned to first baseline of the text.
        SizedRectangle(
            color = Color.Red,
            width = 80.dp,
            height = 40.dp,
            modifier = LayoutGravity.RelativeToSiblings { it.height * 0.5 }
        )
        Container(width = 80.dp, modifier = LayoutGravity.RelativeToSiblings(FirstBaseline)) {
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
        SizedRectangle(
            LayoutGravity.RelativeToSiblings { it.width * 0.5 },
            color = Color.Blue,
            width = 80.dp,
            height = 40.dp
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