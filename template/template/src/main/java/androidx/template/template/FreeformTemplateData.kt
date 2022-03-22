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

import androidx.glance.ImageProvider
import androidx.glance.unit.ColorProvider

/**
 * The semantic data required to build Freeform Template layouts
 *
 * @param backgroundColor Glanceable background color
 * @param headerIcon Logo icon, displayed in the glanceable header
 * @param actionIcon Action icon button
 * @param header Main header text
 * @param title Text section main title, priority ordered
 * @param subtitle Text section subtitle, priority ordered
 * @param backgroundImage Background image, if set replaces the glanceable background
 */
class FreeformTemplateData(
    val backgroundColor: ColorProvider,
    val headerIcon: TemplateImageWithDescription,
    val actionIcon: TemplateImageButton?,
    val header: TemplateText? = null,
    val title: TemplateText? = null,
    val subtitle: TemplateText? = null,
    val backgroundImage: ImageProvider? = null,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FreeformTemplateData

        if (backgroundColor != other.backgroundColor) return false
        if (headerIcon != other.headerIcon) return false
        if (actionIcon != other.actionIcon) return false
        if (header != other.header) return false
        if (title != other.title) return false
        if (subtitle != other.subtitle) return false
        if (backgroundImage != other.backgroundImage) return false

        return true
    }

    override fun hashCode(): Int {
        var result = backgroundColor.hashCode()
        result = 31 * result + headerIcon.hashCode()
        result = 31 * result + (actionIcon?.hashCode() ?: 0)
        result = 31 * result + (header?.hashCode() ?: 0)
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (subtitle?.hashCode() ?: 0)
        result = 31 * result + (backgroundImage?.hashCode() ?: 0)
        return result
    }
}
