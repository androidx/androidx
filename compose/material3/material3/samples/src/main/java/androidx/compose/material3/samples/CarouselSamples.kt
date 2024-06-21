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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.max

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

    val items =
        listOf(
            CarouselItem(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
            CarouselItem(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
            CarouselItem(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
            CarouselItem(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
            CarouselItem(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
        )

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier.width(412.dp).height(221.dp),
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        Image(
            modifier = Modifier.height(205.dp).maskClip(MaterialTheme.shapes.extraLarge),
            painter = painterResource(id = item.imageResId),
            contentDescription = stringResource(item.contentDescriptionResId),
            contentScale = ContentScale.Crop
        )
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

    val items =
        listOf(
            CarouselItem(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
            CarouselItem(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
            CarouselItem(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
            CarouselItem(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
            CarouselItem(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
        )
    HorizontalUncontainedCarousel(
        state = rememberCarouselState { items.count() },
        modifier = Modifier.width(412.dp).height(221.dp),
        itemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        Image(
            modifier = Modifier.height(205.dp).maskClip(MaterialTheme.shapes.extraLarge),
            painter = painterResource(id = item.imageResId),
            contentDescription = stringResource(item.contentDescriptionResId),
            contentScale = ContentScale.Crop
        )
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

    val items =
        listOf(
            CarouselItem(0, R.drawable.carousel_image_1, R.string.carousel_image_1_description),
            CarouselItem(1, R.drawable.carousel_image_2, R.string.carousel_image_2_description),
            CarouselItem(2, R.drawable.carousel_image_3, R.string.carousel_image_3_description),
            CarouselItem(3, R.drawable.carousel_image_4, R.string.carousel_image_4_description),
            CarouselItem(4, R.drawable.carousel_image_5, R.string.carousel_image_5_description),
        )
    val state = rememberCarouselState { items.count() }
    HorizontalMultiBrowseCarousel(
        state = state,
        modifier = Modifier.width(412.dp).height(221.dp),
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        // For item 1 and 4, create a stacked item layout that clips two images independently
        // to the item's mask
        if (i == 1 || i == 4) {
            Column(
                modifier = Modifier.height(205.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    modifier =
                        Modifier.fillMaxWidth()
                            .fillMaxHeight(.5f)
                            .maskClip(MaterialTheme.shapes.extraLarge)
                            .maskBorder(
                                BorderStroke(3.dp, Color.Magenta),
                                MaterialTheme.shapes.extraLarge
                            ),
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    contentScale = ContentScale.Crop
                )
                Image(
                    modifier =
                        Modifier.fillMaxSize()
                            .maskClip(RoundedCornerShape(8.dp))
                            .maskBorder(BorderStroke(5.dp, Color.Green), RoundedCornerShape(8.dp)),
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            // Mask using a generic path shape
            val pathShape = remember {
                object : Shape {
                    override fun createOutline(
                        size: Size,
                        layoutDirection: LayoutDirection,
                        density: Density
                    ): Outline {
                        val roundRect =
                            RoundRect(0f, 0f, size.width, size.height, CornerRadius(30f))
                        val shapePath = Path().apply { addRoundRect(roundRect) }
                        return Outline.Generic(shapePath)
                    }
                }
            }
            Box(
                modifier =
                    Modifier.height(205.dp)
                        .maskClip(pathShape)
                        .maskBorder(BorderStroke(5.dp, Color.Red), pathShape),
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                ElevatedAssistChip(
                    onClick = { /* Do something! */ },
                    label = { Text("Image $i") },
                    modifier =
                        Modifier.graphicsLayer {
                            // Fade the chip in once the carousel item's size is large enough to
                            // display the entire chip
                            alpha =
                                lerp(
                                    0f,
                                    1f,
                                    max(
                                        size.width - (carouselItemInfo.maxSize) +
                                            carouselItemInfo.size,
                                        0f
                                    ) / size.width
                                )
                            // Translate the chip to be pinned to the left side of the item's mask
                            translationX = carouselItemInfo.maskRect.left + 8.dp.toPx()
                        },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Image,
                            contentDescription = "Localized description",
                            Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
            }
        }
    }
}
