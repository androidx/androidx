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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard

@Sampled
@Composable
fun CardSample() {
    Card(
        onClick = { /* Do something */ },
    ) {
        Text("Card")
    }
}

@Sampled
@Composable
fun CardWithOnLongClickSample(onLongClickHandler: () -> Unit) {
    Card(
        onClick = { /* Do something */ },
        onLongClick = onLongClickHandler,
        onLongClickLabel = "Long click"
    ) {
        Text("Card with long click")
    }
}

@Sampled
@Composable
fun AppCardSample() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        title = { Text("Card title") },
        time = { Text("now") },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun AppCardWithIconSample() {
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
        title = { Text("Card title") },
        time = { Text("now") },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun AppCardWithImageSample() {
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
        title = { Text("With image") },
        time = { Text("now") },
    ) {
        Spacer(Modifier.height(4.dp))
        Image(
            modifier =
                Modifier.fillMaxWidth(0.85f)
                    .align(Alignment.Start)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp)),
            painter = painterResource(id = R.drawable.card_content_image),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}

@Sampled
@Composable
fun TitleCardSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("now") },
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun TitleCardWithSubtitleAndTimeSample() {
    TitleCard(
        onClick = { /* Do something */ },
        time = { Text("now") },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") }
    )
}

@Sampled
@Composable
fun TitleCardWithMultipleImagesSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("now") },
        modifier = Modifier.semantics { contentDescription = "Background image" }
    ) {
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                modifier =
                    Modifier.weight(2f)
                        .align(Alignment.CenterVertically)
                        .aspectRatio(4f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = R.drawable.card_content_image),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
            Spacer(Modifier.width(4.dp))
            Image(
                modifier =
                    Modifier.weight(1f)
                        .align(Alignment.CenterVertically)
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                painter = painterResource(id = R.drawable.card_content_image),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }
    }
}

@Sampled
@Composable
fun TitleCardWithImageBackgroundSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Card title") },
        time = { Text("now") },
        colors =
            CardDefaults.imageCardColors(
                containerPainter =
                    CardDefaults.imageWithScrimBackgroundPainter(
                        backgroundImagePainter = painterResource(id = R.drawable.backgroundimage)
                    ),
                contentColor = MaterialTheme.colorScheme.onSurface,
                titleColor = MaterialTheme.colorScheme.onSurface
            ),
        contentPadding = CardDefaults.ImageContentPadding,
        modifier = Modifier.semantics { contentDescription = "Background image" }
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun OutlinedCardSample() {
    OutlinedCard(
        onClick = { /* Do something */ },
    ) {
        Text("Outlined card")
    }
}

@Sampled
@Composable
fun OutlinedAppCardSample() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        appImage = {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = "Favorite icon",
                modifier = Modifier.size(CardDefaults.AppImageSize)
            )
        },
        title = { Text("App card") },
        time = { Text("now") },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text("Card content")
    }
}

@Sampled
@Composable
fun OutlinedTitleCardSample() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("Title card") },
        time = { Text("now") },
        colors = CardDefaults.outlinedCardColors(),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text("Card content")
    }
}
