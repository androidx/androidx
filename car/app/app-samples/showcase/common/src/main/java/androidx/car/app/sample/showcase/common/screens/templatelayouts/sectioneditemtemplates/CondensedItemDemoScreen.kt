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
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.Background
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarProgressBar
import androidx.car.app.model.CondensedItem
import androidx.car.app.model.CondensedItemStyle
import androidx.car.app.model.CondensedSection
import androidx.car.app.model.Header
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Shape
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/** A screen demonstrating [CondensedItem] in a [SectionedItemTemplate]. */
@RequiresCarApi(9)
@OptIn(ExperimentalCarApi::class)
class CondensedItemDemoScreen(carContext: CarContext) : Screen(carContext) {

    private fun showToast(text: String) {
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }

    override fun onGetTemplate(): Template {
        val mediaIcon =
            CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.test_android_media)
                )
                .build()
        val playIcon = CarIcon.MEDIA_PLAYBACK
        val arrowIcon =
            CarIcon.Builder(
                    IconCompat.createWithResource(carContext, android.R.drawable.ic_media_play)
                )
                .build()

        val grayColor = Color.rgb(60, 62, 65)
        val grayBackground =
            Background.Builder().setColor(CarColor.createCustom(grayColor, grayColor)).build()

        // 1. Road Trip Section
        val roadTripSectionBuilder = CondensedSection.Builder().setTitle("Road Trip")
        for (i in 1..6) {
            roadTripSectionBuilder.addItem(
                CondensedItem.Builder()
                    .setTitle("90s Road Trip")
                    .setText("Playlist • Media")
                    .setLeadingImage(mediaIcon, CondensedItem.IMAGE_TYPE_LARGE)
                    .setTrailingImage(arrowIcon, CondensedItem.IMAGE_TYPE_ICON)
                    .setOnClickListener { showToast("Clicked Road Trip Item $i") }
                    .build()
            )
        }
        val roadTripSection = roadTripSectionBuilder.build()

        // 2. Default Items Section
        val defaultSection =
            CondensedSection.Builder()
                .setTitle("Default Items")
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Standard Item")
                        .setText("Default shape and background")
                        .setLeadingImage(mediaIcon)
                        .setOnClickListener { showToast("Clicked Standard Item") }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Title Only")
                        .setLeadingImage(mediaIcon)
                        .setOnClickListener { showToast("Clicked Title Only") }
                        .build()
                )
                .build()

        // 3. Different Shapes Section (Gray Background)
        val shapesSection =
            CondensedSection.Builder()
                .setTitle("Different Shapes")
                .addItem(buildShapeItem("Shape: None", Shape.NONE, grayBackground, mediaIcon))
                .addItem(
                    buildShapeItem(
                        "Shape: Extra Small",
                        Shape.CORNER_EXTRA_SMALL,
                        grayBackground,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildShapeItem("Shape: Small", Shape.CORNER_SMALL, grayBackground, mediaIcon)
                )
                .addItem(
                    buildShapeItem("Shape: Medium", Shape.CORNER_MEDIUM, grayBackground, mediaIcon)
                )
                .addItem(
                    buildShapeItem("Shape: Large", Shape.CORNER_LARGE, grayBackground, mediaIcon)
                )
                .addItem(
                    buildShapeItem(
                        "Shape: Extra Large",
                        Shape.CORNER_EXTRA_LARGE,
                        grayBackground,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildShapeItem("Shape: Full", Shape.CORNER_FULL, grayBackground, mediaIcon)
                )
                .build()

        // 4. Different Default Colors Section
        val colorsSection =
            CondensedSection.Builder()
                .setTitle("Standard Colors")
                .addItem(buildColorItem("Color: Red", CarColor.RED, playIcon))
                .addItem(buildColorItem("Color: Yellow", CarColor.YELLOW, playIcon))
                .addItem(buildColorItem("Color: Blue", CarColor.BLUE, playIcon))
                .addItem(buildColorItem("Color: Green", CarColor.GREEN, playIcon))
                .addItem(buildColorItem("Color: Primary", CarColor.PRIMARY, playIcon))
                .addItem(buildColorItem("Color: Secondary", CarColor.SECONDARY, playIcon))
                .addItem(buildColorItem("Color: Default", CarColor.DEFAULT, playIcon))
                .build()

        // 5. Different Image Sizes Section
        val imageSizesSection =
            CondensedSection.Builder()
                .setTitle("Image Sizes & Shapes")
                // NONE Shape
                .addItem(
                    buildSizeShapeItem(
                        "None + Large",
                        Shape.NONE,
                        CondensedItem.IMAGE_TYPE_LARGE,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildSizeShapeItem(
                        "None + Small",
                        Shape.NONE,
                        CondensedItem.IMAGE_TYPE_SMALL,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildSizeShapeItem(
                        "None + Icon",
                        Shape.NONE,
                        CondensedItem.IMAGE_TYPE_ICON,
                        mediaIcon,
                    )
                )
                // FULL Shape
                .addItem(
                    buildSizeShapeItem(
                        "Full + Large",
                        Shape.CORNER_FULL,
                        CondensedItem.IMAGE_TYPE_LARGE,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildSizeShapeItem(
                        "Full + Small",
                        Shape.CORNER_FULL,
                        CondensedItem.IMAGE_TYPE_SMALL,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildSizeShapeItem(
                        "Full + Icon",
                        Shape.CORNER_FULL,
                        CondensedItem.IMAGE_TYPE_ICON,
                        mediaIcon,
                    )
                )
                // MEDIUM Shape
                .addItem(
                    buildSizeShapeItem(
                        "Medium + Large",
                        Shape.CORNER_MEDIUM,
                        CondensedItem.IMAGE_TYPE_LARGE,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildSizeShapeItem(
                        "Medium + Small",
                        Shape.CORNER_MEDIUM,
                        CondensedItem.IMAGE_TYPE_SMALL,
                        mediaIcon,
                    )
                )
                .addItem(
                    buildSizeShapeItem(
                        "Medium + Icon",
                        Shape.CORNER_MEDIUM,
                        CondensedItem.IMAGE_TYPE_ICON,
                        mediaIcon,
                    )
                )
                .build()

        // 6. Progress Indicators Section
        val progressSection =
            CondensedSection.Builder()
                .setTitle("Progress Indicators")
                .addItem(buildProgressItem("Default Progress", 0.3f, CarColor.DEFAULT))
                .addItem(buildProgressItem("Red Progress", 0.5f, CarColor.RED))
                .addItem(buildProgressItem("Yellow Progress", 0.7f, CarColor.YELLOW))
                .addItem(buildProgressItem("Blue Progress", 0.9f, CarColor.BLUE))
                .addItem(buildProgressItem("Green Progress", 1.0f, CarColor.GREEN))
                .build()

        // 7. Diverse & Bad Configurations Section
        val lightColor = Color.rgb(240, 240, 240)
        val alphaColor = Color.argb(128, 255, 0, 0)
        val notAlphaColor = Color.argb(255, 255, 0, 0)
        val diverseSection =
            CondensedSection.Builder()
                .setTitle("Diverse & Bad Configs")
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Alpha Background")
                        .setStyle(
                            CondensedItemStyle.Builder()
                                .setBackground(
                                    Background.Builder()
                                        .setColor(CarColor.createCustom(alphaColor, alphaColor))
                                        .build()
                                )
                                .build()
                        )
                        .setOnClickListener { showToast("Clicked Alpha Background") }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Not-Alpha Background")
                        .setStyle(
                            CondensedItemStyle.Builder()
                                .setBackground(
                                    Background.Builder()
                                        .setColor(
                                            CarColor.createCustom(notAlphaColor, notAlphaColor)
                                        )
                                        .build()
                                )
                                .build()
                        )
                        .setOnClickListener { showToast("Clicked Alpha Background") }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Light Background")
                        .setStyle(
                            CondensedItemStyle.Builder()
                                .setBackground(
                                    Background.Builder()
                                        .setColor(CarColor.createCustom(lightColor, lightColor))
                                        .build()
                                )
                                .build()
                        )
                        .setOnClickListener { showToast("Clicked Light Background") }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("2 Large Images")
                        .setLeadingImage(mediaIcon, CondensedItem.IMAGE_TYPE_LARGE)
                        .setTrailingImage(mediaIcon, CondensedItem.IMAGE_TYPE_LARGE)
                        .setStyle(
                            CondensedItemStyle.Builder().setBackground(grayBackground).build()
                        )
                        .setOnClickListener { showToast("Clicked 2 Large Images") }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("2 Large Images")
                        .setLeadingImage(mediaIcon, CondensedItem.IMAGE_TYPE_LARGE)
                        .setTrailingImage(mediaIcon, CondensedItem.IMAGE_TYPE_LARGE)
                        .setStyle(
                            CondensedItemStyle.Builder().setBackground(grayBackground).build()
                        )
                        .setOnClickListener { showToast("Clicked 2 Large Images") }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Custom Alpha Tint")
                        .setLeadingImage(
                            CarIcon.Builder(mediaIcon)
                                .setTint(
                                    CarColor.createCustom(
                                        Color.argb(100, 0, 255, 0),
                                        Color.argb(100, 0, 255, 0),
                                    )
                                )
                                .build()
                        )
                        .setOnClickListener { showToast("Clicked Alpha Tint") }
                        .build()
                )
                .build()

        return SectionedItemTemplate.Builder()
            .addSection(roadTripSection)
            .addSection(defaultSection)
            .addSection(shapesSection)
            .addSection(colorsSection)
            .addSection(imageSizesSection)
            .addSection(progressSection)
            .addSection(diverseSection)
            .setHeader(
                Header.Builder()
                    .setTitle("Condensed Item Demo")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private fun buildShapeItem(
        title: String,
        shape: Shape,
        background: Background,
        icon: CarIcon,
    ): CondensedItem {
        return CondensedItem.Builder()
            .setTitle(title)
            .setText("Subtitle")
            .setTrailingImage(icon)
            .setStyle(
                CondensedItemStyle.Builder().setShape(shape).setBackground(background).build()
            )
            .setOnClickListener { showToast("Clicked $title") }
            .build()
    }

    private fun buildColorItem(title: String, color: CarColor, icon: CarIcon): CondensedItem {
        return CondensedItem.Builder()
            .setTitle(title)
            .setTrailingImage(icon, CondensedItem.IMAGE_TYPE_ICON)
            .setStyle(
                CondensedItemStyle.Builder()
                    .setShape(Shape.CORNER_MEDIUM)
                    .setBackground(Background.Builder().setColor(color).build())
                    .build()
            )
            .setOnClickListener { showToast("Clicked $title") }
            .build()
    }

    private fun buildSizeShapeItem(
        title: String,
        shape: Shape,
        imageType: Int,
        icon: CarIcon,
    ): CondensedItem {
        return CondensedItem.Builder()
            .setTitle(title)
            .setLeadingImage(icon, imageType)
            .setStyle(CondensedItemStyle.Builder().setShape(shape).build())
            .setOnClickListener { showToast("Clicked $title") }
            .build()
    }

    private fun buildProgressItem(title: String, progress: Float, color: CarColor): CondensedItem {
        return CondensedItem.Builder()
            .setTitle(title)
            .setProgressBar(CarProgressBar.Builder(progress).setColor(color).build())
            .setOnClickListener { showToast("Clicked $title") }
            .build()
    }
}
