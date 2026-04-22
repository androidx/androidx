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
import androidx.car.app.CarToast
import androidx.car.app.CarToast.LENGTH_SHORT
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
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
import androidx.car.app.versioning.CarAppApiLevels
import androidx.core.graphics.drawable.IconCompat
import kotlin.math.min

/** Simple demo of how to present a map template with a list. */
class MapWithListTemplateDemoScreen(private val carContext: CarContext) : Screen(carContext) {
    private val routingDemoModelFactory = RoutingDemoModelFactory(carContext)
    private var isFavorite: Boolean = false

    private val pageTitle = resToText(R.string.map_template_list_demo_title)
    private val navigateTitle = resToText(R.string.navigate)
    private val closeTitle = resToText(R.string.close_action_title)
    private val parkedTitle = resToText(R.string.parked_only_title)
    private val titlePrefix = resToText(R.string.title_prefix)
    private val otherRowText = resToText(R.string.other_row_text)
    private val firstLineText = resToText(R.string.first_line_text)
    private val secondLineText = makeSecondLineText()
    private val clickedRow = resToText(R.string.clicked_row_prefix)
    private val icClose = resToIcon(R.drawable.ic_close_white_24dp)
    private val icFavorite = resToIcon(R.drawable.ic_favorite_filled_white_24dp)
    private val icNonFavorite = resToIcon(R.drawable.ic_favorite_white_24dp)
    private val icBug = resToIcon(R.drawable.ic_bug_report_24px)
    private val icQuestion = resToIcon(R.drawable.baseline_question_mark_24)
    private val icNavigate = resToIcon(R.drawable.arrow_right_turn)
    private val msgFavorite = resToText(R.string.favorite_toast_msg)
    private val msgNonFavorite = resToText(R.string.not_favorite_toast_msg)
    private val msgBugReported = resToText(R.string.bug_reported_toast_msg)
    private val msgParkedToast = resToText(R.string.parked_toast_msg)
    private val listLimit = determineListLimit()

    companion object {
        private const val MAX_LIST_ITEMS = 100
    }

    override fun onGetTemplate(): Template {
        val demoList = makeListWithButtons()

        val actionFavorite =
            makeAction(icon = if (isFavorite) icFavorite else icNonFavorite, title = null) {
                isFavorite = !isFavorite
                makeToast(if (isFavorite) msgFavorite else msgNonFavorite).show()
                invalidate()
            }
        val actionFinish = makeAction(icon = icClose, title = null) { finish() }
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
            makeAction(icon = icBug, flags = FLAG_IS_PERSISTENT, title = null) {
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

        withApiGuard(CarAppApiLevels.LEVEL_2) {
            demoRows.add(createRowWithParkedOnlyContent(rowIndex))
            demoRows.add(makeRow(++rowIndex)) // this plain row is shown always

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
                demoRows.add(makeRow(++rowIndex, secondLine = secondLineText))
            }
        }

        val listBuilder = ItemList.Builder()
        demoRows.take(listLimit).forEach { listBuilder.addItem(it) }
        return listBuilder.build()
    }

    private fun createRowWithParkedOnlyContent(index: Int): Row {
        return makeRow(
            index,
            title = parkedTitle,
            clickListener = ParkedOnlyOnClickListener.create { makeToast(msgParkedToast).show() },
        )
    }

    private fun createRowWithSecondaryAction(index: Int): Row {
        return makeRow(index, actions = listOf(makeAction(index, icon = icQuestion, title = null)))
    }

    private fun createRowWithRightArrowChevron(index: Int): Row {
        return makeRow(index, isBrowsable = true)
    }

    private fun createOnePrimaryTextButtonRow(index: Int): Row {
        return makeRow(
            index,
            actions = listOf(makeAction(index, flags = FLAG_PRIMARY, icon = null)),
        )
    }

