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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.samples.AppCardSample
import androidx.wear.compose.material3.samples.AppCardWithIconSample
import androidx.wear.compose.material3.samples.AppCardWithImageSample
import androidx.wear.compose.material3.samples.CardSample
import androidx.wear.compose.material3.samples.CardWithOnLongClickSample
import androidx.wear.compose.material3.samples.OutlinedAppCardSample
import androidx.wear.compose.material3.samples.OutlinedCardSample
import androidx.wear.compose.material3.samples.OutlinedTitleCardSample
import androidx.wear.compose.material3.samples.R
import androidx.wear.compose.material3.samples.TitleCardSample
import androidx.wear.compose.material3.samples.TitleCardWithImageBackgroundSample
import androidx.wear.compose.material3.samples.TitleCardWithMultipleImagesSample
import androidx.wear.compose.material3.samples.TitleCardWithSubtitleAndTimeSample

@Composable
fun CardDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("Card") } }
        item { CardSample() }
        item { CardWithOnLongClickSample { showOnLongClickToast(context) } }
        item { CardWithImageDemo() }
        item { CardWithMultipleImagesDemo() }
        item { OutlinedCardSample() }
        item { VerticallyCenteredBaseCard() }
        item { ListHeader { Text("App card") } }
        item { AppCardSample() }
        item { AppCardWithIconSample() }
        item { AppCardWithImageSample() }
        item { AppCardWithMultipleImagesDemo() }
        item { OutlinedAppCardSample() }
        item { ListHeader { Text("Title card") } }
        item { TitleCardSample() }
        item { TitleCardWithSubtitleDemo() }
        item { TitleCardWithSubtitleAndTimeSample() }
        item { TitleCardWithContentSubtitleAndTimeDemo() }
        item { TitleCardWithImageDemo() }
        item { TitleCardWithMultipleImagesSample() }
        item { OutlinedTitleCardSample() }
        item { OutlinedTitleCardWithSubtitleDemo() }
        item { OutlinedTitleCardWithSubtitleAndTimeDemo() }
        item { ListHeader { Text("Image card") } }
        item { TitleCardWithImageBackgroundSample() }
    }
}

@Composable
private fun VerticallyCenteredBaseCard() {
    // Provide a demo of a base Card with vertically centered content
    Card(
        onClick = {},
    ) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(
                "ABCD",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CardWithImageDemo() {
    Card(
        onClick = { /* Do something */ },
    ) {
        Spacer(Modifier.height(4.dp))
        ImageContent()
    }
}

@Composable
fun CardWithMultipleImagesDemo() {
    Card(
        onClick = { /* Do something */ },
    ) {
        Spacer(Modifier.height(4.dp))
        MultipleImagesContent()
    }
}

@Composable
fun AppCardWithMultipleImagesDemo() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        appImage = {
            Icon(
                painter = painterResource(id = android.R.drawable.star_big_off),
                contentDescription = "Star icon",
                modifier =
                    Modifier.size(CardDefaults.AppImageSize)
                        .wrapContentSize(align = Alignment.Center),
            )
        },
        title = { Text("With images") },
        time = { Text("now") },
    ) {
        Spacer(Modifier.height(4.dp))
        MultipleImagesContent()
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

@Composable
fun TitleCardWithImageDemo() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("now") },
        modifier = Modifier.semantics { contentDescription = "Background image" }
    ) {
        Spacer(Modifier.height(4.dp))
        ImageContent()
    }
}

@Composable
private fun ImageContent() {
    val configuration = LocalConfiguration.current
    val imageEndPaddingDp = (0.15f * configuration.screenWidthDp).dp
    Row(modifier = Modifier.fillMaxWidth()) {
        Image(
            modifier = Modifier.weight(1f).aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
            painter = painterResource(id = R.drawable.card_content_image),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(imageEndPaddingDp))
    }
}

@Composable
private fun MultipleImagesContent() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Image(
            modifier =
                Modifier.weight(2f)
                    .height(68.dp)
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(16.dp)),
            painter = painterResource(id = R.drawable.card_content_image),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
        Spacer(Modifier.width(4.dp))
        Image(
            modifier =
                Modifier.weight(1f)
                    .height(68.dp)
                    .align(Alignment.CenterVertically)
                    .clip(RoundedCornerShape(16.dp)),
            painter = painterResource(id = R.drawable.card_content_image),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}
