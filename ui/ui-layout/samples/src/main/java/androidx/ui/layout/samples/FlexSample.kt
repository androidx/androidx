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
import androidx.ui.text.FirstBaseline
import androidx.ui.core.Modifier
import androidx.ui.foundation.Box
import androidx.ui.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.text.TextStyle
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

@Sampled
@Composable
fun SimpleRow() {
    Row {
        // The child with no weight will have the specified size.
        Box(Modifier.preferredSize(40.dp, 80.dp), backgroundColor = Color.Magenta)
        // Has weight, the child will occupy half of the remaining width.
        Box(Modifier.preferredHeight(40.dp).weight(1f), backgroundColor = Color.Yellow)
        // Has weight and does not fill, the child will occupy at most half of the remaining width.
        Box(
            Modifier.preferredHeight(80.dp).weight(1f, fill = false),
            backgroundColor = Color.Green
        )
    }
}

@Sampled
@Composable
fun SimpleColumn() {
    Column {
        // The child with no weight will have the specified size.
        Box(Modifier.preferredSize(40.dp, 80.dp), backgroundColor = Color.Magenta)
        // Has weight, the child will occupy half of the remaining height.
        Box(Modifier.preferredWidth(40.dp).weight(1f), backgroundColor = Color.Yellow)
        // Has weight and does not fill, the child will occupy at most half of the remaining height.
        Box(
            Modifier.preferredHeight(80.dp)
                .weight(1f, fill = false),
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
            Modifier.preferredSize(80.dp, 40.dp).alignWithSiblings { it.width * 0.5 },
            backgroundColor = Color.Blue
        )
        Box(
            Modifier.preferredSize(80.dp, 40.dp).alignWithSiblings { it.width },
            backgroundColor = Color.Magenta
        )
        Box(
            Modifier.preferredSize(80.dp, 40.dp).alignWithSiblings { 0.ipx },
            backgroundColor = Color.Red
        )
    }
}

@Sampled
@Composable
fun SimpleRelativeToSiblingsInRow() {
    Row(Modifier.fillMaxHeight()) {
        // Center of the colored rectangle is aligned to first baseline of the text.
        Box(
            backgroundColor = Color.Red,
            modifier = Modifier.preferredSize(80.dp, 40.dp)
                .alignWithSiblings { it.height * 0.5 }
        )
        Box(Modifier.preferredWidth(80.dp).alignWithSiblings(FirstBaseline)) {
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
            Modifier.preferredSize(80.dp, 40.dp).alignWithSiblings { it.width * 0.5 },
            backgroundColor = Color.Blue
        )
        SizedRectangleWithLines(
            Modifier.alignWithSiblings(End),
            color = Color.Magenta,
            width = 80.dp,
            height = 40.dp
        )
        SizedRectangleWithLines(
            Modifier.alignWithSiblings(Start),
            color = Color.Red,
            width = 80.dp,
            height = 40.dp
        )
    }
}