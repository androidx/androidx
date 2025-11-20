/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R

@RequiresCarApi(8)
@OptIn(ExperimentalCarApi::class)
/** A screen demonstrating lists with sectioned item list using the SectionedItemTemplate. */
class SimpleListDemoScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {

        val listBuilderTwo = ItemList.Builder()
        listBuilderTwo.addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext))

        return SectionedItemTemplate.Builder()
            .addSection(
                RowSection.Builder()
                    .setTitle("Simple Single Section RowList")
                    .addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext))
                    .addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext))
                    .build()
            )
            .addSection(
                RowSection.Builder()
                    .setTitle("Second Rowlist")
                    .addItem(buildRowForTemplate(R.string.sectioned_item_list_subtext))
                    .build()
            )
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.sectioned_item_list_demo_title))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private fun buildRowForTemplate(title: Int): Row {
        return Row.Builder().setTitle(carContext.getString(title)).build()
    }
}
