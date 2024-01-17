/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(ExperimentalTvMaterial3Api::class)

package androidx.tv.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.CompactCard
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.tv.material3.WideClassicCard

@Sampled
@Composable
fun CardSample() {
    Card(
        modifier = Modifier.size(150.dp, 120.dp),
        onClick = { }
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                text = "Card",
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Sampled
@Composable
fun ClassicCardSample() {
    ClassicCard(
        modifier = Modifier.size(150.dp, 120.dp),
        image = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Blue)
            )
        },
        title = {
            Text("Classic Card")
        },
        contentPadding = PaddingValues(8.dp),
        onClick = { }
    )
}

@Sampled
@Composable
fun CompactCardSample() {
    CompactCard(
        modifier = Modifier.size(150.dp, 120.dp),
        image = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Blue)
            )
        },
        title = {
            Text(
                text = "Compact Card",
                modifier = Modifier.padding(8.dp)
            )
        },
        onClick = { }
    )
}

@Sampled
@Composable
fun WideClassicCardSample() {
    WideClassicCard(
        modifier = Modifier.size(180.dp, 100.dp),
        image = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(Color.Blue)
            )
        },
        title = {
            Text(
                text = "Wide Classic Card",
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        contentPadding = PaddingValues(8.dp),
        onClick = { }
    )
}
