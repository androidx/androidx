/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text

/**
 * A screen that includes a partial-height row that is horizontally-scrollable, in order to
 * demonstrate swipe-to-dismiss handling on a screen that contains both scrollable and
 * non-scrollable sections.
 */
// TODO(b/200699800) Does not support swipe-to-dismiss on the row, we need explicit edge-swiping
//  support.
@Composable
fun ScrollableRowDemo() {
    LocalTextInputService
    Column {
        NotScrollableLabel(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .horizontalScroll(rememberScrollState())
                .background(Color.Green.copy(alpha = 0.1f))
                .padding(vertical = 24.dp)
        ) {
            for (i in 0 until 100) {
                Text("$i")
            }
        }
        NotScrollableLabel(Modifier.weight(1f))
    }
}

@Composable
private fun NotScrollableLabel(modifier: Modifier) {
    Text(
        "Not scrollable",
        color = Color.Red,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.1f))
            .wrapContentSize()
    )
}
