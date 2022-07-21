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
import androidx.glance.Image
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.template.GalleryTemplateData
import androidx.glance.template.LocalTemplateColors
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.TemplateMode
import androidx.glance.text.Text

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
    val modifier = createTopLevelModifier(data, true)

    Column(modifier = modifier) {
        data.header?.let { AppWidgetTemplateHeader(it) }
        Spacer(modifier = GlanceModifier.defaultWeight())
        AppWidgetTextSection(
            listOfNotNull(
                data.mainTextBlock.text1,
                data.mainTextBlock.text2,
                data.mainTextBlock.text3
            )
        )
    }
}

// TODO: Implement when UX has specs.
@Composable
private fun WidgetLayoutVertical(data: GalleryTemplateData) {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                MainImageBlock(data)
            }
            Spacer(GlanceModifier.width(8.dp))
            Column {
                Text(data.mainTextBlock.text1.text)
                data.mainTextBlock.text2?.let { headline ->
                    Text(headline.text)
                }
            }
            Column(verticalAlignment = Alignment.Top) {
                MainImageBlock(data)
            }
        }
    }
}

@Composable
private fun WidgetLayoutHorizontal(data: GalleryTemplateData) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            MainImageBlock(data)
        }
        Spacer(GlanceModifier.width(8.dp))
        Column {
            Text(data.mainTextBlock.text1.text)
            data.mainTextBlock.text2?.let { headline ->
                Text(headline.text)
            }
        }
        Column(verticalAlignment = Alignment.Top) {
            MainImageBlock(data)
        }
    }
}

@Composable
private fun MainImageBlock(data: GalleryTemplateData) {
    if (data.mainImageBlock.images.isNotEmpty()) {
        val mainImage = data.mainImageBlock.images[0]
        Image(provider = mainImage.image, contentDescription = mainImage.description)
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
