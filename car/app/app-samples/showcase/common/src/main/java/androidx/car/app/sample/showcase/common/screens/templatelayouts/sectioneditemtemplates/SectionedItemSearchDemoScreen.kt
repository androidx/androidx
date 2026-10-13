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

import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.Banner
import androidx.car.app.model.BannerSection
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Chip
import androidx.car.app.model.ChipSection
import androidx.car.app.model.CondensedItem
import androidx.car.app.model.CondensedSection
import androidx.car.app.model.GridSection
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SearchCallback
import androidx.car.app.model.SearchHeader
import androidx.car.app.model.SectionHeader
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.SpotlightSection
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/**
 * A demo screen showcasing modern search and discovery using [SectionedItemTemplate] and
 * [SearchHeader], featuring all permitted section types: [ChipSection], [BannerSection],
 * [SpotlightSection], [GridSection], [CondensedSection], and [RowSection].
 */
@RequiresCarApi(9)
@OptIn(ExperimentalCarApi::class)
class SectionedItemSearchDemoScreen(carContext: CarContext) : Screen(carContext) {
    private var searchQuery = ""
    private var selectedCategoryIndex = 0

    private val circularCityIcon: CarIcon by lazy {
        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.patio)).build()
    }

    override fun onGetTemplate(): Template {
        // 1. Interactive SearchHeader
        val searchCallback =
            object : SearchCallback {
                override fun onSearchTextChanged(searchText: String) {
                    searchQuery = searchText
                    invalidate()
                }

                override fun onSearchSubmitted(searchText: String) {
                    CarToast.makeText(carContext, "Searched: $searchText", CarToast.LENGTH_LONG)
                        .show()
                }
            }

        // 2. ChipSection (Filter Chips) - Must be first section if present
        val chipSection =
            ChipSection.Builder()
                .addItem(
                    Chip.Builder()
                        .setTitle("All")
                        .setSelected(selectedCategoryIndex == 0)
                        .setOnClickListener {
                            selectedCategoryIndex = 0
                            invalidate()
                        }
                        .build()
                )
                .addItem(
                    Chip.Builder()
                        .setTitle("Gas & EV")
                        .setSelected(selectedCategoryIndex == 1)
                        .setOnClickListener {
                            selectedCategoryIndex = 1
                            invalidate()
                        }
                        .build()
                )
                .addItem(
                    Chip.Builder()
                        .setTitle("Coffee & Food")
                        .setSelected(selectedCategoryIndex == 2)
                        .setOnClickListener {
                            selectedCategoryIndex = 2
                            invalidate()
                        }
                        .build()
                )
                .build()

        // 3. CondensedSection (Quick Suggestions) - Moved to the top
        val condensedSection =
            CondensedSection.Builder()
                .setTitle("Quick Suggestions")
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Office")
                        .setText("15 min • Light traffic")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Office",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Home")
                        .setText("25 min • Moderate traffic")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Home",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .build()

        // 4. SpotlightSection (Explore the City with circular city picture and 6 featured
        // destinations)
        val spotlightSection =
            SpotlightSection.Builder(circularCityIcon)
                .setSectionHeader(
                    SectionHeader.Builder("Explore the City")
                        .setSubtitle("Popular destinations around town")
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("City Center Plaza")
                        .setText("Historic district • 1.1 mi • 4.9 ★")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to City Center Plaza",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Harbor Bay Boardwalk")
                        .setText("Scenic waterfront • 2.5 mi • 4.7 ★")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Harbor Bay Boardwalk",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Art & Science Museum")
                        .setText("Open until 8 PM • 3.2 mi • 4.8 ★")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Art & Science Museum",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Skyline Observatory")
                        .setText("Panoramic views • 4.0 mi • 4.9 ★")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Skyline Observatory",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Botanical Gardens")
                        .setText("Floral exhibits • 5.1 mi • 4.6 ★")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Botanical Gardens",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    CondensedItem.Builder()
                        .setTitle("Old Town Market")
                        .setText("Local crafts & food • 1.8 mi • 4.7 ★")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Old Town Market",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .build()

        // 5. BannerSection (Promotions & Alerts)
        val bannerSection =
            BannerSection.Builder()
                .setSectionHeader(SectionHeader.Builder("Promotions & Alerts").build())
                .addItem(
                    Banner.Builder()
                        .setTitle("Special Offer")
                        .setSubtitle("Save 15% on EV charging at participating stations.")
                        .setLeadingImage(CarIcon.MEDIA_PLAYBACK, Banner.IMAGE_TYPE_SMALL)
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Banner clicked: EV Offer",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .build()

        // 6. RowSection (Categorized Results / Recent Searches)
        val resultsSection =
            RowSection.Builder()
                .setTitle(
                    CarText.create(
                        if (searchQuery.isEmpty()) "Recent Searches"
                        else "Results for '$searchQuery'"
                    )
                )
                .addItem(
                    Row.Builder()
                        .setTitle("Supercharger Station")
                        .addText("1.2 miles away • 8 stalls open")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Supercharger",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    Row.Builder()
                        .setTitle("Downtown Coffee Roasters")
                        .addText("0.4 miles away • Open now")
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Downtown Coffee",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .build()

        // 7. RowSection Copy (Recent Searches with Leading Icons)
        val searchIcon1 =
            CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_520)).build()
        val searchIcon2 =
            CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.patio)).build()

        val resultsWithIconsSection =
            RowSection.Builder()
                .setTitle(
                    CarText.create(
                        if (searchQuery.isEmpty()) "Recent Searches (with Icons)"
                        else "Results with Icons for '$searchQuery'"
                    )
                )
                .addItem(
                    Row.Builder()
                        .setTitle("Supercharger Station")
                        .addText("1.2 miles away • 8 stalls open")
                        .setImage(searchIcon1, Row.IMAGE_TYPE_SMALL)
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Supercharger",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .addItem(
                    Row.Builder()
                        .setTitle("Downtown Coffee Roasters")
                        .addText("0.4 miles away • Open now")
                        .setImage(searchIcon2, Row.IMAGE_TYPE_SMALL)
                        .setOnClickListener {
                            CarToast.makeText(
                                    carContext,
                                    "Navigating to Downtown Coffee",
                                    CarToast.LENGTH_SHORT,
                                )
                                .show()
                        }
                        .build()
                )
                .build()

        val searchHeader =
            SearchHeader.Builder(searchCallback)
                .setSearchHint("Search destinations or categories...")
                .setInitialSearchText(searchQuery)
                .setShowKeyboardByDefault(false)
                .setStartHeaderAction(Action.BACK)
                .build()

        return SectionedItemTemplate.Builder()
            .setSearchHeader(searchHeader)
            .addSection(chipSection)
            .addSection(condensedSection)
            .addSection(spotlightSection)
            .addSection(bannerSection)
            .addSection(resultsSection)
            .addSection(resultsWithIconsSection)
            .build()
    }
}
