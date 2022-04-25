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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text

@Composable
fun ScrollableColumnDemo() {
    Row {
        NotScrollableLabel(Modifier.weight(1f))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentWidth()
                .verticalScroll(rememberScrollState())
                .background(Color.Green.copy(alpha = 0.1f))
                .padding(horizontal = 24.dp)
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
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color.Red.copy(alpha = 0.1f))
            .wrapContentSize()
    )
}
