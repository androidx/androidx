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
import androidx.compose.ui.unit.Dp
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
 * @param type the [Type] of the item, used for styling
 */
public class TemplateText(val text: String, val type: Type) {

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateText

        if (text != other.text) return false
        if (type != other.type) return false

        return true
    }

    /**
     * The text types that can be used with templates. Types are set on [TemplateText] items and
     * can be used by templates to determine text styling.
     */
    public enum class Type {
        Display,
        Title,
        Label,
        Body
    }
}

/**
 * Contains the information required to display an image on a template.
 *
 * @param image The image to display
 * @param description The image description, usually used as alt text
 * @param cornerRadius The image corner radius in Dp
 */
public class TemplateImageWithDescription(
    val image: ImageProvider,
    val description: String,
    val cornerRadius: Dp = 16.dp
) {

    override fun hashCode(): Int =
        31 * image.hashCode() + description.hashCode() + cornerRadius.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TemplateImageWithDescription
        return image == other.image &&
            description == other.description &&
            cornerRadius == other.cornerRadius
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
    headerIcon: TemplateImageWithDescription?,
    header: TemplateText?
) {
    if (headerIcon == null && header == null) return

    Row(
        modifier = GlanceModifier.background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        headerIcon?.let {
            Image(
                provider = it.image,
                contentDescription = it.description,
                modifier = GlanceModifier.height(24.dp).width(24.dp)
            )
        }
        header?.let {
            if (headerIcon != null) {
                Spacer(modifier = GlanceModifier.width(8.dp))
            }

            val size =
                textSize(TemplateText.Type.Title, DisplaySize.fromDpSize(LocalSize.current))
            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = header.text,
                style = TextStyle(fontSize = size),
                maxLines = 1
            )
        }
    }
}

/**
 * Default glanceable text section layout. Displays a list of reorderable text fields, ordered by
 * priority and styled according to the [TemplateText.Type] of each field.
 *
 * @param textList the list of text fields to display in the block
 */
@Composable
internal fun TextSection(textList: List<TemplateText>) {
    if (textList.isEmpty()) return

    Column(modifier = GlanceModifier.background(Color.Transparent)) {
        textList.forEachIndexed { index, item ->
            val size = textSize(item.type, DisplaySize.fromDpSize(LocalSize.current))
            Text(
                item.text,
                style = TextStyle(fontSize = size),
                maxLines = maxLines(item.type),
                modifier = GlanceModifier.background(Color.Transparent)
            )
            if (index < textList.size - 1) {
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
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

private fun textSize(textClass: TemplateText.Type, displaySize: DisplaySize): TextUnit =
    when (textClass) {
        // TODO: Does display scale?
        TemplateText.Type.Display -> 45.sp
        TemplateText.Type.Title -> {
            when (displaySize) {
                DisplaySize.Small -> 14.sp
                DisplaySize.Medium -> 16.sp
                DisplaySize.Large -> 22.sp
            }
        }
        TemplateText.Type.Body -> {
            when (displaySize) {
                DisplaySize.Small -> 12.sp
                DisplaySize.Medium -> 14.sp
                DisplaySize.Large -> 14.sp
            }
        }
        TemplateText.Type.Label -> {
            when (displaySize) {
                DisplaySize.Small -> 11.sp
                DisplaySize.Medium -> 12.sp
                DisplaySize.Large -> 14.sp
            }
        }
    }

private fun maxLines(textClass: TemplateText.Type): Int =
    when (textClass) {
        TemplateText.Type.Display -> 1
        TemplateText.Type.Title -> 3
        TemplateText.Type.Body -> 3
        TemplateText.Type.Label -> 1
    }