    private fun createTwoButtonsTextAndIconRow(index: Int): Row {
        val primary = makeAction(index, flags = FLAG_PRIMARY, icon = null)
        val secondary = makeAction(index, title = null)
        return makeRow(index, firstLine = otherRowText, actions = listOf(primary, secondary))
    }

    private fun createTwoButtonsTextAndTextRow(index: Int): Row {
        val primaryAction = makeAction(index, flags = FLAG_PRIMARY, icon = null)
        val secondaryAction = makeAction(index, title = closeTitle, icon = null)
        return makeRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun createTwoButtonsTextTimerAndTextRow(index: Int): Row {
        val primaryAction = makeAction(index, flags = FLAG_DEFAULT)
        val secondaryAction = makeAction(index, title = closeTitle, icon = null)
        return makeRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun createNavigateWithIconAndTextRow(index: Int): Row {
        val primaryAction = makeAction(index, icon = icNavigate, flags = FLAG_PRIMARY)
        val secondaryAction = makeAction(index, title = closeTitle, icon = icClose)
        return makeRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun createTwoButtonsIconAndTextRow(index: Int): Row {
        val primary = makeAction(index, flags = FLAG_PRIMARY, title = null)
        val secondary = makeAction(index, title = closeTitle, icon = null)
        return makeRow(index, firstLine = otherRowText, actions = listOf(primary, secondary))
    }

    private fun createTwoButtonsIconAndIconRow(index: Int): Row {
        val primaryAction = makeAction(index, title = null, icon = icFavorite, flags = FLAG_PRIMARY)
        val secondaryAction = makeAction(index, icon = icNonFavorite)
        return makeRow(index, actions = listOf(primaryAction, secondaryAction))
    }

    private fun makeRow(
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
        val builder = Row.Builder().setTitle(title)
        clickListener?.let { builder.setOnClickListener(it) }
        firstLine?.let { builder.addText(it) }
        secondLine?.let { builder.addText(it) }
        actions?.forEach { builder.addAction(it) }
        isBrowsable?.let { builder.setBrowsable(it) }
        return builder.build()
    }

    private fun makeAction(
        index: Int? = null,
        title: String? = navigateTitle,
        icon: CarIcon? = icQuestion,
        flags: Int? = null,
        clickListener: OnClickListener? = OnClickListener {
            makeToast("$clickedRow: ${index ?: ""}").show()
        },
    ): Action {
        val builder = Action.Builder()
        title?.let { builder.setTitle(it) }
        icon?.let { builder.setIcon(it) }
        flags?.let { builder.setFlags(it) }
        clickListener?.let { builder.setOnClickListener(it) }
        return builder.build()
    }

    private fun makeSecondLineText(): CarText {
        // For row text, set text variants that fit best in different screen sizes.
        val defaultText = resToText(R.string.second_line_text)
        val textVariant1 = "=================== $defaultText ==================="
        val textVariant2 = "------------------------ $defaultText -----------------------"
        val textVariant3 = "<<<<<<<<<<<<<<<<<<<<<<<<<<<<< $defaultText >>>>>>>>>>>>>>>>>>>>>>>>>>>>"
        return CarText.Builder(textVariant1)
            .addVariant(textVariant2)
            .addVariant(textVariant3)
            .addVariant(defaultText)
            .build()
    }

    private fun resToIcon(resId: Int): CarIcon {
        return CarIcon.Builder(IconCompat.createWithResource(carContext, resId)).build()
    }

    private fun resToText(resId: Int): String {
        return carContext.getString(resId)
    }

    private fun makeToast(text: String): CarToast {
        return CarToast.makeText(carContext, text, LENGTH_SHORT)
    }

    private fun determineListLimit(): Int {
        return min(
            MAX_LIST_ITEMS,
            carContext
                .getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST),
        )
    }

    private inline fun withApiGuard(atLeastApiLevel: Int, action: () -> Unit) {
        val currentLevel = carContext.getCarAppApiLevel()
        if (currentLevel >= atLeastApiLevel) action()
    }
}
