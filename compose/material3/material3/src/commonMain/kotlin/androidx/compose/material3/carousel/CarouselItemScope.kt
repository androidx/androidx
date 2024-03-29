/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.material3.ExperimentalMaterial3Api

/**
 * Receiver scope for [Carousel] item content.
 */
@ExperimentalMaterial3Api
sealed interface CarouselItemScope {
    /**
     * Information regarding the carousel item, such as its minimum and maximum size.
     *
     * The item information is updated after every scroll. If you use it in a composable function,
     * it will be recomposed on every change causing potential performance issues. Avoid using it
     * in the composition.
     */
    val carouselItemInfo: CarouselItemInfo
}

@ExperimentalMaterial3Api
internal class CarouselItemScopeImpl(
    private val itemInfo: CarouselItemInfo
) : CarouselItemScope {
    override val carouselItemInfo: CarouselItemInfo
        get() = itemInfo
}
