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
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CondensedItem
import androidx.car.app.model.Header
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.SpotlightSection
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

@RequiresCarApi(9)
@OptIn(ExperimentalCarApi::class)
/** A screen demonstrating SpotlightSection within the SectionedItemTemplate. */
class SpotlightSectionDemoScreen(carContext: CarContext) : Screen(carContext) {
    private val testImage: CarIcon by lazy {
        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.test_image_square))
            .build()
    }

    override fun onGetTemplate(): Template {

        val builder = SectionedItemTemplate.Builder()
        for (i in 1..7) {
            val sectionBuilder = SpotlightSection.Builder(testImage)
            sectionBuilder.setTitle("Image with $i items")
            for (j in 1..i) {
                sectionBuilder.addItem(buildCondensedItem("Item $j", "Subtitle"))
            }
            builder.addSection(sectionBuilder.build())
        }
        return builder
            .setHeader(
                Header.Builder()
                    .setTitle("Spotlight Section Demo")
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private fun buildCondensedItem(title: String, subtitle: String): CondensedItem {
        return CondensedItem.Builder()
            .setTitle(title)
            .setText(subtitle)
            .setLeadingImage(testImage)
            .build()
    }
}
