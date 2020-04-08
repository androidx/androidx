/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Row
import androidx.ui.layout.fillMaxHeight
import androidx.ui.layout.preferredHeight
import androidx.ui.layout.preferredSize
import androidx.ui.layout.preferredWidth
import androidx.ui.unit.dp

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
fun SimpleRelativeToSiblingsInRow() {
    Row(Modifier.fillMaxHeight()) {
        // Center of the colored rectangle is aligned to first baseline of the text.
        Box(
            backgroundColor = Color.Red,
            modifier = Modifier.preferredSize(80.dp, 40.dp)
                .alignWithSiblings { it.height * 0.5 }
        )
        Text(
            text = "Text.",
            modifier = Modifier.alignWithSiblings(FirstBaseline).drawBackground(Color.Cyan)
        )
    }
}
