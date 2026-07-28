/*
 * Copyright 2026 The Android Open Source Project
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
@file:OptIn(ExperimentalCarApi::class)

package androidx.car.app.sample.showcase.common.utils

import androidx.annotation.OptIn
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Badge
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarProgressBar
import androidx.car.app.model.CarText
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridItem.IMAGE_TYPE_LARGE
import androidx.car.app.model.GridSection
import androidx.car.app.model.OnClickListener

/**
 * Constructs a [GridItem] using a clean, declarative Kotlin syntax. Automatically ignores `null`
 * parameters, bypassing the need to use [GridItem.Builder] directly.
 */
fun createGridItem(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    image: CarIcon? = null,
    @GridItem.GridItemImageType imageType: Int = IMAGE_TYPE_LARGE,
    text: CharSequence? = null,
    textCarText: CarText? = null,
    clickListener: OnClickListener? = null,
    isLoading: Boolean = false,
    badge: Badge? = null,
    isIndexable: Boolean = true,
    progressBar: CarProgressBar? = null,
): GridItem {
    val builder = GridItem.Builder()

    // Title handling (supports CharSequence or CarText with variants)
    titleCarText?.let { builder.setTitle(it) } ?: title?.let { builder.setTitle(it) }

    // Image & Badge handling
    image?.let {
        if (badge != null) {
            builder.setImage(it, imageType, badge)
        } else {
            builder.setImage(it, imageType)
        }
    }

    // Text & ProgressBar (Mutually exclusive in GridItem)
    if (progressBar != null) {
        builder.setProgressBar(progressBar)
    } else {
        textCarText?.let { builder.setText(it) } ?: text?.let { builder.setText(it) }
    }

    clickListener?.let { builder.setOnClickListener(it) }

    builder.setLoading(isLoading)
    builder.setIndexable(isIndexable)

    return builder.build()
}

/**
 * Constructs a [GridSection] using a clean, declarative Kotlin syntax. Bypasses the need to use
 * [GridSection.Builder] directly.
 */
fun createGridSection(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    @GridSection.ItemSize itemSize: Int = GridSection.ITEM_SIZE_MEDIUM,
    @GridSection.ItemImageShape itemImageShape: Int = GridSection.ITEM_IMAGE_SHAPE_UNSET,
    noItemsMessage: CharSequence? = null,
    noItemsMessageCarText: CarText? = null,
    items: List<GridItem> = emptyList(),
): GridSection {
    val builder = GridSection.Builder()

    titleCarText?.let { builder.setTitle(it) } ?: title?.let { builder.setTitle(it) }
    noItemsMessageCarText?.let { builder.setNoItemsMessage(it) }
        ?: noItemsMessage?.let { builder.setNoItemsMessage(it) }

    builder.setItemSize(itemSize)
    builder.setItemImageShape(itemImageShape)

    items.forEach { builder.addItem(it) }

    return builder.build()
}
