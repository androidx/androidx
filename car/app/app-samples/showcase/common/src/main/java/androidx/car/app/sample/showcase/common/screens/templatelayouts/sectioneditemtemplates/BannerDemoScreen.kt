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
import androidx.car.app.model.Banner
import androidx.car.app.model.BannerSection
import androidx.car.app.model.BannerStyle
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.SectionHeader
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Shape
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/** A screen demonstrating [Banner] in a [SectionedItemTemplate]. */
@RequiresCarApi(9)
@OptIn(ExperimentalCarApi::class)
class BannerDemoScreen(carContext: CarContext) : Screen(carContext) {

    private fun showToast(text: String) {
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }

    private fun createSection(title: String?, banner: Banner): BannerSection {
        return BannerSection.Builder()
            .apply {
                if (title != null) {
                    setSectionHeader(SectionHeader.Builder(title).build())
                }
            }
            .addItem(banner)
            .build()
    }

    override fun onGetTemplate(): Template {
        val mediaIcon =
            CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.test_android_media)
                )
                .build()
        val playIcon = CarIcon.MEDIA_PLAYBACK
        val settingsIcon =
            CarIcon.Builder(
                    IconCompat.createWithResource(
                        carContext,
                        android.R.drawable.ic_menu_preferences,
                    )
                )
                .build()

        val grayColor = Color.rgb(154, 160, 166)
        val grayBackground =
            Background.Builder().setColor(CarColor.createCustom(grayColor, grayColor)).build()

        // 0. Simple Banner Section
        val simpleBanner = Banner.Builder().setTitle("Title Only").build()
        val simpleSection = createSection("Simple Banners", simpleBanner)

        // 1. Standard Banner Section
        val standardBanner =
            Banner.Builder()
                .setTitle("Standard Banner")
                .setSubtitle("This is a subtitle for the banner.")
                .setOnClickListener { showToast("Clicked Standard Banner") }
                .build()
        val standardSection = createSection(null, standardBanner)

        // 2. Banner with Leading Image
        val leadingImageBanner =
            Banner.Builder()
                .setTitle("Leading Image")
                .setSubtitle("This is a subtitle for the banner.")
                .setLeadingImage(mediaIcon)
                .build()
        val leadingImageSection = createSection("Leading Banners", leadingImageBanner)

        // 3. Banner with Leading Icon
        val leadingIconBanner =
            Banner.Builder()
                .setTitle("Leading Icon")
                .setSubtitle("This is a subtitle for the banner.")
                .setLeadingIcon(playIcon)
                .build()
        val leadingIconSection = createSection(null, leadingIconBanner)

        // 4. Banner with Trailing Icon
        val trailingIconBanner =
            Banner.Builder()
                .setTitle("Trailing Icon")
                .setSubtitle("This is a subtitle for the banner.")
                .addTrailingIcon(playIcon)
                .build()
        val trailingIconSection = createSection("Trailing Banners", trailingIconBanner)

        // 5. Banner with Trailing Image
        val trailingImageBanner =
            Banner.Builder()
                .setTitle("Trailing Image")
                .setSubtitle("This is a subtitle for the banner.")
                .addTrailingImage(mediaIcon)
                .build()
        val trailingImageSection = createSection(null, trailingImageBanner)

        // 6. Banner with Trailing Action (Settings Icon)
        val trailingActionBanner =
            Banner.Builder()
                .setTitle("Trailing Icon Button")
                .setSubtitle("This is a subtitle for the banner.")
                .addTrailingAction(
                    Action.Builder()
                        .setTitle("Settings")
                        .setIcon(settingsIcon)
                        .setOnClickListener { showToast("Clicked Settings Action") }
                        .build()
                )
                .build()
        val trailingActionSection = createSection(null, trailingActionBanner)

        // 7. Banner with Trailing Button
        val trailingButtonBanner =
            Banner.Builder()
                .setTitle("Trailing Button")
                .addTrailingAction(
                    Action.Builder()
                        .setTitle("Action")
                        .setOnClickListener { showToast("Clicked Trailing Button") }
                        .build()
                )
                .addTrailingIcon(playIcon)
                .build()
        val trailingButtonSection = createSection(null, trailingButtonBanner)

        // 8. Banner with 1 Below Action
        val belowActionBanner =
            Banner.Builder()
                .setTitle("Below Action")
                .setSubtitle("Only 1 action below.")
                .addBelowAction(
                    Action.Builder()
                        .setTitle("Action")
                        .setOnClickListener { showToast("Clicked Below Action") }
                        .build()
                )
                .build()
        val belowActionSection = createSection("Below Actions", belowActionBanner)

        // 9. Banner with Below Actions (2 actions and subtitle)
        val belowActionsBanner =
            Banner.Builder()
                .setTitle("Below Actions")
                .setSubtitle("Actions appear below the text.")
                .addBelowAction(
                    Action.Builder()
                        .setTitle("Primary")
                        .setOnClickListener { showToast("Clicked Primary Action") }
                        .build()
                )
                .addBelowAction(
                    Action.Builder()
                        .setTitle("Secondary")
                        .setOnClickListener { showToast("Clicked Secondary Action") }
                        .build()
                )
                .build()
        val belowActionsSection = createSection(null, belowActionsBanner)

        // 10. Rich Banner Section
        val richDemoBanner =
            Banner.Builder()
                .setTitle("Rich Banner")
                .setSubtitle("All banner elements combined.")
                .setLeadingImage(mediaIcon)
                .addTrailingIcon(playIcon)
                .addBelowAction(
                    Action.Builder()
                        .setTitle("Primary")
                        .setOnClickListener { showToast("Clicked Primary Action") }
                        .build()
                )
                .addBelowAction(
                    Action.Builder()
                        .setTitle("Secondary")
                        .setOnClickListener { showToast("Clicked Secondary Action") }
                        .build()
                )
                .addBelowAction(
                    Action.Builder()
                        .setIcon(settingsIcon)
                        .setOnClickListener { showToast("Clicked Settings Action") }
                        .build()
                )
                .build()
        val richDemoSection = createSection("Rich Banner", richDemoBanner)

        // 11. Different Shapes Section
        val shapes =
            listOf(
                "None" to Shape.NONE,
                "Small" to Shape.CORNER_SMALL,
                "Medium" to Shape.CORNER_MEDIUM,
                "Large" to Shape.CORNER_LARGE,
                "Extra Large" to Shape.CORNER_EXTRA_LARGE,
                "Full" to Shape.CORNER_FULL,
            )

        val simpleShapesSection = mutableListOf<BannerSection>()
        shapes.forEachIndexed { index, (shapeName, shape) ->
            val banner =
                Banner.Builder()
                    .setTitle("Simple : $shapeName")
                    .setStyle(
                        BannerStyle.Builder().setBackground(grayBackground).setShape(shape).build()
                    )
                    .build()
            val headerTitle = if (index == 0) "Simple Banner Shapes" else null
            simpleShapesSection.add(createSection(headerTitle, banner))
        }

        val richShapesSection = mutableListOf<BannerSection>()
        shapes.forEachIndexed { index, (shapeName, shape) ->
            val banner =
                Banner.Builder()
                    .setTitle("Rich : $shapeName")
                    .setLeadingImage(mediaIcon)
                    .addTrailingIcon(playIcon)
                    .setStyle(
                        BannerStyle.Builder().setBackground(grayBackground).setShape(shape).build()
                    )
                    .addBelowAction(
                        Action.Builder()
                            .setTitle("Primary")
                            .setOnClickListener { showToast("Clicked Primary Action") }
                            .build()
                    )
                    .addBelowAction(
                        Action.Builder()
                            .setTitle("Secondary")
                            .setOnClickListener { showToast("Clicked Secondary Action") }
                            .build()
                    )
                    .build()
            val headerTitle = if (index == 0) "Rich Banner Shapes" else null
            richShapesSection.add(createSection(headerTitle, banner))
        }

        // 12. Different Background Colors Section
        // TODO: b/510071734 - Revert hex custom colors back to standard built-in CarColor constants
        //  (CarColor.RED, BLUE, etc.) once host BannerLayout supports
        // ColorBackgroundUiModel.BuiltIn.
        val colorPairs =
            listOf(
                "Red" to Color.parseColor("#E53935"),
                "Yellow" to Color.parseColor("#FDD835"),
                "Blue" to Color.parseColor("#1E88E5"),
                "Green" to Color.parseColor("#43A047"),
                "Purple" to Color.parseColor("#8E24AA"),
                "Orange" to Color.parseColor("#FB8C00"),
                "Teal" to Color.parseColor("#00897B"),
            )
        val colorsSection = mutableListOf<BannerSection>()
        colorPairs.forEachIndexed { index, (colorName, colorInt) ->
            val customColor = CarColor.createCustom(colorInt, colorInt)
            val banner =
                Banner.Builder()
                    .setTitle("Color: $colorName")
                    .addTrailingIcon(playIcon)
                    .setStyle(
                        BannerStyle.Builder()
                            .setBackground(Background.Builder().setColor(customColor).build())
                            .setShape(Shape.CORNER_MEDIUM)
                            .build()
                    )
                    .setOnClickListener { showToast("Clicked Color: $colorName") }
                    .build()
            val headerTitle = if (index == 0) "Background Colors" else null
            colorsSection.add(createSection(headerTitle, banner))
        }

        return SectionedItemTemplate.Builder()
            .addSection(simpleSection)
            .addSection(standardSection)
            .addSection(leadingImageSection)
            .addSection(leadingIconSection)
            .addSection(trailingIconSection)
            .addSection(trailingImageSection)
            .addSection(trailingActionSection)
            .addSection(trailingButtonSection)
            .addSection(belowActionSection)
            .addSection(belowActionsSection)
            .addSection(richDemoSection)
            .apply { simpleShapesSection.forEach { addSection(it) } }
            .apply { richShapesSection.forEach { addSection(it) } }
            .apply { colorsSection.forEach { addSection(it) } }
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.banner_demo_title))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }
}
