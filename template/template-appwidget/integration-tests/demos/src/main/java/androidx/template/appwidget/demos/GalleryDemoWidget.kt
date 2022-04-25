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

package androidx.template.appwidget.demos

import androidx.compose.runtime.Composable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.ImageProvider
import androidx.glance.appwidget.SizeMode
import androidx.glance.unit.ColorProvider
import androidx.template.appwidget.GalleryTemplate
import androidx.template.template.GalleryTemplateData
import androidx.template.template.TemplateImageWithDescription

/**
 * A widget that uses [GalleryTemplate]. Template locals are not used, so the widget is a regular
 * [GlanceAppWidget].
 */
class GalleryTemplateWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        GalleryTemplate(
            GalleryTemplateData(
                header = "Gallery Template example",
                title = "Gallery Template title",
                headline = "Gallery Template headline",
                image = TemplateImageWithDescription(
                    ImageProvider(R.drawable.compose),
                    "test image"
                ),
                logo = TemplateImageWithDescription(ImageProvider(R.drawable.compose), "test logo"),
                backgroundColor = ColorProvider(R.color.default_widget_background)
            )
        )
    }
}

class GalleryDemoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GalleryTemplateWidget()
}
