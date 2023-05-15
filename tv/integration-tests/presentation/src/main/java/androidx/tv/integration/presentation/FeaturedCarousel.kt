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

package androidx.tv.integration.presentation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeaturedCarousel(
    movies: List<Movie> = featuredCarouselMovies,
    modifier: Modifier = Modifier
) {
    val carouselState: CarouselState = remember { CarouselState() }
    val slidesCount = movies.size

    Carousel(
        itemCount = slidesCount,
        carouselState = carouselState,
        modifier = modifier
            .height(340.dp)
            .fillMaxWidth(),
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                itemCount = slidesCount,
                activeItemIndex = carouselState.activeItemIndex,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 58.dp, bottom = 16.dp),
            )
        },
        contentTransformEndToStart =
            fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000))),
        contentTransformStartToEnd =
            fadeIn(tween(1000)).togetherWith(fadeOut(tween(1000)))
    ) { itemIndex ->
        val movie = movies[itemIndex]

        CarouselSlide(
            title = movie.name,
            description = movie.description,
            background = {
                LandscapeImageBackground(movie)
            },
            actions = {
                AppButton(
                    text = "Watch on YouTube",
                    icon = Icons.Outlined.ArrowRight,
                )
            },
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun AnimatedContentScope.CarouselSlide(
    title: String,
    description: String,
    background: @Composable () -> Unit,
    actions: @Composable () -> Unit
) {
    Box {
        background()
        Column(
            modifier = Modifier
                .padding(start = 58.dp, top = 150.dp)
                .animateEnterExit(
                    enter = slideInHorizontally(animationSpec = tween(1000)) { it / 2 },
                    exit = slideOutHorizontally(animationSpec = tween(1000))
                )
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 40.sp
            )

            AppSpacer(height = 16.dp)

            Text(
                text = description,
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                modifier = Modifier.width(500.dp),
            )

            AppSpacer(height = 15.dp)

            actions()
        }
    }
}
