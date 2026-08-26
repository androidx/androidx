/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package androidx.car.app.sample.showcase.common.screens.templatelayouts.gridtemplates

import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.annotations.ExperimentalCarApi
import androidx.car.app.constraints.ConstraintManager.CONTENT_LIMIT_TYPE_GRID
import androidx.car.app.model.Action
import androidx.car.app.model.Action.BACK
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.sample.showcase.common.utils.CarContextAware
import androidx.car.app.sample.showcase.common.utils.createGridItem
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/** Creates a screen that demonstrates usage of the full screen [GridTemplate]. */
@OptIn(ExperimentalCarApi::class)
class GridTemplateDemoScreen(carContext: CarContext) :
    Screen(carContext), CarContextAware, DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private val itemLimit = determineListLimit(MAX_GRID_ITEMS, CONTENT_LIMIT_TYPE_GRID)
    private val pageImage = getCarIcon(R.drawable.test_image_square)
    private val pageIcon = getCarIcon(R.drawable.ic_fastfood_white_48dp)
    private val mapXIcon = getCarIcon(R.drawable.ic_emoji_food_beverage_white_48dp)
    private val pageTitle = getString(R.string.grid_template_demo_title)
    private val firstItemTitle = getString(R.string.non_actionable)
    private val secondItemTitle = getString(R.string.second_item)
    private val secondItemToastMessage = getString(R.string.second_item_toast_msg)
    private val thirdItemTitle = getString(R.string.third_item)
    private val thirdItemToastMessage = getString(R.string.third_item_checked_toast_msg)
    private val fourthItemTitle = getString(R.string.fourth_item)
    private val fifthItemTitle = getString(R.string.fifth_item)
    private val fifthItemToastMessage = getString(R.string.fifth_item_checked_toast_msg)
    private val sixthItemTitle = getString(R.string.sixth_item)
    private val sixthItemToastMessage = getString(R.string.sixth_item_toast_msg)
    private val checkedText = getString(R.string.checked_action_title)
    private val uncheckedText = getString(R.string.unchecked_action_title)
    private val stateTextOn = getString(R.string.on_action_title)
    private val stateTextOff = getString(R.string.off_action_title)
    private var isFourthItemLoading = false
    private var thirdItemChecked = false
    private var fourthItemChecked = true
    private var fifthItemChecked = false

    init {
        lifecycle.addObserver(this)
    }

    companion object {
        private const val MAX_GRID_ITEMS = 100
        private const val LOADING_TIME_MILLIS = 2000
    }

    override fun onStart(owner: LifecycleOwner) {
        isFourthItemLoading = false

        // Post a message that starts loading the fourth item for some time.
        triggerFourthItemLoading()
    }

    private fun makeGridItem(index: Int): GridItem =
        when (index) {
            0 -> buildFirstGridItem()
            1 -> buildSecondGridItem()
            2 -> buildThirdGridItem()
            3 -> buildFourthGridItem()
            4 -> buildFifthGridItem()
            5 -> buildSixthGridItem()
            else -> buildDefaultGridItem(index)
        }

    /** Grid item 0: Non-actionable item with icon and title. */
    private fun buildFirstGridItem(): GridItem =
        createGridItem(title = firstItemTitle, image = pageIcon)

    /** Grid item 1: Actionable item with icon, title, and toast click listener. */
    private fun buildSecondGridItem(): GridItem =
        createGridItem(
            title = secondItemTitle,
            image = pageIcon,
            clickListener = { makeToast(secondItemToastMessage).show() },
        )

    /** Grid item 2: Interactive item with toggle state text. */
    private fun buildThirdGridItem(): GridItem =
        createGridItem(
            title = thirdItemTitle,
            image = pageIcon,
            text = if (thirdItemChecked) checkedText else uncheckedText,
            clickListener = {
                thirdItemChecked = !thirdItemChecked
                makeToast("$thirdItemToastMessage: $thirdItemChecked").show()
                invalidate()
            },
        )

    /** Grid item 3: Item with simulated 2-second async loading state. */
    private fun buildFourthGridItem(): GridItem {
        val stateText = if (fourthItemChecked) stateTextOn else stateTextOff
        return if (isFourthItemLoading) {
            createGridItem(title = fourthItemTitle, text = stateText, isLoading = true)
        } else {
            createGridItem(
                title = fourthItemTitle,
                text = stateText,
                image = pageImage,
                clickListener = ::triggerFourthItemLoading,
            )
        }
    }

    /**
     * Changes the fourth item to a loading state for some time and changes it back to the loaded
     * state.
     */
    private fun triggerFourthItemLoading() {
        handler.post {
            isFourthItemLoading = true
            invalidate()

            handler.postDelayed(
                {
                    isFourthItemLoading = false
                    fourthItemChecked = !fourthItemChecked
                    invalidate()
                },
                LOADING_TIME_MILLIS.toLong(),
            )
        }
    }

    /** Grid item 4: Interactive item with checked/unchecked toast state. */
    private fun buildFifthGridItem(): GridItem =
        createGridItem(
            title = fifthItemTitle,
            image = pageIcon,
            clickListener = {
                fifthItemChecked = !fifthItemChecked
                makeToast("$fifthItemToastMessage: $fifthItemChecked").show()
                invalidate()
            },
        )

    /** Grid item 5: Item with icon, long title, long text, and toast click listener. */
    private fun buildSixthGridItem(): GridItem =
        createGridItem(
            title = sixthItemTitle,
            text = sixthItemTitle,
            image = pageIcon,
            clickListener = { makeToast(sixthItemToastMessage).show() },
        )

    /** Default grid item fallback for indices >= 6. */
    private fun buildDefaultGridItem(index: Int): GridItem =
        createGridItem(
            title = "${index + 1}th item",
            image = pageIcon,
            clickListener = { makeToast("Clicked ${index + 1}th item").show() },
        )

    override fun onGetTemplate(): Template {
        return buildGridTemplate()
    }

    /** Helper method to build the GridTemplate. */
    private fun buildGridTemplate(): GridTemplate {
        val gridItemList =
            ItemList.Builder()
                .apply { repeat(itemLimit) { index -> addItem(makeGridItem(index)) } }
                .build()

        val mapXAction =
            Action.Builder()
                .setTitle("Map+X this!")
                .setIcon(mapXIcon)
                .setOnClickListener { screenManager.push(MapGridDemoScreen(carContext)) }
                .build()

        val pageHeader =
            Header.Builder()
                .setStartHeaderAction(BACK)
                .setTitle(pageTitle)
                .addEndHeaderAction(mapXAction)
                .build()

        return GridTemplate.Builder().setHeader(pageHeader).setSingleList(gridItemList).build()
    }

    /**
     * A new screen that displays the MapWithContentTemplate containing the exact same GridTemplate.
     */
    private inner class MapGridDemoScreen(carContext: CarContext) : Screen(carContext) {

        override fun onGetTemplate(): Template {
            val innerTemplate = this@GridTemplateDemoScreen.buildGridTemplate()
            return MapWithContentTemplate.Builder().setContentTemplate(innerTemplate).build()
        }
    }
}
