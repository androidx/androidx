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
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.template.template.ListTemplateData
import androidx.template.template.TemplateMode

/**
 * Composable layout for a list template app widget. The template is optimized to display a list of
 * items.
 *
 * @param data the data that defines the widget
 */
@Composable
public fun ListTemplate(data: ListTemplateData) {
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

// TODO: Placeholder layouts

@Composable
private fun WidgetLayoutCollapsed(data: ListTemplateData) {
    Column(modifier = createTopLevelModifier(data.backgroundColor)) {
        data.header?.let { header ->
            AppWidgetTemplateHeader(data.headerIcon, header)
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.title?.let { title ->
            Text(title.text, style = TextStyle(fontSize = 20.sp))
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.button?.let { button ->
            Button(text = button.text, onClick = button.action)
        }
    }
}

@Composable
private fun WidgetLayoutVertical(data: ListTemplateData) {
    Column(modifier = createTopLevelModifier(data.backgroundColor)) {
        data.header?.let { header ->
            AppWidgetTemplateHeader(data.headerIcon, header)
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.title?.let { title ->
            Text(title.text, style = TextStyle(fontSize = 20.sp))
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.button?.let { button ->
            Button(text = button.text, onClick = button.action)
        }
        LazyColumn {
            itemsIndexed(data.listContent) { _, item ->
                // TODO: Extract and allow override
                var itemModifier = GlanceModifier.fillMaxWidth()
                item.action?.let { action -> itemModifier = itemModifier.clickable(action) }
                Row(modifier = itemModifier) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(item.title.text, style = TextStyle(fontSize = 18.sp))
                        item.body?.let { body -> Text(body.text) }
                    }
                    item.image?.let { image ->
                        Spacer(modifier = GlanceModifier.width(16.dp))
                        Image(provider = image.image, contentDescription = image.description)
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetLayoutHorizontal(data: ListTemplateData) {
    Column(modifier = createTopLevelModifier(data.backgroundColor)) {
        data.header?.let { header ->
            AppWidgetTemplateHeader(data.headerIcon, header)
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.title?.let { title ->
            Text(title.text, style = TextStyle(fontSize = 20.sp))
            Spacer(modifier = GlanceModifier.height(16.dp))
        }
        data.button?.let { button ->
            Button(text = button.text, onClick = button.action)
        }
    }
}

private fun createTopLevelModifier(backgroundColor: ColorProvider?): GlanceModifier {
    var modifier = GlanceModifier.fillMaxSize().padding(16.dp)
    backgroundColor?.let { color -> modifier = modifier.background(color) }

    return modifier
}
