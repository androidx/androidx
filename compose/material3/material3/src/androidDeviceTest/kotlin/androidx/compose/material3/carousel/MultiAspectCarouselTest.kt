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

package androidx.compose.material3.carousel

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.GOLDEN_MATERIAL3
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.samples.R
import androidx.compose.material3.setMaterialContent
import androidx.compose.runtime.remember
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class MultiAspectCarouselTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun carouselRow_default() {
        rule.setMaterialContent(lightColorScheme()) {
            MultiAspectCarouselScope {
                val state = rememberLazyListState()
                LazyRow(
                    state = state,
                    modifier =
                        Modifier.width(412.dp).height(221.dp).testTag(MultiAspectCarouselTestTag),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(items) { i, item ->
                        val drawInfo = remember { MultiAspectCarouselItemDrawInfo(i, state) }
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

        assertAgainstGolden("default")
    }

    @Test
    fun horizontalGrid_singleRow() {
        rule.setMaterialContent(lightColorScheme()) {
            MultiAspectCarouselScope {
                val state = rememberLazyGridState()
                LazyHorizontalGrid(
                    rows = GridCells.Fixed(1),
                    modifier = Modifier.requiredHeight(221.dp).testTag(MultiAspectCarouselTestTag),
                    state = state,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(items) { i, item ->
                        val drawInfo = remember { MultiAspectCarouselItemDrawInfo(i, state) }
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

        assertAgainstGolden("horizontalGrid_singleRow")
    }

    @Test
    fun verticalGrid_twoColumns() {
        rule.setMaterialContent(lightColorScheme()) {
            MultiAspectCarouselScope {
                val state = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.testTag(MultiAspectCarouselTestTag),
                    state = state,
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(items) { i, item ->
                        val drawInfo = remember { MultiAspectCarouselItemDrawInfo(i, state) }
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = stringResource(item.contentDescriptionResId),
                            modifier =
                                Modifier.width(item.mainAxisSize)
                                    .height(300.dp)
                                    .maskClip(MaterialTheme.shapes.extraLarge, drawInfo),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }
        }

        assertAgainstGolden("verticalGrid_twoColumns")
    }

    private fun assertAgainstGolden(goldenIdentifier: String) {
        rule
            .onNodeWithTag(MultiAspectCarouselTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "multi_aspect_carousel_$goldenIdentifier")
    }

    private val items =
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

    data class CarouselItem(
        val id: Int,
        @DrawableRes val imageResId: Int,
        @StringRes val contentDescriptionResId: Int,
        val mainAxisSize: Dp,
    )
}

internal const val MultiAspectCarouselTestTag = "multi_aspect_carousel"
