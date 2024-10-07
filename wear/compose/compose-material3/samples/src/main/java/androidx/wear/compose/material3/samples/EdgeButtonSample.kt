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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Sampled
@Composable
fun EdgeButtonSample() {
    Box(Modifier.fillMaxSize()) {
        Text("Confirm", Modifier.align(Alignment.Center))
        EdgeButton(
            onClick = { /* Do something */ },
            buttonSize = EdgeButtonSize.Medium,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Check icon",
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        }
    }
}

@Sampled
@Composable
fun EdgeButtonListSample() {
    val state = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = state,
        bottomButton = {
            EdgeButton(
                onClick = {},
                buttonSize = EdgeButtonSize.Large,
                colors = buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Ok", textAlign = TextAlign.Center)
            }
        }
    ) {
        ScalingLazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            autoCentering = null,
            contentPadding = PaddingValues(10.dp, 20.dp, 10.dp, 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(10) {
                Card(onClick = {}, modifier = Modifier.fillMaxWidth(0.9f)) { Text("Item #$it") }
            }
        }
    }
}
