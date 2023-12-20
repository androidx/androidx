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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.samples.AppCardSample
import androidx.wear.compose.material3.samples.AppCardWithIconSample
import androidx.wear.compose.material3.samples.CardSample
import androidx.wear.compose.material3.samples.OutlinedAppCardSample
import androidx.wear.compose.material3.samples.OutlinedCardSample
import androidx.wear.compose.material3.samples.OutlinedTitleCardSample
import androidx.wear.compose.material3.samples.TitleCardSample
import androidx.wear.compose.material3.samples.TitleCardWithImageSample
import androidx.wear.compose.material3.samples.TitleCardWithSubtitleAndTimeSample

@Composable
fun CardDemo() {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { ListHeader { Text("Card") } }
        item { CardSample() }
        item { OutlinedCardSample() }

        item { ListHeader { Text("App card") } }
        item { AppCardSample() }
        item { AppCardWithIconSample() }
        item { AppCardWithImageDemo() }
        item { OutlinedAppCardSample() }

        item { ListHeader { Text("Title card") } }
        item { TitleCardSample() }
        item { TitleCardWithSubtitleDemo() }
        item { TitleCardWithSubtitleAndTimeSample() }
        item { TitleCardWithContentSubtitleAndTimeDemo() }
        item { OutlinedTitleCardSample() }
        item { OutlinedTitleCardWithSubtitleDemo() }
        item { OutlinedTitleCardWithSubtitleAndTimeDemo() }

        item { ListHeader { Text("Image card") } }
        item { TitleCardWithImageSample() }
    }
}

@Composable
private fun AppCardWithImageDemo() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        appImage = { StandardIcon(CardDefaults.AppImageSize) },
        title = { Text("With image") },
        time = { Text("now") },
    ) {
        Spacer(Modifier.height(6.dp))
        Image(
            modifier = Modifier
                .padding(end = 28.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp)),
            painter = painterResource(id = R.drawable.card_background),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}

@Composable
private fun OutlinedTitleCardWithSubtitleAndTimeDemo() {
    TitleCard(
        onClick = { /* Do something */ },
        time = { Text("now") },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
    )
}

@Composable
fun TitleCardWithSubtitleDemo() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") }
    )
}

@Composable
fun TitleCardWithContentSubtitleAndTimeDemo() {
    TitleCard(
        onClick = { /* Do something */ },
        time = { Text("now") },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") }
    ) {
        Text("Card content")
    }
}

@Composable
fun OutlinedTitleCardWithSubtitleDemo() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
    )
}
