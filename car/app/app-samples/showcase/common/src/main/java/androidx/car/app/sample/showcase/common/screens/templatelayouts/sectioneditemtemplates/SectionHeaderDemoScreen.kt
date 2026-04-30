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

import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionHeader
import androidx.car.app.model.SectionHeader.SectionHeaderImageType
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

@RequiresCarApi(8)
@OptIn(ExperimentalCarApi::class)
/** A comprehensive screen demonstrating all RSL validation scenarios for SectionHeader. */
class SectionHeaderDemoScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val iconDefault = createCarIcon(R.drawable.test_image_square)
        val iconTrailing = createCarIcon(R.drawable.ic_chevron_right_24)
        val iconAvatar = createCarIcon(R.drawable.ic_face_24px)

        val header1 = createSectionHeader("Simple Title")
        val header2 =
            createSectionHeader(
                "Title with Icon",
                startIcon = iconDefault,
                imageType = SectionHeader.IMAGE_TYPE_SMALL,
            )
        val header3 =
            createSectionHeader(
                "Clickable Title with Chevron",
                endIcon = iconTrailing,
                onClickListener = {
                    CarToast.makeText(
                            carContext,
                            "Clickable Title with Chevron",
                            CarToast.LENGTH_SHORT,
                        )
                        .show()
                },
            )
        val header4 =
            createSectionHeader(
                "Avatar",
                startIcon = iconAvatar,
                imageType = SectionHeader.IMAGE_TYPE_LARGE,
            )
        val header5 =
            createSectionHeader(
                "Clickable Avatar",
                startIcon = iconAvatar,
                imageType = SectionHeader.IMAGE_TYPE_LARGE,
                endIcon = iconTrailing,
                onClickListener = {
                    CarToast.makeText(carContext, "Clickable Avatar", CarToast.LENGTH_SHORT).show()
                },
            )
        val header6 =
            createSectionHeader(
                "Title with Large Icon",
                startIcon = iconDefault,
                imageType = SectionHeader.IMAGE_TYPE_LARGE,
            )

        return SectionedItemTemplate.Builder()
            .addSection(RowSection.Builder().setSectionHeader(header1).build())
            .addSection(RowSection.Builder().setSectionHeader(header2).build())
            .addSection(RowSection.Builder().setSectionHeader(header3).build())
            .addSection(RowSection.Builder().setSectionHeader(header4).build())
            .addSection(RowSection.Builder().setSectionHeader(header5).build())
            .addSection(RowSection.Builder().setSectionHeader(header6).build())
            .addSection(
                GridSection.Builder()
                    .setSectionHeader(header1)
                    .addItem(GridItem.Builder().setTitle("Item 1").setImage(iconDefault).build())
                    .addItem(GridItem.Builder().setTitle("Item 2").setImage(iconDefault).build())
                    .build()
            )
            .setHeader(
                Header.Builder()
                    .setTitle("Section Header Demo")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private fun createCarIcon(@DrawableRes resId: Int): CarIcon {
        return CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()
    }

    private fun createSectionHeader(
        title: String,
        startIcon: CarIcon? = null,
        @SectionHeaderImageType imageType: Int = SectionHeader.IMAGE_TYPE_SMALL,
        endIcon: CarIcon? = null,
        onClickListener: (() -> Unit)? = null,
    ): SectionHeader {
        val builder = SectionHeader.Builder(CarText.create(title))
        if (startIcon != null) {
            builder.setStartIcon(startIcon, imageType)
        }
        if (endIcon != null) {
            builder.setEndIcon(endIcon)
        }
        if (onClickListener != null) {
            builder.setOnClickListener(onClickListener)
        }
        return builder.build()
    }

    private fun createRow(scenario: String): Row {
        return Row.Builder().setTitle(scenario).build()
    }
}
