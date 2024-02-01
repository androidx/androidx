/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Sampled
@Composable
fun CardSample() {
    Card(Modifier.size(width = 180.dp, height = 100.dp)) {
        Box(Modifier.fillMaxSize()) {
            Text("Card content", Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Sampled
@Composable
fun ClickableCardSample() {
    Card(
        onClick = { /* Do something */ },
        modifier = Modifier.size(width = 180.dp, height = 100.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Text("Clickable", Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Sampled
@Composable
fun ElevatedCardSample() {
    ElevatedCard(Modifier.size(width = 180.dp, height = 100.dp)) {
        Box(Modifier.fillMaxSize()) {
            Text("Card content", Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Sampled
@Composable
fun ClickableElevatedCardSample() {
    ElevatedCard(
        onClick = { /* Do something */ },
        modifier = Modifier.size(width = 180.dp, height = 100.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Text("Clickable", Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Sampled
@Composable
fun OutlinedCardSample() {
    OutlinedCard(Modifier.size(width = 180.dp, height = 100.dp)) {
        Box(Modifier.fillMaxSize()) {
            Text("Card content", Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Sampled
@Composable
fun ClickableOutlinedCardSample() {
    OutlinedCard(
        onClick = { /* Do something */ },
        modifier = Modifier.size(width = 180.dp, height = 100.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            Text("Clickable", Modifier.align(Alignment.Center))
        }
    }
}
