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
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
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
import androidx.glance.template.ListStyle
import androidx.glance.template.ListTemplateData
import androidx.glance.template.LocalTemplateColors
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.TemplateMode

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
    Column(modifier = createTopLevelModifier()) {
        if (data.listStyle == ListStyle.Full) {
            HeaderBlockTemplate(data.headerBlock)
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
        data.listContent.firstOrNull()?.let { item ->
            val itemModifier = GlanceModifier.fillMaxSize().padding(vertical = 8.dp)
            Row(modifier = itemModifier) {
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    TextBlockTemplate(item.textBlock)
                }
            }
        }
    }
}

@Composable
private fun WidgetLayoutExpanded(data: ListTemplateData) {
    Column(modifier = createTopLevelModifier()) {
        if (data.listStyle == ListStyle.Full) {
            HeaderBlockTemplate(data.headerBlock)
            // TODO(b/247613894): Do not add this spacing if header block is empty
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        LazyColumn {
            itemsIndexed(data.listContent) { _, item ->
                val itemSpacer = if (data.listStyle == ListStyle.Full) 8.dp else 0.dp
                val itemModifier = GlanceModifier.fillMaxSize().padding(vertical = itemSpacer)
                Row(
                    modifier = itemModifier,
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    TextAndImageBlockTemplate(
                        item.textBlock,
                        item.imageBlock,
                        GlanceModifier.defaultWeight()
                    )
                    ActionBlockTemplate(item.actionBlock)
                }
            }
        }
    }
}

@Composable
private fun createTopLevelModifier(): GlanceModifier {
    return GlanceModifier.fillMaxSize().padding(16.dp)
        .background(LocalTemplateColors.current.primaryContainer)
}
