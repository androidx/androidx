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

package androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent

import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Banner
import androidx.car.app.model.BannerSection
import androidx.car.app.model.CarProgressBar
import androidx.car.app.model.ChipSection
import androidx.car.app.model.CondensedItem
import androidx.car.app.model.CondensedSection
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionHeader
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.SpotlightSection
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModelFactory
import androidx.car.app.sample.showcase.common.utils.CarContextAware
import androidx.car.app.sample.showcase.common.utils.createAction
import androidx.car.app.sample.showcase.common.utils.createBadge
import androidx.car.app.sample.showcase.common.utils.createChip
import androidx.car.app.sample.showcase.common.utils.createChipSection
import androidx.car.app.sample.showcase.common.utils.createCondensedItem
import androidx.car.app.sample.showcase.common.utils.createCondensedSection
import androidx.car.app.sample.showcase.common.utils.createGridItem
import androidx.car.app.sample.showcase.common.utils.createGridSection
import androidx.car.app.sample.showcase.common.utils.createRow
import androidx.car.app.sample.showcase.common.utils.createSpotlightSection
import androidx.car.app.sample.showcase.common.utils.withApiGuard
import androidx.car.app.versioning.CarAppApiLevels

/** Demonstrates how to present a [MapWithContentTemplate] alongside a [SectionedItemTemplate]. */
@OptIn(ExperimentalCarApi::class)
class MapWithSectionedItemsDemoScreen(carContext: CarContext) :
    Screen(carContext), CarContextAware {
    private val routingDemoModelFactory = RoutingDemoModelFactory(carContext)
    private var isHovEnabled = true
    private val navigateTitle = getString(R.string.navigate)
    private val icMenu = getCarIcon(R.drawable.ic_fastfood_white_48dp)
    private val icPhotos = getCarIcon(R.drawable.patio)
    private val icAndroid = getCarIcon(R.drawable.test_android_media)
    private val icSquare = getCarIcon(R.drawable.test_image_square)
    private val icNavigate = getCarIcon(R.drawable.ic_explore_white_24dp)
    private val icRoute = getCarIcon(R.drawable.arrow_right_turn)
    private val icRefresh = getCarIcon(R.drawable.baseline_refresh_24)
    private val icInfo = getCarIcon(R.drawable.outline_info_24)
    private val icBug = getCarIcon(R.drawable.ic_bug_report_24px)

    private var selectedChipIndex = 0

    override fun onGetTemplate(): Template {
        val sectionedTemplate =
            with(SectionedItemTemplate.Builder()) {
                setHeader(makeHeader())
                carContext.withApiGuard(CarAppApiLevels.LEVEL_9) {
                    addSection(makeChipSection())
                    addSection(makeSpotlightSection())
                    addSection(makeBannerSection())
                    addSection(makeCondensedSection())
                }
                addSection(makeGridSection())
                addSection(makeListSection())
                build()
            }

        return MapWithContentTemplate.Builder()
            .setContentTemplate(sectionedTemplate)
            .setActionStrip(makeActionStrip())
            .setMapController(makeMapController())
            .build()
    }

    private fun makeHeader(): Header {
        return Header.Builder()
            .setTitle(getString(R.string.map_with_sectioned_items_demo_title))
            .setStartHeaderAction(Action.BACK)
            .build()
    }

    private fun makeListSection(): RowSection {
        val primaryAction =
            createAction(title = navigateTitle, icon = icNavigate, flags = Action.FLAG_PRIMARY)
        val secondaryAction = createAction(title = "", icon = icRoute)

        val hovToggle =
            Toggle.Builder { checked -> isHovEnabled = checked }.setChecked(isHovEnabled).build()

        return RowSection.Builder()
            .setTitle("List Items Section")
            .addItem(createRow(title = "HOV", toggle = hovToggle))
            .addItem(createRow(title = "Last visit 5 days", image = icRefresh))
            .addItem(createRow(title = "(555) 555-0123", image = icInfo))
            .addItem(createRow(title = " ", actions = listOf(primaryAction, secondaryAction)))
            .build()
    }

    private fun makeGridSection(): GridSection {
        val redDotBadge = createBadge()
        val iconBadge = createBadge(icon = icInfo)
        val progressBar = CarProgressBar.Builder(0.6f).build()

        return createGridSection(
            title = "Grid Items Section",
            itemSize = GridSection.ITEM_SIZE_MEDIUM,
            items =
                listOf(
                    createGridItem(
                        title = "Standard",
                        text = "With click listener",
                        image = icMenu,
                        clickListener = { makeToast("Clicked Grid Item!").show() },
                    ),
                    createGridItem(
                        title = "Simple Icon",
                        text = "inst. of Image",
                        image = icNavigate,
                        imageType = GridItem.IMAGE_TYPE_ICON,
                        clickListener = {},
                    ),
                    createGridItem(
                        title = "Dot Badge",
                        text = "Unread notification",
                        image = icPhotos,
                        badge = redDotBadge,
                        clickListener = {},
                    ),
                    createGridItem(
                        title = "Icon Badge",
                        text = "Additional context",
                        image = icPhotos,
                        badge = iconBadge,
                        clickListener = {},
                    ),
                    createGridItem(
                        title = "Progress Bar",
                        image = icAndroid,
                        progressBar = progressBar,
                        clickListener = {},
                    ),
                    createGridItem(
                        title = "Non-indexable",
                        text = "Skipped in indexed lists",
                        image = icSquare,
                        isIndexable = false,
                        clickListener = {},
                    ),
                ),
        )
    }

    private fun makeChipSection(): ChipSection {
        val filterTitles = listOf("All", "Filter 1", "Filter 2", "Filter 3")

        val chips =
            filterTitles.mapIndexed { index, title ->
                createChip(
                    title = title,
                    startIcon = if (index == 2) icPhotos else null,
                    isSelected = (index == selectedChipIndex),
                    clickListener = {
                        selectedChipIndex = index
                        invalidate()
                    },
                )
            }

        return createChipSection(title = "Filters Section", items = chips)
    }

    private fun makeSpotlightSection(): SpotlightSection {
        val spotlightIcons = listOf(icMenu, icAndroid, icPhotos)
        val items =
            spotlightIcons.mapIndexed { i, icon ->
                createCondensedItem(
                    title = "Condensed Item ${i + 1}",
                    text = "Spotlight item ${i + 1}",
                    image = icon,
                )
            }

        return createSpotlightSection(image = icPhotos, title = "Spotlight Section", items = items)
    }

    private fun makeBannerSection(): BannerSection {
        val banner =
            Banner.Builder()
                .apply {
                    setTitle("Rich Banner")
                    setSubtitle("Combined banner elements: images, actions, and details.")
                    setLeadingImage(icAndroid)
                    addTrailingImage(icNavigate, Banner.IMAGE_TYPE_ICON)
                    addBelowAction(createAction(title = "Primary"))
                    addBelowAction(createAction(title = "Secondary"))
                    addBelowAction(createAction(icon = icInfo))
                }
                .build()

        return BannerSection.Builder()
            .setSectionHeader(SectionHeader.Builder("Rich Banner Section").build())
            .addItem(banner)
            .build()
    }

    private fun makeCondensedSection(): CondensedSection {
        val items =
            List(3) { i ->
                createCondensedItem(
                    title = "Condensed Item #${i + 1}",
                    text = "Playlist • Media",
                    image = icAndroid,
                    imageType = CondensedItem.IMAGE_TYPE_LARGE,
                    trailingImage = icRoute,
                )
            }

        return createCondensedSection(title = "Condensed Items Section", items = items)
    }

    private fun makeActionStrip(): ActionStrip {
        val bugAction = createAction(title = null, icon = icBug, clickListener = {})
        return ActionStrip.Builder().addAction(bugAction).build()
    }

    private fun makeMapController(): MapController {
        return MapController.Builder()
            .setMapActionStrip(routingDemoModelFactory.mapActionStrip)
            .build()
    }
}
