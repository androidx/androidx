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

package androidx.tv.samples

import androidx.annotation.Sampled
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Sampled
@Composable
fun SimpleCarousel() {
    val backgrounds = listOf(
        Color.Red.copy(alpha = 0.3f),
        Color.Yellow.copy(alpha = 0.3f),
        Color.Green.copy(alpha = 0.3f)
    )

    Carousel(
        slideCount = backgrounds.size,
        modifier = Modifier
            .height(300.dp)
            .fillMaxWidth(),
    ) { itemIndex ->
        CarouselItem(
            background = {
                Box(
                    modifier = Modifier
                        .background(backgrounds[itemIndex])
                        .border(2.dp, Color.White.copy(alpha = 0.5f))
                        .fillMaxSize()
                )
            }
        ) {
            var isFocused by remember { mutableStateOf(false) }

            Button(
                onClick = { },
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(40.dp)
                    .border(
                        width = 2.dp,
                        color = if (isFocused) Color.Red else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 2.dp, horizontal = 5.dp)
            ) {
                Text(text = "Play")
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class, ExperimentalAnimationApi::class)
@Sampled
@Composable
fun CarouselIndicatorWithRectangleShape() {
    val backgrounds = listOf(
        Color.Red.copy(alpha = 0.3f),
        Color.Yellow.copy(alpha = 0.3f),
        Color.Green.copy(alpha = 0.3f)
    )
    val carouselState = remember { CarouselState() }

    Carousel(
        slideCount = backgrounds.size,
        modifier = Modifier
            .height(300.dp)
            .fillMaxWidth(),
        carouselState = carouselState,
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                slideCount = backgrounds.size,
                activeSlideIndex = carouselState.activeSlideIndex,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                indicator = { isActive ->
                    val activeColor = Color.Red
                    val inactiveColor = activeColor.copy(alpha = 0.5f)
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (isActive) activeColor else inactiveColor,
                                shape = RectangleShape,
                            ),
                    )
                }
            )
        }
    ) { itemIndex ->
        CarouselItem(
            background = {
                Box(
                    modifier = Modifier
                        .background(backgrounds[itemIndex])
                        .border(2.dp, Color.White.copy(alpha = 0.5f))
                        .fillMaxSize()
                )
            }
        ) {
            var isFocused by remember { mutableStateOf(false) }

            Button(
                onClick = { },
                modifier = Modifier
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(40.dp)
                    .border(
                        width = 2.dp,
                        color = if (isFocused) Color.Red else Color.Transparent,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 2.dp, horizontal = 5.dp)
            ) {
                Text(text = "Play")
            }
        }
    }
}
