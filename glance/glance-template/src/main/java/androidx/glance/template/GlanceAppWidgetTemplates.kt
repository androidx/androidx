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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Default header template layout implementation for AppWidgets, usually displayed at the top of the
 * glanceable in default layout implementations.
 *
 * @param headerIcon glanceable main logo icon
 * @param header main header text
 */
@Composable
internal fun AppWidgetTemplateHeader(
    headerIcon: TemplateImageWithDescription? = null,
    header: TemplateText? = null,
) {
    if (headerIcon == null && header == null) return

    Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
            val size = textSize(TextType.Title, DisplaySize.fromDpSize(LocalSize.current))
            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = header.text,
                style = TextStyle(fontSize = size, color = GlanceTheme.colors.onSurface),
                maxLines = 1
            )
        }
    }
}

/**
 * Default header template layout implementation for AppWidgets, usually displayed at the top of the
 * glanceable in default layout implementations by [HeaderBlock].
 *
 * @param headerBlock The glanceable header block to display
 */
@Composable
internal fun AppWidgetTemplateHeader(headerBlock: HeaderBlock) {
    AppWidgetTemplateHeader(
        headerBlock.icon,
        headerBlock.text,
    )
}

/**
 * Default text section layout for AppWidgets. Displays an ordered list of text fields, styled
 * according to the [TextType] of each field.
 *
 * @param textList the ordered list of text fields to display in the block
 */
@Composable
internal fun AppWidgetTextSection(textList: List<TemplateText>) {
    if (textList.isEmpty()) return

    Column {
        textList.forEachIndexed { index, item ->
            val size = textSize(item.type, DisplaySize.fromDpSize(LocalSize.current))
            Text(
                item.text,
                style = TextStyle(fontSize = size, color = GlanceTheme.colors.onSurface),
                maxLines = maxLines(item.type)
            )
            if (index < textList.size - 1) {
                Spacer(modifier = GlanceModifier.height(8.dp))
            }
        }
    }
}

/**
 * Displays a [TemplateButton] for AppWidget layouts.
 *
 * @param button text or image button
 * @param glanceModifier Glance modifier for further text or image button customization
 */
@Composable
internal fun AppWidgetTemplateButton(
    button: TemplateButton,
    glanceModifier: GlanceModifier = GlanceModifier
) {
    when (button) {
        is TemplateImageButton -> {
            // TODO: Specify sizing for image button
            val image = button.image
            Image(
                provider = image.image,
                contentDescription = image.description,
                modifier = glanceModifier.clickable(button.action)
            )
        }
        is TemplateTextButton -> {
            Button(
                text = button.text,
                onClick = button.action,
                style = TextStyle(color = GlanceTheme.colors.onPrimary),
                modifier = glanceModifier
            )
        }
    }
}

/**
 * Displays an [ImageBlock] as a single image for AppWidget layout template.
 *
 * @param imageBlock The [ImageBlock] data containing the main image for display
 * @param modifier Glance modifier for further image button customization
 */
@Composable
internal fun SingleImageBlockTemplate(
    imageBlock: ImageBlock,
    modifier: GlanceModifier = GlanceModifier
) {
    if (imageBlock.images.isNotEmpty()) {
        val mainImage = imageBlock.images[0]
        val imageSize: Dp =
            when (imageBlock.size) {
                ImageSize.Small -> 64.dp
                ImageSize.Medium -> 96.dp
                ImageSize.Large -> 128.dp
                ImageSize.Undefined -> 96.dp
                else -> 96.dp
            }
        Image(
            provider = mainImage.image,
            contentDescription = mainImage.description,
            modifier =
                if (ImageSize.Undefined == imageBlock.size) modifier else modifier.size(imageSize),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Displays a [TextBlock] with top three types of text lines for AppWidget layout template.
 *
 * @param textBlock The [TextBlock] data containing top three types of text lines.
 */
@Composable
internal fun TextBlockTemplate(textBlock: TextBlock) {
    AppWidgetTextSection(
        listOfNotNull(
            textBlock.text1,
            textBlock.text2,
            textBlock.text3,
        )
    )
}

/**
 * Displays a [HeaderBlock] with header information.
 *
 * @param headerBlock The [HeaderBlock] data containing the header information.
 */
@Composable
internal fun HeaderBlockTemplate(headerBlock: HeaderBlock?) {
    headerBlock?.let { AppWidgetTemplateHeader(it) }
}

/**
 * Displays an [ActionBlock] as a sequence of action buttons for AppWidget layout template.
 *
 * @param actionBlock The [ActionBlock] data containing a list of buttons for display
 */
@Composable
internal fun ActionBlockTemplate(actionBlock: ActionBlock?) {
    if (actionBlock?.actionButtons?.isNotEmpty() == true) {
        Row {
            actionBlock.actionButtons.forEach { button ->
                AppWidgetTemplateButton(button)
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
        }
    }
}

/**
 * Displays an entity of a block of texts from a [TextBlock] and an image from an [ImageBlock]
 * ordered by priority of the blocks with the default to the [TextBlock] being ahead of the
 * [ImageBlock] if they have the same priority.
 *
 * @param textBlock The [TextBlock] for an entity.
 * @param imageBlock The [ImageBlock] for an entity.
 * @param modifier The modifier for the textBlock in relation to the imageBlock.
 */
@Composable
internal fun TextAndImageBlockTemplate(
    textBlock: TextBlock,
    imageBlock: ImageBlock? = null,
    modifier: GlanceModifier = GlanceModifier
) {
    if (imageBlock == null || imageBlock.images.isEmpty()) {
        TextBlockTemplate(textBlock)
    } else {
        // Show first block by lower numbered priority
        if (textBlock.priority <= imageBlock.priority) {
            Column(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
                TextBlockTemplate(textBlock)
            }
            Spacer(modifier = GlanceModifier.width(16.dp))
            SingleImageBlockTemplate(imageBlock)
        } else {
            SingleImageBlockTemplate(imageBlock)
            Spacer(modifier = GlanceModifier.width(16.dp))
            Column(modifier = modifier, verticalAlignment = Alignment.Vertical.CenterVertically) {
                TextBlockTemplate(textBlock)
            }
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

private fun textSize(textClass: TextType, displaySize: DisplaySize): TextUnit =
    when (textClass) {
        // TODO: Does display scale?
        TextType.Display -> 45.sp
        TextType.Title -> {
            when (displaySize) {
                DisplaySize.Small -> 14.sp
                DisplaySize.Medium -> 16.sp
                DisplaySize.Large -> 22.sp
            }
        }
        TextType.Headline -> {
            when (displaySize) {
                DisplaySize.Small -> 12.sp
                DisplaySize.Medium -> 14.sp
                DisplaySize.Large -> 18.sp
            }
        }
        TextType.Body -> {
            when (displaySize) {
                DisplaySize.Small -> 12.sp
                DisplaySize.Medium -> 14.sp
                DisplaySize.Large -> 14.sp
            }
        }
        TextType.Label -> {
            when (displaySize) {
                DisplaySize.Small -> 11.sp
                DisplaySize.Medium -> 12.sp
                DisplaySize.Large -> 14.sp
            }
        }
        else -> 12.sp
    }

private fun maxLines(textClass: TextType): Int =
    when (textClass) {
        TextType.Display -> 1
        TextType.Title -> 3
        TextType.Body -> 3
        TextType.Label -> 1
        TextType.Headline -> 1
        else -> 1
    }
