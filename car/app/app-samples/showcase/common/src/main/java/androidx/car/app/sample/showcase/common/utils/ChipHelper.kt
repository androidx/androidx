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
import androidx.car.app.model.CarText
import androidx.car.app.model.Chip
import androidx.car.app.model.ChipSection
import androidx.car.app.model.ChipStyle
import androidx.car.app.model.OnClickListener

/** Constructs a [Chip] using declarative Kotlin syntax. */
fun createChip(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    startIcon: CarIcon? = null,
    endIcon: CarIcon? = null,
    isSelected: Boolean = false,
    style: ChipStyle? = null,
    clickListener: OnClickListener? = null,
): Chip {
    val builder = Chip.Builder()

    titleCarText?.let { builder.setTitle(it) } ?: title?.let { builder.setTitle(it) }
    startIcon?.let { builder.setStartIcon(it) }
    endIcon?.let { builder.setEndIcon(it) }
    style?.let { builder.setStyle(it) }
    clickListener?.let { builder.setOnClickListener(it) }

    builder.setSelected(isSelected)

    return builder.build()
}

/** Constructs a [ChipSection] using declarative Kotlin syntax. */
fun createChipSection(
    title: CharSequence? = null,
    titleCarText: CarText? = null,
    style: ChipStyle? = null,
    items: List<Chip>,
): ChipSection {
    val builder = ChipSection.Builder()

    titleCarText?.let { builder.setTitle(it) } ?: title?.let { builder.setTitle(it) }
    style?.let { builder.setStyle(it) }

    items.forEach { builder.addItem(it) }

    return builder.build()
}
