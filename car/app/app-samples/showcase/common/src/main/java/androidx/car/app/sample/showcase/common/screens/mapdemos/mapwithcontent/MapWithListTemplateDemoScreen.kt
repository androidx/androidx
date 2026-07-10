/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.screens.mapdemos.mapwithcontent

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Action.FLAG_DEFAULT
import androidx.car.app.model.Action.FLAG_IS_PERSISTENT
import androidx.car.app.model.Action.FLAG_PRIMARY
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.CarText
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.OnClickListener
import androidx.car.app.model.ParkedOnlyOnClickListener
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.MapController
import androidx.car.app.navigation.model.MapWithContentTemplate
import androidx.car.app.sample.showcase.common.R
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModelFactory
import androidx.car.app.sample.showcase.common.utils.CarContextAware
import androidx.car.app.sample.showcase.common.utils.createAction
import androidx.car.app.sample.showcase.common.utils.createRow
import androidx.car.app.sample.showcase.common.utils.withApiGuard
import androidx.car.app.versioning.CarAppApiLevels

/**
 * Demonstrates how to present a [MapWithContentTemplate] alongside an interactive [ItemList].
 *
 * This screen highlights various row and action capabilities, including combinations of primary
 * buttons, secondary icons, and parked-only content.
 */
class MapWithListTemplateDemoScreen(carContext: CarContext) : Screen(carContext), CarContextAware {
    private val routingDemoModelFactory = RoutingDemoModelFactory(carContext)
    private var isFavorite: Boolean = false
    private val pageTitle = getString(R.string.map_template_list_demo_title)
    private val navigateTitle = getString(R.string.navigate)
    private val closeTitle = getString(R.string.close_action_title)
    private val parkedTitle = getString(R.string.parked_only_title)
    private val titlePrefix = getString(R.string.title_prefix)
    private val otherRowText = getString(R.string.other_row_text)
    private val firstLineText = getString(R.string.first_line_text)
    private val secondLineText = makeSecondLineText()
    private val clickedRow = getString(R.string.clicked_row_prefix)
    private val icClose = getCarIcon(R.drawable.ic_close_white_24dp)
    private val icFavorite = getCarIcon(R.drawable.ic_favorite_filled_white_24dp)
    private val icNonFavorite = getCarIcon(R.drawable.ic_favorite_white_24dp)
    private val icBug = getCarIcon(R.drawable.ic_bug_report_24px)
    private val icQuestion = getCarIcon(R.drawable.baseline_question_mark_24)
    private val icNavigate = getCarIcon(R.drawable.arrow_right_turn)
    private val msgFavorite = getString(R.string.favorite_toast_msg)
    private val msgNonFavorite = getString(R.string.not_favorite_toast_msg)
    private val msgBugReported = getString(R.string.bug_reported_toast_msg)
    private val msgParkedToast = getString(R.string.parked_toast_msg)
    private val listLimit = determineListLimit(MAX_LIST_ITEMS)

    companion object {
        private const val MAX_LIST_ITEMS = 100
    }

    override fun onGetTemplate(): Template {
        val demoList = makeListWithButtons()

        val actionFavorite =
            makeDemoAction(icon = if (isFavorite) icFavorite else icNonFavorite, title = null) {
                isFavorite = !isFavorite
                makeToast(if (isFavorite) msgFavorite else msgNonFavorite).show()
                invalidate()
            }
        val actionFinish = makeDemoAction(icon = icClose, title = null) { finish() }
        val header =
            Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(actionFavorite)
                .addEndHeaderAction(actionFinish)
                .setTitle(pageTitle)
                .build()

        val mapController =
            MapController.Builder()
                .setMapActionStrip(routingDemoModelFactory.mapActionStrip)
                .build()

        val actionBug =
            makeDemoAction(icon = icBug, flags = FLAG_IS_PERSISTENT, title = null) {
                makeToast(msgBugReported).show()
            }
        val actionStrip = ActionStrip.Builder().addAction(actionBug).build()

        val builder =
            MapWithContentTemplate.Builder()
                .setContentTemplate(
                    ListTemplate.Builder().setHeader(header).setSingleList(demoList).build()
                )
                .setActionStrip(actionStrip)
                .setMapController(mapController)

        return builder.build()
    }

