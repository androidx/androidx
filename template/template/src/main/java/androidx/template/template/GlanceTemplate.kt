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

import androidx.compose.runtime.Composable
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Transforms semantic data into a composable layout for any glanceable */
abstract class GlanceTemplate<T> {

    /** Defines the data associated with this template */
    abstract fun getData(state: Any?): T

    /** Default layout implementation for AppWidget glanceable, at "collapsed" display size. */
    @Composable
    abstract fun WidgetLayoutCollapsed()

    /** Default layout implementation for AppWidget glanceable, vertical orientation. */
    @Composable
    abstract fun WidgetLayoutVertical()

    /** Default layout implementation for AppWidget glanceable, horizontal orientation. */
    @Composable
    abstract fun WidgetLayoutHorizontal()

    // TODO: include layouts for every supported size and surface type
}

/**
 * Contains the information required to display an image on a template.
 *
 * @param image The image to display
 * @param description The image description, usually used as alt text
 */
class TemplateImageWithDescription(val image: ImageProvider, val description: String) {

    override fun hashCode(): Int = 31 * image.hashCode() + description.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateImageWithDescription
        return image == other.image && description == other.description
    }
}

/**
 * Contains the information required to display a button on a template.
 *
 * @param action The onClick action
 */
sealed class TemplateButton(val action: Action) {

    override fun hashCode(): Int = action.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return action == (other as TemplateButton).action
    }
}

/**
 * A text based [TemplateButton].
 *
 * @param action The onClick action
 * @param text The button display text
 */
class TemplateTextButton(action: Action, val text: String) : TemplateButton(action) {

    override fun hashCode(): Int = 31 * super.hashCode() + text.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        return text == (other as TemplateTextButton).text
    }
}

/**
 * An image based [TemplateButton].
 *
 * @param action The onClick action
 * @param image The button image
 */
class TemplateImageButton(
    action: Action,
    val image: TemplateImageWithDescription
) : TemplateButton(action) {

    override fun hashCode(): Int = 31 * super.hashCode() + image.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        return image == (other as TemplateImageButton).image
    }
}

/**
 * Default glanceable header layout implementation, usually displayed at the top of the
 * glanceable in default layout implementations.
 *
 * @param headerIcon glanceable main logo icon
 * @param header main header text
 */
@Composable
internal fun TemplateHeader(
    headerIcon: TemplateImageWithDescription,
    header: String? = null
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = headerIcon.image,
            contentDescription = headerIcon.description,
            modifier = GlanceModifier.height(24.dp).width(24.dp)
        )
        header?.let {
            Spacer(modifier = GlanceModifier.width(8.dp))
            // TODO: Text color customization
            Text(
                text = header,
                style = TextStyle(fontSize = 20.sp),
                maxLines = 1
            )
        }
    }
}
