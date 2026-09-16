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
import androidx.car.app.model.CondensedItem
import androidx.car.app.model.CondensedSection
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

@RequiresCarApi(9)
@OptIn(ExperimentalCarApi::class)
/**
 * A screen demonstrating SectionHeader configurations including headline and subtitle hierarchy.
 */
class SectionHeaderDemoScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val iconDefault = createOriginalCarIcon(R.drawable.test_image_square)
        val iconTrailing = createTintedCarIcon(R.drawable.ic_chevron_right_24)
        val iconAvatar = createTintedCarIcon(R.drawable.ic_face_24px)

        val recentEpisodesHeader =
            createSectionHeader(
                title = "Tech & Auto Podcasts",
                headline = "RECENT EPISODES",
                endIcon = iconTrailing,
                onClickListener = { showToast("Clicked Recent Episodes") },
            )

        val madeForYouHeader =
            createSectionHeader(
                title = "Daily Drive",
                headline = "Made for You",
                startIcon = iconDefault,
                imageType = SectionHeader.IMAGE_TYPE_SMALL,
                endIcon = iconTrailing,
                onClickListener = { showToast("Clicked Daily Drive") },
            )

        val artistSpotlightHeader =
            createSectionHeader(
                title = "Road Trip Favorites",
                headline = "Artist Spotlight",
                startIcon = iconAvatar,
                imageType = SectionHeader.IMAGE_TYPE_LARGE,
                endIcon = iconTrailing,
                onClickListener = { showToast("Clicked Artist Spotlight") },
            )

        val savedPlaylistsHeader =
            createSectionHeader(
                title = "Saved Playlists",
                subtitle = "Available offline",
                startIcon = iconDefault,
                imageType = SectionHeader.IMAGE_TYPE_SMALL,
                endIcon = iconTrailing,
                onClickListener = { showToast("Clicked Saved Playlists") },
            )

        val recommendedHeader =
            createSectionHeader(title = "New Releases", headline = "Recommended")

        val topChartsHeader = createSectionHeader(title = "Top Charts", subtitle = "Updated daily")

        val simpleTitleHeader = createSectionHeader("Simple Title")

        return SectionedItemTemplate.Builder()
            .addSection(
                RowSection.Builder()
                    .setSectionHeader(recentEpisodesHeader)
                    .addItem(
                        createRow(
                            "Episode 42: Designing for Glanceability",
                            "28 min • Car UX Weekly",
                        )
                    )
                    .addItem(createRow("Episode 41: Next-Gen In-Car UX", "34 min • Car UX Weekly"))
                    .build()
            )
            .addSection(
                CondensedSection.Builder()
                    .setSectionHeader(madeForYouHeader)
                    .addItem(
                        createCondensedItem(
                            "Morning Commute Mix",
                            "Upbeat tracks & news",
                            iconDefault,
                        )
                    )
                    .addItem(
                        createCondensedItem("Evening Wind Down", "Acoustic & chill", iconDefault)
                    )
                    .build()
            )
            .addSection(
                GridSection.Builder()
                    .setSectionHeader(artistSpotlightHeader)
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Indie Rock")
                            .setText("50 tracks")
                            .setImage(iconDefault)
                            .build()
                    )
                    .addItem(
                        GridItem.Builder()
                            .setTitle("Synthwave")
                            .setText("40 tracks")
                            .setImage(iconDefault)
                            .build()
                    )
                    .build()
            )
            .addSection(
                RowSection.Builder()
                    .setSectionHeader(savedPlaylistsHeader)
                    .addItem(createRow("90s Alternative", "Downloaded • 45 songs"))
                    .addItem(createRow("Acoustic Favorites", "Downloaded • 32 songs"))
                    .build()
            )
            .addSection(
                RowSection.Builder()
                    .setSectionHeader(recommendedHeader)
                    .addItem(createRow("Fresh Finds", "Updated Wednesday"))
                    .addItem(createRow("Release Radar", "Personalized for you"))
                    .build()
            )
            .addSection(
                RowSection.Builder()
                    .setSectionHeader(topChartsHeader)
                    .addItem(createRow("Global Top 50", "Daily chart update"))
                    .addItem(createRow("Viral Hits", "Trending worldwide"))
                    .build()
            )
            .addSection(
                RowSection.Builder()
                    .setSectionHeader(simpleTitleHeader)
                    .addItem(createRow("Row Item 1", "Sample row description"))
                    .addItem(createRow("Row Item 2", "Sample row description"))
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

    private fun showToast(text: String) {
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }

    private fun createTintedCarIcon(@DrawableRes resId: Int): CarIcon {
        return CarIcon.createTintedIcon(IconCompat.createWithResource(carContext, resId))
    }

    private fun createOriginalCarIcon(@DrawableRes resId: Int): CarIcon {
        return CarIcon.createOriginalIcon(IconCompat.createWithResource(carContext, resId))
    }

    private fun createSectionHeader(
        title: String,
        headline: String? = null,
        subtitle: String? = null,
        startIcon: CarIcon? = null,
        @SectionHeaderImageType imageType: Int = SectionHeader.IMAGE_TYPE_SMALL,
        endIcon: CarIcon? = null,
        onClickListener: (() -> Unit)? = null,
    ): SectionHeader {
        val builder = SectionHeader.Builder(CarText.create(title))
        if (headline != null) {
            builder.setHeadline(CarText.create(headline))
        }
        if (subtitle != null) {
            builder.setSubtitle(CarText.create(subtitle))
        }
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

    private fun createRow(title: String, subtitle: String? = null): Row {
        val builder = Row.Builder().setTitle(title)
        if (subtitle != null) {
            builder.addText(subtitle)
        }
        return builder.build()
    }

    private fun createCondensedItem(
        title: String,
        subtitle: String,
        image: CarIcon,
    ): CondensedItem {
        return CondensedItem.Builder()
            .setTitle(title)
            .setText(subtitle)
            .setLeadingImage(image)
            .build()
    }
}
