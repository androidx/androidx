/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.glimmer.demos

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.xr.glimmer.Button
import androidx.xr.glimmer.Card
import androidx.xr.glimmer.GlimmerTheme
import androidx.xr.glimmer.Icon
import androidx.xr.glimmer.LocalTextStyle
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.pager.GlimmerHorizontalPager
import androidx.xr.glimmer.pager.rememberGlimmerPagerState
import androidx.xr.glimmer.samples.GlimmerHorizontalPagerSample

internal val PagerDemos =
    listOf(
        ComposableDemo("Pager with one-line cards") { GlimmerHorizontalPagerSample() },
        ComposableDemo("Pager with various content") {
            GlimmerHorizontalPagerWithVariousContentDemo()
        },
    )

@Composable
private fun GlimmerHorizontalPagerWithVariousContentDemo() {
    val pagerState = rememberGlimmerPagerState(pageCount = { 15 })
    GlimmerHorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (page % 4) {
            0 ->
                Card {
                    Text(
                        text = "Page: $page",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            1 ->
                Card(
                    title = {
                        Text(
                            text = "Page: $page",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    },
                    header = {
                        Image(
                            painter = SampleImage,
                            contentDescription = "Localized description",
                            contentScale = ContentScale.FillWidth,
                        )
                    },
                ) {
                    Text(
                        text = "This is a card with a title and header image",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            2 ->
                Card(
                    title = {
                        Text(
                            text = "Page: $page",
                            style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                        )
                    },
                    action = {
                        Button(
                            onClick = {},
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.SendIcon,
                                    contentDescription = "Send icon",
                                    modifier = Modifier.size(GlimmerTheme.iconSizes.medium),
                                )
                            },
                        ) {
                            Text(
                                text = "Send",
                                style =
                                    LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                            )
                        }
                    },
                ) {
                    Text(
                        text = "This is a card with a title and action button.",
                        style = LocalTextStyle.current.copy(textMotion = TextMotion.Animated),
                    )
                }
            3 ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        modifier = Modifier.weight(0.4f),
                        painter = SampleImage,
                        contentDescription = "Localized description",
                        contentScale = ContentScale.FillWidth,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(0.6f)) {
                        Text(
                            text = "Page: $page",
                            style =
                                GlimmerTheme.typography.titleMedium.copy(
                                    textMotion = TextMotion.Animated
                                ),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Body small text",
                            style =
                                GlimmerTheme.typography.bodySmall.copy(
                                    textMotion = TextMotion.Animated
                                ),
                        )
                    }
                }
        }
    }
}
