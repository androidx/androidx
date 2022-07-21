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

@file:GlanceComposable

package androidx.glance.appwidget.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.template.ListStyle
import androidx.glance.template.ListTemplateData
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.TemplateMode
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/**
 * Composable layout for a list template app widget. The template is optimized to display a list of
 * items.
 *
 * @param data the data that defines the widget
 */
@Composable
fun ListTemplate(data: ListTemplateData) {
    when (LocalTemplateMode.current) {
        TemplateMode.Collapsed -> WidgetLayoutCollapsed(data)
        TemplateMode.Vertical -> WidgetLayoutExpanded(data)
        TemplateMode.Horizontal -> WidgetLayoutExpanded(data) // same as Vertical for the List view
    }
}

// TODO: Placeholder layouts

@Composable
private fun WidgetLayoutCollapsed(data: ListTemplateData) {
    Column(modifier = createTopLevelModifier(data)) {
        if (data.listStyle == ListStyle.Full) {
            AppWidgetTemplateHeader(data.headerIcon, data.header, data.button)
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        data.listContent.firstOrNull()?.let { item ->
            var itemModifier = GlanceModifier.fillMaxSize().padding(vertical = 8.dp)
            item.action?.let { action -> itemModifier = itemModifier.clickable(action) }
            Row(modifier = itemModifier) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(item.title.text, style = TextStyle(fontSize = 18.sp), maxLines = 1)
                    if (data.listStyle == ListStyle.Full) {
                        item.body?.let { body ->
                            Spacer(modifier = GlanceModifier.height(4.dp))
                            Text(body.text, style = TextStyle(fontSize = 16.sp), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetLayoutExpanded(data: ListTemplateData) {
    Column(modifier = createTopLevelModifier(data)) {
        if (data.listStyle == ListStyle.Full) {
            AppWidgetTemplateHeader(data.headerIcon, data.header, data.button)
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.title?.let { title ->
            Text(title.text, style = TextStyle(fontSize = 20.sp))
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        LazyColumn {
            itemsIndexed(data.listContent) { _, item ->
                // TODO: Extract and allow override
                val itemSpacer = if (data.listStyle == ListStyle.Full) 8.dp else 0.dp
                var itemModifier = GlanceModifier.fillMaxSize().padding(vertical = itemSpacer)
                item.action?.let { action -> itemModifier = itemModifier.clickable(action) }
                Row(
                    modifier = itemModifier,
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    if (data.listStyle == ListStyle.Full) {
                        item.image?.let { image ->
                            Image(
                                provider = image.image,
                                contentDescription = image.description,
                            )
                            Spacer(modifier = GlanceModifier.width(16.dp))
                        }
                    }
                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            item.title.text,
                            style = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                            maxLines = 1
                        )
                        if (data.listStyle == ListStyle.Full) {
                            item.body?.let { body ->
                                Text(body.text, style = TextStyle(fontSize = 16.sp), maxLines = 2)
                            }
                        }
                        if (data.listStyle == ListStyle.Full) {
                            item.label?.let { label ->
                                Spacer(modifier = GlanceModifier.height(4.dp))
                                Text(label.text)
                            }
                        }
                    }
                    item.button?.let { button ->
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        AppWidgetTemplateButton(button)
                    }
                }
            }
        }
    }
}

private fun createTopLevelModifier(data: ListTemplateData): GlanceModifier {
    var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
    data.backgroundColor?.let { color -> modifier = modifier.background(color) }

    return modifier
}
