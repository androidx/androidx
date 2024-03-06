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

package androidx.compose.material3.samples

import androidx.annotation.DrawableRes
import androidx.annotation.Sampled
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.startOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun HorizontalMultiBrowseCarouselSample() {

    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int
    )

    val items = listOf(
        CarouselItem(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
        CarouselItem(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
        CarouselItem(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
        CarouselItem(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
        CarouselItem(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
    )

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier
            .width(412.dp)
            .height(221.dp),
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        Card(
            modifier = Modifier
                .height(205.dp)
        ) {
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = stringResource(item.contentDescriptionResId),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun HorizontalUncontainedCarouselSample() {

    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int
    )

    val items = listOf(
        CarouselItem(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
        CarouselItem(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
        CarouselItem(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
        CarouselItem(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
        CarouselItem(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
    )
    HorizontalUncontainedCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier
            .width(412.dp)
            .height(221.dp),
        itemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        Card(
            modifier = Modifier
                .height(205.dp)
        ) {
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = stringResource(item.contentDescriptionResId),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Sampled
@Composable
fun FadingHorizontalMultiBrowseCarouselSample() {

    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int
    )

    val items = listOf(
        CarouselItem(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
        CarouselItem(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
        CarouselItem(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
        CarouselItem(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
        CarouselItem(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
    )
    val state = rememberCarouselState { items.count() }
    HorizontalMultiBrowseCarousel(
        state = state,
        modifier = Modifier
            .width(412.dp)
            .height(221.dp),
        preferredItemWidth = 130.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        Card(
            modifier = Modifier
                .height(205.dp)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = carouselItemInfo.size / carouselItemInfo.maxSize
                    }
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = "sample text",
                    modifier = Modifier.graphicsLayer {
                        translationX = carouselItemInfo.startOffset()
                    }
                )
            }
        }
    }
}
