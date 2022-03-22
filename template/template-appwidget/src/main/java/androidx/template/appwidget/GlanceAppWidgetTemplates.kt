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

package androidx.template.appwidget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.template.template.TemplateButton
import androidx.template.template.TemplateImageButton
import androidx.template.template.TemplateImageWithDescription
import androidx.template.template.TemplateText
import androidx.template.template.TemplateTextButton

/**
 * Default header template layout implementation for AppWidgets, usually displayed at the top of the
 * glanceable in default layout implementations.
 *
 * @param headerIcon glanceable main logo icon
 * @param header main header text
 */
@Composable
internal fun AppWidgetTemplateHeader(
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
 * Default text section layout for AppWidgets. Displays an ordered list of text fields, styled
 * according to the [TemplateText.Type] of each field.
 *
 * @param textList the ordered list of text fields to display in the block
 */
@Composable
internal fun AppWidgetTextSection(textList: List<TemplateText>) {
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
 * Displays a [TemplateButton] for AppWidget layouts.
 */
@Composable
internal fun AppWidgetTemplateButton(button: TemplateButton) {
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
