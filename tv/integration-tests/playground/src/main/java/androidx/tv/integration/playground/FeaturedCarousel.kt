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

package androidx.tv.integration.playground

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api

@Composable
fun FeaturedCarouselContent() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        items(3) { SampleLazyRow() }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .background(Color.Magenta.copy(alpha = 0.3f))
                                .width(50.dp)
                                .height(50.dp)
                                .drawBorderOnFocus()
                                .focusable()
                        )
                    }
                }

                FeaturedCarousel(Modifier.weight(1f))

                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .background(Color.Magenta.copy(alpha = 0.3f))
                                .width(50.dp)
                                .height(50.dp)
                                .drawBorderOnFocus()
                                .focusable()
                        )
                    }
                }
            }
        }
        items(2) { SampleLazyRow() }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
internal fun FeaturedCarousel(modifier: Modifier = Modifier) {
    val backgrounds = listOf(
        Color.Red.copy(alpha = 0.3f),
        Color.Yellow.copy(alpha = 0.3f),
        Color.Green.copy(alpha = 0.3f),
        Color.Blue.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.3f),
        Color.Magenta.copy(alpha = 0.3f),
        Color.DarkGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.3f),
    )

    val carouselState = remember { CarouselState() }
    Carousel(
        itemCount = backgrounds.size,
        carouselState = carouselState,
        modifier = modifier
            .height(300.dp)
            .fillMaxWidth(),
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                itemCount = backgrounds.size,
                activeItemIndex = carouselState.activeItemIndex,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    ) { itemIndex ->
        CarouselItem(
            modifier = Modifier.semantics {
                contentDescription = "Featured Content"
            },
            background = {
                Box(
                    modifier = Modifier
                        .background(backgrounds[itemIndex])
                        .fillMaxSize()
                )
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(text = "This is sample text content.", color = Color.Yellow)
                    Text(text = "Sample description.", color = Color.Yellow)
                    Row {
                        OverlayButton(text = "Play")
                        OverlayButton(text = "Add to Watchlist")
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayButton(modifier: Modifier = Modifier, text: String = "Play") {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = { },
        modifier = modifier
            .onFocusChanged {
                isFocused = it.isFocused
            }
            .padding(20.dp)
            .border(
                width = 2.dp,
                color = if (isFocused) Color.Red else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .padding(vertical = 2.dp, horizontal = 5.dp)
    ) {
        Text(text = text)
    }
}
