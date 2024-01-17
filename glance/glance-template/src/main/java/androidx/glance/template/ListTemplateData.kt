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

package androidx.glance.template

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
 * @param headerBlock The header of the template by [HeaderBlock].
 * @param listContent List of items by [ListTemplateItem]. Default to empty list.
 * @param listStyle The level of data details by [ListStyle]. Default to the [ListStyle.Full] level.
 */
class ListTemplateData(
    val headerBlock: HeaderBlock? = null,
    val listContent: List<ListTemplateItem> = listOf(),
    val listStyle: ListStyle = ListStyle.Full
) {

    override fun hashCode(): Int {
        var result = headerBlock.hashCode()
        result = 31 * result + listContent.hashCode()
        result = 31 * result + listStyle.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListTemplateData

        if (headerBlock != other.headerBlock) return false
        if (listContent != other.listContent) return false
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
 * @param textBlock The text block for title, body, and other texts of the item.
 * @param imageBlock The image block for a list item defined by [ImageBlock].
 * @param actionBlock The item onClick action buttons defined by [ActionBlock].
 */
class ListTemplateItem(
    val textBlock: TextBlock,
    val imageBlock: ImageBlock? = null,
    val actionBlock: ActionBlock? = null,
) {

    override fun hashCode(): Int {
        var result = textBlock.hashCode()
        result = 31 * result + (imageBlock?.hashCode() ?: 0)
        result = 31 * result + (actionBlock?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ListTemplateItem

        if (textBlock != other.textBlock) return false
        if (imageBlock != other.imageBlock) return false
        if (actionBlock != other.actionBlock) return false

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
value class ListStyle private constructor(private val value: Int) {
    companion object {
        /**
         * Show list data in full details relative to the platform.
         */
        val Full: ListStyle = ListStyle(0)

        /**
         * Show list data in minimal details relative to the platform.
         */
        val Brief: ListStyle = ListStyle(1)
    }
}
