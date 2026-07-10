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
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarProgressBar
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.Header
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.core.graphics.drawable.IconCompat

/** A screen demonstrating sectioned item lists with progress bars and different configurations. */
@RequiresCarApi(8)
@OptIn(ExperimentalCarApi::class)
class ProgressBarDemoScreen(carContext: CarContext) : Screen(carContext) {

    private val actionClickable: Action =
        Action.Builder()
            .setTitle("Alert")
            .setIcon(CarIcon.ALERT)
            .setOnClickListener { showToast("Clicked on Alert") }
            .build()

    private val testImage: CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.test_image_square))
            .build()

    private val largeTestImage: CarIcon =
        CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.test_android_media))
            .build()

    override fun onGetTemplate(): Template {
        return SectionedItemTemplate.Builder()
            .addSection(createRegularRowsSection())
            .addSection(createRowConfigurationsSection())
            .addSection(createColoredRowsSection())
            .addSection(createGridSection(GridSection.ITEM_SIZE_SMALL))
            .addSection(createGridSection(GridSection.ITEM_SIZE_MEDIUM))
            .addSection(createGridSection(GridSection.ITEM_SIZE_LARGE))
            .addSection(createGridSection(GridSection.ITEM_SIZE_EXTRA_LARGE))
            .addSection(createGridSection(GridSection.ITEM_SIZE_EXTRA_LARGE, colored = true))
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.progress_bar_demo_title))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private fun createRegularRowsSection(): RowSection {
        val rows =
            (1..3).map { i ->
                val builder =
                    Row.Builder().setTitle("Row Item $i").setImage(testImage, Row.IMAGE_TYPE_LARGE)

                if (i <= 2) {
                    val progress = i * 0.3f
                    builder.addText(
                        "Row with Large Image + Default Progress Bar (${(progress * 100).toInt()}%)"
                    )
                    builder.setProgressBar(CarProgressBar.Builder(progress).build())
                } else {
                    builder.addText("Row with Large Image + No Progress Bar")
                }
                builder.build()
            }
        return RowSection.Builder().setTitle("Regular Rows").setItems(rows).build()
    }

    private fun createColoredRowsSection(): RowSection {
        val colors = listOf(CarColor.RED, CarColor.GREEN, CarColor.BLUE, CarColor.YELLOW)
        val colorNames = listOf("Red", "Green", "Blue", "Yellow")

        val rows =
            colors.zip(colorNames).map { (color, name) ->
                Row.Builder()
                    .setTitle("$name Progress Bar Row")
                    .addText("Colored progress bar example")
                    .setImage(testImage, Row.IMAGE_TYPE_LARGE)
                    .setProgressBar(CarProgressBar.Builder(0.5f).setColor(color).build())
                    .build()
            }
        return RowSection.Builder().setTitle("Colored Progress Bars").setItems(rows).build()
    }

    private fun createGridSection(
        itemSize: Int = GridSection.ITEM_SIZE_MEDIUM,
        colored: Boolean = false,
    ): GridSection {
        val sizeLabel =
            when (itemSize) {
                GridSection.ITEM_SIZE_SMALL -> "Small"
                GridSection.ITEM_SIZE_MEDIUM -> "Medium"
                GridSection.ITEM_SIZE_LARGE -> "Large"
                GridSection.ITEM_SIZE_EXTRA_LARGE -> "Extra Large"
                else -> "Unknown"
            }
        val title = if (colored) "Colored Grid Items ($sizeLabel)" else "Grid Items ($sizeLabel)"
        val builder = GridSection.Builder().setItemSize(itemSize).setTitle(title)

        // 1. Title + Progress (0.3)
        builder.addItem(
            GridItem.Builder()
                .setTitle("Progress 0.3")
                .setImage(largeTestImage, GridItem.IMAGE_TYPE_LARGE)
                .setProgressBar(CarProgressBar.Builder(0.3f).build())
                .build()
        )

        // 2. Title + Progress (0.6)
        builder.addItem(
            GridItem.Builder()
                .setTitle("Progress 0.6")
                .setImage(largeTestImage, GridItem.IMAGE_TYPE_LARGE)
                .setProgressBar(CarProgressBar.Builder(0.6f).build())
                .build()
        )

        // 3. Title + Progress (0.9)
        builder.addItem(
            GridItem.Builder()
                .setTitle("Progress 0.9")
                .setImage(largeTestImage, GridItem.IMAGE_TYPE_LARGE)
                .setProgressBar(CarProgressBar.Builder(0.9f).build())
                .build()
        )

        // 4. No Progress
        builder.addItem(
            GridItem.Builder()
                .setTitle("Primary")
                .setText("Subtitle")
                .setImage(largeTestImage, GridItem.IMAGE_TYPE_LARGE)
                .build()
        )

        // 5. Title + Colored Progress (0.5), Large Image
        builder.addItem(
            GridItem.Builder()
                .setTitle("Colored 0.5")
                .setImage(largeTestImage, GridItem.IMAGE_TYPE_LARGE)
                .setProgressBar(CarProgressBar.Builder(0.5f).setColor(CarColor.GREEN).build())
                .build()
        )

        // 6. Just Progress Bar, Small Icon
        builder.addItem(
            GridItem.Builder()
                .setImage(largeTestImage, GridItem.IMAGE_TYPE_LARGE)
                .setProgressBar(CarProgressBar.Builder(0.5f).setColor(CarColor.YELLOW).build())
                .build()
        )

        return builder.build()
    }

    private fun createRowConfigurationsSection(): RowSection {
        val rows = mutableListOf<Row>()

        // 1. With leading small image
        rows.add(
            Row.Builder()
                .setTitle("Small Leading Image")
                .addText("Progress bar with small image")
                .setImage(testImage, Row.IMAGE_TYPE_SMALL)
                .setProgressBar(CarProgressBar.Builder(0.3f).build())
                .build()
        )

        // 2. With leading large image
        rows.add(
            Row.Builder()
                .setTitle("Large Leading Image")
                .addText("Progress bar with large image")
                .setImage(testImage, Row.IMAGE_TYPE_LARGE)
                .setProgressBar(CarProgressBar.Builder(0.6f).build())
                .build()
        )

        // 3. Without leading image
        rows.add(
            Row.Builder()
                .setTitle("No Leading Image")
                .addText("Progress bar without leading image")
                .setProgressBar(CarProgressBar.Builder(0.9f).build())
                .build()
        )

        // 4. With actions (trailing or below depending on host)
        rows.add(
            Row.Builder()
                .setTitle("With Actions")
                .addText("Progress bar with 2 actions")
                .setProgressBar(CarProgressBar.Builder(0.5f).build())
                .addAction(actionClickable)
                .addAction(actionClickable)
                .build()
        )

        // 5. With 1 trailing action
        rows.add(
            Row.Builder()
                .setTitle("With 1 Action")
                .addText("Progress bar with 1 action")
                .setProgressBar(CarProgressBar.Builder(0.5f).build())
                .addAction(actionClickable)
                .build()
        )

        return RowSection.Builder().setTitle("Row Configurations").setItems(rows).build()
    }

    private fun showToast(text: String) {
        CarToast.makeText(carContext, text, CarToast.LENGTH_SHORT).show()
    }
}
