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

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
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
import androidx.glance.template.AspectRatio
import androidx.glance.template.GalleryTemplateData
import androidx.glance.template.ImageSize
import androidx.glance.template.LocalTemplateColors
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.TemplateMode
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
    val aspectRatio: Double = when (data.galleryImageBlock.aspectRatio) {
        AspectRatio.Ratio1x1 -> 1.0
        AspectRatio.Ratio2x3 -> 2.0 / 3
        AspectRatio.Ratio16x9 -> 16.0 / 9
        else -> 1.0
    }
    val imageSize: Double = when (data.galleryImageBlock.size) {
        ImageSize.Small -> 64.0.pow(2.0)
        ImageSize.Medium -> 96.0.pow(2.0)
        ImageSize.Large -> 128.0.pow(2.0)
        else -> 64.0.pow(2.0)
    }
    val margin = 16
    val imageHeight = sqrt(imageSize / aspectRatio)
    val imageWidth = imageHeight * aspectRatio
    val galleryWidth = LocalSize.current.width.value
    val nCols =
        1.coerceAtLeast(ceil(((galleryWidth - margin) / (imageWidth + margin))).roundToInt())
    val gridCells = if (Build.VERSION.SDK_INT >= 31) {
        GridCells.Adaptive((imageWidth + margin).dp)
    } else {
        GridCells.Fixed(nCols)
    }
    Column {
        Row(
            modifier = createCardModifier(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = GlanceModifier.defaultWeight()
            ) {
                HeaderBlockTemplate(data.header)
                Spacer(modifier = GlanceModifier.height(16.dp).defaultWeight())
                TextBlockTemplate(data.mainTextBlock)
                ActionBlockTemplate(data.mainActionBlock)
            }
            SingleImageBlockTemplate(
                data.mainImageBlock
            )
        }
        Row(
            modifier = createCardModifier(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyVerticalGrid(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                gridCells = gridCells
            ) {
                itemsIndexed(data.galleryImageBlock.images) { _, image ->
                    Image(
                        provider = image.image,
                        contentDescription = image.description,
                        modifier = GlanceModifier.padding((margin / 2).dp)
                            .height(imageHeight.dp).width(imageWidth.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
private fun createTopLevelModifier(
    data: GalleryTemplateData,
    isImmersive: Boolean = false
): GlanceModifier {
    var modifier = GlanceModifier
        .fillMaxSize().background(LocalTemplateColors.current.surface)
    if (isImmersive && data.mainImageBlock.images.isNotEmpty()) {
        val mainImage = data.mainImageBlock.images[0]
        modifier = modifier.background(mainImage.image, ContentScale.Crop)
    }

    return modifier
}

@Composable
private fun createCardModifier() = GlanceModifier.fillMaxWidth().padding(16.dp).cornerRadius(16.dp)
    .background(LocalTemplateColors.current.primaryContainer)
