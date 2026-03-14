/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.car.app.sample.showcase.common.screens.templatelayouts.tabtemplates

import android.text.SpannableString
import android.text.Spanned
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarIcon
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridSection
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.RowSection
import androidx.car.app.model.SectionedItemTemplate
import androidx.car.app.model.Tab
import androidx.car.app.model.TabContents
import androidx.car.app.model.TabTemplate
import androidx.car.app.model.TabTemplate.TabCallback
import androidx.car.app.model.Template
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.sample.showcase.common.screens.templatelayouts.messagetemplates.ShortMessageTemplateDemoScreen
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.graphics.drawable.IconCompat

/** Creates a screen that demonstrates usage of the full screen [TabTemplate]. */
@OptIn(ExperimentalCarApi::class)
class TabTemplateDemoScreen(carContext: CarContext) : Screen(carContext) {
    private companion object {
        private const val LIST_SIZE = 10

        private val TAB_PROPERTIES =
            mapOf(
                R.string.tab_title_message to R.drawable.ic_explore_white_24dp,
                R.string.tab_title_pane to R.drawable.ic_face_24px,
                R.string.tab_title_sectioned_item to R.drawable.ic_place_white_24dp,
                R.string.tab_title_grid to R.drawable.ic_favorite_white_24dp,
            )
    }

    private val mTabs =
        TAB_PROPERTIES.entries.mapIndexed { index, entry ->
            Tab.Builder()
                .setContentId("$index")
                .setTitle(carContext.getString(entry.key))
                .setIcon(
                    CarIcon.Builder(IconCompat.createWithResource(carContext, entry.value)).build()
                )
                .build()
        }
    private var mActiveContentId: String = "0"
    private var counter: Int = 0

    override fun onGetTemplate(): Template {
        return TabTemplate.Builder(
                object : TabCallback {
                    override fun onTabSelected(tabContentId: String) {
                        mActiveContentId = tabContentId
                        invalidate()
                    }
                }
            )
            .apply { mTabs.forEach(this::addTab) }
            .setActiveTabContentId(mActiveContentId)
            .setHeaderAction(Action.APP_ICON)
            .setTabContents(
                TabContents.Builder(
                        when (mActiveContentId) {
                            "0" -> createShortMessageTemplate()
                            "1" -> createPaneTemplate()
                            "2" -> createSectionedItemTemplate()
                            "3" -> createGridTemplate()
                            else -> throw IllegalStateException("Invalid tab id: $mActiveContentId")
                        }
                    )
                    .build()
            )
            .build()
    }

    private fun createSectionedItemTemplate(): Template {
        if (carContext.carAppApiLevel < CarAppApiLevels.LEVEL_8) {
            return MessageTemplate.Builder(
                    "The current API is only " +
                        carContext.carAppApiLevel +
                        " and this tab needs API 8 or above :("
                )
                .build()
        }
        return SectionedItemTemplate.Builder()
            .addSection(
                RowSection.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("Tapping on any row below opens the message template demo")
                            .addText("This demonstrates a step after the tab template")
                            .build()
                    )
                    .apply {
                        for (i in 0..<LIST_SIZE) {
                            // Give some variety to the titles
                            val rowLeadingChar = 'A' + (i % 26)
                            addItem(buildRowForTemplate("$rowLeadingChar Row $i", true))
                        }
                    }
                    .setTitle("")
                    .build()
            )
            .addSection(
                RowSection.Builder()
                    .addItem(
                        Row.Builder()
                            .setTitle("A special row that changes text!")
                            .apply {
                                val string = "The count is $counter"
                                val subtitle = SpannableString(string)
                                val color =
                                    when {
                                        counter > 0 -> CarColor.GREEN
                                        counter == 0 -> CarColor.SECONDARY
                                        else -> CarColor.RED
                                    }
                                subtitle.setSpan(
                                    ForegroundCarColorSpan.create(color),
                                    0,
                                    string.length,
                                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE,
                                )
                                addText(subtitle)
                            }
                            .addAction(
                                Action.Builder()
                                    .setTitle("--")
                                    .setOnClickListener {
                                        counter--
                                        invalidate()
                                    }
                                    .build()
                            )
                            .addAction(
                                Action.Builder()
                                    .setTitle("++")
                                    .setOnClickListener {
                                        counter++
                                        invalidate()
                                    }
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .addSection(
                GridSection.Builder()
                    .apply {
                        for (i in 0..<LIST_SIZE) {
                            addItem(buildGridItemForTemplate("$i"))
                        }
                    }
                    .build()
            )
            .setScrollStatePersistenceStrategy(SectionedItemTemplate.SCROLL_STATE_PRESERVE_INDEX)
            .setAlphabeticalIndexingStrategy(
                SectionedItemTemplate.ALPHABETICAL_INDEXING_TITLE_AS_IS
            )
            .addAction(createFabBackAction())
            .build()
    }

    private fun buildRowForTemplate(title: String, clickable: Boolean): Row {
        val rowBuilder = Row.Builder().setTitle("$title")
        if (clickable) {
            rowBuilder.setOnClickListener {
                screenManager.push(ShortMessageTemplateDemoScreen(carContext))
            }
        }
        return rowBuilder.build()
    }

    private fun createGridTemplate(): GridTemplate {
        val listBuilder = ItemList.Builder()
        for (i in 0..<LIST_SIZE) {
            listBuilder.addItem(buildGridItemForTemplate(i.toString()))
        }
        return GridTemplate.Builder()
            .setSingleList(listBuilder.build())
            .addAction(createFabBackAction())
            .build()
    }

    private fun buildGridItemForTemplate(title: CharSequence?): GridItem {
        return GridItem.Builder()
            .setImage(
                CarIcon.Builder(
                        IconCompat.createWithResource(
                            carContext,
                            R.drawable.ic_emoji_food_beverage_white_48dp,
                        )
                    )
                    .build(),
                GridItem.IMAGE_TYPE_ICON,
            )
            .setTitle(title)
            .build()
    }

    private fun createShortMessageTemplate(): MessageTemplate {
        val action =
            Action.Builder()
                .setTitle(carContext.getString(R.string.back_caps_action_title))
                .setIcon(CarIcon.BACK)
                .setOnClickListener { screenManager.pop() }
                .build()
        return MessageTemplate.Builder(carContext.getString(R.string.msg_template_demo_text))
            .setIcon(
                CarIcon.Builder(IconCompat.createWithResource(carContext, R.drawable.ic_launcher))
                    .build()
            )
            .addAction(action)
            .build()
    }

    private fun createPaneTemplate(): PaneTemplate {
        val paneBuilder =
            Pane.Builder()
                .setImage(
                    CarIcon.Builder(
                            IconCompat.createWithResource(carContext, R.drawable.ic_launcher)
                        )
                        .build()
                )
        for (i in 0..<LIST_SIZE) {
            paneBuilder.addRow(buildRowForTemplate("$i Row", false))
        }
        return PaneTemplate.Builder(paneBuilder.build()).build()
    }

    private fun createFabBackAction(): Action {
        val action =
            Action.Builder()
                .setIcon(CarIcon.BACK)
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener { screenManager.pop() }
                .build()
        return action
    }
}
