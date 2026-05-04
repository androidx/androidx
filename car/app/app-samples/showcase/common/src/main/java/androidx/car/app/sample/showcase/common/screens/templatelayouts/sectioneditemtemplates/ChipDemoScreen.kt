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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.sectioneditemtemplates

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Chip
import androidx.car.app.model.ChipSection
import androidx.car.app.model.ChipStyle
import androidx.car.app.model.Header
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Shape
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/**
 * A screen demonstrating how a [ChipSection] with [Chip]s can be utilized within a
 * [SectionedItemTemplate].
 *
 * This screen also demonstrates that you can have custom chip styling in one of two ways:
 * * Setting a separate ChipStyle per chip using [Chip#setStyle] (see chip called Custom1 and
 *   Custom2)
 * * Setting a ChipStyle on the section using [ChipSection#setStyle], and overriding this style for
 *   a single chip using [Chip#setStyle] when the chip is selected
 */
@OptIn(ExperimentalCarApi::class)
@RequiresCarApi(8)
class ChipDemoScreen(carContext: CarContext) : Screen(carContext) {
    private val mSectionStyle =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(0xFFE3F2FD.toInt(), 0xFF0D47A1.toInt()))
            .setContentColor(CarColor.createCustom(0xFF0D47A1.toInt(), 0xFFE3F2FD.toInt()))
            .setOutlineColor(CarColor.createCustom(0xFF2196F3.toInt(), 0xFF64B5F6.toInt()))
            .build()
    private val mSelectedSectionStyle =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(Color.BLUE, Color.BLUE))
            .setContentColor(CarColor.createCustom(Color.WHITE, Color.WHITE))
            .setOutlineColor(CarColor.createCustom(Color.BLUE, Color.BLUE))
            .build()

    private val mCustomContentColorUnselectedStyle =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(0xFFE8F5E9.toInt(), 0xFF1B5E20.toInt()))
            .setContentColor(CarColor.createCustom(0xFF1B5E20.toInt(), 0xFFE8F5E9.toInt()))
            .setShape(Shape.CORNER_FULL)
            .build()
    private val mCustomContentColorSelectedStyle =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(Color.GREEN, Color.GREEN))
            .setContentColor(CarColor.createCustom(Color.BLACK, Color.BLACK))
            .setShape(Shape.CORNER_FULL)
            .build()

    private val mCustomUnselectedStyle =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(Color.WHITE, 0xFF121212.toInt()))
            .setOutlineColor(CarColor.RED)
            .setContentColor(CarColor.createCustom(0xFFB71C1C.toInt(), 0xFFEF9A9A.toInt()))
            .build()
    private val mCustomSelectedStyle =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(0xFFD32F2F.toInt(), 0xFFD32F2F.toInt()))
            .setOutlineColor(CarColor.createCustom(0xFFD32F2F.toInt(), 0xFFD32F2F.toInt()))
            .setContentColor(CarColor.createCustom(Color.WHITE, Color.WHITE))
            .build()

    private val mNonContrastSuitableColors =
        ChipStyle.Builder()
            .setBackgroundColor(CarColor.createCustom(Color.BLUE, Color.BLUE))
            .setContentColor(CarColor.createCustom(Color.BLUE, Color.BLUE))
            .build()

    private val mChips =
        listOf(
            // title-only chip styled from ChipSection
            DemoChip("Text only") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("Text only")
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },

            // startIcon-only chip styled from ChipSection
            DemoChip("Start icon only") { isSelected, onClick ->
                Chip.Builder()
                    .setStartIcon(
                        CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_face_24px)
                            )
                            .build()
                    )
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },

            // Chip with startIcon and title, styled from ChipSection
            DemoChip("Start + Text") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("Start + Text")
                    .setStartIcon(
                        CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_face_24px)
                            )
                            .build()
                    )
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },

            // Chip with title and endIcon, styled from ChipSection
            DemoChip("End + Text") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("End + Text")
                    .setEndIcon(CarIcon.ALERT)
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },

            // Chip with startIcon, endIcon, and title, styled singularly using
            // ChipStyle#contentColor
            DemoChip("Custom content color") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("Custom1")
                    .setStartIcon(
                        CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_face_24px)
                            )
                            .build()
                    )
                    .setEndIcon(CarIcon.ALERT)
                    .setSelected(isSelected)
                    .setStyle(
                        if (isSelected) mCustomContentColorSelectedStyle
                        else mCustomContentColorUnselectedStyle
                    )
                    .setOnClickListener(onClick)
                    .build()
            },

            // Custom chip with different colors for startIcon (colored with supplied tint) and
            // title (colored via style.contentColor)
            // Recommended way to achieve a chip with a different icon color and title color
            DemoChip("Custom content color & icon tint") { isSelected, onClick ->
                val customUnselectedIconColor =
                    CarColor.createCustom(0xFF0D47A1.toInt(), 0xFF82B1FF.toInt())
                val customSelectedIconColor = CarColor.createCustom(Color.YELLOW, Color.YELLOW)

                val customCurrentIconColor =
                    if (isSelected) customSelectedIconColor else customUnselectedIconColor

                Chip.Builder()
                    // Title color is styled via ChipStyle.setContentColor
                    .setTitle("Custom2")
                    .setStartIcon(
                        CarIcon.Builder(
                                IconCompat.createWithResource(carContext, R.drawable.ic_face_24px)
                            )
                            .setTint(customCurrentIconColor)
                            .build()
                    )
                    .setSelected(isSelected)
                    .setStyle(if (isSelected) mCustomSelectedStyle else mCustomUnselectedStyle)
                    .setOnClickListener(onClick)
                    .build()
            },

            // Chip with startIcon, endIcon, and title. The contentColor fails contrast, so the chip
            // is styled via Host defaults.
            DemoChip("Filled default-style chip") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("LongTextLongText")
                    .setStartIcon(CarIcon.APP_ICON)
                    .setEndIcon(CarIcon.ALERT)
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .setStyle(mNonContrastSuitableColors)
                    .build()
            },

            // Ignored 1
            DemoChip("Extra 1") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("Extra #1")
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },

            // Ignored 2
            DemoChip("Extra 2") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("Extra #2")
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },

            // Ignored 3
            DemoChip("Extra 3") { isSelected, onClick ->
                Chip.Builder()
                    .setTitle("Extra #3")
                    .setSelected(isSelected)
                    .setOnClickListener(onClick)
                    .apply {
                        if (isSelected) {
                            setStyle(mSelectedSectionStyle)
                        }
                    }
                    .build()
            },
        )

    override fun onGetTemplate(): Template {
        val chipSectionBuilder = ChipSection.Builder().setStyle(mSectionStyle)

        for (chip in mChips) {
            chipSectionBuilder.addItem(
                chip.createChip(chip.isSelected) {
                    chip.isSelected = !chip.isSelected
                    invalidate()
                }
            )
        }

        val anySelected = mChips.any { it.isSelected }
        val showAll = !anySelected

        val rowSectionBuilder = RowSection.Builder()
        rowSectionBuilder.addItem(Row.Builder().setTitle("Items:").build())

        for (chip in mChips) {
            if (showAll || chip.isSelected) {
                rowSectionBuilder.addItem(Row.Builder().setTitle(chip.rowTitle).build())
            }
        }

        return SectionedItemTemplate.Builder()
            .addSection(chipSectionBuilder.build())
            .addSection(rowSectionBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.chip_demo_title))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private class DemoChip(
        val rowTitle: String,
        var isSelected: Boolean = false,
        val createChip: (isSelected: Boolean, onClick: () -> Unit) -> Chip,
    )
}
