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
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.template.template.GalleryTemplateData
import androidx.template.template.TemplateMode

/**
 * Composable layout for a gallery template app widget. The template is optimized to show images.
 *
 * @param data the data that defines the widget
 */
@Composable
public fun GalleryTemplate(data: GalleryTemplateData) {
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
private fun WidgetLayoutCollapsed(data: GalleryTemplateData) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(data.backgroundColor),
    ) {
        Row {
            Image(provider = data.image.image, contentDescription = data.image.description)
            Image(provider = data.image.image, contentDescription = data.image.description)
        }
        Text(data.title)
        Text(data.headline)
    }
}

// TODO: Implement when UX has specs.
@Composable
private fun WidgetLayoutVertical(data: GalleryTemplateData) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(data.backgroundColor),
    ) {
    }
}

@Composable
private fun WidgetLayoutHorizontal(data: GalleryTemplateData) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp).background(data.backgroundColor),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Image(provider = data.image.image, contentDescription = data.image.description)
        }
        Spacer(GlanceModifier.width(8.dp))
        Column {
            Text(data.title)
            Text(data.headline)
        }
        Column(verticalAlignment = Alignment.Top) {
            Image(provider = data.image.image, contentDescription = data.image.description)
        }
    }
}
