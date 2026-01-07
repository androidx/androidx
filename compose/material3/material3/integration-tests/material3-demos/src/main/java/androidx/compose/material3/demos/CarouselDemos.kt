/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.demos

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.MultiAspectCarouselItemDrawInfo
import androidx.compose.material3.carousel.MultiAspectCarouselScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAspectCarouselLazyColumnDemo() {
    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int,
        val mainAxisSize: Dp,
    )

    val items =
        listOf(
            CarouselItem(
                0,
                R.drawable.carousel_image_1,
                R.string.carousel_image_1_description,
                305.dp,
            ),
            CarouselItem(
                1,
                R.drawable.carousel_image_2,
                R.string.carousel_image_2_description,
                205.dp,
            ),
            CarouselItem(
                2,
                R.drawable.carousel_image_3,
                R.string.carousel_image_3_description,
                275.dp,
            ),
            CarouselItem(
                3,
                R.drawable.carousel_image_4,
                R.string.carousel_image_4_description,
                350.dp,
            ),
            CarouselItem(
                4,
                R.drawable.carousel_image_5,
                R.string.carousel_image_5_description,
                100.dp,
            ),
            CarouselItem(
                5,
                R.drawable.carousel_image_1,
                R.string.carousel_image_1_description,
                100.dp,
            ),
            CarouselItem(
                6,
                R.drawable.carousel_image_2,
                R.string.carousel_image_2_description,
                225.dp,
            ),
            CarouselItem(
                7,
                R.drawable.carousel_image_3,
                R.string.carousel_image_3_description,
                85.dp,
            ),
            CarouselItem(
                8,
                R.drawable.carousel_image_4,
                R.string.carousel_image_4_description,
                175.dp,
            ),
            CarouselItem(
                9,
                R.drawable.carousel_image_5,
                R.string.carousel_image_5_description,
                300.dp,
            ),
        )

    MultiAspectCarouselScope {
        val state = rememberLazyListState()
        LazyColumn(
            state = state,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        ) {
            itemsIndexed(items) { i, item ->
                val drawInfo = remember { MultiAspectCarouselItemDrawInfo(i, state) }
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    modifier =
                        Modifier.height(item.mainAxisSize)
                            .fillMaxWidth()
                            .maskClip(MaterialTheme.shapes.extraLarge, drawInfo),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FadingMultiAspectCarouselLazyRowDemo() {
    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int,
        val mainAxisSize: Dp,
    )

    val items =
        listOf(
            CarouselItem(
                0,
                R.drawable.carousel_image_1,
                R.string.carousel_image_1_description,
                305.dp,
            ),
            CarouselItem(
                1,
                R.drawable.carousel_image_2,
                R.string.carousel_image_2_description,
                205.dp,
            ),
            CarouselItem(
                2,
                R.drawable.carousel_image_3,
                R.string.carousel_image_3_description,
                275.dp,
            ),
            CarouselItem(
                3,
                R.drawable.carousel_image_4,
                R.string.carousel_image_4_description,
                350.dp,
            ),
            CarouselItem(
                4,
                R.drawable.carousel_image_5,
                R.string.carousel_image_5_description,
                100.dp,
            ),
        )

    MultiAspectCarouselScope {
        val state = rememberLazyListState()
        LazyRow(
            state = state,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(221.dp),
        ) {
            itemsIndexed(items) { i, item ->
                val drawInfo = remember { MultiAspectCarouselItemDrawInfo(i, state) }
                if (i == 1) {
                    Column(
                        modifier = Modifier.height(205.dp).width(item.mainAxisSize),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Image(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .fillMaxHeight(.5f)
                                    .maskBorder(
                                        BorderStroke(3.dp, Color.Magenta),
                                        MaterialTheme.shapes.extraLarge,
                                        drawInfo,
                                    )
                                    .maskClip(MaterialTheme.shapes.extraLarge, drawInfo),
                            painter = painterResource(id = item.imageResId),
                            contentDescription = stringResource(item.contentDescriptionResId),
                            contentScale = ContentScale.Crop,
                        )
                        Image(
                            modifier =
                                Modifier.fillMaxSize().maskClip(RoundedCornerShape(8.dp), drawInfo),
                            painter = painterResource(id = item.imageResId),
                            contentDescription = stringResource(item.contentDescriptionResId),
                            contentScale = ContentScale.Crop,
                        )
                    }
                } else {
                    Box(
                        modifier =
                            Modifier.height(205.dp)
                                .width(item.mainAxisSize)
                                .maskClip(MaterialTheme.shapes.extraLarge, drawInfo)
                    ) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = stringResource(item.contentDescriptionResId),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        ElevatedAssistChip(
                            modifier =
                                Modifier.padding(all = 14.dp).graphicsLayer {
                                    alpha =
                                        1f -
                                            lerp(
                                                0f,
                                                1f,
                                                (drawInfo.maxSize - drawInfo.size) /
                                                    ((drawInfo.maxSize - drawInfo.minSize) * .5f),
                                            )
                                    translationX = drawInfo.maskStart
                                },
                            onClick = { /* Do nothing */ },
                            label = { Text("Image $i") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = "Localized description",
                                    Modifier.size(AssistChipDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAspectCarouselLazyHorizontalGridDemo() {
    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int,
        val mainAxisSize: Dp,
    )

    val items =
        listOf(
            CarouselItem(
                0,
                R.drawable.carousel_image_1,
                R.string.carousel_image_1_description,
                305.dp,
            ),
            CarouselItem(
                1,
                R.drawable.carousel_image_2,
                R.string.carousel_image_2_description,
                205.dp,
            ),
            CarouselItem(
                2,
                R.drawable.carousel_image_3,
                R.string.carousel_image_3_description,
                275.dp,
            ),
            CarouselItem(
                3,
                R.drawable.carousel_image_4,
                R.string.carousel_image_4_description,
                350.dp,
            ),
            CarouselItem(
                4,
                R.drawable.carousel_image_5,
                R.string.carousel_image_5_description,
                100.dp,
            ),
        )

    MultiAspectCarouselScope {
        val state = rememberLazyGridState()
        LazyHorizontalGrid(
            rows = GridCells.Fixed(1),
            modifier = Modifier.requiredHeight(221.dp),
            state = state,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items) { index, item ->
                val drawInfo = remember { MultiAspectCarouselItemDrawInfo(index, state) }
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    modifier =
                        Modifier.width(item.mainAxisSize)
                            .height(205.dp)
                            .maskClip(MaterialTheme.shapes.extraLarge, drawInfo),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiAspectCarouselLazyVerticalGridDemo() {
    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int,
        val mainAxisSize: Dp,
    )

    val items =
        listOf(
            CarouselItem(
                0,
                R.drawable.carousel_image_1,
                R.string.carousel_image_1_description,
                305.dp,
            ),
            CarouselItem(
                1,
                R.drawable.carousel_image_2,
                R.string.carousel_image_2_description,
                205.dp,
            ),
            CarouselItem(
                2,
                R.drawable.carousel_image_3,
                R.string.carousel_image_3_description,
                275.dp,
            ),
            CarouselItem(
                3,
                R.drawable.carousel_image_4,
                R.string.carousel_image_4_description,
                350.dp,
            ),
            CarouselItem(
                4,
                R.drawable.carousel_image_5,
                R.string.carousel_image_5_description,
                100.dp,
            ),
            CarouselItem(
                5,
                R.drawable.carousel_image_1,
                R.string.carousel_image_1_description,
                100.dp,
            ),
            CarouselItem(
                6,
                R.drawable.carousel_image_2,
                R.string.carousel_image_2_description,
                225.dp,
            ),
            CarouselItem(
                7,
                R.drawable.carousel_image_3,
                R.string.carousel_image_3_description,
                85.dp,
            ),
            CarouselItem(
                8,
                R.drawable.carousel_image_4,
                R.string.carousel_image_4_description,
                175.dp,
            ),
            CarouselItem(
                9,
                R.drawable.carousel_image_5,
                R.string.carousel_image_5_description,
                300.dp,
            ),
        )

    MultiAspectCarouselScope {
        val state = rememberLazyGridState()
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = state,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(items) { index, item ->
                val drawInfo = remember { MultiAspectCarouselItemDrawInfo(index, state) }
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = stringResource(item.contentDescriptionResId),
                    modifier =
                        Modifier.height(300.dp).maskClip(MaterialTheme.shapes.extraLarge, drawInfo),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
