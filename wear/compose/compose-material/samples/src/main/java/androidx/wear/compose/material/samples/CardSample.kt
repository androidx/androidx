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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard

@Sampled
@Composable
fun AppCardWithIcon() {
    AppCard(
        onClick = {},
        appName = { Text("AppName") },
        appImage = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier =
                    Modifier.size(CardDefaults.AppImageSize)
                        .wrapContentSize(align = Alignment.Center),
            )
        },
        title = { Text("AppCard") },
        time = { Text("now") },
    ) {
        Text("Some body content")
        Text("and some more body content")
    }
}

@Sampled
@Composable
fun AppCardWithImage() {
    AppCard(
        onClick = {},
        appName = { Text("App name") },
        appImage = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier =
                    Modifier.size(CardDefaults.AppImageSize)
                        .wrapContentSize(align = Alignment.Center),
            )
        },
        title = {
            Text(
                text = "Title with maximum two lines",
                maxLines = 2,
            )
        },
        time = { Text("now") },
    ) {
        Spacer(Modifier.height(6.dp))
        Image(
            modifier =
                Modifier.padding(end = 28.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(16.dp)),
            painter = painterResource(R.drawable.card_background),
            contentScale = ContentScale.Crop,
            contentDescription = null
        )
    }
}

@Sampled
@Composable
fun TitleCardStandard() {
    TitleCard(
        onClick = {},
        title = { Text("TitleCard") },
        time = { Text("now") },
    ) {
        Text("Some body content")
        Text("and some more body content")
    }
}

@Sampled
@Composable
fun TitleCardWithImageBackground() {
    TitleCard(
        onClick = { /* Do something */ },
        title = { Text("TitleCard With an ImageBackground") },
        backgroundPainter =
            CardDefaults.imageWithScrimBackgroundPainter(
                backgroundImagePainter = painterResource(id = R.drawable.backgroundimage)
            ),
        contentColor = MaterialTheme.colors.onSurface,
        titleColor = MaterialTheme.colors.onSurface,
    ) {
        // Apply 24.dp padding in bottom for TitleCard with an ImageBackground.
        // Already 12.dp padding exists. Ref - [CardDefaults.ContentPadding]
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp),
        ) {
            Text("Text coloured to stand out on the image")
        }
    }
}
