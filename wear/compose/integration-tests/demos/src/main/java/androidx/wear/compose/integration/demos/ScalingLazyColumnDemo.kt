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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import kotlin.math.roundToInt

@Composable
fun GuideLines() {
    Canvas(
        modifier = Modifier
            .fillMaxSize(),
        onDraw = {
            for (i in 1..9) {
                drawLine(
                    Color.Magenta, Offset(0f, (size.height / 10f) * i),
                    Offset(size.width - 1, (size.height / 10f) * i)
                )
            }
        })
}

@Composable
fun ScalingLazyColumnDetail() {
    val state = rememberScalingLazyListState()
    val applicationContext = LocalContext.current
    val screenHeightPx = with(LocalDensity.current) {
        Dp(applicationContext.resources.configuration.screenHeightDp.toFloat()).roundToPx()
    }
    val halfScreenHeightPx = screenHeightPx / 2f
    ScalingLazyColumn(state = state) {
        item {
            ListHeader {
                Text(text = "Screen height :${screenHeightPx}px")
            }
        }
        items(20, key = { ix -> ix }) { ix ->
            val item = state.layoutInfo.visibleItemsInfo.find { i -> i.index == ix + 1 }
            var description = "$ix"
            if (item != null) {
                val unScaledSize = (item.size / item.scale).roundToInt()
                val itemStart = item.offset - item.size / 2f + halfScreenHeightPx
                val itemEnd = itemStart + item.size
                description = description + ": Start:${itemStart.roundToInt()} " +
                    "End:${itemEnd.roundToInt()} Size:${item.size}/${unScaledSize}px " +
                    "Scale:${item.scale}"
            }
            Chip(
                onClick = {},
                colors = ChipDefaults.secondaryChipColors()) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = description,
                    style = MaterialTheme.typography.caption3)
            }
        }
    }
}

@Composable
fun ScalingLazyColumnMixedTypes() {
    ScalingLazyColumn {
        item {
            ListHeader { Text("Activity") }
        }
        item {
            Text(
                text = "2 hours of listening 400mb of 2.8gb",
                textAlign = TextAlign.Center
            )
        }
        item {
            DemoIconChip(
                label = "App Title",
                secondaryLabel = "Defaults",
                colors = ChipDefaults.secondaryChipColors(),
            ) {
                DemoImage(resourceId = R.drawable.ic_maps_icon)
            }
        }
        item {
            AppCard(
                onClick = {},
                appName = { Text("AppName") },
                appImage = {
                    DemoImage(
                        resourceId = R.drawable.ic_maps_icon,
                        size = CardDefaults.AppImageSize
                    )
                },
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
            DemoIconChip(
                label = "App Title",
                secondaryLabel = "Defaults",
                colors = ChipDefaults.secondaryChipColors(),
            ) {
                DemoImage(resourceId = R.drawable.ic_maps_icon)
            }
        }
        item {
            ListHeader { Text("Activity") }
        }
        item {
            AppCard(
                onClick = {},
                appName = { Text("AppName") },
                appImage = {
                    DemoImage(
                        resourceId = R.drawable.ic_maps_icon,
                        size = CardDefaults.AppImageSize
                    )
                },
                title = { Text("AppCard") },
                time = { Text("now") },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do " +
                            "eiusmod tempor incididunt ut labore et dolore magna aliqua. Lorem " +
                            "ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod " +
                            "tempor incididunt ut labore et dolore magna aliqua."
                    )
                }
            }
        }
        item {
            Text(
                text = "2 hours of listening 400mb of 2.8gb",
                textAlign = TextAlign.Center
            )
        }
        item {
            AppCard(
                onClick = {},
                appName = { Text("AppName") },
                appImage = {
                    DemoImage(
                        resourceId = R.drawable.ic_maps_icon,
                        size = CardDefaults.AppImageSize
                    )
                },
                title = { Text("AppCard") },
                time = { Text("now") },
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Some body content")
                }
            }
        }
    }
}
