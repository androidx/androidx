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

package androidx.tv.tvmaterial.samples

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Cyan
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.graphics.Color.Companion.Yellow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material.ExperimentalTvMaterialApi
import androidx.tv.material.carousel.Carousel
import androidx.tv.material.carousel.CarouselItem
import androidx.tv.material.carousel.CarouselState

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
fun FeaturedCarousel() {
    val carouselState = remember { CarouselState(0) }
    LazyColumn {
        item {
            Carousel(
                modifier = Modifier
                    .height(400.dp)
                    .width(950.dp),
                carouselState = carouselState,
                slideCount = 3
            ) { SampleFrame(it) }
        }

        items(7) { SampleLazyRow() }
    }
}

@OptIn(ExperimentalTvMaterialApi::class)
@Composable
fun SampleFrame(idx: Int) {
    val item = mediaItems[idx]

    CarouselItem(
        background = {
            Box(
                Modifier.background(item.backgroundColor).fillMaxSize()
            )
        }) {
        Box {
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(0.dp, 8.dp, 0.dp, 0.dp)
                )

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    SampleButton(text = "PLAY")
                    SampleButton(text = "INFO")
                }
            }
        }
    }
}

@Composable
fun SampleButton(text: String) {
    var cardScale
        by remember { mutableStateOf(0.5f) }
    val borderGlowColorTransition =
        rememberInfiniteTransition()
    var initialValue
        by remember { mutableStateOf(Color.Transparent) }
    val glowingColor
        by borderGlowColorTransition.animateColor(
            initialValue = initialValue,
            targetValue = Color.Transparent,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

    Button(
        onClick = {},
        modifier = Modifier
            .scale(cardScale)
            .border(
                2.dp, glowingColor,
                RoundedCornerShape(12.dp)
            )
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    cardScale = 1.0f
                    initialValue = Color.White
                } else {
                    cardScale = 0.5f
                    initialValue = Color.Transparent
                }
            }) {
        Text(text = text)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleLazyRow() {
    LazyRow(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)) {
        items((1..10).map { it.toString() }) {
            var cardScale by remember { mutableStateOf(0.5f) }
            val borderGlowColorTransition = rememberInfiniteTransition()
            var initialValue by remember { mutableStateOf(Color.Transparent) }
            val glowingColor by borderGlowColorTransition.animateColor(
                initialValue = initialValue,
                targetValue = Color.Transparent,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Card(
                modifier = Modifier
                    .width(100.dp)
                    .height(100.dp)
                    .scale(cardScale)
                    .border(2.dp, glowingColor, RoundedCornerShape(12.dp))
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            cardScale = 1.0f
                            initialValue = Color.White
                        } else {
                            cardScale = 0.5f
                            initialValue = Color.Transparent
                        }
                    }
                    .focusable()
            ) {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(12.dp),
                    color = Red,
                    fontWeight = FontWeight.Bold

                )
            }
        }
    }
}

val mediaItems = listOf(
    Media(id = "1", title = "Title 1", description = "Description 1", backgroundColor = Gray),
    Media(id = "2", title = "Title 2", description = "Description 2", backgroundColor = Yellow),
    Media(id = "3", title = "Title 3", description = "Description 3", backgroundColor = Cyan)
)
