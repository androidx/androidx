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

package androidx.compose.foundation.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview
@Composable
fun CombinedClickableDemo() {
    var clicks by remember { mutableIntStateOf(0) }
    var doubleClicks by remember { mutableIntStateOf(0) }
    var longClicks by remember { mutableIntStateOf(0) }

    Column(Modifier.padding(16.dp)) {
        Text("Clicks: $clicks", fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("Double clicks: $doubleClicks", fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("Long clicks: $longClicks", fontSize = 14.sp)

        Divider(Modifier.padding(vertical = 12.dp))

        Text(
            "Modifier.combinedClickable() doesn't work on a Button, because it consumes touch " +
                "input before it arrives at combinedClickable:",
        )
        Spacer(Modifier.height(4.dp))
        CombinedClickableButton({ clicks++ }, { doubleClicks++ }, { longClicks++ })

        Divider(Modifier.padding(vertical = 12.dp))

        Text(
            "Modifier.combinedClickable() does work on composables that do not consume touch " +
                "input, such as Box:"
        )
        Spacer(Modifier.height(4.dp))
        CombinedClickableBox({ clicks++ }, { doubleClicks++ }, { longClicks++ })
    }
}

@Composable
private fun CombinedClickableBox(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        Modifier.border(2.dp, Color.Black)
            .background(Color(0xFFFFE59C))
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text("CombinedClickable Box")
    }
}

@Composable
private fun CombinedClickableButton(
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Button(
        onClick = {},
        Modifier.combinedClickable(
            onClick = onClick,
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick
        )
    ) {
        Text("CombinedClickable Button")
    }
}
