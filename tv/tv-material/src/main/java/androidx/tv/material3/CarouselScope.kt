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

package androidx.tv.material3

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * CarouselScope provides a [CarouselScope.CarouselItem] function which you can use to
 * provide the slide's animation, background and the inner content.
 */
@ExperimentalTvMaterial3Api
class CarouselScope @OptIn(ExperimentalTvMaterial3Api::class)
internal constructor(private val carouselState: CarouselState) {
    /**
     * [CarouselScope.CarouselItem] can be used to define a slide's animation, background, and
     * content. Using this is optional and you can choose to define your own CarouselItem from
     * scratch
     *
     * @param modifier modifier applied to the CarouselItem
     * @param background composable defining the background of the slide
     * @param contentTransformForward content transform to be applied to the content of the slide
     * when scrolling forward in the carousel
     * @param contentTransformBackward content transform to be applied to the content of the slide
     * when scrolling backward in the carousel
     * @param content composable defining the content displayed on top of the background
     */
    @Composable
    @Suppress("IllegalExperimentalApiUsage")
    @OptIn(ExperimentalAnimationApi::class)
    @ExperimentalTvMaterial3Api
    fun CarouselItem(
        modifier: Modifier = Modifier,
        background: @Composable () -> Unit = {},
        contentTransformForward: ContentTransform =
            CarouselItemDefaults.contentTransformForward,
        contentTransformBackward: ContentTransform =
            CarouselItemDefaults.contentTransformBackward,
        content: @Composable () -> Unit
    ) {
        CarouselItem(
            background = background,
            slideIndex = carouselState.activeSlideIndex,
            contentTransform =
            if (carouselState.isMovingBackward)
                contentTransformBackward
            else
                contentTransformForward,
            modifier = modifier,
            content = content,
        )
    }
}
