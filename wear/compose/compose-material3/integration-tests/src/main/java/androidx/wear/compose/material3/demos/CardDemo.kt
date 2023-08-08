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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.AppCardSample
import androidx.wear.compose.material3.samples.AppCardWithIconSample
import androidx.wear.compose.material3.samples.CardSample
import androidx.wear.compose.material3.samples.OutlinedAppCardSample
import androidx.wear.compose.material3.samples.OutlinedCardSample
import androidx.wear.compose.material3.samples.OutlinedTitleCardSample
import androidx.wear.compose.material3.samples.R
import androidx.wear.compose.material3.samples.TitleCardSample
import androidx.wear.compose.material3.samples.TitleCardWithImageSample

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
        item { OutlinedAppCardSample() }

        item { ListHeader { Text("Title card") } }
        item { TitleCardSample() }
        item { OutlinedTitleCardSample() }

        item { ListHeader { Text("Image card") } }
        item {
            AppCard(
                onClick = { /* Do something */ },
                appName = { Text("App name") },
                appImage = {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "favourites",
                        modifier = Modifier.size(CardDefaults.AppImageSize)
                    )
                },
                title = { Text("Card title") },
                time = { Text("now") },
                colors = CardDefaults.imageCardColors(
                    containerPainter = CardDefaults.imageWithScrimBackgroundPainter(
                        backgroundImagePainter = painterResource(id = R.drawable.backgroundimage)
                    ),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    titleColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.semantics { contentDescription = "Background image" }
            ) {
                Text("Card content")
            }
        }
        item { TitleCardWithImageSample() }
    }
}
