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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.FixedScale
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
import androidx.wear.compose.material3.ListSubHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.samples.AppCardSample
import androidx.wear.compose.material3.samples.AppCardWithIconSample
import androidx.wear.compose.material3.samples.AppCardWithImageSample
import androidx.wear.compose.material3.samples.CardFillContentSample
import androidx.wear.compose.material3.samples.CardSample
import androidx.wear.compose.material3.samples.CardWithOnLongClickSample
import androidx.wear.compose.material3.samples.NonClickableAppCardSample
import androidx.wear.compose.material3.samples.NonClickableCardSample
import androidx.wear.compose.material3.samples.NonClickableOutlinedCardSample
import androidx.wear.compose.material3.samples.NonClickableTitleCardSample
import androidx.wear.compose.material3.samples.OutlinedAppCardSample
import androidx.wear.compose.material3.samples.OutlinedCardSample
import androidx.wear.compose.material3.samples.OutlinedTitleCardSample
import androidx.wear.compose.material3.samples.R
import androidx.wear.compose.material3.samples.TitleCardSample
import androidx.wear.compose.material3.samples.TitleCardWithMultipleImagesSample
import androidx.wear.compose.material3.samples.TitleCardWithSubtitleAndTimeSample

@Composable
fun CardDemo() {
    val context = LocalContext.current
    ScalingLazyDemo {
        item { ListHeader { Text("Card") } }
        item { CardSample() }
        item { NonClickableCardSample() }
        item { CardWithOnLongClickSample { showOnLongClickToast(context) } }
        item { CardWithNestedImageDemo() }
        item { CardWithMultipleImagesDemo() }
        item { VerticallyCenteredBaseCard() }
        item { CardFillContentSample() }
    }
}

@Composable
fun OutlinedCardDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Outlined Card") } }
        item { OutlinedCardSample() }
        item { NonClickableOutlinedCardSample() }
        item {
            OutlinedCard(onClick = { /* Do something */ }, enabled = false) {
                Text("Disabled Outlined")
            }
        }
    }
}

@Composable
fun AppCardDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("App card") } }
        item { AppCardSample() }
        item { NonClickableAppCardSample() }
        item { AppCardDisabledDemo() }
        item { AppCardWithIconSample() }
        item { AppCardWithImageSample() }
        item { AppCardWithMultipleImagesDemo() }
        item { OutlinedAppCardSample() }
    }
}

@Composable
fun TitleCardDemo() {
    ScalingLazyDemo {
        item { ListHeader { Text("Title card") } }
        item { TitleCardSample() }
        item { NonClickableTitleCardSample() }
        item { TitleCardWithSubtitleDemo() }
        item { TitleCardWithSubtitleAndTimeSample() }
        item { TitleCardWithContentSubtitleAndTimeDemo() }
        item { TitleCardWithImageDemo() }
        item { TitleCardWithMultipleImagesSample() }
        item { OutlinedTitleCardSample() }
        item { OutlinedTitleCardWithSubtitleDemo() }
        item { OutlinedTitleCardWithSubtitleAndTimeDemo() }
    }
}

@Composable
fun VerticallyCenteredBaseCard() {
    // Provide a demo of a base Card with vertically centered content
    Card(onClick = {}) {
        Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(
                "ABCD",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun CardWithNestedImageDemo() {
    Card(onClick = { /* Do something */ }) {
        Spacer(Modifier.height(4.dp))
        ImageContent()
    }
}

@Composable
fun CardWithMultipleImagesDemo() {
    Card(onClick = { /* Do something */ }) {
        Spacer(Modifier.height(4.dp))
        MultipleImagesContent()
    }
}

@Composable
fun AppCardDisabledDemo() {
    AppCard(
        onClick = { /* Do something */ },
        appName = { Text("App name") },
        title = { Text("Card title") },
        time = { Text("Now") },
        enabled = false,
    ) {
        Text("Card content")
        Text("Disabled state")
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
fun ImageCardBuilder() {
    var alignment by remember { mutableStateOf(Alignment.Center) }
    var contentScale by remember { mutableStateOf(ContentScale.Fit) }
    var alpha by remember { mutableFloatStateOf(1f) }
    var sizeToIntrinsics by remember { mutableStateOf(false) }

    ScalingLazyDemo {
        item { ListHeader { Text("Image Card") } }

        item { ListSubHeader { Text("Alignment") } }
        val alignments =
            listOf(
                "Top Start" to Alignment.TopStart,
                "Top Center" to Alignment.TopCenter,
                "Center Start" to Alignment.CenterStart,
                "Center" to Alignment.Center,
                "CenterEnd" to Alignment.CenterEnd,
                "Bottom Center" to Alignment.BottomCenter,
                "Bottom End" to Alignment.BottomEnd,
            )
        items(alignments.size) {
            val (label, alignmentValue) = alignments[it]
            RadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = alignment == alignmentValue,
                onSelect = { alignment = alignmentValue },
                label = { Text(label) },
            )
        }

        item { ListSubHeader { Text("Content Scale") } }
        val contentScales =
            listOf(
                "Crop" to ContentScale.Crop,
                "Fit" to ContentScale.Fit,
                "Inside" to ContentScale.Inside,
                "None" to ContentScale.None,
                "Fill Bounds" to ContentScale.FillBounds,
                "Fill Height" to ContentScale.FillHeight,
                "Fill Width" to ContentScale.FillWidth,
                "Fixed X2" to FixedScale(2f),
            )
        items(contentScales.size) {
            val (label, contentScaleValue) = contentScales[it]
            RadioButton(
                modifier = Modifier.fillMaxWidth(),
                selected = contentScale == contentScaleValue,
                onSelect = { contentScale = contentScaleValue },
                label = { Text(label) },
            )
        }

        item { ListSubHeader { Text("Alpha=$alpha") } }
        item {
            Slider(
                value = alpha,
                onValueChange = { alpha = it },
                valueRange = 0f..1f,
                steps = 99,
                segmented = false,
            )
        }

        item { ListSubHeader { Text("Intrinsic size") } }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth(),
                checked = sizeToIntrinsics,
                onCheckedChange = { sizeToIntrinsics = it },
                label = { Text("Used") },
            )
        }

        item { ListHeader { Text("Image Card") } }
        item {
            val painter =
                painterResource(
                    androidx.wear.compose.material3.demos.R.drawable.backgroundsplitimage
                )

            TitleCard(
                onClick = { /* Do something */ },
                title = { Text("Title") },
                subtitle = { Text("Secondary label") },
                containerPainter =
                    CardDefaults.containerPainter(
                        image =
                            painterResource(
                                androidx.wear.compose.material3.demos.R.drawable
                                    .backgroundsplitimage
                            ),
                        sizeToIntrinsics = sizeToIntrinsics,
                        alignment = alignment,
                        contentScale = contentScale,
                        alpha = alpha,
                    ),
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            ) {
                Text("Content that could go over a few lines")
            }
        }
    }
}

@Composable
fun OutlinedTitleCardWithSubtitleAndTimeDemo() {
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
        subtitle = { Text("Subtitle") },
    )
}

@Composable
fun TitleCardWithContentSubtitleAndTimeDemo() {
    TitleCard(
        onClick = { /* Do something */ },
        time = { Text("now") },
        title = { Text("Title card") },
        subtitle = { Text("Subtitle") },
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
        modifier = Modifier.semantics { contentDescription = "Background image" },
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
            contentDescription = "Large blank image",
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
            contentDescription = "Medium blank image",
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
            contentDescription = "Small blank image",
        )
    }
}
