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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TitleCard

@Composable
fun CardDemo() {
    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Basic unopinionated chip")
                    Text("Sets the shape")
                    Text("and the background")
                }
            }
        }
        item {
            AppCard(
                onClick = {},
                appName = { Text("AppName") },
                title = { Text("AppCard") },
                time = { Text("now") },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Some body content")
                    Text("and some more body content")
                }
            }
        }
        item {
            AppCard(
                onClick = {},
                appName = { Text("AppName") },
                appImage = { DemoImage(resourceId = R.drawable.ic_maps_icon) },
                title = { Text("AppCard") },
                time = { Text("now") },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Some body content")
                    Text("and some more body content")
                }
            }
        }
        item {
            TitleCard(
                onClick = {},
                title = { Text("TitleCard") },
                time = { Text("now") },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Some body content")
                    Text("and some more body content")
                }
            }
        }
        item {
            TitleCard(
                onClick = {},
                title = { Text("TitleCard") },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This title card doesn't show time")
                }
            }
        }
        item {
            TitleCard(
                onClick = {},
                title = { Text("Custom TitleCard") },
                titleColor = Color.Yellow
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("This title card emphasises the title with custom color")
                }
            }
        }
        item {
            TitleCard(
                onClick = {},
                title = { Text("TitleCard With an ImageBackground") },
                backgroundPainter = CardDefaults.imageWithScrimBackgroundPainter(
                    backgroundImagePainter = painterResource(id = R.drawable.backgroundimage1)
                ),
                contentColor = MaterialTheme.colors.onSurface,
                titleColor = MaterialTheme.colors.onSurface,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Text coloured to stand out on the image")
                }
            }
        }
    }
}