/*
 * Copyright 2022 The Android Open Source Project
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

@file:GlanceComposable

package androidx.glance.appwidget.template.demos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.template.GlanceTemplateAppWidget
import androidx.glance.appwidget.template.ListTemplate
import androidx.glance.currentState
import androidx.glance.template.ListStyle
import androidx.glance.template.ListTemplateData
import androidx.glance.template.ListTemplateItem
import androidx.glance.template.TemplateImageButton
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateText
import androidx.glance.template.TextType
import androidx.glance.unit.ColorProvider

/**
 * List demo with list items in full details and list header with action button using data and list
 * template from [BaseListDemoWidget].
 */
class FullHeaderActionListDemoWidget : BaseListDemoWidget() {
    @Composable
    override fun TemplateContent() = ListTemplateContent(ListStyle.Full, true, true, 1)

    override fun headerButtonAction(): Action = actionRunCallback<ListAddButtonAction>()

    override fun itemSelectAction(params: ActionParameters): Action =
        actionRunCallback<ListTemplateItemAction>(params)
}

/**
 * List demo with list items in full details and list header without action button using data and
 * list template from [BaseListDemoWidget].
 */
class FullHeaderListDemoWidget : BaseListDemoWidget() {
    @Composable
    override fun TemplateContent() = ListTemplateContent(ListStyle.Full, true)
}

/**
 * List demo with list items in some details without list header and action button using data and
 * list template from [BaseListDemoWidget].
 */
class NoHeaderListDemoWidget : BaseListDemoWidget() {
    @Composable
    override fun TemplateContent() = ListTemplateContent(ListStyle.Full)
}

/**
 * Brief list demo with list items in minimum details and compact form without list header and
 * action button using data and list template from [BaseListDemoWidget].
 */
class BriefListDemoWidget : BaseListDemoWidget() {
    @Composable
    override fun TemplateContent() = ListTemplateContent(ListStyle.Brief)
}

class FullActionListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullHeaderActionListDemoWidget()
}

class FullHeaderListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullHeaderListDemoWidget()
}

class NoHeaderListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoHeaderListDemoWidget()
}

class BriefListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BriefListDemoWidget()
}

/**
 * Base List Demo app widget creating demo data by [ListTemplateData] to layout by [ListTemplate].
 * It is overridable by list style, header action, and item selection action.
 */
abstract class BaseListDemoWidget : GlanceTemplateAppWidget() {
    /**
     * Defines the handling of header button action.
     */
    open fun headerButtonAction(): Action = actionRunCallback<DefaultNoopAction>()

    /**
     * Defines the handling of item select action.
     *
     * @param params action parameters for selection index
     */
    open fun itemSelectAction(params: ActionParameters): Action =
        actionRunCallback<DefaultNoopAction>()

    /**
     * Create list data and render list template by list style.
     *
     * @param listStyle styling the list by [ListStyle] based data details
     * @param initialNumItems initial number of list items to generate in the demo
     * @param showHeaderAction whether to show list header action button
     * @param showHeader whether to show list header as a whole
     */
    @Composable
    internal fun ListTemplateContent(
        listStyle: ListStyle,
        showHeader: Boolean = false,
        showHeaderAction: Boolean = false,
        initialNumItems: Int = 3,
    ) {
        val state = currentState<Preferences>()
        val content = mutableListOf<ListTemplateItem>()
        for (i in 1..(state[CountKey] ?: initialNumItems)) {
            var label = "Item $i"
            if (state[ItemClickedKey] == i) {
                label = "$label (selected)"
            }
            content.add(
                ListTemplateItem(
                    title = TemplateText("Title Medium", TextType.Title),
                    body = TemplateText(
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                        TextType.Body
                    ),
                    label = TemplateText(label, TextType.Label),
                    image = TemplateImageWithDescription(ImageProvider(R.drawable.compose), "$i"),
                    button = TemplateImageButton(
                        itemSelectAction(
                            actionParametersOf(ClickedKey to i)
                        ),
                        TemplateImageWithDescription(
                            ImageProvider(R.drawable.ic_favorite),
                            "button"
                        )
                    ),
                    action = itemSelectAction(actionParametersOf(ClickedKey to i)),
                )
            )
        }
        ListTemplate(
            ListTemplateData(
                header = if (showHeader) TemplateText(
                    "List Demo",
                    TextType.Title
                ) else null,
                headerIcon = if (showHeader) TemplateImageWithDescription(
                    ImageProvider(R.drawable.ic_widget),
                    "Logo"
                ) else null,
                button = if (showHeader && showHeaderAction) TemplateImageButton(
                    headerButtonAction(),
                    TemplateImageWithDescription(ImageProvider(R.drawable.ic_add), "Add item")
                ) else null,
                listContent = content,
                backgroundColor = ColorProvider(Color(0xDDD7E8CD)),
                listStyle = listStyle
            )
        )
    }
}

class DefaultNoopAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
    }
}

class ListAddButtonAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            var count = prefs[CountKey] ?: 1
            if (count >= MAX_ITEMS) {
                count = 0
                if (prefs[ItemClickedKey] != 1) {
                    prefs.minusAssign(ItemClickedKey)
                }
            }
            prefs[CountKey] = ++count
        }
        FullHeaderActionListDemoWidget().update(context, glanceId)
    }
}

class ListTemplateItemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) {
            it[ItemClickedKey] = parameters[ClickedKey] ?: -1
        }
        FullHeaderActionListDemoWidget().update(context, glanceId)
    }
}

val CountKey = intPreferencesKey("item_count_key")
val ItemClickedKey = intPreferencesKey("item_clicked_key")
val ClickedKey = ActionParameters.Key<Int>("item_clicked_key")
const val MAX_ITEMS = 10
