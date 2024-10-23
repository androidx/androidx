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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.AppCard
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedCard
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen
import androidx.wear.compose.material3.macrobenchmark.common.R
import androidx.wear.compose.material3.macrobenchmark.common.scrollDown

val CardScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Card(
                        onClick = {},
                        onLongClick = {},
                        onLongClickLabel = "Long click",
                        colors = CardDefaults.cardColors()
                    ) {
                        Text("Card")
                    }
                    OutlinedCard(onClick = {}) { Text("Outlined card") }
                    AppCard(
                        onClick = {},
                        appName = { Text("AppName") },
                        title = {},
                        time = { Text("02:34") },
                        appImage = {
                            Icon(
                                painter = painterResource(id = android.R.drawable.star_big_off),
                                contentDescription = "Star icon",
                                modifier =
                                    Modifier.size(CardDefaults.AppImageSize)
                                        .wrapContentSize(align = Alignment.Center),
                            )
                        },
                    ) {
                        Text("AppCard")
                    }
                    TitleCard(
                        onClick = {},
                        title = { Text("Title") },
                        subtitle = { Text("Subtitle") },
                        colors =
                            CardDefaults.imageCardColors(
                                containerPainter =
                                    CardDefaults.imageWithScrimBackgroundPainter(
                                        backgroundImagePainter =
                                            painterResource(id = R.drawable.backgroundimage)
                                    ),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                titleColor = MaterialTheme.colorScheme.onSurface
                            ),
                    ) {
                        Text("TitleCard")
                    }
                    TitleCard(
                        onClick = { /* Do something */ },
                        title = { Text("Card title") },
                        time = { Text("now") },
                        colors =
                            CardDefaults.imageCardColors(
                                containerPainter =
                                    CardDefaults.imageWithScrimBackgroundPainter(
                                        backgroundImagePainter =
                                            painterResource(id = R.drawable.backgroundimage)
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
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = { device.scrollDown() }
    }