    private fun makeListWithButtons(): ItemList {
        var rowIndex = 0
        val demoRows = mutableListOf<Row>() // make a list with unknown size, that will be iterated

        with(carContext) {
            withApiGuard(CarAppApiLevels.LEVEL_2) {
                demoRows.add(createRowWithParkedOnlyContent(rowIndex))
                demoRows.add(makeDemoRow(++rowIndex)) // this plain row is shown always

                withApiGuard(CarAppApiLevels.LEVEL_8) {
                    demoRows.add(createRowWithSecondaryAction(++rowIndex))
                    demoRows.add(createRowWithRightArrowChevron(++rowIndex))
                    demoRows.add(createOnePrimaryTextButtonRow(++rowIndex))
                    demoRows.add(createTwoButtonsTextAndIconRow(++rowIndex))
                    demoRows.add(createTwoButtonsTextAndTextRow(++rowIndex))
                    demoRows.add(createTwoButtonsTextTimerAndTextRow(++rowIndex))
                    demoRows.add(createNavigateWithIconAndTextRow(++rowIndex))
                    demoRows.add(createTwoButtonsIconAndTextRow(++rowIndex))
                    demoRows.add(createTwoButtonsIconAndIconRow(++rowIndex))
                }

                while (demoRows.size < listLimit) {
                    demoRows.add(makeDemoRow(++rowIndex, secondLine = secondLineText))
                }
            }
        }

        val listBuilder = ItemList.Builder()
        demoRows.take(listLimit).forEach { listBuilder.addItem(it) }
        return listBuilder.build()
    }

    private fun createRowWithParkedOnlyContent(index: Int): Row {
        return makeDemoRow(
            index,
            title = parkedTitle,
            clickListener = ParkedOnlyOnClickListener.create { makeToast(msgParkedToast).show() },
        )
    }

    private fun createRowWithSecondaryAction(index: Int): Row {
        return makeDemoRow(index, actions = listOf(makeDemoAction(index, null, icQuestion)))
    }

    private fun createRowWithRightArrowChevron(index: Int): Row {
        return makeDemoRow(index, isBrowsable = true)
    }

    private fun createOnePrimaryTextButtonRow(index: Int): Row {
        return makeDemoRow(
            index,
            actions = listOf(makeDemoAction(index, flags = FLAG_PRIMARY, icon = null)),
        )
    }

    private fun createTwoButtonsTextAndIconRow(index: Int): Row {
        val primary = makeDemoAction(index, flags = FLAG_PRIMARY, icon = null)
        val secondary = makeDemoAction(index, title = null)
        return makeDemoRow(index, firstLine = otherRowText, actions = listOf(primary, secondary))
    }

    private fun createTwoButtonsTextAndTextRow(index: Int): Row {
        val primaryAction = makeDemoAction(index, flags = FLAG_PRIMARY, icon = null)
        val secondaryAction = makeDemoAction(index, title = closeTitle, icon = null)
        return makeDemoRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun createTwoButtonsTextTimerAndTextRow(index: Int): Row {
        val primaryAction = makeDemoAction(index, flags = FLAG_DEFAULT)
        val secondaryAction = makeDemoAction(index, title = closeTitle, icon = null)
        return makeDemoRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun createNavigateWithIconAndTextRow(index: Int): Row {
        val primaryAction = makeDemoAction(index, icon = icNavigate, flags = FLAG_PRIMARY)
        val secondaryAction = makeDemoAction(index, title = closeTitle, icon = icClose)
        return makeDemoRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun createTwoButtonsIconAndTextRow(index: Int): Row {
        val primary = makeDemoAction(index, flags = FLAG_PRIMARY, title = null)
        val secondary = makeDemoAction(index, title = closeTitle, icon = null)
        return makeDemoRow(index, firstLine = otherRowText, actions = listOf(primary, secondary))
    }

    private fun createTwoButtonsIconAndIconRow(index: Int): Row {
        val primaryAction = makeDemoAction(index, null, icFavorite, FLAG_PRIMARY)
        val secondaryAction = makeDemoAction(index, icon = icNonFavorite)
        return makeDemoRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    /** Constructs a [Row] injected with screen-specific defaults. */
    private fun makeDemoRow(
        index: Int,
        title: String = "$titlePrefix $index",
        firstLine: String? = firstLineText,
        secondLine: CarText? = null,
        actions: List<Action>? = null,
        isBrowsable: Boolean? = null,
        clickListener: OnClickListener? = OnClickListener {
            makeToast("$clickedRow: $index").show()
        },
    ): Row {
        return createRow(title, firstLine, secondLine, actions, isBrowsable, clickListener)
    }

    /** Constructs an [Action] injected with screen-specific defaults. */
    private fun makeDemoAction(
        index: Int? = null,
        title: String? = navigateTitle,
        icon: CarIcon? = icQuestion,
        flags: Int? = null,
        clickListener: OnClickListener? = OnClickListener {
            makeToast("$clickedRow: ${index ?: ""}").show()
        },
    ): Action {
        return createAction(title, icon, flags, clickListener)
    }

    private fun makeSecondLineText(): CarText {
        // For row text, set text variants that fit best in different screen sizes.
        val defaultText = getString(R.string.second_line_text)
        val textVariant1 = "=================== $defaultText ==================="
        val textVariant2 = "------------------------ $defaultText -----------------------"
        val textVariant3 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<< $defaultText >>>>>>>>>>>>>>>>>>>>>>>>>>>>"
        return CarText.Builder(textVariant1)
            .addVariant(textVariant2)
            .addVariant(textVariant3)
            .addVariant(defaultText)
            .build()
    }
}
