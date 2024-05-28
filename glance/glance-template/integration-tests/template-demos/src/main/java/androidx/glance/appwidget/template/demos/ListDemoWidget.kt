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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.template.ActionBlock
import androidx.glance.template.GlanceTemplateAppWidget
import androidx.glance.template.HeaderBlock
import androidx.glance.template.ImageBlock
import androidx.glance.template.ImageSize
import androidx.glance.template.ListStyle
import androidx.glance.template.ListTemplate
import androidx.glance.template.ListTemplateData
import androidx.glance.template.ListTemplateItem
import androidx.glance.template.TemplateImageButton
import androidx.glance.template.TemplateImageWithDescription
import androidx.glance.template.TemplateText
import androidx.glance.template.TextBlock
import androidx.glance.template.TextType

/**
 * List demo with list items in full details and list item action button using data and list
 * template from [BaseListDemoWidget].
 */
class FullHeaderActionListDemoWidget : BaseListDemoWidget() {
    @Composable override fun TemplateContent() = ListTemplateContent(ListStyle.Full, true)

    override fun itemSelectAction(params: ActionParameters): Action =
        actionRunCallback<ListTemplateItemAction>(params)
}

/**
 * List demo with list items in full details and list header without action button using data and
 * list template from [BaseListDemoWidget] with custom theme.
 */
class FullHeaderListThemedDemoWidget : BaseListDemoWidget() {
    @Composable override fun TemplateContent() = ListTemplateContent(ListStyle.Full, true, true)
}

/**
 * List demo with list items in some details without list header and action button using data and
 * list template from [BaseListDemoWidget].
 */
class NoHeaderListDemoWidget : BaseListDemoWidget() {
    @Composable override fun TemplateContent() = ListTemplateContent(ListStyle.Full)
}

/**
 * Brief list demo with list items in minimum details and compact form without list header and
 * action button using data and list template from [BaseListDemoWidget].
 */
class BriefListDemoWidget : BaseListDemoWidget() {
    @Composable override fun TemplateContent() = ListTemplateContent(ListStyle.Brief)
}

class FullActionListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullHeaderActionListDemoWidget()
}

class FullHeaderThemedListReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullHeaderListThemedDemoWidget()
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
     * @param showHeader whether to show list header as a whole
     * @param customTheme whether to override Glance system theme with custom theme
     */
    @Composable
    internal fun ListTemplateContent(
        listStyle: ListStyle,
        showHeader: Boolean = false,
        customTheme: Boolean = false,
        initialNumItems: Int = MAX_ITEMS,
    ) {
        GlanceTheme(if (customTheme) PalmLeafScheme.colors else GlanceTheme.colors) {
            val state = currentState<Preferences>()
            val content = mutableListOf<ListTemplateItem>()
            for (i in 1..(state[CountKey] ?: initialNumItems)) {
                var label = "Item $i"
                if (state[ItemClickedKey] == i) {
                    label = "$label (selected)"
                }
                content.add(
                    ListTemplateItem(
                        textBlock =
                            TextBlock(
                                text1 = TemplateText("Title Medium", TextType.Title),
                                text2 =
                                    if (listStyle == ListStyle.Full)
                                        TemplateText(
                                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
                                            TextType.Body
                                        )
                                    else null,
                                text3 =
                                    if (listStyle == ListStyle.Full)
                                        TemplateText(label, TextType.Label)
                                    else null,
                                priority = 1,
                            ),
                        imageBlock =
                            ImageBlock(
                                images =
                                    listOf(
                                        TemplateImageWithDescription(
                                            ImageProvider(R.drawable.palm_leaf),
                                            "$i"
                                        )
                                    ),
                                size = ImageSize.Medium,
                                priority = 0, // ahead of textBlock
                            ),
                        actionBlock =
                            ActionBlock(
                                actionButtons =
                                    listOf(
                                        TemplateImageButton(
                                            itemSelectAction(actionParametersOf(ClickedKey to i)),
                                            TemplateImageWithDescription(
                                                ImageProvider(R.drawable.ic_bookmark),
                                                "button"
                                            )
                                        ),
                                    ),
                            ),
                    )
                )
            }
            ListTemplate(
                ListTemplateData(
                    headerBlock =
                        if (showHeader)
                            HeaderBlock(
                                text = TemplateText("List Demo", TextType.Title),
                                icon =
                                    TemplateImageWithDescription(
                                        ImageProvider(R.drawable.ic_widgets),
                                        "Logo"
                                    ),
                            )
                        else null,
                    listContent = content,
                    listStyle = listStyle
                )
            )
        }
    }
}

class DefaultNoopAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {}
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
