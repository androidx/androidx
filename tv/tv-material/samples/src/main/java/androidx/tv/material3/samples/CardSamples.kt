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

package androidx.tv.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClassicCard
import androidx.tv.material3.CompactCard
import androidx.tv.material3.Text
import androidx.tv.material3.WideClassicCard

@Sampled
@Composable
fun HorizontalImageAspectRatioCardSample() {
    Card(
        onClick = {},
        modifier = Modifier.width(200.dp).aspectRatio(CardDefaults.HorizontalImageAspectRatio),
        border =
            CardDefaults.border(
                focusedBorder =
                    Border(
                        border = BorderStroke(width = 3.dp, color = Color.Green),
                        shape = RoundedCornerShape(5),
                    ),
            ),
        colors =
            CardDefaults.colors(containerColor = Color.Red, focusedContainerColor = Color.Yellow),
        scale =
            CardDefaults.scale(
                focusedScale = 1.05f,
            )
    ) {}
}

@Sampled
@Composable
fun VerticalImageAspectRatioCardSample() {
    Card(
        onClick = {},
        modifier = Modifier.width(200.dp).aspectRatio(CardDefaults.VerticalImageAspectRatio),
        border =
            CardDefaults.border(
                focusedBorder =
                    Border(
                        border = BorderStroke(width = 3.dp, color = Color.Green),
                        shape = RoundedCornerShape(5),
                    ),
            ),
        colors =
            CardDefaults.colors(containerColor = Color.Red, focusedContainerColor = Color.Yellow),
        scale =
            CardDefaults.scale(
                focusedScale = 1.05f,
            )
    ) {}
}

@Sampled
@Composable
fun SquareImageAspectRatioCardSample() {
    Card(
        onClick = {},
        modifier = Modifier.width(150.dp).aspectRatio(CardDefaults.SquareImageAspectRatio),
        border =
            CardDefaults.border(
                focusedBorder = Border(border = BorderStroke(width = 3.dp, color = Color.Green)),
            ),
        shape =
            CardDefaults.shape(
                shape = CircleShape,
            ),
        colors =
            CardDefaults.colors(containerColor = Color.Red, focusedContainerColor = Color.Yellow),
        scale =
            CardDefaults.scale(
                focusedScale = 1.05f,
            )
    ) {}
}

@Sampled
@Composable
fun ClassicCardSample() {
    ClassicCard(
        modifier = Modifier.size(150.dp, 120.dp),
        image = { Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Blue)) },
        title = { Text("Classic Card") },
        contentPadding = PaddingValues(8.dp),
        onClick = {}
    )
}

@Sampled
@Composable
fun CompactCardSample() {
    CompactCard(
        modifier = Modifier.size(150.dp, 120.dp),
        image = { Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Blue)) },
        title = { Text(text = "Compact Card", modifier = Modifier.padding(8.dp)) },
        onClick = {}
    )
}

@Sampled
@Composable
fun WideClassicCardSample() {
    WideClassicCard(
        modifier = Modifier.size(180.dp, 100.dp),
        image = { Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color.Blue)) },
        title = { Text(text = "Wide Classic Card", modifier = Modifier.padding(start = 8.dp)) },
        contentPadding = PaddingValues(8.dp),
        onClick = {}
    )
}
