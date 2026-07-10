/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.app.sample.showcase.common.screens.templatelayouts.listtemplates

import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.CarToast
import androidx.car.app.Screen
import androidx.car.app.annotations.RequiresCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/**
 * A screen demonstrating items with secondary actions and end images. This is similar to the
 * current EndImageAndActionsDemo except that uses SectionedItemTemplate and this one uses a
 * ListTemplate to depict the exact behavior. This is added as MapWithContentTemplate supports List
 * and not SectionedItem today as Content.
 */
@RequiresCarApi(8)
class RowEndImageAndActionsDemo(carContext: CarContext) : Screen(carContext) {

    private val actionClickable: Action =
        Action.Builder()
            .setTitle("Alert")
            .setIcon(CarIcon.ALERT)
            .setOnClickListener { showToast("Clicked on Alert") }
            .build()

    private val actionIconClickable: Action =
        Action.Builder()
            .setIcon(
                CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_commute_24px)
                    )
                    .build()
            )
            .setOnClickListener { showToast("Clicked on Commute") }
            .build()

    private val actionIconNonClickable: Action =
        Action.Builder()
            .setIcon(
                CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            R.drawable.baseline_directions_boat_filled_24,
                        )
                    )
                    .build()
            )
            // No setOnClickListener means this action is NOT interactable/clickable
            .build()

    private val endImage: CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.test_image_square))
            .build()

    override fun onGetTemplate(): Template {
        return buildListTemplate()
    }

    /** Helper method to build the ListTemplate. */
    private fun buildListTemplate(): ListTemplate {
        val rows = createItemListForEndImageAndActions()
        val itemListBuilder = ItemList.Builder()

        for (row in rows) {
            itemListBuilder.addItem(row)
        }

        val mapXAction =
            Action.Builder()
                .setTitle("Map+X this!")
                .setOnClickListener { screenManager.push(MapEndImageDemoScreen(carContext)) }
                .build()

        return ListTemplate.Builder()
            .setSingleList(itemListBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.row_end_image_and_actions_demo_title))
                    .setStartHeaderAction(Action.BACK)
                    .addEndHeaderAction(mapXAction)
                    .build()
            )
            .build()
    }

    /** Helper to create rows demonstrating complex focus and layout scenarios. */
    private fun createItemListForEndImageAndActions(): List<Row> {
        val rowList = mutableListOf<Row>()

        // --- Scenario 1: Only End Image Present ---
        rowList.add(
            Row.Builder()
                .setTitle("1. Image Only, non Clickable Row")
                .setEndImage(endImage, Row.IMAGE_TYPE_LARGE)
                .build()
        )

        // --- Scenario 2: End Image + 1 Action ---
        rowList.add(
            Row.Builder()
                .setTitle("2. Image + 1 Clickable Action")
                .setEndImage(endImage, Row.IMAGE_TYPE_LARGE)
                .addAction(actionClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        rowList.add(
            Row.Builder()
                .setTitle("2b. Image + 1 Non-Clickable Action")
                .setEndImage(endImage, Row.IMAGE_TYPE_LARGE)
                .addAction(actionIconNonClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        // --- Scenario 3. Image + 2 Actions, only one with a label ---
        rowList.add(
            Row.Builder()
                .setTitle("3. Image + 2 Actions, only one with a label")
                .setEndImage(endImage, Row.IMAGE_TYPE_LARGE)
                .addAction(actionIconClickable)
                .addAction(actionClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        // --- Scenario 4. No Image + 2 Labeled Actions ---
        rowList.add(
            Row.Builder()
                .setTitle("4. No Image + 2 Labeled Actions ")
                .addAction(actionClickable)
                .addAction(actionClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        // --- Scenario 5: No Image + 2 Clickable Icon Actions ---
        rowList.add(
            Row.Builder()
                .setTitle("5. No Image + 2 Icon only actions")
                .addAction(actionIconClickable)
                .addAction(actionIconClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        // --- Scenario 6: 2 Actions (Left Clickable, Right Not) ---
        rowList.add(
            Row.Builder()
                .setTitle("6. Left Clickable, Right Not Clickable")
                .addAction(actionIconClickable)
                .addAction(actionIconNonClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        // --- Scenario 7: 2 Actions (Left Not Clickable, Right Clickable) ---
        rowList.add(
            Row.Builder()
                .setTitle("7. Left Not Clickable, Right Clickable")
                .addAction(actionIconNonClickable)
                .addAction(actionIconClickable)
                .setOnClickListener { showToast("Clicked on Row") }
                .build()
        )

        // --- Scenario 8: End Image + 2 Actions ---
        rowList.add(
            Row.Builder()
                .setTitle("8. End Image + 2 Actions + Non Clickable Row")
                .setEndImage(endImage, Row.IMAGE_TYPE_LARGE)
                .addAction(actionClickable)
                .addAction(actionClickable)
                .build()
        )

        return rowList
    }

    private fun showToast(text: String) {
        Log.i(RowEndImageAndActionsDemo::class.simpleName, text)
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }

    /**
     * A new screen that displays the MapWithContentTemplate containing the exact same ListTemplate.
     */
    private inner class MapEndImageDemoScreen(carContext: CarContext) : Screen(carContext) {
        override fun onGetTemplate(): Template {
            val innerTemplate = this@RowEndImageAndActionsDemo.buildListTemplate()

            return MapWithContentTemplate.Builder().setContentTemplate(innerTemplate).build()
        }
    }
}
