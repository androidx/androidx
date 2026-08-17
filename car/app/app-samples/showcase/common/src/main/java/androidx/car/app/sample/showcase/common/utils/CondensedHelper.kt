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
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarProgressBar
import androidx.car.app.model.CarText
import androidx.car.app.model.CondensedItem
import androidx.car.app.model.CondensedItemStyle
import androidx.car.app.model.CondensedSection
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.SectionHeader
import androidx.car.app.model.SpotlightSection

/** Constructs a [CondensedItem] using clean, declarative Kotlin syntax. */
fun createCondensedItem(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    text: CharSequence? = null,
    textCarText: CarText? = null,
    image: CarIcon? = null,
    @CondensedItem.CondensedItemImageType imageType: Int = CondensedItem.IMAGE_TYPE_LARGE,
    trailingImage: CarIcon? = null,
    @CondensedItem.CondensedItemImageType trailingImageType: Int = CondensedItem.IMAGE_TYPE_ICON,
    style: CondensedItemStyle? = null,
    progressBar: CarProgressBar? = null,
    isIndexable: Boolean = true,
    clickListener: OnClickListener? = null,
): CondensedItem {
    val builder = CondensedItem.Builder()

    titleCarText?.let { builder.setTitle(it) } ?: title?.let { builder.setTitle(it) }
    textCarText?.let { builder.setText(it) } ?: text?.let { builder.setText(it) }

    image?.let { builder.setLeadingImage(it, imageType) }
    trailingImage?.let { builder.setTrailingImage(it, trailingImageType) }

    style?.let { builder.setStyle(it) }
    progressBar?.let { builder.setProgressBar(it) }
    builder.setIndexable(isIndexable)
    clickListener?.let { builder.setOnClickListener(it) }

    return builder.build()
}

/** Constructs a [CondensedSection] using declarative Kotlin syntax. */
fun createCondensedSection(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    sectionHeader: SectionHeader? = null,
    noItemsMessage: CharSequence? = null,
    noItemsMessageCarText: CarText? = null,
    items: List<CondensedItem> = emptyList(),
): CondensedSection {
    val builder = CondensedSection.Builder()

    sectionHeader?.let { builder.setSectionHeader(it) }
        ?: titleCarText?.let { builder.setTitle(it) }
        ?: title?.let { builder.setTitle(it) }

    noItemsMessageCarText?.let { builder.setNoItemsMessage(it) }
        ?: noItemsMessage?.let { builder.setNoItemsMessage(it) }

    items.forEach { builder.addItem(it) }

    return builder.build()
}

/** Constructs a [SpotlightSection] using declarative Kotlin syntax. */
fun createSpotlightSection(
    image: CarIcon,
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    sectionHeader: SectionHeader? = null,
    noItemsMessage: CharSequence? = null,
    noItemsMessageCarText: CarText? = null,
    items: List<CondensedItem> = emptyList(),
): SpotlightSection {
    val builder = SpotlightSection.Builder(image)

    sectionHeader?.let { builder.setSectionHeader(it) }
        ?: titleCarText?.let { builder.setTitle(it) }
        ?: title?.let { builder.setTitle(it) }

    noItemsMessageCarText?.let { builder.setNoItemsMessage(it) }
        ?: noItemsMessage?.let { builder.setNoItemsMessage(it) }

    items.forEach { builder.addItem(it) }

    return builder.build()
}
