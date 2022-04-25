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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.template.template.TemplateMode
import androidx.template.template.SingleEntityTemplateData
import androidx.template.template.TemplateText

// TODO: Define template layouts for other surfaces
/**
 * Composable layout for a single entity template app widget. The template describes a widget
 * layout based around a single entity.
 *
 * @param data the data that defines the widget
 */
@Composable
public fun SingleEntityTemplate(data: SingleEntityTemplateData) {
    // TODO: Add parameters here for other layout info, such as color preferences
    val height = LocalSize.current.height
    val width = LocalSize.current.width
    val mode = if (height <= Dp(240f) && width <= Dp(240f)) {
        TemplateMode.Collapsed
    } else if ((width / height) < (3.0 / 2.0)) {
        TemplateMode.Vertical
    } else {
        TemplateMode.Horizontal
    }
    when (mode) {
        TemplateMode.Collapsed -> WidgetLayoutCollapsed(data)
        TemplateMode.Vertical -> WidgetLayoutVertical(data)
        TemplateMode.Horizontal -> WidgetLayoutHorizontal(data)
    }
}

@Composable
private fun WidgetLayoutCollapsed(data: SingleEntityTemplateData) {
    var modifier = GlanceModifier.fillMaxSize().padding(16.dp)

    data.image?.let { image ->
        modifier = modifier.background(image.image, ContentScale.Crop)
    }
    Column(modifier = modifier) {
        data.headerIcon?.let { AppWidgetTemplateHeader(it, data.header) }
        Spacer(modifier = GlanceModifier.defaultWeight())
        AppWidgetTextSection(textList(data.text1, data.text2))
    }
}

@Composable
private fun WidgetLayoutVertical(data: SingleEntityTemplateData) {
    Column(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
        data.headerIcon?.let { AppWidgetTemplateHeader(it, data.header) }
        Spacer(modifier = GlanceModifier.height(16.dp))
        data.image?.let { image ->
            Image(
                provider = image.image,
                contentDescription = image.description,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            AppWidgetTextSection(textList(data.text1, data.text2))
            Spacer(modifier = GlanceModifier.defaultWeight())
            data.button?.let { button -> AppWidgetTemplateButton(button) }
        }
    }
}

@Composable
private fun WidgetLayoutHorizontal(data: SingleEntityTemplateData) {
    Row(modifier = GlanceModifier.fillMaxSize().padding(16.dp)) {
        Column(
            modifier =
            GlanceModifier.fillMaxHeight().background(Color.Transparent).defaultWeight()
        ) {
            data.headerIcon?.let { AppWidgetTemplateHeader(it, data.header) }
            Spacer(modifier = GlanceModifier.height(16.dp))
            Spacer(modifier = GlanceModifier.defaultWeight())

            // TODO: Extract small height as template constant
            val body =
                if (LocalSize.current.height > 240.dp) {
                    data.text3
                } else {
                    null
                }
            AppWidgetTextSection(textList(data.text1, data.text2, body))
            data.button?.let { button ->
                Spacer(modifier = GlanceModifier.height(16.dp))
                AppWidgetTemplateButton(button)
            }
        }
        data.image?.let { image ->
            Spacer(modifier = GlanceModifier.width(16.dp))
            Image(
                provider = image.image,
                contentDescription = image.description,
                modifier = GlanceModifier.fillMaxHeight().defaultWeight(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun textList(
    title: TemplateText? = null,
    subtitle: TemplateText? = null,
    body: TemplateText? = null
): List<TemplateText> {
    val result = mutableListOf<TemplateText>()
    title?.let { result.add(TemplateText(it.text, TemplateText.Type.Title)) }
    subtitle?.let { result.add(TemplateText(it.text, TemplateText.Type.Label)) }
    body?.let { result.add(TemplateText(it.text, TemplateText.Type.Body)) }

    return result
}
