/*
 * Copyright 2021 The Android Open Source Project
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

/**
 * The semantic data required to build Single Entity Template layouts. The template allows for
 * a header, text section with up to three text items, main image, and single action button.
 *
 * @param displayHeader True if the glanceable header should be displayed
 * @param headerIcon Header logo icon, image corner radius is ignored in default layouts
 * @param header Main header text
 * @param text1 Text section first text item
 * @param text2 Text section second text item
 * @param text3 Text section third text item
 * @param button Action button
 * @param image Main image content
 */

class SingleEntityTemplateData(
    val displayHeader: Boolean = true,
    val headerIcon: TemplateImageWithDescription? = null,
    val header: TemplateText? = null,
    val text1: TemplateText? = null,
    val text2: TemplateText? = null,
    val text3: TemplateText? = null,
    val button: TemplateButton? = null,
    val image: TemplateImageWithDescription? = null
) {

    override fun hashCode(): Int {
        var result = displayHeader.hashCode()
        result = 31 * result + (headerIcon?.hashCode() ?: 0)
        result = 31 * result + (header?.hashCode() ?: 0)
        result = 31 * result + (text1?.hashCode() ?: 0)
        result = 31 * result + (text2?.hashCode() ?: 0)
        result = 31 * result + (text3?.hashCode() ?: 0)
        result = 31 * result + (button?.hashCode() ?: 0)
        result = 31 * result + (image?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleEntityTemplateData

        if (displayHeader != other.displayHeader) return false
        if (headerIcon != other.headerIcon) return false
        if (header != other.header) return false
        if (text1 != other.text1) return false
        if (text2 != other.text2) return false
        if (text3 != other.text3) return false
        if (button != other.button) return false
        if (image != other.image) return false

        return true
    }
}
