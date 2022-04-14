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

package androidx.template.template

import androidx.glance.action.Action
import androidx.glance.unit.ColorProvider

/**
 * The semantic data required to build List Template layouts.
 *
 * @param headerIcon Logo icon, displayed in the glanceable header
 * @param listContent
 * @param header Main header text
 * @param title Text section main title
 * @param button Action button
 * @param backgroundColor Glanceable background color
 */
class ListTemplateData(
    val headerIcon: TemplateImageWithDescription,
    val listContent: List<ListTemplateItem> = listOf(),
    val header: TemplateText? = null,
    val title: TemplateText? = null,
    val button: TemplateTextButton? = null,
    val backgroundColor: ColorProvider? = null
) {

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + headerIcon.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + button.hashCode()
        result = 31 * result + listContent.hashCode()
        result = 31 * result + backgroundColor.hashCode()
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

        return true
    }
}

/**
 * The data required to display a list item
 *
 * @param title list item title text
 * @param body list item body text
 * @param action list item onClick action
 * @param image list item image
 * @param button Action button
 */
// TODO: Allow users to define a custom list item
public class ListTemplateItem(
    val title: TemplateText,
    val body: TemplateText?,
    val action: Action?,
    val image: TemplateImageWithDescription?,
    val button: TemplateButton?
) {

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (body?.hashCode() ?: 0)
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
        if (action != other.action) return false
        if (image != other.image) return false
        if (button != other.button) return false

        return true
    }
}
