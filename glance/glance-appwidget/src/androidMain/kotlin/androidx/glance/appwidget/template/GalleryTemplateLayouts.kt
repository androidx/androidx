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

package androidx.glance.appwidget.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
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
import androidx.glance.template.GalleryTemplateData
import androidx.glance.template.LocalTemplateColors
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.TemplateMode

/**
 * Composable layout for a gallery template app widget. The template is optimized to show images.
 *
 * @param data the data that defines the widget
 */
@Composable
fun GalleryTemplate(data: GalleryTemplateData) {
    when (LocalTemplateMode.current) {
        TemplateMode.Collapsed -> WidgetLayoutCollapsed(data)
        TemplateMode.Vertical -> WidgetLayoutVertical(data)
        TemplateMode.Horizontal -> WidgetLayoutHorizontal(data)
    }
}

@Composable
private fun WidgetLayoutCollapsed(data: GalleryTemplateData) {
    Column(modifier = createTopLevelModifier(data, true)) {
        HeaderBlockTemplate(data.header)
        Spacer(modifier = GlanceModifier.defaultWeight())
        TextBlockTemplate(data.mainTextBlock)
    }
}

@Composable
private fun WidgetLayoutHorizontal(data: GalleryTemplateData) {
    Row(modifier = createTopLevelModifier(data)) {
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
        ) {
            HeaderBlockTemplate(data.header)
            Spacer(modifier = GlanceModifier.height(16.dp).defaultWeight())
            TextBlockTemplate(data.mainTextBlock)
            ActionBlockTemplate(data.mainActionBlock)
        }
        SingleImageBlockTemplate(
            data.mainImageBlock,
            GlanceModifier.fillMaxHeight().defaultWeight()
        )
    }
}

@Composable
private fun WidgetLayoutVertical(data: GalleryTemplateData) {
    Column(modifier = createTopLevelModifier(data)) {
        HeaderBlockTemplate(data.header)
        Spacer(modifier = GlanceModifier.height(16.dp))
        SingleImageBlockTemplate(data.mainImageBlock, GlanceModifier.fillMaxWidth().defaultWeight())
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            TextBlockTemplate(data.mainTextBlock)
            Spacer(modifier = GlanceModifier.defaultWeight())
            ActionBlockTemplate(data.mainActionBlock)
        }
    }
}

@Composable
private fun createTopLevelModifier(
    data: GalleryTemplateData,
    isImmersive: Boolean = false
): GlanceModifier {
    var modifier = GlanceModifier
        .fillMaxSize().padding(16.dp).background(LocalTemplateColors.current.surface)
    if (isImmersive && data.mainImageBlock.images.isNotEmpty()) {
        val mainImage = data.mainImageBlock.images[0]
        modifier = modifier.background(mainImage.image, ContentScale.Crop)
    }

    return modifier
}
