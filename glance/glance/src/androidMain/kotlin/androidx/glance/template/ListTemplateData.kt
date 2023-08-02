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

package androidx.glance.template

import androidx.glance.action.Action
import androidx.glance.unit.ColorProvider

/**
 * The semantic data required to build List Template layouts.
 *
 * The data has a header for general information and a list of items defined by [ListTemplateItem].
 * The header can have a header icon followed by header text and optionally an action button. No
 * header would be shown if all header data are omitted.
 *
 * The data is independent of the layout presentation layer by each platform. For example, Glance
 * AppWidget can put an action button in the header while Glance Tile might layout the button
 * without a header. Only the level of data details can be indicated for use in the list style.
 *
 * @param headerIcon Logo icon displayed in the list header by [TemplateImageWithDescription].
 * Default to no display when null valued.
 * @param listContent List of items by [ListTemplateItem]. Default to empty list.
 * @param header Main header text by [TemplateText]. Default to no display when null valued.
 * @param title Text section title by [TemplateText] independent of header. Default to no display
 * when null valued.
 * @param button List action button by [TemplateButton] that can be a part of the header with its
 * own icon image. Default to no display when null valued.
 * @param backgroundColor Glanceable background color by [ColorProvider] for the whole list.
 * Default to the system background color.
 * @param listStyle The level of data details by [ListStyle]. Default to the [ListStyle.Full] level.
 */
class ListTemplateData(
    val headerIcon: TemplateImageWithDescription? = null,
    val listContent: List<ListTemplateItem> = listOf(),
    val header: TemplateText? = null,
    val title: TemplateText? = null,
    val button: TemplateButton? = null,
    val backgroundColor: ColorProvider? = null,
    val listStyle: ListStyle = ListStyle.Full
) {

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + headerIcon.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + button.hashCode()
        result = 31 * result + listContent.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + listStyle.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListTemplateData

        if (header != other.header) return false
        if (headerIcon != other.headerIcon) return false
        if (title != other.title) return false
        if (button != other.button) return false
        if (listContent != other.listContent) return false
        if (backgroundColor != other.backgroundColor) return false
        if (listStyle != other.listStyle) return false

        return true
    }
}

/**
 * The data required to display a list item.
 *
 * An item can have brief title with an icon, detailed body text, categorical label, and associated
 * action button. The whole list item can also have a separate action such as item selection.
 *
 * The list style can indicate the level of details of what item elements to show deemed appropriate
 * by developers. For example, Glance AppWidget developer might only show item icon when list header
 * is displayed.
 *
 * @param title The list item title text by [TemplateText] with brief information.
 * @param body The list item body text by [TemplateText] with detailed information. Default to no
 * display when null valued.
 * @param label The list item label text by [TemplateText]. Default to no display when null valued.
 * @param action The list item onClick action by [Action]. Default to no action when null valued.
 * @param image The list item icon image by [TemplateImageWithDescription]. Default to no display
 * when null valued.
 * @param button The item action button by [TemplateButton] that can have its own icon and action
 * independent of item icon and item action. Default to no display when null valued.
 */
// TODO: Allow users to define a custom list item
class ListTemplateItem(
    val title: TemplateText,
    val body: TemplateText? = null,
    val label: TemplateText? = null,
    val action: Action? = null,
    val image: TemplateImageWithDescription? = null,
    val button: TemplateButton? = null,
) {

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (body?.hashCode() ?: 0)
        result = 31 * result + (label?.hashCode() ?: 0)
        result = 31 * result + (action?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + (button?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListTemplateItem

        if (title != other.title) return false
        if (body != other.body) return false
        if (label != other.label) return false
        if (action != other.action) return false
        if (image != other.image) return false
        if (button != other.button) return false

        return true
    }
}

/**
 * The List Styles supported by the list template elements. Use as relative measure to developers on
 * the level of data details and format deemed appropriate for each platform.
 *
 * For example, the Full style can refer to all data fields while the Brief style can refer to a
 * compact form with only the list item title but no list header for Glance AppWidget. And for
 * Glance Tile, the Full style might only make use of the header icon for the whole header while
 * the Brief style shows no header.
 */
@JvmInline
public value class ListStyle private constructor(private val value: Int) {
    public companion object {
        /**
         * Show list data in full details relative to the platform.
         */
        public val Full: ListStyle = ListStyle(0)

        /**
         * Show list data in minimal details relative to the platform.
         */
        public val Brief: ListStyle = ListStyle(1)
    }
}