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

import android.util.Log
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
import androidx.car.app.model.Header
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/**
 * A screen demonstrating SectionedItemTemplate with startHeaderImage and a primary background
 * action.
 */
@RequiresCarApi(9)
@OptIn(ExperimentalCarApi::class)
class EnhancedHeaderDemoScreen(carContext: CarContext) : Screen(carContext) {

    private fun showToast(text: String) {
        Log.i(EnhancedHeaderDemoScreen::class.simpleName, text)
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }

    override fun onGetTemplate(): Template {
        val rowSectionBuilder = RowSection.Builder().setTitle("Scrollable Content Section")
        for (i in 1..20) {
            rowSectionBuilder.addItem(
                Row.Builder()
                    .setTitle("Content item #$i")
                    .setOnClickListener { showToast("Clicked item #$i") }
                    .build()
            )
        }

        val backgroundIcon =
            CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.test_image_square))
                .build()

        val background = Background.Builder().setImage(backgroundIcon).build()

        val startHeaderIcon =
            CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_commute_24px))
                .build()

        val primaryAction =
            Action.Builder()
                .setTitle("Primary")
                .setBackgroundColor(CarColor.BLUE)
                .setFlags(Action.FLAG_PRIMARY)
                .setOnClickListener { showToast("Clicked Primary Action") }
                .build()

        val secondaryAction =
            Action.Builder()
                .setTitle("Secondary")
                .setBackgroundColor(CarColor.DEFAULT)
                .setOnClickListener { showToast("Clicked Secondary Action") }
                .build()

        val templateActionIcon =
            CarIcon.Builder(
                    IconCompat.createWithResource(carContext, R.drawable.ic_place_white_24dp)
                )
                .build()

        val templateAction =
            Action.Builder()
                .setIcon(templateActionIcon)
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener { showToast("Clicked Template Floating Action") }
                .build()

        return SectionedItemTemplate.Builder()
            .addSection(rowSectionBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.enhanced_header_demo_title))
                    .setSubtitle(
                        "Header with start header image and primary background color action"
                    )
                    .setBackground(background)
                    .setStartHeaderImage(startHeaderIcon)
                    .setStartHeaderAction(Action.BACK)
                    .addEndHeaderAction(primaryAction)
                    .addEndHeaderAction(secondaryAction)
                    .build()
            )
            .addAction(templateAction)
            .build()
    }
}
