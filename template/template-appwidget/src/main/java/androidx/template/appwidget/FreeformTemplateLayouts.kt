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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import androidx.template.template.FreeformTemplateData
import androidx.template.template.TemplateMode
import androidx.template.template.TemplateText

/**
 * Composable layout for a freeform template app widget. The freeform template is optimized to
 * highlight a single piece of data.
 *
 * @param data the data that defines the widget
 */
@Composable
public fun FreeformTemplate(data: FreeformTemplateData) {
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
private fun WidgetLayoutCollapsed(data: FreeformTemplateData) {
    Column(
        modifier = createTopLevelModifier(data.backgroundColor, data.backgroundImage),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppWidgetTextSection(textList(data.title, data.subtitle))
    }
    data.actionIcon?.let { icon -> AppWidgetTemplateButton(icon) }
}

@Composable
private fun WidgetLayoutVertical(data: FreeformTemplateData) {
    Column(
        modifier = createTopLevelModifier(data.backgroundColor, data.backgroundImage),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppWidgetTemplateHeader(data.headerIcon, data.header)
        AppWidgetTextSection(textList(data.title, data.subtitle))
    }
    data.actionIcon?.let { icon -> AppWidgetTemplateButton(icon) }
}

@Composable
private fun WidgetLayoutHorizontal(data: FreeformTemplateData) {
    Column(
        modifier = createTopLevelModifier(data.backgroundColor, data.backgroundImage),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppWidgetTemplateHeader(data.headerIcon, data.header)
        AppWidgetTextSection(textList(data.title, data.subtitle))
    }
    data.actionIcon?.let { icon -> AppWidgetTemplateButton(icon) }
}

private fun createTopLevelModifier(
    backgroundColor: ColorProvider,
    backgroundImage: ImageProvider?
): GlanceModifier {
    var modifier = GlanceModifier.fillMaxSize().padding(16.dp).background(backgroundColor)
    backgroundImage?.let { image ->
        modifier = modifier.background(image, ContentScale.Crop)
    }

    return modifier
}

private fun textList(
    title: TemplateText? = null,
    subtitle: TemplateText? = null
): List<TemplateText> {
    val result = mutableListOf<TemplateText>()
    title?.let {
        result.add(TemplateText(it.text, TemplateText.Type.Title))
    }
    subtitle?.let {
        result.add(TemplateText(it.text, TemplateText.Type.Label))
    }

    return result
}