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
import androidx.glance.action.Action
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.glance.Button
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/** Transforms semantic data into a composable layout for any glanceable */
abstract class GlanceTemplate<T> {

    /** Defines the data associated with this template. */
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
 * Contains the information required to display a string on a template.
 *
 * @param text string to be displayed
 * @param priority the priority order for this string, in orderable blocks
 * @param color text color
 */
public class TemplateText(
    val text: String,
    val priority: Int = 0,
    val color: ColorProvider? = null
) {

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + priority
        result = 31 * result + (color?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateText

        if (text != other.text) return false
        if (priority != other.priority) return false
        if (color != other.color) return false

        return true
    }
}

/**
 * Contains the information required to display an image on a template.
 *
 * @param image The image to display
 * @param description The image description, usually used as alt text
 */
public class TemplateImageWithDescription(val image: ImageProvider, val description: String) {

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
public sealed class TemplateButton(val action: Action) {

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
public class TemplateTextButton(action: Action, val text: String) : TemplateButton(action) {

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
public class TemplateImageButton(
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
    header: TemplateText?
) {
    Row(
        modifier = GlanceModifier.background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = headerIcon.image,
            contentDescription = headerIcon.description,
            modifier = GlanceModifier.height(24.dp).width(24.dp)
        )
        header?.let {
            Spacer(modifier = GlanceModifier.width(8.dp))
            val size =
                textSize(TemplateTextType.Title, DisplaySize.fromDpSize(LocalSize.current))

            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = header.text,
                style = TextStyle(fontSize = size, color = it.color),
                maxLines = 1
            )
        }
    }
}

/**
 * Default glanceable text section layout. Displays a list of reorderable text fields, ordered by
 * priority and styled according to the [TemplateTextType] of each field.
 *
 * @param textList the list of text fields to display in the block
 */
@Composable
internal fun TextSection(textList: List<TypedTemplateText>) {
    if (textList.isEmpty()) return

    val sorted = textList.sortedBy { it.text.priority }
    Column(modifier = GlanceModifier.background(Color.Transparent)) {
        sorted.forEach {
            // TODO: Spacing
            val size = textSize(it.textType, DisplaySize.fromDpSize(LocalSize.current))
            Text(
                it.text.text,
                style = TextStyle(fontSize = size, color = it.text.color),
                maxLines = maxLines(it.textType),
                modifier = GlanceModifier.background(Color.Transparent)
            )
        }
    }
}

/**
 * Displays a [TemplateButton]
 */
@Composable
internal fun TemplateButton(button: TemplateButton) {
    when (button) {
        is TemplateImageButton -> {
            // TODO: Specify sizing for image button
            val image = button.image
            Image(
                provider = image.image,
                contentDescription = image.description,
                modifier = GlanceModifier.clickable(button.action)
            )
        }
        is TemplateTextButton -> {
            Button(text = button.text, onClick = button.action)
        }
    }
}

/**
 * A [TemplateText] tagged with the [TemplateTextType] of the field. Templates can use the
 * text type to assign appropriate styling.
 *
 * @param text base text field
 * @param textType text field type type
 */
internal data class TypedTemplateText(val text: TemplateText, val textType: TemplateTextType)

/**
 * The text types that can be used with templates
 */
internal enum class TemplateTextType {
    Display,
    Title,
    Label,
    Body
}

private enum class DisplaySize {
    Small,
    Medium,
    Large;

    companion object {
        fun fromDpSize(dpSize: DpSize): DisplaySize =
            if (dpSize.width < 180.dp && dpSize.height < 120.dp) {
                Small
            } else if (dpSize.width < 280.dp && dpSize.height < 180.dp) {
                Medium
            } else {
                Large
            }
    }
}

private fun textSize(textClass: TemplateTextType, displaySize: DisplaySize): TextUnit =
    when (textClass) {
        // TODO: Does display scale?
        TemplateTextType.Display -> 45.sp
        TemplateTextType.Title -> {
            when (displaySize) {
                DisplaySize.Small -> 14.sp
                DisplaySize.Medium -> 16.sp
                DisplaySize.Large -> 22.sp
            }
        }
        TemplateTextType.Body -> {
            when (displaySize) {
                DisplaySize.Small -> 12.sp
                DisplaySize.Medium -> 14.sp
                DisplaySize.Large -> 14.sp
            }
        }
        TemplateTextType.Label -> {
            when (displaySize) {
                DisplaySize.Small -> 11.sp
                DisplaySize.Medium -> 12.sp
                DisplaySize.Large -> 14.sp
            }
        }
    }

private fun maxLines(textClass: TemplateTextType): Int =
    when (textClass) {
        TemplateTextType.Display -> 1
        TemplateTextType.Title -> 3
        TemplateTextType.Body -> 3
        TemplateTextType.Label -> 1
    }
