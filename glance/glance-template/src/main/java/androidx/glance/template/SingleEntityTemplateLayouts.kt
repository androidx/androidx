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
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.template.GlanceTemplateAppWidget.Companion.sizeMin
import androidx.glance.template.GlanceTemplateAppWidget.Companion.sizeS

// TODO: Define template layouts for other surfaces
/**
 * Composable layout for a single entity template app widget. The template describes a widget layout
 * based around a single entity.
 *
 * @param data the data that defines the widget
 */
@Composable
fun SingleEntityTemplate(data: SingleEntityTemplateData) {
    when (LocalTemplateMode.current) {
        TemplateMode.Collapsed -> WidgetLayoutCollapsed(data)
        TemplateMode.Vertical -> WidgetLayoutVertical(data)
        TemplateMode.Horizontal -> WidgetLayoutHorizontal(data)
    }
}

@Composable
private fun WidgetLayoutCollapsed(data: SingleEntityTemplateData) {
    Column(modifier = createTopLevelModifier(data, true)) {
        HeaderBlockTemplate(data.headerBlock)
        // TODO(b/247613894): Does this need a minimum spacing?
        Spacer(modifier = GlanceModifier.defaultWeight())
        data.textBlock?.let { AppWidgetTextSection(textList(it.text1, it.text2)) }
    }
}

@Composable
private fun WidgetLayoutVertical(data: SingleEntityTemplateData) {

    Column(modifier = createTopLevelModifier(data)) {
        data.headerBlock?.let {
            HeaderBlockTemplate(data.headerBlock)
            // If other blocks exist, add space
            // TODO(b/247613894): These checks are a bit fragile, we'll have to add checks if any
            //  blocks are added to the template in the future. Is there a better way to do this?
            if (data.imageBlock != null || data.textBlock != null || data.actionBlock != null) {
                Spacer(modifier = GlanceModifier.height(16.dp))
            }
        }

        data.imageBlock?.let {
            SingleImageBlockTemplate(it, GlanceModifier.fillMaxWidth().defaultWeight())
            if (data.textBlock != null || data.actionBlock != null) {
                Spacer(modifier = GlanceModifier.height(16.dp))
            }
        }
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            data.textBlock?.let { AppWidgetTextSection(textList(it.text1, it.text2)) }
            // TODO(b/247613894): Fix for multiple actions
            if (LocalSize.current.width > sizeMin) {
                data.actionBlock?.let {
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    ActionBlockTemplate(it)
                }
            }
        }
    }
}

@Composable
private fun WidgetLayoutHorizontal(data: SingleEntityTemplateData) {
    Row(modifier = createTopLevelModifier(data)) {
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            data.headerBlock?.let {
                HeaderBlockTemplate(data.headerBlock)
                Spacer(modifier = GlanceModifier.height(16.dp))
            }
            Spacer(modifier = GlanceModifier.defaultWeight())

            data.textBlock?.let {
                val body = if (LocalSize.current.height >= sizeS) it.text3 else null
                AppWidgetTextSection(textList(it.text1, it.text2, body))
            }
            if (LocalSize.current.height > sizeMin) {
                data.actionBlock?.let {
                    Spacer(modifier = GlanceModifier.height(16.dp))
                    ActionBlockTemplate(it)
                }
            }
        }

        data.imageBlock?.let {
            Spacer(modifier = GlanceModifier.width(16.dp))
            SingleImageBlockTemplate(it, GlanceModifier.fillMaxHeight().defaultWeight())
        }
    }
}

private fun textList(
    title: TemplateText? = null,
    subtitle: TemplateText? = null,
    body: TemplateText? = null
): List<TemplateText> {
    val result = mutableListOf<TemplateText>()
    title?.let { result.add(TemplateText(it.text, TextType.Title)) }
    subtitle?.let { result.add(TemplateText(it.text, TextType.Label)) }
    body?.let { result.add(TemplateText(it.text, TextType.Body)) }

    return result
}

@Composable
private fun createTopLevelModifier(
    data: SingleEntityTemplateData,
    isImmersive: Boolean = false
): GlanceModifier {
    var modifier =
        GlanceModifier.fillMaxSize()
            .padding(16.dp)
            .cornerRadius(16.dp)
            .background(GlanceTheme.colors.primaryContainer)
    if (isImmersive && data.imageBlock?.images?.isNotEmpty() == true) {
        val mainImage = data.imageBlock.images[0]
        modifier = modifier.background(mainImage.image, ContentScale.Crop)
    }

    return modifier
}
